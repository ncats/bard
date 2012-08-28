package gov.nih.ncgc.bard.capextract.handler;

import gov.nih.ncgc.bard.capextract.CAPConstants;
import gov.nih.ncgc.bard.capextract.ICapResourceHandler;
import gov.nih.ncgc.bard.capextract.jaxb.Experiment;
import gov.nih.ncgc.bard.capextract.jaxb.Link;

import java.io.IOException;
import java.math.BigInteger;
import java.util.HashMap;

import javax.xml.datatype.XMLGregorianCalendar;

/**
 * Process CAP <code>Experiment</code> elements.
 * <p/>
 * Currently, the class focuses on getting the lookup from PubChem AID to CAP Experiment and Assay IDs.
 *
 * @author Rajarshi Guha
 */
public class ExperimentHandler extends CapResourceHandler implements ICapResourceHandler {

    static String PUBCHEM = "PubChem,NIH,http://pubchem.ncbi.nlm.nih.gov/assay/assay.cgi?";
    private HashMap<String, String> _CAP_ID_PubChemAID_lookup = new HashMap<String, String>(); 
    
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

        BigInteger exptID = expt.getExperimentId();
        //String status = expt.getStatus();
        //String extraction = expt.getReadyForExtraction();
        //String name = expt.getExperimentName();
        //String desc = expt.getDescription();
        //XMLGregorianCalendar holdUntil = expt.getHoldUntilDate();
        //XMLGregorianCalendar runDateFrom = expt.getRunDateFrom();
        //XMLGregorianCalendar runDateTo = expt.getRunDateTo();
        String assayID = "";
        for (Link link: expt.getLink()) {
            //   <link rel='related' title='Link to Assay' type='application/vnd.bard.cap+xml;type=assay' href='https://bard.broadinstitute.org/dataExport/api/assays/441' />
            if (link.getType().equals(CAPConstants.CapResource.ASSAY.getMimeType()))
        	assayID = link.getHref().substring(link.getHref().lastIndexOf("assays/")+7);
        }
        _CAP_ID_PubChemAID_lookup.put(exptID + "," + assayID, null);
        for (Experiment.ExternalReferences.ExternalReference ref: expt.getExternalReferences().getExternalReference()) {
            String externalRef = ref.getExternalAssayRef();
            Experiment.ExternalReferences.ExternalReference.ExternalSystem sourceObj = ref.getExternalSystem();
            String source = sourceObj.getName() + "," + sourceObj.getOwner() + "," + sourceObj.getSystemUrl();
            if (PUBCHEM.equals(source)) {
        	if (!_CAP_ID_PubChemAID_lookup.containsValue(externalRef)) {
        	    _CAP_ID_PubChemAID_lookup.put(exptID + "," + assayID, externalRef);
        	}
        	else log.info("The same AID maps to multple experiments: "+externalRef + " eid:" + exptID + " eid:" + _CAP_ID_PubChemAID_lookup.get(externalRef));
            } else {
        	log.info("experiment id: "+exptID+" external source is unknown: "+source);
            }
        }
        // TODO handle project context of experiment
        if (expt.getProjectExperiments() != null)
            for (Experiment.ProjectExperiments.ProjectExperiment projExpt: expt.getProjectExperiments().getProjectExperiment()) {
        	projExpt.getDescription();
        	projExpt.getPrecedingExperiment();
        	for (Link link: projExpt.getLink()) {
        	    if (link.getType().equals(CAPConstants.CapResource.STAGE.getMimeType()))
        		link.getHref();
        	}
            }
        // TODO handle result context of experiment
        expt.getResultContextItems();

    }
    
    public void printLookup() {
	for (String key: _CAP_ID_PubChemAID_lookup.keySet())
	    System.out.println(key+","+_CAP_ID_PubChemAID_lookup.get(key));
    }
}
