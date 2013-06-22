package gov.nih.ncgc.bard.rest;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import gov.nih.ncgc.bard.capextract.CAPAnnotation;
import gov.nih.ncgc.bard.entity.Assay;
import gov.nih.ncgc.bard.entity.BardLinkedEntity;
import gov.nih.ncgc.bard.entity.Compound;
import gov.nih.ncgc.bard.entity.Experiment;
import gov.nih.ncgc.bard.entity.ExperimentData;
import gov.nih.ncgc.bard.entity.ExperimentResultType;
import gov.nih.ncgc.bard.entity.Project;
import gov.nih.ncgc.bard.entity.Substance;
import gov.nih.ncgc.bard.tools.AnnotationUtils;
import gov.nih.ncgc.bard.tools.DBUtils;
import gov.nih.ncgc.bard.tools.Util;
import gov.nih.ncgc.util.functional.Functional;
import gov.nih.ncgc.util.functional.IApplyFunction;

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
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Prototype of MLBD REST resources.
 * <p/>
 * This is mainly to explore the use of Jersey for presenting REST
 * services for the MLBD
 *
 * @author Rajarshi Guha
 */
@Path("/experiments")
public class    BARDExperimentResource extends BARDResource<Experiment> {

    public static final String DATE_FORMAT_NOW = "yyyy-MM-dd HH:mm:ss";
    static final String VERSION = "1.0";

    @Context
    ServletContext servletContext;
    @Context
    HttpServletRequest httpServletRequest;
    @Context
    HttpHeaders headers;

    public Class<Experiment> getEntityClass () { return Experiment.class; }
    public String getResourceBase () {
        return BARDConstants.API_BASE+"/experiments";
    }

    @GET
    @Produces("text/plain")
    @Path("/_info")
    public String info() {
        StringBuilder msg = new StringBuilder("Returns experiment information\n\nAvailable resources:\n");
        List<String> paths = Util.getResourcePaths(this.getClass());
        for (String path : paths) msg.append(path).append("\n");
        msg.append("/experiments/" + BARDConstants.API_EXTRA_PARAM_SPEC + "\n");
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
                ret = String.valueOf(db.getEntityCount(Experiment.class));
            } else {
                List<Experiment> experiments = db.searchForEntity(filter, -1, -1, Experiment.class);
                ret = String.valueOf(experiments.size());
            }
            db.closeConnection();
            return ret;
        } catch (SQLException e) {
            throw new WebApplicationException(e, 500);
        } catch (IOException e) {
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

        if (skip == null) skip = -1;
        if (top == null) top = -1;

        DBUtils db = new DBUtils();
        try {
            String linkString = null;
            if (filter == null) {
                if (countRequested)
                    return Response.ok(String.valueOf(db.getEntityCount(Experiment.class)), MediaType.TEXT_PLAIN).build();
                if ((top == -1)) { // top was not specified, so we start from the beginning
                    top = BARDConstants.MAX_COMPOUND_COUNT;
                }
                if (skip == -1) skip = 0;
                String expandClause = "expand=false";
                if (expandEntries) expandClause = "expand=true";
                if (skip + top <= db.getEntityCount(Experiment.class))
                    linkString = BARDConstants.API_BASE + "/experiments?skip=" + (skip + top) + "&top=" + top + "&" + expandClause;
            }
            List<Experiment> experiments = db.searchForEntity(filter, skip, top, Experiment.class);
            db.closeConnection();

            if (countRequested) return Response.ok(String.valueOf(experiments.size()), MediaType.TEXT_PLAIN).build();
            if (expandEntries) {
                BardLinkedEntity linkedEntity = new BardLinkedEntity(experiments, linkString);
                return Response.ok(Util.toJson(linkedEntity), MediaType.APPLICATION_JSON).build();
            } else {
                List<String> links = new ArrayList<String>();
                for (Experiment experiment : experiments) links.add(experiment.getResourcePath());
                BardLinkedEntity linkedEntity = new BardLinkedEntity(links, linkString);
                return Response.ok(Util.toJson(linkedEntity), MediaType.APPLICATION_JSON).build();
            }

        } catch (SQLException e) {
            throw new WebApplicationException(e, 500);
        } catch (IOException e) {
            throw new WebApplicationException(e, 500);
        } finally {
            try {
                db.closeConnection();
            } catch (SQLException e) {

            }
        }
    }

    @POST
    @Path("/")
    @Consumes("application/x-www-form-urlencoded")
    public Response getResources(@FormParam("ids") String eids, 
                                 @QueryParam("expand") String expand) 
        throws SQLException {
        DBUtils db = new DBUtils ();
        try {
            if (eids == null)
                throw new BadRequestException("POST request must specify the ids form parameter, which should be a comma separated string of experiment IDs");
            List<Experiment> experiments = new ArrayList<Experiment>();
            for (String s : eids.split("[,;\\s]")) {
                try {
                    Experiment e = db.getExperimentByExptId
                        (Long.parseLong(s));
                    experiments.add(e);
                }
                catch (NumberFormatException ex) {
                    // ignore bogus 
                }
            }

            return Response.ok(Util.toJson(experiments),
                               MediaType.APPLICATION_JSON).build();
        }
        catch (Exception ex) {
            throw new WebApplicationException (ex, 500);
        }            
        finally {
            db.closeConnection();
        }
    }

    @GET
    @Path("/{eid}")
    public Response getResources(@PathParam("eid") String resourceId, @QueryParam("filter") String filter, @QueryParam("expand") String expand) {
        DBUtils db = new DBUtils();
        Experiment experiment;
        try {
            experiment = db.getExperimentByExptId(Long.valueOf(resourceId));
            if (experiment == null || experiment.getBardExptId() == null) throw new WebApplicationException(404);
            String json = Util.toJson(experiment);
            if (expand != null && expand.toLowerCase().trim().equals("true")) { // expand experiment and project entries
                json = getExpandedJson(experiment, Long.parseLong(resourceId), db).toString();
            }
            return Response.ok(json, MediaType.APPLICATION_JSON).build();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new WebApplicationException(e, 500);
        } catch (IOException e) {
            e.printStackTrace();
            throw new WebApplicationException(e, 500);
        } finally {
            try {
                db.closeConnection();
            } catch (SQLException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
    }

    JsonNode getExpandedJson(Experiment e, Long eid, DBUtils db) throws SQLException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode t = mapper.valueToTree(e);

        List<Assay> assays = db.getAssaysByExperimentId(eid);
        ArrayNode an = mapper.createArrayNode();
        for (Assay a : assays) {
            ObjectNode on = mapper.valueToTree(a);
            an.add(on);
        }
        ((ObjectNode) t).put("assayId", an);

//        List<Project> projs = db.getProjectByAssayId(eid);
//        an = mapper.createArrayNode();
//        for (Project e : projs) {
//            ObjectNode on = mapper.valueToTree(e);
//            an.add(on);
//        }
//        ((ObjectNode) t).put("projects", an);

        return t;
    }

    @GET
    @Path("/{eid}/projects")
    public Response getProjects(@PathParam("eid") Long eid, @QueryParam("filter") String filter, @QueryParam("expand") String expand) throws SQLException, IOException {
        boolean expandEntries = false;
        if (expand != null && (expand.toLowerCase().equals("true") || expand.toLowerCase().equals("yes")))
            expandEntries = true;

        DBUtils db = new DBUtils();
        List<Project> projects = db.getProjectByExperimentId(eid);
        if (!expandEntries) {
            List<String> links = Functional.Apply(projects, new IApplyFunction<Project, String>() {
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
    }

    // TODO right now, we don't support filtering on compounds
    @GET
    @Path("/{eid}/compounds")
    public Response getExperimentCompounds(@PathParam("eid") String resourceId,
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

        boolean filterActives = false;
        if (filter != null && filter.contains("[active]")) filterActives = true;

        try {
            if (countRequested) {
                int n = db.getExperimentCidCount(Long.valueOf(resourceId), filterActives);
                db.closeConnection();
                return Response.ok(String.valueOf(n), MediaType.TEXT_PLAIN).build();
            }

            Experiment experiment = db.getExperimentByExptId(Long.valueOf(resourceId));

            // set up skip and top params
            if (experiment.getCompounds() > BARDConstants.MAX_COMPOUND_COUNT) {
                if ((top == -1)) { // top was not specified, so we start from the beginning
                    top = BARDConstants.MAX_COMPOUND_COUNT;
                }
                if (skip == -1) skip = 0;
                String expandClause = "expand=false";
                if (expandEntries) expandClause = "expand=true";
                String filterClause = "";
                if (filterActives) filterClause = "&filter=[active]";

                if (skip + top <= experiment.getCompounds())
                    linkString = BARDConstants.API_BASE + "/experiments/" + resourceId + "/compounds?skip=" + (skip + top) + "&top=" + top + "&" + expandClause+filterClause;
            }

            if (types.contains(BARDConstants.MIME_SMILES)) {

            } else if (types.contains(BARDConstants.MIME_SDF)) {

            } else { // JSON
                String json;
                if (!expandEntries) {
                    List<Long> cids = db.getExperimentCids(Long.valueOf(resourceId), skip, top, filterActives);
                    List<String> links = new ArrayList<String>();
                    for (Long cid : cids) links.add((new Compound(cid, null, null)).getResourcePath());

                    BardLinkedEntity linkedEntity = new BardLinkedEntity(links, linkString);
                    json = Util.toJson(linkedEntity);
                } else {
                    List<Compound> compounds = db.getExperimentCompounds(Long.valueOf(resourceId), skip, top, filterActives);
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
    public Response getExperimentSubstances(@PathParam("eid") String resourceId,
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

        boolean filterActives = false;
        if (filter != null && filter.contains("[active]")) filterActives = true;

        try {
            if (countRequested) {
                int n = db.getExperimentSidCount(Long.valueOf(resourceId), filterActives);
                db.closeConnection();
                return Response.ok(String.valueOf(n), MediaType.TEXT_PLAIN).build();
            }

            Experiment experiemnt = db.getExperimentByExptId(Long.valueOf(resourceId));

            // set up skip and top params
            if (experiemnt.getSubstances() > BARDConstants.MAX_COMPOUND_COUNT) {
                if ((top == -1)) { // top was not specified, so we start from the beginning
                    top = BARDConstants.MAX_COMPOUND_COUNT;
                }
                if (skip == -1) skip = 0;
                String expandClause = "expand=false";
                if (expandEntries) expandClause = "expand=true";
                String filterClause = "";
                if (filterActives) filterClause = "&filter=[active]";
                if (skip + top <= experiemnt.getSubstances())
                    linkString = BARDConstants.API_BASE + "/experiments/" + resourceId + "/substances?skip=" + (skip + top) + "&top=" + top + "&" + expandClause + filterClause;
            }

            if (types.contains(BARDConstants.MIME_SMILES)) {

            } else if (types.contains(BARDConstants.MIME_SDF)) {

            } else { // JSON
                String json;
                if (!expandEntries) {
                    List<Long> sids = db.getExperimentSids(Long.valueOf(resourceId), skip, top, filterActives);
                    List<String> links = new ArrayList<String>();
                    for (Long sid : sids) links.add((new Substance(sid, null)).getResourcePath());
                    BardLinkedEntity linkedEntity = new BardLinkedEntity(links, linkString);
                    json = Util.toJson(linkedEntity);
                } else {
                    List<Compound> compounds = db.getExperimentSubstances(Long.valueOf(resourceId), skip, top, filterActives);
                    BardLinkedEntity linkedEntity = new BardLinkedEntity(compounds, linkString);
                    json = Util.toJson(linkedEntity);
                }
                db.closeConnection();
                return Response.ok(json, MediaType.APPLICATION_JSON).build();
            }
        } catch (SQLException e) {
            logger.warning(e.toString());
            throw new WebApplicationException(e, 500);
        } catch (IOException e) {
            logger.warning(e.toString());
            throw new WebApplicationException(e, 500);
        }
        return null;
    }

    @GET
    @Path("/{eid}/metadata")
    public Response getExperimentData(@PathParam("eid") String resourceId)
        throws SQLException {
        
        DBUtils db = new DBUtils();
        try {
            String json = db.getExperimentMetadataByExptId
                (Long.parseLong(resourceId));
            return Response.ok(json, MediaType.APPLICATION_JSON).build();
        } 
        catch (Exception e) {
            throw new WebApplicationException(e, 500);
        }
        finally {
            db.closeConnection();
        }
    }

    @GET
    @Path("/{eid}/exptdata")
    public Response getExperimentData(@PathParam("eid") String resourceId,
                                      @QueryParam("filter") String filter,
                                      @QueryParam("expand") String expand,
                                      @QueryParam("skip") Integer skip,
                                      @QueryParam("top") Integer top) throws SQLException {
        boolean expandEntries = false;
        if (expand != null && (expand.toLowerCase().equals("true") || expand.toLowerCase().equals("yes")))
            expandEntries = true;

        DBUtils db = new DBUtils();
        String linkString = null;

        if (skip == null) skip = -1;
        if (top == null) top = -1;

        try {

            Experiment experiemnt = db.getExperimentByExptId(Long.valueOf(resourceId));

            // set up skip and top params
            if (experiemnt.getSubstances() > BARDConstants.MAX_DATA_COUNT) {
                if ((top == -1)) { // top was not specified, so we start from the beginning
                    top = BARDConstants.MAX_DATA_COUNT;
                }
                if (skip == -1) skip = 0;

                String expandClause = "expand=false";
                if (expandEntries) expandClause = "expand=true";

                String filterClause = "";
                if (filter != null) filterClause = "filter=" + filter;
                if (skip + top <= experiemnt.getSubstances())
                    linkString = BARDConstants.API_BASE + "/experiments/" + resourceId + "/exptdata?skip=" + (skip + top) +
                            "&top=" + top +
                            "&" + expandClause +
                            "&" + filterClause;
            }

            String json;
            if (!expandEntries) {
                List<String> edids = db.getExperimentDataIds(Long.valueOf(resourceId), skip, top, filter);
                List<String> links = new ArrayList<String>();
                for (String edid : edids) {
                    ExperimentData ed = new ExperimentData();
                    ed.setExptDataId(edid);
                    links.add(ed.getResourcePath());
                }
                BardLinkedEntity linkedEntity = new BardLinkedEntity(links, linkString);
                json = Util.toJson(linkedEntity);
            } else {
                long start = System.currentTimeMillis();
                List<ExperimentData> data = db.getExperimentData(Long.valueOf(resourceId), skip, top, filter);
                long end = System.currentTimeMillis();
                double dbQueryTime = (end - start) / 1000.0;

                BardLinkedEntity linkedEntity = new BardLinkedEntity(data, linkString);

                start = System.currentTimeMillis();
                json = Util.toJson(linkedEntity);
                end = System.currentTimeMillis();
                double jsonTime = (end - start) / 1000.0;

                logger.info("Time to retrieve " + data.size() + " expanded entries for expt " + resourceId + " used " + dbQueryTime + "s for DB query and " + jsonTime + "s for JSON generation");

            }
            db.closeConnection();
            return Response.ok(json, MediaType.APPLICATION_JSON).build();
        } catch (SQLException e) {
            db.closeConnection();
            throw new WebApplicationException(Response.status(500).entity(e.toString()).build());
        } catch (IOException e) {
            db.closeConnection();
            throw new WebApplicationException(Response.status(500).entity(e.toString()).build());
        }
    }

    /*
    @POST
    @Path("/{eid}/exptdata")
    public Response getExperimentData(@PathParam("eid") String resourceId,
                                      @FormParam("sids") String sids) 
        throws SQLException {
        if (sids == null) {
            throw new BadRequestException
                (getRequestURI()+": No \"sids\" form parameter specified "
                 +"for POST!");
        }

        DBUtils db = new DBUtils();
        try {
            Experiment experiemnt = db.getExperimentByExptId
                (Long.valueOf(resourceId));

            // not done!
            String json = Util.toJson(experiment);
            return Response.ok(json, MediaType.APPLICATION_JSON).build();
        }
        catch (Exception ex) {
            throw new BadRequestException (ex.getMessage());
        }
        finally {
            db.closeConnection();
        }
    }
    */

    @GET
    @Path("/{eid}/etag/{etag}/exptdata")
    public Response getExperimentDataETag(@PathParam("eid") Long eid,
                                          @PathParam("etag") String etag,
                                          @QueryParam("skip") Integer skip,
                                          @QueryParam("top") Integer top) 
        throws SQLException, IOException {
        DBUtils db = new DBUtils ();
        try {
            List<ExperimentData> data = db.getExperimentDataByETag
                (skip != null ? skip : -1, top != null ? top : -1, eid, etag);
            return Response.ok(Util.toJson(data), 
                               MediaType.APPLICATION_JSON).build();
        }
        finally {
            db.closeConnection();
        }
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
            List<Experiment> expts = db.getExperimentsByETag(skip != null ? skip : -1, top != null ? top : -1, resourceId);
            String json = Util.toJson(expts);
            return Response.ok(json, MediaType.APPLICATION_JSON).build();
        } catch (Exception e) {
            throw new WebApplicationException(e, 500);
        } finally {
            try {
                db.closeConnection();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    @GET
    @Path("/{eid}/summary")
    public Response getSummary(@PathParam("eid") Long eid) throws IOException, SQLException {
        long start = System.currentTimeMillis();

        Map<String, Object> s = new HashMap<String, Object>();
        DBUtils db = new DBUtils();

        Experiment e = db.getExperimentByExptId(eid);
        if (e == null || e.getBardExptId() == null) throw new WebApplicationException(404);
        int nsub = e.getSubstances();

        s.put("compounds.tested", e.getCompounds());
        s.put("substances.tested", nsub);

        int nhit = 0;
        Map<String, Integer> colanno = new HashMap<String, Integer>();

        List<ExperimentData> data = db.getActiveExperimentData(eid, -1, -1);
        for (ExperimentData ed : data) {
            // should we check whether it is a confirmatory screen?
//            if (ed.getOutcome() == 2 && e.getType() == 2) {
//                nhit++;
//            }
            Map<String, String[]> annos = db.getCompoundAnnotations(ed.getCid());
            String[] keys = annos.get("anno_key");
            String[] vals = annos.get("anno_val");
            for (int i = 0; i < keys.length; i++) {
                if (keys[i].equals("COLLECTION")) {
                    String val = vals[i].trim().split("\\|")[0];
                    if (colanno.containsKey(val)) colanno.put(val, colanno.get(val) + 1);
                    else colanno.put(val, 1);
                }
            }
        }

        s.put("COLLECTION", colanno);
        s.put("nhit", data.size());
        double duration = (System.currentTimeMillis() - start) / 1000.0;
        logger.info("Time to generate summary was " + duration + "s");
        return Response.ok(Util.toJson(s), MediaType.APPLICATION_JSON).build();
    }

    @GET
    @Path("/{eid}/resulttypes")
    @Produces("application/json")
    public Response getExperimentResultTypes(@PathParam("eid") Long eid,
                                             @QueryParam("expand") String expand,
                                             @QueryParam("collapse") Integer collapse) {
        DBUtils db = new DBUtils();
        try {
            List<ExperimentResultType> rtypes = db.getExperimentResultTypes(eid, collapse);
            String json = null;
            if (expandEntries(expand)) json = Util.toJson(rtypes);
            else {
                List<String> rtypeNames = new ArrayList<String>();
                for (ExperimentResultType rtype : rtypes) rtypeNames.add(rtype.getName());
                json = Util.toJson(rtypeNames);
            }
            return Response.ok(json).type(MediaType.APPLICATION_JSON).build();
        } catch (SQLException e) {
            throw new WebApplicationException(e, 500);
        } catch (IOException e) {
            throw new WebApplicationException(e, 500);
        }
    }

    @GET
    @Produces("application/json")
    @Path("/{eid}/annotations")
    public Response getAnnotations(@PathParam("eid") Long eid, @QueryParam("filter") String filter, @QueryParam("expand") String expand) throws ClassNotFoundException, IOException, SQLException {
        DBUtils db = new DBUtils();
        List<CAPAnnotation> a;
        try {
            a = db.getExperimentAnnotations(eid);
            if (a == null) throw new WebApplicationException(404);
            JsonNode topLevel = AnnotationUtils.getAnnotationJson(a);
            ObjectMapper mapper = new ObjectMapper();
            Writer writer = new StringWriter();
            JsonFactory fac = new JsonFactory();
            JsonGenerator jsg = fac.createJsonGenerator(writer);
            mapper.writeTree(jsg, topLevel);
            String json = writer.toString();
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

}