package gov.nih.ncgc.bard.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import gov.nih.ncgc.bard.entity.Assay;
import gov.nih.ncgc.bard.entity.ExperimentData;
import gov.nih.ncgc.bard.entity.FitModel;
import gov.nih.ncgc.bard.entity.Project;
import gov.nih.ncgc.bard.rest.rowdef.AssayDefinitionObject;
import gov.nih.ncgc.bard.rest.rowdef.DataResultObject;
import gov.nih.ncgc.bard.rest.rowdef.DoseResponseResultObject;
import gov.nih.ncgc.bard.tools.DBUtils;
import gov.nih.ncgc.bard.tools.Util;

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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Prototype of MLBD REST resources.
 * <p/>
 * This is mainly to explore the use of Jersey for presenting REST
 * services for the MLBD
 *
 * @author Rajarshi Guha
 */
@Path("/exptdata")
public class BARDExperimentDataResource extends BARDResource<ExperimentData> {
    static final Logger logger =
            Logger.getLogger(BARDExperimentDataResource.class.getName());

    public static final String DATE_FORMAT_NOW = "yyyy-MM-dd HH:mm:ss";
    static final String VERSION = "1.0";

    @Context
    ServletContext servletContext;
    @Context
    HttpServletRequest httpServletRequest;
    @Context
    HttpHeaders headers;

    @Override
    public Class<ExperimentData> getEntityClass() {
        return ExperimentData.class;
    }

    @Override
    public String getResourceBase() {
        return BARDConstants.API_BASE + "/exptdata";
    }


    @GET
    @Produces("text/plain")
    @Path("/_info")
    public String info() {
        StringBuilder msg = new StringBuilder("Returns experiment data information\n\nAvailable resources:\n");
        List<String> paths = Util.getResourcePaths(this.getClass());
        for (String path : paths) msg.append(path).append("\n");
        msg.append("/exptdata/" + BARDConstants.API_EXTRA_PARAM_SPEC + "\n");
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
                ret = String.valueOf(db.getEntityCount(ExperimentData.class));
            } else {
                List<ExperimentData> experiments = db.searchForExperimentData(filter, -1, -1);
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
    @Path("/")
    public Response getResources(@QueryParam("filter") String filter,
                                 @QueryParam("expand") String expand,
                                 @QueryParam("skip") Integer skip,
                                 @QueryParam("top") Integer top) {
        boolean expandEntries = false;
        if (expand != null && (expand.toLowerCase().equals("true") || expand.toLowerCase().equals("yes")))
            expandEntries = true;

        DBUtils db = new DBUtils();
        Response response;
        try {
            if (filter == null) {
                if (countRequested) return Response.ok(String.valueOf(db.getEntityCount(ExperimentData.class)), MediaType.TEXT_PLAIN).build();
                else return Response.status(413).build();
            } else {
                List<ExperimentData> experimentData = db.searchForExperimentData(filter, skip, top); // TODO search needs to be reworked
                if (countRequested) return Response.ok(String.valueOf(experimentData.size()), MediaType.TEXT_PLAIN).build();
                if (expandEntries) {
                    String json = Util.toJson(experimentData);
                    response = Response.ok(json, MediaType.APPLICATION_JSON).build();
                } else {
                    List<String> links = new ArrayList<String>();
                    for (ExperimentData a : experimentData) links.add(a.getResourcePath());
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

    /**
     * Get multiple experiment data objects via POST'ing a list of ids.
     * <p/>
     * Currently this method ignores the TID specification of an experiment data identifier. That
     * is it only considers identifiers of the form <code>eid.sid</code>.
     * <p/>
     * The return value only contains experiment data for the identifiers that actually had
     * experiment data. That is, the number of experiment data entries in the response may be
     * less than the number of identifiers specified.
     *
     * @param ids    A comma separated list of experiment data identifiers
     * @param filter A filter string. Currently ignored
     * @param expand If <code>true</code> show detailed respose, else a compact response. Currently ignored
     * @return
     * @throws IOException
     */
    @POST
    @Path("/")
    @Consumes("application/x-www-form-urlencoded")
    public Response getExptDataByIds(@FormParam("sids") String ids,
                                     @FormParam("cids") String cids,
                                     @FormParam("eids") String eids,
                                     @FormParam("aids") String aids,
                                     @FormParam("pids") String pids,
                                     @QueryParam("filter") String filter,
                                     @QueryParam("top") Integer top,
                                     @QueryParam("skip") Integer skip,
                                     @QueryParam("expand") String expand) throws IOException, SQLException {
        List<ExperimentData> edlist = new ArrayList<ExperimentData>();
        List<String> edids = null;
        DBUtils db = new DBUtils();

        if (skip == null) skip = -1;
        if (top == null) top = -1;

        if (cids != null) {
            // if CIDs specified we get back corresponding SIDs and
            // then set them in the ids variable. Implies that that
            // if CIDs are supplied, ids is ignored
            List<Long> sids = new ArrayList<Long>();
            String[] toks = cids.split(",");
            for (String cid : toks) sids.addAll(db.getSidsByCid(Long.parseLong(cid.trim())));
            ids = Util.join(sids, ",");
            logger.info("CIDs were specified. Converted " + toks.length + " CIDs to " + sids.size() + " SIDs");
        }

        if (ids != null && eids == null && aids == null && pids == null) {
            // in this case, id's can be simple SID's in which case we have
            // to find out which experiments they are tested in. Or else, they
            // are of the form eid.sid and we don't need to do anything more
            edids = new ArrayList<String>();
            if (ids.indexOf(".") > 0) {
                Collections.addAll(edids, ids.split(","));
                logger.info("EID.SID specified.");
            } else {
                int nexpt = 0;
                for (String id : ids.split(",")) {
                    List<Long> sEids = db.getSubstanceExperimentIds(Long.parseLong(id.trim()), -1, -1);
                    for (Long asEid : sEids) edids.add(asEid + "." + id);
                    nexpt += sEids.size();
                }
                logger.info("SIDs specified. Converted to " + edids.size() + " EID.SIDs across " + nexpt + " experiments");
            }
        } else if (ids != null && (eids != null || aids != null || pids != null)) {
            // SID's specified and also specific experiment, assay or project
            // is specified in this case, I don't think we need to do any
            // filtering because we've already got a list of SIDs
            logger.info("SIDs specified along with experiments, assays or projects");
            if (eids != null) edids = getEdidFromExperiments(ids.split(","), eids.split(","), skip, top, filter);
            else if (aids != null) edids = getEdidFromAssays(ids.split(","), aids.split(","), skip, top, filter);
            else if (pids != null) edids = getEdidFromProjects(ids.split(","), pids.split(","), skip, top, filter);
        } else if (eids != null || aids != null || pids != null) {
            logger.info("No SIDs specified. Will retrieve all from experiments, assays or projects");
            // no SID's specified. We have to pull relevant SID's from experiment, assays or projects
            if (eids != null) edids = getAllEdidFromExperiments(eids.split(","), skip, top, filter);
            else if (aids != null) edids = getAllEdidFromAssays(aids.split(","), skip, top, filter);
        } else {
            db.closeConnection();
            throw new BadRequestException("If no SID's are specified, must provide one or experiment, assay or project identifiers");
        }

        // pull in the actual data - at this point we should have the filtered (but
        // not sorted) list of experiment data ids
        if (edids != null && edids.size() > 0) {

            logger.info("Will work with " + edids.size() + " edids");

            // we first pull in all edids - this is required in the presence of top/skip
            // since if we apply top/skip to the edid list, it's possible that the remaining
            // edids will not resolve to actual data. Changing the top/skip will include an
            // edid(s) that do resolve to valid data. This behavior is confusing (especially so
            // when the query started with CIDs which would resolve to multiple SIDs).
            //
            // As a result even though this is not good for performance, we pull back results
            // and then apply top/skip. One way to improve performance is to save the full list
            // to cache - as a result paging after the first query (on the same query params)
            // should only pull from cache


            // group the edids by experiment since the db method
            // assumes all SIDs are from a given experiment
            Map<String, List<String>> map = new HashMap<String, List<String>>();
            for (String edid : edids) {
                String eid = edid.split("\\.")[0];
                if (map.containsKey(eid)) {
                    List<String> l = map.get(eid);
                    l.add(edid);
                    map.put(eid, l);
                } else {
                    List<String> l = new ArrayList<String>();
                    l.add(edid);
                    map.put(eid, l);
                }
            }

            edlist = new ArrayList<ExperimentData>();
            for (String eid : map.keySet()) {
                edlist.addAll(db.getExperimentDataByDataId(map.get(eid)));
            }

            // TODO we should do sort on edlist at this point

            if (skip == -1) skip = 0;
            if (top > 0) {
                List<ExperimentData> tmp = new ArrayList<ExperimentData>();

                // if we have N results and are told to skip N, we won't return any
                // otherwise lets skip and take the top
                if (edlist.size() - skip > 0) {
                    int ttop = top;
                    if (ttop > edlist.size()) ttop = edlist.size();

                    int end = skip + ttop;
                    if (end > edlist.size()) end = edlist.size();

                    for (int i = skip; i < end; i++) tmp.add(edlist.get(i));
                }
                edlist = tmp;
            }

            if (countRequested) {
                return Response.ok(String.valueOf(edlist.size()), MediaType.TEXT_PLAIN).build();
            }
        }

        db.closeConnection();
        if (edlist.size() == 0)
            return Response.status(404).entity("No data available for ids: " + ids).type(MediaType.TEXT_PLAIN).build();
        else {
            // we need to convert the JSON strings to a JSON array
            StringBuilder sb = new StringBuilder("[");
            String delim = "";
            for (ExperimentData ed : edlist) {
                sb.append(delim).append(ed.getResultJson());
                delim = ",";
            }
            sb.append("]");
            return Response.ok(sb.toString()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    private List<String> getAllEdidFromAssays(String[] aids, int skip, int top, String filter) throws SQLException {
        List<String> edids = new ArrayList<String>();
        DBUtils db = new DBUtils();
        for (String aid : aids) {
            List<Long> eids = db.getAssayByAid(Long.parseLong(aid)).getExperiments();
            String[] s = new String[eids.size()];
            for (int i = 0; i < eids.size(); i++) s[i] = eids.get(i).toString();
            edids.addAll(getAllEdidFromExperiments(s, skip, top, filter));
        }
        db.closeConnection();
        return edids;
    }

    private List<String> getAllEdidFromExperiments(String[] eids, int skip, int top, String filter) throws SQLException {
        List<String> edids = new ArrayList<String>();
        DBUtils db = new DBUtils();
        for (String eid : eids) {
            edids.addAll(db.getExperimentDataIds(Long.parseLong(eid), skip, top, filter));
        }
        db.closeConnection();
        return edids;
    }

    private List<String> getEdidFromProjects(String[] ids, String[] pids, int skip, int top, String filter) throws SQLException {
        DBUtils db = new DBUtils();
        List<Long> eids = new ArrayList<Long>();
        for (String pid : pids) {
            Project project = db.getProject(Long.parseLong(pid));
            eids.addAll(project.getEids());
        }
        db.closeConnection();
        return getEdidFromExperiments(ids, eids.toArray(new String[0]), skip, top, filter);
    }

    private List<String> getEdidFromAssays(String[] ids, String[] aids, int skip, int top, String filter) throws SQLException {
        DBUtils db = new DBUtils();
        List<Long> eids = new ArrayList<Long>();
        for (String aid : aids) {
            Assay assay = db.getAssayByAid(Long.parseLong(aid));
            eids.addAll(assay.getExperiments());
        }
        db.closeConnection();
        return getEdidFromExperiments(ids, eids.toArray(new String[0]), skip, top, filter);
    }

    private List<String> getEdidFromExperiments(String[] ids, String[] eida, int skip, int top, String filter) {
        List<String> edids = new ArrayList<String>();
        for (String sid : ids) {
            for (String eid : eida) edids.add(eid.trim() + "." + sid.trim());
        }
        return edids;
    }

    private List<ExperimentData> getExperimentData(String[] edids, String filter) throws SQLException {
        DBUtils db = new DBUtils();
        List<ExperimentData> edlist = new ArrayList<ExperimentData>();
        for (String edid : edids) {
            try {
                String exptId = "";
                String[] tokens = edid.split("\\.");
                if (tokens.length < 2) {
                    throw new Exception("Bogus experiment data id: " + edid);
                } else if (tokens.length == 2) {
                    exptId = edid.trim();
                } else {
                    exptId = tokens[0].trim() + "." + tokens[1].trim();
                }
                ExperimentData ed = db.getExperimentDataByDataId(exptId);
                if (ed != null) edlist.add(ed);
            } catch (Exception e) {

            }
        }
        db.closeConnection();
        return edlist;
    }


    @GET
    @Path("/{edid}")
    public Response getResources(@PathParam("edid") String resourceId, @QueryParam("filter") String filter, @QueryParam("expand") String expand) {
        DBUtils db = new DBUtils();
        ExperimentData experimentData;
        try {
            String exptId = "";
            String[] tokens = resourceId.split("\\.");
            if (tokens.length < 2) {

                throw new BadRequestException("Bogus experiment data id: " + resourceId);
            } else if (tokens.length == 2) {
                exptId = resourceId;
            } else {
                exptId = tokens[0] + "." + tokens[1];
            }
            //System.err.println("** "+httpServletRequest.getPathInfo()+": resourceId="+resourceId+" exptId="+exptId);

            experimentData = db.getExperimentDataByDataId(exptId);

            if (experimentData == null || experimentData.getExptDataId() == null)
                return Response.status(404).entity("No data for " + resourceId).type("text/plain").build();

            if (tokens.length == 2) {
                return Response.ok(experimentData.getResultJson(), MediaType.APPLICATION_JSON).build();
            }

            int tid = Integer.parseInt(tokens[2]);
            if (tid != 0) { // only keep the specific entry from readout[]
                FitModel m = experimentData.getReadouts().get(tid - 1);
                experimentData.setReadouts(Arrays.asList(m));
            }
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode root = mapper.createObjectNode();
            root.putPOJO("exptdata", experimentData);

            ArrayNode array = root.putArray("results");
            AssayDefinitionObject[] ado = experimentData.getDefs();
            DataResultObject[] results = experimentData.getResults();

            // check the tid; data tid are stored in column coordinate,
            //  so we need to offset by 8
            if (tid == 0) { // return all?
                for (AssayDefinitionObject d : ado) {
                    if ("DoseResponse".equals(d.getType())) {
                        // ignore dose response
                        continue;
                    }

                    tid = Integer.parseInt(d.getTid());
                    DataResultObject res = null;
                    for (DataResultObject r : results) {
                        if (tid == r.getTid()) {
                            res = r;
                            break;
                        }
                    }

                    if (res == null) {
                        logger.info("no matching result object for tid=" + tid);
                        continue;
                    }

                    ObjectNode node = array.addObject();
                    node.putPOJO("result", d);
                    Object value = res.getValue();
                    if (value instanceof String) {
                        value = ((String) value).replaceAll("\"", "");
                        if ("".equals(value)) {
                            value = null;
                        }
                    }
                    node.putPOJO("value", value);
                }
            } else {
                AssayDefinitionObject def = null;
                for (AssayDefinitionObject d : ado) {
                    if (tid == Integer.parseInt(d.getTid())) {
                        def = d;
                        break;
                    }
                }

                DataResultObject res = null;
                for (DataResultObject r : results) {
                    if (tid == r.getTid()) {
                        res = r;
                        break;
                    }
                }

                ObjectNode node = array.addObject();
                node.putPOJO("result", def);

                if ("DoseResponse".equals(def.getType())) {
                    DoseResponseResultObject drObj = null;
                    for (DoseResponseResultObject dr :
                            experimentData.getDr()) {
                        if (tid == Integer.parseInt(dr.getTid())) {
                            drObj = dr;
                            break;
                        }
                    }

                    node.putPOJO("value", drObj);
                } else {
                    Object value = res.getValue();
                    if (value instanceof String) {
                        value = ((String) value).replaceAll("\"", "");
                        if ("".equals(value)) {
                            value = null;
                        }
                    }
                    node.putPOJO("value", value);
                }
            }

            String json = mapper.writeValueAsString(root);
            //System.err.println("** JSON: "+json);

            return Response.ok(json, MediaType.APPLICATION_JSON).build();
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

}