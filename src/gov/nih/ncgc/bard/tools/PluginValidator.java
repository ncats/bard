package gov.nih.ncgc.bard.tools;

import gov.nih.ncgc.bard.plugin.CSLSPlugin;
import gov.nih.ncgc.bard.plugin.IPlugin;
import gov.nih.ncgc.bard.rest.BARDDocumentResource;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
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

    public boolean validate(Class klass) throws IllegalAccessException, InstantiationException {

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
        boolean collidesWithRegistry = false;
        if (klass.isAnnotationPresent(Path.class)) {
            Path annot = (Path) klass.getAnnotation(Path.class);
            String value = annot.value();
            if (value != null && value.indexOf("/plugins/") == 0) {
                hasClassLevelPathAnnot = true;
            }
            if (value != null && value.indexOf("/plugins/registry") == 0) {
                collidesWithRegistry = true;
            }
        }
        if (!hasClassLevelPathAnnot)
            errors.add("Missing the class level @Path annotation or else the annotation did not start with '/plugins/'");
        if (collidesWithRegistry)
            errors.add("Class level @Path annotation cannot start with '/plugins/registry'");

        Method[] methods = klass.getMethods();

        // check for the _info resource
        boolean infoResourcePresent = false;
        for (Method method : methods) {
            if (method.isAnnotationPresent(Path.class)) {
                Path annot = method.getAnnotation(Path.class);
                // make sure the @GET annotation is present and the annotation is on the expected method
                if (annot.value().equals("/_info") && method.getAnnotation(GET.class) != null && method.getName().equals("getDescription")) {
                    infoResourcePresent = true;
                    break;
                }
            }
        }
        if (!infoResourcePresent)
            errors.add("Missing the getDescription() method with @Path(\"/_info\") and @GET annotations");

        // check that we have at least one (public) method that is annotated 
        // with a GET or a POST and has a non null @Path annotation
        // and a @Produces annotations
        // 
        // Note that this check excludes the method annotated with the _info 
        // resource path
        boolean resourcePresent = false;
        for (Method method : methods) {
            if (method.isAnnotationPresent(Path.class)) {
                Path annot = method.getAnnotation(Path.class);
                if (annot.value().equals("/_info")) continue;

                // check for a @GET/@POST/@PUT
                if (method.isAnnotationPresent(GET.class) ||
                        method.isAnnotationPresent(POST.class) ||
                        method.isAnnotationPresent(PUT.class)) {
                    // check for a @Produces
                    if (method.isAnnotationPresent(Produces.class)) {
                        resourcePresent = true;
                        break;
                    }
                }
            }
        }
        if (!resourcePresent)
            errors.add("At least one public method must have a @Path annotation (in addition to the _info resource");

        // ok, now we create the class
        IPlugin plugin = (IPlugin) klass.newInstance();

        // check for a non-null description, version, manifest
        String s = plugin.getDescription();
        if (s == null) errors.add("getDescription() returned a null value");
        s = plugin.getManifest();
        if (s == null) errors.add("getManifest() returned a null value");
        s = plugin.getVersion();
        if (s == null) errors.add("getVersion() returned a null value");

        // validate the manifest document
        return errors.size() == 0;
    }

    public static void main(String[] args) throws InstantiationException, IllegalAccessException {
        PluginValidator v = new PluginValidator();
        CSLSPlugin p = new CSLSPlugin();
        BARDDocumentResource d = new BARDDocumentResource();
        boolean status = v.validate(p.getClass());
        System.out.println("status = " + status);
        if (!status) {
            for (String s : v.getErrors()) System.out.println(s);
        }
    }
}
