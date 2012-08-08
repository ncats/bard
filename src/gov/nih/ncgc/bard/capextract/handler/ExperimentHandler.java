package gov.nih.ncgc.bard.capextract.handler;

import gov.nih.ncgc.bard.capextract.CAPConstants;
import gov.nih.ncgc.bard.capextract.ICapResourceHandler;
import gov.nih.ncgc.bard.capextract.jaxb.Experiment;
import gov.nih.ncgc.bard.capextract.jaxb.Link;

import java.io.IOException;
import java.math.BigInteger;
import java.util.HashMap;

/**
 * Process CAP <code>Experiment</code> elements.
 * <p/>
 * Currently, the class focuses on getting the lookup from PubChem AID to CAP Experiment and Assay IDs.
 *
 * @author Rajarshi Guha
 */
public class ExperimentHandler extends CapResourceHandler implements ICapResourceHandler {

    static String PUBCHEM = "PubChem,NIH,http://pubchem.ncbi.nlm.nih.gov/assay/assay.cgi?";
    private HashMap<String, String> _PubChemAID_CAP_IDlookup = new HashMap<String, String>(); 
    
    public ExperimentHandler() {
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
        if (resource != CAPConstants.CapResource.EXPERIMENT) return;
        log.info("Processing " + resource);

        // get the Assays object here
        Experiment expt = getResponse(url, resource);

        BigInteger eid = expt.getExperimentId();
        //String status = expt.getStatus();
        //String name = expt.getExperimentName();
        //String desc = expt.getDescription();
        String CAPassay = "";
        for (Link link: expt.getLink()) {
            if (link.getType().equals(CAPConstants.CapResource.ASSAY.getMimeType()))
        	CAPassay = link.getHref().substring(54);
        }
        for (Experiment.ExternalReferences.ExternalReference ref: expt.getExternalReferences().getExternalReference()) {
            String externalRef = ref.getExternalAssayRef();
            Experiment.ExternalReferences.ExternalReference.ExternalSystem sourceObj = ref.getExternalSystem();
            String source = sourceObj.getName() + "," + sourceObj.getOwner() + "," + sourceObj.getSystemUrl();
            if (PUBCHEM.equals(source)) {
        	if (!_PubChemAID_CAP_IDlookup.containsKey(externalRef)) {
        	    _PubChemAID_CAP_IDlookup.put(externalRef, eid + "," + CAPassay);
        	}
        	else log.info("The same AID maps to multple experiments: "+externalRef + " eid:" + eid + " eid:" + _PubChemAID_CAP_IDlookup.get(externalRef));
            } else {
        	log.info("experiment source is unknown: "+source);
            }
        }
    }
    
    public void printLookup() {
	for (String key: _PubChemAID_CAP_IDlookup.keySet())
	    System.out.println(key+","+_PubChemAID_CAP_IDlookup.get(key));
    }
}
