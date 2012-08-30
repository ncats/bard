package gov.nih.ncgc.bard.search;

import gov.nih.ncgc.bard.tools.DBUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

import java.util.ArrayList;
import java.util.List;

/**
 * A one line summary.
 *
 * @author Rajarshi Guha
 */
public abstract class SolrSearch implements ISolrSearch {
    protected String query = null;
    protected int numHit = -1;
    protected List<Facet> facets;
    protected SearchResult results = null;

    protected String solrURL = "http://localhost:8090/solr";

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

    public void setSolrURL(String url) {
        solrURL = url;
    }

    public String getSolrURL() {
        return solrURL;
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

    /**
     * Initiale highlighting.
     * <p/>
     * This initialization is pretty much independent of the entity we're searching on,  hence
     * it's placement in the superclass.
     *
     * @param solrQuery      The query object
     * @param highlightField which field to highlight on
     * @return the updated query object
     */
    protected SolrQuery setHighlighting(SolrQuery solrQuery, String highlightField) {
        solrQuery = solrQuery.setHighlight(true).
                setHighlightSnippets(1).
                setHighlightFragsize(300).
                setHighlightSimplePre("<b>").
                setHighlightSimplePost("</b>");
        return solrQuery.addHighlightField(highlightField);
    }

    /**
     * Convert user specified field based filters to the Solr form.
     *
     * @param solrQuery the query object
     * @param filter    the filter string
     * @return the updated query object
     */
    protected SolrQuery setFilterQueries(SolrQuery solrQuery, String filter) {
        if (filter != null) {
            List<String[]> fq = SearchUtil.extractFilterQueries(filter);
            for (String[] entry : fq) {
                String fname = entry[0];
                String fvalue = entry[1];
                if (fvalue.contains("[")) solrQuery.addFilterQuery(fname + ":" + fvalue);
                else solrQuery.addFilterQuery(fname + ":\"" + fvalue + "\"");
            }
        }
        return solrQuery;
    }

    protected List<SolrDocument> getHighlightedDocuments(QueryResponse response, String primaryKey, String highlightField) {
        List<SolrDocument> docs = new ArrayList<SolrDocument>();
        SolrDocumentList sdl = response.getResults();
        for (SolrDocument doc : sdl) {
            String pkey = (String) doc.getFieldValue(primaryKey);
            List<String> hls = response.getHighlighting().get(pkey).get(highlightField);
            if (hls != null) {
                doc.addField("highlight", hls.get(0));
            }
            docs.add(doc);
        }
        return docs;
    }

    protected String putEtag(List<Long> ids, Class klass) throws Exception {
        DBUtils db = new DBUtils();
        try {
            String etag = db.newETag(query, klass.getName());
            db.putETag(etag, ids.toArray(new Long[0]));
            results.setETag(etag);
            return etag;
        } finally {
            try {
                db.closeConnection();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}
