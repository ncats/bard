package gov.nih.ncgc.bard.rest;

import gov.nih.ncgc.bard.entity.Compound;
import gov.nih.ncgc.bard.tools.DBUtils;
import gov.nih.ncgc.bard.tools.Util;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/**
 * Prototype of MLBD REST resources.
 * <p/>
 * This is mainly to explore the use of Jersey for presenting REST
 * services for the MLBD
 *
 * @author Rajarshi Guha
 */
@Path("/v1/compounds")
public class MLBDCompoundResource implements IMLBDResource {

    public static final String DATE_FORMAT_NOW = "yyyy-MM-dd HH:mm:ss";
    static final String VERSION = "1.0";

    @Context
    ServletContext servletContext;
    @Context
    HttpServletRequest httpServletRequest;

    @GET
    @Produces("text/plain")
    public String info() {
        StringBuilder msg = new StringBuilder("Returns compound information\n\nAvailable resources:\n");
        List<String> paths = Util.getResourcePaths(this.getClass());
        for (String path : paths) msg.append(path).append("\n");
        return msg.toString();
    }

    public Response getResources(@QueryParam("filter") String filter, @QueryParam("search") String search, @QueryParam("expand") String expand) {
        return getResources(null, filter, search, expand);
    }

    @GET
    @Path("/cid/{cid}")
    public Response getResources(@PathParam("cid") String resourceId, @QueryParam("filter") String filter, @QueryParam("search") String search, @QueryParam("expand") String expand) {
        DBUtils db = new DBUtils();
        Compound compound = null;
        try {
            compound = db.getCompoundByCid(Long.parseLong(resourceId));
            String json = compound.toJson();
            return Response.ok(json, MediaType.APPLICATION_JSON).build();
        } catch (IOException e) {
            throw new WebApplicationException(e, 500);
        } catch (SQLException e) {
            throw new WebApplicationException(e, 500);
        }
    }

    @GET
    @Path("/probeid/{pid}")
    public Response getCompoundByProbeid(@PathParam("pid") String resourceId, @QueryParam("filter") String filter, @QueryParam("search") String search, @QueryParam("expand") String expand) {
        DBUtils db = new DBUtils();
        Compound compound = null;
        try {
            compound = db.getCompoundByCid(Long.parseLong(resourceId));
            String json = compound.toJson();
            return Response.ok(json, MediaType.APPLICATION_JSON).build();
        } catch (IOException e) {
            throw new WebApplicationException(e, 500);
        } catch (SQLException e) {
            throw new WebApplicationException(e, 500);
        }
    }

}