package gov.nih.ncgc.bard.rest;

import com.sun.jersey.api.NotFoundException;
import gov.nih.ncgc.bard.entity.BardLinkedEntity;
import gov.nih.ncgc.bard.entity.ETag;
import gov.nih.ncgc.bard.tools.DBUtils;
import gov.nih.ncgc.bard.tools.Util;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
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

    @GET
    @Path("/_info")
    public String info() {
        return "Interact with etag data";
    }

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
                if (countRequested)
                    return Response.ok(String.valueOf(db.getEntityCount(ETag.class)), MediaType.TEXT_PLAIN).build();

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

    @GET
    @Path("/{etag}")
    public Response getEtag(@PathParam("etag") String etagId) {
        DBUtils db = new DBUtils();
        try {
            ETag etag = db.getEtagByEtagId(etagId);
            List entities = db.getEntitiesByEtag(etag.getEtag(), 0, 0);
            if (entities == null || entities.size() == 0)
                throw new NotFoundException("No objects associated with etag " + etagId);
            return Response.ok(Util.toJson(entities), MediaType.APPLICATION_JSON).build();
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
}
