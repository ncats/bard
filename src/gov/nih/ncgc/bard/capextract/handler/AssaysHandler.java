package gov.nih.ncgc.bard.capextract.handler;

import gov.nih.ncgc.bard.capextract.CAPConstants;
import gov.nih.ncgc.bard.capextract.CapResourceHandlerRegistry;
import gov.nih.ncgc.bard.capextract.ICapResourceHandler;
import gov.nih.ncgc.bard.capextract.jaxb.Assays;
import gov.nih.ncgc.bard.capextract.jaxb.Link;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;

/**
 * A one line summary.
 *
 * @author Rajarshi Guha
 */
public class AssaysHandler extends CapResourceHandler implements ICapResourceHandler {

    public AssaysHandler() {
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
        if (resource != CAPConstants.CapResource.ASSAYS) return;
        log.info("Processing " + resource);

        // get the Assays object here
        Assays assays = getResponse(url, resource);
        BigInteger n = assays.getCount();
        log.info("\tWill be processing " + n + " assays");
        List<Link> links = assays.getLink();
        for (Link link : links) {
            String href = link.getHref();
            String type = link.getType();
            String title = link.getTitle();
            log.info("\t" + title + "/" + type + "/ href = " + href);

            // for now lets just handle a few specific assays
            if (href.endsWith("/1640")) {
                ICapResourceHandler handler = CapResourceHandlerRegistry.getInstance().getHandler(CAPConstants.CapResource.ASSAY);
                if (handler != null) handler.process(href, CAPConstants.CapResource.ASSAY);
            }
        }
    }
}
