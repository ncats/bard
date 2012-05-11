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
     *
     * @return a description of the plugin.
     * @GET
     * @Path("/_info") </pre>
     * <p/>
     * where the annotations are from the <code>javax.ws.rs</code> hierarchy.
     */
    public String getDescription();

    /**
     * Get the manifest for this plugin.
     * <p/>
     * This should be an XML document conforming
     * to the plugin manifest specification described
     * <a href="http://foo.bar">here</a>
     *
     * @return an XML document containing the plugin manifest
     */
    public String getManifest();

    /**
     * Get the version for the plugin.
     *
     * @return the plugin version
     */
    public String getVersion();

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
    public String[] getResourcePaths();
}
