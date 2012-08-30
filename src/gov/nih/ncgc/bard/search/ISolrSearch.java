package gov.nih.ncgc.bard.search;

import org.apache.solr.client.solrj.SolrServerException;

import java.net.MalformedURLException;
import java.util.List;
import java.util.Map;

/**
 * An interface that classes supporting Solr search will implement.
 *
 * @author Rajarshi Guha
 */
public interface ISolrSearch {
    public String getSolrURL ();

    public void run(boolean detailed, String filter, Integer top, Integer skip) throws MalformedURLException, SolrServerException;

    public List<Facet> getFacets();

    public int getHitCount();

    public SearchResult getSearchResults();

    public String getQuery();
    
    public void setSolrURL(String url);
    
    public List<String> getFieldNames() throws Exception;
    
    public Map<String, List<String>> suggest(String field, String q, Integer n) throws MalformedURLException, SolrServerException;
}
