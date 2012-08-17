package gov.nih.ncgc.bard.rest;

import gov.nih.ncgc.bard.search.AssaySearch;
import gov.nih.ncgc.bard.search.CompoundSearch;
import gov.nih.ncgc.bard.search.ISolrSearch;
import gov.nih.ncgc.bard.search.ProjectSearch;
import gov.nih.ncgc.bard.search.SearchResult;
import gov.nih.ncgc.bard.tools.Util;
import org.apache.solr.client.solrj.SolrServerException;
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
import java.net.MalformedURLException;
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
    @Path("/")
    public Response runSearch(@QueryParam("q") String q,
                              @QueryParam("filter") String filter,
                              @QueryParam("skip") Integer skip,
                              @QueryParam("top") Integer top,
                              @QueryParam("expand") String expand) throws IOException, SolrServerException {
        if (q == null) throw new WebApplicationException(400);
        ISolrSearch as = new AssaySearch(q);
        as.run(expand != null && expand.toLowerCase().equals("true"), filter, top, skip);
        SearchResult s = as.getSearchResults();
        return Response.ok(Util.toJson(s)).type("application/json").build();
    }


    @GET
    @Path("/compounds")
    public Response runCompoundSearch(@QueryParam("q") String q,
                                      @QueryParam("filter") String filter,
                                      @QueryParam("skip") Integer skip,
                                      @QueryParam("top") Integer top,
                                      @QueryParam("expand") String expand) throws IOException, SolrServerException {
        if (q == null) throw new WebApplicationException(400);
        SearchResult s = doSearch(new CompoundSearch(q), skip, top, expand, filter);
        return Response.ok(Util.toJson(s)).type("application/json").build();
    }

    @GET
    @Path("/assays")
    public Response runAssaySearch(@QueryParam("q") String q,
                                   @QueryParam("filter") String filter,
                                   @QueryParam("skip") Integer skip,
                                   @QueryParam("top") Integer top,
                                   @QueryParam("expand") String expand) throws IOException, SolrServerException {
        if (q == null) throw new WebApplicationException(400);
        SearchResult s = doSearch(new AssaySearch(q), skip, top, expand, filter);
        return Response.ok(Util.toJson(s)).type("application/json").build();
    }

    @GET
    @Path("/projects")
    public Response runProjectSearch(@QueryParam("q") String q,
                                     @QueryParam("filter") String filter,
                                     @QueryParam("skip") Integer skip,
                                     @QueryParam("top") Integer top,
                                     @QueryParam("expand") String expand) throws IOException, SolrServerException {
        if (q == null) throw new WebApplicationException(400);
        SearchResult s = doSearch(new ProjectSearch(q), skip, top, expand, filter);
        return Response.ok(Util.toJson(s)).type("application/json").build();
    }

    private SearchResult doSearch(ISolrSearch s, Integer skip, Integer top, String expand, String filter) throws MalformedURLException, SolrServerException {
        if (top == null) top = 10;
        if (skip == null) skip = 0;

        s.run(expand != null && expand.toLowerCase().equals("true"), filter, top, skip);
        SearchResult sr = s.getSearchResults();

        String link = null;
        if (skip + top <= sr.getMetaData().getNhit()) {
            if (s instanceof AssaySearch) link = "/search/assays?q=" + s.getQuery();
            else if (s instanceof CompoundSearch) link = "/search/compounds?q=" + s.getQuery();
            else if (s instanceof ProjectSearch) link = "/search/projects?q=" + s.getQuery();

            if (filter == null) filter = "";
            else filter = "&filter=" + filter;
            link = link + "&skip=" + (skip + top) + "&top=" + top + filter;

            if (expand == null) expand = "&expand=false";
            else expand = "&expand=" + expand;
            link += expand;
        }
        sr.setLink(link);
        return sr;
    }
}
