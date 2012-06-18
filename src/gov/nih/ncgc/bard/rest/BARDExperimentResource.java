package gov.nih.ncgc.bard.rest;

import gov.nih.ncgc.bard.entity.BardLinkedEntity;
import gov.nih.ncgc.bard.entity.Compound;
import gov.nih.ncgc.bard.entity.Experiment;
import gov.nih.ncgc.bard.entity.Substance;
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
@Path("/v1/experiments")
public class BARDExperimentResource implements IBARDResource {

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
        StringBuilder msg = new StringBuilder("Returns experiment information\n\nAvailable resources:\n");
        List<String> paths = Util.getResourcePaths(this.getClass());
        for (String path : paths) msg.append(path).append("\n");
        msg.append("/v1/experiments/" + BARDConstants.API_EXTRA_PARAM_SPEC + "\n");
        return msg.toString();

    }

    @GET
    @Produces("text/plain")
    @Path("/_count")
    public String count(@QueryParam("filter") String filter) {
        DBUtils db = new DBUtils();
        try {
            if (filter == null) {
                int n = db.getExperimentCount();
                return String.valueOf(n);
            } else {
                List<Experiment> experiments = db.searchForExperiment(filter);
                return String.valueOf(experiments.size());
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
                List<Long> ids = db.getExperimentIds();
                if (!expandEntries) {
                    List<String> links = new ArrayList<String>();
                    for (Long id : ids) links.add(BARDConstants.API_BASE + "/experiments/" + id);
                    return Response.ok(Util.toJson(links), MediaType.APPLICATION_JSON).build();
                } else {
                    List<Experiment> experiments = new ArrayList<Experiment>();
                    for (Long id : ids) experiments.add(db.getExperimentByExptId(id));
                    return Response.ok(Util.toJson(experiments), MediaType.APPLICATION_JSON).build();
                }
            } else {
                List<Experiment> experiments = db.searchForExperiment(filter);
                if (expandEntries) {
                    String json = Util.toJson(experiments);
                    return Response.ok(json, MediaType.APPLICATION_JSON).build();
                } else {
                    List<String> links = new ArrayList<String>();
                    for (Experiment a : experiments) links.add(a.getResourcePath());
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
    @Path("/{eid}")
    public Response getResources(@PathParam("eid") String resourceId, @QueryParam("filter") String filter, @QueryParam("expand") String expand) {
        DBUtils db = new DBUtils();
        Experiment experiment;
        try {
            experiment = db.getExperimentByExptId(Long.valueOf(resourceId));
            if (experiment.getExptId() == null) throw new WebApplicationException(404);
            String json = Util.toJson(experiment);
            return Response.ok(json, MediaType.APPLICATION_JSON).build();
        } catch (SQLException e) {
            throw new WebApplicationException(e, 500);
        } catch (IOException e) {
            throw new WebApplicationException(e, 500);
        }
    }

    // TODO right now, we don't support filtering on compounds
    @GET
    @Path("/{eid}/compounds")
    public Response getAssayCompounds(@PathParam("eid") String resourceId,
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
            Experiment experiment = db.getExperimentByExptId(Long.valueOf(resourceId));

            // set up skip and top params
            if (experiment.getSubstances() > BARDConstants.MAX_COMPOUND_COUNT) {
                if ((top == -1)) { // top was not specified, so we start from the beginning
                    top = BARDConstants.MAX_COMPOUND_COUNT;
                }
                if (skip == -1) skip = 0;
                String expandClause = "expand=false";
                if (expandEntries) expandClause = "expand=true";
                if (skip + top <= experiment.getSubstances())
                    linkString = BARDConstants.API_BASE + "/experiments/" + resourceId + "/compounds?skip=" + (skip + top) + "&top=" + top + "&" + expandClause;
            }

            if (types.contains(BARDConstants.MIME_SMILES)) {

            } else if (types.contains(BARDConstants.MIME_SDF)) {

            } else { // JSON
                String json;
                if (!expandEntries) {
                    List<Long> cids = db.getExperimentCompoundCids(Long.valueOf(resourceId), skip, top);
                    List<String> links = new ArrayList<String>();
                    for (Long cid : cids) links.add((new Compound(cid, null, null)).getResourcePath());

                    BardLinkedEntity linkedEntity = new BardLinkedEntity(links, linkString);
                    json = Util.toJson(linkedEntity);
                } else {
                    List<Compound> compounds = db.getExperimentCompounds(Long.valueOf(resourceId), skip, top);
                    BardLinkedEntity linkedEntity = new BardLinkedEntity(compounds, linkString);
                    json = Util.toJson(linkedEntity);
                }
                db.closeConnection();
                return Response.ok(json, MediaType.APPLICATION_JSON).build();
            }
        } catch (SQLException e) {
            throw new WebApplicationException(e, 500);
        } catch (IOException e) {
            throw new WebApplicationException(e, 500);
        }
        return null;
    }

    @GET
    @Path("/{eid}/substances")
    public Response getAssaySubstances(@PathParam("eid") String resourceId,
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
            Experiment experiemnt = db.getExperimentByExptId(Long.valueOf(resourceId));

            // set up skip and top params
            if (experiemnt.getSubstances() > BARDConstants.MAX_COMPOUND_COUNT) {
                if ((top == -1)) { // top was not specified, so we start from the beginning
                    top = BARDConstants.MAX_COMPOUND_COUNT;
                }
                if (skip == -1) skip = 0;
                String expandClause = "expand=false";
                if (expandEntries) expandClause = "expand=true";
                if (skip + top <= experiemnt.getSubstances())
                    linkString = BARDConstants.API_BASE + "/experiments/" + resourceId + "/substances?skip=" + (skip + top) + "&top=" + top + "&" + expandClause;
            }

            if (types.contains(BARDConstants.MIME_SMILES)) {

            } else if (types.contains(BARDConstants.MIME_SDF)) {

            } else { // JSON
                String json;
                if (!expandEntries) {
                    List<Long> sids = db.getExperimentCompoundSids(Long.valueOf(resourceId), skip, top);
                    List<String> links = new ArrayList<String>();
                    for (Long sid : sids) links.add((new Substance(sid, null)).getResourcePath());

                    BardLinkedEntity linkedEntity = new BardLinkedEntity(links, linkString);
                    json = Util.toJson(linkedEntity);
                } else {
                    List<Compound> compounds = db.getExperimentSubstances(Long.valueOf(resourceId), skip, top);
                    BardLinkedEntity linkedEntity = new BardLinkedEntity(compounds, linkString);
                    json = Util.toJson(linkedEntity);
                }
                db.closeConnection();
                return Response.ok(json, MediaType.APPLICATION_JSON).build();
            }
        } catch (SQLException e) {
            throw new WebApplicationException(e, 500);
        } catch (IOException e) {
            throw new WebApplicationException(e, 500);
        }
        return null;
    }
}