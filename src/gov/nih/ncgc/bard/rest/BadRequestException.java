package gov.nih.ncgc.bard.rest;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

/**
 * Provides an exception for HTTP 400 (Bad request)
 *
 * @author Rajarshi Guha
 */
public class BadRequestException extends WebApplicationException {
    /**
     * Create a HTTP 400  exception.
     */
    public BadRequestException() {
        super(Response.status(400).build());
    }

    /**
     * Create a HTTP 400 exception.
     *
     * @param message the String that is the entity of the 400 response.
     */
    public BadRequestException(String message) {
        super(Response.status(400).entity(message).type("text/plain").build());
    }

}
