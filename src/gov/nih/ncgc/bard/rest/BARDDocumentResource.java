package gov.nih.ncgc.bard.rest;

import gov.nih.ncgc.bard.entity.BardLinkedEntity;
import gov.nih.ncgc.bard.entity.Publication;
import gov.nih.ncgc.bard.tools.Util;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
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
@Path("/documents")
public class BARDDocumentResource extends BARDResource<Publication> {

    public static final String DATE_FORMAT_NOW = "yyyy-MM-dd HH:mm:ss";
    static final String VERSION = "1.0";

    public Class<Publication> getEntityClass () { return Publication.class; }
    public String getResourceBase () {
        return BARDConstants.API_BASE+"/documents";
    }

    @GET
    @Produces("text/plain")
    @Path("/_info")
    public String info() {
        StringBuilder msg = new StringBuilder("Returns publication information\n\nAvailable resources:\n");
        List<String> paths = Util.getResourcePaths(this.getClass());
        for (String path : paths) msg.append(path).append("\n");
        msg.append("/documents/" + BARDConstants.API_EXTRA_PARAM_SPEC + "\n");
        return msg.toString();
    }

    @GET
    public Response getResources(@QueryParam("filter") String filter,
                                 @QueryParam("expand") String expand,
                                 @QueryParam("skip") Integer skip,
                                 @QueryParam("top") Integer top) {
        // validate skip/top
        if (skip == null && top != null) {
            skip = 0;
        } else if (skip == null) {
            skip = -1;
            top = -1;
        }

        
        Response response = null;
        try {
            String linkString = null;
            if (filter == null) {
                if ((top == -1)) { // top was not specified, so we start from the beginning
                    top = BARDConstants.MAX_COMPOUND_COUNT;
                }
                if (skip == -1) skip = 0;
                String expandClause = "expand=false";
                if (expandEntries(expand)) expandClause = "expand=true";
                if (skip + top <= db.getEntityCount(Publication.class))
                    linkString = BARDConstants.API_BASE + "/documents?skip=" + (skip + top) + "&top=" + top + "&" + expandClause;

                if (countRequested) {
                    int n = db.getEntityCount(Publication.class);
                    db.closeConnection();
                    return Response.ok(String.valueOf(n), MediaType.TEXT_PLAIN).build();
                }
            }

            List<Publication> publications = db.searchForEntity(filter, skip, top, Publication.class);
            if (countRequested)
                response = Response.ok(String.valueOf(publications.size()), MediaType.TEXT_PLAIN).build();
            else {
                if (expandEntries(expand)) {
                    BardLinkedEntity linkedEntity = new BardLinkedEntity(publications, linkString);
                    response = Response.ok(Util.toJson(linkedEntity), MediaType.APPLICATION_JSON).build();
                } else {
                    List<String> links = new ArrayList<String>();
                    for (Publication a : publications) links.add(a.getResourcePath());
                    BardLinkedEntity linkedEntity = new BardLinkedEntity(links, linkString);
                    response = Response.ok(Util.toJson(linkedEntity), MediaType.APPLICATION_JSON).build();
                }
            }

            db.closeConnection();
            return response;
        } catch (SQLException e) {
            throw new WebApplicationException(e, 500);
        } catch (IOException e) {
            throw new WebApplicationException(e, 500);
        }
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

        
        try {
            Publication p = db.getPublicationByPmid(Long.parseLong(resourceId));
            if (countRequested && p != null) return Response.ok("1", MediaType.TEXT_PLAIN).build();
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
        
        try {
            Publication p = db.getPublicationByDoi(resourceId);
            if (countRequested && p != null) return Response.ok("1", MediaType.TEXT_PLAIN).build();
            String json = Util.toJson(p);
            return Response.ok(json, MediaType.APPLICATION_JSON).build();
        } catch (SQLException e) {
            throw new WebApplicationException(500);
        } catch (IOException e) {
            throw new WebApplicationException(500);
        }
    }
}