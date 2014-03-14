package gov.nih.ncgc.bard.capextract.handler;

import gov.nih.ncgc.bard.capextract.CAPConstants;
import gov.nih.ncgc.bard.capextract.ICapResourceHandler;
import gov.nih.ncgc.bard.capextract.jaxb.ExternalSystem;
import gov.nih.ncgc.bard.capextract.jaxb.ExternalSystems;

import java.io.IOException;

/**
 * A one line summary.
 *
 * @author Rajarshi Guha
 */
public class ExternalSystemHandler extends CapResourceHandler implements ICapResourceHandler {
    ExternalSystem extsys;

    public ExternalSystemHandler() {
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
        if (resource != CAPConstants.CapResource.EXTSYS) return CAPConstants.CAP_EXTRACT_LOAD_STATUS_FAILED;

        // get the Project object here
        extsys = getResponse(url, resource);
        
        return CAPConstants.CAP_EXTRACT_LOAD_STATUS_COMPLETE;
    }

    public ExternalSystem getExtsys() {
        return extsys;
    }
}
