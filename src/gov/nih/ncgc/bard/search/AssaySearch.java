package gov.nih.ncgc.bard.search;

import gov.nih.ncgc.bard.capextract.CAPDictionary;
import gov.nih.ncgc.bard.capextract.CAPDictionaryElement;
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

import java.io.IOException;
import java.net.MalformedURLException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Full text search for assay entities.
 *
 * @author Rajarshi Guha
 */
public class AssaySearch extends SolrSearch {
    private final String HL_FIELD = "text";
    private final String PKEY_ASSAY_DOC = "bardAssayId";

    Logger log;

    String[] facetNames = {"assay_component_role", "assay_mode", "assay_type", "Cell line", "detection_method_type", "target_name", "kegg_disease_cat"};

    public AssaySearch(String query) {
        super(query);

        CORE_NAME = "/core-assay/";
        log = LoggerFactory.getLogger(this.getClass());

        // since we currently facte on dictionary terms, lets pre-populate the facet
        // values with the children of the terms, setting their counts to 0
        facets = new ArrayList<Facet>();
        DBUtils db = new DBUtils();
        CAPDictionary dict = null;
        try {
            dict = db.getCAPDictionary();
            db.closeConnection();
        } catch (SQLException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (ClassNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        if (dict != null) {
            log.info("Got CAP dictionary with " + dict.getNodes().size() + " elements");
            for (String name : facetNames) {
                Set<CAPDictionaryElement> childs = dict.getChildren(name);
                if (childs == null) childs = new HashSet<CAPDictionaryElement>();
                Map<String, Integer> counts = new HashMap<String, Integer>();
                for (CAPDictionaryElement child : childs) {
                    counts.put(child.getLabel(), 0);
                }

                Facet f = new Facet(name);
                f.setCounts(counts);
                facets.add(f);
            }
        } else log.error("CAP dictionary was null. Strange!");
    }

    public void run(boolean detailed, String filter, Integer top, Integer skip) throws MalformedURLException, SolrServerException {
        results = new SearchResult();

        SolrServer solr = new CommonsHttpSolrServer
                (getSolrURL() + CORE_NAME);

        SolrQuery sq = new SolrQuery(query);
        sq = setFilterQueries(sq, filter);
        sq.setRows(10000);
        sq.setShowDebugInfo(true);

        sq.setFacet(true);
        sq.setFacetMinCount(1);
        sq.addFacetField("target_name");
        sq.addFacetField("detection_method_type");
        sq.addFacetField("assay_mode");
        sq.addFacetField("assay_component_role");
        sq.addFacetField("kegg_disease_cat");


        QueryResponse response = solr.query(sq);
        List<SolrDocument> docs = getHighlightedDocuments(response, PKEY_ASSAY_DOC, HL_FIELD);

        // get facet counts
        long start = System.currentTimeMillis();

        // first pull in direct facet counts via Solr
        for (Facet aFacet : facets) {
            FacetField targetFacet = response.getFacetField(aFacet.getFacetName());
            if (targetFacet == null) continue;
            List<FacetField.Count> fcounts = targetFacet.getValues();
            if (fcounts != null) {
                for (FacetField.Count fcount : fcounts) {
                    aFacet.counts.put(fcount.getName(), (int) fcount.getCount());
                }
            }
        }

        List<Long> aids = new ArrayList<Long>();
        // now process annotations to get remaining facet counts
        for (SolrDocument doc : docs) {

            Collection<Object> keys = doc.getFieldValues("ak_dict_label");
            Collection<Object> values = doc.getFieldValues("av_dict_label");
            if (keys == null || values == null) continue;
            List<Object> keyList = new ArrayList<Object>(keys);
            List<Object> valueList = new ArrayList<Object>(values);
            for (Facet facet : facets) {
                for (int i = 0; i < keyList.size(); i++) {
                    if (keyList.get(i).equals(facet.getFacetName()))
                        if (i < valueList.size()) {
                            facet.addFacetValue((String) valueList.get(i));
                        }
                }
            }

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
        long end = System.currentTimeMillis();
        log.info("Facet summary calculated in " + (end - start) / 1000.0 + "s");

        SearchMeta meta = new SearchMeta();
        meta.setNhit(response.getResults().getNumFound());
        meta.setFacets(facets);
        meta.setQueryTime(response.getQTime());
        meta.setElapsedTime(response.getElapsedTime());

        try {
            String etag = putEtag(aids, Assay.class);
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
        int size = Math.min(skip+top, docs.size());
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
                for (int i = skip; i < size; i++)  {
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
