package gov.nih.ncgc.bard.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import gov.nih.ncgc.bard.search.AssaySearch;
import gov.nih.ncgc.bard.search.CompoundSearch;
import gov.nih.ncgc.bard.search.ISolrSearch;
import gov.nih.ncgc.bard.search.ProjectSearch;
import gov.nih.ncgc.bard.search.SearchResult;
import gov.nih.ncgc.bard.search.SolrField;
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
import javax.ws.rs.core.Context;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.HttpHeaders;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.servlet.ServletContext;
import javax.annotation.PostConstruct;

import gov.nih.ncgc.bard.tools.Util;
import gov.nih.ncgc.bard.tools.DBUtils;

/**
 * A resource to expose full-text and faceted search as well as autocomplete suggestions.
 *
 * @author Rajarshi Guha
 */
@Path("/search")
public class BARDSearchResource implements IBARDResource {
    static final String DEFAULT_SOLR_SERVICE = "http://localhost:8090/solr";

    Logger log;
    String solrService;
    @Context
    ServletContext servletContext;
    @Context
    protected HttpHeaders headers;


    public BARDSearchResource() {
        log = LoggerFactory.getLogger(this.getClass());
    }

    synchronized public String getSolrService() {
        if (solrService == null) {
            solrService = servletContext.getInitParameter("solr-server");
            if (solrService == null) {
                log.warn("No solr_server specified; using default value!");
                solrService = DEFAULT_SOLR_SERVICE;
            }
            log.info("** Solr service: " + solrService);
        }
        return solrService;
    }

    protected static boolean init = false;
    @PostConstruct
    protected void postConstruct() {
        if (!init) {
            String ctx = servletContext.getInitParameter("datasource-context");
            if (ctx != null) {
                log.info("## datasource context: "+ctx);
                DBUtils.setDataSourceContext(ctx);
            }
            init = true;
        }
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
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/projects/fields")
    public Response getProjectields() throws IOException {
        ProjectSearch search = new ProjectSearch(null);
        List<SolrField> fields;
        try {
            search.setSolrURL(getSolrService ());
            fields = search.getFieldNames();
        } catch (Exception e) {
            throw new WebApplicationException(e, 500);
        }
        return Response.ok(Util.toJson(fields)).type(MediaType.APPLICATION_JSON).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/compounds/fields")
    public Response getCompoundFields() throws IOException {
        CompoundSearch search = new CompoundSearch(null);
        List<SolrField> fields;
        try {
            search.setSolrURL(getSolrService ());
            fields = search.getFieldNames();
        } catch (Exception e) {
            throw new WebApplicationException(e, 500);
        }
        return Response.ok(Util.toJson(fields)).type(MediaType.APPLICATION_JSON).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/assays/fields")
    public Response getAssayFields() throws IOException {
        AssaySearch search = new AssaySearch(null);
        List<SolrField> fields;
        try {
            search.setSolrURL(getSolrService ());
            fields = search.getFieldNames();
        } catch (Exception e) {
            throw new WebApplicationException(e, 500);
        }
        return Response.ok(Util.toJson(fields)).type(MediaType.APPLICATION_JSON).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/assays/suggest")
    public Response autoSuggestAssays(@QueryParam("q") String q, @QueryParam("top") Integer top) throws Exception {
        return autoSuggest(q, "assays", top);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/compounds/suggest")
    public Response autoSuggestCompounds(@QueryParam("q") String q, @QueryParam("top") Integer top) throws Exception {
        return autoSuggest(q, "compounds", top);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/projects/suggest")
    public Response autoSuggestProjects(@QueryParam("q") String q, @QueryParam("top") Integer top) throws Exception {
        return autoSuggest(q, "projects", top);
    }

    private Response autoSuggest(String q, String entity, Integer top) throws Exception {
        ISolrSearch search = null;

        if (q == null) throw new WebApplicationException(400);
        if (top == null) top = 10;
        if (entity == null) search = new AssaySearch(q);
        else if (entity.toLowerCase().equals("assays")) search = new AssaySearch(q);
        else if (entity.toLowerCase().equals("projects")) search = new ProjectSearch(q);
        else if (entity.toLowerCase().equals("compounds")) search = new CompoundSearch(q);

        search.setSolrURL(getSolrService ());
        // get field names associated with this entity search
        List<SolrField> fieldNames = search.getFieldNames();

        // get terms for each field
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode node = mapper.createObjectNode();
        node.putPOJO("query", q);

        long start = System.currentTimeMillis();
        Map<String, List<String>> terms = search.suggest(fieldNames.toArray(new SolrField[0]), q, top);
        long end = System.currentTimeMillis();
        System.out.println("Auto suggest for '" + q + "' on " + search.getClass().getName() + " took " + ((end - start) / 1000.0) + "s");

        for (String fieldName : terms.keySet()) {
            // ignore fields that provided no matching terms
            if (terms.get(fieldName).size() > 0) node.putPOJO(fieldName, terms.get(fieldName));
        }

        String json = mapper.writeValueAsString(node);
        return Response.ok(json).type(MediaType.APPLICATION_JSON).build();
    }

    class SuggestHelper {
        Map<String, List<String>> map;
        String entity;

        SuggestHelper(Map<String, List<String>> map, String entity) {
            this.map = map;
            this.entity = entity;
        }
    }

    class SuggestRunner implements Callable<SuggestHelper> {

        private ISolrSearch search;
        private String q;
        private Integer n;

        private String name;

        SuggestRunner(ISolrSearch search, String q, Integer n) {
            this.search = search;
            this.q = q;
            this.n = n;
            if (search instanceof AssaySearch) name = "assay";
            else if (search instanceof CompoundSearch) name = "compound";
            else if (search instanceof ProjectSearch) name = "project";
        }

        public SuggestHelper call() throws Exception {
            List<SolrField> fieldNames = search.getFieldNames();
            return new SuggestHelper(search.suggest(fieldNames.toArray(new SolrField[0]), q, n), name);
        }
    }

    @GET
    @Path("/suggest")
    public Response autoSuggest(@QueryParam("q") String q,
                                @QueryParam("top") Integer top) throws IOException, SolrServerException {
        if (q == null) throw new WebApplicationException(400);
        if (top == null) top = 10;

        AssaySearch as = new AssaySearch(q);
        CompoundSearch cs = new CompoundSearch(q);
        ProjectSearch ps = new ProjectSearch(q);

        ISolrSearch[] searches = new ISolrSearch[]{as, cs, ps};
        ArrayList<Callable<SuggestHelper>> callables = new ArrayList<Callable<SuggestHelper>>();
        for (ISolrSearch search : searches) {
            search.setSolrURL(getSolrService());
            callables.add(new SuggestRunner(search, q, top));
        }
        long start = System.currentTimeMillis();
        ExecutorService pool = Executors.newFixedThreadPool(searches.length);

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root = mapper.createObjectNode();

        try {
            List<Future<SuggestHelper>> futures = pool.invokeAll(callables);
            for (Future<SuggestHelper> future : futures) {
                Map<String, List<String>> terms = future.get().map;
                String entity = future.get().entity;
                ObjectNode node = mapper.createObjectNode();
                for (String fieldName : terms.keySet()) {
                    if (terms.get(fieldName).size() > 0) node.putPOJO(fieldName, terms.get(fieldName));
                }
                root.putPOJO(entity, node);
            }
        } catch (InterruptedException e) {

        } catch (ExecutionException e) {

        }
        long end = System.currentTimeMillis();
        System.out.println("Autosuggest for all entities in " + ((end - start) / 1000.0) + "s");

        String json = mapper.writeValueAsString(root);
        return Response.ok(json).type(MediaType.APPLICATION_JSON).build();
    }


    class SearchRunner implements Callable<ISolrSearch> {

        private ISolrSearch search;
        private Integer top;
        private boolean expand;
        private String filter;
        private Integer skip;

        SearchRunner(ISolrSearch search, boolean expand, String filter, Integer top, Integer skip) {
            this.search = search;
            this.skip = skip;
            this.top = top;
            this.filter = filter;
            this.expand = expand;
        }

        public ISolrSearch call() throws Exception {
            search.run(expand, filter, top, skip);
            return search;
        }
    }

    /**
     * Run full-text search simultaneously across all entities.
     *
     * @param q      The query string
     * @param filter field based filter parameters of the form <code>fq(field_name:field_value)</code>
     * @param skip   Number of results to skip
     * @param top    How many results to return
     * @param expand If <code>true</code> return detailed response, else return a condensed summary of the hits
     * @return A JSON response containing hit summaries for all entities searched.
     * @throws IOException
     * @throws SolrServerException
     */
    @GET
    @Path("/")
    public Response runSearch(@QueryParam("q") String q,
                              @QueryParam("filter") String filter,
                              @QueryParam("skip") Integer skip,
                              @QueryParam("top") Integer top,
                              @QueryParam("expand") String expand) throws IOException, SolrServerException {
        if (q == null) throw new WebApplicationException(400);
        if (top == null) top = 10;
        if (skip == null) skip = 0;

        AssaySearch as = new AssaySearch(q);
        CompoundSearch cs = new CompoundSearch(q);
        ProjectSearch ps = new ProjectSearch(q);

        ISolrSearch[] searches = new ISolrSearch[]{as, cs, ps};
        Collection<Callable<ISolrSearch>> callables = new ArrayList<Callable<ISolrSearch>>();
        for (ISolrSearch search : searches) {
            search.setSolrURL(getSolrService());
            callables.add(new SearchRunner(search, expand != null && expand.toLowerCase().equals("true"), filter, top, skip));
        }
        long start = System.currentTimeMillis();
        ExecutorService pool = Executors.newFixedThreadPool(searches.length);
        try {
            List<Future<ISolrSearch>> futures = pool.invokeAll(callables);
            for (int i = 0; i < futures.size(); i++) searches[i] = futures.get(i).get();
        } catch (InterruptedException e) {

        } catch (ExecutionException e) {

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
                node.putPOJO(entityName, results);
            }
        }
        String json = mapper.writeValueAsString(node);
        return Response.ok(json).type(MediaType.APPLICATION_JSON).build();
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

        if (Util.countRequested(headers))
            return Response.ok(String.valueOf(s.getMetaData().getNhit())).type(MediaType.TEXT_PLAIN).build();
        return Response.ok(Util.toJson(s)).tag(s.getETag()).type("application/json").build();
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

        if (Util.countRequested(headers))
            return Response.ok(String.valueOf(s.getMetaData().getNhit())).type(MediaType.TEXT_PLAIN).build();
        return Response.ok(Util.toJson(s)).tag(s.getETag()).type("application/json").build();
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
        if (Util.countRequested(headers))
            return Response.ok(String.valueOf(s.getMetaData().getNhit())).type(MediaType.TEXT_PLAIN).build();
        return Response.ok(Util.toJson(s)).tag(s.getETag()).type("application/json").build();
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
