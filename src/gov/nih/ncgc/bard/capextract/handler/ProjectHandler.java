package gov.nih.ncgc.bard.capextract.handler;

import com.sun.jersey.api.client.ClientResponse;
import gov.nih.ncgc.bard.capextract.CAPConstants;
import gov.nih.ncgc.bard.capextract.ICapResourceHandler;
import gov.nih.ncgc.bard.entity.Project;

import java.io.IOException;

/**
 * A one line summary.
 *
 * @author Rajarshi Guha
 */
public class ProjectHandler extends CapResourceHandler implements ICapResourceHandler {

    public ProjectHandler() {
        super();
    }

    /**
     * Process a CAP entity that is located at some URL.
     *
     * @param url      The URL from which to retrieve the entity fron
     * @param resource The CAP resource that is meant to be processed. An implementing class
     *                 can choose to proceed or not based on this parameter.
     */
    public void process(String url, CAPConstants.CapResource resource) throws IOException {
        if (resource != CAPConstants.CapResource.PROJECT) return;
        log.info("Processing " + resource);

        ClientResponse response = getResponse(url, resource);
        if (response.getStatus() != 200)
            throw new IOException("Got HTTP " + response.getStatus() + " from CAP project resource");

        // get the Projects object here
        Project project = response.getEntity(Project.class);
        log.info("project.getResourcePath() = " + project.getResourcePath());
    }
}
