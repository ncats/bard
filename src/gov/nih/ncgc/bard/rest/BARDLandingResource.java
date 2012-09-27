package gov.nih.ncgc.bard.rest;

import gov.nih.ncgc.bard.entity.BardLinkedEntity;
import gov.nih.ncgc.bard.tools.Util;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * A one line summary.
 *
 * @author Rajarshi Guha
 */
@Path("/")
public class BARDLandingResource extends BARDResource {
    @Override
    public Class getEntityClass() {
        return null;
    }

    @Override
    public String getResourceBase() {
        return BARDConstants.API_BASE;
    }

    public String info() {
        return "This is the root of the BARD REST API hierarchy.";
    }

    @GET
    @Path("/")
    public Response getResources(@QueryParam("filter") String filter, @QueryParam("expand") String expand, @QueryParam("skip") Integer skip, @QueryParam("top") Integer top) {
        List<String> res = new ArrayList<String>(Arrays.asList(new String[]{"/assays", "/compounds", "/projects", "/experiments", "/targets", "/documents", "/substances"}));
        Collections.sort(res);
        BardLinkedEntity ble = new BardLinkedEntity(res, null);
        try {
            String json = Util.toJson(ble);
            return Response.ok(json).type(MediaType.APPLICATION_JSON).build();
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        return Response.ok("System error").status(500).type(MediaType.TEXT_PLAIN).build();
    }

    public Response getResources(@PathParam("name") String resourceId, @QueryParam("filter") String filter, @QueryParam("expand") String expand) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
