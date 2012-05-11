package gov.nih.ncgc.bard.tools;

/**
 * A tool to validate BARD plugins.
 *
 * @author Rajarshi Guha
 */
public class PluginValidator {

    public boolean validate() {

        // check appropriate interface
        // check that we have at least one resource path
        // check for a non-null description, version
        // check that we get back a manifest document
        // validate the manifest document
        // check for expected annotations and ensure that resource paths are properly defined
        //  check for @GET/@POST
        //  check for @Path (at least one must be present)
        //  check for @Produces (at least one must be present for each method annotated with @GET/@POST)
        return true;
    }
}
