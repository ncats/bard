package gov.nih.ncgc.bard.capextract.handler;

import gov.nih.ncgc.bard.capextract.CAPConstants;
import gov.nih.ncgc.bard.capextract.ICapResourceHandler;
import gov.nih.ncgc.bard.capextract.jaxb.Link;
import gov.nih.ncgc.bard.capextract.jaxb.Result;
import gov.nih.ncgc.bard.capextract.jaxb.Result.ResultHierarchies;
import gov.nih.ncgc.bard.capextract.jaxb.Result.ResultHierarchies.ResultHierarchy;
import gov.nih.ncgc.bard.capextract.jaxb.ResultContextItems;
import gov.nih.ncgc.bard.capextract.jaxb.ResultContextItems.ResultContextItem;

import java.io.IOException;

/**
 * Process CAP <code>Experiment</code> elements.
 * <p/>
 * Currently, the class focuses on getting the lookup from PubChem AID to CAP Experiment and Assay IDs.
 *
 * @author Rajarshi Guha
 */
public class ResultHandler extends CapResourceHandler implements ICapResourceHandler {

    public ResultHandler() {
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
        if (resource != CAPConstants.CapResource.RESULT) return;
        //log.info("Processing " + resource);

        // get the result object here
        Result result = getResponse(url, resource);
        result.getStatus();
        result.getReadyForExtraction();
        result.getValueDisplay();
        result.getQualifier();
        result.getValueNum();
        result.getValueMax();
        result.getValueMin();
        if (result.getResultType() != null) {
            result.getResultType().getLabel();
            if (result.getResultType().getLink() != null) {
        	result.getResultType().getLink().getType();
        	result.getResultType().getLink().getHref();
            }
        }
        String sid = result.getSubstance().getSid();
        String resultid = null;
        String experimentid = null;
        for (Link link: result.getLink()) {
            if (link.getType().equals(CAPConstants.CapResource.RESULT.getMimeType()))
        	resultid = link.getHref().substring(link.getHref().lastIndexOf('/')+1);
            if (link.getType().equals(CAPConstants.CapResource.EXPERIMENT.getMimeType()))
        	experimentid = link.getHref().substring(link.getHref().lastIndexOf('/')+1);
        }       
        
        ResultContextItems rcis = result.getResultContextItems();
        if (rcis != null) {
            for (ResultContextItem rci: rcis.getResultContextItem()) {
        	rci.getResultContextItemId();
        	rci.getParentGroup();
        	rci.getExtValueId();
        	rci.getValueDisplay();
        	rci.getQualifier();
        	rci.getValueNum();
        	rci.getValueMin();
        	rci.getValueMax();
        	rci.getAttribute().getLabel();
        	rci.getAttribute().getLink().getType();
        	rci.getAttribute().getLink().getHref();
            }
        }
        
        ResultHierarchies rhs = result.getResultHierarchies();
        String rhString = "";
        if (rhs != null) {
            for (ResultHierarchy rh: rhs.getResultHierarchy()) {
        	rh.getParentResultId();
        	rh.getHierarchyType();
        	rhString += rh.getParentResultId()+":"+rh.getHierarchyType()+" ";
            }
        }
        log.info(rhString);
    }
}
