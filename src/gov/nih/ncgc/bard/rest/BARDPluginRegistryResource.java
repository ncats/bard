package gov.nih.ncgc.bard.rest;

import gov.nih.ncgc.bard.tools.Util;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * A one line summary.
 *
 * @author Rajarshi Guha
 */
@Path("/plugins/registry")
public class BARDPluginRegistryResource implements IBARDResource {

    @GET
    @Produces("text/plain")
    @Path("/_info")
    public String info() {
        StringBuilder msg = new StringBuilder("Lists available plugins\n\nAvailable resources:\n");
        List<String> paths = Util.getResourcePaths(this.getClass());
        for (String path : paths) msg.append(path).append("\n");
        msg.append("/plguins/" + BARDConstants.API_EXTRA_PARAM_SPEC + "\n");
        return msg.toString();
    }

    /**
     * Return a count of (possibly filtered) instances of a given resource.
     *
     * @param filter A query filter or null
     * @return the number of instances
     */
    public String count(@QueryParam("filter") String filter) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @GET
    @Produces("application/json")
    public Response getResources(@QueryParam("filter") String filter, @QueryParam("expand") String expand, @QueryParam("skip") Integer skip, @QueryParam("top") Integer top) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Response getResources(@PathParam("name") String resourceId, @QueryParam("filter") String filter, @QueryParam("expand") String expand) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
