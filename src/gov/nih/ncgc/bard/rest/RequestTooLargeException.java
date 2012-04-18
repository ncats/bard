package gov.nih.ncgc.bard.rest;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

/**
 * Provides an exception for HTTP 413 (Request Too Large)mplement
 *
 * @author Rajarshi Guha
 */
public class RequestTooLargeException extends WebApplicationException {
    /**
     * Create a HTTP 413  exception.
     */
    public RequestTooLargeException() {
        super(Response.status(413).build());
    }

    /**
     * Create a HTTP 413 exception.
     *
     * @param message the String that is the entity of the 413 response.
     */
    public RequestTooLargeException(String message) {
        super(Response.status(413).entity(message).type("text/plain").build());
    }

}
