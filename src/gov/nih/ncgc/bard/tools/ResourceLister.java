package gov.nih.ncgc.bard.tools;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * List available REST resources.
 *
 * Currently looks at bard.jar, so you should run ant before running this class
 * <code>
 *     ant jar-rest
 * </code>
 *
 * @author Rajarshi Guha
 */
public class ResourceLister {
    void run() throws IOException, ClassNotFoundException {
        int n = 0;
        int npost = 0, nget = 0;
        JarFile jarFile = new JarFile("deploy/bard.jar");
        Enumeration allEntries = jarFile.entries();
        while (allEntries.hasMoreElements()) {
            JarEntry entry = (JarEntry) allEntries.nextElement();
            if (!entry.getName().contains(".class") || entry.getName().contains("$")) continue;
            String className = entry.getName().replace(".class", "").replace("/", ".");
            Class c = Class.forName(className);
            if (className.startsWith("gov.nih.ncgc.bard.rest")) {
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
                        String res = httpMethod + " " + rootResource + "/" + subResource;
                        res = res.replace("//", "/");
                        System.out.println(res);
                        n++;
                    }
                }

            }
        }
        System.out.println(String.format("Got %d methods with %d GET and %d POST", n, nget, npost));
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        ResourceLister rl = new ResourceLister();
        rl.run();
    }
}
