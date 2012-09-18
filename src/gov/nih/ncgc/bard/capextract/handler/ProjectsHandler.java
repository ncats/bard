package gov.nih.ncgc.bard.capextract.handler;

import gov.nih.ncgc.bard.capextract.CAPConstants;
import gov.nih.ncgc.bard.capextract.CapResourceHandlerRegistry;
import gov.nih.ncgc.bard.capextract.ICapResourceHandler;
import gov.nih.ncgc.bard.capextract.jaxb.Project;
import gov.nih.ncgc.bard.capextract.jaxb.Projects;

import java.io.IOException;
import java.math.BigInteger;

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
        log.info("Processing " + resource);

        // get the Projects object here
        Projects projects = getResponse(url, resource);
        
        // map CAP project IDs to BARD project IDs !!! done by hand in excel !!!
        
        // load project annotations
        ProjectHandler ph = (ProjectHandler)CapResourceHandlerRegistry.getInstance().getHandler(CAPConstants.CapResource.PROJECT);
        for (Project project : projects.getProject()) {
            //ph.process(CAPConstants.CAP_ROOT+"/projects/"+project.getProjectId(), CAPConstants.CapResource.PROJECT);
            String readyToXtract = project.getReadyForExtraction();
            String title = project.getProjectName();
            BigInteger pid = project.getProjectId();

            log.info("\taurl = [" + readyToXtract + "] for " + title + " pid " + pid);
            if (readyToXtract.equals("Ready")) {
                log.info("\tExtracting " + title);
                ph.process(project);
            }
        }
    }
}
