package gov.nih.ncgc.bard.tools;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.testng.annotations.Test;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

/**
 * A one line summary.
 *
 * @author Rajarshi Guha
 */
public class SearchTest {
    protected static String SOLR_URL = "http://tripod.nih.gov/servlet/solr/";

    @Test
    public void testConnect() throws MalformedURLException, SolrServerException {
        SolrServer solr = new CommonsHttpSolrServer(SOLR_URL);
        SolrQuery sq = new SolrQuery("dna+repair");
        QueryResponse response = solr.query(sq);
        List<SolrDocument> docs = new ArrayList<SolrDocument>();
        SolrDocumentList sdl = response.getResults();
        System.out.println("sdl.getNumFound() = " + sdl.getNumFound());
    }
}
