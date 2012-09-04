package gov.nih.ncgc.bard.rest;

import gov.nih.ncgc.bard.plugin.IPlugin;
import gov.nih.ncgc.bard.tools.Util;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * A one line summary.
 *
 * @author Rajarshi Guha
 */
@Path("/plugins/registry")
public class BARDPluginRegistryResource extends BARDResource {

    @GET
    @Produces("text/plain")
    @Path("/_info")
    public String info() {
        StringBuilder msg = new StringBuilder("Lists available plugins\n\nAvailable resources:\n");
        List<String> paths = Util.getResourcePaths(this.getClass());
        for (String path : paths) msg.append(path).append("\n");
        msg.append("/plugins/" + BARDConstants.API_EXTRA_PARAM_SPEC + "\n");
        return msg.toString();
    }

    @GET
    @Path("/")
    @Produces("application/json")
    public Response getResources(@QueryParam("filter") String filter, @QueryParam("expand") String expand, @QueryParam("skip") Integer skip, @QueryParam("top") Integer top) {
        List<IPlugin> pluginClasses = new ArrayList<IPlugin>();


        String path = servletConfig.getServletContext().getRealPath("/");
        File dir = new File(path + File.separator + "WEB-INF" + File.separator + "lib");
        for (File file : dir.listFiles()) {
            try {
                JarFile jarFile = new JarFile(file.getAbsolutePath());
                Enumeration allEntries = jarFile.entries();
                while (allEntries.hasMoreElements()) {
                    JarEntry entry = (JarEntry) allEntries.nextElement();
                    if (!entry.getName().contains(".class") || entry.getName().contains("$")) continue;
                    String className = entry.getName().replace(".class", "").replace("/", ".");
                    Class c = Class.forName(className);
                    for (Class iface : c.getInterfaces()) {
                        if (iface.isAssignableFrom(IPlugin.class)) pluginClasses.add((IPlugin) c.newInstance());
                    }
                }
            } catch (IOException e) {
                throw new WebApplicationException(e, 500);
            } catch (NoClassDefFoundError e) {
                // ignore this exception
            } catch (ClassNotFoundException e) {
                throw new WebApplicationException(e, 500);
            } catch (InstantiationException e) {
                throw new WebApplicationException(e, 500);
            } catch (IllegalAccessException e) {
                throw new WebApplicationException(e, 500);
            }
        }


        List<String> classes = new ArrayList<String>();
        walk(path + File.separator + "WEB-INF" + File.separator + "classes", classes);
        for (String aClass : classes)
            try {
                String className = getClassName(aClass, path + "WEB-INF" + File.separator + "classes/");
                Class c = Class.forName(className);
                for (Class iface : c.getInterfaces()) {
                    if (iface.isAssignableFrom(IPlugin.class)) pluginClasses.add((IPlugin) c.newInstance());
                }
            } catch (ClassNotFoundException e) {
                throw new WebApplicationException(e, 500);
            } catch (InstantiationException e) {
                throw new WebApplicationException(e, 500);
            } catch (IllegalAccessException e) {
                throw new WebApplicationException(e, 500);
            }


        Set<String> links = new HashSet<String>();
        for (IPlugin plugin : pluginClasses) {
            String[] paths = plugin.getResourcePaths();
            for (String aPath : paths) {
                String[] toks = aPath.split("/");
                links.add("/" + toks[1] + "/" + toks[2]);
            }
        }

        String json = "{}";
        try {
            json = Util.toJson(links);
        } catch (IOException e) {
            throw new WebApplicationException(e, 500);
        }

        if (countRequested) return Response.ok(links.size()).type(MediaType.TEXT_PLAIN).build();
        return Response.ok(json).type(MediaType.APPLICATION_JSON).build();

    }

    public Response getResources(@PathParam("name") String resourceId, @QueryParam("filter") String filter, @QueryParam("expand") String expand) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    private String getClassName(String path, String prefix) {
        path = path.replace(prefix, "").replace(".class", "").replace("/", ".");
        return path;
    }

    private void walk(String path, List<String> paths) {
        File root = new File(path);
        File[] list = root.listFiles();
        for (File f : list) {
            if (f.isDirectory()) {
                walk(f.getAbsolutePath(), paths);
            } else {
                paths.add(f.getAbsolutePath());
            }
        }
    }

}
