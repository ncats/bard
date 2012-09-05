package gov.nih.ncgc.bard.search;

import gov.nih.ncgc.bard.entity.Compound;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Full text search for compound entities.
 *
 * @author Rajarshi Guha
 */
public class CompoundSearch extends SolrSearch {
    private final String HL_FIELD = "text";
    private final String PKEY_COMPOUND_DOC = "cid";

    Logger log;
    String[] facetNames = {"collection", "mw", "tpsa", "xlogp"};

    public CompoundSearch(String query) {
        super(query);
        CORE_NAME = "/core-compound/";
        log = LoggerFactory.getLogger(this.getClass());
    }

    public void run(boolean detailed, String filter, Integer top, Integer skip) throws MalformedURLException, SolrServerException {
        results = new SearchResult();

        SolrServer solr = null;
        solr = new CommonsHttpSolrServer(getSolrURL() + CORE_NAME);

        QueryResponse response = null;

        SolrQuery sq = new SolrQuery(query);
        sq = setHighlighting(sq, filter == null ? HL_FIELD : HL_FIELD);
        sq = setFilterQueries(sq, filter);
        sq.setRows(10000);

        // add in some default faceting stuff
        sq.setFacet(true);
        sq.addFacetQuery("mw:[* TO 100]");
        sq.addFacetQuery("mw:[100 TO 200]");
        sq.addFacetQuery("mw:[200 TO 300]");
        sq.addFacetQuery("mw:[300 TO *]");

        sq.addFacetQuery("tpsa:[* TO 40]");
        sq.addFacetQuery("tpsa:[40 TO 120]");
        sq.addFacetQuery("tpsa:[120 TO 180]");
        sq.addFacetQuery("tpsa:[180 TO *]");

        sq.addFacetQuery("xlogp:[* TO 1]");
        sq.addFacetQuery("xlogp:[1 TO 3]");
        sq.addFacetQuery("xlogp:[3 TO 5]");
        sq.addFacetQuery("xlogp:[5 TO *]");

        response = solr.query(sq);
        List<SolrDocument> docs = getHighlightedDocuments(response, PKEY_COMPOUND_DOC, HL_FIELD);

        // get facet counts
        long start = System.currentTimeMillis();
        facets = new ArrayList<Facet>();
        for (String f : facetNames) facets.add(new Facet(f));

        // before doing some manual faceting, we extract the
        // facets that we set irectly in the query
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

        List<Long> cids = new ArrayList<Long>();
        for (SolrDocument doc : docs) {

            Collection<Object> keys = doc.getFieldValues("anno_key");
            Collection<Object> values = doc.getFieldValues("anno_val");

            if (keys == null || values == null) continue;

            List<Object> keyList = new ArrayList<Object>(keys);
            List<Object> valueList = new ArrayList<Object>(values);
            for (Facet facet : facets) {
                for (int i = 0; i < keyList.size(); i++) {
                    if (keyList.get(i).equals(facet.getFacetName())) {
                        String v = ((String) valueList.get(i)).trim();
                        if (v.length() == 0 || v.equals("")) continue;
                        if (v.indexOf("|") >= 0) v = v.split("|")[0];
                        facet.addFacetValue(v);
                    }
                }
            }

            Object id = doc.getFieldValue("cid");
            try {
                if (id != null) {
                    long cid = Long.parseLong(id.toString());
                    cids.add(cid);
                }
            } catch (Exception ex) {
                log.warn("** Bogus cid " + id);
            }
        }
        long end = System.currentTimeMillis();
        log.info("Facet summary calculated in " + (end - start) / 1000.0 + "s");

        // only return the requested number of docs, from the requested starting point
        // and generate reduced representation if required
        List<SolrDocument> ret = copyRange(docs, skip, top, detailed, "cid", "iso_smiles", "iupac_name", "highlight");
        SearchMeta meta = new SearchMeta();
        meta.setNhit(response.getResults().getNumFound());
        meta.setFacets(facets);

        try {
            String etag = putEtag(cids, Compound.class);
            results.setETag(etag);
        } catch (Exception e) {
            log.error("Can't process ETag", e);
        }

        results.setDocs(ret);
        results.setMetaData(meta);
    }

}