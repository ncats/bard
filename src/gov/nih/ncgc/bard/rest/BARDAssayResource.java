package gov.nih.ncgc.bard.rest;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import gov.nih.ncgc.bard.entity.Assay;
import gov.nih.ncgc.bard.entity.BardLinkedEntity;
import gov.nih.ncgc.bard.entity.Compound;
import gov.nih.ncgc.bard.entity.Project;
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
import javax.ws.rs.core.HttpHeaders;
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
@Path("/v1/assays")
public class BARDAssayResource implements IBARDResource {

    public static final String DATE_FORMAT_NOW = "yyyy-MM-dd HH:mm:ss";
    static final String VERSION = "1.0";

    @Context
    ServletContext servletContext;
    @Context
    HttpServletRequest httpServletRequest;
    @Context
    HttpHeaders headers;


    @GET
    @Produces("text/plain")
    @Path("/_info")
    public String info() {
        StringBuilder msg = new StringBuilder("Returns assay information\n\nAvailable resources:\n");
        List<String> paths = Util.getResourcePaths(this.getClass());
        for (String path : paths) msg.append(path).append("\n");
        msg.append("/v1/assays/" + BARDConstants.API_EXTRA_PARAM_SPEC + "\n");
        return msg.toString();

    }

    @GET
    @Produces("text/plain")
    @Path("/_count")
    public String count(@QueryParam("filter") String filter) {
        DBUtils db = new DBUtils();
        try {
            if (filter == null) {
                int n = db.getAssayCount().size();
                return String.valueOf(n);
            } else {
                List<Assay> assays = db.searchForAssay(filter);
                return String.valueOf(assays.size());
            }
        } catch (SQLException e) {
            throw new WebApplicationException(e, 500);
        }
    }

    @GET
    public Response getResources(@QueryParam("filter") String filter,
                                 @QueryParam("expand") String expand,
                                 @QueryParam("skip") Integer skip,
                                 @QueryParam("top") Integer top) {
        boolean expandEntries = false;
        if (expand != null && (expand.toLowerCase().equals("true") || expand.toLowerCase().equals("yes")))
            expandEntries = true;

        DBUtils db = new DBUtils();
        try {

            if (filter == null) {
                List<Long> ids = db.getAssayCount();
                if (!expandEntries) {
                    List<String> links = new ArrayList<String>();
                    for (Long id : ids) links.add(BARDConstants.API_BASE + "/assays/" + id);
                    return Response.ok(Util.toJson(links), MediaType.APPLICATION_JSON).build();
                } else {
                    List<Project> projects = new ArrayList<Project>();
                    for (Long id : ids) projects.add(db.getProjectByAid(id));
                    return Response.ok(Util.toJson(projects), MediaType.APPLICATION_JSON).build();
                }
            } else {
                List<Assay> assays = db.searchForAssay(filter);
                if (expandEntries) {
                    String json = Util.toJson(assays);
                    return Response.ok(json, MediaType.APPLICATION_JSON).build();
                } else {
                    List<String> links = new ArrayList<String>();
                    for (Assay a : assays) links.add(a.getResourcePath());
                    String json = Util.toJson(links);
                    return Response.ok(json, MediaType.APPLICATION_JSON).build();
                }
            }
        } catch (SQLException e) {
            throw new WebApplicationException(e, 500);
        } catch (IOException e) {
            throw new WebApplicationException(e, 500);
        }
    }

    @GET
    @Path("/{aid}")
    public Response getResources(@PathParam("aid") String resourceId, @QueryParam("filter") String filter, @QueryParam("expand") String expand) {
        DBUtils db = new DBUtils();
        Assay a = null;
        try {
            a = db.getAssayByAid(Long.valueOf(resourceId));
            if (a.getAid() == null) throw new WebApplicationException(404);
            String json = Util.toJson(a);
            return Response.ok(json, MediaType.APPLICATION_JSON).build();
        } catch (SQLException e) {
            throw new WebApplicationException(e, 500);
        } catch (IOException e) {
            throw new WebApplicationException(e, 500);
        }
    }

    @GET
    @Path("/{aid}/targets")
    public Response getAssayTargets(@PathParam("aid") String resourceId, @QueryParam("filter") String filter, @QueryParam("expand") String expand) {
        boolean expandEntries = false;
        if (expand != null && (expand.toLowerCase().equals("true") || expand.toLowerCase().equals("yes")))
            expandEntries = true;

        DBUtils db = new DBUtils();
        List<ProteinTarget> targets = null;
        try {
            targets = db.getAssayTargets(Long.valueOf(resourceId));
            if (expandEntries) {
                String json = Util.toJson(targets);
                return Response.ok(json, MediaType.APPLICATION_JSON).build();
            } else {
                List<String> links = new ArrayList<String>();
                for (ProteinTarget t : targets)
                    links.add(t.getResourcePath());
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

    @GET
    @Path("/{aid}/publications")
    public Response getAssayPublications(@PathParam("aid") String resourceId, @QueryParam("filter") String filter, @QueryParam("expand") String expand) {
        boolean expandEntries = false;
        if (expand != null && (expand.toLowerCase().equals("true") || expand.toLowerCase().equals("yes")))
            expandEntries = true;

        DBUtils db = new DBUtils();
        List<Publication> targets = null;
        try {
            targets = db.getAssayPublications(Long.valueOf(resourceId));
            if (expandEntries) {
                String json = Util.toJson(targets);
                return Response.ok(json, MediaType.APPLICATION_JSON).build();
            } else {
                List<String> links = new ArrayList<String>();
                for (Publication pub : targets)
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

    // TODO right now, we don't support filtering on compounds
    @GET
    @Path("/{aid}/compounds")
    public Response getAssayCompounds(@PathParam("aid") String resourceId,
                                      @QueryParam("filter") String filter,
                                      @QueryParam("expand") String expand,
                                      @QueryParam("skip") Integer skip,
                                      @QueryParam("top") Integer top) {
        boolean expandEntries = false;
        if (expand != null && (expand.toLowerCase().equals("true") || expand.toLowerCase().equals("yes")))
            expandEntries = true;

        List<MediaType> types = headers.getAcceptableMediaTypes();
        DBUtils db = new DBUtils();
        String linkString = null;

        if (skip == null) skip = -1;
        if (top == null) top = -1;

        try {
            Assay a = db.getAssayByAid(Long.valueOf(resourceId));

            // set up skip and top params
            if (a.getSamples() > BARDConstants.MAX_COMPOUND_COUNT) {
                if ((top == -1)) { // top was not specified, so we start from the beginning
                    top = BARDConstants.MAX_COMPOUND_COUNT;
                }
                if (skip == -1) skip = 0;
                String expandClause = "expand=false";
                if (expandEntries) expandClause = "expand=true";
                linkString = BARDConstants.API_BASE + "/assays/" + resourceId + "/compounds?skip=" + (skip + top) + "&top=" + top + "&" + expandClause;
            }

            if (types.contains(BARDConstants.MIME_SMILES)) {

            } else if (types.contains(BARDConstants.MIME_SDF)) {

            } else { // JSON
                if (!expandEntries) {
                    List<Long> cids = db.getAssayCompoundCids(Long.valueOf(resourceId), skip, top);
                    List<String> links = new ArrayList<String>();
                    for (Long cid : cids) links.add((new Compound(cid, null, null)).getResourcePath());

                    BardLinkedEntity linkedEntity = new BardLinkedEntity(links, linkString);
                    String json = Util.toJson(linkedEntity);
                    return Response.ok(json, MediaType.APPLICATION_JSON).build();
                } else {
                    List<Compound> compounds = db.getAssayCompounds(Long.valueOf(resourceId), skip, top);
                    BardLinkedEntity linkedEntity = new BardLinkedEntity(compounds, linkString);
                    String json = Util.toJson(linkedEntity);
                    return Response.ok(json, MediaType.APPLICATION_JSON).build();
                }
            }
        } catch (SQLException e) {
            throw new WebApplicationException(e, 500);
        } catch (IOException e) {
            throw new WebApplicationException(e, 500);
        }
        return null;
    }
}