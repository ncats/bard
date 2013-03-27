package gov.nih.ncgc.bard.search;

import gov.nih.ncgc.bard.entity.Compound;
import gov.nih.ncgc.bard.tools.DBUtils;

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

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Full text search for compound entities.
 *
 * @author Rajarshi Guha
 */
public class CompoundSearch extends SolrSearch {
    private final String HL_FIELD = "text";
    private final String PKEY_COMPOUND_DOC = "cid";

    Logger log;
    String[] facetNames = {"compound_class", "COLLECTION", "mwt", "tpsa", "xlogp"};

    public CompoundSearch(String query, String coreName) {
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
        sq = setFilterQueries(sq, filter);
        sq.setRows(10000);
        sq.setShowDebugInfo(true);

        // add in some default faceting stuff
        sq.setFacet(true);
        sq.addFacetQuery("mwt:[* TO 100]");
        sq.addFacetQuery("mwt:[100 TO 200]");
        sq.addFacetQuery("mwt:[200 TO 300]");
        sq.addFacetQuery("mwt:[300 TO *]");

        sq.addFacetQuery("tpsa:[* TO 40]");
        sq.addFacetQuery("tpsa:[40 TO 120]");
        sq.addFacetQuery("tpsa:[120 TO 180]");
        sq.addFacetQuery("tpsa:[180 TO *]");

        sq.addFacetQuery("xlogp:[* TO 1]");
        sq.addFacetQuery("xlogp:[1 TO 3]");
        sq.addFacetQuery("xlogp:[3 TO 5]");
        sq.addFacetQuery("xlogp:[5 TO *]");

        sq.setFacetMinCount(1);
        sq.addFacetField("compound_class");

        response = solr.query(sq);
        List<SolrDocument> docs = getHighlightedDocuments(response, PKEY_COMPOUND_DOC, HL_FIELD);

        // get facet counts
        long start = System.currentTimeMillis();
        facets = new ArrayList<Facet>();
        for (String f : facetNames) facets.add(new Facet(f));

        // before doing some manual faceting, we extract the
        // facets (query and field) that we set directly in the query
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

        // we manually update facet counts COLLECTION
        List<Long> cids = new ArrayList<Long>();
        for (SolrDocument doc : docs) {
            Object id = doc.getFieldValue(PKEY_COMPOUND_DOC);
            try {
                if (id != null) {
                    long cid = Long.parseLong(id.toString());
                    cids.add(cid);
                }
            } catch (Exception ex) {
                log.warn("** Bogus cid " + id);
            }

            Collection<Object> collection = doc.getFieldValues("COLLECTION");
            if (collection == null) {
                continue;
            }

            List<Object> clist = new ArrayList<Object>(collection);
            Set<String> vset = new HashSet<String>();
            for (Facet facet : facets) {
                if (!facet.getFacetName().equals("COLLECTION")) continue;
                for (Object aClist : clist) {
                    String v = ((String) aClist).trim();
                    if (v == null || v.length() == 0 || v.equals("")) continue;
                    if (v.contains("|")) v = v.split("|")[0].trim();
                    if (v.length() == 0 || v.equals("")) continue;
                    vset.add(v);
                }

                // at this stage we have a unique set of COLLECTION values for this document
                // lets update the COLLECTION facet
                for (String v : vset) facet.addFacetValue(v);
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
            putEtag(cids, Compound.class);
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
            String compoundId = (String) doc.getFieldValue(PKEY_COMPOUND_DOC);
            Map<String, Object> value = new HashMap<String, Object>();
            List<String> fns = SearchUtil.getMatchingFieldNames(xplainMap.get(compoundId));
            for (String fn : fns) {
                Object obj = doc.getFieldValue(fn);
                value.put(fn, obj);
            }
            matchFields.put(compoundId, value);
            scores.put(compoundId, (Float) doc.getFieldValue("score"));
        }
        meta.setMatchingFields(matchFields);
        meta.setScores(scores);

        List ret;
        if (!detailed) {
            ret = copyRange(docs, skip, top, detailed, PKEY_COMPOUND_DOC, "smiles", "iupacName", "preferredTerm", "compound_class");
        } else {
            DBUtils db = new DBUtils();
            ret = new ArrayList();
            try {
                for (int i = skip; i < size; i++) {
                    SolrDocument doc = docs.get(i);
                    String compoundId = (String) doc.getFieldValue(PKEY_COMPOUND_DOC);
                    List<Compound> cmpds = db.getCompoundsByCid(Long.parseLong(compoundId));
                    if (cmpds != null && cmpds.size() >0) ret.add(cmpds.get(0));
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