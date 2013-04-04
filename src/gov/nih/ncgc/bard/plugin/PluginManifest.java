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
        String format = "path";

        public PathArg() {
        }

        public PathArg(String arg, String argtype) {
            this(arg, argtype, "path");
        }

        public PathArg(String arg, String argtype, String format) {
            this.arg = arg;
            this.argtype = argtype;
            this.format = format;
        }

        public String getArg() {
            return arg;
        }

        public void setArg(String arg) {
            this.arg = arg;
        }

        /**
         * Get the type of the argument.
         * <p/>
         * Valid values are defined in the JSON schema
         * <a href="http://tools.ietf.org/html/draft-zyp-json-schema-03#section-5.1">specification</a>
         *
         * @return The type of the argument
         */
        public String getArgtype() {
            return argtype;
        }

        /**
         * Set the type of the argument.
         * <p/>
         * Valid values are defined in the JSON schema
         * <a href="http://tools.ietf.org/html/draft-zyp-json-schema-03#section-5.1">specification</a>
         *
         * @param argtype The type of the argument
         */
        public void setArgtype(String argtype) {
            this.argtype = argtype;
        }

        /**
         * The format of the argument - either a path argument or query argument.
         * <p/>
         * Query arguments are specified in the form <code>?argname=argvalue</code>.
         *
         * @return The format of the argument
         */
        public String getFormat() {
            return format;
        }

        /**
         * Set the format of the argument - either a path argument or query argument.
         * <p/>
         * Query arguments are specified in the form <code>?argname=argvalue</code>.
         * Currently, the valid values are <code>path</code> or <code>query</code>
         */
        public void setFormat(String format) {
            if (format.toLowerCase().equals("path") ||
                    format.toLowerCase().equals("query"))
                this.format = format;
            else throw new IllegalArgumentException("format must be path or query");
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
