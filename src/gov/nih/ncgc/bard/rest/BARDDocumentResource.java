package gov.nih.ncgc.bard.rest;

import gov.nih.ncgc.bard.entity.Publication;
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
@Path("/v1/documents")
public class BARDDocumentResource implements IBARDResource {

    public static final String DATE_FORMAT_NOW = "yyyy-MM-dd HH:mm:ss";
    static final String VERSION = "1.0";

    @Context
    ServletContext servletContext;
    @Context
    HttpServletRequest httpServletRequest;

    @GET
    @Produces("text/plain")
    @Path("/_info")
    public String info() {
        StringBuilder msg = new StringBuilder("Returns publication information\n\nAvailable resources:\n");
        List<String> paths = Util.getResourcePaths(this.getClass());
        for (String path : paths) msg.append(path).append("\n");
        msg.append("/v1/documents/" + BARDConstants.API_EXTRA_PARAM_SPEC + "\n");
        return msg.toString();
    }

    /**
     * Return a count of (possibly filtered) instances of a given resource.
     *
     * @param filter A query filter or null
     * @return the number of instances
     */
    @GET
    @Produces("text/plain")
    @Path("/_count")
    public String count(@QueryParam("filter") String filter) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @GET
    public Response getResources(@QueryParam("filter") String filter,
                                 @QueryParam("expand") String expand,
                                 @QueryParam("skip") Integer skip,
                                 @QueryParam("top") Integer top) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @GET
    @Path("/{pmid}")
    public Response getResources(@PathParam("pmid") String resourceId, @QueryParam("filter") String filter, @QueryParam("expand") String expand) {
        // check that we have a valid pmid (must be a number)
        try {
            Long.parseLong(resourceId);
        } catch (NumberFormatException e) {
            throw new WebApplicationException(400);
        }

        DBUtils db = new DBUtils();
        try {
            Publication p = db.getPublicationByPmid(Long.parseLong(resourceId));
            String json = Util.toJson(p);
            return Response.ok(json, MediaType.APPLICATION_JSON).build();
        } catch (SQLException e) {
            throw new WebApplicationException(500);
        } catch (IOException e) {
            throw new WebApplicationException(500);
        }
    }

    @GET
    @Path("/doi/{doi}")
    public Response getResourcesByDoi(@PathParam("doi") String resourceId, @QueryParam("filter") String filter, @QueryParam("expand") String expand) {
        DBUtils db = new DBUtils();
        try {
            Publication p = db.getPublicationByDoi(resourceId);
            String json = Util.toJson(p);
            return Response.ok(json, MediaType.APPLICATION_JSON).build();
        } catch (SQLException e) {
            throw new WebApplicationException(500);
        } catch (IOException e) {
            throw new WebApplicationException(500);
        }
    }

}