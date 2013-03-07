package gov.nih.ncgc.bard.plugin;

/**
 * An interface for BARD API plugins.
 * <p/>
 * This is currently a minimal interface, mainly to
 * support compliance and quality control.
 * <p/>
 * Plugins should employ a specified set of annotations
 * which are checked at submission and deployment time
 * that provide the actual resource as well as extra
 * information. See the BARD plugin
 * <a href="http://plugin.spec">documentation</a> for more
 * details.
 *
 * @author Rajarshi Guha
 */
public interface IPlugin {

    /**
     * Get a description of the plugin.
     * <p/>
     * In the implementing class, this method should be annotated using
     * <p/>
     * <pre>
     * @GET
     * @Path("/_info")
     * @Produces(MediaType.TEXT_PLAIN) </pre>
     * <p/>
     * where the annotations are from the <code>javax.ws.rs</code> hierarchy.
     *
     * @return a description of the plugin.
     */
    public String getDescription();

    /**
     * Get the manifest for this plugin.
     * <p/>
     * This should be an JSON document conforming
     * to the <a href="https://github.com/ncatsdpiprobedev/bardplugins/blob/master/resources/manifest.json">plugin manifest schema</a>
     * <p/>
     * In the implementing class, this method should be annotated using
     * <pre>
     * @GET
     * @Path("/_manifest")
     * @Produces(MediaType.APPLICATION_JSON) </pre>
     * where the annotations are from the <code>javax.ws.rs</code> hierarchy.
     *
     * @return a JSON document containing the plugin manifest
     */
    public String getManifest();
}
