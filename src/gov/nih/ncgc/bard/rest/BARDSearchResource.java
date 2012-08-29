package gov.nih.ncgc.bard.rest;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import gov.nih.ncgc.bard.search.AssaySearch;
import gov.nih.ncgc.bard.search.CompoundSearch;
import gov.nih.ncgc.bard.search.ISolrSearch;
import gov.nih.ncgc.bard.search.ProjectSearch;
import gov.nih.ncgc.bard.search.SearchResult;
import gov.nih.ncgc.bard.tools.Util;
import org.apache.solr.client.solrj.SolrServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * A resource to expose full-text and faceted search.
 *
 * @author Rajarshi Guha
 */
@Path("/search")
public class BARDSearchResource extends BARDResource {
    static final String DEFAULT_SOLR_SERVICE = "http://localhost:8090/solr";

    Logger log;
    String solrService;

    public BARDSearchResource() {
        log = LoggerFactory.getLogger(this.getClass());
    }

    synchronized public String getSolrService() {
        if (solrService == null) {
            solrService = getServletContext().getInitParameter("solr-server");
            if (solrService == null) {
                log.warn("No solr_server specified; using default value!");
                solrService = DEFAULT_SOLR_SERVICE;
            }
            log.info("** Solr service: " + solrService);
        }
        return solrService;
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

    class SearchRunner implements Callable<ISolrSearch> {

        private ISolrSearch search;
        private int top;

        SearchRunner(ISolrSearch search, int top) {
            this.search = search;
            this.top = top;
        }

        public ISolrSearch call() throws Exception {
            search.run(false, null, top, 0);
            return search;
        }
    }

    @GET
    @Produces("application/json")
    @Path("/suggest")
    public Response runSuggest(@QueryParam("q") String q,
                               @QueryParam("top") Integer top) throws IOException, SolrServerException, JsonGenerationException {
        if (q == null) throw new WebApplicationException(400);
        if (top == null) top = 10;

        AssaySearch as = new AssaySearch(q);
        as.setSolrURL(getSolrService());
        CompoundSearch cs = new CompoundSearch(q);
        cs.setSolrURL(getSolrService());
        ProjectSearch ps = new ProjectSearch(q);
        ps.setSolrURL(getSolrService());

        ISolrSearch[] searches = new ISolrSearch[]{as, cs, ps};
        Collection<Callable<ISolrSearch>> callables = new ArrayList<Callable<ISolrSearch>>();
        for (ISolrSearch search : searches) callables.add(new SearchRunner(search, top));
        long start = System.currentTimeMillis();
        ExecutorService pool = Executors.newFixedThreadPool(searches.length);
        try {
            List<Future<ISolrSearch>> futures = pool.invokeAll(callables);
            for (int i = 0; i < futures.size(); i++) searches[i] = futures.get(i).get();
        } catch (InterruptedException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (ExecutionException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        long end = System.currentTimeMillis();
        log.info("Queried all resources in " + ((end - start) / 1000.0) + "s");

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode node = mapper.createObjectNode();
        node.putPOJO("query", q);

        for (ISolrSearch search : searches) {
            SearchResult results = search.getSearchResults();

            String entityName;
            if (search.getClass().isAssignableFrom(AssaySearch.class)) entityName = "assays";
            else if (search.getClass().isAssignableFrom(CompoundSearch.class)) entityName = "compounds";
            else if (search.getClass().isAssignableFrom(ProjectSearch.class)) entityName = "projects";
            else throw new IllegalArgumentException("We don't handle searches of type " + search);

            if (results.getDocs().size() > 0) {
                ObjectNode subNode = mapper.createObjectNode();
                subNode.putPOJO("url", "/search/" + entityName + "?q=" + q);
                subNode.putPOJO("hits", results.getDocs());
                node.putPOJO(entityName, subNode);
            }
        }
        String json = mapper.writeValueAsString(node);
        return Response.ok(json).type(MediaType.APPLICATION_JSON).build();
    }

    @GET
    @Path("/")
    public Response runSearch(@QueryParam("q") String q,
                              @QueryParam("filter") String filter,
                              @QueryParam("skip") Integer skip,
                              @QueryParam("top") Integer top,
                              @QueryParam("expand") String expand) throws IOException, SolrServerException {
        if (q == null) throw new WebApplicationException(400);
        AssaySearch as = new AssaySearch(q);
        as.setSolrURL(getSolrService());
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
        CompoundSearch cs = new CompoundSearch(q);
        cs.setSolrURL(getSolrService());
        SearchResult s = doSearch(cs, skip, top, expand, filter);
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
        AssaySearch as = new AssaySearch(q);
        as.setSolrURL(getSolrService());
        SearchResult s = doSearch(as, skip, top, expand, filter);
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
        ProjectSearch ps = new ProjectSearch(q);
        ps.setSolrURL(getSolrService());
        SearchResult s = doSearch(ps, skip, top, expand, filter);
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
