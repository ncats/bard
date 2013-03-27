package gov.nih.ncgc.bard.capextract.handler;

import gov.nih.ncgc.bard.capextract.CAPConstants;
import gov.nih.ncgc.bard.capextract.CAPConstants.CapResource;
import gov.nih.ncgc.bard.capextract.CAPExtractor;
import gov.nih.ncgc.bard.capextract.CapResourceHandlerRegistry;
import gov.nih.ncgc.bard.capextract.ICapResourceHandler;
import gov.nih.ncgc.bard.capextract.jaxb.Experiments;
import gov.nih.ncgc.bard.capextract.jaxb.Link;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;

/**
 * Handles loading experiment results connected with experiments.
 * The class polls CAP export for experiments, then processess by calling the RESULT_JSON resource handler
 *  
 * @author braistedjc
 *
 */
public class ExperimentResultsHandler extends CapResourceHandler implements ICapResourceHandler {

    public ExperimentResultsHandler() {
	super();
    }
    
    /**
     * Process experiment results for the available experiments.
     * 
     * @param url URL to list all CAP Experiments
     * @param resource CAPResource for experiments.
     */
    public void process(String url, CapResource resource) throws IOException {
        if (resource != CAPConstants.CapResource.EXPERIMENTS) return;
        log.info("Processing " + resource);

        while (url != null) { // in case 206 partial response is returned, we should continue to iterate
            // get the Experiments object here
            Experiments experiments = getResponse(url, resource);
            url = null;
            BigInteger n = experiments.getCount();
            log.info("Will be processing " + n + " experiments result");
            List<Link> links = experiments.getLink();
            for (Link link : links) {
        	if (link.getRel().equals("next")) {
        	    url = link.getHref();
        	    log.info("have next link, not related");
        	} else if (link.getRel().equals("related") &&
        		link.getType().equals(CAPConstants.CapResource.EXPERIMENT.getMimeType())) {

        	    log.info("have related link, process results");
        	    String href = link.getHref();
        	    link.getType();
        	    link.getTitle();

        	    //log.info("\t" + title + "/" + type + "/ href = " + href);
        	    ICapResourceHandler handler = CapResourceHandlerRegistry.getInstance().getHandler(CAPConstants.CapResource.RESULT_JSON);
        	    if (handler != null) { 
        		//set start status
        		setExtractionStatus(CAPConstants.CAP_STATUS_STARTED, href, 
        			CAPConstants.CapResource.EXPERIMENT);
        		//process expt results
        		handler.process(href, CAPConstants.CapResource.RESULT_JSON);
        		//set complete status
        		setExtractionStatus(CAPConstants.CAP_STATUS_COMPLETE, href, 
        			CAPConstants.CapResource.EXPERIMENT);
        	    }
        	}
            }
        }
	
    }
    
    /*
     * Test load
     */
    public static void main(String [] args) {
	
	
	
	
	//initialize handlers
	CAPExtractor extractor = new CAPExtractor();
	extractor.setHandlers();
	
	ExperimentResultsHandler h = new ExperimentResultsHandler();
	
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/967",CAPConstants.CapResource.EXPERIMENT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/1462",CAPConstants.CapResource.EXPERIMENT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/1942",CAPConstants.CapResource.EXPERIMENT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/2045",CAPConstants.CapResource.EXPERIMENT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/5157",CAPConstants.CapResource.EXPERIMENT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/5162",CAPConstants.CapResource.EXPERIMENT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/5215",CAPConstants.CapResource.EXPERIMENT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/5217",CAPConstants.CapResource.EXPERIMENT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/5409",CAPConstants.CapResource.EXPERIMENT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/5494",CAPConstants.CapResource.EXPERIMENT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/5504",CAPConstants.CapResource.EXPERIMENT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/5511",CAPConstants.CapResource.EXPERIMENT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/5514",CAPConstants.CapResource.EXPERIMENT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/5627",CAPConstants.CapResource.EXPERIMENT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/5628",CAPConstants.CapResource.EXPERIMENT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/5629",CAPConstants.CapResource.EXPERIMENT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/5630",CAPConstants.CapResource.EXPERIMENT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/5631",CAPConstants.CapResource.EXPERIMENT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/5632",CAPConstants.CapResource.EXPERIMENT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/5633",CAPConstants.CapResource.EXPERIMENT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/5634",CAPConstants.CapResource.EXPERIMENT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/5635",CAPConstants.CapResource.EXPERIMENT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/5637",CAPConstants.CapResource.EXPERIMENT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/5638",CAPConstants.CapResource.EXPERIMENT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/5639",CAPConstants.CapResource.EXPERIMENT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/5640",CAPConstants.CapResource.EXPERIMENT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/5641",CAPConstants.CapResource.EXPERIMENT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/5929",CAPConstants.CapResource.EXPERIMENT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/5957",CAPConstants.CapResource.EXPERIMENT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/5973",CAPConstants.CapResource.EXPERIMENT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/5976",CAPConstants.CapResource.EXPERIMENT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/5977",CAPConstants.CapResource.EXPERIMENT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/5979",CAPConstants.CapResource.EXPERIMENT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/5981",CAPConstants.CapResource.EXPERIMENT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/5982",CAPConstants.CapResource.EXPERIMENT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/5984",CAPConstants.CapResource.EXPERIMENT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/5993",CAPConstants.CapResource.EXPERIMENT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/6018",CAPConstants.CapResource.EXPERIMENT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/6019",CAPConstants.CapResource.EXPERIMENT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/6024",CAPConstants.CapResource.EXPERIMENT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/6025",CAPConstants.CapResource.EXPERIMENT);
	
System.out.println("done expt");
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/5168",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/5981",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/5982",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/5984",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/5986",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/5987",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/5934",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/5962",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/5998",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/6029",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/6030",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/6024",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/6023",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/5516",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/5519",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/5640",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/5643",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/5646",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/973",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/5499",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/5632",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/5633",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/5639",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/5989",CAPConstants.CapResource.ASSAY);

	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/3",CAPConstants.CapResource.PROJECT);

	
	
//	try {
//	    handler.process(CAPConstants.CAP_ROOT+"/experiments", CAPConstants.CapResource.EXPERIMENTS);
//	} catch (IOException e) {
//	    // TODO Auto-generated catch block
//	    e.printStackTrace();
//	}
    }

}
