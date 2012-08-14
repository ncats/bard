package gov.nih.ncgc.bard.search;

import java.util.Map;

/**
 * A one line summary.
 *
 * @author Rajarshi Guha
 */
public abstract class SolrSearch implements ISolrSearch {
    protected static String SOLR_URL = "http://localhost:8090/solr/";
    protected String query = null;
    protected int numHit = -1;
    protected Map<String, Integer> facetCounts = null;
    protected SearchResult results = null;

    protected SolrSearch(String query) {
        this.query = query;
    }

    public Map<String, Integer> getFacetCounts() {
        return facetCounts;
    }

    public int getHitCount() {
        return numHit;
    }

    public SearchResult getSearchResults() {
        return results;
    }
}
