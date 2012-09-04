package gov.nih.ncgc.bard.plugin;

import gov.nih.ncgc.bard.entity.Assay;
import gov.nih.ncgc.bard.rest.BARDConstants;
import gov.nih.ncgc.bard.tools.DBUtils;
import gov.nih.ncgc.bard.tools.Util;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.sql.SQLException;
import java.util.List;

/**
 * A BARD plugin implementing trivial assay related operations.
 *
 * @author Rajarshi Guha
 */
@Path("/plugins/ainfo")
public class AssayInfoPlugin implements IPlugin {
    private static final String VERSION = "1.0";

    /**
     * Get a description of the plugin.
     *
     * @return a description of the plugin.
     */
    @GET
    @Produces("text/plain")
    @Path("/_info")
    public String getDescription() {
        return "description of the plugin, including inputs, outputs and media types";
    }

    @GET
    @Path("/{aid}/title")
    public Response getAssayTitle(@PathParam("aid") Long aid) {
        DBUtils db = new DBUtils();
        try {
            Assay assay = db.getAssayByAid(aid);
            db.closeConnection();
            return Response.ok(assay.getName(), MediaType.TEXT_PLAIN).build();
        } catch (SQLException e) {
            throw new WebApplicationException(500);
        }
    }


    @GET
    @Path("/{aid}/description")
    public Response getAssayDescription(@PathParam("aid") Long aid) {
        DBUtils db = new DBUtils();
        try {
            Assay assay = db.getAssayByAid(aid);
            db.closeConnection();
            return Response.ok(assay.getDescription(), MediaType.TEXT_PLAIN).build();
        } catch (SQLException e) {
            throw new WebApplicationException(500);
        }
    }

    /**
     * Get the manifest for this plugin.
     * <p/>
     * This should be an XML document conforming
     * to the plugin manifest specification described
     * <a href="http://foo.bar">here</a>
     *
     * @return an XML document containing the plugin manifest
     */
    public String getManifest() {
        return "";
    }

    /**
     * Get the version for the plugin.
     *
     * @return the plugin version
     */
    public String getVersion() {
        return VERSION;
    }

    /**
     * Get the REST resource paths for this plugin.
     * <p/>
     * The paths should omit the BARD prefix. Multiple paths
     * can be returned (and should ideally be documented via the
     * description, though this is currently not enforced).
     * <p/>
     * A plugin must return at least one resource path. If not
     * it will fail the compliance checks
     *
     * @return an array of REST resource paths
     */
    public String[] getResourcePaths() {
        List<String> paths = Util.getResourcePaths(this.getClass());
        String[] ret = new String[paths.size()];
        for (int i = 0; i < paths.size(); i++)
            ret[i] = BARDConstants.API_BASE + paths.get(i);
        return ret;
    }
}
