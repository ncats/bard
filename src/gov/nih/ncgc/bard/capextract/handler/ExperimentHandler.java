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
    public void process(String url, CAPConstants.CapResource resource) throws IOException {
        if (resource != CAPConstants.CapResource.EXPERIMENT) return;

        Experiment expt = getResponse(url, resource);
        BigInteger exptID = expt.getExperimentId();
        log.info("Processing experiment " + exptID + " " + url);

        // lets do a first check to see if we have this experiment already
        int localBardExptId = -1;
        try {
            Connection conn = CAPUtil.connectToBARD();
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
            if (bardExptId != -1) {
                log.info("CAP experiment id " + expt.getExperimentId() + " already exist. Should do an update");
                return;
            }
        } catch (SQLException e) {
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
                Connection conn = CAPUtil.connectToBARD();
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
                        log.error("Invalid bardAssayId even after inserting CAP assay id "+capAssayId+". Skipping this err");
                        return;
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
                    ExternalSystems.ExternalSystem extsys = extsysHandler.getExtsys();
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

            // this is a new experiment
            Connection conn = CAPUtil.connectToBARD();
            boolean experimentExists = false;
            PreparedStatement pstExpt = conn.prepareStatement(
                    "insert into bard_experiment (bard_assay_id, cap_expt_id, category, classification, description, pubchem_aid, type, name) values(?,?,?,?,?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS);
            if (localBardExptId == -1) {
                pstExpt.setInt(1,_CAP_ExptID_AssayID_lookup.get(exptID));
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
                ResultSet rs = pstExpt.getGeneratedKeys();
                while (rs.next()) localBardExptId = rs.getInt(1);
                rs.close();
                pstExpt.close();
                log.info("Inserted CAP experiment id " + expt.getExperimentId() + " as BARD experiment id " + localBardExptId);
            } else {
                log.info("CAP experiment id " + expt.getExperimentId() + " already exist. Should do an update");
                experimentExists = true;
            }

            // TODO this block implies we don't update expt annotations for pre-existing expts
            if (!experimentExists) {
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

            conn.close();

        } catch (SQLException e) {
            e.printStackTrace();
            log.error("Error inserting annotations for CAP expt id " + expt.getExperimentId() + "\n" + e.getMessage());
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
