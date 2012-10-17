package gov.nih.ncgc.bard.rest;

import gov.nih.ncgc.bard.entity.DummyEntity;
import gov.nih.ncgc.bard.tools.DBUtils;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Map;

/**
 * A one line summary.
 *
 * @author Rajarshi Guha
 */
@Path("/monitor")
public class BARDMonitorResource extends BARDResource<DummyEntity> implements IBARDResource {

    @Override
    public Class<DummyEntity> getEntityClass() {
        return DummyEntity.class;
    }

    public String getResourceBase () {
        return BARDConstants.API_BASE+"/monitor";
    }

    @GET
    @Path("/_info")
    public String info() {
        return "Retrieve information about current system resources";
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/cache")
    public Response getCacheStatistics() {
        DBUtils db = new DBUtils();
        Map<String, String> stats = db.getCacheStatistics();
        StringBuilder sb = new StringBuilder();
        String sep = "";
        for (String key : stats.keySet()) {
            sb.append(sep).append(stats.get(key));
            sep = "\n";
        }
        return Response.ok(sb.toString(), MediaType.TEXT_PLAIN).build();
    }


    public Response getResources(@QueryParam("filter") String filter, @QueryParam("expand") String expand, @QueryParam("skip") Integer skip, @QueryParam("top") Integer top) {
       return null;
    }

    public Response getResources(@PathParam("name") String resourceId, @QueryParam("filter") String filter, @QueryParam("expand") String expand) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

}
