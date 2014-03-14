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
    public int process(String url, CAPConstants.CapResource resource) throws IOException {
	if (resource != CAPConstants.CapResource.EXPERIMENTS) return CAPConstants.CAP_EXTRACT_LOAD_STATUS_FAILED;
	log.info("Processing " + resource);

	while (url != null) { // in case 206 partial response is returned, we should continue to iterate
	    // get the Experiments object here
	    Experiments experiments = getResponse(url, resource);
	    url = null;
	    BigInteger n = experiments.getCount();
	    log.info("Will be processing " + n + " experiments");
	    List<Link> links = experiments.getLink();
	    
	    for (Link link : links) {
		if (link.getRel().equals("next")) {
		    url = link.getHref();
		} else if (link.getRel().equals("related") &&
			link.getType().equals(CAPConstants.CapResource.EXPERIMENT.getMimeType())) {
		    String href = link.getHref();
		    link.getType();
		    link.getTitle();

		    //log.info("\t" + title + "/" + type + "/ href = " + href);

		    //load experiment, then results
		    ICapResourceHandler handler = CapResourceHandlerRegistry.getInstance().getHandler(CAPConstants.CapResource.EXPERIMENT);
		    int loadStatus = CAPConstants.CAP_EXTRACT_LOAD_STATUS_COMPLETE;
		    int dataLoadStatus = CAPConstants.CAP_EXTRACT_LOAD_STATUS_COMPLETE;
		    if (handler != null) { 
			//status set to started
			setExtractionStatus(CAPConstants.CAP_STATUS_STARTED, href, CAPConstants.CapResource.EXPERIMENT);

			//load experiment
			loadStatus = handler.process(href, CAPConstants.CapResource.EXPERIMENT);

			//if loading experiment, load experiment results
			handler = CapResourceHandlerRegistry.getInstance().getHandler(CAPConstants.CapResource.RESULT_JSON);
			if (handler != null) {
			   dataLoadStatus = handler.process(href, CAPConstants.CapResource.RESULT_JSON);
			} else {
			    log.warn("!!! Don't have handler for result json, it's null.");
			}
			//set status to completed
			if(loadStatus == CAPConstants.CAP_EXTRACT_LOAD_STATUS_COMPLETE 
				&& dataLoadStatus == CAPConstants.CAP_EXTRACT_LOAD_STATUS_COMPLETE) {
			    
			    this.setExtractionStatus(CAPConstants.CAP_STATUS_COMPLETE, href, CAPConstants.CapResource.EXPERIMENT);
			} else {
			    this.setExtractionStatus(CAPConstants.CAP_STATUS_FAILED, href, CAPConstants.CapResource.EXPERIMENT);
			}
		    }
		}
	    }
	}
	return CAPConstants.CAP_EXTRACT_LOAD_STATUS_COMPLETE;
    }
}
