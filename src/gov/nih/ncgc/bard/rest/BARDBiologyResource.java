package gov.nih.ncgc.bard.rest;

import com.sun.jersey.api.NotFoundException;
import gov.nih.ncgc.bard.entity.BardLinkedEntity;
import gov.nih.ncgc.bard.entity.Biology;
import gov.nih.ncgc.bard.tools.DBUtils;
import gov.nih.ncgc.bard.tools.Util;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
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
@Path("/biology")
public class BARDBiologyResource extends BARDResource<Biology> {

    public static final String DATE_FORMAT_NOW = "yyyy-MM-dd HH:mm:ss";
    static final String VERSION = "1.0";

    @Context
    ServletContext servletContext;
    @Context
    HttpServletRequest httpServletRequest;

    public Class<Biology> getEntityClass() {
        return Biology.class;
    }

    public String getResourceBase() {
        return BARDConstants.API_BASE + "/biology";
    }

    @GET
    @Produces("text/plain")
    @Path("/_info")
    public String info() {
        StringBuilder msg = new StringBuilder("Returns target biology information\n\nAvailable resources:\n");
        List<String> paths = Util.getResourcePaths(this.getClass());
        for (String path : paths) msg.append(path).append("\n");
        msg.append("/targets/" + BARDConstants.API_EXTRA_PARAM_SPEC + "\n");
        return msg.toString();
    }

    /**
     * A paged list of all stored biologies
     *
     * @param filter
     * @param expand
     * @param skip
     * @param top
     * @return
     */
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
                    return Response.ok(String.valueOf(db.getEntityCount(Biology.class)), MediaType.TEXT_PLAIN).build();
                if ((top == -1)) { // top was not specified, so we start from the beginning
                    top = BARDConstants.MAX_COMPOUND_COUNT;
                }
                if (skip == -1) skip = 0;
                String expandClause = "expand=false";
                if (expandEntries) expandClause = "expand=true";
                if (skip + top <= db.getEntityCount(Biology.class))
                    linkString = BARDConstants.API_BASE + "/biology?skip=" + (skip + top) + "&top=" + top + "&" + expandClause;
            }

            List<Biology> targets = db.searchForEntity(filter, skip, top, Biology.class);
            db.closeConnection();

            if (countRequested) return Response.ok(String.valueOf(targets.size()), MediaType.TEXT_PLAIN).build();
            if (expandEntries) {
                BardLinkedEntity linkedEntity = new BardLinkedEntity(targets, linkString);
                return Response.ok(Util.toJson(linkedEntity), MediaType.APPLICATION_JSON).build();
            } else {
                List<String> links = new ArrayList<String>();
                for (Biology a : targets) links.add(a.getResourcePath());
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
    @Path("/types")
    @Produces("application/json")
    public Response getBiologyTypes() {
        Biology.BiologyType[] types = Biology.BiologyType.values();
        List<String> typeStrings = new ArrayList<String>();
        for (Biology.BiologyType type : types) typeStrings.add(type.toString());
        String json = null;
        try {
            json = Util.toJson(typeStrings);
        } catch (IOException e) {
            throw new WebApplicationException(e, 500);
        }
        return Response.ok(json).build();
    }

    @GET
    @Path("/types/{typeName}")
    @Produces("application/json")
    public Response getBiologyByType(@PathParam("typeName") String typeName,
                                     @QueryParam("expand") String expand) {
        DBUtils db = new DBUtils();
        try {
            String json;
            List<Biology> biologies = db.getBiologyByType(typeName);
            db.closeConnection();
            if (biologies.size() == 0)
                throw new NotFoundException("No biology information for " + typeName);
            if (countRequested) json = String.valueOf(biologies.size());
            else if (expandEntries(expand)) {
                json = Util.toJson(biologies);
            } else {
                List<String> links = new ArrayList<String>();
                for (Biology bio : biologies)
                    links.add(bio.getResourcePath());
                json = Util.toJson(links);
            }
            return Response.ok(json, MediaType.APPLICATION_JSON).build();
        } catch (SQLException e) {
            throw new WebApplicationException(e, 500);
        } catch (IOException e) {
            throw new WebApplicationException(e, 500);
        }
    }

    @GET
    @Path("/dict/{dictId}")
    @Produces("application/json")
    public Response getBiologyByDictId(@PathParam("dictId") String dictId,
                                       @QueryParam("expand") String expand) {
        DBUtils db = new DBUtils();
        try {
            String json;
            List<Biology> biologies = db.getBiologyByDictId(dictId);
            db.closeConnection();
            if (biologies.size() == 0)
                throw new NotFoundException("No biology information for dict id " + dictId);
            if (countRequested) json = String.valueOf(biologies.size());
            else if (expandEntries(expand)) {
                json = Util.toJson(biologies);
            } else {
                List<String> links = new ArrayList<String>();
                for (Biology bio : biologies)
                    links.add(bio.getResourcePath());
                json = Util.toJson(links);
            }
            return Response.ok(json, MediaType.APPLICATION_JSON).build();
        } catch (SQLException e) {
            throw new WebApplicationException(e, 500);
        } catch (IOException e) {
            throw new WebApplicationException(e, 500);
        }
    }

    @GET
    @Path("/types/{typeName}/{extId}")
    @Produces("application/json")
    public Response getBiologyByTypeAndExtId(@PathParam("typeName") String typeName,
                                             @PathParam("extId") String extId,
                                             @QueryParam("expand") String expand) {
        DBUtils db = new DBUtils();
        try {
            String json;
            List<Biology> biologies = db.getBiologyByType(typeName, extId);
            db.closeConnection();
            if (biologies.size() == 0)
                throw new NotFoundException("No biology information for " + typeName);
            if (countRequested) json = String.valueOf(biologies.size());
            else if (expandEntries(expand)) {
                json = Util.toJson(biologies);
            } else {
                List<String> links = new ArrayList<String>();
                for (Biology bio : biologies)
                    links.add(bio.getResourcePath());
                json = Util.toJson(links);
            }
            return Response.ok(json, MediaType.APPLICATION_JSON).build();
        } catch (SQLException e) {
            throw new WebApplicationException(e, 500);
        } catch (IOException e) {
            throw new WebApplicationException(e, 500);
        }
    }

    @GET
    @Path("/{entity}/{entityId}")
    @Produces("application/json")
    public Response getBiologyForEntity(@PathParam("entity") String entity,
                                        @PathParam("entityId") int entityId,
                                        @QueryParam("expand") String expand) {
        DBUtils db = new DBUtils();
        try {
            String json;
            List<Biology> biologies = db.getBiologyByEntity(entity, entityId);
            db.closeConnection();
            if (biologies.size() == 0)
                throw new NotFoundException("No biology information for " + entity + " " + entityId);
            if (countRequested) json = String.valueOf(biologies.size());
            else if (expandEntries(expand)) {
                json = Util.toJson(biologies);
            } else {
                List<String> links = new ArrayList<String>();
                for (Biology bio : biologies)
                    links.add(bio.getResourcePath());
                json = Util.toJson(links);
            }
            return Response.ok(json, MediaType.APPLICATION_JSON).build();
        } catch (SQLException e) {
            throw new WebApplicationException(e, 500);
        } catch (IOException e) {
            throw new WebApplicationException(e, 500);
        }
    }

    @GET
    @Path("/{entity}/{entityId}/{typeName}")
    @Produces("application/json")
    public Response getBiologyForEntityAndType(@PathParam("entity") String entity,
                                               @PathParam("entityId") int entityId,
                                               @PathParam("typeName") String typeName,
                                               @QueryParam("expand") String expand) {
        DBUtils db = new DBUtils();
        try {
            String json;
            List<Biology> biologies = db.getBiologyByEntity(entity, entityId);
            db.closeConnection();
            if (biologies.size() == 0)
                throw new NotFoundException("No biology information for " + entity + " " + entityId);

            List<Biology> tmp = new ArrayList<Biology>();
            for (Biology bio : biologies) {
                if (bio.getBiology().equals(typeName)) tmp.add(bio);
            }
            biologies = tmp;

            if (biologies.size() == 0) throw new NotFoundException();

            if (countRequested) json = String.valueOf(biologies.size());
            else if (expandEntries(expand)) {
                json = Util.toJson(biologies);
            } else {
                List<String> links = new ArrayList<String>();
                for (Biology bio : biologies)
                    links.add(bio.getResourcePath());
                json = Util.toJson(links);
            }
            return Response.ok(json, MediaType.APPLICATION_JSON).build();
        } catch (SQLException e) {
            throw new WebApplicationException(e, 500);
        } catch (IOException e) {
            throw new WebApplicationException(e, 500);
        }
    }

    @GET
    @Path("/{bid}")
    @Produces("application/json")
    @Override
    public Response getResources(@PathParam("bid") String resourceId,
                                 @QueryParam("filter") String filter,
                                 @QueryParam("expand") String expand) {
        DBUtils db = new DBUtils();
        try {
            if (resourceId != null && !Util.isNumber(resourceId)) throw new WebApplicationException(400);
            List<Biology> bios = db.getBiologyBySerial(Long.parseLong(resourceId));
            if (bios.size() == 0) throw new NotFoundException();
            String json;
            if (countRequested) json = "1";
            else if (expandEntries(expand)) {
                json = Util.toJson(bios);
            } else {
                List<String> links = new ArrayList<String>();
                for (Biology bio : bios)
                    links.add(bio.getResourcePath());
                json = Util.toJson(links);
            }
            return Response.ok(json, MediaType.APPLICATION_JSON).build();
        } catch (SQLException e) {
            throw new WebApplicationException(e, 500);
        } catch (IOException e) {
            throw new WebApplicationException(e, 500);
        }
    }

    @POST
    @Path("/")
    @Produces("application/json")
    @Consumes("application/x-www-form-urlencoded")
    public Response getResourcesByPost(@FormParam("bids") String bids,
                                       @QueryParam("filter") String filter,
                                       @QueryParam("expand") String expand) {
        DBUtils db = new DBUtils();
        try {
            List<Biology> allBiologies = new ArrayList<Biology>();
            for (String bid : bids.split(",")) {
                bid = bid.trim();
                List<Biology> bios = db.getBiologyBySerial(Long.parseLong(bid));
                if (bios != null) allBiologies.addAll(bios);
            }
            if (allBiologies.size() == 0) throw new NotFoundException();
            String json;
            if (countRequested) json = String.valueOf(allBiologies.size());
            else if (expandEntries(expand)) {
                json = Util.toJson(allBiologies);
            } else {
                List<String> links = new ArrayList<String>();
                for (Biology bio : allBiologies)
                    links.add(bio.getResourcePath());
                json = Util.toJson(links);
            }
            return Response.ok(json, MediaType.APPLICATION_JSON).build();
        } catch (SQLException e) {
            throw new WebApplicationException(e, 500);
        } catch (IOException e) {
            throw new WebApplicationException(e, 500);
        }
    }

}