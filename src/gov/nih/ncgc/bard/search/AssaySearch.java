package gov.nih.ncgc.bard.search;

import gov.nih.ncgc.bard.entity.Assay;
import gov.nih.ncgc.bard.tools.DBUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Full text search for assay entities.
 *
 * @author Rajarshi Guha
 */
public class AssaySearch extends SolrSearch {
    private final String HL_FIELD = "text";
    private final String PKEY_ASSAY_DOC = "bardAssayId";

    Logger log;

    public AssaySearch(String query, String coreName) {
        super(query);

        CORE_NAME = coreName;
        log = LoggerFactory.getLogger(this.getClass());
        DBUtils db = new DBUtils();
        try {
            db.closeConnection();
        } catch (SQLException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    public void run(boolean detailed, String filter, Integer top, Integer skip) throws MalformedURLException, SolrServerException {
        facets = new ArrayList<Facet>();
        results = new SearchResult();

        SolrServer solr = new CommonsHttpSolrServer
                (getSolrURL() + CORE_NAME);

        SolrQuery sq = new SolrQuery(query);
        sq = setFilterQueries(sq, filter);
        sq.setRows(10000);
        sq.setShowDebugInfo(true);

        sq.setFacet(true);
        sq.setFacetMinCount(1);

        QueryResponse response = solr.query(sq);
        List<SolrDocument> docs = getHighlightedDocuments(response, PKEY_ASSAY_DOC, HL_FIELD);

        // get facet counts
        long start = System.currentTimeMillis();

        // pull in field facets
        List<FacetField> solrf = response.getFacetFields();
        for (FacetField f : solrf) {
            Facet bardFacet = new Facet(f.getName());
            List<FacetField.Count> values = f.getValues();
            if (values != null) {
                for (FacetField.Count value : values) bardFacet.counts.put(value.getName(), (int) value.getCount());
            }
            facets.add(bardFacet);
        }
        long end = System.currentTimeMillis();
        log.info("Facet summary calculated in " + (end - start) / 1000.0 + "s");


        List<Long> aids = new ArrayList<Long>();
        for (SolrDocument doc : docs) {
            Object id = doc.getFieldValue(PKEY_ASSAY_DOC);
            try {
                long aid = Long.parseLong(id.toString());
                if (aid > 0l) {
                    aids.add(aid);
                }
            } catch (Exception ex) {
                log.warn("Bogus bardAssayid " + id);
            }
        }


        SearchMeta meta = new SearchMeta();
        meta.setNhit(response.getResults().getNumFound());
        meta.setFacets(facets);
        meta.setQueryTime(response.getQTime());
        meta.setElapsedTime(response.getElapsedTime());

        try {
            putEtag(aids, Assay.class);
        } catch (Exception e) {
            log.error("Can't process ETag", e);
        }

        // only return the requested number of docs, from the requested starting point
        // and generate reduced representation if required
        //
        // Also extract the matching field names for the docs we do return
        Map<String, String> xplainMap = response.getExplainMap();
        Map<String, Map<String, Object>> matchFields = new HashMap<String, Map<String, Object>>();
        Map<String, Float> scores = new LinkedHashMap<String, Float>(); // to maintain doc id ordering

        // first set up field match details & document scores
        int size = Math.min(skip + top, docs.size());
        for (int i = skip; i < size; i++) {
            SolrDocument doc = docs.get(i);
            String assayId = (String) doc.getFieldValue(PKEY_ASSAY_DOC);
            Map<String, Object> value = new HashMap<String, Object>();
            List<String> fns = SearchUtil.getMatchingFieldNames(xplainMap.get(assayId));
            for (String fn : fns) {
                Object obj = doc.getFieldValue(fn);
                value.put(fn, obj);
            }
            matchFields.put(assayId, value);
            scores.put(assayId, (Float) doc.getFieldValue("score"));
        }
        meta.setMatchingFields(matchFields);
        meta.setScores(scores);

        List ret;
        if (!detailed) {
            ret = copyRange(docs, skip, top, detailed, PKEY_ASSAY_DOC, "name");
        } else {
            DBUtils db = new DBUtils();
            ret = new ArrayList();
            try {
                for (int i = skip; i < size; i++) {
                    SolrDocument doc = docs.get(i);
                    String assayId = (String) doc.getFieldValue(PKEY_ASSAY_DOC);
                    ret.add(db.getAssayByAid(Long.parseLong(assayId)));
                }
                db.closeConnection();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        results.setDocs(ret);
        results.setMetaData(meta);
    }
}
