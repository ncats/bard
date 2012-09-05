package gov.nih.ncgc.bard.rest;

import gov.nih.ncgc.bard.entity.BardLinkedEntity;
import gov.nih.ncgc.bard.entity.Compound;
import gov.nih.ncgc.bard.entity.Experiment;
import gov.nih.ncgc.bard.entity.Assay;
import gov.nih.ncgc.bard.entity.Project;
import gov.nih.ncgc.bard.entity.ProteinTarget;
import gov.nih.ncgc.bard.tools.DBUtils;
import gov.nih.ncgc.bard.tools.Util;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
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
@Path("/projects")
public class BARDProjectResource extends BARDResource<Project> {

    public static final String DATE_FORMAT_NOW = "yyyy-MM-dd HH:mm:ss";
    static final String VERSION = "1.0";

    public Class<Project> getEntityClass () { return Project.class; }

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
            String json = Util.toJson(p);
            if (countRequested) json = Util.toJson("1");
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
        boolean expandEntries = false;
        if (expand != null && (expand.toLowerCase().equals("true") || expand.toLowerCase().equals("yes")))
            expandEntries = true;

        List<MediaType> types = headers.getAcceptableMediaTypes();

        DBUtils db = new DBUtils();
        Response response = null;
        try {
            List<Long> probes = db.getProbesForProject(Long.valueOf(resourceId));
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
                response = Response.ok(Util.toJson(links), MediaType.APPLICATION_JSON).build();
            }
            db.closeConnection();
            return response;
        } catch (SQLException e) {
            throw new WebApplicationException(e, 500);
        } catch (IOException e) {
            throw new WebApplicationException(e, 500);
        }
    }
}
