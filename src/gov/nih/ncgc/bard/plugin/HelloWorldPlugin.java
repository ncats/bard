package gov.nih.ncgc.bard.plugin;

import gov.nih.ncgc.bard.rest.BARDConstants;
import gov.nih.ncgc.bard.tools.Util;

import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * The simplest possible plugin.
 *
 * @author Rajarshi Guha
 */
@Path("/plugins/hworld")
public class HelloWorldPlugin implements IPlugin {
    private static final String VERSION = "1.0";

    public HelloWorldPlugin(@Context ServletContext context) {
        // access servlet config information
    }

    /**
     * Get a description of the plugin.
     *
     * @return a description of the plugin.
     */
    @GET
    @Produces("text/plain")
    @Path("/_info")
    public String getDescription() {
        return "Trivial plugin that does nothing";
    }

    @GET
    @Path("/aresource")
    @Produces("text/html")
    public Response getResource() {
        return Response.ok("<b>Hello World</b>", MediaType.TEXT_HTML).build();
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
            ret[i] = BARDConstants.API_BASE + paths.get(i);
        return ret;
    }
}
