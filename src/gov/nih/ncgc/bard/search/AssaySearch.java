package gov.nih.ncgc.bard.search;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
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

    String[] facetNames = {"assay component", "assay mode", "assay type", "Cell line", "detection method type"};

    public AssaySearch(String query) {
        super(query);
        log = LoggerFactory.getLogger(this.getClass());
    }

    public void run(boolean detailed, Integer top, Integer skip) throws MalformedURLException, SolrServerException {
        results = new SearchResult();

        SolrServer solr = null;
        solr = new CommonsHttpSolrServer(SOLR_URL);

        QueryResponse response = null;

        SolrQuery sq = new SolrQuery(query);
        sq = sq.setHighlight(true).setHighlightSnippets(1).setRows(10000);
//        if (skip != null) sq = sq.setStart(skip);
        if (!detailed) {
            sq = sq.setFields("assay_id", "name");
        }
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

        for (Facet f : facets) {
            log.info("FACET: " + f.getFacetName());
            for (String key : f.getCounts().keySet()) {
                log.info("\t" + key + "=" + f.getCounts().get(key));
            }
        }

        SearchMeta meta = new SearchMeta();
        meta.setNhit(sdl.getNumFound());
        meta.setFacets(facets);

        // only return the requested number of docs, from the requested starting point
        List<SolrDocument> ret = new ArrayList<SolrDocument>();
        if (top == null) top = 10;
        if (skip == null) skip = 0;
        for (int i = skip; i <= top; i++) ret.add(docs.get(i));

        results.setDocs(ret);
        results.setMetaData(meta);
    }

    private <T> int getIndex(Collection<T> l, T q) {
        if (l == null) return -1;
        int i = 0;
        for (T t : l) {
            if (t.equals(q)) return i;
            i++;
        }
        return -1;
    }

}
