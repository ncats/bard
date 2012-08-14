package gov.nih.ncgc.bard.rest;

import gov.nih.ncgc.bard.entity.SearchMeta;
import gov.nih.ncgc.bard.entity.SearchResult;
import gov.nih.ncgc.bard.tools.Util;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A resource to expose full-text and faceted search.
 *
 * @author Rajarshi Guha
 */
@Path("/search")
public class BARDSearchResource extends BARDResource {
    @Context
    ServletContext servletContext;

    // TODO in the future we will have multiple Solr cores, that should
    // be queries simultaneously
    private static String SOLR_URL = "http://tripod.nih.gov/servlet/solr/";

    Logger log;

    public BARDSearchResource() {
        log = LoggerFactory.getLogger(this.getClass());
    }

    @GET
    @Produces("text/plain")
    @Path("/_info")
    public String info() {
        StringBuilder msg = new StringBuilder("General search resource\n\nAvailable resources:\n");
        List<String> paths = Util.getResourcePaths(this.getClass());
        for (String path : paths) msg.append(path).append("\n");
        msg.append("/search/" + BARDConstants.API_EXTRA_PARAM_SPEC + "\n");
        return msg.toString();
    }

    public Response getResources(@QueryParam("filter") String filter, @QueryParam("expand") String expand, @QueryParam("skip") Integer skip, @QueryParam("top") Integer top) {
        return null;
    }

    public Response getResources(@PathParam("name") String resourceId, @QueryParam("filter") String filter, @QueryParam("expand") String expand) {
        return null;
    }

    @GET
    @Path("/{q}")
    public Response runSearch(@PathParam("q") String q,
                              @QueryParam("skip") Integer skip,
                              @QueryParam("top") Integer top,
                              @QueryParam("expand") String expand) throws IOException {
        SearchResult s = new SearchResult();

        SolrServer solr = new CommonsHttpSolrServer(SOLR_URL);
        QueryResponse response = null;
        try {
            SolrQuery sq = new SolrQuery(q);
            sq = sq.setHighlight(true).setHighlightSnippets(1);
            if (top != null) sq = sq.setRows(top);
            if (skip != null) sq = sq.setStart(skip);
            if (expand != null && !expand.toLowerCase().equals("true")) {
                sq = sq.setFields("assay_id", "name");
            }
            response = solr.query(sq);
        } catch (SolrServerException e) {
            throw new WebApplicationException(e, 500);
        }

        List<SolrDocument> docs = new ArrayList<SolrDocument>();
        SolrDocumentList results = response.getResults();
        for (SolrDocument doc : results) {
            docs.add(doc);
        }

        SearchMeta meta = new SearchMeta();
        meta.setNhit(results.getNumFound());

        s.setDocs(docs);
        s.setMetaData(meta);

        return Response.ok(Util.toJson(s)).type("application/json").build();
    }


}
