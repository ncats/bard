package gov.nih.ncgc.bard.capextract.handler;

import com.sun.jersey.api.client.ClientResponse;
import gov.nih.ncgc.bard.capextract.CAPConstants;
import gov.nih.ncgc.bard.capextract.ICapResourceHandler;
import gov.nih.ncgc.bard.capextract.jaxb.Project;
import gov.nih.ncgc.bard.capextract.jaxb.Projects;

import java.io.IOException;

/**
 * A one line summary.
 *
 * @author Rajarshi Guha
 */
public class ProjectsHandler extends CapResourceHandler implements ICapResourceHandler {

    public ProjectsHandler() {
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
        if (resource != CAPConstants.CapResource.PROJECTS) return;
        System.out.println("Processing " + resource);

        ClientResponse response = getResponse(url, resource);
        if (response.getStatus() != 200)
            throw new IOException("Got HTTP " + response.getStatus() + " from CAP projects resource");

        // get the Projects object here
        Projects projects = response.getEntity(Projects.class);
        for (Project project : projects.getProject()) {
            String readyToXtract = project.getReadyForExtraction();
            String title = project.getProjectName();
            System.out.println("\taurl = " + readyToXtract + " for " + title);
        }
    }
}
