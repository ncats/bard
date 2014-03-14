package gov.nih.ncgc.bard.capextract.handler;

import gov.nih.ncgc.bard.capextract.CAPConstants;
import gov.nih.ncgc.bard.capextract.CapResourceHandlerRegistry;
import gov.nih.ncgc.bard.capextract.ICapResourceHandler;
import gov.nih.ncgc.bard.capextract.jaxb.Link;
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
    public int process(String url, CAPConstants.CapResource resource) throws IOException {
        if (resource != CAPConstants.CapResource.PROJECTS) return CAPConstants.CAP_EXTRACT_LOAD_STATUS_FAILED;
        log.info("Processing " + resource);

        // get the Projects object here
        Projects projects = getResponse(url, resource);

        // map CAP project IDs to BARD project IDs !!! done by hand in excel !!!

        // load project annotations
        ProjectHandler ph = (ProjectHandler) CapResourceHandlerRegistry.getInstance().getHandler(CAPConstants.CapResource.PROJECT);
        for (Link projLink : projects.getLink()) {
            CAPConstants.CapResource res = CAPConstants.getResource(projLink.getType());
            if (res == CAPConstants.CapResource.PROJECT && ph != null) {
                Project project = (Project) ph.poll(projLink.getHref(), res).get(0);
                if (project == null) {
                    log.warn("null response for "+projLink.getHref());
                    continue;
                }

                String readyToXtract = project.getReadyForExtraction();
                String title = project.getProjectName();
                BigInteger pid = project.getProjectId();
//                log.info("\taurl = [" + readyToXtract + "] for " + title + " pid " + pid);
                int loadStatus = CAPConstants.CAP_EXTRACT_LOAD_STATUS_COMPLETE;
                if (readyToXtract.equals("Ready")) {                    
                    //start
                    setExtractionStatus(CAPConstants.CAP_STATUS_STARTED, projLink.getHref(), 
                	    CAPConstants.CapResource.PROJECT);
                    //process
                    loadStatus = ph.process(project, projLink.getHref(), res);
                    //mark complete
                    if(loadStatus == CAPConstants.CAP_EXTRACT_LOAD_STATUS_COMPLETE) {
                	setExtractionStatus(CAPConstants.CAP_STATUS_COMPLETE, projLink.getHref(), 
                		CAPConstants.CapResource.PROJECT);  
                    } else if (loadStatus == CAPConstants.CAP_EXTRACT_LOAD_STATUS_FAILED) {
                	setExtractionStatus(CAPConstants.CAP_STATUS_FAILED, projLink.getHref(), 
                		CAPConstants.CapResource.PROJECT);                  	
                    }
                }
            }
        }
        return CAPConstants.CAP_EXTRACT_LOAD_STATUS_COMPLETE;
    }
}
