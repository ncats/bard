package gov.nih.ncgc.bard.rest;

import gov.nih.ncgc.bard.capextract.CAPDictionary;
import gov.nih.ncgc.bard.tools.DBUtils;
import gov.nih.ncgc.bard.tools.Util;

import java.io.IOException;
import java.sql.SQLException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * A resource to expose CAP information.
 * <p/>
 * Currently the this resource only has one subresource that exposes
 * the CAP dictionary. This is is exposed a direct conversion from the
 * internal data structure ({@link CAPDictionary}) to a JSON representation,
 * so it could be optimized in the future.
 *
 * @author Rajarshi Guha
 */
@Path("/cap")
public class BARDCapResource implements IBARDResource {
    public String info() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @GET
    @Path("/dictionary")
    public Response getResources(@QueryParam("filter") String filter, @QueryParam("expand") String expand, @QueryParam("skip") Integer skip, @QueryParam("top") Integer top) {
        DBUtils db = new DBUtils();
        try {
            CAPDictionary dict = db.getCAPDictionary();
            String json = Util.toJson(dict);
            db.closeConnection();
            return Response.ok(json).type(MediaType.APPLICATION_JSON).build();
        } catch (SQLException e) {
            throw new WebApplicationException(Response.status(500).entity(e).build());
        } catch (IOException e) {
            throw new WebApplicationException(Response.status(500).entity(e).build());
        } catch (ClassNotFoundException e) {
            throw new WebApplicationException(Response.status(500).entity(e).build());
        }

    }

    public Response getResources(@PathParam("name") String resourceId, @QueryParam("filter") String filter, @QueryParam("expand") String expand) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
