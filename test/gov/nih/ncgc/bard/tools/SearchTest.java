package gov.nih.ncgc.bard.tools;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.testng.annotations.Test;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A one line summary.
 *
 * @author Rajarshi Guha
 */
public class SearchTest {
    protected static String SOLR_URL = "http://tripod.nih.gov/servlet/solr/";

    @Test
    public void testConnect() throws MalformedURLException, SolrServerException {
        SolrServer solr = new HttpSolrServer(SOLR_URL);
        SolrQuery sq = new SolrQuery("dna+repair");
        QueryResponse response = solr.query(sq);
        List<SolrDocument> docs = new ArrayList<SolrDocument>();
        SolrDocumentList sdl = response.getResults();
        System.out.println("sdl.getNumFound() = " + sdl.getNumFound());
    }

    @Test
    public void testFacet() throws MalformedURLException, SolrServerException {
        String url = "http://tripod.nih.gov/servlet/solr/core-compound/";
        SolrServer solr = new HttpSolrServer(url);
        SolrQuery sq = new SolrQuery("dna+repair");

        sq.setFacet(true);
        sq.addFacetQuery("mw:[* TO 100]");
        sq.addFacetQuery("mw:[100 TO 200]");
        sq.addFacetQuery("mw:[200 TO 300]");
        sq.addFacetQuery("mw:[300 TO *]");

        QueryResponse response = solr.query(sq);
        List<SolrDocument> docs = new ArrayList<SolrDocument>();
        SolrDocumentList sdl = response.getResults();
        System.out.println("sdl.getNumFound() = " + sdl.getNumFound());

        Map<String, Integer> fq = response.getFacetQuery();
        for (String key : fq.keySet()) {
            System.out.println(key + " => " + fq.get(key));
        }
    }

    @Test
    public void testFacetField() throws MalformedURLException, SolrServerException {
        String url = "http://tripod.nih.gov/servlet/solr/core-assay/";
        SolrServer solr = new HttpSolrServer(url);
        SolrQuery sq = new SolrQuery("dna+repair");

        sq.setFacet(true);
        sq.addFacetField("target_name");

        QueryResponse response = solr.query(sq);
        List<SolrDocument> docs = new ArrayList<SolrDocument>();
        SolrDocumentList sdl = response.getResults();
        System.out.println("sdl.getNumFound() = " + sdl.getNumFound());

        FacetField targetFacet = response.getFacetField("target_name");
        List<FacetField.Count> fcounts = targetFacet.getValues();
        for (FacetField.Count fcount : fcounts) {
            System.out.println(fcount.getName() + "=>" + fcount.getCount());
        }
    }


}
