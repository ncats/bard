package gov.nih.ncgc.bard.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import gov.nih.ncgc.bard.entity.BardLinkedEntity;
import gov.nih.ncgc.bard.entity.ETag;
import gov.nih.ncgc.bard.tools.DBUtils;
import gov.nih.ncgc.bard.tools.Util;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * A one line summary.
 *
 * @author Rajarshi Guha
 */
@Path("/etags")
public class BARDEtagResource extends BARDResource<ETag> implements IBARDResource {

    @Override
    public Class<ETag> getEntityClass() {
        return ETag.class;
    }

    public String getResourceBase() {
        return BARDConstants.API_BASE + "/etags";
    }

    @GET
    @Path("/_info")
    public String info() {
        return "Interact with etag data";
    }

    @GET
    public Response getResources(@QueryParam("filter") String filter, @QueryParam("expand") String expand, @QueryParam("skip") Integer skip, @QueryParam("top") Integer top) {
        DBUtils db = new DBUtils();
        Response response = null;

        if (skip == null) skip = -1;
        if (top == null) top = -1;

        boolean expandEntries = false;
        if (expand != null && (expand.toLowerCase().equals("true") || expand.toLowerCase().equals("yes")))
            expandEntries = true;


        try {
            String linkString = null;
            if (filter == null) {
                if (countRequested) {
                    db.closeConnection();
                    return Response.ok(String.valueOf(db.getEntityCount(ETag.class)), MediaType.TEXT_PLAIN).build();
                }

                if ((top == -1)) { // top was not specified, so we start from the beginning
                    top = BARDConstants.MAX_COMPOUND_COUNT;
                }
                if (skip == -1) skip = 0;
                String expandClause = "expand=false";
                if (expandEntries) expandClause = "expand=true";
                if (skip + top <= db.getEntityCount(ETag.class))
                    linkString = BARDConstants.API_BASE + "/etags?skip=" + (skip + top) + "&top=" + top + "&" + expandClause;
            }

            List<ETag> etags = db.searchForEntity(filter, skip, top, ETag.class);
            db.closeConnection();

            if (countRequested) return Response.ok(String.valueOf(etags.size()), MediaType.TEXT_PLAIN).build();
            if (expandEntries) {
                BardLinkedEntity linkedEntity = new BardLinkedEntity(etags, linkString);
                return Response.ok(Util.toJson(linkedEntity), MediaType.APPLICATION_JSON).build();
            } else {
                List<String> links = new ArrayList<String>();
                for (ETag a : etags) links.add(a.getResourcePath());
                BardLinkedEntity linkedEntity = new BardLinkedEntity(links, linkString);
                return Response.ok(Util.toJson(linkedEntity), MediaType.APPLICATION_JSON).build();
            }
        } catch (SQLException e) {
            throw new WebApplicationException(e, 500);
        } catch (IOException e) {
            throw new WebApplicationException(e, 500);
        }
    }

    public Response getResources(@PathParam("name") String resourceId, @QueryParam("filter") String filter, @QueryParam("expand") String expand) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }


    @GET
    @Path("/{etag}/_info")
    public Response getEtagInfo(@PathParam("etag") String etagId) {
        DBUtils db = new DBUtils();
        try {
            ETag etag = db.getEtagByEtagId(etagId);
            if (etag.getName() == null)
                return Response.status(404).entity("No such etag " + etagId).type("text/plain").build();
            return Response.ok(Util.toJson(etag), MediaType.APPLICATION_JSON).build();
        } catch (Exception ex) {
            throw new WebApplicationException(ex, 500);
        } finally {
            try {
                db.closeConnection();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    // recurse through a composite etag - this assumes we want expanded entries
    private JsonNode recurseCompositeEtag(ETag etag, DBUtils db, Integer skip, Integer top) throws SQLException {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode nodes = mapper.createArrayNode();
        for (ETag child : etag.getLinkedTags()) {
            String type = child.getType().substring(child.getType().lastIndexOf(".") + 1).toLowerCase() + "s";
            ObjectNode onode = mapper.createObjectNode();
            onode.put("etag", child.getEtag());
            onode.put("type", type);

            if (!type.equals("etags")) {
                List entities = db.getEntitiesByEtag(child.getEtag(), skip != null ? skip : -1, top != null ? top : -1);
                onode.put("entities", mapper.valueToTree(entities));
            } else {
                JsonNode rnode = recurseCompositeEtag(child, db, skip, top);
                onode.put("entities", rnode);
            }
            nodes.add(onode);
        }
        return nodes;
    }

    @GET
    @Path("/{etag}")
    public Response getEtag(@PathParam("etag") String etagId,
                            @QueryParam("skip") Integer skip,
                            @QueryParam("top") Integer top,
                            @QueryParam("expand") String expand) {
        boolean expandEntries = expand != null && expand.toLowerCase().equals("true");

        DBUtils db = new DBUtils();
        try {
            ETag etag = db.getEtagByEtagId(etagId);
            if (etag.getType().equals(ETag.class.getCanonicalName())) {
                // this is a composite etag, so we just need to deal with linked etags
                ObjectMapper mapper = new ObjectMapper();
                JsonNode nodes;
                if (expandEntries)
                    nodes = recurseCompositeEtag(etag, db, skip, top);
                else {
                    nodes = mapper.createArrayNode();
                    for (ETag child : etag.getLinkedTags()) {
                        String type = child.getType().substring(child.getType().lastIndexOf(".") + 1).toLowerCase() + "s";
                        ObjectNode onode = mapper.createObjectNode();
                        onode.put("etag", child.getEtag());
                        onode.put("type", type);
                        ((ArrayNode) nodes).add(onode);
                    }
                }
                return Response.ok(mapper.writeValueAsString(nodes), MediaType.APPLICATION_JSON).tag(etagId).build();
            } else {
                List entities = db.getEntitiesByEtag
                        (etag.getEtag(), skip != null ? skip : -1,
                                top != null ? top : -1);
                if (entities == null || entities.size() == 0)
                    return Response.status(404).entity("No objects associated with etag " + etagId).type("text/plain").build();
                return Response.ok(Util.toJson(entities), MediaType.APPLICATION_JSON).tag(etagId).build();
            }
        } catch (Exception ex) {
            throw new WebApplicationException(ex, 500);
        } finally {
            try {
                db.closeConnection();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    @GET
    @Path("/{etag}/meta")
    public Response getEtag(@PathParam("etag") String etagId) {
        DBUtils db = new DBUtils();
        try {
            ETag etag = db.getEtagByEtagId(etagId);
            return Response.ok
                    (Util.toJson(etag), MediaType.APPLICATION_JSON).build();
        } catch (Exception ex) {
            throw new WebApplicationException(ex, 500);
        } finally {
            try {
                db.closeConnection();
            } catch (Exception ex) {
            }
        }
    }
}
