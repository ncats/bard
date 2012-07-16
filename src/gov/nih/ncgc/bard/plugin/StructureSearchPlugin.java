package gov.nih.ncgc.bard.plugin;

import gov.nih.ncgc.bard.rest.BARDConstants;
import gov.nih.ncgc.bard.tools.SearchResultHandler;
import gov.nih.ncgc.bard.tools.Util;
import gov.nih.ncgc.search.SearchParams;
import gov.nih.ncgc.search.SearchService2;

import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;


/**
 * A simplistic plugin that provides access to chemical structure search.
 * <p/>
 * The <code>runSearch</code> is the method of interest and accepts a query string
 * along with a type (super, sub or sim), cutoff (a float between 0 and 1) and
 * a method (search, count).
 *
 * @author Rajarshi Guha
 */
@Path("/v1/plugins/ss")
public class StructureSearchPlugin implements IPlugin {
    static final String VERSION = "1.0";

    SearchService2 search = null;

    public StructureSearchPlugin() throws Exception {

        // get an instance of the search service
        search = Util.getSearchService();
    }

    /**
     * Get a description of the plugin.
     * <p/>
     * In the implementing class, this method should be annotated using
     * <p/>
     * <pre>
     *
     * @return a description of the plugin.
     * @GET
     * @Path("/_info") </pre>
     * <p/>
     * where the annotations are from the <code>javax.ws.rs</code> hierarchy.
     */
    @GET
    @Produces("text/plain")
    @Path("/_info")
    public String getDescription() {
        return "Provides a simple interface to chemical structure searches";
    }


    @GET
    @Produces("text/plain")
    @Path("/")
    public Response getSearch(@QueryParam("q") String query,
                              @QueryParam("type") String type,
                              @QueryParam("cutoff") String cutoff,
                              @QueryParam("method") String method) {
        if (search == null)
            throw new WebApplicationException(new Exception("Did not get an instance of the search service"), 500);

        if (query == null) throw new WebApplicationException(new Exception("Need to specify the q parameter"), 400);

        Double dcutoff = null;
        if (cutoff != null) {
            try {
                dcutoff = Double.parseDouble(cutoff);
            } catch (NumberFormatException ex) {
                throw new WebApplicationException(new Exception("Bogus similarity value specified"), 400);
            }
        }
        if (!"search".equalsIgnoreCase(method) && "count".equalsIgnoreCase(method))
            throw new WebApplicationException(new Exception("Unsupport method " + method), 400);

        return Response.ok(doSearch(query, type, dcutoff, method)).build();
    }

    @POST
    @Produces("text/plain")
    @Path("/")
    public Response postSearch(@FormParam("q") String query,
                               @FormParam("type") String type,
                               @FormParam("cutoff") String cutoff,
                               @FormParam("method") String method) {
        if (search == null)
            throw new WebApplicationException(new Exception("Did not get an instance of the search service"), 500);

        if (query == null) throw new WebApplicationException(new Exception("Need to specify the q parameter"), 400);

        Double dcutoff = null;
        if (cutoff != null) {
            try {
                dcutoff = Double.parseDouble(cutoff);
            } catch (NumberFormatException ex) {
                throw new WebApplicationException(new Exception("Bogus similarity value specified"), 400);
            }
        }
        if (!"search".equalsIgnoreCase(method) && "count".equalsIgnoreCase(method))
            throw new WebApplicationException(new Exception("Unsupport method " + method), 400);

        return Response.ok(doSearch(query, type, dcutoff, method)).build();
    }

    /**
     * oerform a chemical structure search.
     *
     * @param query  A SMILE string (URL encoded if required)
     * @param type   type of search. Can be super, sub or sim
     * @param cutoff a floating point value for similarity cutoff (0-1)
     * @param method specifying 'search' returns matching molecules and
     *               specifying 'count' returns an approximate count of the
     *               matches
     * @return an SDF formatted response with the matching molecules. The default
     *         response will have a Content-type of text/plain.
     */
    private String doSearch(String query,
                            String type,
                            double cutoff,
                            String method) {

        SearchParams params = null;
        if (type != null) {
            if (type.startsWith("sub")) {
                params = SearchParams.substructure();
            } else if (type.startsWith("super")) {
                params = SearchParams.superstructure();
            } else if (type.startsWith("sim")) {
                params = SearchParams.similarity();
                params.setSimilarity(cutoff);
            }
        } else if (type.startsWith("exact")) {
            params = SearchParams.exact();
        } else {
            params = SearchParams.substructure();
        }

        if (method == null) {
            method = "search";
        }

        Writer writer = new StringWriter();
        PrintWriter pw = new PrintWriter(writer);
        if ("search".equalsIgnoreCase(method)) {
            search.search(query, params, new SearchResultHandler(params, pw));
        } else if ("count".equalsIgnoreCase(method)) {
            pw.println(search.count(query, params));
        }
        return writer.toString();
    }

    /**
     * Get the manifest for this plugin.
     * <p/>
     * This should be an XML document conforming
     * to the plugin manifest specification described
     * <a href="http://foo.bar">here</a>
     *
     * @return an XML document containing the plugin manifest
     */

    public String getManifest() {
        return "";
    }

    /**
     * Get the version for the plugin.
     *
     * @return the plugin version
     */
    public String getVersion() {
        return VERSION;
    }

    /**
     * Get the REST resource paths for this plugin.
     * <p/>
     * The paths should omit the BARD prefix. Multiple paths
     * can be returned (and should ideally be documented via the
     * description, though this is currently not enforced).
     * <p/>
     * A plugin must return at least one resource path. If not
     * it will fail the compliance checks
     *
     * @return an array of REST resource paths
     */
    public String[] getResourcePaths() {
        List<String> paths = Util.getResourcePaths(this.getClass());
        String[] ret = new String[paths.size()];
        for (int i = 0; i < paths.size(); i++)
            ret[i] = BARDConstants.API_BASE + "plugins/" + paths.get(i);
        return ret;
    }
}
