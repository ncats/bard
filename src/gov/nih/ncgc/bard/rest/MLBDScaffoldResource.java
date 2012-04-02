package gov.nih.ncgc.bard.rest;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

/**
 * Prototype of MLBD REST resources.
 * <p/>
 * This is mainly to explore the use of Jersey for presenting REST
 * services for the MLBD
 *
 * @author Rajarshi Guha
 */
@Path("/v1/scaffolds")
public class MLBDScaffoldResource implements IMLBDResource {

    public static final String DATE_FORMAT_NOW = "yyyy-MM-dd HH:mm:ss";
    static final String VERSION = "1.0";

    @Context
    ServletContext servletContext;
    @Context
    HttpServletRequest httpServletRequest;

    @GET
    @Produces("text/plain")
    public String info() {
        return "Return scaffold information";
    }

    @GET
    public Response getResources(@QueryParam("filter") String filter, @QueryParam("search") String search, @QueryParam("expand") String expand) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @GET
    @Path("/{name}")
    public Response getResources(@PathParam("name") String resourceId, @QueryParam("filter") String filter, @QueryParam("search") String search, @QueryParam("expand") String expand) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}