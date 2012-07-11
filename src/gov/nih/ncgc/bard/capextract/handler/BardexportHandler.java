package gov.nih.ncgc.bard.capextract.handler;

import com.sun.jersey.api.client.ClientResponse;
import gov.nih.ncgc.bard.capextract.CAPConstants;
import gov.nih.ncgc.bard.capextract.CapResourceHandlerRegistry;
import gov.nih.ncgc.bard.capextract.ICapResourceHandler;
import gov.nih.ncgc.bard.capextract.jaxb.Bardexport;
import gov.nih.ncgc.bard.capextract.jaxb.Link;

import java.io.IOException;

/**
 * A one line summary.
 *
 * @author Rajarshi Guha
 */
public class BardexportHandler extends CapResourceHandler implements ICapResourceHandler {

    public BardexportHandler() {
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
        if (resource != CAPConstants.CapResource.BARDEXPORT) return;
        System.out.println("Processing " + resource);

        ClientResponse response = getResponse(url, resource);
        if (response.getStatus() != 200)
            throw new IOException("Got HTTP " + response.getStatus() + " from CAP bardexport resource");

        Bardexport export = response.getEntity(Bardexport.class);

        for (Link link : export.getLink()) {
            String aurl = link.getHref();
            CAPConstants.CapResource res = CAPConstants.getResource(link.getType());
            ICapResourceHandler handler = CapResourceHandlerRegistry.getInstance().getHandler(res);
            if (handler == null) {
                System.err.println("No handler for " + link.getType());
            } else handler.process(aurl, res);
        }
    }
}
