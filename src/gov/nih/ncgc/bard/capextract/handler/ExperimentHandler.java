package gov.nih.ncgc.bard.capextract.handler;

import gov.nih.ncgc.bard.capextract.CAPAnnotation;
import gov.nih.ncgc.bard.capextract.CAPConstants;
import gov.nih.ncgc.bard.capextract.CAPUtil;
import gov.nih.ncgc.bard.capextract.ICapResourceHandler;
import gov.nih.ncgc.bard.capextract.jaxb.*;
import gov.nih.ncgc.bard.tools.Util;

import java.io.IOException;
import java.math.BigInteger;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
    private HashMap<BigInteger, String> _CAP_ExptID_PubChemAID_lookup = new HashMap<BigInteger, String>();
    private HashMap<BigInteger, String> _CAP_ExptID_AssayID_lookup = new HashMap<BigInteger, String>();
    private HashMap<String, String> _CAP_ExptID_ProjID_lookup = new HashMap<String, String>();
    private Vector<String[]> _CAP_Proj_Expt_link = new Vector<String[]>();

    public ExperimentHandler() {
        super();
    }

    public HashMap<BigInteger, String> getCAP_Expt_PubChemAID() {
        return this._CAP_ExptID_PubChemAID_lookup;
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

        Experiment expt = getResponse(url, resource);
        BigInteger exptID = expt.getExperimentId();
        log.info("\tProcessing experiment " + exptID + " " + url);

        ExternalReferenceHandler extrefHandler = new ExternalReferenceHandler();

        //String status = expt.getStatus();
        //String extraction = expt.getReadyForExtraction();
        //XMLGregorianCalendar holdUntil = expt.getHoldUntilDate();
        //XMLGregorianCalendar runDateFrom = expt.getRunDateFrom();
        //XMLGregorianCalendar runDateTo = expt.getRunDateTo();

        String assayID = "";

        for (Link link : expt.getLink()) {
            //   <link rel='related' title='Link to Assay' type='application/vnd.bard.cap+xml;type=assay' href='https://bard.broadinstitute.org/dataExport/api/assays/441' />
            if (link.getType().equals(CAPConstants.CapResource.ASSAY.getMimeType())) {
                assayID = Util.getEntityIdFromUrl(link.getHref());
                _CAP_ExptID_AssayID_lookup.put(exptID, assayID);
            } else if (link.getType().equals(CAPConstants.CapResource.EXTREF.getMimeType())) { // get Pubchem AID here
                   extrefHandler.process(link.getHref(), CAPConstants.CapResource.EXTREF);
                System.out.println("link = " + link);
//                _CAP_ExptID_PubChemAID_lookup.put(exptID.toString(), null);
//                ExternalReferences.ExternalReference ref = getResponse(link.getHref(), CAPConstants.CapResource.EXTREF);
//                String externalRef = ref.getExternalAssayRef();
//                Experiment.ExternalReferences.ExternalReference.ExternalSystem sourceObj = ref.getExternalSystem();
//                String source = sourceObj.getName() + "," + sourceObj.getOwner() + "," + sourceObj.getSystemUrl();
//                if (PUBCHEM.equals(source)) {
//                    if (!_CAP_ExptID_PubChemAID_lookup.containsValue(externalRef)) {
//                        _CAP_ExptID_PubChemAID_lookup.put(exptID.toString(), externalRef);
//                    } else {
//                        log.error("The same AID maps to multple experiments: " + externalRef + " eid:" + exptID + " eid:" + _CAP_ExptID_PubChemAID_lookup.get(externalRef));
//                    }
//                } else {
//                    log.error("experiment id: " + exptID + " external source is unknown: " + source);
//                }
            }
        }

        List<CAPAnnotation> annos = new ArrayList<CAPAnnotation>();
        Contexts contexts = expt.getContexts();
        if (contexts != null) {
            for (ContextType context : contexts.getContext()) {
                BigInteger contextId = context.getId();
                String contextName = context.getContextName();
                for (ContextItemType contextItem : context.getContextItems().getContextItem()) {
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

                    annos.add(new CAPAnnotation(contextId.intValue(), expt.getExperimentId().intValue(), valueDisplay, contextName, key, value, null, "cap-context", null, displayOrder, "experiment", null));
                }
            }
        }

        // ready to load in the data
        try {
            int bardExptId = -1;
            Connection conn = CAPUtil.connectToBARD();

            Statement query = conn.createStatement();
            query.execute("select bard_expt_id, cap_expt_id from bard_experiment where cap_expt_id=" + expt.getExperimentId());
            ResultSet rs = query.getResultSet();
            while (rs.next()) {
                bardExptId = rs.getInt(1);
            }
            rs.close();
            query.close();

            // this is a new experiment
            PreparedStatement pstExpt = conn.prepareStatement(
                    "insert into bard_experiment (bard_assay_id, cap_expt_id, category, classification, description, pubchem_aid, type, name) values(?,?,?,?,?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS);
            if (bardExptId == -1) {
                pstExpt.setInt(1, Integer.parseInt(_CAP_ExptID_AssayID_lookup.get(exptID)));
                pstExpt.setInt(2, exptID.intValue());
                pstExpt.setInt(3, -1);
                pstExpt.setInt(4, -1);
                pstExpt.setString(5, expt.getDescription());
                pstExpt.setInt(6, Integer.parseInt(_CAP_ExptID_PubChemAID_lookup.get(exptID)));
                pstExpt.setInt(7, -1);
                pstExpt.setString(8, expt.getExperimentName());

                int insertedRows = pstExpt.executeUpdate();
                if (insertedRows == 0) {

                }
                rs = pstExpt.getGeneratedKeys();
                while (rs.next()) bardExptId = rs.getInt(1);
                rs.close();
                pstExpt.close();
                log.info("Inserted CAP experiment id " + expt + " as BARD experiment id " + bardExptId);
            } else {
                log.info("CAP experiment id " + expt + " already exist. Should do an update");
            }

            PreparedStatement pstAssayAnnot = conn.prepareStatement("insert into cap_annotation (source, entity, entity_id, anno_id, anno_key, anno_value, anno_value_text, anno_display, context_name, related, url, display_order) values(?,'experiment',?,?,?,?,?,?,?,?,?,?)");
            for (CAPAnnotation anno : annos) {
                pstAssayAnnot.setString(1, anno.source);
                pstAssayAnnot.setInt(2, bardExptId);  // TODO or should we use CAP expt id?
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
            pstExpt.executeBatch();
            int[] updateCounts = pstAssayAnnot.executeBatch();
            conn.commit();
            pstAssayAnnot.close();
            conn.close();
            log.info("\tInserted " + updateCounts.length + " annotations for cap aid " + expt.getExperimentId());
        } catch (SQLException e) {
            e.printStackTrace();
            log.error("Error inserting annotations for cap expt id " + expt.getExperimentId() + "\n" + e.getMessage());
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
