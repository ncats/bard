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

//	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/1580",CAPConstants.CapResource.EXPERIMENT);
	h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/experiments/2930",CAPConstants.CapResource.EXPERIMENT);
	
//	try {
//	    handler.process(CAPConstants.CAP_ROOT+"/experiments", CAPConstants.CapResource.EXPERIMENTS);
//	} catch (IOException e) {
//	    // TODO Auto-generated catch block
//	    e.printStackTrace();
//	}
    }

}
