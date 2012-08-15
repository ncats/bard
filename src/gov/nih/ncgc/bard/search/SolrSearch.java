package gov.nih.ncgc.bard.search;

import java.util.List;

/**
 * A one line summary.
 *
 * @author Rajarshi Guha
 */
public abstract class SolrSearch implements ISolrSearch {
    protected final String SOLR_BASE = "http://localhost:8090/solr";
    protected String query = null;
    protected int numHit = -1;
    protected List<Facet> facets;
    protected SearchResult results = null;

    protected SolrSearch(String query) {
        this.query = query;
    }

    public List<Facet> getFacets() {
        return facets;
    }

    public int getHitCount() {
        return numHit;
    }

    public SearchResult getSearchResults() {
        return results;
    }
}
