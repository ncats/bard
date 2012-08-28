package gov.nih.ncgc.bard.rest;

import java.util.logging.Logger;
import java.util.logging.Level;

import java.util.List;
import java.util.ArrayList;
import gov.nih.ncgc.bard.tools.Util;

import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.EntityTag;

/**
 * A base class for all REST resource class.
 * <p/>
 * Provides some useful utility methods and fields.
 *
 * @author Rajarshi Guha
 */
public abstract class BARDResource implements IBARDResource {
    static final Logger logger = 
        Logger.getLogger(BARDResource.class.getName());
    
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
                    System.err.print(" "+t.getValue());
                    etagsRequested.add(t);
                }
            }
            System.err.println(" "+etagsRequested.size());
        }
    }

    protected List<EntityTag> getETagsRequested () { return etagsRequested; }

    protected boolean expandEntries(String expand) {
        boolean expandEntries = false;
        if (expand != null && (expand.toLowerCase().equals("true") || expand.toLowerCase().equals("yes")))
            expandEntries = true;
        return expandEntries;
    }

    protected String getRequestURI () {
        String query = httpServletRequest.getQueryString();
        return (httpServletRequest.getMethod()+":"
                +httpServletRequest.getRequestURI()
                +(query != null ? ("?"+query) : ""));
    }

    protected void log (String mesg) {
        //servletContext.log(mesg);
        logger.info(mesg);
    }

    protected void log (String mesg, Throwable t) {
        //servletContext.log(mesg, t);
        logger.log(Level.SEVERE, mesg, t);
    }

    protected void warning (String mesg) {
        logger.warning(mesg);
    }
}
