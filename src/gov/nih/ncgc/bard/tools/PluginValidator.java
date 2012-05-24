package gov.nih.ncgc.bard.tools;

import gov.nih.ncgc.bard.plugin.CSLSPlugin;
import gov.nih.ncgc.bard.plugin.IPlugin;
import gov.nih.ncgc.bard.rest.MLBDDocumentResource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * r
 * A tool to validate BARD plugins.
 *
 * @author Rajarshi Guha
 */
public class PluginValidator {

    private List<String> errors;

    public PluginValidator() {
        errors = new ArrayList<String>();
    }

    public List<String> getErrors() {
        return errors;
    }

    public boolean validate(Class klass) {

        // check appropriate interface
        boolean implementsInterface = false;
        Class pluginInterface = IPlugin.class;
        Class[] interfaces = klass.getInterfaces();
        for (Class iface : interfaces) {
            if (iface.equals(pluginInterface)) {
                implementsInterface = true;
                break;
            }
        }
        if (!implementsInterface) {
            errors.add("Does not implement IPlugin");
        }

        // check that the class has a class level @Path annotation
        // and ensure that it has "/v1/plugins" at the beginning
        boolean hasClassLevelPathAnnot = false;
        if (klass.isAnnotationPresent(Path.class)) {
            Path annot = (Path) klass.getAnnotation(Path.class);
            String value = annot.value();
            if (value != null && value.indexOf("/v1/plugins/") == 0) {
                hasClassLevelPathAnnot = true;
            }
        }
        if (!hasClassLevelPathAnnot)
            errors.add("Missing the class level @Path annotation or else the annotation did not start with '/v1/plugins/'");

        // check that we have at least one (public) method that is annotated 
        // with a GET or a POST and has a non null @Path annotation
        // and a @Produces annotations
        // 
        // Note that this check excludes the method annotated with the _info 
        // resource path
        boolean resourcePresent = false;
        boolean infoResourcePresent = false;
        Method[] methods = klass.getMethods();

        for (Method method : methods) {
            if (method.isAnnotationPresent(Path.class)) {
                Path annot = method.getAnnotation(Path.class);
                // make sure the @GET annotation is present
                if (annot.value().equals("/_info") && method.getAnnotation(GET.class) != null) {
                    infoResourcePresent = true;
                    break;
                }
            }
        }
        if (!infoResourcePresent) errors.add("Missing a method with @Path(\"info\") and @GET annotations");

        for (Method method : methods) {
            if (method.isAnnotationPresent(Path.class)) {
                Path annot = method.getAnnotation(Path.class);
                if (!annot.value().equals("/_info")) {
                    resourcePresent = true;
                    break;
                }
            }
        }
        if (!resourcePresent)
            errors.add("At least one public method must have a @Path annotation (in addition to the _info resource");


        // check for a non-null description, version
        // check that we get back a manifest document
        // validate the manifest document
        // check for expected annotations and ensure that resource paths are properly defined
        //  check for @GET/@POST
        //  check for @Path (at least one must be present)
        //  check for @Produces (at least one must be present for each method annotated with @GET/@POST)

        return errors.size() == 0;
    }

    public static void main(String[] args) {
        PluginValidator v = new PluginValidator();
        CSLSPlugin p = new CSLSPlugin();
        MLBDDocumentResource d = new MLBDDocumentResource();
        boolean status = v.validate(p.getClass());
        System.out.println("status = " + status);
        if (!status) {
            for (String s : v.getErrors()) System.out.println(s);
        }
    }
}
