package gov.nih.ncgc.bard.capextract.handler;

import gov.nih.ncgc.bard.capextract.CAPConstants;
import gov.nih.ncgc.bard.capextract.CapResourceHandlerRegistry;
import gov.nih.ncgc.bard.capextract.ICapResourceHandler;
import gov.nih.ncgc.bard.capextract.jaxb.Experiments;
import gov.nih.ncgc.bard.capextract.jaxb.Link;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;

/**
 * A one line summary.
 *
 * @author Rajarshi Guha
 */
public class ExperimentsHandler extends CapResourceHandler implements ICapResourceHandler {

    public ExperimentsHandler() {
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
        if (resource != CAPConstants.CapResource.EXPERIMENTS) return;
        log.info("Processing " + resource);

        // get the Experiments object here
        Experiments experiments = getResponse(url, resource);
        BigInteger n = experiments.getCount();
        log.info("\tWill be processing " + n + " experiments");
        List<Link> links = experiments.getLink();
        for (Link link : links) {
            String href = link.getHref();
            String type = link.getType();
            String title = link.getTitle();

            // for now lets just handle a few specific experiments
//            if (href.endsWith("/45")) {
            if (true) {
                log.info("\t" + title + "/" + type + "/ href = " + href);
                ICapResourceHandler handler = CapResourceHandlerRegistry.getInstance().getHandler(CAPConstants.CapResource.EXPERIMENT);
                if (handler != null) handler.process(href, CAPConstants.CapResource.EXPERIMENT);
            }
        }
    }
}
