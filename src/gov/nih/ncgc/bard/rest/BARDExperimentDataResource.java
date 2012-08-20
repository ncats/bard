package gov.nih.ncgc.bard.rest;

import gov.nih.ncgc.bard.entity.ExperimentData;
import gov.nih.ncgc.bard.tools.DBUtils;
import gov.nih.ncgc.bard.tools.Util;
import gov.nih.ncgc.bard.rest.rowdef.DataResultObject;
import gov.nih.ncgc.bard.rest.rowdef.AssayDefinitionObject;

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

import java.io.StringWriter;
import com.fasterxml.jackson.databind.node.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonFactory;

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

    @GET
    @Path("/{edid}")
    public Response getResources(@PathParam("edid") String resourceId, @QueryParam("filter") String filter, @QueryParam("expand") String expand) {
        DBUtils db = new DBUtils();
        ExperimentData experimentData;
        try {
            String exptId = "";
            String[] tokens = resourceId.split("\\.");
            if (tokens.length < 2) {
                throw new IllegalArgumentException 
                    ("Bogus experiment data id: "+resourceId);
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
                throw new WebApplicationException(404);

            experimentData.transform();
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

            // check the tid
            int tid = Integer.parseInt(tokens[2]);
            if (tid == 0) { // return all?
                for (AssayDefinitionObject d : ado) {
                    tid = Integer.parseInt(d.getTid());
                    DataResultObject res = null;
                    for (DataResultObject r : results) {
                        if (tid == r.getTid()) {
                            res = r;
                            break;
                        }
                    }

                    ObjectNode node = array.addObject();
                    node.putPOJO("type", d);
                    node.putPOJO("value", res);
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
                    if (tid == r.getTid()) {
                        res = r;
                        break;
                    }
                }

                ObjectNode node = array.addObject();
                node.putPOJO("type", def);
                node.putPOJO("value", res);
            }

            String json = mapper.writeValueAsString(root);
            //System.err.println("** JSON: "+json);
            
            return Response.ok(json, MediaType.APPLICATION_JSON).build();
        } 
        catch (Exception e) {
            e.printStackTrace();
            throw new WebApplicationException(e, 500);
        }
    }
}