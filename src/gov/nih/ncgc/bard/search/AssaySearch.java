package gov.nih.ncgc.bard.search;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Full text search for assay entities.
 *
 * @author Rajarshi Guha
 */
public class AssaySearch extends SolrSearch {
    private final String SOLR_URL = SOLR_BASE + "/core-assay/";

    Logger log;

    String[] facetNames = {"assay component", "assay mode", "assay type", "Cell line", "detection method type", "target_name"};

    public AssaySearch(String query) {
        super(query);
        log = LoggerFactory.getLogger(this.getClass());
    }

    public void run(boolean detailed, String filter, Integer top, Integer skip) throws MalformedURLException, SolrServerException {
        results = new SearchResult();

        SolrServer solr = null;
        solr = new CommonsHttpSolrServer(SOLR_URL);

        QueryResponse response = null;

        SolrQuery sq = new SolrQuery(query);
        sq = sq.setHighlight(true).setHighlightSnippets(1).setRows(10000);
        sq.setFacet(true);
        sq.addFacetField("target_name");

        response = solr.query(sq);

        List<SolrDocument> docs = new ArrayList<SolrDocument>();
        SolrDocumentList sdl = response.getResults();
        for (SolrDocument doc : sdl) {
            docs.add(doc);
        }

        // get facet counts
        long start = System.currentTimeMillis();
        facets = new ArrayList<Facet>();
        for (String f : facetNames) facets.add(new Facet(f));

        // first pull in direct facet counts via Solr
        Facet f = null;
        for (Facet aFacet : facets) {
            if (aFacet.getFacetName().equals("target_name")) {
                f = aFacet;
                break;
            }
        }
        FacetField targetFacet = response.getFacetField("target_name");
        List<FacetField.Count> fcounts = targetFacet.getValues();
        if (fcounts != null) {
            for (FacetField.Count fcount : fcounts) {
                f.counts.put(fcount.getName(), (int) fcount.getCount());
            }
        }

        // now process annotations to get remaining facet counts
        for (SolrDocument doc : docs) {

            Collection<Object> keys = doc.getFieldValues("ak_dict_label");
            Collection<Object> values = doc.getFieldValues("av_dict_label");
            if (keys == null || values == null) continue;
            if (keys.size() != values.size())
                log.error("for assay_id = " + doc.getFieldValue("assay_id") + " keys had " + keys.size() + " elements and values had " + values.size() + " elements");

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

        }
        long end = System.currentTimeMillis();
        log.info("Facet summary calculated in " + (end - start) / 1000.0 + "s");

        SearchMeta meta = new SearchMeta();
        meta.setNhit(sdl.getNumFound());
        meta.setFacets(facets);

        // only return the requested number of docs, from the requested starting point
        // and generate reduced representation if required
        List<SolrDocument> ret = copyRange(docs, skip, top, detailed, "assay_id", "name");
        results.setDocs(ret);
        results.setMetaData(meta);

    }
}
