package gov.nih.ncgc.bard.rest;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import gov.nih.ncgc.bard.entity.ProteinTarget;
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
import java.util.ArrayList;
import java.util.List;

/**
 * Prototype of MLBD REST resources.
 * <p/>
 * This is mainly to explore the use of Jersey for presenting REST
 * services for the MLBD
 *
 * @author Rajarshi Guha
 */
@Path("/v1/targets")
public class MLBDTargetResource implements IMLBDResource {

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
        StringBuilder msg = new StringBuilder("Returns protein target information\n\nAvailable resources:\n");
        List<String> paths = Util.getResourcePaths(this.getClass());
        for (String path : paths) msg.append(path).append("\n");
        msg.append("/v1/targets/" + MLBDConstants.API_EXTRA_PARAM_SPEC + "\n");
        return msg.toString();
    }

    @GET
    @Produces("text/plain")
    @Path("/_count")
    public String count(@QueryParam("filter") String filter) {
        DBUtils db = new DBUtils();
        try {
            if (filter == null)
                return String.valueOf(db.getTargetCount());
            else return String.valueOf(db.searchForTargets(filter, -1, -1).size());
        } catch (SQLException e) {
            throw new WebApplicationException(e, 500);
        }
    }

    @GET
    public Response getResources(@QueryParam("filter") String filter,
                                 @QueryParam("expand") String expand,
                                 @QueryParam("skip") Integer skip,
                                 @QueryParam("top") Integer top) {
        if (filter == null) return null;

        // validate skip/top
        if (skip == null && top != null) {
            skip = 0;
        } else if (skip == null) {
            skip = -1;
            top = -1;
        }

        boolean expandEntries = false;
        if (expand != null && (expand.toLowerCase().equals("true") || expand.toLowerCase().equals("yes")))
            expandEntries = true;

        DBUtils db = new DBUtils();
        try {
            List<ProteinTarget> targets = db.searchForTargets(filter, skip, top);
            if (expandEntries) {
                String json = Util.toJson(targets);
                return Response.ok(json, MediaType.APPLICATION_JSON).build();
            } else {
                List<String> links = new ArrayList<String>();
                for (ProteinTarget a : targets) links.add(a.getResourcePath());
                String json = Util.toJson(links);
                return Response.ok(json, MediaType.APPLICATION_JSON).build();
            }
        } catch (SQLException e) {
            throw new WebApplicationException(e, 500);
        } catch (IOException e) {
            throw new WebApplicationException(e, 500);
        }
    }

    @GET
    @Path("/accession/{acc}")
    @Produces("application/json")
    public Response getResources(@PathParam("acc") String resourceId, @QueryParam("filter") String filter, @QueryParam("expand") String expand) {
        DBUtils db = new DBUtils();
        ProteinTarget p;
        try {
            p = db.getProteinTargetByAccession(resourceId);
            if (p.getAcc() == null) throw new WebApplicationException(404);
            String json = p.toJson();
            return Response.ok(json, MediaType.APPLICATION_JSON).build();
        } catch (SQLException e) {
            throw new WebApplicationException(e, 500);
        } catch (IOException e) {
            throw new WebApplicationException(e, 500);
        }
    }

    @GET
    @Path("/geneid/{id}")
    @Produces("application/json")
    public Response getByGeneid(@PathParam("id") String resourceId, @QueryParam("filter") String filter, @QueryParam("search") String search, @QueryParam("expand") String expand) {
        DBUtils db = new DBUtils();
        ProteinTarget p;
        try {
            p = db.getProteinTargetByGeneid(Long.parseLong(resourceId));
            if (p.getAcc() == null) throw new WebApplicationException(404);
            String json = Util.toJson(p);
            return Response.ok(json, MediaType.APPLICATION_JSON).build();
        } catch (SQLException e) {
            throw new WebApplicationException(e, 500);
        } catch (IOException e) {
            throw new WebApplicationException(e, 500);
        }
    }

    @GET
    @Path("/accession/{acc}/publications")
    public Response getTargetPublications(@PathParam("acc") String resourceId,
                                          @QueryParam("filter") String filter,
                                          @QueryParam("search") String search,
                                          @QueryParam("expand") String expand) {
        boolean expandEntries = false;
        if (expand != null && (expand.toLowerCase().equals("true") || expand.toLowerCase().equals("yes")))
            expandEntries = true;

        DBUtils db = new DBUtils();
        List<Publication> pubs = null;
        try {
            pubs = db.getProteinTargetPublications(resourceId);
            if (expandEntries) {
                String json = Util.toJson(pubs);
                return Response.ok(json, MediaType.APPLICATION_JSON).build();
            } else {
                List<String> links = new ArrayList<String>();
                for (Publication pub : pubs)
                    links.add(pub.getResourcePath());
                String json = Util.toJson(links);
                return Response.ok(json, MediaType.APPLICATION_JSON).build();
            }
        } catch (SQLException e) {
            throw new WebApplicationException(e, 500);
        } catch (JsonMappingException e) {
            throw new WebApplicationException(e, 500);
        } catch (JsonGenerationException e) {
            throw new WebApplicationException(e, 500);
        } catch (IOException e) {
            throw new WebApplicationException(e, 500);
        }
    }


}