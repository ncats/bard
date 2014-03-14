package gov.nih.ncgc.bard.capextract.handler;

import gov.nih.ncgc.bard.capextract.CAPConstants;
import gov.nih.ncgc.bard.capextract.ICapResourceHandler;
import gov.nih.ncgc.bard.capextract.jaxb.Results;

import java.io.IOException;
import java.math.BigInteger;

/**
 * A one line summary.
 *
 * @author Rajarshi Guha
 */
public class ResultsHandler extends CapResourceHandler implements ICapResourceHandler {

    public ResultsHandler() {
        super();
    }

    /**
     * Process a CAP entity that is located at some URL.
     *
     * @param url      The URL from which to retrieve the entity from
     * @param resource The CAP resource that is meant to be processed. An implementing class
     *                 can choose to proceed or not based on this parameter.
     */
    public int process(String url, CAPConstants.CapResource resource) throws IOException {
        if (resource != CAPConstants.CapResource.RESULTS) return CAPConstants.CAP_EXTRACT_LOAD_STATUS_FAILED;
        log.info("Processing " + resource);

        while (url != null) { // in case 206 partial response is returned, we should continue to iterate
            Results results = getResponse(url, resource);
            url = null;
            BigInteger n = results.getCount();
            log.info("\tWill be processing " + n + " results");
// !!! For now we will not load experiment results directly from CAP Extract API        	    
//            List<Link> links = results.getLink();
//            for (Link link : links) {
//        	if (link.getRel().equals("next")) {
//        	    url = link.getHref();
//        	} else if (link.getRel().equals("related") && 
//        		link.getType().equals(CAPConstants.CapResource.RESULT.getMimeType())) {
//        	    String href = link.getHref();
//        	    link.getTitle();
//
//        	    //log.info("\t" + title + "/" + type + "/ href = " + href);
//        	    ICapResourceHandler handler = CapResourceHandlerRegistry.getInstance().getHandler(CAPConstants.CapResource.RESULT);
//        	    if (handler != null) handler.process(href, CAPConstants.CapResource.RESULT);
//        	}
//            }
        }
        return CAPConstants.CAP_EXTRACT_LOAD_STATUS_COMPLETE;
    }
}
