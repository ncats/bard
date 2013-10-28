package gov.nih.ncgc.bard.capextract.handler;

import gov.nih.ncgc.bard.capextract.CAPConstants;
import gov.nih.ncgc.bard.capextract.ICapResourceHandler;
import gov.nih.ncgc.bard.capextract.jaxb.ExternalReference;
import gov.nih.ncgc.bard.capextract.jaxb.Link;

import java.io.IOException;
import java.util.List;

/**
 * A one line summary.
 *
 * @author Rajarshi Guha
 */
public class ExternalReferenceHandler extends CapResourceHandler implements ICapResourceHandler {
    String externalAssayRef;
    List<Link> links;

    public ExternalReferenceHandler() {
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
        if (resource != CAPConstants.CapResource.EXTREF) return;

        // get the Project object here
        ExternalReference tmp = getResponse(url, resource);
        externalAssayRef = tmp.getExternalAssayRef();
        links = tmp.getLink();
    }

    public String getExternalAssayRef() {
        return externalAssayRef;
    }

    public List<Link> getLinks() {
        return links;
    }
}
