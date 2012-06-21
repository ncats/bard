package gov.nih.ncgc.bard.rest;

import gov.nih.ncgc.bard.tools.Util;

import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;

/**
 * A base class for all REST resource class.
 * <p/>
 * Provides some useful utility methods and fields.
 *
 * @author Rajarshi Guha
 */
public abstract class BARDResource implements IBARDResource {
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

    @PostConstruct
    protected void postConstruct() {
        countRequested = Util.countRequested(headers);
    }
}
