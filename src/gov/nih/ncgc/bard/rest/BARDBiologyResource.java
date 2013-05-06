package gov.nih.ncgc.bard.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.jersey.api.NotFoundException;
import gov.nih.ncgc.bard.entity.BardLinkedEntity;
import gov.nih.ncgc.bard.entity.Biology;
import gov.nih.ncgc.bard.entity.TargetClassification;
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

    @Override
    public Response getResources(@PathParam("name") String resourceId, String filter, String expand) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
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


    @POST
    @Path("/accession/classification/{source}")
    @Consumes("application/x-www-form-urlencoded")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getClassificationsForAccessions(@PathParam("source") String source,
                                                    @FormParam("accs") String accs) throws SQLException, IOException {

        DBUtils db = new DBUtils();
        String[] laccs = accs.split(",");

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode node = mapper.createObjectNode();

        for (String acc : laccs) {
            List<TargetClassification> classes = null;
            if (source.toLowerCase().equals("panther")) {
                classes = db.getPantherClassesForAccession(acc.trim());
            }
            JsonNode classNodes = mapper.valueToTree(classes);
            node.put(acc, classNodes);
        }
        db.closeConnection();
        String json = mapper.writeValueAsString(node);
        return Response.ok(json).type(MediaType.APPLICATION_JSON_TYPE).build();
    }

}