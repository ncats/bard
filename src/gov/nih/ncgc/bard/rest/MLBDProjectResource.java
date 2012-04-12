package gov.nih.ncgc.bard.rest;

import chemaxon.formats.MolFormatException;
import chemaxon.formats.MolImporter;
import chemaxon.struc.Molecule;
import gov.nih.ncgc.bard.entity.Assay;
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
public class MLBDProjectResource implements IMLBDResource {

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
    @Path("info")
    public String info() {
        StringBuilder msg = new StringBuilder("Returns project information\n\nAvailable resources:\n");
        List<String> paths = Util.getResourcePaths(this.getClass());
        for (String path : paths) msg.append(path).append("\n");
        msg.append("/v1/projects/?search=[field:]query_string&expand=true|false\n");
        return msg.toString();
    }

    @GET
    public Response getResources(@QueryParam("filter") String filter, @QueryParam("search") String search, @QueryParam("expand") String expand) {
        boolean expandEntries = false;
        if (expand != null && (expand.toLowerCase().equals("true") || expand.toLowerCase().equals("yes")))
            expandEntries = true;

        DBUtils db = new DBUtils();
        try {
            if (filter == null && search == null) { // just list all projects

                List<Long[]> ids = db.getProjectCount();
                if (!expandEntries) {
                    List<String> links = new ArrayList<String>();
                    for (Long[] id : ids) links.add(MLBDConstants.API_BASE + "/projects/" + id[0]);
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
        return getResources(null, filter, search, expand);
    }

    @GET
    @Path("/{id}")
    public Response getResources(@PathParam("id") String resourceId, @QueryParam("filter") String filter, @QueryParam("search") String search, @QueryParam("expand") String expand) {
        DBUtils db = new DBUtils();
        try {
            Project p = db.getProjectByAid(Long.valueOf(resourceId));
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
    @Path("/{id}/compounds")
    public Response getCompoundsForProject(@PathParam("name") String resourceId, @QueryParam("filter") String filter, @QueryParam("search") String search, @QueryParam("expand") String expand) {
        boolean expandEntries = false;
        if (expand != null && (expand.toLowerCase().equals("true") || expand.toLowerCase().equals("yes")))
            expandEntries = true;

        String ret = "";
        List<MediaType> types = headers.getAcceptableMediaTypes();

        Molecule mol;
        try {
            mol = MolImporter.importMol("C1CCCCC1");
        } catch (MolFormatException e) {
            throw new WebApplicationException(e, 500);
        }
        mol.setName("molid1");
        if (types.contains(MLBDConstants.MIME_SMILES) || types.contains(MediaType.TEXT_HTML)) {
            ret = mol.toFormat("smiles");
        } else if (types.contains(MLBDConstants.MIME_SDF)) {
            ret = mol.toFormat("sdf");
        }
        Response.ResponseBuilder builder = Response.ok(ret, types.get(0));
        return builder.build();
    }
}
