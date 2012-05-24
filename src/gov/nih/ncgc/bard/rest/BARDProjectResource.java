package gov.nih.ncgc.bard.rest;

import chemaxon.formats.MolFormatException;
import gov.nih.ncgc.bard.entity.Assay;
import gov.nih.ncgc.bard.entity.Compound;
import gov.nih.ncgc.bard.entity.Project;
import gov.nih.ncgc.bard.entity.ProteinTarget;
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
@Path("/v1/projects")
public class BARDProjectResource implements IBARDResource {

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
        StringBuilder msg = new StringBuilder("Returns project information\n\nAvailable resources:\n");
        List<String> paths = Util.getResourcePaths(this.getClass());
        for (String path : paths) msg.append(path).append("\n");
        msg.append("/v1/projects/" + BARDConstants.API_EXTRA_PARAM_SPEC + "\n");
        return msg.toString();
    }


    @GET
    @Produces("text/plain")
    @Path("/_count")
    public String count(@QueryParam("filter") String filter) {
        DBUtils db = new DBUtils();
        try {
            if (filter == null) {
                int n = db.getProjectCount().size();
                return String.valueOf(n);
            } else { // run the query and return count of results
                List<Project> projects = db.searchForProject(filter);
                return String.valueOf(projects.size());
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
            if (filter == null) { // just list all projects

                List<Long[]> ids = db.getProjectCount();
                if (!expandEntries) {
                    List<String> links = new ArrayList<String>();
                    for (Long[] id : ids) links.add(BARDConstants.API_BASE + "/projects/" + id[0]);
                    return Response.ok(Util.toJson(links), MediaType.APPLICATION_JSON).build();
                } else {
                    List<Project> projects = new ArrayList<Project>();
                    for (Long[] id : ids) projects.add(db.getProjectByAid(id[0]));
                    return Response.ok(Util.toJson(projects), MediaType.APPLICATION_JSON).build();
                }
            } else if (filter != null) {
                List<Project> projects = db.searchForProject(filter);
                if (expandEntries) {
                    String json = Util.toJson(projects);
                    return Response.ok(json, MediaType.APPLICATION_JSON).build();
                } else {
                    List<String> links = new ArrayList<String>();
                    for (Project a : projects)
                        links.add(a.getResourcePath());
                    String json = Util.toJson(links);
                    return Response.ok(json, MediaType.APPLICATION_JSON).build();
                }
            }
        } catch (SQLException e) {
            throw new WebApplicationException(e, 500);
        } catch (IOException e) {
            throw new WebApplicationException(e, 500);
        }
        return getResources(null, filter, expand);
    }

    @GET
    @Path("/{id}")
    public Response getResources(@PathParam("id") String resourceId, @QueryParam("filter") String filter, @QueryParam("expand") String expand) {
        DBUtils db = new DBUtils();
        try {
            Project p = db.getProjectByAid(Long.valueOf(resourceId));
            if (p.getAid() == null) throw new WebApplicationException(404);
            String json = Util.toJson(p);
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
            if (expandEntries) json = Util.toJson(targets);
            else {
                List<String> links = new ArrayList<String>();
                for (ProteinTarget pt : targets) links.add(pt.getResourcePath());
                json = Util.toJson(links);
            }
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
            if (expandEntries) json = Util.toJson(p);
            else {
                List<String> links = new ArrayList<String>();
                for (Assay anAssay : a) links.add(anAssay.getResourcePath());
                json = Util.toJson(links);
            }
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
        try {
            List<Long> probes = db.getProbesForProject(Long.valueOf(resourceId));
            if (types.contains(BARDConstants.MIME_SMILES)) {
                List<String> smiles = new ArrayList<String>();
                for (Long probe : probes) {
                    Compound c = db.getCompoundByCid(probe);
                    smiles.add(c.getSmiles() + "\t" + probe);
                }
                return Response.ok(Util.join(smiles, "\n"), BARDConstants.MIME_SMILES).build();
            } else if (types.contains(BARDConstants.MIME_SDF)) {

            } else {
                List<String> links = new ArrayList<String>();
                for (Long id : probes) links.add(BARDConstants.API_BASE + "/compounds/" + id);
                return Response.ok(Util.toJson(links), MediaType.APPLICATION_JSON).build();
            }
        } catch (SQLException e) {
            throw new WebApplicationException(e, 500);
        } catch (IOException e) {
            throw new WebApplicationException(e, 500);
        }

        return null;
    }
}
