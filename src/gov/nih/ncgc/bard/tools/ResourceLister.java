package gov.nih.ncgc.bard.tools;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * List available REST resources.
 * <p/>
 * Looks in the runtime classpath for relevant classes.
 *
 * @author Rajarshi Guha
 */
public class ResourceLister {
    String apiVersion = "v17";

    public ResourceLister(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    List<Class> getClassesByPackage(String packageName) throws IOException, ClassNotFoundException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        assert classLoader != null;
        String path = packageName.replace('.', '/');
        Enumeration<URL> resources = classLoader.getResources(path);
        List<File> dirs = new ArrayList<File>();
        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            dirs.add(new File(resource.getFile()));
        }
        ArrayList<Class> classes = new ArrayList<Class>();
        for (File directory : dirs) {
            classes.addAll(findClasses(directory, packageName));
        }
        return classes;
    }

    private static List<Class> findClasses(File directory, String packageName) throws ClassNotFoundException {
        List<Class> classes = new ArrayList<Class>();
        if (!directory.exists()) {
            return classes;
        }
        File[] files = directory.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                assert !file.getName().contains(".");
                classes.addAll(findClasses(file, packageName + "." + file.getName()));
            } else if (file.getName().endsWith(".class")) {
                classes.add(Class.forName(packageName + '.' + file.getName().substring(0, file.getName().length() - 6)));
            }
        }
        return classes;
    }

    void run() throws IOException, ClassNotFoundException {
        int n = 0;
        int npost = 0, nget = 0;
        List<Class> classes = getClassesByPackage("gov.nih.ncgc.bard");
        for (Class c : classes) {
            if (c.getName().contains("$")) continue;

            if (c.isAnnotationPresent(Path.class)) {
                String rootResource = ((Path) c.getAnnotation(Path.class)).value();

                Method[] methods = c.getMethods();
                for (Method method : methods) {
                    String subResource = "";
                    if (method.isAnnotationPresent(Path.class)) {
                        subResource = method.getAnnotation(Path.class).value();
                    }

                    String httpMethod = null;
                    if (method.getAnnotation(GET.class) != null) {
                        httpMethod = "GET";
                        nget++;
                    } else if (method.getAnnotation(POST.class) != null) {
                        httpMethod = "POST";
                        npost++;
                    }

                    if (httpMethod == null) continue; // ignore a method with no GET/POST
                    String res = rootResource + "/" + subResource;
                    res = res.replace("//", "/");
                    res = "http://bard.nih.gov/api/" + apiVersion + "/" + res;
                    System.out.println("curl -o /dev/null -sL -w \"%{http_code} %{url_effective} %{time_total} %{size_download}\\\\n\"  -X " + httpMethod + " " + res);
                    n++;
                }
            }


        }
        System.out.println(String.format("Got %d methods with %d GET and %d POST", n, nget, npost));
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        ResourceLister rl = new ResourceLister("v17");
        rl.run();
    }
}
