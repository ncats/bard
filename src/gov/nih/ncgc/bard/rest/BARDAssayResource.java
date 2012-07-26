package gov.nih.ncgc.bard.rest;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import gov.nih.ncgc.bard.entity.Assay;
import gov.nih.ncgc.bard.entity.Experiment;
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
import javax.ws.rs.core.UriBuilder;
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
        String ret;
        try {
            if (filter == null) {
                int n = db.getAssayCount().size();
                ret = String.valueOf(n);
            } else {
                List<Assay> assays = db.searchForAssay(filter);
                ret = String.valueOf(assays.size());
            }
            db.closeConnection();
            return ret;
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
        Response response;
        try {

            if (filter == null) {
                List<Long> ids = db.getAssayCount();
                if (!expandEntries) {
                    List<String> links = new ArrayList<String>();
                    for (Long id : ids) links.add(BARDConstants.API_BASE + "/assays/" + id);
                    response = Response.ok(Util.toJson(links), MediaType.APPLICATION_JSON).build();
                } else {
                    List<Assay> assays = new ArrayList<Assay>();
                    for (Long id : ids) assays.add(db.getAssayByAid(id));
                    response = Response.ok(Util.toJson(assays), MediaType.APPLICATION_JSON).build();
                }
            } else {
                List<Assay> assays = db.searchForAssay(filter);
                if (expandEntries) {
                    String json = Util.toJson(assays);
                    response = Response.ok(json, MediaType.APPLICATION_JSON).build();
                } else {
                    List<String> links = new ArrayList<String>();
                    for (Assay a : assays) links.add(a.getResourcePath());
                    String json = Util.toJson(links);
                    response = Response.ok(json, MediaType.APPLICATION_JSON).build();
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
    @Path("/{aid}")
    public Response getResources(@PathParam("aid") String resourceId, @QueryParam("filter") String filter, @QueryParam("expand") String expand) {
        DBUtils db = new DBUtils();
        Assay a = null;
        try {
            a = db.getAssayByAid(Long.valueOf(resourceId));
            if (a.getAid() == null) throw new WebApplicationException(404);
            String json = Util.toJson(a);
            db.closeConnection();
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
        Response response;
        try {
            targets = db.getAssayTargets(Long.valueOf(resourceId));
            db.closeConnection();
            if (expandEntries) {
                String json = Util.toJson(targets);
                response = Response.ok(json, MediaType.APPLICATION_JSON).build();
            } else {
                List<String> links = new ArrayList<String>();
                for (ProteinTarget t : targets)
                    links.add(t.getResourcePath());
                String json = Util.toJson(links);
                response = Response.ok(json, MediaType.APPLICATION_JSON).build();
            }
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
    @Path("/{aid}/publications")
    public Response getAssayPublications(@PathParam("aid") String resourceId, @QueryParam("filter") String filter, @QueryParam("expand") String expand) {
        boolean expandEntries = false;
        if (expand != null && (expand.toLowerCase().equals("true") || expand.toLowerCase().equals("yes")))
            expandEntries = true;

        DBUtils db = new DBUtils();
        List<Publication> targets = null;
        try {
            targets = db.getAssayPublications(Long.valueOf(resourceId));
            db.closeConnection();
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

    @GET
    @Path("/{aid}/experiments")
    public Response getAssayExperiments(@PathParam("aid") String resourceId, @QueryParam("filter") String filter, @QueryParam("expand") String expand) {
        boolean expandEntries = false;
        if (expand != null && (expand.toLowerCase().equals("true") || expand.toLowerCase().equals("yes")))
            expandEntries = true;

        DBUtils db = new DBUtils();
        List<Experiment> experiments = null;
        try {
            experiments = db.getExperimentByAssayId(Long.valueOf(resourceId));
            db.closeConnection();
            if (expandEntries) {
                String json = Util.toJson(experiments);
                return Response.ok(json, MediaType.APPLICATION_JSON).build();
            } else {
                List<String> links = new ArrayList<String>();
                for (Experiment experiment : experiments)
                    links.add(experiment.getResourcePath());
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
    @Path("/{aid}/experiments/{eid}")
    public Response getAssayExperiment(@PathParam("aid") String aid,
                                       @PathParam("eid") String eid,
                                       @QueryParam("filter") String filter, @QueryParam("expand") String expand) {
        Experiment e = new Experiment();
        e.setExptId(Long.parseLong(eid));
        UriBuilder ub = UriBuilder.fromUri("/v1/experiments/" + eid);
        if (filter != null) ub.queryParam("filter", filter);
        if (expand != null) ub.queryParam("name", expand);
        return Response.temporaryRedirect(ub.build()).build();
    }
}