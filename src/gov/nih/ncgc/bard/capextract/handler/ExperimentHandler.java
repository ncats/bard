package gov.nih.ncgc.bard.capextract.handler;

import gov.nih.ncgc.bard.capextract.CAPConstants;
import gov.nih.ncgc.bard.capextract.CAPUtil;
import gov.nih.ncgc.bard.capextract.CapResourceHandlerRegistry;
import gov.nih.ncgc.bard.capextract.ICapResourceHandler;
import gov.nih.ncgc.bard.capextract.jaxb.Experiment;
import gov.nih.ncgc.bard.capextract.jaxb.Link;

import java.io.IOException;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Vector;

/**
 * Process CAP <code>Experiment</code> elements.
 * <p/>
 * Currently, the class focuses on getting the lookup from PubChem AID to CAP Experiment and Assay IDs.
 *
 * @author Rajarshi Guha
 */
public class ExperimentHandler extends CapResourceHandler implements ICapResourceHandler {

    static String PUBCHEM = "PubChem,NIH,http://pubchem.ncbi.nlm.nih.gov/assay/assay.cgi?";
    private HashMap<String, String> _CAP_ExptID_PubChemAID_lookup = new HashMap<String, String>(); 
    private HashMap<String, String> _CAP_ExptID_AssayID_lookup = new HashMap<String, String>(); 
    private HashMap<String, String> _CAP_ExptID_ProjID_lookup = new HashMap<String, String>(); 
    private Vector<String[]> _CAP_Proj_Expt_link = new Vector<String[]>(); 
    
    public ExperimentHandler() {
        super();
    }

    public HashMap<String, String> getCAP_Expt_PubChemAID() {return this._CAP_ExptID_PubChemAID_lookup;}
    
    /**
     * Process a CAP entity that is located at some URL.
     *
     * @param url      The URL from which to retrieve the entity fron
     * @param resource The CAP resource that is meant to be processed. An implementing class
     *                 can choose to proceed or not based on this parameter.
     */
    public void process(String url, CAPConstants.CapResource resource) throws IOException {
        if (resource != CAPConstants.CapResource.EXPERIMENT) return;
        //log.info("Processing " + resource);

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
            if (link.getType().equals(CAPConstants.CapResource.ASSAY.getMimeType())) {
        	assayID = link.getHref().substring(link.getHref().lastIndexOf("assays/")+7);
        	_CAP_ExptID_AssayID_lookup.put(exptID.toString(), assayID);
            }
        }
        _CAP_ExptID_PubChemAID_lookup.put(exptID.toString(), null);
        for (Experiment.ExternalReferences.ExternalReference ref: expt.getExternalReferences().getExternalReference()) {
            String externalRef = ref.getExternalAssayRef();
            Experiment.ExternalReferences.ExternalReference.ExternalSystem sourceObj = ref.getExternalSystem();
            String source = sourceObj.getName() + "," + sourceObj.getOwner() + "," + sourceObj.getSystemUrl();
            if (PUBCHEM.equals(source)) {
        	if (!_CAP_ExptID_PubChemAID_lookup.containsValue(externalRef)) {
        	    _CAP_ExptID_PubChemAID_lookup.put(exptID.toString(), externalRef);
        	}
        	else {
        	    log.error("The same AID maps to multple experiments: "+externalRef + " eid:" + exptID + " eid:" + _CAP_ExptID_PubChemAID_lookup.get(externalRef));
        	}
            } else {
        	log.error("experiment id: "+exptID+" external source is unknown: "+source);
            }
        }
        
        // handle project context of experiment
        if (expt.getProjectSteps() != null)
            for (Experiment.ProjectSteps.ProjectStep projExpt: expt.getProjectSteps().getProjectStep()) {
        	projExpt.getDescription();
        	projExpt.getPrecedingExperiment();
        	for (Link link: projExpt.getLink()) {
        	    if (link.getType().equals(CAPConstants.CapResource.PROJECT.getMimeType())) {
        		String projID = link.getHref().substring(link.getHref().lastIndexOf("/")+1);
        		String[] entry = {projID, exptID.toString()};
                	if (_CAP_ExptID_ProjID_lookup.containsKey(exptID.toString())) {
                	    // experiment maps to multiple projects
                	    _CAP_ExptID_ProjID_lookup.put(exptID.toString(), null);
                	} else {
                	    _CAP_ExptID_ProjID_lookup.put(exptID.toString(), projID);
                	}
        		_CAP_Proj_Expt_link.add(entry);
        	    }
        	    else log.error("Project context not being captured for exptID "+exptID+": "+link.getHref()+" "+projExpt.getDescription()+projExpt.getPrecedingExperiment());

        	}
            }
        
        // TODO handle result context of experiment
        if (expt.getExperimentContextItems() != null)
            for (Experiment.ExperimentContextItems.ExperimentContextItem context: expt.getExperimentContextItems().getExperimentContextItem()) {
        	log.error("Result context item not being captured for exptID "+exptID+": "+context.getExtValueId()+":"+context.getValueDisplay()+":"+context.getAttribute());
            }

// We no longer get results from the CAP Extract API        
//        // get experiment results
//        for (Link link: expt.getLink()) {
//            //<link rel='related' title='List Related Results' type='application/vnd.bard.cap+xml;type=results' href='https://bard.broadinstitute.org/dataExport/api/experiments/439/results?offset=0' /> 
//            if (link.getType().equals(CAPConstants.CapResource.RESULTS.getMimeType())) {
//            	ICapResourceHandler handler = CapResourceHandlerRegistry.getInstance().getHandler(CAPConstants.CapResource.RESULTS);
//            	handler.process(link.getHref(), CAPConstants.CapResource.RESULTS);
//            }
//        }
        
    }
    
    public void printLookup() {
	try {
	    Connection conn = CAPUtil.connectToBARD();
	    Statement st = conn.createStatement();
	    
	    ResultSet result = st.executeQuery("select cap_expt_id, pubchem_aid from bard_experiment"); // where cap_expt_id=3134");
	    while (result.next()) {
		String capExptId = result.getString(1);
		String pubchemAID = "aid="+result.getString(2);
		if (!_CAP_ExptID_PubChemAID_lookup.containsKey(capExptId)) {
		    log.error("CAP Experiment no longer exists: CAP Expt ID="+capExptId);
		} else {
		    if (!_CAP_ExptID_PubChemAID_lookup.get(capExptId).equals(pubchemAID))
			log.error("CAP Experiment now maps to different PubChemAID: CAP Expt ID, PubChemAID="+capExptId+","+_CAP_ExptID_PubChemAID_lookup.get(capExptId));
		    _CAP_ExptID_PubChemAID_lookup.remove(capExptId);
		}
	    }
	    result.close();
	    for (String capExptId: _CAP_ExptID_PubChemAID_lookup.keySet())
		log.error("New CAP Experiment (and AID?): CAP Expt ID="+capExptId+" (AID="+_CAP_ExptID_PubChemAID_lookup.get(capExptId)+")");

	    ResultSet result2 = st.executeQuery("select cap_expt_id, cap_assay_id from bard_experiment"); // where cap_expt_id=3134");
	    while (result2.next()) {
		String capExptId = result2.getString(1);
		String capAssayId = result2.getString(2);
		if (!_CAP_ExptID_AssayID_lookup.containsKey(capExptId)) {
		    log.error("CAP Experiment no longer exists: CAP Expt ID="+capExptId);
		} else {
		    if (!_CAP_ExptID_AssayID_lookup.get(capExptId).equals(capAssayId))
			log.error("CAP Experiment now maps to differen CAP Assay ID: CAP Expt ID="+capExptId);
		    _CAP_ExptID_AssayID_lookup.remove(capExptId);
		}
	    }
	    result2.close();
	    for (String capExptId: _CAP_ExptID_AssayID_lookup.keySet())
		log.error("New CAP Experiment (and CAP AID?): CAP Expt ID="+capExptId+" (AID="+_CAP_ExptID_AssayID_lookup.get(capExptId)+")");

	    ResultSet result3 = st.executeQuery("select b.cap_proj_id, c.cap_expt_id, c.cap_assay_id, a.bard_expt_id, a.bard_proj_id, a.pubchem_aid from bard_project_experiment a, bard_project b, bard_experiment c where a.bard_proj_id=b.bard_proj_id and a.bard_expt_id=c.bard_expt_id"); // and cap_expt_id=3134");
	    while (result3.next()) {
		String capProjId = result3.getString(1);
		String capExptId = result3.getString(2);
		
		int match = -1;
		for (int i=_CAP_Proj_Expt_link.size()-1; i>-1; i--) {
		    if (_CAP_Proj_Expt_link.get(i)[0].equals(capProjId) && 
		    	_CAP_Proj_Expt_link.get(i)[1].equals(capExptId)) {
			match = i;
			_CAP_Proj_Expt_link.remove(match);
		    }
		}
		if (match == -1)
		    log.error("Project Expt link no longer exists: CAP Proj, Expt="+capProjId+","+capExptId);
	    }
	    result3.close();
	    for (String[] newer: _CAP_Proj_Expt_link)
		log.error("New Project Expt link: CAP Proj, Expt="+newer[0]+","+newer[1]);
	} catch (Exception e) {e.printStackTrace();}
		
//	for (String key: _CAP_ExptID_PubChemAID_lookup.keySet())
//	    System.out.println(key+","+_CAP_ExptID_PubChemAID_lookup.get(key)+","+_CAP_ExptID_AssayID_lookup.get(key)+","+_CAP_ExptID_ProjID_lookup.get(key));
//	System.out.println("CAP Project -> Expt Links");
//	for (String[] entry: _CAP_Proj_Expt_link) {
//	    System.out.println(entry[0]+","+entry[1]+","+_CAP_ExptID_AssayID_lookup.get(entry[1])+","+_CAP_ExptID_PubChemAID_lookup.get(entry[1]));
	
    }
}
