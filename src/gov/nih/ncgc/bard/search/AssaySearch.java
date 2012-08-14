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
import java.util.List;

/**
 * Full text search for assay entities.
 *
 * @author Rajarshi Guha
 */
public class AssaySearch extends SolrSearch {

    Logger log;

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
        sq = sq.setHighlight(true).setHighlightSnippets(1);
        if (top != null) sq = sq.setRows(top);
        if (skip != null) sq = sq.setStart(skip);
        if (!detailed) {
            sq = sq.setFields("assay_id", "name");
        }
        response = solr.query(sq);

        List<SolrDocument> docs = new ArrayList<SolrDocument>();
        SolrDocumentList sdl = response.getResults();
        for (SolrDocument doc : sdl) {
            docs.add(doc);
        }

        SearchMeta meta = new SearchMeta();
        meta.setNhit(sdl.getNumFound());

        results.setDocs(docs);
        results.setMetaData(meta);
    }

}
