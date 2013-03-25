package gov.nih.ncgc.bard.rest;

import gov.nih.ncgc.bard.entity.Assay;
import gov.nih.ncgc.bard.entity.BardLinkedEntity;
import gov.nih.ncgc.bard.entity.ProteinTarget;
import gov.nih.ncgc.bard.entity.Publication;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.sun.jersey.api.NotFoundException;
import gov.nih.ncgc.bard.entity.*;

import gov.nih.ncgc.bard.tools.DBUtils;
import gov.nih.ncgc.bard.tools.Util;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;

/**
 * Prototype of MLBD REST resources.
 * <p/>
 * This is mainly to explore the use of Jersey for presenting REST
 * services for the MLBD
 *
 * @author Rajarshi Guha
 */
@Path("/targets")
public class BARDTargetResource extends BARDResource<ProteinTarget> {

    public static final String DATE_FORMAT_NOW = "yyyy-MM-dd HH:mm:ss";
    static final String VERSION = "1.0";

    @Context
    ServletContext servletContext;
    @Context
    HttpServletRequest httpServletRequest;

    public Class<ProteinTarget> getEntityClass () { 
        return ProteinTarget.class; 
    }
    public String getResourceBase () {
        return BARDConstants.API_BASE+"/targets";
    }

    @GET
    @Produces("text/plain")
    @Path("/_info")
    public String info() {
        StringBuilder msg = new StringBuilder("Returns protein target information\n\nAvailable resources:\n");
        List<String> paths = Util.getResourcePaths(this.getClass());
        for (String path : paths) msg.append(path).append("\n");
        msg.append("/targets/" + BARDConstants.API_EXTRA_PARAM_SPEC + "\n");
        return msg.toString();
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
                if (countRequested)
                    return Response.ok(String.valueOf(db.getEntityCount(ProteinTarget.class)), MediaType.TEXT_PLAIN).build();
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

            if (countRequested) return Response.ok(String.valueOf(targets.size()), MediaType.TEXT_PLAIN).build();
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
            String json;
            if (countRequested) json = "1";
            else {
                p = db.getProteinTargetByAccession(resourceId);
                db.closeConnection();
                if (p.getAcc() == null) throw new WebApplicationException(404);
                json = p.toJson();
            }
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
        String json;
        try {
            if (countRequested) json = "1";
            else {
                p = db.getProteinTargetByGeneid(Long.parseLong(resourceId));
                db.closeConnection();
                if (p.getAcc() == null) throw new WebApplicationException(404);
                json = Util.toJson(p);
            }
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
            if (countRequested) response = Response.ok(String.valueOf(pubs.size()), MediaType.TEXT_PLAIN).build();
            else if (expandEntries) {
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

    @GET
    @Path("/accession/{acc}/assays")
    public Response getExperimentsForTarget(@PathParam("acc") String acc,
                                            @QueryParam("expand") String expand) {
        boolean expandEntries = false;
        if (expand != null && (expand.toLowerCase().equals("true") || expand.toLowerCase().equals("yes")))
            expandEntries = true;

        DBUtils db = new DBUtils();
        List<Assay> assays;
        try {
            assays = db.getAssaysByTargetAccession(acc);
            Response response;
            if (countRequested) response = Response.ok(String.valueOf(assays.size()), MediaType.TEXT_PLAIN).build();
            else if (expandEntries) {
                String json = Util.toJson(assays);
                response = Response.ok(json, MediaType.APPLICATION_JSON).build();
            } else {
                List<String> links = new ArrayList<String>();
                for (Assay assay : assays)
                    links.add(assay.getResourcePath());
                String json = Util.toJson(links);
                response = Response.ok(json, MediaType.APPLICATION_JSON).build();
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
    @Path("/accession/{acc}/classification/{source}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getClassificationsForAccession(@PathParam("source") String source,
                                                   @PathParam("acc") String acc) throws SQLException, IOException {
        List<TargetClassification> classes = null;
        DBUtils db = new DBUtils();
        if (source.toLowerCase().equals("panther")) {
            classes = db.getPantherClassesForAccession(acc);
        }
        db.closeConnection();
        if (classes == null)
            throw new NotFoundException("No classifications for " + acc + " in the " + source + " hierarchy");
        if (countRequested) return Response.ok(String.valueOf(classes.size())).type(MediaType.TEXT_PLAIN_TYPE).build();
        else return Response.ok(Util.toJson(classes)).type(MediaType.APPLICATION_JSON_TYPE).build();
    }

    @GET
    @Path("/classification/{source}/{clsid}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAccessionsForClassification(@PathParam("source") String source,
                                                   @PathParam("clsid") String clsid) throws SQLException, IOException {
        List<ProteinTarget> targets = null;
        DBUtils db = new DBUtils();
        if (source.toLowerCase().equals("panther")) {
            targets = db.getProteinTargetsForPantherClassification(clsid);
        }
        db.closeConnection();
        if (targets == null)
            throw new NotFoundException("No protein targets for " + clsid + " in the " + source + " hierarchy");
        if (countRequested) return Response.ok(String.valueOf(targets.size())).type(MediaType.TEXT_PLAIN_TYPE).build();
        else return Response.ok(Util.toJson(targets)).type(MediaType.APPLICATION_JSON_TYPE).build();
    }


    @GET
    @Path("/geneid/{geneid}/assays")
    public Response getExperimentsForTargetByGeneid(@PathParam("geneid") Long geneid,
                                                    @QueryParam("expand") String expand) {
        boolean expandEntries = false;
        if (expand != null && (expand.toLowerCase().equals("true") || expand.toLowerCase().equals("yes")))
            expandEntries = true;

        DBUtils db = new DBUtils();
        List<Assay> assays;
        try {
            assays = db.getAssaysByTargetGeneid(geneid);
            Response response;
            if (countRequested) response = Response.ok(String.valueOf(assays.size()), MediaType.TEXT_PLAIN).build();
            else if (expandEntries) {
                String json = Util.toJson(assays);
                response = Response.ok(json, MediaType.APPLICATION_JSON).build();
            } else {
                List<String> links = new ArrayList<String>();
                for (Assay assay : assays)
                    links.add(assay.getResourcePath());
                String json = Util.toJson(links);
                response = Response.ok(json, MediaType.APPLICATION_JSON).build();
            }
            db.closeConnection();
            return response;
        } catch (SQLException e) {
            throw new WebApplicationException(e, 500);
        } catch (IOException e) {
            throw new WebApplicationException(e, 500);
        }
    }

}