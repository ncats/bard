package gov.nih.ncgc.bard.rest;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import gov.nih.ncgc.bard.capextract.CAPAnnotation;
import gov.nih.ncgc.bard.capextract.CAPDictionary;
import gov.nih.ncgc.bard.capextract.CAPDictionaryElement;
import gov.nih.ncgc.bard.entity.*;
import gov.nih.ncgc.bard.search.Facet;
import gov.nih.ncgc.bard.tools.DBUtils;
import gov.nih.ncgc.bard.tools.Util;
import gov.nih.ncgc.util.functional.Functional;
import gov.nih.ncgc.util.functional.IApplyFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.math.BigInteger;
import java.sql.SQLException;
import java.util.*;

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

    public Class<Assay> getEntityClass() {
        return Assay.class;
    }

    public String getResourceBase() {
        return BARDConstants.API_BASE + "/assays";
    }

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
    @Produces("application/json")
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

            long start = System.currentTimeMillis();
            List<Assay> assays = db.searchForEntity(filter, skip, top, Assay.class);
            log.info(getRequestURI() + "..." + assays.size() + " in "
                    + String.format("%1$.3fs", 1.e-3 * (System.currentTimeMillis() - start)));

            if (countRequested) return Response.ok(String.valueOf(assays.size()), MediaType.TEXT_PLAIN).build();
            if (expandEntries) {
                BardLinkedEntity linkedEntity = new BardLinkedEntity(assays, linkString);
                start = System.currentTimeMillis();

                String json = getExpandedJson(linkedEntity, null, db).toString();
                log.info("## Generating json in " + String.format("%1$.3fs", 1.e-3 * (System.currentTimeMillis() - start)));
                return Response.ok(json, MediaType.APPLICATION_JSON).build();
            } else {
                List<String> links = new ArrayList<String>();
                for (Assay a : assays) links.add(a.getResourcePath());
                BardLinkedEntity linkedEntity = new BardLinkedEntity(links, linkString);
                return Response.ok(Util.toJson(linkedEntity), MediaType.APPLICATION_JSON).build();
            }

        } catch (SQLException e) {
            e.printStackTrace();
            throw new WebApplicationException(Response.status(500).entity(e.getMessage()).build());
        } catch (IOException e) {
            e.printStackTrace();
            throw new WebApplicationException(Response.status(500).entity(e.getMessage()).build());
        } finally {
            try {
                db.closeConnection();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }

    JsonNode getSingleExpandedNode(Assay a, Long aid, DBUtils db) throws SQLException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode t = mapper.valueToTree(a);

        ((ObjectNode) t).put("description", a.getDescription());
        ((ObjectNode) t).put("protocol", a.getProtocol());
        ((ObjectNode) t).put("comments", a.getComments());

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

        List<Publication> pubs = db.getAssayPublications(aid);
        an = mapper.createArrayNode();
        for (Publication pub : pubs) {
            ObjectNode on = mapper.valueToTree(pub);
            an.add(on);
        }
        ((ObjectNode) t).put("documents", an);

        List<Biology> targets = db.getBiologyByEntity("assay", aid);
        an = mapper.createArrayNode();
        for (Biology target : targets) {
            ObjectNode on = mapper.valueToTree(target);
            an.add(on);
        }
        ((ObjectNode) t).put("targets", an);
        return t;
    }

    JsonNode getExpandedJson(Object o, Long aid, DBUtils db) throws SQLException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode t = null;
        if (o instanceof Assay) {
            return getSingleExpandedNode((Assay) o, aid, db);
        } else if (o instanceof BardLinkedEntity) {
            ArrayNode an = mapper.createArrayNode();
            BardLinkedEntity e = (BardLinkedEntity) o;
            List assays = (List) e.getCollection();
            for (Object assay : assays) {
                Assay a = (Assay) assay;
                an.add(getSingleExpandedNode(a, a.getBardAssayId(), db));
            }
            t = mapper.createObjectNode();
            ((ObjectNode) t).put("collection", an);
            ((ObjectNode) t).put("link", e.getLink());
        }
        return t;
    }

    @GET
    @Produces("application/json")
    @Path("/{aid}")
    public Response getResources(@PathParam("aid") String resourceId, @QueryParam("filter") String filter, @QueryParam("expand") String expand) {
        if (!Util.isNumber(resourceId)) throw new WebApplicationException(400);

        DBUtils db = new DBUtils();
        Assay a;
        try {
            a = db.getAssayByAid(Long.valueOf(resourceId));
            if (a == null || a.getBardAssayId() == null) throw new WebApplicationException(404);

            JsonNode node;
            if (expandEntries(expand)) { // expand experiment and project entries
                node = getExpandedJson(a, Long.parseLong(resourceId), db);
            } else {
                ObjectMapper mapper = new ObjectMapper();
                node = mapper.valueToTree(a);
            }
            String json = node.toString();

            return Response.ok(json, MediaType.APPLICATION_JSON).build();
        } catch (SQLException e) {
            e.printStackTrace();
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
    @Produces("application/json")
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

            List<Assay> tassays = db.getAssays(ids);
            // remove null assays. If all assays are null return a 404
            List<Assay> assays = new ArrayList<Assay>();
            for (Assay a : tassays) {
                if (a != null) {
                    assays.add(a);
                }
            }
            if (assays.size() == 0) throw new WebApplicationException(404);

            if (countRequested) return Response.ok(String.valueOf(assays.size()), MediaType.TEXT_PLAIN).build();
            db.closeConnection();

            String json;
            if (expand == null || expand.toLowerCase().equals("false")) {
                List<String> links = new ArrayList<String>();
                for (Assay ap : assays) links.add(ap.getResourcePath());
                json = Util.toJson(links);
            } else {
                db = new DBUtils();
                ObjectMapper mapper = new ObjectMapper();
                ArrayNode an = mapper.createArrayNode();
                for (Assay a : assays) {
                    an.add(getExpandedJson(a, a.getBardAssayId(), db));
                }
                json = an.toString();
                db.closeConnection();
            }
            return Response.ok(json, MediaType.APPLICATION_JSON).build();
        } catch (SQLException e) {
            throw new WebApplicationException(Response.status(500).entity(e.getMessage()).build());
        } catch (IOException e) {
            throw new WebApplicationException(Response.status(500).entity(e.getMessage()).build());
        }
    }


    JsonNode getAnnotationJson(List<CAPAnnotation> a) throws ClassNotFoundException, IOException, SQLException {
        DBUtils db = new DBUtils();
        CAPDictionary dict = db.getCAPDictionary();

        // lets group these annotations and construct our JSON response
        CAPDictionaryElement node;

        Map<Integer, List<CAPAnnotation>> contexts = new HashMap<Integer, List<CAPAnnotation>>();
        for (CAPAnnotation anno : a) {
            Integer id = anno.id;
            if (id == null) id = -1; // corresponds to dynamically generated annotations (from non-CAP sources)

            // go from dict key to label
            if (anno.key != null && Util.isNumber(anno.key)) {
                node = dict.getNode(new BigInteger(anno.key));
                anno.key = node != null ? node.getLabel() : anno.key;
            }
            if (anno.value != null && Util.isNumber(anno.value)) {
                node = dict.getNode(new BigInteger(anno.value));
                anno.value = node != null ? node.getLabel() : anno.value;
            }

            if (contexts.containsKey(id)) {
                List<CAPAnnotation> la = contexts.get(id);
                la.add(anno);
                contexts.put(id, la);
            } else {
                List<CAPAnnotation> la = new ArrayList<CAPAnnotation>();
                la.add(anno);
                contexts.put(id, la);
            }
        }

        ObjectMapper mapper = new ObjectMapper();
        ArrayNode docNode = mapper.createArrayNode();
        ArrayNode contextNode = mapper.createArrayNode();
        ArrayNode measureNode = mapper.createArrayNode();
        ArrayNode miscNode = mapper.createArrayNode();

        for (Integer contextId : contexts.keySet()) {
            List<CAPAnnotation> comps = contexts.get(contextId);
            Collections.sort(comps, new Comparator<CAPAnnotation>() {
                @Override
                public int compare(CAPAnnotation o1, CAPAnnotation o2) {
                    if (o1.displayOrder == o2.displayOrder) return 0;
                    return o1.displayOrder < o2.displayOrder ? -1 : 1;
                }
            });
            JsonNode arrayNode = mapper.valueToTree(comps);
            ObjectNode n = mapper.createObjectNode();
            n.put("id", comps.get(0).id);
            n.put("name", comps.get(0).contextRef);
            n.put("comps", arrayNode);

            if (comps.get(0).source.equals("cap-doc")) docNode.add(n);
            else if (comps.get(0).source.equals("cap-context")) contextNode.add(n);
            else if (comps.get(0).source.equals("cap-measure")) measureNode.add(n);
            else {
                for (CAPAnnotation misca : comps) miscNode.add(mapper.valueToTree(misca));
            }
        }
        ObjectNode topLevel = mapper.createObjectNode();
        topLevel.put("contexts", contextNode);
        topLevel.put("measures", measureNode);
        topLevel.put("docs", docNode);
        topLevel.put("misc", miscNode);

        return topLevel;
    }

    @POST
    @Path("/annotations")
    @Produces("application/json")
    @Consumes("application/x-www-form-urlencoded")
    public Response getMultipleAnnotations(@FormParam("aids") String aids,
                                           @QueryParam("filter") String filter, @QueryParam("expand") String expand)
            throws ClassNotFoundException, IOException, SQLException {
        DBUtils db = new DBUtils();
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode topLevel = mapper.createObjectNode();

        for (String anAid : aids.split(",")) {
            Long aid = Long.valueOf(anAid.trim());
            List<CAPAnnotation> a = db.getAssayAnnotations(aid);
            if (a == null || a.size() == 0) {
                log.info("Got 0 annotations for aid " + aid);
                topLevel.put(aid.toString(), "");
            } else {
                log.info("Got " + a.size() + " annotations for aid " + aid);
                topLevel.put(aid.toString(), getAnnotationJson(a));
            }
        }
        Writer writer = new StringWriter();
        JsonFactory fac = new JsonFactory();
        JsonGenerator jsg = fac.createJsonGenerator(writer);
        mapper.writeTree(jsg, topLevel);
        String json = writer.toString();
        return Response.ok(json, MediaType.APPLICATION_JSON).build();
    }

    @GET
    @Produces("application/json")
    @Path("/{aid}/annotations")
    public Response getAnnotations(@PathParam("aid") Long resourceId, @QueryParam("filter") String filter, @QueryParam("expand") String expand) throws ClassNotFoundException, IOException, SQLException {
        DBUtils db = new DBUtils();
        List<CAPAnnotation> a;
        try {
            a = db.getAssayAnnotations(resourceId);
            if (a == null) throw new WebApplicationException(404);
            JsonNode topLevel = getAnnotationJson(a);
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

    @GET
    @Produces("application/json")
    @Path("/{aid}/targets")
    public Response getAssayTargets(@PathParam("aid") String resourceId, @QueryParam("filter") String filter, @QueryParam("expand") String expand) {
        if (!Util.isNumber(resourceId)) throw new WebApplicationException(400);
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
    @Produces("application/json")
    @Path("/{aid}/documents")
    public Response getAssayPublications(@PathParam("aid") String resourceId, @QueryParam("filter") String filter, @QueryParam("expand") String expand) {
        if (!Util.isNumber(resourceId)) throw new WebApplicationException(400);
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
    @Produces("application/json")
    @Path("/{aid}/experiments")
    public Response getAssayExperiments(@PathParam("aid") String resourceId, @QueryParam("filter") String filter, @QueryParam("expand") String expand) {
        if (!Util.isNumber(resourceId)) throw new WebApplicationException(400);
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
    @Produces("application/json")
    @Path("/{aid}/compounds")
    public Response getAssayCompounds(@PathParam("aid") String resourceId,
                                      @QueryParam("filter") String filter,
                                      @QueryParam("expand") String expand,
                                      @QueryParam("skip") Integer skip,
                                      @QueryParam("top") Integer top) {
        if (!Util.isNumber(resourceId)) throw new WebApplicationException(400);
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
            int n = db.getAssayCidCount(Long.valueOf(resourceId), filterActives);
            if (countRequested) {
                db.closeConnection();
                return Response.ok(String.valueOf(n), MediaType.TEXT_PLAIN).build();
            }

            // set up skip and top params
            if (n > BARDConstants.MAX_COMPOUND_COUNT) {
                if ((top == -1)) { // top was not specified, so we start from the beginning
                    top = BARDConstants.MAX_COMPOUND_COUNT;
                }
                if (skip == -1) skip = 0;
                String expandClause = "expand=false";
                if (expandEntries) expandClause = "expand=true";
                String filterClause = "";
                if (filterActives) filterClause = "&filter=[active]";

                if (skip + top <= n)
                    linkString = BARDConstants.API_BASE + "/assays/" + resourceId + "/compounds?skip=" + (skip + top) + "&top=" + top + "&" + expandClause + filterClause;
            }

            if (types.contains(BARDConstants.MIME_SMILES)) {

            } else if (types.contains(BARDConstants.MIME_SDF)) {

            } else { // JSON
                String json;
                if (!expandEntries) {
                    List<Long> cids = db.getAssayCids(Long.valueOf(resourceId), skip, top, filterActives);
                    List<String> links = new ArrayList<String>();
                    for (Long cid : cids) links.add((new Compound(cid, null, null)).getResourcePath());

                    BardLinkedEntity linkedEntity = new BardLinkedEntity(links, linkString);
                    json = Util.toJson(linkedEntity);
                } else {
                    List<Compound> compounds = db.getAssayCompounds(Long.valueOf(resourceId), skip, top, filterActives);
                    BardLinkedEntity linkedEntity = new BardLinkedEntity(compounds, linkString);
                    json = Util.toJson(linkedEntity);
                }
                db.closeConnection();
                return Response.ok(json, MediaType.APPLICATION_JSON).build();
            }
        } catch (SQLException e) {
            log.debug(e.toString());
            throw new WebApplicationException(e, 500);
        } catch (IOException e) {
            log.debug(e.toString());
            throw new WebApplicationException(e, 500);
        }
        return null;
    }

    @GET
    @Produces("application/json")
    @Path("/{aid}/substances")
    public Response getAssaySubstances(@PathParam("aid") String resourceId,
                                       @QueryParam("filter") String filter,
                                       @QueryParam("expand") String expand,
                                       @QueryParam("skip") Integer skip,
                                       @QueryParam("top") Integer top) {
        if (!Util.isNumber(resourceId)) throw new WebApplicationException(400);
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
            int n = db.getAssaySidCount(Long.valueOf(resourceId), filterActives);
            if (countRequested) {
                db.closeConnection();
                return Response.ok(String.valueOf(n), MediaType.TEXT_PLAIN).build();
            }

            // set up skip and top params
            if (n > BARDConstants.MAX_COMPOUND_COUNT) {
                if ((top == -1)) { // top was not specified, so we start from the beginning
                    top = BARDConstants.MAX_COMPOUND_COUNT;
                }
                if (skip == -1) skip = 0;
                String expandClause = "expand=false";
                if (expandEntries) expandClause = "expand=true";
                String filterClause = "";
                if (filterActives) filterClause = "&filter=[active]";

                if (skip + top <= n)
                    linkString = BARDConstants.API_BASE + "/assays/" + resourceId + "/substances?skip=" + (skip + top) + "&top=" + top + "&" + expandClause + filterClause;
            }

            if (types.contains(BARDConstants.MIME_SMILES)) {

            } else if (types.contains(BARDConstants.MIME_SDF)) {

            } else { // JSON
                String json;
                if (!expandEntries) {
                    List<Long> sids = db.getAssaySids(Long.valueOf(resourceId), skip, top, filterActives);
                    List<String> links = new ArrayList<String>();
                    for (Long sid : sids) links.add((new Substance(sid, null)).getResourcePath());

                    BardLinkedEntity linkedEntity = new BardLinkedEntity(links, linkString);
                    json = Util.toJson(linkedEntity);
                } else {
                    List<Substance> substances = db.getAssaySubstances(Long.valueOf(resourceId), skip, top, filterActives);
                    BardLinkedEntity linkedEntity = new BardLinkedEntity(substances, linkString);
                    json = Util.toJson(linkedEntity);
                }
                db.closeConnection();
                return Response.ok(json, MediaType.APPLICATION_JSON).build();
            }
        } catch (SQLException e) {
            log.debug(e.toString());
            throw new WebApplicationException(e, 500);
        } catch (IOException e) {
            log.debug(e.toString());
            throw new WebApplicationException(e, 500);
        }
        return null;
    }


    @GET
    @Produces("application/json")
    @Path("/{aid}/experiments/{eid}")
    public Response getAssayExperiment(@PathParam("aid") String aid,
                                       @PathParam("eid") String eid,
                                       @QueryParam("filter") String filter, @QueryParam("expand") String expand) {
        if (!Util.isNumber(aid)) throw new WebApplicationException(400);
        Experiment e = new Experiment();
        e.setBardExptId(Long.parseLong(eid));
        UriBuilder ub = UriBuilder.fromUri("/experiments/" + eid);
        if (filter != null) ub.queryParam("filter", filter);
        if (expand != null) ub.queryParam("name", expand);
        return Response.temporaryRedirect(ub.build()).build();
    }

    String toJson(DBUtils db, List<Assay> assays,
                  boolean annotation) throws Exception {
        if (!annotation) {
            return Util.toJson(assays);
        }

        CAPDictionary dict = db.getCAPDictionary();
        ObjectMapper mapper = new ObjectMapper();

        ArrayNode array = (ArrayNode) mapper.valueToTree(assays);
        for (int i = 0; i < array.size(); ++i) {
            ObjectNode n = (ObjectNode) array.get(i);
            long aid = n.get("bardAssayId").asLong();

            try {
                List<CAPAnnotation> a = db.getAssayAnnotations(aid);
                if (a == null) throw new WebApplicationException(404);

                CAPDictionaryElement node;
                for (CAPAnnotation as : a) {
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
            } catch (Exception ex) {
                log.warn("Can't get annotation for assay " + aid);
            }
        }

        return mapper.writeValueAsString(array);
    }

    @Override
    @GET
    @Produces("application/json")
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

    @Override
    @GET
    @Produces("application/json")
    @Path("/etag/{etag}/facets")
    public Response getFacets(@PathParam("etag") String resourceId) {
        DBUtils db = new DBUtils();
        try {
            List<Facet> facets = db.getAssayFacets(resourceId);
            return Response.ok(Util.toJson(facets),
                    MediaType.APPLICATION_JSON).build();
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