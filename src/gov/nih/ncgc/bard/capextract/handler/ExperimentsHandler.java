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

	while (url != null) { // in case 206 partial response is returned, we should continue to iterate
	    // get the Experiments object here
	    Experiments experiments = getResponse(url, resource);
	    url = null;
	    BigInteger n = experiments.getCount();
	    log.info("Will be processing " + n + " experiments");
	    List<Link> links = experiments.getLink();
	    
	    //need to change status immediately so that subsequent pulls won't encounter these experiments
	    //if they are still loading results
	    log.info("Setting experiment status to started for all experiments in queue.");
	    for(Link link : links) {
		if(link.getRel().equals(link.getRel().equals("related") &&
			link.getType().equals(CAPConstants.CapResource.EXPERIMENT.getMimeType()))) {
		    String href = link.getHref();
		    //set status to started
		    setExtractionStatus(CAPConstants.CAP_STATUS_STARTED, href, CAPConstants.CapResource.EXPERIMENT);
		}
	    }
	    
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
		    if (handler != null) { 
			//Note status ALREADY set to started

			//load experiment
			handler.process(href, CAPConstants.CapResource.EXPERIMENT);

			//if loading experiment, load experiment results
			handler = CapResourceHandlerRegistry.getInstance().getHandler(CAPConstants.CapResource.RESULT_JSON);
			if (handler != null) {
			    handler.process(href, CAPConstants.CapResource.RESULT_JSON);
			}
			//set status to completed
			this.setExtractionStatus(CAPConstants.CAP_STATUS_COMPLETE, href, CAPConstants.CapResource.EXPERIMENT);
		    }
		}
	    }
	}
    }
}
