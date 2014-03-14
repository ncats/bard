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
    public int process(String url, CapResource resource) throws IOException {
        if (resource != CAPConstants.CapResource.EXPERIMENTS) return CAPConstants.CAP_EXTRACT_LOAD_STATUS_FAILED;
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
        	    int loadStatus;
        	    ICapResourceHandler handler = CapResourceHandlerRegistry.getInstance().getHandler(CAPConstants.CapResource.RESULT_JSON);
        	    if (handler != null) { 
        		//set start status
        		setExtractionStatus(CAPConstants.CAP_STATUS_STARTED, href, 
        			CAPConstants.CapResource.EXPERIMENT);
        		//process expt results
        		loadStatus = handler.process(href, CAPConstants.CapResource.RESULT_JSON);
        		//set complete status
        		if(loadStatus == CAPConstants.CAP_EXTRACT_LOAD_STATUS_COMPLETE) {
        		    setExtractionStatus(CAPConstants.CAP_STATUS_COMPLETE, href, 
        			    CAPConstants.CapResource.EXPERIMENT);
        		} else if(loadStatus == CAPConstants.CAP_EXTRACT_LOAD_STATUS_FAILED) {
        		    setExtractionStatus(CAPConstants.CAP_STATUS_FAILED, href, 
        			    CAPConstants.CapResource.EXPERIMENT);
        		}
        	    }
        	}
            }
        }
	return CAPConstants.CAP_EXTRACT_LOAD_STATUS_COMPLETE;
    }
    
    /*
     * Test load
     */
    public static void main(String [] args) {
	
	//initialize handlers
	CAPExtractor extractor = new CAPExtractor();
	extractor.setHandlers();
	
	ExperimentResultsHandler h = new ExperimentResultsHandler();
	

	int [] ids = {

		//These are SP's to load on 11/19
//		4863,
//		5188,
//		5197,
//		5214,
//		7634,
//		7663,
//		7668,
//		7669,
//		7672,
//		7683,
//		7690,
//		7691,
//		7694,
//		7696,
//		7719,
//		7722,
//		7723,
//		7746,
//		8088
		
		// NCGC experiments to pick up correct cid., done
//		7570,
//		7830,
//		7608,
//		7831,
//		7609,
//		7832,
//		7610,
//		7835,
//		7836,
//		7834,
//		7837,
//		7833
		
		//ASSAYS !!!!! These have panel stubs. DONE!!!!!
//		8143,
//		8145,
//		8148,
//		8141,
//		8144,
//		8146,
//		8149,
//		8150,
//		8151,
//		8152,
//		8153,
//		8154,
//		8147,
//		8155,
//		8156,
//		8157,
//		8158,
//		8159,
//		8160,
//		8161,
//		8162,
//		8163,
//		8164,
//		8165,
//		8166,
//		8167,
//		8168,
//		8169,
//		8170,
//		8171,
//		8172,
//		8174,
//		8175,
//		8173,
//		8142

		
		
// ----- These are experiments to load, SP's		
//		8105,
//		1323,
//		1332,
//		1359,
//		1351,
//		1329,
//		1352,
//		7749,
//		7750,
//		4182,
//		3471,
//		3327,
//		6210,
//		6209,
//		3166,
//		7575,
//		8386,
//		8387,
//		8194,
//		8189,
//		8188,
//		1778,
//		2488
		
		
		74,
		882,
		883


		

	};

	int i =0;
	for(int id : ids) {
	    h.setExtractionStatus("Ready", "https://bard-qa.broadinstitute.org/dataExport/api/projects/"+id, CAPConstants.CapResource.PROJECT);
	    System.out.println("Change #"+(++i));
	}
	
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/163",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/229",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/1545",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/1",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/2065",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/1540",CAPConstants.CapResource.EXPERIMENT);

	//Production assays
	
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/5168",CAPConstants.CapResource.ASSAY);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/5981",CAPConstants.CapResource.ASSAY);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/5982",CAPConstants.CapResource.ASSAY);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/5006",CAPConstants.CapResource.ASSAY);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/5007",CAPConstants.CapResource.ASSAY);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/5009",CAPConstants.CapResource.ASSAY);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/5056",CAPConstants.CapResource.ASSAY);
//
//	
	//production projects
	
//	
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/1140",CAPConstants.CapResource.PROJECT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/1141",CAPConstants.CapResource.PROJECT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/1142",CAPConstants.CapResource.PROJECT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/1143",CAPConstants.CapResource.PROJECT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/1144",CAPConstants.CapResource.PROJECT);


	
    }

}
