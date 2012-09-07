package gov.nih.ncgc.bard.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import gov.nih.ncgc.bard.entity.ExperimentData;
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
import java.util.List;

/**
 * Prototype of MLBD REST resources.
 * <p/>
 * This is mainly to explore the use of Jersey for presenting REST
 * services for the MLBD
 *
 * @author Rajarshi Guha
 */
@Path("/exptdata")
public class BARDExperimentDataResource implements IBARDResource {

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
            if (filter == null) { // don't bother returning all experiment_data objects
                response = Response.status(413).build();
            } else {
                List<ExperimentData> experimentData = db.searchForExperimentData(filter, skip, top); // TODO search needs to be reworked
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
     * 
     * Currently this method ignores the TID specification of an experiment data identifier. That
     * is it only considers identifiers of the form <code>eid.sid</code>.
     *
     * The return value only contains experiment data for the identifiers that actually had
     * experiment data. That is, the number of experiment data entries in the response may be
     * less than the number of identifiers specified.
     *
     * @param ids A comma separated list of experiment data identifiers
     * @param filter A filter string. Currently ignored
     * @param expand If <code>true</code> show detailed respose, else a compact response. Currently ignored
     * @return
     * @throws IOException
     */
    @POST
    @Path("/")
    @Consumes("application/x-www-form-urlencoded")
    public Response getExptDataByIds(@FormParam("ids") String ids, @QueryParam("filter") String filter, @QueryParam("expand") String expand) throws IOException {
        if (ids == null || ids.trim().equals("")) throw new BadRequestException("Must specify the ids form field");
        String[] toks = ids.split(",");

        List<ExperimentData> edlist = null;
        try {
            edlist = getExperimentData(toks, filter);
        } catch (SQLException e) {
            return Response.status(500).entity("Error while retrieving experiment data for: "+ids).type(MediaType.TEXT_PLAIN).build();
        }
        if (edlist.size() == 0) return Response.status(404).entity("No data available for ids: "+ids).type(MediaType.TEXT_PLAIN).build();
        else return Response.ok(Util.toJson(edlist)).type(MediaType.APPLICATION_JSON).build();
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

                throw new BadRequestException("Bogus experiment data id: "+resourceId);
            }
            else if (tokens.length == 2) {
                exptId = resourceId;
            }
            else {
                exptId = tokens[0]+"."+tokens[1];
            }
            //System.err.println("** "+httpServletRequest.getPathInfo()+": resourceId="+resourceId+" exptId="+exptId);

            experimentData = db.getExperimentDataByDataId(exptId);

            if (experimentData == null || experimentData.getExptDataId() == null)
                return Response.status(404).entity("No data for "+resourceId).type("text/plain").build();

            if (tokens.length == 2) {
                String json = Util.toJson(experimentData);
                return Response.ok(json, MediaType.APPLICATION_JSON).build();
            }

            //System.err.println("*** "+ Util.toJson(experimentData));

            ObjectMapper mapper = new ObjectMapper ();
            ObjectNode root = mapper.createObjectNode();
            root.putPOJO("exptdata", experimentData);

            ArrayNode array = root.putArray("results");
            AssayDefinitionObject[] ado = experimentData.getDefs();
            DataResultObject[] results = experimentData.getResults();

            // check the tid; data tid are stored in column coordinate,
            //  so we need to offset by 8
            int tid = Integer.parseInt(tokens[2]);
            if (tid == 0) { // return all?
                for (AssayDefinitionObject d : ado) {
                    if ("DoseResponse".equals(d.getType())) {
                        // ignore dose response
                        continue;
                    }

                    tid = Integer.parseInt(d.getTid());
                    DataResultObject res = null;
                    for (DataResultObject r : results) {
                        if (tid == r.getTid() - 7) {
                            res = r;
                            break;
                        }
                    }

                    ObjectNode node = array.addObject();
                    node.putPOJO("result", d);
                    Object value = res.getValue();
                    if (value instanceof String) {
                        value = ((String)value).replaceAll("\"", "");
                        if ("".equals(value)) {
                            value = null;
                        }
                    }
                    node.putPOJO("value", value);
                }
            }
            else {
                AssayDefinitionObject def = null;
                for (AssayDefinitionObject d : ado) {
                    if (tid == Integer.parseInt(d.getTid())) {
                        def = d;
                        break;
                    }
                }

                DataResultObject res = null;
                for (DataResultObject r : results) {
                    if (tid == r.getTid()-7) {
                        res = r;
                        break;
                    }
                }

                ObjectNode node = array.addObject();
                node.putPOJO("result", def);

                if ("DoseResponse".equals(def.getType())) {
                    DoseResponseResultObject drObj= null;
                    for (DoseResponseResultObject dr : 
                             experimentData.getDr()) {
                        if (tid == Integer.parseInt(dr.getTid())) {
                            drObj = dr;
                            break;
                        }
                    }

                    node.putPOJO("value", drObj);
                }
                else {
                    Object value = res.getValue();
                    if (value instanceof String) {
                        value = ((String)value).replaceAll("\"", "");
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
        } 
        catch (Exception e) {
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