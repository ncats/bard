package gov.nih.ncgc.bard.rest;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
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

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
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
@Path("/projects")
public class BARDProjectResource extends BARDResource<Project> {

    public static final String DATE_FORMAT_NOW = "yyyy-MM-dd HH:mm:ss";
    static final String VERSION = "1.0";

    public Class<Project> getEntityClass () { return Project.class; }
    public String getResourceBase () {
        return BARDConstants.API_BASE+"/projects";
    }

    @GET
    @Produces("text/plain")
    @Path("/_info")
    public String info() {
        StringBuilder msg = new StringBuilder("Returns project information\n\nAvailable resources:\n");
        List<String> paths = Util.getResourcePaths(this.getClass());
        for (String path : paths) msg.append(path).append("\n");
        msg.append("/projects/" + BARDConstants.API_EXTRA_PARAM_SPEC + "\n");
        return msg.toString();
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
        Response response = null;
        try {
            String linkString = null;
            if (filter == null) { // just list all projects

                if (countRequested)
                    return Response.ok(String.valueOf(db.getEntityCount(Project.class)), MediaType.TEXT_PLAIN).build();
                if ((top == -1)) { // top was not specified, so we start from the beginning
                    top = BARDConstants.MAX_COMPOUND_COUNT;
                }
                if (skip == -1) skip = 0;
                String expandClause = "expand=false";
                if (expandEntries) expandClause = "expand=true";
                if (skip + top <= db.getEntityCount(Project.class))
                    linkString = BARDConstants.API_BASE + "/projects?skip=" + (skip + top) + "&top=" + top + "&" + expandClause;
            }

            List<Project> projects = db.searchForEntity(filter, skip, top, Project.class);
            db.closeConnection();

            if (countRequested) return Response.ok(String.valueOf(projects.size()), MediaType.TEXT_PLAIN).build();
            if (expandEntries) {
                BardLinkedEntity linkedEntity = new BardLinkedEntity(projects, linkString);
                return Response.ok(Util.toJson(linkedEntity), MediaType.APPLICATION_JSON).build();
            } else {
                List<String> links = new ArrayList<String>();
                for (Project project : projects) links.add(project.getResourcePath());
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
    @Path("/{id}")
    public Response getResources(@PathParam("id") String resourceId, @QueryParam("filter") String filter, @QueryParam("expand") String expand) {
        DBUtils db = new DBUtils();
        try {
            Project p = db.getProject(Long.valueOf(resourceId));
            if (p == null) throw new WebApplicationException(404);
            String json;
            if (countRequested) json = Util.toJson("1");
            else {
                json = Util.toJson(p);

                if (expandEntries(expand)) {
                    // need to update publication, experiment and assay entries
                    List<Assay> assays = new ArrayList<Assay>();
                    for (Long aid : p.getAids()) 
                        assays.add(db.getAssayByAid(aid));

                    List<Experiment> expts = new ArrayList<Experiment>();
                    for (Long eid : p.getEids()) 
                        expts.add(db.getExperimentByExptId(eid));

                    List<Publication> pubs = new ArrayList<Publication>();
                    for (Long pmid : p.getPublications())
                        pubs.add(db.getPublicationByPmid(pmid));

                    ObjectMapper mapper = new ObjectMapper();
                    ArrayNode an = mapper.createArrayNode();
                    for (Assay assay : assays) {
                        an.add(mapper.valueToTree(assay));
                    }
                    ArrayNode en = mapper.createArrayNode();
                    for (Experiment expt : expts) {
                        en.add(mapper.valueToTree(expt));
                    }
                    ArrayNode pn = mapper.createArrayNode();
                    for (Publication pub : pubs) {
                        pn.add(mapper.valueToTree(pub));
                    }

                    JsonNode tree = mapper.valueToTree(p);
                    ((ObjectNode)tree).put("eids", en);
                    ((ObjectNode)tree).put("aids", an);
                    ((ObjectNode)tree).put("publications", pn);

                    Writer writer = new StringWriter();
                    JsonFactory fac = new JsonFactory();
                    JsonGenerator jsg = fac.createJsonGenerator(writer);
                    mapper.writeTree(jsg, tree);
                    json = writer.toString();
                }
            }

            db.closeConnection();
            return Response.ok(json, MediaType.APPLICATION_JSON).build();
        } catch (SQLException e) {
            throw new WebApplicationException(e, 500);
        } catch (IOException e) {
            throw new WebApplicationException(e, 500);
        }
    }

    @POST
    @Path("/")
    @Consumes("application/x-www-form-urlencoded")
    public Response getResources(@FormParam("ids") String pids, @QueryParam("expand") String expand) {
        if (pids == null)
            throw new WebApplicationException(new Exception("POST request must specify the pids form parameter, which should be a comma separated string of project IDs"), 400);
        DBUtils db = new DBUtils();
        try {
            // we'll asssume an ID list if we're being called via POST
            String[] s = pids.split(",");
            Long[] ids = new Long[s.length];
            for (int i = 0; i < s.length; i++) ids[i] = Long.parseLong(s[i].trim());

            List<Project> p = db.getProjects(ids);
            if (countRequested) return Response.ok(String.valueOf(p.size()), MediaType.TEXT_PLAIN).build();
            db.closeConnection();

            String json;
            if (expand == null || expand.toLowerCase().equals("false")) {
                List<String> links = new ArrayList<String>();
                for (Project ap : p) 
                    if (ap != null) 
                        links.add(ap.getResourcePath());
                json = Util.toJson(links);
            } else json = Util.toJson(p);
            return Response.ok(json, MediaType.APPLICATION_JSON).build();
        } catch (SQLException e) {
            throw new WebApplicationException(e, 500);
        } catch (IOException e) {
            throw new WebApplicationException(e, 500);
        }
    }


    // TODO only list targets associated with the summary assay, not the member assays. Correct?
    @GET
    @Path("/{id}/targets")
    public Response getTargetsForProject(@PathParam("id") String resourceId, @QueryParam("filter") String filter, @QueryParam("search") String search, @QueryParam("expand") String expand) {
        boolean expandEntries = false;
        if (expand != null && (expand.toLowerCase().equals("true") || expand.toLowerCase().equals("yes")))
            expandEntries = true;
        DBUtils db = new DBUtils();
        try {
            Project project = db.getProject(Long.valueOf(resourceId));
            List<ProteinTarget> targets = new ArrayList<ProteinTarget>();
            if (project.getTargets() != null) {
                for (ProteinTarget target : project.getTargets())
                    targets.add(db.getProteinTargetByAccession(target.getAcc()));
            }
            String json;
            if (countRequested) json = Util.toJson(targets.size());
            else if (expandEntries) json = Util.toJson(targets);
            else {
                List<String> links = new ArrayList<String>();
                for (ProteinTarget pt : targets) links.add(pt.getResourcePath());
                json = Util.toJson(links);
            }
            db.closeConnection();
            return Response.ok(json, MediaType.APPLICATION_JSON).build();
        } catch (SQLException e) {
            throw new WebApplicationException(e, 500);
        } catch (IOException e) {
            throw new WebApplicationException(e, 500);
        }
    }

    @GET
    @Path("/{id}/experiments")
    public Response getExperimentsForProject
        (@PathParam("id") String resourceId, 
         @QueryParam("filter") String filter, 
         @QueryParam("search") String search, 
         @QueryParam("expand") String expand) {

        boolean expandEntries = false;
        if (expand != null && (expand.toLowerCase().equals("true") 
                               || expand.toLowerCase().equals("yes")))
            expandEntries = true;
        DBUtils db = new DBUtils();
        try {
            Project p = db.getProject(Long.valueOf(resourceId));
            List<Experiment> e = new ArrayList<Experiment>();
            for (Long eid : p.getEids()) e.add(db.getExperimentByExptId(eid));
            String json;
            if (countRequested) json = Util.toJson(e.size());
            else if (expandEntries) {
                json = Util.toJson(e);
            }
            else {
                List<String> links = new ArrayList<String>();
                for (Experiment experiment : e) 
                    links.add(experiment.getResourcePath());
                json = Util.toJson(links);
            }
            db.closeConnection();
            return Response.ok(json, MediaType.APPLICATION_JSON).build();
        } catch (SQLException e) {
            throw new WebApplicationException(e, 500);
        } catch (IOException e) {
            throw new WebApplicationException(e, 500);
        }
    }

    @GET
    @Path("/{id}/assays")
    public Response getAssaysForProject
        (@PathParam("id") String resourceId, 
         @QueryParam("filter") String filter, 
         @QueryParam("search") String search, 
         @QueryParam("expand") String expand) {
        boolean expandEntries = false;
        if (expand != null && (expand.toLowerCase().equals("true") 
                               || expand.toLowerCase().equals("yes")))
            expandEntries = true;
        DBUtils db = new DBUtils();
        try {
            Project p = db.getProject(Long.valueOf(resourceId));
            List<Assay> e = new ArrayList<Assay>();
            for (Long aid : p.getAids()) e.add(db.getAssayByAid(aid));
            String json;
            if (countRequested) json = Util.toJson(e.size());
            else if (expandEntries) {
                json = Util.toJson(e);
            }
            else {
                List<String> links = new ArrayList<String>();
                for (Assay assay : e) 
                    links.add(assay.getResourcePath());
                json = Util.toJson(links);
            }
            db.closeConnection();
            return Response.ok(json, MediaType.APPLICATION_JSON).build();
        } catch (SQLException e) {
            throw new WebApplicationException(e, 500);
        } catch (IOException e) {
            throw new WebApplicationException(e, 500);
        }        
    }

    /**
     * Return compounds for a project.
     *
     * @param resourceId
     * @param filter
     * @param search
     * @param expand
     * @return String representation of compounds. Format is specified via Accepts: header and can be
     *         chemical/x-daylight-smiles or chemical/x-mdl-sdfile for SMILES or SDF formats.
     */
    @GET
    @Path("/{id}/probes")
    public Response getProbesForProject(@PathParam("id") String resourceId, @QueryParam("filter") String filter, @QueryParam("search") String search, @QueryParam("expand") String expand) {
        List<MediaType> types = headers.getAcceptableMediaTypes();

        DBUtils db = new DBUtils();
        Response response = null;
        try {
            List<Long> probes = db.getProbeCidsForProject(Long.valueOf(resourceId));
            if (countRequested) response = Response.ok(Util.toJson(probes.size()), MediaType.APPLICATION_JSON).build();
            else if (types.contains(BARDConstants.MIME_SMILES)) {
                List<String> smiles = new ArrayList<String>();
                for (Long probe : probes) {
                    List<Compound> c = db.getCompoundsByCid(probes.toArray(new Long[]{}));
                    for (Compound ac : c) smiles.add(ac.getSmiles());
                }
                response = Response.ok(Util.join(smiles, "\n"), BARDConstants.MIME_SMILES).build();
            } else if (types.contains(BARDConstants.MIME_SDF)) {

            } else {
                List<String> links = new ArrayList<String>();
                for (Long id : probes) links.add(BARDConstants.API_BASE + "/compounds/" + id);
                if (expandEntries(expand)) {
                    List<Compound> cmpds = db.getCompoundsByCid(probes.toArray(new Long[]{}));
                    response = Response.ok(Util.toJson(cmpds), MediaType.APPLICATION_JSON).build();
                } else {
                    response = Response.ok(Util.toJson(links), MediaType.APPLICATION_JSON).build();
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
    @Path("/{pid}/annotations")
    public Response getAnnotations(@PathParam("pid") Long resourceId, @QueryParam("filter") String filter, @QueryParam("expand") String expand) throws ClassNotFoundException, IOException, SQLException {
        DBUtils db = new DBUtils();
        List<CAPAnnotation> a;
        CAPDictionary dict = db.getCAPDictionary();
        try {
            a = db.getProjectAnnotations(resourceId);
            if (a == null) throw new WebApplicationException(404);

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


    @Override
    @GET
    @Path("/etag/{etag}/facets")
    public Response getFacets(@PathParam("etag") String resourceId) {
        DBUtils db = new DBUtils();
        try {
            List<Facet> facets = db.getProjectFacets(resourceId);
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

    @GET
    @Path("/{pid}/summary")
    public Response getSummary(@PathParam("pid") Long projectId) {
        DBUtils db = new DBUtils();

        try {
            Map<String, Object> summary = db.getProjectSumary(projectId);
            if (summary == null) throw new WebApplicationException(404);
            return Response.ok(Util.toJson(summary),
                    MediaType.APPLICATION_JSON).build();
        } catch (Exception e) {
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

    @GET
    @Path("/{pid}/steps")
    public Response getProjectSteps(@PathParam("pid") Long projectId, @QueryParam("expand") String expand) throws SQLException, IOException {
        DBUtils db = new DBUtils();
        List<ProjectStep> steps = db.getProjectStepsByProjectId(projectId);
        if (steps.size() == 0) {
            db.closeConnection();
            throw new WebApplicationException(404);
        }

        String json;
        if (expandEntries(expand)) {
            ObjectMapper mapper = new ObjectMapper();
            ArrayNode anode = mapper.createArrayNode();
            for (ProjectStep step : steps) {
                ObjectNode node = mapper.valueToTree(step);
                Experiment e = db.getExperimentByExptId(step.getNextBardExpt());
                node.put("nextBardExpt", mapper.valueToTree(e));
                e = db.getExperimentByExptId(step.getPrevBardExpt());
                node.put("prevBardExpt", mapper.valueToTree(e));
                anode.add(node);
            }
            json = mapper.writeValueAsString(anode);
        } else {
            json = Util.toJson(steps);
        }
        db.closeConnection();
        return Response.ok(json).type(MediaType.APPLICATION_JSON_TYPE).build();
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
            List<Project> projects = db.getProjectsByETag
                    (skip != null ? skip : -1, top != null ? top : -1, resourceId);

            String json;
            if (expand == null || expand.toLowerCase().equals("false")) {
                List<String> links = new ArrayList<String>();
                for (Project ap : projects)
                    if (ap != null)
                        links.add(ap.getResourcePath());
                json = Util.toJson(links);
            } else json = Util.toJson(projects);
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
