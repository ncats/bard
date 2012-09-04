package gov.nih.ncgc.bard.capextract;

import java.io.IOException;
import java.util.Vector;

/**
 * An interface for classes that intend to process specific CAP entities (aka resources).
 * <p/>
 * Entries include the dictionary, result, assay etc.
 *
 * @author Rajarshi Guha
 */
public interface ICapResourceHandler {

    /**
     * Process a CAP entity that is located at some URL.
     *
     * @param url      The URL from which to retrieve the entity fron
     * @param resource The CAP resource that is meant to be processed. An implementing class
     *                 can choose to proceed or not based on this parameter.
     * @throws java.io.IOException if there is an error in retrieving the resource
     */
    public void process(String url, CAPConstants.CapResource resource) throws IOException;

    // retrieves objects (handles http 206 response), but does not push results into DB
    public <T> Vector<T> poll(String url, CAPConstants.CapResource resource) throws IOException;
    public <T> Vector<T> poll(String url, CAPConstants.CapResource resource, boolean skipPartial) throws IOException;
}
