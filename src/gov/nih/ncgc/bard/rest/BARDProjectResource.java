package gov.nih.ncgc.bard.rest;

import gov.nih.ncgc.bard.entity.Assay;
import gov.nih.ncgc.bard.entity.Compound;
import gov.nih.ncgc.bard.entity.Project;
import gov.nih.ncgc.bard.entity.ProteinTarget;
import gov.nih.ncgc.bard.tools.DBUtils;
import gov.nih.ncgc.bard.tools.Util;

import javax.ws.rs.GET;
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
@Path("/v1/projects")
public class BARDProjectResource extends BARDResource {

    public static final String DATE_FORMAT_NOW = "yyyy-MM-dd HH:mm:ss";
    static final String VERSION = "1.0";

    @GET
    @Produces("text/plain")
    @Path("/_info")
    public String info() {
        StringBuilder msg = new StringBuilder("Returns project information\n\nAvailable resources:\n");
        List<String> paths = Util.getResourcePaths(this.getClass());
        for (String path : paths) msg.append(path).append("\n");
        msg.append("/v1/projects/" + BARDConstants.API_EXTRA_PARAM_SPEC + "\n");
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

        DBUtils db = new DBUtils();
        Response response = null;
        try {
            if (filter == null) { // just list all projects
                List<Long> ids = db.getProjectIds();
                if (countRequested) response = Response.ok(String.valueOf(ids.size()), MediaType.TEXT_PLAIN).build();
                else if (!expandEntries) {
                    List<String> links = new ArrayList<String>();
                    for (Long id : ids) links.add(BARDConstants.API_BASE + "/projects/" + id);
                    response = Response.ok(Util.toJson(links), MediaType.APPLICATION_JSON).build();
                } else {
                    List<Project> projects = new ArrayList<Project>();
                    for (Long id : ids) projects.add(db.getProjectByAid(id));
                    response = Response.ok(Util.toJson(projects), MediaType.APPLICATION_JSON).build();
                }
            } else {
                List<Project> projects = db.searchForEntity(filter, skip, top, Project.class);
                if (countRequested) response = Response.ok(projects.size(), MediaType.TEXT_PLAIN).build();
                else if (expandEntries) {
                    String json = Util.toJson(projects);
                    response = Response.ok(json, MediaType.APPLICATION_JSON).build();
                } else {
                    List<String> links = new ArrayList<String>();
                    for (Project a : projects)
                        links.add(a.getResourcePath());
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
    @Path("/{id}")
    public Response getResources(@PathParam("id") String resourceId, @QueryParam("filter") String filter, @QueryParam("expand") String expand) {
        DBUtils db = new DBUtils();
        try {
            Project p = db.getProjectByAid(Long.valueOf(resourceId));
            if (p.getAid() == null) throw new WebApplicationException(404);
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

    // TODO only list targets associated with the summary assay, not the member assays. Correct?
    @GET
    @Path("/{id}/targets")
    public Response getTargetsForProject(@PathParam("id") String resourceId, @QueryParam("filter") String filter, @QueryParam("search") String search, @QueryParam("expand") String expand) {
        boolean expandEntries = false;
        if (expand != null && (expand.toLowerCase().equals("true") || expand.toLowerCase().equals("yes")))
            expandEntries = true;
        DBUtils db = new DBUtils();
        try {
            Project project = db.getProjectByAid(Long.valueOf(resourceId));
            List<ProteinTarget> targets = new ArrayList<ProteinTarget>();
            for (ProteinTarget target : project.getTargets())
                targets.add(db.getProteinTargetByAccession(target.getAcc()));
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
    @Path("/{id}/assays")
    public Response getAssaysForProject(@PathParam("id") String resourceId, @QueryParam("filter") String filter, @QueryParam("search") String search, @QueryParam("expand") String expand) {
        boolean expandEntries = false;
        if (expand != null && (expand.toLowerCase().equals("true") || expand.toLowerCase().equals("yes")))
            expandEntries = true;
        DBUtils db = new DBUtils();
        try {
            Project p = db.getProjectByAid(Long.valueOf(resourceId));
            List<Assay> a = new ArrayList<Assay>();
            for (Long aid : p.getAids()) a.add(db.getAssayByAid(aid));
            String json;
            if (countRequested) json = Util.toJson(a.size());
            else if (expandEntries) json = Util.toJson(p);
            else {
                List<String> links = new ArrayList<String>();
                for (Assay anAssay : a) links.add(anAssay.getResourcePath());
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
     * @throws MolFormatException
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
                    Compound c = db.getCompoundByCid(probe);
                    smiles.add(c.getSmiles() + "\t" + probe);
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
