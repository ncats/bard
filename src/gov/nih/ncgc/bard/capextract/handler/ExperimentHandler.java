package gov.nih.ncgc.bard.capextract.handler;

import gov.nih.ncgc.bard.capextract.CAPAnnotation;
import gov.nih.ncgc.bard.capextract.CAPConstants;
import gov.nih.ncgc.bard.capextract.CAPUtil;
import gov.nih.ncgc.bard.capextract.ICapResourceHandler;
import gov.nih.ncgc.bard.capextract.ScoreHandler;
import gov.nih.ncgc.bard.capextract.jaxb.AbstractContextItemType;
import gov.nih.ncgc.bard.capextract.jaxb.ContextItemType;
import gov.nih.ncgc.bard.capextract.jaxb.ContextType;
import gov.nih.ncgc.bard.capextract.jaxb.ContextType.ContextItems;
import gov.nih.ncgc.bard.capextract.jaxb.Contexts;
import gov.nih.ncgc.bard.capextract.jaxb.Experiment;
import gov.nih.ncgc.bard.capextract.jaxb.ExternalSystem;
import gov.nih.ncgc.bard.capextract.jaxb.Link;
import gov.nih.ncgc.bard.search.SearchUtil;
import gov.nih.ncgc.bard.tools.Util;

import java.io.IOException;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import org.apache.solr.client.solrj.SolrServerException;

/**
 * Process CAP <code>Experiment</code> elements.
 * <p/>
 * Currently, the class focuses on getting the lookup from PubChem AID to CAP Experiment and Assay IDs.
 *
 * @author Rajarshi Guha
 */
public class ExperimentHandler extends CapResourceHandler implements ICapResourceHandler {

    static String PUBCHEM = "PubChem,NIH,http://pubchem.ncbi.nlm.nih.gov/assay/assay.cgi?";
    private HashMap<BigInteger, String> _CAP_ExptID_PubChemAID_lookup = new HashMap<BigInteger, String>();
    private HashMap<BigInteger, Integer> _CAP_ExptID_AssayID_lookup = new HashMap<BigInteger, Integer>();
    private HashMap<String, String> _CAP_ExptID_ProjID_lookup = new HashMap<String, String>();
    private Vector<String[]> _CAP_Proj_Expt_link = new Vector<String[]>();
    private int bardExptId;
    private int pubchemAid;

    public ExperimentHandler() {
        super();
    }

    public int getBardExptId() {
        return bardExptId;
    }

    public int getPubchemAid() {
        return pubchemAid;
    }

    /**
     * Process a CAP entity that is located at some URL.
     *
     * @param url      The URL from which to retrieve the entity fron
     * @param resource The CAP resource that is meant to be processed. An implementing class
     *                 can choose to proceed or not based on this parameter.
     */
    public int process(String url, CAPConstants.CapResource resource) throws IOException {
	//set this to -1 initially. The handler persists and may call on the captured bardExptId
	bardExptId = -1;
	
        if (resource != CAPConstants.CapResource.EXPERIMENT) return CAPConstants.CAP_EXTRACT_LOAD_STATUS_FAILED;

        Experiment expt = getResponse(url, resource);
        if (expt == null) return CAPConstants.CAP_EXTRACT_LOAD_STATUS_FAILED;

        BigInteger exptID = expt.getExperimentId();
        BigInteger confLevel = expt.getConfidenceLevel();
        String status = expt.getStatus();
        String extractionStatus = expt.getReadyForExtraction();
        
        log.info("Processing CAP experiment " + exptID + " " + url);
        log.info("Cap experiment = "+exptID + " status ="+status);
        log.info("Cap experiment = "+exptID + " extraction status ="+extractionStatus);
        
        //first check if it's approved
        if(!"Approved".equals(status) && !"Retired".equals(status) && !"Provisional".equals(status)) {
            log.warn("Unable to process "+ status +" experiments (aborting experiment load), experiment:" + url + " " + status);
            setExtractionStatus("Failed", url, resource);
            return CAPConstants.CAP_EXTRACT_LOAD_STATUS_FAILED;
        }
        
        if("Retired".equals(status)) {
            log.info("RETIRED EXPERIMENT! CAP Experiment " + exptID + " has Retired status. Initiating Retirement.");
            this.retireExperiment(exptID.longValue());
            return CAPConstants.CAP_EXTRACT_LOAD_STATUS_COMPLETE;
        }
        
        //check the EXTRACTION status, if can't determine readyForExtraction, or it's 'Not Ready', don't load.
        if(extractionStatus == null || extractionStatus.equals("Not Ready")) {
            log.warn("Aborting Load!!! Cap experiment = "+exptID + " extraction status ="+extractionStatus);
            return CAPConstants.CAP_EXTRACT_LOAD_STATUS_FAILED;
        }
        
        ExternalReferenceHandler extrefHandler = new ExternalReferenceHandler();
        ExternalSystemHandler extsysHandler = new ExternalSystemHandler();
        AssayHandler assayHandler = new AssayHandler();

        int bardAssayId = -1;

        // first lets go through all the links and look for an assay id
        // given an assay id, check to see if we already loaded it. If
        // so, carry on with the experiment. Otherwise first load the
        // assay and then get the BARD assay id
        for (Link link : expt.getLink()) {
            if (!link.getType().equals(CAPConstants.CapResource.ASSAY.getMimeType())) continue;
            String capAssayId = Util.getEntityIdFromUrl(link.getHref());
            try {
                Connection conn = CAPUtil.connectToBARD(CAPConstants.getBardDBJDBCUrl());
                PreparedStatement pst = conn.prepareStatement("select bard_assay_id, cap_assay_id from bard_assay where cap_assay_id = ?");
                pst.setLong(1, Long.parseLong(capAssayId));
                ResultSet rs = pst.executeQuery();
                while (rs.next()) bardAssayId = rs.getInt("bard_assay_id");
                rs.close();
                pst.close();
                conn.close();
                if (bardAssayId == -1) {
                    assayHandler.process(link.getHref(), CAPConstants.CapResource.ASSAY);
                    bardAssayId = assayHandler.getBardAssayId();
                    if (bardAssayId == -1) {
                        log.error("Invalid (missing referenced assay) bardAssayId even after inserting CAP assay id " + capAssayId + ". ABORTING EXPERIMENT LOAD");
                        bardExptId = -1;
                        return CAPConstants.CAP_EXTRACT_LOAD_STATUS_FAILED;
                    }
                }
                _CAP_ExptID_AssayID_lookup.put(exptID, bardAssayId);
            } catch (SQLException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }

        for (Link link : expt.getLink()) {
            //   <link rel='related' title='Link to Assay' type='application/vnd.bard.cap+xml;type=assay' href='https://bard.broadinstitute.org/dataExport/api/assays/441' />
            if (!link.getType().equals(CAPConstants.CapResource.EXTREF.getMimeType())) continue;

            // get a Pubchem AID
            extrefHandler.process(link.getHref(), CAPConstants.CapResource.EXTREF);
            String externalAssayRef = extrefHandler.getExternalAssayRef();
            String aid = null;
            if (externalAssayRef != null && externalAssayRef.startsWith("aid=")) {
                aid = externalAssayRef.split("=")[1];
            }
            for (Link refLink : extrefHandler.getLinks()) {
                if (refLink.getType().equals(CAPConstants.CapResource.EXTSYS.getMimeType())) {
                    extsysHandler.process(refLink.getHref(), CAPConstants.CapResource.EXTSYS);
                    ExternalSystem extsys = extsysHandler.getExtsys();
                    String source = extsys.getName() + "," + extsys.getOwner() + "," + extsys.getSystemUrl();
                    if (PUBCHEM.equals(source)) {
                        if (_CAP_ExptID_PubChemAID_lookup.containsValue(aid)) {
                            log.error("The same AID maps to multple experiments: " +
                                    aid + " eid:" + exptID + " eid:" + _CAP_ExptID_PubChemAID_lookup.get(exptID));
                            pubchemAid = -1;
                        } else {
                            _CAP_ExptID_PubChemAID_lookup.put(exptID, aid);
                            pubchemAid = Integer.parseInt(aid);
                        }
                    }
                }
            }
        }

        List<CAPAnnotation> annos = new ArrayList<CAPAnnotation>();
        Contexts contexts = expt.getContexts();
        if (contexts != null) {
            for (ContextType context : contexts.getContext()) {
                BigInteger contextId = context.getId();
                String contextName = context.getContextName();
                String contextGroup = context.getContextGroup();
                ContextItems contextItems = context.getContextItems();
                if(contextItems != null) {
                    for (ContextItemType contextItem : contextItems.getContextItem()) {
                	String valueDisplay = contextItem.getValueDisplay();
                	int displayOrder = contextItem.getDisplayOrder();

                	// dict id for the annotation key
                	String key = null;
                	AbstractContextItemType.AttributeId attr = contextItem.getAttributeId();
                	if (attr != null) key = Util.getEntityIdFromUrl(attr.getLink().getHref());

                	// dict id for the annotation value
                	String value = null;
                	AbstractContextItemType.ValueId vc = contextItem.getValueId();
                	if (vc != null) value = Util.getEntityIdFromUrl(vc.getLink().getHref());

                	annos.add(new CAPAnnotation(contextId.intValue(), expt.getExperimentId().intValue(),
                		valueDisplay, contextName, key, value,
                		null, "cap-context", null, displayOrder, "experiment", null, contextGroup));
                    }
                }
            }
        }

        // lets do a first check to see if we have this experiment already
        // 07.17.2013 - moved this block after handling assays in case an assay links to the experment
        // and loads it in the code above.  We need to check for this experiment after handling linked assays.

        int localBardExptId = -1;
        boolean doUpdate = false;
        try {
            Connection conn = CAPUtil.connectToBARD(CAPConstants.getBardDBJDBCUrl());
            Statement query = conn.createStatement();
            query.execute("select bard_expt_id, pubchem_aid from bard_experiment where cap_expt_id=" + expt.getExperimentId());
            ResultSet rs = query.getResultSet();
            while (rs.next()) {
                localBardExptId = rs.getInt(1);
                pubchemAid = rs.getInt(2);
            }
            rs.close();
            query.close();
            conn.close();
            bardExptId = localBardExptId;
            if (bardExptId != -1) doUpdate = true;
        } catch (SQLException e) {
        }
        
        // ready to load in the data
        try {

            Connection conn = CAPUtil.connectToBARD(CAPConstants.getBardDBJDBCUrl());
            String pubchemAidStr = null;
            PreparedStatement pstExpt;
            if (localBardExptId == -1) {
                pstExpt = conn.prepareStatement(
                        "insert into bard_experiment (bard_assay_id, cap_expt_id, category, classification, description, pubchem_aid, type, name, confidence_level, status) values(?,?,?,?,?,?,?,?,?,?)",
                        Statement.RETURN_GENERATED_KEYS);
                log.info("Inserting CAP experiment id " + expt.getExperimentId() + " as BARD experiment id " + localBardExptId);
            } else {
                pstExpt = conn.prepareStatement(
                        "update bard_experiment set bard_assay_id=?, cap_expt_id=?, category=?, classification=?, description=?, pubchem_aid=?, type=?, name=?, confidence_level=?, status=? where bard_expt_id = ?");
                log.info("Updating CAP experiment id " + expt.getExperimentId());
            }
            pstExpt.setInt(1, _CAP_ExptID_AssayID_lookup.get(exptID));
            pstExpt.setInt(2, exptID.intValue());
            pstExpt.setInt(3, -1);
            pstExpt.setInt(4, -1);
            pstExpt.setString(5, expt.getDescription());
            pubchemAidStr = _CAP_ExptID_PubChemAID_lookup.get(exptID);
            if(pubchemAidStr != null)
        	pstExpt.setInt(6, Integer.parseInt(pubchemAidStr));
            else
        	pstExpt.setInt(6, -1);
            pstExpt.setInt(7, -1);
            pstExpt.setString(8, expt.getExperimentName());
            if(confLevel != null)
        	pstExpt.setFloat(9, (float) confLevel.intValue());
            else
        	pstExpt.setNull(9, java.sql.Types.FLOAT);
            pstExpt.setString(10, status);
            if (doUpdate) pstExpt.setLong(11, bardExptId);

            if(doUpdate) {
                // set the updated field even if none of the core entity fields change.
                setEntityUpdateField(bardExptId, resource);
            }
            
            pstExpt.executeUpdate();

            if (!doUpdate) { // get the bard id that we just inserted
                ResultSet rs = pstExpt.getGeneratedKeys();
                while (rs.next()) localBardExptId = rs.getInt(1);
                bardExptId = localBardExptId;
                rs.close();
            }

            pstExpt.close();


            // TODO this block implies we don't update expt annotations for pre-existing expts
            if (!doUpdate) {
                PreparedStatement pstAssayAnnot = conn.prepareStatement("insert into cap_annotation (source, entity, entity_id, anno_id, anno_key, anno_value, anno_value_text, anno_display, context_name, related, url, display_order) values(?,'experiment',?,?,?,?,?,?,?,?,?,?)");
                for (CAPAnnotation anno : annos) {
                    pstAssayAnnot.setString(1, anno.source);
                    pstAssayAnnot.setInt(2, localBardExptId);
                    pstAssayAnnot.setInt(3, anno.id);
                    pstAssayAnnot.setString(4, anno.key);
                    pstAssayAnnot.setString(5, anno.value);
                    pstAssayAnnot.setString(6, anno.extValueId); // anno_value_text
                    pstAssayAnnot.setString(7, anno.display);
                    pstAssayAnnot.setString(8, anno.contextRef); // context_name
                    pstAssayAnnot.setString(9, anno.related); // put into related field
                    pstAssayAnnot.setString(10, anno.url);
                    pstAssayAnnot.setInt(11, anno.displayOrder);
                    pstAssayAnnot.addBatch();
                }
                int[] updateCounts = pstAssayAnnot.executeBatch();
                conn.commit();
                pstAssayAnnot.close();
                log.info("Inserted " + updateCounts.length + " annotations for CAP experiment id " + expt.getExperimentId());
            }

            // Finally we update the scores of connected assays and projects
            ScoreHandler scoreHandler = new ScoreHandler(conn);
            scoreHandler.updateScores(bardExptId);
            conn.commit();
            conn.close();

        } catch (SQLException e) {
            e.printStackTrace();
            log.error("Error inserting/updating the experiment or related annotations (see stack trace) for CAP expt id " + expt.getExperimentId() + "\n" + e.getMessage());
        }
        return CAPConstants.CAP_EXTRACT_LOAD_STATUS_COMPLETE;
    }

    
    public void retireExperiment(long capExptId) {
	
	long bardExptId = 0l;
	    
	
	try {
	    Connection conn = CAPUtil.connectToBARD(CAPConstants.getBardDBJDBCUrl());
	    Statement stmt = conn.createStatement();

	    //get bard_expt_id
	    ResultSet rs = stmt.executeQuery("select bard_expt_id from bard_experiment where cap_expt_id = "+capExptId);
	    if(rs.next()) {
		bardExptId = rs.getLong(1);
	    } else {
		//if bard assay doesn't exist, then we're done.
		log.info("Retirement Log ("+capExptId+"): No bardExptId exists. Exit Retirement.");
		return;
	    }
	    rs.close();

	    //delete experiment
	    stmt.executeUpdate("delete from bard_experiment where bard_expt_id = " + bardExptId);
	    log.info("Retirement Log ("+capExptId+"): Deleting experiment, bard_expt_id: " + bardExptId);

	    //delete experiment data
	    stmt.executeUpdate("delete from bard_experiment_data where bard_expt_id = " + bardExptId);
	    log.info("Retirement Log ("+capExptId+"): Deleting experiment data, bard_expt_id: " + bardExptId);

	    //delete experiment json responses
	    stmt.executeUpdate("delete from bard_experiment_result where bard_expt_id = " + bardExptId);	    
	    log.info("Retirement Log ("+capExptId+"): Deleting experiment results, bard_expt_id: " + bardExptId);

	    //delete exploded data
	    stmt.executeUpdate("delete from exploded_histograms where bard_expt_id = " + bardExptId);	    
	    log.info("Retirement Log ("+capExptId+"): Deleted experiment exploded histograms, bard_expt_id:" + bardExptId);
	    stmt.executeUpdate("delete from exploded_results where bard_expt_id = " + bardExptId);	    
	    log.info("Retirement Log ("+capExptId+"): Deleted experiment exploded results, bard_expt_id:" + bardExptId);
	    stmt.executeUpdate("delete from exploded_statistics where bard_expt_id = " + bardExptId);	    
	    log.info("Retirement Log ("+capExptId+"): Deleted experiment exploded statistics, bard_expt_id:" + bardExptId);

	    //delete project experiment mapping
	    stmt.executeUpdate("delete from bard_project_experiment where bard_expt_id = " + bardExptId);
	    log.info("Retirement Log ("+capExptId+"): Deleting project-experiment mapping, bard_expt_id: " + bardExptId);

	    //delete project experiment steps
	    stmt.executeUpdate("delete from project_step where prev_bard_expt_id = " + bardExptId + 
		    " or next_bard_expt_id = " + bardExptId);
	    log.info("Retirement Log ("+capExptId+"): Deleting project-experiment steps, bard_expt_id: " + bardExptId);

	    //delete experiment annotations
	    stmt.executeUpdate("delete from cap_annotation where entity = 'experiment' and entity_id =" + bardExptId);
	    log.info("Retirement Log ("+capExptId+"): Deleting experiment annotations, bard_expt_id: " + bardExptId);

	    //commit to finish experiment updates to DB
	    conn.commit();
	    conn.close();
	} catch (SQLException sqle) {
	    sqle.printStackTrace();
	}
	
	log.info("Retirement Log ("+capExptId+"): Completed DB clean-up for bardExptID: "+bardExptId);

	
	//clean up related search indices
	String solrCoreUrl = null;
	try {
	    log.info("Retirement Log ("+capExptId+"): Removing documents from SOLR for bardExptID: "+bardExptId);
	    solrCoreUrl = CAPConstants.getSolrURL(CAPConstants.SOLR_RESOURCE_KEY_EXPERIMENT);
	    if(solrCoreUrl != null) {
		SearchUtil.deleteDocs(solrCoreUrl, Long.toString(bardExptId));
		log.info("Retirement Log ("+capExptId+"): Issued command to remove documents from SOLR for bardExptID: "+bardExptId+" SOLR URL:"+solrCoreUrl);
	    } else {
		log.warn("Retirement Log ("+capExptId+"): FAILED to remove documents from SOLR for bardExptID: "+bardExptId+" SOLR URL: NULL!");	    
	    }
	} catch (IOException e) {
	    log.warn("Retirement Log ("+capExptId+"): IOException removing documents from SOLR for bardExptID: "+bardExptId+" SOLR URL:"+solrCoreUrl);	    	
	    e.printStackTrace();
	} catch (SolrServerException e) {
	    log.warn("Retirement Log ("+capExptId+"): SolrServerException, FAILED to remove documents from SOLR for bardExptID: "+bardExptId+" SOLR URL:"+solrCoreUrl);	    
	    e.printStackTrace();
	}

    }
    
//
//    public void printLookup() {
//	try {
//	    Connection conn = CAPUtil.connectToBARD();
//	    Statement st = conn.createStatement();
//
//	    ResultSet result = st.executeQuery("select cap_expt_id, pubchem_aid from bard_experiment"); // where cap_expt_id=3134");
//	    while (result.next()) {
//		String capExptId = result.getString(1);
//		String pubchemAID = "aid="+result.getString(2);
//		if (!_CAP_ExptID_PubChemAID_lookup.containsKey(capExptId)) {
//		    log.error("CAP Experiment no longer exists: CAP Expt ID="+capExptId);
//		} else {
//		    if (!_CAP_ExptID_PubChemAID_lookup.get(capExptId).equals(pubchemAID))
//			log.error("CAP Experiment now maps to different PubChemAID: CAP Expt ID, PubChemAID="+capExptId+","+_CAP_ExptID_PubChemAID_lookup.get(capExptId));
//		    _CAP_ExptID_PubChemAID_lookup.remove(capExptId);
//		}
//	    }
//	    result.close();
//	    for (String capExptId: _CAP_ExptID_PubChemAID_lookup.keySet())
//		log.error("New CAP Experiment (and AID?): CAP Expt ID="+capExptId+" (AID="+_CAP_ExptID_PubChemAID_lookup.get(capExptId)+")");
//
//	    ResultSet result2 = st.executeQuery("select cap_expt_id, cap_assay_id from bard_experiment"); // where cap_expt_id=3134");
//	    while (result2.next()) {
//		String capExptId = result2.getString(1);
//		String capAssayId = result2.getString(2);
//		if (!_CAP_ExptID_AssayID_lookup.containsKey(capExptId)) {
//		    log.error("CAP Experiment no longer exists: CAP Expt ID="+capExptId);
//		} else {
//		    if (!_CAP_ExptID_AssayID_lookup.get(capExptId).equals(capAssayId))
//			log.error("CAP Experiment now maps to differen CAP Assay ID: CAP Expt ID="+capExptId);
//		    _CAP_ExptID_AssayID_lookup.remove(capExptId);
//		}
//	    }
//	    result2.close();
//	    for (String capExptId: _CAP_ExptID_AssayID_lookup.keySet())
//		log.error("New CAP Experiment (and CAP AID?): CAP Expt ID="+capExptId+" (AID="+_CAP_ExptID_AssayID_lookup.get(capExptId)+")");
//
//	    ResultSet result3 = st.executeQuery("select b.cap_proj_id, c.cap_expt_id, c.cap_assay_id, a.bard_expt_id, a.bard_proj_id, a.pubchem_aid from bard_project_experiment a, bard_project b, bard_experiment c where a.bard_proj_id=b.bard_proj_id and a.bard_expt_id=c.bard_expt_id"); // and cap_expt_id=3134");
//	    while (result3.next()) {
//		String capProjId = result3.getString(1);
//		String capExptId = result3.getString(2);
//
//		int match = -1;
//		for (int i=_CAP_Proj_Expt_link.size()-1; i>-1; i--) {
//		    if (_CAP_Proj_Expt_link.get(i)[0].equals(capProjId) &&
//		    	_CAP_Proj_Expt_link.get(i)[1].equals(capExptId)) {
//			match = i;
//			_CAP_Proj_Expt_link.remove(match);
//		    }
//		}
//		if (match == -1)
//		    log.error("Project Expt link no longer exists: CAP Proj, Expt="+capProjId+","+capExptId);
//	    }
//	    result3.close();
//	    for (String[] newer: _CAP_Proj_Expt_link)
//		log.error("New Project Expt link: CAP Proj, Expt="+newer[0]+","+newer[1]);
//	} catch (Exception e) {e.printStackTrace();}
//
////	for (String key: _CAP_ExptID_PubChemAID_lookup.keySet())
////	    System.out.println(key+","+_CAP_ExptID_PubChemAID_lookup.get(key)+","+_CAP_ExptID_AssayID_lookup.get(key)+","+_CAP_ExptID_ProjID_lookup.get(key));
////	System.out.println("CAP Project -> Expt Links");
////	for (String[] entry: _CAP_Proj_Expt_link) {
////	    System.out.println(entry[0]+","+entry[1]+","+_CAP_ExptID_AssayID_lookup.get(entry[1])+","+_CAP_ExptID_PubChemAID_lookup.get(entry[1]));

//    }
}
