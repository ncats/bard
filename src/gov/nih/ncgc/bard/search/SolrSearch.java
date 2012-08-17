package gov.nih.ncgc.bard.search;

import org.apache.solr.common.SolrDocument;

import java.util.ArrayList;
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

    public String getQuery() {
        return query;
    }

    public SearchResult getSearchResults() {
        return results;
    }

    protected List<SolrDocument> copyRange(List<SolrDocument> docs, Integer skip, Integer top, boolean detailed, String... fields) {
        List<SolrDocument> ret = new ArrayList<SolrDocument>();
        if (top == null) top = 10;
        if (skip == null) skip = 0;
        for (int i = skip; i < (skip + top); i++) {
            if (i >= docs.size()) continue;
            docs.get(i).removeFields("text");
            if (!detailed) {
                SolrDocument newDoc = new SolrDocument();
                for (String field : fields) newDoc.addField(field, docs.get(i).getFieldValue(field));
                ret.add(newDoc);
            } else ret.add(docs.get(i));
        }
        return ret;
    }
}
