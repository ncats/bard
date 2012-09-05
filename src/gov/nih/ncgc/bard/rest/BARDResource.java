package gov.nih.ncgc.bard.rest;

import gov.nih.ncgc.bard.entity.BardEntity;
import gov.nih.ncgc.bard.tools.DBUtils;
import gov.nih.ncgc.bard.tools.Util;

import javax.annotation.PostConstruct;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

    @PostConstruct
    protected void postConstruct() {
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

    public abstract Class<T> getEntityClass();

    /*
     * ETag common resources
     */
    @GET
    @Path("/etag/{etag}")
    public Response getEntitiesByETag(@PathParam("etag") String resourceId,
                                      @QueryParam("filter") String filter,
                                      @QueryParam("expand") String expand,
                                      @QueryParam("skip") Integer skip,
                                      @QueryParam("top") Integer top) {
        throw new WebApplicationException
                (new UnsupportedOperationException(), 500);
    }

    @POST
    @Path("/etag")
    @Consumes("application/x-www-form-urlencoded")
    public Response createETag(@FormParam("name") String name,
                               @FormParam("ids") String ids) {
        DBUtils db = new DBUtils();
        try {
            if (name == null) {
                throw new IllegalArgumentException
                        ("No \"name\" specified!");
            }

            EntityTag etag = new EntityTag
                    (db.newETag(name, getEntityClass().getName()));

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
                            @FormParam("ids") String ids) {
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
            int cnt = db.putETag(etag, list.toArray(new Long[0]));
            log("** put ETag: " + etag + " " + cnt);

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
}
