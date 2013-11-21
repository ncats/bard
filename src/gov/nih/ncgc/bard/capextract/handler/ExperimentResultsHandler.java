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
	

	int [] ids = {
		//assays
		36,
		45,
		88,
		89,
		112,
		114,
		118,
		127,
		129,
		154,
		239,
		242,
		244,
		325,
		360,
		502,
		570,
		576,
		582,
		614,
		641,
		642,
		670,
		717,
		798,
		817,
		856,
		857,
		858,
		865,
		866,
		871,
		877,
		880,
		881,
		889,
		913,
		974,
		975,
		976,
		979,
		983,
		991,
		1011,
		1013,
		1017,
		1022,
		1025,
		1028,
		1036,
		1121,
		1125,
		1128,
		1130,
		1199,
		1300,
		1328,
		1329,
		1349,
		1357,
		1384,
		1436,
		1462,
		1465,
		1486,
		1489,
		1499,
		1504,
		1507,
		1574,
		1589,
		1592,
		1604,
		1612,
		1632,
		1633,
		1646,
		1651,
		1664,
		1675,
		1720,
		1730,
		1732,
		1733,
		1734,
		1735,
		1738,
		1739,
		1743,
		1744,
		1745,
		1751,
		1752,
		1768,
		1772,
		1774,
		1832,
		1877,
		1879,
		1889,
		1945,
		1973,
		2003,
		2039,
		2051,
		2103,
		2109,
		2155,
		2156,
		2167,
		2200,
		2215,
		2337,
		2353,
		2360,
		2365,
		2372,
		2379,
		2387,
		2401,
		2411,
		2412,
		2461,
		2464,
		2472,
		2473,
		2475,
		2477,
		2483,
		2488,
		2489,
		2490,
		2491,
		2523,
		2527,
		2529,
		2534,
		2536,
		2540,
		2548,
		2551,
		2552,
		2564,
		2567,
		2592,
		2597,
		2614,
		2615,
		2623,
		2626,
		2627,
		2628,
		2637,
		2638,
		2639,
		2642,
		2645,
		2646,
		2647,
		2761,
		2776,
		2799,
		2823,
		2843,
		2935,
		2936,
		3119,
		3137,
		3138,
		3140,
		3174,
		3221,
		3314,
		3382,
		3398,
		3402,
		3413,
		3418,
		3431,
		3432,
		3433,
		3436,
		3441,
		3455,
		3456,
		3457,
		3520,
		3568,
		3600,
		3682,
		3683,
		3685,
		3686,
		3709,
		3730,
		3736,
		3739,
		3744,
		3745,
		3747,
		3755,
		3901,
		3944,
		3945,
		3946,
		3947,
		4012,
		4013,
		4037,
		4058,
		4060,
		4061,
		4068,
		4069,
		4075,
		4077,
		4081,
		4154,
		4215,
		4226,
		4311,
		4315,
		4357,
		4359,
		4368,
		4383,
		4399,
		4405,
		4418,
		4487,
		4658,
		4662,
		4663,
		5168,
		5224,
		5226,
		5239,
		5263,
		5388,
		5441,
		5499,
		5516,
		5520,
		5581,
		5582,
		5583,
		5629,
		5632,
		5633,
		5639,
		5640,
		5646,
		5916,
		5917,
		5918,
		5919,
		5928,
		5932,
		5934,
		5935,
		5954,
		5955,
		5956,
		5971,
		5972,
		5977,
		5987,
		5998,
		6006,
		6009,
		6025,
		8110,
		8111,
		8116,
		8117,
		8118,
		8119,
		8120,
		8121,
		8122,
		8123,
		8124,
		8125,
		8126,
		8130,
		8132,
		8133,
		8176,
		8178,
		8179,
		8180,
		8181,
		8183,
		8184,
		8185,
		8186,
		8266,
		8316,
		8317,
		8427,
		8428,
		8429,
		8430,
		8431
	};

	int i =0;
	for(int id : ids) {
	    h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/"+id, CAPConstants.CapResource.ASSAY);
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
