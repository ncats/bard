package gov.nih.ncgc.bard.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonschema.exceptions.ProcessingException;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.github.fge.jsonschema.report.ProcessingMessage;
import com.github.fge.jsonschema.report.ProcessingReport;
import com.github.fge.jsonschema.util.JsonLoader;
import gov.nih.ncgc.bard.plugin.IPlugin;

import javax.ws.rs.*;
import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * A tool to validate BARD plugins.
 * <p/>
 * If used in your own code, you should ensure that the plugin manifest schema
 * is located at <code>/manifest.json</code> in your CLASSPATH. When run from the
 * command line, the schema is bundled with the final JAR file.
 *
 * @author Rajarshi Guha
 */
public class PluginValidator {

    private String[] packagesToIgnore = {"javax.servlet"};

    private String currentClassName = "";

    private ErrorList errors;

    public PluginValidator() {
        errors = new ErrorList();
    }

    public List<String> getErrors() {
        return errors;
    }

    class ErrorList extends ArrayList<String> {
        public void info(String s) {
            add("INFO:\t" + s);
        }

        public void error(String s) {
            add("ERROR:\t" + s);
        }
    }

    class ByteArrayClassLoader extends ClassLoader {
        byte[] bytes;

        public ByteArrayClassLoader(byte[] bytes) {
            this.bytes = bytes;
        }

        public Class findClass(String name) {
            Class klass = null;
            try {
                if (name.startsWith("java")) {
                    System.out.println("trying to load via super");
                    klass = super.findClass(name);
                    System.out.println("  got " + name + " from super");
                } else klass = defineClass(name, bytes, 0, bytes.length);
                resolveClass(klass);
            } catch (IllegalAccessError e) {
                errors.info("Got an IllegalAccessError when loading " + name);
                return null;
            } catch (NoClassDefFoundError e) {
                errors.info("Got an NoClassDefFoundError when loading " + name);
                return null;
            } catch (ClassNotFoundException e) {
                errors.info("Got an ClassNotFound when loading " + name);
                return null;
            }
            return klass;
        }
    }

    // from http://stackoverflow.com/a/9505409
    void extractFolder(String zipFile, String todir) throws IOException {
        int BUFFER = 2048;
        File file = new File(zipFile);

        ZipFile zip = new ZipFile(file);
        String newPath = todir;
        if (newPath == null) newPath = zipFile.substring(0, zipFile.length() - 4);

        Enumeration zipFileEntries = zip.entries();

        // Process each entry
        while (zipFileEntries.hasMoreElements()) {
            // grab a zip file entry
            ZipEntry entry = (ZipEntry) zipFileEntries.nextElement();
            String currentEntry = entry.getName();
            File destFile = new File(newPath, currentEntry);
            //destFile = new File(newPath, destFile.getName());
            File destinationParent = destFile.getParentFile();

            // create the parent directory structure if needed
            destinationParent.mkdirs();

            if (!entry.isDirectory()) {
                BufferedInputStream is = new BufferedInputStream(zip
                        .getInputStream(entry));
                int currentByte;
                // establish buffer for writing file
                byte data[] = new byte[BUFFER];

                // write the current file to disk
                FileOutputStream fos = new FileOutputStream(destFile);
                BufferedOutputStream dest = new BufferedOutputStream(fos,
                        BUFFER);

                // read and write until last byte is encountered
                while ((currentByte = is.read(data, 0, BUFFER)) != -1) {
                    dest.write(data, 0, currentByte);
                }
                dest.flush();
                dest.close();
                is.close();
            } else {
                destFile.mkdirs();
            }
            if (currentEntry.endsWith(".zip")) {
                // found a zip file, try to open
                extractFolder(destFile.getAbsolutePath(), null);
            }
        }
        zip.close();
    }

    void loadJarFile(String filePath) throws IOException {
        URLClassLoader sysLoader;
        URL u;
        Class sysclass;
        try {
            u = new URL("file://" + filePath);
            sysLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
            sysclass = URLClassLoader.class;
            Method method = sysclass.getDeclaredMethod("addURL", new Class[]{URL.class});
            method.setAccessible(true);
            method.invoke(sysLoader, new Object[]{u});
        } catch (Throwable t) {
            t.printStackTrace(System.err);
        }
    }

    private boolean ignoreClass(String className) {
        for (String pkg : packagesToIgnore) {
            if (className.contains(pkg)) return true;
        }
        return false;
    }

    public boolean validate(String filename) throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException {

        String basename = (new File(filename)).getName();

        boolean atLeastOnePlugin = false;
        boolean status = false;

        // extract the war to a temp dir
        int nJar = 0;
        String tempDir = System.getProperty("java.io.tmpdir") + File.separator + "tmp" + System.nanoTime();
        File tempDirFile = new File(tempDir);
        if (!tempDirFile.exists())
            tempDirFile.mkdir();
        extractFolder(filename, tempDir);

        // load JARs and classes we just extracted
        File[] jars = (new File(tempDir + File.separator + "WEB-INF/lib")).listFiles();
        for (File jar : jars) loadJarFile(jar.getAbsolutePath());
        System.out.println("Added " + jars.length + " jars from WEB-INF/lib to the current CLASSPATH");
        loadJarFile(tempDir + File.separator + "WEB-INF/classes/");
        System.out.println("Added class from WEB-INF/classes to current CLASSPATH");

        ZipFile zf = new ZipFile(filename);
        Enumeration entries = zf.entries();
        ByteArrayClassLoader loader;
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
                if (ignoreClass(className)) continue;
                loader = new ByteArrayClassLoader(bytes);
                Class klass = loader.findClass(className);
                if (klass != null && implementsPluginInterface(klass)) {
                    status = validate(klass, basename);
                    atLeastOnePlugin = true;
                }
            } else if (entryName.endsWith(".jar")) { // look for classes in the jar file

                JarInputStream jis = new JarInputStream(zf.getInputStream(ze));
                ZipEntry entry;
                while ((entry = jis.getNextEntry()) != null) {
                    if (!entry.getName().contains(".class")) continue;
                    String className = entry.getName().replace(".class", "").replace("/", ".");
                    if (ignoreClass(className)) continue;
                    if (entry.getSize() <= 0) continue;

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    byte[] bytes = new byte[1024];
                    long nbyte = 0;
                    while (true) {
                        int n = jis.read(bytes);
                        if (n == -1) break;
                        baos.write(bytes, 0, n);
                        nbyte += n;
                    }
                    bytes = baos.toByteArray();
                    loader = new ByteArrayClassLoader(bytes);
                    Class klass = loader.findClass(className);
                    if (klass != null && implementsPluginInterface(klass)) {
                        status = validate(klass, basename);
                        atLeastOnePlugin = true;
                    }
                }
                jis.close();
            }
        }
        zf.close();

        tempDirFile.delete();

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
                    String tmp = toks[0].replace("bardplugin_", "");
                    matchesWarFileName = tmp.equals(value.replace("/", ""));
                }
            }
        }
        if (!hasClassLevelPathAnnot)
            errors.error("Missing the class level @Path annotation");
        if (warName != null && !matchesWarFileName)
            errors.error("WAR file name does not correspond to @Path annotation");
        if (collidesWithRegistry)
            errors.error("Class level @Path annotation cannot start with '/plugins/registry'");

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
            errors.error("Missing the getDescription() method with @Path(\"/_info\") and @GET annotations");

        // check for the _info resource
        boolean manifestResourcePresent = false;
        for (Method method : methods) {
            if (method.isAnnotationPresent(Path.class)) {
                Path annot = method.getAnnotation(Path.class);
                // make sure the @GET annotation is present and the annotation is on the expected method
                if (annot.value().equals("/_manifest") && method.getAnnotation(GET.class) != null && method.getName().equals("getManifest")) {
                    manifestResourcePresent = true;
                    break;
                }
            }
        }
        if (!manifestResourcePresent)
            errors.error("Missing the getManifest() method with @Path(\"/_manifest\") and @GET annotations");


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
            errors.error("At least one public method must have a @Path annotation (in addition to the _info & _manifest resources");

        boolean hasEmptyCtor = false;
        Constructor[] ctors = klass.getConstructors();
        for (Constructor ctor : ctors) {
            if (ctor.getParameterTypes().length == 0) {
                hasEmptyCtor = true;
                break;
            }
        }
        if (!hasEmptyCtor) {
            errors.error("Cannot instantiate plugin because it does not have an empty constructor");
            return false;
        }

        // ok, now we create the class
        IPlugin plugin = (IPlugin) klass.newInstance();

        // check for a non-null description, version, manifest
        String s = plugin.getDescription();
        if (s == null) errors.error("getDescription() returned a null value");
        s = plugin.getManifest();
        if (s == null) errors.error("getManifest() returned a null value");

        // validate the manifest document. First we need to get the manifest schema
        boolean manifestIsValid = false;
        try {
            if (s != null && !s.equals("")) {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode manifestNode = mapper.readTree(s);
                JsonSchemaFactory factory = JsonSchemaFactory.byDefault();

                JsonNode schemaNode = JsonLoader.fromResource("/manifest.json");
                JsonSchema schema = factory.getJsonSchema(schemaNode);

                ProcessingReport report = schema.validate(manifestNode);
                manifestIsValid = report.isSuccess();
                if (!manifestIsValid) {
                    for (ProcessingMessage msg : report) errors.error(msg.getMessage());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (ProcessingException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        if (!manifestIsValid) errors.error("Manifest did not validate");

        return errors.size() == 0;
    }

    public static void main(String[] args) throws Exception {
        boolean printInfo = false;
        boolean printWarn = false;

        if (args.length < 1) {
            System.out.println("\nUsage: java -jar validator.jar bardplugin_FOO.war [-i|-w]");
            System.out.println("\n-i\tPrint INFO messages");
            System.out.println("-w\tPrint WARN messages");
            System.out.println("\nBy default only ERROR messages are reported");
            System.exit(-1);
        }

        for (String arg : args) {
            if (arg.contains("-i")) printInfo = true;
            if (arg.contains("-w")) printWarn = true;
        }

        PluginValidator v = new PluginValidator();
        boolean status = v.validate(args[0]);
//        boolean status = v.validate("/Users/guhar/Downloads/bardplugin_badapple.war");
//        boolean status = v.validate("/Users/guhar/Downloads/bardplugin_hellofromunm.war");
        System.out.println("status = " + status);
        for (String s : v.getErrors()) {
            if (s.startsWith("INFO") && printInfo) System.out.println(s);
            else if (s.startsWith("WARN") && printWarn) System.out.println(s);
            else if (s.startsWith("ERROR")) System.out.println(s);
        }
    }
}
