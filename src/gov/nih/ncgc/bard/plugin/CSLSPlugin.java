package gov.nih.ncgc.bard.plugin;

import gov.nih.ncgc.bard.rest.BARDConstants;
import gov.nih.ncgc.bard.tools.Util;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.List;

/**
 * A BARD plugin whose main goal is to retrieve data from an external service.
 * <p/>
 * This can be used as an example of a stub plugin, which offloads the actual
 * job to an external service (which can be implemented in arbitrary) languages
 *
 * @author Rajarshi Guha
 */
@Path("/plugins/csls")
public class CSLSPlugin implements IPlugin {
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
        return "Looks up the SMILES string for a chemical name using the NCI CSLS. Return type is plain text";
    }

    @GET
    @Path("/{term}")
    @Produces("text/plain")
    public Response getTermFromCsls(@PathParam("term") String term) {
        try {
            URL url = new URI("http://cactus.nci.nih.gov/chemical/structure/" + URLEncoder.encode(term, "UTF-8") + "/smiles").toURL();
            URLConnection con = url.openConnection();
            con.connect();
            HttpURLConnection hcon = (HttpURLConnection) con;
            int response = hcon.getResponseCode();
            if (response == 200) {
                StringBuilder sb = new StringBuilder();
                BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String str;
                while ((str = reader.readLine()) != null) sb.append(str);
                reader.close();
                return Response.ok(sb.toString(), MediaType.TEXT_PLAIN).build();
            } else throw new WebApplicationException(response);
        } catch (MalformedURLException e) {
            throw new WebApplicationException(e, 500);
        } catch (URISyntaxException e) {
            throw new WebApplicationException(e, 500);
        } catch (IOException e) {
            throw new WebApplicationException(e, 500);
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
            ret[i] = BARDConstants.API_BASE + "plugins/" + paths.get(i);
        return ret;
    }
}
