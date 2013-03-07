package gov.nih.ncgc.bard.plugin;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * Java representation of a plugin manifest.
 * <p/>
 * This class conforms to the <a href="https://github.com/ncatsdpiprobedev/bardplugins/blob/master/resources/manifest.json">
 * plugin manifest schema</a>. Plugins can use this class to generate a JSON representation, or else can
 * generate the JSON manually.
 *
 * @author Rajarshi Guha
 */
public class PluginManifest {
    String title, description, version;
    String author, authorEmail;
    String maintainer, maintainerEmail;
    PluginResource[] resources;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getAuthorEmail() {
        return authorEmail;
    }

    public void setAuthorEmail(String authorEmail) {
        this.authorEmail = authorEmail;
    }

    public String getMaintainer() {
        return maintainer;
    }

    public void setMaintainer(String maintainer) {
        this.maintainer = maintainer;
    }

    public String getMaintainerEmail() {
        return maintainerEmail;
    }

    public void setMaintainerEmail(String maintainerEmail) {
        this.maintainerEmail = maintainerEmail;
    }

    public PluginResource[] getResources() {
        return resources;
    }

    public void setResources(PluginResource[] resources) {
        this.resources = resources;
    }

    public static class PluginResource {
        String path, mimetype, method;
        PathArg[] args;

        public PluginResource() {
        }

        public String getMethod() {
            return method;
        }

        public void setMethod(String method) {
            this.method = method;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getMimetype() {
            return mimetype;
        }

        public void setMimetype(String mimetype) {
            this.mimetype = mimetype;
        }

        public PathArg[] getArgs() {
            return args;
        }

        public void setArgs(PathArg[] args) {
            this.args = args;
        }
    }

    public static class PathArg {
        String arg, argtype;

        public PathArg() {
        }

        public PathArg(String arg, String argtype) {
            this.arg = arg;
            this.argtype = argtype;
        }

        public String getArg() {
            return arg;
        }

        public void setArg(String arg) {
            this.arg = arg;
        }

        public String getArgtype() {
            return argtype;
        }

        public void setArgtype(String argtype) {
            this.argtype = argtype;
        }
    }

    public String toJson() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            String json = mapper.writeValueAsString(this);
            return json;
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        return "";
    }
}
