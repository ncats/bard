package gov.nih.ncgc.bard.search;

import gov.nih.ncgc.bard.tools.DBUtils;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Node;
import nu.xom.Nodes;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

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

    protected String CORE_NAME = null;

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

    public Map<String, List<String>> suggest(SolrField[] fields, String q, Integer n) throws MalformedURLException, SolrServerException {
        return SearchUtil.getTerms(getSolrURL() + CORE_NAME, fields, q + ".*", n);
    }

    /**
     * Get field names associated with a Solr document.
     * <p/>
     * As we store Solr entity documents in different cores, we can identify fields
     * based on the name of the core.
     *
     * @return A list of {@link SolrField} objects.
     * @throws Exception
     */
    public List<SolrField> getFieldNames() throws Exception {
        if (CORE_NAME == null) throw new Exception("Must have a valid CORE_NAME");

        String lukeUrl = getSolrURL() + CORE_NAME + "admin/luke?numTerms=0";
        List<SolrField> fieldNames = new ArrayList<SolrField>();
        Client client = Client.create();
        WebResource resource = client.resource(lukeUrl);
        ClientResponse response = resource.get(ClientResponse.class);
        int status = response.getStatus();
        if (status != 200) {
            throw new Exception("There was a problem querying " + lukeUrl);
        }
        String xml = response.getEntity(String.class);
        Document doc = new Builder(false).build(xml, null);
        Nodes nodes = doc.query("/response/lst[@name='fields']");
        if (nodes.size() > 0) {
            Node node = nodes.get(0);
            for (int i = 0; i < node.getChildCount(); i++) {
                Node n = node.getChild(i);
                String name = ((Element) n).getAttribute("name").getValue();
                if (name.endsWith("text")) continue;

                Node sn = n.getChild(0);
                String type = sn.getValue();

                fieldNames.add(new SolrField(name, type));
            }
        }
        client.destroy();
        return fieldNames;
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

        if (filter == null) return solrQuery;
        try {
            List<SolrField> fields = getFieldNames();
            List<String> fnames = new ArrayList<String>();
            for (SolrField field : fields) fnames.add(field.getName());

            Map<String, List<String>> fq = SearchUtil.extractFilterQueries(filter, getFieldNames());
            for (Map.Entry<String, List<String>> entry : fq.entrySet()) {
                String fname = entry.getKey();
                List<String> fvalues = entry.getValue();
                if (!fnames.contains(fname)) continue;

                StringBuilder sb = new StringBuilder();
                sb.append(fvalues.get(0));
                for (int i = 1; i < fvalues.size(); i++) {
                    if (fvalues.get(i).contains("["))
                        sb.append(" OR ").append(fvalues.get(i)); // name + ":" + fvalue
                    else {
                        sb.append(" OR ").append("\"").append(fvalues.get(i).replace("\"", "")).append("\"");
                        sb.append(" OR ").append(fvalues.get(i));
                    }
                }
                if (fvalues.size() == 1) {
                    solrQuery.addFilterQuery(fname + ":" + sb.toString()+"");
                }
                else solrQuery.addFilterQuery(fname + ":(" + sb.toString()+")");
            }
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        return solrQuery;
    }

    protected List<SolrDocument> getHighlightedDocuments(QueryResponse response, String primaryKey, String highlightField) {
        List<SolrDocument> docs = new ArrayList<SolrDocument>();
        SolrDocumentList sdl = response.getResults();
        for (SolrDocument doc : sdl) {
            String pkey = (String) doc.getFieldValue(primaryKey);
            if (response.getHighlighting() != null && highlightField != null) {
                List<String> hls = response.getHighlighting().get(pkey).get(highlightField);
                if (hls != null) {
                    doc.addField("highlight", hls.get(0));
                }
            }
            doc.removeFields("anno_val");
            doc.removeFields("anno_key");
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
