package gov.nih.ncgc.bard.rest;

import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

/**
 * A one line summary.
 *
 * @author Rajarshi Guha
 */
public class BARDPluginRegistryResource implements IBARDResource {
    public String info() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
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

    public Response getResources(@QueryParam("filter") String filter, @QueryParam("expand") String expand, @QueryParam("skip") Integer skip, @QueryParam("top") Integer top) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Response getResources(@PathParam("name") String resourceId, @QueryParam("filter") String filter, @QueryParam("expand") String expand) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
