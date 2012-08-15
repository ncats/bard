package gov.nih.ncgc.bard.search;

import org.apache.solr.client.solrj.SolrServerException;

import java.net.MalformedURLException;
import java.util.List;

/**
 * An interface that classes supporting Solr search will implement.
 *
 * @author Rajarshi Guha
 */
public interface ISolrSearch {
    public void run(boolean detailed, Integer top, Integer skip) throws MalformedURLException, SolrServerException;

    public List<Facet> getFacets();

    public int getHitCount();

    public SearchResult getSearchResults();
}
