package gov.nih.ncgc.bard.tools;

import gov.nih.ncgc.bard.plugin.IPlugin;

import javax.ws.rs.*;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * A tool to validate BARD plugins.
 *
 * @author Rajarshi Guha
 */
public class PluginValidator {

    private String currentClassName = "";

    private List<String> errors;

    public PluginValidator() {
        errors = new ArrayList<String>();
    }

    public List<String> getErrors() {
        return errors;
    }

    public class ByteArrayClassLoader extends ClassLoader {
        byte[] bytes;

        public ByteArrayClassLoader(byte[] bytes) {
            this.bytes = bytes;
        }

        public Class findClass(String name) {
            return defineClass(name, bytes, 0, bytes.length);
        }
    }


    public boolean validate(String filename) throws IOException, InstantiationException, IllegalAccessException {

        String basename = (new File(filename)).getName();

        boolean atLeastOnePlugin = false;
        boolean status = false;

        ZipFile zf = new ZipFile(filename);
        Enumeration entries = zf.entries();
        while (entries.hasMoreElements()) {
            ZipEntry ze = (ZipEntry) entries.nextElement();
            String entryName = ze.getName();
            if (entryName.endsWith(".class")) {
                BufferedInputStream bis = new BufferedInputStream(zf.getInputStream(ze));
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                int c;
                while ((c = bis.read()) != -1) {
                    baos.write(c);
                }
                bis.close();
                baos.close();
                byte[] bytes = baos.toByteArray();

                String className = entryName.split("\\.")[0].replace("WEB-INF/classes/", "").replace("/", ".");
                ByteArrayClassLoader loader = new ByteArrayClassLoader(bytes);
                Class klass = loader.findClass(className);
                if (implementsPluginInterface(klass)) {
                    status = validate(klass, basename);
                    atLeastOnePlugin = true;
                }
            }
        }
        zf.close();

        if (!atLeastOnePlugin) {
            errors.add("This does not seem to be a BARD plugin as there were no classes implementing the IPlugin interface");
            return false;
        } else return status;
    }

    private boolean implementsPluginInterface(Class klass) {
        boolean implementsInterface = false;
        Class pluginInterface = IPlugin.class;
        Class[] interfaces = klass.getInterfaces();
        for (Class iface : interfaces) {
            if (iface.equals(pluginInterface)) {
                implementsInterface = true;
                break;
            }
        }
        return implementsInterface;
    }

    /**
     * Validate a class that will expose a BARD plugin service.
     *
     * @param klass   The class in question.
     * @param warName If validating via a WAR file, this should be the war file name. Otherwise, null
     * @return true if a valid plugin class, otherwise false.
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    public boolean validate(Class klass, String warName) throws IllegalAccessException, InstantiationException {

        // check appropriate interface
        boolean implementsInterface = implementsPluginInterface(klass);
        if (!implementsInterface) {
            errors.add("Does not implement IPlugin");
        }

        // check that the class has a class level @Path annotation
        // if @Path is present, ensure it matches war file name (if we got one)
        boolean hasClassLevelPathAnnot = false;
        boolean collidesWithRegistry = false;
        boolean matchesWarFileName = false;
        if (klass.isAnnotationPresent(Path.class)) {
            hasClassLevelPathAnnot = true;
            Path annot = (Path) klass.getAnnotation(Path.class);
            String value = annot.value();
            if (value != null && value.indexOf("/plugins/registry") == 0) {
                collidesWithRegistry = true;
            }
            if (warName != null) {
                String[] toks = warName.split("\\.");
                if (toks.length == 2) {
                    String tmp = toks[0].replace("bardplugin_","");
                    matchesWarFileName = tmp.equals(value.replace("/",""));
                }
            }
        }
        if (!hasClassLevelPathAnnot)
            errors.add("Missing the class level @Path annotation");
        if (!matchesWarFileName)
            errors.add("WAR file name does not correspond to @Path annotation");
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

    public static void main(String[] args) throws InstantiationException, IllegalAccessException, IOException {
        PluginValidator v = new PluginValidator();
        boolean status = v.validate("/Users/guhar/src/bard.plugins/csls/deploy/bardplugin_csls.war");
        System.out.println("status = " + status);
        if (!status) {
            for (String s : v.getErrors()) System.out.println(s);
        }
    }
}
