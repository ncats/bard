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
	
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/967",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/1462",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/1942",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/2045",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/5157",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/5162",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/5215",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/5217",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/5409",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/5494",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/5504",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/5511",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/5514",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/5627",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/5628",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/5629",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/5630",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/5631",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/5632",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/5633",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/5634",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/5635",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/5637",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/5638",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/5639",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/5640",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/5641",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/5929",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/5957",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/5973",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/5976",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/5977",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/5979",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/5981",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/5982",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/5984",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/5993",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/6018",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/6019",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/6024",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/6025",CAPConstants.CapResource.EXPERIMENT);
//	
//System.out.println("done expt");
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/5168",CAPConstants.CapResource.ASSAY);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/5981",CAPConstants.CapResource.ASSAY);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/5982",CAPConstants.CapResource.ASSAY);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/5984",CAPConstants.CapResource.ASSAY);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/5986",CAPConstants.CapResource.ASSAY);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/5987",CAPConstants.CapResource.ASSAY);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/5934",CAPConstants.CapResource.ASSAY);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/5962",CAPConstants.CapResource.ASSAY);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/5998",CAPConstants.CapResource.ASSAY);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/6029",CAPConstants.CapResource.ASSAY);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/6030",CAPConstants.CapResource.ASSAY);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/6024",CAPConstants.CapResource.ASSAY);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/6023",CAPConstants.CapResource.ASSAY);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/5516",CAPConstants.CapResource.ASSAY);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/5519",CAPConstants.CapResource.ASSAY);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/5640",CAPConstants.CapResource.ASSAY);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/5643",CAPConstants.CapResource.ASSAY);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/5646",CAPConstants.CapResource.ASSAY);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/973",CAPConstants.CapResource.ASSAY);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/5499",CAPConstants.CapResource.ASSAY);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/5632",CAPConstants.CapResource.ASSAY);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/5633",CAPConstants.CapResource.ASSAY);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/5639",CAPConstants.CapResource.ASSAY);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/5989",CAPConstants.CapResource.ASSAY);

//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/3",CAPConstants.CapResource.PROJECT);
	
	
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/754",CAPConstants.CapResource.ASSAY);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/782",CAPConstants.CapResource.ASSAY);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/783",CAPConstants.CapResource.ASSAY);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/817",CAPConstants.CapResource.ASSAY);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/818",CAPConstants.CapResource.ASSAY);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/856",CAPConstants.CapResource.ASSAY);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1017",CAPConstants.CapResource.ASSAY);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1328",CAPConstants.CapResource.ASSAY);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1337",CAPConstants.CapResource.ASSAY);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1339",CAPConstants.CapResource.ASSAY);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1349",CAPConstants.CapResource.ASSAY);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1568",CAPConstants.CapResource.ASSAY);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1612",CAPConstants.CapResource.ASSAY);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1633",CAPConstants.CapResource.ASSAY);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1651",CAPConstants.CapResource.ASSAY);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2360",CAPConstants.CapResource.ASSAY);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2365",CAPConstants.CapResource.ASSAY);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2472",CAPConstants.CapResource.ASSAY);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2487",CAPConstants.CapResource.ASSAY);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2488",CAPConstants.CapResource.ASSAY);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2489",CAPConstants.CapResource.ASSAY);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2490",CAPConstants.CapResource.ASSAY);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2523",CAPConstants.CapResource.ASSAY);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2529",CAPConstants.CapResource.ASSAY);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2534",CAPConstants.CapResource.ASSAY);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2548",CAPConstants.CapResource.ASSAY);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2614",CAPConstants.CapResource.ASSAY);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1578",CAPConstants.CapResource.ASSAY);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1597",CAPConstants.CapResource.ASSAY);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1604",CAPConstants.CapResource.ASSAY);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2615",CAPConstants.CapResource.ASSAY);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2623",CAPConstants.CapResource.ASSAY);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2626",CAPConstants.CapResource.ASSAY);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2826",CAPConstants.CapResource.ASSAY);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3329",CAPConstants.CapResource.ASSAY);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3330",CAPConstants.CapResource.ASSAY);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3331",CAPConstants.CapResource.ASSAY);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3342",CAPConstants.CapResource.ASSAY);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3345",CAPConstants.CapResource.ASSAY);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3608",CAPConstants.CapResource.ASSAY);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3709",CAPConstants.CapResource.ASSAY);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4100",CAPConstants.CapResource.ASSAY);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4193",CAPConstants.CapResource.ASSAY);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4228",CAPConstants.CapResource.ASSAY);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4303",CAPConstants.CapResource.ASSAY);
//
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/747",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/748",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/776",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/777",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/811",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/812",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/827",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/850",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/1011",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/1236",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/1320",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/1322",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/1325",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/1330",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/1331",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/1333",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/1334",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/1343",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/1461",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/1463",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/1481",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/1562",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/1563",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/1569",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/1572",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/1591",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/1598",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/1600",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/1606",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/1621",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/1627",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/1646",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/2355",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/2360",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/2433",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/2467",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/2482",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/2483",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/2484",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/2485",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/2518",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/2524",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/2529",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/2543",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/2607",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/2609",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/2610",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/2617",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/2618",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/2621",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/2779",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/2782",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/2821",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/3324",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/3325",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/3326",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/3337",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/3340",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/3603",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/3650",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/3704",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/3724",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/4033",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/4034",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/4049",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/4051",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/4052",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/4095",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/4188",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/4223",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/4298",CAPConstants.CapResource.EXPERIMENT);
//
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/72",CAPConstants.CapResource.PROJECT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/73",CAPConstants.CapResource.PROJECT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/74",CAPConstants.CapResource.PROJECT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/75",CAPConstants.CapResource.PROJECT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/76",CAPConstants.CapResource.PROJECT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/77",CAPConstants.CapResource.PROJECT);
	
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/1583",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/1847",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/2094",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/2105",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/2602",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/2985",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/3470",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/3593",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/4120",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/1854",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/1874",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/1934",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/2016",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/2092",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/2384",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/2473",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/2763",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/2982",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/2984",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/2986",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/3333",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/3472",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/3526",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/3527",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/3528",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/3573",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/3581",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/3591",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/3592",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/4166",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/4168",CAPConstants.CapResource.EXPERIMENT);

	
	
	
	
//	6036
//	6037
//	6038
//	6039
//	6028
//	6029
//	6033
//	6034
	
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/6036",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/6037",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/6038",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/6039",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/6028",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/6029",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/6033",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/6034",CAPConstants.CapResource.EXPERIMENT);

	int [] ids = {
//		"5976",
//		"5977",
//		"5929",
//		"5993",
//		"6024",
//		"6025",
//		"6019",
//		"5511",
//		"5514",
//		"5635",
//		"5638",
//		"5641",
//		"967",
//		"5637",
//		"5634",
//		"5984",
//		"1325",
//		"1322",
//		"1463",
//		"1330",
//		"1461",
//		"1236",
//		"1333",
//		"1334",
//		"1621",
//		"1606",
//		"1627",
//		"1646",
//		"2518",
//		"1591",
//		"1600",
//		"1598",
//		"3325",
//		"3724",
//		"3326",
//		"3337",
//		"3340",
//		"3603",
//		"4188",
//		"2107",
//		"1991",
//		"793",
//		"1260",
//		"1916",
//		"1447",
//		"1294",
//		"1540",
//		"2065",
//		"1545",
//		"1571",
//		"2473"
//		
//		
//		"2760",
//		"4166",
//		"1581",
//		"1986",
//		"1583",
//		"2014",
//		"1763",
//		"813",
//		"854",
//		"1537",
//		"1827",
//		"1856",
//		"1874",
//		"1884",
//		"1936",
//		"5188",
//		"1968",
//		"1993",
//		"1994",
//		"1998",
//		"5197",
//		"2280",
//		"2016",
//		"339",
//		"1847",
//		"1999",
//		"1854",
//		"705",
//		"2098",
//		"2104",
//		"5214",
//		"2178",
//		"2075",
//		"1580",
//		"1603",
//		"2311",
//		"2384",
//		"2400",
//		"2564",
//		"2602",
//		"2763",
//		"2861",
//		"3177",
//		"3176",
//		"2982",
//		"2984",
//		"2985",
//		"2986",
//		"3114",
//		"3017",
//		"3141"
//		
		
//		"3142",
//		"3143",
//		"3144",
//		"3216",
//		"3527",
//		"3526",
//		"3470",
//		"3528",
//		"3472",
//		"3573",
//		"3581",
//		"3591",
//		"3592",
//		"3593",
//		"3756",
//		"3763",
//		"3919",
//		"3920",
//		"3921",
//		"3923",
//		"3924",
//		"3995",
//		"4102",
//		"4103",
//		"4105",
//		"4106",
//		"4107",
//		"4119",
//		"4120",
//		"4127",
//		"4576",
//		"4577",
//		"4578",
//		"4579",
//		"4582",
//		"2279",
//		"201",
//		"2092",
//		"2282",
//		"203",
//		"1995",
//		"2094",
//		"2109",
//		"2106",
//		"2105",
//		"140",
//		"88",
//		"165",
//		"2291",
//		"2118",
//		"389",
//		"6030",
//		"6029"

		//projects
		//		3,
//		72,
//		73,
//		74,
//		75,
//		76,
//		77,
//		109,
//		110,
//		111,
//		112,
//		113,
//		114,
//		115,
//		116,
//		117,
//		118,
//		119,
//		120,
//		121,
//		122,
//		123,
//		124,
//		125,
//		126,
//		127,
//		128,
//		129,
//		130,
//		131,
//		132,
//		133,
//		134,
//		135,
//		136,
//		137,
//		138,
//		147,
//		148,
//		149,
//		150,
//		151,
//		152,
//		153,
//		154
//		2467,
//		1600
//		3325
		
//		3175,
//		3589,
//		5381
	};

	int i =0;
	for(int id : ids) {
	    h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/"+id,CAPConstants.CapResource.EXPERIMENTS);
	    System.out.println("Change #"+(++i));
	}
	
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/2045",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/5162",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/5976",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/5977",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/5979",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/5981",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/5982",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/5929",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/5957",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/5993",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/6024",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/6025",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/6019",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/6018",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/5504",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/5511",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/5514",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/5635",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/5638",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/5641",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/967",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/5409",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/5633",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/1462",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/5215",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/5157",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/5629",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/5494",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/5632",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/5640",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/5973",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/5217",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/5627",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/5628",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/5639",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/1942",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/5637",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/5631",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/5630",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/5634",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/5984",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/748",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/1563",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/2779",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/776",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/4033",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/1569",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/777",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/4034",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/812",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/850",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/1325",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/1011",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/1463",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/1330",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/1461",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/1236",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/1322",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/1331",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/1333",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/1334",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/1320",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/1343",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/827",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/747",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/2782",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/1562",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/1606",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/1621",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/1627",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/1646",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/2355",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/2607",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/2360",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/2617",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/2467",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/4049",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/2482",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/2483",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/2484",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/2485",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/2518",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/2524",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/2529",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/2543",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/2433",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/2609",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/4051",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/1572",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/1600",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/1591",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/1598",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/4052",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/2610",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/2618",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/3650",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/2621",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/2821",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/3324",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/3325",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/3724",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/3326",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/3337",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/3340",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/3603",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/3704",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/4095",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/4188",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/4223",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/4298",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/811",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/1481",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/798",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/2564",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/2602",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/2763",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/1985",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/2705",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/1581",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/1986",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/2156",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/223",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/163",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/229",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/1545",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/1",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/2065",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/1540",CAPConstants.CapResource.EXPERIMENT);

	//Production assays
	
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
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/834",CAPConstants.CapResource.ASSAY);
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
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/754",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/782",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/783",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/817",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/818",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/856",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1017",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1328",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1337",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1339",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1349",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1568",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1612",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1633",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1651",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2360",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2365",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2472",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2487",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2488",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2489",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2490",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2523",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2529",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2534",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2548",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2614",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1578",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1597",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1604",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2615",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2623",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2626",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2826",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3329",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3330",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3331",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3342",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3345",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3608",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3709",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4100",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4193",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4228",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4303",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/244",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/799",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1266",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1300",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1551",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1574",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/234",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1587",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1589",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1768",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1832",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1861",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1879",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1889",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1937",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1941",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1969",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1973",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1988",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1998",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1999",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2003",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2021",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2091",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2103",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2109",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2161",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2183",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2316",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2389",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2607",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2780",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2781",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2782",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2783",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2866",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2935",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2936",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2987",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2989",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2990",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2991",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3119",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3137",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3138",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3140",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3146",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3147",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3148",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3149",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3221",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3325",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3327",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3333",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3334",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3335",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3341",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3578",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3586",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3596",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3597",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3598",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3624",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3626",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3627",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3761",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3763",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3765",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3766",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3768",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3924",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3925",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3926",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3928",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3929",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4000",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4107",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4108",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4110",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4111",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4112",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4118",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4124",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4125",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4132",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4156",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4159",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4161",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4166",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4352",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4356",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4425",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4515",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4516",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4517",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4521",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4525",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4581",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4582",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4583",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4584",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4585",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4587",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/77",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/420",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/78",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/334",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/112",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/114",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/118",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/142",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/167",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/197",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/233",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2743",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2744",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2745",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2746",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3153",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3250",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3905",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3991",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4341",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4386",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4421",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/840",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/842",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/845",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/851",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/897",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/898",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/906",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1115",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1116",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1118",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1119",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1120",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1121",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1122",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1123",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1125",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1128",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1129",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1130",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1288",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1289",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1360",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1390",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1412",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1413",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1414",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1417",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1418",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1420",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1462",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1484",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1486",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1489",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1502",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1508",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1509",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1510",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1538",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1539",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1544",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1545",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1548",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1563",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1321",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1564",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1565",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1596",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1615",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1629",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1642",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1646",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1664",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1729",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1730",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1732",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1733",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1734",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1735",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1743",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1744",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1751",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1752",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1762",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1763",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1783",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1815",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1914",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1917",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/36",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/325",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/537",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/538",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/543",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/548",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/614",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/627",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/644",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/648",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/707",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/716",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/717",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/760",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/805",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/808",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/814",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/821",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2973",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3020",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3033",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3159",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3175",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3178",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3180",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3206",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3228",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3241",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3242",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3249",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3252",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3260",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3262",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3272",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3277",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3284",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3305",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3314",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3323",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3324",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3326",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3371",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3382",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3391",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3398",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3401",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3402",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3412",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3413",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3418",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3419",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3420",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3431",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3432",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3433",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3436",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3441",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3455",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3456",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3457",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3483",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3520",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3538",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3554",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3568",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3569",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3573",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3575",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3576",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3577",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3587",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3594",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3605",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3612",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3615",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3617",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3621",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3622",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3623",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1918",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1927",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1964",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1965",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1968",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2040",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2051",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2060",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2072",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2175",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2180",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2191",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2193",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2288",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2289",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2290",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2291",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2292",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2293",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2343",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2379",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2387",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2391",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2393",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2411",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2412",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3642",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3673",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3677",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3679",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3682",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3683",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3685",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2417",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2457",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2461",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2469",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2483",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2492",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2493",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2495",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2501",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2549",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2559",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2564",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2565",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2567",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2568",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2577",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2579",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2581",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2582",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2583",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2584",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2585",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2587",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3686",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3688",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3730",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3734",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3736",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3739",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3742",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3744",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3745",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3747",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3755",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3798",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3817",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3874",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3887",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3901",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3902",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3909",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3911",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3930",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3931",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3944",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2589",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2590",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2592",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2593",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2597",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2602",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2606",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2608",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2633",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2723",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2724",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2732",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2844",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2846",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2864",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2865",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2881",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2885",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2887",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3945",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3946",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3947",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3995",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3997",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4032",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4037",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4041",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4043",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4058",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4060",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4061",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4068",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4069",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4073",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4074",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4075",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4076",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4077",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4078",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4080",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4081",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4082",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2906",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2932",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2933",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2955",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2956",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2957",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2958",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2962",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4330",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4355",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4367",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4374",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4387",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4398",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4399",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4401",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4405",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4416",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4418",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4419",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4431",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4463",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4088",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4089",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4141",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4154",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4168",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4203",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4215",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4221",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4226",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4229",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4230",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4241",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4302",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4314",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4315",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4321",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4323",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4324",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4325",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4327",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4329",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4502",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4503",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4507",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4513",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4529",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4533",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4541",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4554",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4558",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4559",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4560",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4563",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4658",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4662",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4663",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4464",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/5382",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/5383",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/5384",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/5385",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/5386",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/5387",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/5884",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/5885",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/6053",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/5360",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/5361",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/5364",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/5365",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/5376",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/5377",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/5378",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/5379",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/5380",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/5381",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3625",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3628",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3639",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3640",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3641",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/5227",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/5239",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/5352",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/5388",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/5441",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/5520",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/5581",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/5582",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/5583",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/5930",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/5931",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/5932",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/5935",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/5971",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/5979",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/6004",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/6006",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/6025",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/6077",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/6078",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/6079",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/6082",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/6083",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/6084",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/6085",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/6086",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/45",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/88",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/89",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/127",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/128",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/129",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/130",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/140",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/151",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/154",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/372",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/502",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/570",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/572",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/576",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/582",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/641",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/642",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/645",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/649",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/657",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/708",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/730",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/798",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/809",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/824",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/847",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/857",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/858",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/859",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/862",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/863",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/864",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/865",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/866",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/867",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/871",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/877",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/878",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/880",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/881",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/889",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/901",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/913",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/930",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/933",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/951",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/964",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/974",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/975",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/979",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/983",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/991",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1011",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1013",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1022",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1025",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1028",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1042",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1050",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1164",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1199",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1250",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1257",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1260",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1261",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1262",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1265",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1267",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1271",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1272",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1277",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1285",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1327",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1332",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1384",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1430",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1436",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1494",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1499",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1504",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1507",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1632",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1672",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1673",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1675",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1738",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1739",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1263",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1741",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1745",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1771",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1772",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1774",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1877",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1945",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1979",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2013",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2014",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2039",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2200",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2215",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2337",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2353",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2370",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2371",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2372",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2464",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2466",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2473",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2475",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2477",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2482",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2484",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2540",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2550",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2551",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2552",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2566",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2571",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2596",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2601",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2627",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2628",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2637",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2638",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2639",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2640",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2642",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2645",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2646",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2647",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2655",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2692",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2698",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2699",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2701",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2705",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2720",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2761",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2843",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2931",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2947",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2983",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2992",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2993",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2995",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/2996",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3007",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3160",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3161",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3173",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3174",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3245",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3389",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3600",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/3779",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4012",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4013",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4205",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4357",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4359",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4368",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4370",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4383",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4705",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4706",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4709",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4710",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/5177",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/5186",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/5197",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/5224",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/5226",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4614",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4880",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4927",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/4981",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/5004",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/5005",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/5006",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/5007",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/5009",CAPConstants.CapResource.ASSAY);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/5056",CAPConstants.CapResource.ASSAY);

	
	//production projects
	
	
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/3",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/72",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/73",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/74",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/75",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/76",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/77",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/109",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/110",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/111",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/112",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/113",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/114",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/115",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/116",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/117",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/118",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/119",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/120",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/121",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/122",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/123",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/124",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/125",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/126",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/127",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/128",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/129",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/130",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/131",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/132",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/133",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/134",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/135",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/136",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/137",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/138",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/147",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/148",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/149",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/150",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/151",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/152",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/153",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/154",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/879",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/880",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/881",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/882",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/883",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/884",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/885",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/886",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/887",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/888",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/889",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/890",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/891",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/892",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/893",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/894",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/895",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/896",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/897",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/898",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/899",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/900",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/901",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/902",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/903",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/904",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/905",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/906",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/907",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/908",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/909",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/910",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/911",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/912",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/913",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/914",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/915",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/916",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/917",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/918",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/919",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/920",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/921",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/922",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/923",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/1114",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/1115",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/1116",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/1117",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/1118",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/1119",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/1120",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/1121",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/1122",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/1123",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/1124",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/1125",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/1126",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/1127",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/1128",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/1129",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/1130",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/1131",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/1132",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/1133",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/1134",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/1135",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/1136",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/1137",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/1138",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/1139",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/1140",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/1141",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/1142",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/1143",CAPConstants.CapResource.PROJECT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/projects/1144",CAPConstants.CapResource.PROJECT);


//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/3",CAPConstants.CapResource.EXPERIMENT);
//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/2930",CAPConstants.CapResource.EXPERIMENT);
	
//	try {
//	    handler.process(CAPConstants.CAP_ROOT+"/experiments", CAPConstants.CapResource.EXPERIMENTS);
//	} catch (IOException e) {
//	    // TODO Auto-generated catch block
//	    e.printStackTrace();
//	}
    }

}
