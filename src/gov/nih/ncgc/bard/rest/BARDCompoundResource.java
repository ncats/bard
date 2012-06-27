package gov.nih.ncgc.bard.rest;

import gov.nih.ncgc.bard.entity.BardLinkedEntity;
import gov.nih.ncgc.bard.entity.Compound;
import gov.nih.ncgc.bard.entity.Experiment;
import gov.nih.ncgc.bard.entity.ExperimentData;
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
import java.util.Arrays;
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
@Path("/v1/compounds")
public class BARDCompoundResource extends BARDResource {

    public static final String DATE_FORMAT_NOW = "yyyy-MM-dd HH:mm:ss";
    static final String VERSION = "1.0";

    @GET
    @Produces("text/plain")
    @Path("/_info")
    public String info() {
        StringBuilder msg = new StringBuilder("Returns compound information\n\nAvailable resources:\n");
        List<String> paths = Util.getResourcePaths(this.getClass());
        for (String path : paths) msg.append(path).append("\n");
        return msg.toString();
    }

    public Response getResources(@QueryParam("filter") String filter,
                                 @QueryParam("expand") String expand,
                                 @QueryParam("skip") Integer skip,
                                 @QueryParam("top") Integer top) {
        return getResources(null, filter, expand);
    }

    @GET
    public Response getAll(@QueryParam("filter") String filter,
                           @QueryParam("expand") String expand,
                           @QueryParam("skip") Integer skip,
                           @QueryParam("top") Integer top) throws SQLException {
        DBUtils db = new DBUtils();
        Response response = null;
        if (filter == null) {
            if (countRequested)
                response = Response.ok(String.valueOf(db.getEntityCount(Compound.class))).build();
            else {
                // make a paged response of all substances
            }
        }
        db.closeConnection();
        return response;
    }

    private Response getCompoundResponse(String id, String type, List<MediaType> mediaTypes, boolean expand) throws SQLException, IOException {
        DBUtils db = new DBUtils();

        List<String> validTypes = Arrays.asList("cid", "sid", "probeid", "name");

        if (!validTypes.contains(type)) return null;
        List<Compound> c = new ArrayList<Compound>();
        if (type.equals("cid")) c.add(db.getCompoundByCid(Long.parseLong(id)));
        else if (type.equals("probeid")) c.add(db.getCompoundByProbeId(id));
        else if (type.equals("sid")) c.add(db.getCompoundBySid(Long.parseLong(id)));
        else if (type.equals("name")) c.addAll(db.getCompoundByName(id));
        db.closeConnection();

        if (c.size() == 0) throw new WebApplicationException(404);

        if (mediaTypes.contains(BARDConstants.MIME_SMILES)) {
            StringBuilder s = new StringBuilder();
            for (Compound ac : c) s.append(ac.getSmiles() + "\t" + ac.getCid());
            return Response.ok(s, BARDConstants.MIME_SMILES).build();
        } else if (mediaTypes.contains(BARDConstants.MIME_SDF)) {   // TODO handle multi-molecule SDFs
            throw new WebApplicationException(406);
//            Molecule mol = MolImporter.importMol(c.getSmiles());
//            mol.setProperty("cid", String.valueOf(c.getCid()));
//            mol.setProperty("probeId", c.getProbeId());
//            mol.setProperty("url", c.getUrl());
//            mol.setProperty("resourecePath", c.getResourcePath());
//            String sdf = mol.exportToFormat("sdf");
//            return Response.ok(sdf, BARDConstants.MIME_SDF).build();
        } else {
            String json;
            if (!type.equals("name") && c.size() == 1) json = c.get(0).toJson();
            else {
                if (expand) json = Util.toJson(c);
                else {
                    List<String> links = new ArrayList<String>();
                    for (Compound ac : c) links.add(ac.getResourcePath());
                    json = Util.toJson(links);
                }
            }

            return Response.ok(json, MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Path("/{cid}")
    public Response getResources(@PathParam("cid") String resourceId, @QueryParam("filter") String filter, @QueryParam("expand") String expand) {
        try {
            Response response = getCompoundResponse(resourceId, "cid", headers.getAcceptableMediaTypes(), true);
            if (countRequested && response != null) return Response.ok("1", MediaType.TEXT_PLAIN).build();
            else return response;
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
            Response response = getCompoundResponse(resourceId, "sid", headers.getAcceptableMediaTypes(), true);
            if (countRequested && response != null) return Response.ok("1", MediaType.TEXT_PLAIN).build();
            else return response;
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
            Response response = getCompoundResponse(resourceId, "probeid", headers.getAcceptableMediaTypes(), true);
            if (countRequested && response != null) return Response.ok("1", MediaType.TEXT_PLAIN).build();
            else return response;
        } catch (SQLException e) {
            throw new WebApplicationException(e, 500);
        } catch (IOException e) {
            throw new WebApplicationException(e, 500);
        }
    }

    @GET
    @Path("/name/{name}")
    public Response getCompoundByName(@PathParam("name") String name, @QueryParam("filter") String filter, @QueryParam("expand") String expand) {
        try {
            Response response = getCompoundResponse(name, "name", headers.getAcceptableMediaTypes(), expand != null && expand.toLowerCase().equals("true"));
            if (countRequested && response != null) return Response.ok("1", MediaType.TEXT_PLAIN).build();
            else return response;
        } catch (SQLException e) {
            throw new WebApplicationException(e, 500);
        } catch (IOException e) {
            throw new WebApplicationException(e, 500);
        }
    }

    @POST
    @Path("/name/")
    @Consumes("application/x-www-form-urlencoded")
    public Response getCompoundByNameList(@FormParam("names") String names, @QueryParam("expand") String expand) {
        if (names == null || names.trim().equals("")) throw new WebApplicationException(400);
        String[] toks = names.trim().split(",");
        Map<String, List<Compound>> map = new HashMap<String, List<Compound>>();
        DBUtils db = new DBUtils();
        Response response = null;
        try {
            for (String tok : toks) map.put(tok.trim(), db.getCompoundByName(tok.trim()));
            db.closeConnection();
            if (expandEntries(expand)) response = Response.ok(Util.toJson(map), MediaType.APPLICATION_JSON).build();
            else {
                Map<String, List<String>> lmap = new HashMap<String, List<String>>();
                for (String key : map.keySet()) {
                    List<Compound> compounds = map.get(key);
                    List<String> links = new ArrayList<String>();
                    for (Compound c : compounds) links.add(c.getResourcePath());
                    lmap.put(key, links);
                }
                response = Response.ok(Util.toJson(lmap), MediaType.APPLICATION_JSON).build();
            }
        } catch (SQLException e) {
            throw new WebApplicationException(e, 500);
        } catch (IOException e) {
            throw new WebApplicationException(e, 500);
        }
        return response;
    }

    // return alle xperiment data for this CID
    @GET
    @Path("/{cid}/exptdata")
    public Response getExperimentData(@PathParam("cid") String resourceId,
                                      @QueryParam("filter") String filter,
                                      @QueryParam("expand") String expand,
                                      @QueryParam("skip") Integer skip,
                                      @QueryParam("top") Integer top) {

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
                if (expandEntries(expand)) expandClause = "expand=true";
                if (skip + top <= experiemnt.getSubstances())
                    linkString = BARDConstants.API_BASE + "/compounds/" + resourceId + "/exptdata?skip=" + (skip + top) + "&top=" + top + "&" + expandClause;
            }

            String json;
            if (!expandEntries(expand)) {
                List<Long> edids = db.getCompoundDataIds(Long.valueOf(resourceId), skip, top);
                if (countRequested) json = String.valueOf(edids.size());
                else {
                    List<String> links = new ArrayList<String>();
                    for (Long edid : edids) {
                        ExperimentData ed = new ExperimentData();
                        ed.setExptDataId(edid);
                        links.add(ed.getResourcePath());
                    }
                    BardLinkedEntity linkedEntity = new BardLinkedEntity(links, linkString);
                    json = Util.toJson(linkedEntity);
                }
            } else {
                List<ExperimentData> data = db.getCompoundData(Long.valueOf(resourceId), skip, top);
                if (countRequested) json = String.valueOf(data.size());
                else {
                    BardLinkedEntity linkedEntity = new BardLinkedEntity(data, linkString);
                    json = Util.toJson(linkedEntity);
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

    @GET
    @Path("/{cid}/experiments")
    public Response getExperiments(@PathParam("cid") String resourceId,
                                   @QueryParam("filter") String filter,
                                   @QueryParam("expand") String expand,
                                   @QueryParam("skip") Integer skip,
                                   @QueryParam("top") Integer top) {
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
                if (expandEntries(expand)) expandClause = "expand=true";
                if (skip + top <= experiemnt.getSubstances())
                    linkString = BARDConstants.API_BASE + "/compounds/" + resourceId + "/experiments?skip=" + (skip + top) + "&top=" + top + "&" + expandClause;
            }

            String json;
            if (!expandEntries(expand)) {
                List<Long> eids = db.getCompoundExperimentIds(Long.valueOf(resourceId), skip, top);
                if (countRequested) json = String.valueOf(eids.size());
                else {
                    List<String> links = new ArrayList<String>();
                    for (Long eid : eids) {
                        Experiment ed = new Experiment();
                        ed.setExptId(eid);
                        links.add(ed.getResourcePath());
                    }
                    BardLinkedEntity linkedEntity = new BardLinkedEntity(links, linkString);
                    json = Util.toJson(linkedEntity);
                }
            } else {
                List<Experiment> data = db.getCompoundExperiment(Long.valueOf(resourceId), skip, top);
                if (countRequested) json = String.valueOf(data.size());
                else {
                    BardLinkedEntity linkedEntity = new BardLinkedEntity(data, linkString);
                    json = Util.toJson(linkedEntity);
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
}