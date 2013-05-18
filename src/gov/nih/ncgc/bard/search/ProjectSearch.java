package gov.nih.ncgc.bard.search;

import gov.nih.ncgc.bard.entity.Project;
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
import java.util.*;

/**
 * Full text search for project entities.
 *
 * @author Rajarshi Guha
 */
public class ProjectSearch extends SolrSearch {
    private final String HL_FIELD = "text";
    private final String PKEY_PROJECT_DOC = "projectId";

    Logger log;

    String[] facetNames = {"num_expt", "biology", "target_name", "kegg_disease_cat", "class_name"};

    public ProjectSearch(String query, String coreName) {
        super(query);
        CORE_NAME = coreName;
        log = LoggerFactory.getLogger(this.getClass());
    }

    public void run(boolean detailed, String filter, Integer top, Integer skip) throws MalformedURLException, SolrServerException {
        results = new SearchResult();

        SolrServer solr = null;
        solr = new CommonsHttpSolrServer(getSolrURL() + CORE_NAME);

        QueryResponse response = null;

        SolrQuery sq = new SolrQuery(query);
//        sq = setHighlighting(sq, filter == null ? HL_FIELD : HL_FIELD);
        sq = setFilterQueries(sq, filter);
        sq.setShowDebugInfo(true);
        sq.setRows(10000);
        sq.setFacet(true);

        sq.addFacetQuery("num_expt:[* TO 1]");
        sq.addFacetQuery("num_expt:[1 TO 5]");
        sq.addFacetQuery("num_expt:[5 TO 10]");
        sq.addFacetQuery("num_expt:[10 TO *]");

        sq.addFacetField("target_name");
        sq.addFacetField("kegg_disease_cat");
        sq.addFacetField("biology");
        sq.addFacetField("class_name");

        response = solr.query(sq);
        List<SolrDocument> docs = getHighlightedDocuments(response, PKEY_PROJECT_DOC, HL_FIELD);

        // get facet counts
        long start = System.currentTimeMillis();
        facets = new ArrayList<Facet>();
        for (String f : facetNames) facets.add(new Facet(f));

        // non-range facets
        for (Facet aFacet : facets) {
            FacetField targetFacet = response.getFacetField(aFacet.getFacetName());
            if (targetFacet == null) continue;
            List<FacetField.Count> fcounts = targetFacet.getValues();
            if (fcounts != null) {
                for (FacetField.Count fcount : fcounts) {
                    if (fcount.getCount() > 0) aFacet.counts.put(fcount.getName(), (int) fcount.getCount());
                }
            }
        }

        // range facets
        Map<String, Integer> solrf = response.getFacetQuery();
        if (solrf != null) {
            for (Facet f : facets) {
                for (String key : solrf.keySet()) {
                    if (key.startsWith(f.getFacetName())) {
                        f.counts.put(key.replace(f.getFacetName() + ":", ""), solrf.get(key));
                    }
                }
            }
        }

        // TODO in the future facet on project annotations
        List<Long> projIds = new ArrayList<Long>();
        for (SolrDocument doc : docs) {
            projIds.add(Long.parseLong((String) doc.getFieldValue(PKEY_PROJECT_DOC)));
//            Collection<Object> keys = doc.getFieldValues("ak_dict_label");
//            Collection<Object> values = doc.getFieldValues("av_dict_label");
//            if (keys == null || values == null) continue;
//            if (keys.size() != values.size())
//                log.error("for assay_id = " + doc.getFieldValue("assay_id") + " keys had " + keys.size() + " elements and values had " + values.size() + " elements");
//
//            List<Object> keyList = new ArrayList<Object>(keys);
//            List<Object> valueList = new ArrayList<Object>(values);
//            for (Facet facet : facets) {
//                for (int i = 0; i < keyList.size(); i++) {
//                    if (keyList.get(i).equals(facet.getFacetName()))
//                        if (i < valueList.size()) {
//                            facet.addFacetValue((String) valueList.get(i));
//                        }
//                }
//            }

        }
        long end = System.currentTimeMillis();
        log.info("Facet summary calculated in " + (end - start) / 1000.0 + "s");

        SearchMeta meta = new SearchMeta();
        meta.setNhit(response.getResults().getNumFound());
        meta.setFacets(facets);
        meta.setQueryTime(response.getQTime());
        meta.setElapsedTime(response.getElapsedTime());

        try {
            String etag = putEtag(projIds, Project.class);
            results.setETag(etag);
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
            String projectId = (String) doc.getFieldValue(PKEY_PROJECT_DOC);
            Map<String, Object> value = new HashMap<String, Object>();
            List<String> fns = SearchUtil.getMatchingFieldNames(xplainMap.get(projectId));
            for (String fn : fns) {
                Object obj = doc.getFieldValue(fn);
                value.put(fn, obj);
            }
            matchFields.put(projectId, value);
            scores.put(projectId, (Float) doc.getFieldValue("score"));
        }
        meta.setMatchingFields(matchFields);
        meta.setScores(scores);

        List ret;
        if (!detailed) {
            ret = copyRange(docs, skip, top, detailed, PKEY_PROJECT_DOC, "name");
        } else {
            DBUtils db = new DBUtils();
            ret = new ArrayList();
            try {
                for (int i = skip; i < size; i++) {
                    SolrDocument doc = docs.get(i);
                    String projectId = (String) doc.getFieldValue(PKEY_PROJECT_DOC);
                    ret.add(db.getProject(Long.parseLong(projectId)));
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
