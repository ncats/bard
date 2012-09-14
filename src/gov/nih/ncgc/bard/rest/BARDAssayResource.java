package gov.nih.ncgc.bard.rest;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import gov.nih.ncgc.bard.capextract.CAPAssayAnnotation;
import gov.nih.ncgc.bard.capextract.CAPDictionary;
import gov.nih.ncgc.bard.capextract.CAPDictionaryElement;
import gov.nih.ncgc.bard.entity.Assay;
import gov.nih.ncgc.bard.entity.BardLinkedEntity;
import gov.nih.ncgc.bard.entity.Experiment;
import gov.nih.ncgc.bard.entity.Project;
import gov.nih.ncgc.bard.entity.ProteinTarget;
import gov.nih.ncgc.bard.entity.Publication;
import gov.nih.ncgc.bard.tools.DBUtils;
import gov.nih.ncgc.bard.tools.Util;
import gov.nih.ncgc.util.functional.Functional;
import gov.nih.ncgc.util.functional.IApplyFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
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
import java.math.BigInteger;
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
@Path("/assays")
public class BARDAssayResource extends BARDResource<Assay> {
    Logger log;

    public static final String DATE_FORMAT_NOW = "yyyy-MM-dd HH:mm:ss";
    static final String VERSION = "1.0";

    @Context
    ServletContext servletContext;
    @Context
    HttpServletRequest httpServletRequest;
    @Context
    HttpHeaders headers;

    public BARDAssayResource() {
        log = LoggerFactory.getLogger(this.getClass());
    }

    public Class<Assay> getEntityClass () { return Assay.class; }

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
            return ret;
        } catch (SQLException e) {
            throw new WebApplicationException(Response.status(500).entity(e.getMessage()).build());
        } finally {
            try {
                db.closeConnection();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
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

        if (skip == null) skip = -1;
        if (top == null) top = -1;

        String linkString = null;
        DBUtils db = new DBUtils();
        Response response;
        try {

            if (filter == null) { //  page all assays
                if (countRequested) {
                    return Response.ok(String.valueOf(db.getEntityCount(Assay.class)), MediaType.TEXT_PLAIN).build();
                }
                if ((top == -1)) { // top was not specified, so we start from the beginning
                    top = BARDConstants.MAX_COMPOUND_COUNT;
                }
                if (skip == -1) skip = 0;
                String expandClause = "expand=false";
                if (expandEntries) expandClause = "expand=true";
                if (skip + top <= db.getEntityCount(Assay.class))
                    linkString = BARDConstants.API_BASE + "/assays?skip=" + (skip + top) + "&top=" + top + "&" + expandClause;
            }
            log.info("Request had skip = " + skip + ", top = " + top + ", filter = " + filter);

            List<Assay> assays = db.searchForEntity(filter, skip, top, Assay.class);

            if (countRequested) return Response.ok(String.valueOf(assays.size()), MediaType.TEXT_PLAIN).build();
            if (expandEntries) {
                BardLinkedEntity linkedEntity = new BardLinkedEntity(assays, linkString);
                return Response.ok(Util.toJson(linkedEntity), MediaType.APPLICATION_JSON).build();
            } else {
                List<String> links = new ArrayList<String>();
                for (Assay a : assays) links.add(a.getResourcePath());
                BardLinkedEntity linkedEntity = new BardLinkedEntity(links, linkString);
                return Response.ok(Util.toJson(linkedEntity), MediaType.APPLICATION_JSON).build();
            }

        } catch (SQLException e) {
            throw new WebApplicationException(Response.status(500).entity(e.getMessage()).build());
        } catch (IOException e) {
            throw new WebApplicationException(Response.status(500).entity(e.getMessage()).build());
        } finally {
            try {
                db.closeConnection();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }

    JsonNode getExpandedJson(Assay a, Long aid, DBUtils db) throws SQLException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode t = mapper.valueToTree(a);

        List<Experiment> expts = db.getExperimentByAssayId(aid);
        ArrayNode an = mapper.createArrayNode();
        for (Experiment e : expts) {
            ObjectNode on = mapper.valueToTree(e);
            an.add(on);
        }
        ((ObjectNode) t).put("experiments", an);

        List<Project> projs = db.getProjectByAssayId(aid);
        an = mapper.createArrayNode();
        for (Project e : projs) {
            ObjectNode on = mapper.valueToTree(e);
            an.add(on);
        }
        ((ObjectNode) t).put("projects", an);

        return t;
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
            if (expand != null && expand.toLowerCase().trim().equals("true")) { // expand experiment and project entries 
                json = getExpandedJson(a, Long.parseLong(resourceId), db).toString();
            }
            return Response.ok(json, MediaType.APPLICATION_JSON).build();
        } catch (SQLException e) {
            throw new WebApplicationException(Response.status(500).entity(e.getMessage()).build());
        } catch (IOException e) {
            throw new WebApplicationException(Response.status(500).entity(e.getMessage()).build());
        } finally {
            try {
                db.closeConnection();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }

    @POST
    @Path("/")
    @Consumes("application/x-www-form-urlencoded")
    public Response getResources(@FormParam("ids") String aids, @QueryParam("expand") String expand) {
        if (aids == null)
            throw new WebApplicationException(new Exception("POST request must specify the aids form parameter, which should be a comma separated string of assay IDs"), 400);
        DBUtils db = new DBUtils();
        try {
            // we'll asssume an ID list if we're being called via POST
            String[] s = aids.split(",");
            Long[] ids = new Long[s.length];
            for (int i = 0; i < s.length; i++) ids[i] = Long.parseLong(s[i].trim());

            List<Assay> assays = db.getAssays(ids);
            if (countRequested) return Response.ok(String.valueOf(assays.size()), MediaType.TEXT_PLAIN).build();
            db.closeConnection();

            String json;
            if (expand == null || expand.toLowerCase().equals("false")) {
                List<String> links = new ArrayList<String>();
                for (Assay ap : assays) links.add(ap.getResourcePath());
                json = Util.toJson(links);
            } else {
                ObjectMapper mapper = new ObjectMapper();
                ArrayNode an = mapper.createArrayNode();
                for (Assay a : assays) {
                    an.add(getExpandedJson(a, a.getAid(), db));
                }
                json = an.toString();
            }
            return Response.ok(json, MediaType.APPLICATION_JSON).build();
        } catch (SQLException e) {
            throw new WebApplicationException(Response.status(500).entity(e.getMessage()).build());
        } catch (IOException e) {
            throw new WebApplicationException(Response.status(500).entity(e.getMessage()).build());
        }
    }


    @GET
    @Path("/{aid}/annotations")
    public Response getAnnotations(@PathParam("aid") Long resourceId, @QueryParam("filter") String filter, @QueryParam("expand") String expand) throws ClassNotFoundException, IOException, SQLException {
        DBUtils db = new DBUtils();
        List<CAPAssayAnnotation> a;
        CAPDictionary dict = db.getCAPDictionary();
        try {
            a = db.getAssayAnnotations(resourceId);
            if (a == null) throw new WebApplicationException(404);
            CAPDictionaryElement node;
            for (CAPAssayAnnotation as : a) {
                if (as.key != null) {
                    node = dict.getNode(new BigInteger(as.key));
                    as.key = node != null ? node.getLabel() : as.key;
                }
                if (as.value != null) {
                    node = dict.getNode(new BigInteger(as.value));
                    as.value = node != null ? node.getLabel() : as.value;
                }
            }
            String json = Util.toJson(a);
            return Response.ok(json, MediaType.APPLICATION_JSON).build();
        } catch (SQLException e) {
            throw new WebApplicationException(Response.status(500).entity(e.getMessage()).build());
        } catch (IOException e) {
            throw new WebApplicationException(Response.status(500).entity(e.getMessage()).build());
        } finally {
            try {
                db.closeConnection();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
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
            throw new WebApplicationException(Response.status(500).entity(e.getMessage()).build());
        } catch (JsonMappingException e) {
            throw new WebApplicationException(Response.status(500).entity(e.getMessage()).build());
        } catch (JsonGenerationException e) {
            throw new WebApplicationException(Response.status(500).entity(e.getMessage()).build());
        } catch (IOException e) {
            throw new WebApplicationException(Response.status(500).entity(e.getMessage()).build());
        } finally {
            try {
                db.closeConnection();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
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
            throw new WebApplicationException(Response.status(500).entity(e.getMessage()).build());
        } catch (JsonMappingException e) {
            throw new WebApplicationException(Response.status(500).entity(e.getMessage()).build());
        } catch (JsonGenerationException e) {
            throw new WebApplicationException(Response.status(500).entity(e.getMessage()).build());
        } catch (IOException e) {
            throw new WebApplicationException(Response.status(500).entity(e.getMessage()).build());
        } finally {
            try {
                db.closeConnection();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }

    @GET
    @Path("/{aid}/projects")
    public Response getAssayProjects(@PathParam("aid") Long aid, @QueryParam("filter") String filter, @QueryParam("expand") String expand) throws SQLException, IOException {
        boolean expandEntries = false;
        if (expand != null && (expand.toLowerCase().equals("true") || expand.toLowerCase().equals("yes")))
            expandEntries = true;

        DBUtils db = new DBUtils();
        try {
            List<Project> projects = db.getProjectByAssayId(aid);
            if (!expandEntries) {
                List<String> links = Functional.Apply
                        (projects, new IApplyFunction<Project, String>() {
                            public String eval(Project project) {
                                return project.getResourcePath();
                            }
                        });
                BardLinkedEntity linkedEntity = new BardLinkedEntity(links, null);
                return Response.ok(Util.toJson(linkedEntity), MediaType.APPLICATION_JSON).build();
            } else {
                BardLinkedEntity linkedEntity = new BardLinkedEntity(projects, null);
                return Response.ok(Util.toJson(linkedEntity), MediaType.APPLICATION_JSON).build();
            }
        } finally {
            db.closeConnection();
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
            throw new WebApplicationException(Response.status(500).entity(e.getMessage()).build());
        } catch (JsonMappingException e) {
            throw new WebApplicationException(Response.status(500).entity(e.getMessage()).build());
        } catch (JsonGenerationException e) {
            throw new WebApplicationException(Response.status(500).entity(e.getMessage()).build());
        } catch (IOException e) {
            throw new WebApplicationException(Response.status(500).entity(e.getMessage()).build());
        } finally {
            try {
                db.closeConnection();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }

    @GET
    @Path("/{aid}/experiments/{eid}")
    public Response getAssayExperiment(@PathParam("aid") String aid,
                                       @PathParam("eid") String eid,
                                       @QueryParam("filter") String filter, @QueryParam("expand") String expand) {
        Experiment e = new Experiment();
        e.setExptId(Long.parseLong(eid));
        UriBuilder ub = UriBuilder.fromUri("/experiments/" + eid);
        if (filter != null) ub.queryParam("filter", filter);
        if (expand != null) ub.queryParam("name", expand);
        return Response.temporaryRedirect(ub.build()).build();
    }

    String toJson (DBUtils db, List<Assay> assays,
                   boolean annotation) throws Exception {
        if (!annotation) {
            return Util.toJson(assays);
        }

        CAPDictionary dict = db.getCAPDictionary();
        ObjectMapper mapper = new ObjectMapper();

        ArrayNode array = (ArrayNode) mapper.valueToTree(assays);
        for (int i = 0; i < array.size(); ++i) {
            ObjectNode n = (ObjectNode) array.get(i);
            long aid = n.get("aid").asLong();

            try {
                List<CAPAssayAnnotation> a = db.getAssayAnnotations(aid);
                if (a == null) throw new WebApplicationException(404);

                CAPDictionaryElement node;
                for (CAPAssayAnnotation as : a) {
                    if (as.key != null) {
                        node = dict.getNode(new BigInteger(as.key));
                        as.key = node != null ? node.getLabel() : as.key;
                    }
                    if (as.value != null) {
                        node = dict.getNode(new BigInteger(as.value));
                        as.value = node != null ? node.getLabel() : as.value;
                    }
                }
                n.putPOJO("annotations", a);
            }
            catch (Exception ex) {
                log.warn("Can't get annotation for assay "+aid);
            }
        }

        return mapper.writeValueAsString(array);        
    }

    @Override
    @GET
    @Path("/etag/{etag}")
    public Response getEntitiesByETag(@PathParam("etag") String resourceId,
                                      @QueryParam("filter") String filter,
                                      @QueryParam("expand") String expand,
                                      @QueryParam("skip") Integer skip,
                                      @QueryParam("top") Integer top) {
        DBUtils db = new DBUtils();
        try {
            List<Assay> assays = db.getAssaysByETag
                (skip != null ? skip : -1, top != null ? top : -1, resourceId);

            String json = toJson
                (db, assays, expand != null
                 && expand.toLowerCase().equals("true"));

            return Response.ok(json, MediaType.APPLICATION_JSON).build();
        } catch (Exception e) {
            throw new WebApplicationException(Response.status(500).entity(e.getMessage()).build());
        } finally {
            try {
                db.closeConnection();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}