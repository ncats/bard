package gov.nih.ncgc.bard.rest;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import gov.nih.ncgc.bard.entity.BardLinkedEntity;
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
public class BARDTargetResource implements IBARDResource {

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
        msg.append("/v1/targets/" + BARDConstants.API_EXTRA_PARAM_SPEC + "\n");
        return msg.toString();
    }

    @GET
    @Produces("text/plain")
    @Path("/_count")
    public String count(@QueryParam("filter") String filter) {
        DBUtils db = new DBUtils();
        try {
            if (filter == null)
                return String.valueOf(db.getEntityCount(ProteinTarget.class));
            else return String.valueOf(db.searchForEntity(filter, -1, -1, ProteinTarget.class).size());
        } catch (SQLException e) {
            throw new WebApplicationException(e, 500);
        } catch (IOException e) {
            throw new WebApplicationException(e, 500);
        }
    }

    @GET
    public Response getResources(@QueryParam("filter") String filter,
                                 @QueryParam("expand") String expand,
                                 @QueryParam("skip") Integer skip,
                                 @QueryParam("top") Integer top) {
        DBUtils db = new DBUtils();

        if (skip == null) skip = -1;
        if (top == null) top = -1;

        boolean expandEntries = false;
        if (expand != null && (expand.toLowerCase().equals("true") || expand.toLowerCase().equals("yes")))
            expandEntries = true;

        try {
            String linkString = null;
            if (filter == null) {
                if ((top == -1)) { // top was not specified, so we start from the beginning
                    top = BARDConstants.MAX_COMPOUND_COUNT;
                }
                if (skip == -1) skip = 0;
                String expandClause = "expand=false";
                if (expandEntries) expandClause = "expand=true";
                if (skip + top <= db.getEntityCount(ProteinTarget.class))
                    linkString = BARDConstants.API_BASE + "/targets?skip=" + (skip + top) + "&top=" + top + "&" + expandClause;
            }

            List<ProteinTarget> targets = db.searchForEntity(filter, skip, top, ProteinTarget.class);
            db.closeConnection();
            if (expandEntries) {
                BardLinkedEntity linkedEntity = new BardLinkedEntity(targets, linkString);
                return Response.ok(Util.toJson(linkedEntity), MediaType.APPLICATION_JSON).build();
            } else {
                List<String> links = new ArrayList<String>();
                for (ProteinTarget a : targets) links.add(a.getResourcePath());
                BardLinkedEntity linkedEntity = new BardLinkedEntity(links, linkString);
                return Response.ok(Util.toJson(linkedEntity), MediaType.APPLICATION_JSON).build();
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
            db.closeConnection();
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
            db.closeConnection();
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
            Response response;
            if (expandEntries) {
                String json = Util.toJson(pubs);
                response = Response.ok(json, MediaType.APPLICATION_JSON).build();
            } else {
                List<String> links = new ArrayList<String>();
                for (Publication pub : pubs)
                    links.add(pub.getResourcePath());
                String json = Util.toJson(links);
                response = Response.ok(json, MediaType.APPLICATION_JSON).build();
            }
            db.closeConnection();
            return response;
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