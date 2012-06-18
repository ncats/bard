package gov.nih.ncgc.bard.rest;

import chemaxon.formats.MolImporter;
import chemaxon.marvin.util.MolExportException;
import chemaxon.struc.Molecule;
import gov.nih.ncgc.bard.entity.BardLinkedEntity;
import gov.nih.ncgc.bard.entity.Compound;
import gov.nih.ncgc.bard.entity.Experiment;
import gov.nih.ncgc.bard.entity.ExperimentData;
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
@Path("/v1/compounds")
public class BARDCompoundResource implements IBARDResource {

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
        StringBuilder msg = new StringBuilder("Returns compound information\n\nAvailable resources:\n");
        List<String> paths = Util.getResourcePaths(this.getClass());
        for (String path : paths) msg.append(path).append("\n");
        return msg.toString();
    }

    /**
     * Return a count of (possibly filtered) instances of a given resource.
     *
     * @param filter A query filter or null
     * @return the number of instances
     */
    @GET
    @Produces("text/plain")
    @Path("/_count")
    public String count(@QueryParam("filter") String filter) {
        DBUtils db = new DBUtils();
        try {
            int n = db.getCompoundCount();
            db.closeConnection();
            return String.valueOf(n);
        } catch (SQLException e) {
            throw new WebApplicationException(e, 500);
        }
    }

    public Response getResources(@QueryParam("filter") String filter,
                                 @QueryParam("expand") String expand,
                                 @QueryParam("skip") Integer skip,
                                 @QueryParam("top") Integer top) {
        return getResources(null, filter, expand);
    }

    private Response getCompoundResponse(String id, String type, List<MediaType> mediaTypes) throws SQLException, IOException, MolExportException {
        DBUtils db = new DBUtils();

        if (!type.equals("cid") && !type.equals("probeid") && !type.equals("sid")) return null;
        Compound c = null;
        if (type.equals("cid")) c = db.getCompoundByCid(Long.parseLong(id));
        else if (type.equals("probeid")) c = db.getCompoundByProbeId(id);
        else if (type.equals("sid")) c = db.getCompoundBySid(Long.parseLong(id));
        db.closeConnection();

        if (c == null || c.getCid() == null) throw new WebApplicationException(404);

        if (mediaTypes.contains(BARDConstants.MIME_SMILES)) {
            String smiles = c.getSmiles() + "\t" + id;
            return Response.ok(smiles, BARDConstants.MIME_SMILES).build();
        } else if (mediaTypes.contains(BARDConstants.MIME_SDF)) {
            Molecule mol = MolImporter.importMol(c.getSmiles());
            mol.setProperty("cid", String.valueOf(c.getCid()));
            mol.setProperty("probeId", c.getProbeId());
            mol.setProperty("url", c.getUrl());
            mol.setProperty("resourecePath", c.getResourcePath());
            String sdf = mol.exportToFormat("sdf");
            return Response.ok(sdf, BARDConstants.MIME_SDF).build();
        } else {
            String json = c.toJson();
            return Response.ok(json, MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Path("/{cid}")
    public Response getResources(@PathParam("cid") String resourceId, @QueryParam("filter") String filter, @QueryParam("expand") String expand) {
        try {
            return getCompoundResponse(resourceId, "cid", headers.getAcceptableMediaTypes());
        } catch (SQLException e) {
            throw new WebApplicationException(e, 500);
        } catch (IOException e) {
            throw new WebApplicationException(e, 500);
        }
    }

    @GET
    @Path("/sid/{sid}")
    public Response getCompoundBySid(@PathParam("sid") String resourceId, @QueryParam("filter") String filter, @QueryParam("expand") String expand) {
        try {
            return getCompoundResponse(resourceId, "sid", headers.getAcceptableMediaTypes());
        } catch (SQLException e) {
            throw new WebApplicationException(e, 500);
        } catch (IOException e) {
            throw new WebApplicationException(e, 500);
        }
    }

    @GET
    @Path("/probeid/{pid}")
    public Response getCompoundByProbeid(@PathParam("pid") String resourceId, @QueryParam("filter") String filter, @QueryParam("expand") String expand) {
        try {
            return getCompoundResponse(resourceId, "probeid", headers.getAcceptableMediaTypes());
        } catch (SQLException e) {
            throw new WebApplicationException(e, 500);
        } catch (IOException e) {
            throw new WebApplicationException(e, 500);
        }
    }

    // return alle xperiment data for this CID
    @GET
    @Path("/{cid}/exptdata")
    public Response getExperimentData(@PathParam("cid") String resourceId,
                                      @QueryParam("filter") String filter,
                                      @QueryParam("expand") String expand,
                                      @QueryParam("skip") Integer skip,
                                      @QueryParam("top") Integer top) {
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
                if (skip + top <= experiemnt.getSubstances())
                    linkString = BARDConstants.API_BASE + "/compounds/" + resourceId + "/exptdata?skip=" + (skip + top) + "&top=" + top + "&" + expandClause;
            }

            String json;
            if (!expandEntries) {
                List<Long> edids = db.getCompoundDataIds(Long.valueOf(resourceId), skip, top);
                List<String> links = new ArrayList<String>();
                for (Long edid : edids) {
                    ExperimentData ed = new ExperimentData();
                    ed.setExptDataId(edid);
                    links.add(ed.getResourcePath());
                }
                BardLinkedEntity linkedEntity = new BardLinkedEntity(links, linkString);
                json = Util.toJson(linkedEntity);
            } else {
                List<ExperimentData> data = db.getCompoundData(Long.valueOf(resourceId), skip, top);
                BardLinkedEntity linkedEntity = new BardLinkedEntity(data, linkString);
                json = Util.toJson(linkedEntity);
            }
            db.closeConnection();
            return Response.ok(json, MediaType.APPLICATION_JSON).build();
        } catch (SQLException e) {
            throw new WebApplicationException(e, 500);
        } catch (IOException e) {
            throw new WebApplicationException(e, 500);
        }
    }
}