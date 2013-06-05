package gov.nih.ncgc.bard.rest;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nih.ncgc.bard.entity.BardEntity;
import gov.nih.ncgc.bard.tools.DBUtils;
import gov.nih.ncgc.bard.tools.JsonUtil;
import gov.nih.ncgc.bard.tools.Util;

import javax.annotation.PostConstruct;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A base class for all REST resource class.
 * <p/>
 * Provides some useful utility methods and fields.
 *
 * @author Rajarshi Guha
 */
public abstract class BARDResource<T extends BardEntity>
        implements IBARDResource {

    static final Logger logger =
            Logger.getLogger(BARDResource.class.getName());

    @Context
    ServletConfig servletConfig;
    @Context
    ServletContext servletContext;
    @Context
    HttpServletRequest httpServletRequest;
    @Context
    protected HttpHeaders headers;

    /**
     * <code>true</code> if the request specified a count of entities rather than the entities themselves.
     */
    protected boolean countRequested;
    protected List<EntityTag> etagsRequested = new ArrayList<EntityTag>();

    protected static boolean init = false;

    protected BARDResource () {
    }

    synchronized void init () {
        if (!init) {
            String ctx = servletContext.getInitParameter("datasource-context");
            if (ctx != null) {
                logger.info("## datasource context: "+ctx);
                DBUtils.setDataSourceContext(ctx);
            }
            init = true;
        }
    }

    @PostConstruct
    protected void postConstruct() {
        init ();

        countRequested = Util.countRequested(headers);
        List<String> etags = headers.getRequestHeader(HttpHeaders.IF_MATCH);
        if (etags != null) {
            System.err.print("## If-Match: ");
            for (String entry : etags) {
                for (String e : entry.split(",")) {
                    EntityTag t = EntityTag.valueOf(e.trim());
                    System.err.print(" " + t.getValue());
                    etagsRequested.add(t);
                }
            }
            System.err.println(" " + etagsRequested.size());
        }

        System.err.println("## Request URI: "+getRequestURI ());
    }

    protected List<EntityTag> getETagsRequested() {
        return etagsRequested;
    }

    protected boolean expandEntries(String expand) {
        boolean expandEntries = false;
        if (expand != null && (expand.toLowerCase().equals("true") || expand.toLowerCase().equals("yes")))
            expandEntries = true;
        return expandEntries;
    }

    protected ServletContext getServletContext() {
        return servletContext;
    }

    protected String getRequestURI() {
        String query = httpServletRequest.getQueryString();
        return (httpServletRequest.getMethod() + ":"
                + httpServletRequest.getRequestURI()
                + (query != null ? ("?" + query) : ""));
    }

    public abstract Class<T> getEntityClass ();
    public abstract String getResourceBase ();

    /*
     * ETag common resources
     */
    @GET
    @Path("/etag")
    public Response getETags (@QueryParam("expand") String expand,
                              @QueryParam("skip") Integer skip,
                              @QueryParam("top") Integer top) {
        DBUtils db = new DBUtils ();
        try {
            Response response = null;
            if (top == null) {
                top = BARDConstants.MAX_DATA_COUNT;
            }
            if (skip == null) {
                skip = 0;
            }

            List<String> etags = db.getETagsForEntity 
                (skip, top, null /* Principal */, getEntityClass());

            String linkString = null;
            if (etags.size() == top) { // there are more
                linkString = getResourceBase()+"/etag?skip="+(skip+top)
                    +"&top="+top+"&expand="+expand;
            }
            
            if (expandEntries (expand)) {
                List<Map> entities = new ArrayList<Map>();
                for (String e : etags) {
                    Map et = db.getETagInfo(e);
                    entities.add(et);
                }

                Map map = new TreeMap ();
                map.put("collection", entities);
                map.put("link", linkString);
                response = Response.ok(Util.toJson(map), 
                                       MediaType.APPLICATION_JSON).build();
            }
            else {
                Map res = new TreeMap ();
                res.put("collection", etags);
                res.put("link", linkString);
                response = Response.ok(Util.toJson(res), 
                                       MediaType.APPLICATION_JSON).build();
            }

            return response;
        }
        catch (Exception ex) {
            throw new WebApplicationException (ex, 500);            
        }
        finally {
            try {
                db.closeConnection();
            }
            catch (Exception ex) {
            }
        }
    }

    @GET
    @Path("/etag/{etag}")
    public Response getEntitiesByETag(@PathParam("etag") String resourceId,
                                      @QueryParam("filter") String filter,
                                      @QueryParam("expand") String expand,
                                      @QueryParam("skip") Integer skip,
                                      @QueryParam("top") Integer top) {
        throw new WebApplicationException
                (new UnsupportedOperationException(), 501);
    }

    @POST
    @Path("/etag")
    @Consumes("application/x-www-form-urlencoded")
    public Response createETag(@FormParam("name") String name,
                               @FormParam("url") String url,
                               @FormParam("ids") String ids,
                               @FormParam("etagids") String etagids) {
        DBUtils db = new DBUtils();
        try {
            if (name == null) {
                throw new IllegalArgumentException
                        ("No \"name\" specified!");
            }

            EntityTag etag = new EntityTag
                    (db.newETag(name, url, getEntityClass().getName()));

            if (ids != null) {
                List<Long> list = new ArrayList<Long>();
                for (String id : ids.split("[,;\\s]")) {
                    try {
                        list.add(Long.parseLong(id));
                    } catch (NumberFormatException ex) {
                    }
                }
                int cnt = db.putETag
                        (etag.getValue(), list.toArray(new Long[0]));

                log("** New ETag: " + etag.getValue() + " \"" + name + "\" " + cnt);
            } else {
                log("** New ETag: " + etag.getValue() + " \"" + name + "\"");
            }

            if (etagids != null) {
                for (String anEtag : etagids.split(",")) {
                    db.createETagLinks(anEtag.trim(), etag.getValue());
                }
            }

            return Response.ok().tag(etag).build();
        } catch (Exception ex) {
            throw new WebApplicationException(ex, 500);
        } finally {
            try {
                db.closeConnection();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    @PUT
    @Path("/etag/{etag}")
    @Consumes("application/x-www-form-urlencoded")
    public Response putETag(@PathParam("etag") String etag,
                            @FormParam("name") String name,
                            @FormParam("ids") String ids,
                            @FormParam("etagids") String etagids) {
        DBUtils db = new DBUtils();
        try {
            if (ids == null) {
                throw new IllegalArgumentException
                        ("No \"ids\" param specified!");
            }

            // check to make sure the correct type
            Map info = db.getETagInfo(etag);
            if (!getEntityClass().getName().equals(info.get("type"))) {
                throw new WebApplicationException
                    (new IllegalArgumentException 
                     ("ETag "+etag+" is not of type "+getEntityClass()), 500);
            }

            List<Long> list = new ArrayList<Long>();
            for (String id : ids.split("[,;\\s]")) {
                try {
                    list.add(Long.parseLong(id));
                } catch (NumberFormatException ex) {
                }
            }
            int cnt = db.putETag(etag, name,  list.toArray(new Long[0]));
            log("** put ETag: " + etag + " " + cnt);

            if (etagids != null) {
                for (String anEtag : etagids.split(",")) {
                    db.createETagLinks(anEtag.trim(), etag);
                }
            }


            return Response.ok(String.valueOf(cnt), "text/plain")
                    .tag(etag).build();
        } catch (Exception ex) {
            throw new WebApplicationException(ex, 500);
        } finally {
            try {
                db.closeConnection();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    @GET
    @Path("/etag/{etag}/_info")
    public Response getETagInfo (@PathParam("etag") String resourceId) {
        DBUtils db = new DBUtils();
        try {
            Map info = db.getETagInfo(resourceId);
            return Response.ok(Util.toJson(info),
                    MediaType.APPLICATION_JSON).build();
        } catch (Exception ex) {
            throw new WebApplicationException(ex, 500);
        } finally {
            try {
                db.closeConnection();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    @GET
    @Path("/etag/{etag}/facets")
    public Response getFacets(@PathParam("etag") String resourceId) {
        throw new WebApplicationException
                (new UnsupportedOperationException(), 500);
    }

    protected void log(String mesg) {
        //servletContext.log(mesg);
        logger.info(mesg);
    }

    protected void log(String mesg, Throwable t) {
        //servletContext.log(mesg, t);
        logger.log(Level.SEVERE, mesg, t);
    }

    protected void warning(String mesg) {
        logger.warning(mesg);
    }

    @GET
    @Path("/_schema")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSchema() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode schemaNode = JsonUtil.getJsonSchema(getEntityClass());
            return Response.ok(mapper.writeValueAsString(schemaNode)).type(MediaType.APPLICATION_JSON_TYPE).build();
        } catch (JsonMappingException e) {
            e.printStackTrace();
            throw new WebApplicationException(500);
        } catch (IOException e) {
            e.printStackTrace();
            throw new WebApplicationException(500);
        }
    }


    @GET
    @Path("/recent/{n}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getRecent(@PathParam("n") Integer n,
                              @QueryParam("expand") String expand) {
        DBUtils db = new DBUtils();
        try {
            List<T> entities = db.getRecentEntities(getEntityClass(), n);
            if (expandEntries(expand))
                return Response.ok(Util.toJson(entities)).type(MediaType.APPLICATION_JSON_TYPE).build();
            else {
                List<String> ids = new ArrayList<String>();
                for (T entity : entities) ids.add(entity.getResourcePath());
                return Response.ok(Util.toJson(ids)).type(MediaType.APPLICATION_JSON_TYPE).build();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new WebApplicationException(500);
        } catch (IOException e) {
            e.printStackTrace();
            throw new WebApplicationException(500);
        } finally {
            try {
                db.closeConnection();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }



}
