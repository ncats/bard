package gov.nih.ncgc.bard.capextract.handler;

import gov.nih.ncgc.bard.capextract.CAPAnnotation;
import gov.nih.ncgc.bard.capextract.CAPConstants;
import gov.nih.ncgc.bard.capextract.CAPDictionary;
import gov.nih.ncgc.bard.capextract.CAPDictionaryElement;
import gov.nih.ncgc.bard.capextract.CAPUtil;
import gov.nih.ncgc.bard.capextract.CapResourceHandlerRegistry;
import gov.nih.ncgc.bard.capextract.ICapResourceHandler;
import gov.nih.ncgc.bard.capextract.jaxb.AbstractContextItemType;
import gov.nih.ncgc.bard.capextract.jaxb.ContextItemType;
import gov.nih.ncgc.bard.capextract.jaxb.ContextType;
import gov.nih.ncgc.bard.capextract.jaxb.Contexts;
import gov.nih.ncgc.bard.capextract.jaxb.DocumentType;
import gov.nih.ncgc.bard.capextract.jaxb.ExternalSystem;
import gov.nih.ncgc.bard.capextract.jaxb.Link;
import gov.nih.ncgc.bard.capextract.jaxb.Project;
import gov.nih.ncgc.bard.capextract.jaxb.ProjectExperiment;
import gov.nih.ncgc.bard.capextract.jaxb.ProjectStep;
import gov.nih.ncgc.bard.entity.Biology;
import gov.nih.ncgc.bard.tools.Util;

import java.io.IOException;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBElement;

import nu.xom.ParsingException;

import org.w3c.dom.Node;

import com.sun.org.apache.xerces.internal.dom.ElementNSImpl;

/**
 * A one line summary.
 *
 * @author Rajarshi Guha
 */
public class ProjectHandler extends CapResourceHandler implements ICapResourceHandler {
    boolean projectExists = false;
    int bardProjId = -1;
    Connection conn;

    final static String PUBCHEM = "PubChem,NIH,http://pubchem.ncbi.nlm.nih.gov/assay/assay.cgi?";

    public ProjectHandler() {
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
        if (resource != CAPConstants.CapResource.PROJECT) return;

        // get the Project object here
        Project project = getResponse(url, resource);
        if (project == null) return;

        if (project.getProjectSteps() == null) {
            log.info("$$$ null project steps");
        } else {
            log.info("$$$ have project steps!!!");
            if (project.getProjectSteps().getProjectStep() != null) {
                log.info("$$$ have step list, list size=" + project.getProjectSteps().getProjectStep().size());
            }
        }

        String readyToXtract = project.getReadyForExtraction();
        String title = project.getProjectName();
        BigInteger pid = project.getProjectId();

//        log.info("\taurl = [" + readyToXtract + "] for " + title + " pid " + pid);

        //JB: Note, project will not be exposed unless it's 'Ready'    
        process(project);

    }

    public void process(Project project) {
        String readyToXtract = project.getReadyForExtraction();
        if (!"Ready".equals(readyToXtract)) log.error("Proceeding even though project not ready: " + readyToXtract);

        String groupType = project.getGroupType();
        if (!"Project".equals(groupType)) log.error("Group type other than Project: " + groupType);

        ExternalReferenceHandler extrefHandler = new ExternalReferenceHandler();
        ExternalSystemHandler extsysHandler = new ExternalSystemHandler();

        // 3/26/13 note: we used to perform a check for projectSteps, they are no longer mandatory for project loading

        int capProjectId = project.getProjectId().intValue();
        int pubchemAid = -1;

        try {
            // look for a Pubchem AID (ie summary aid)
            for (Link link : project.getLink()) {
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
                        if (PUBCHEM.equals(source)) pubchemAid = Integer.parseInt(aid);
                    }
                }
            }
            log.info("Got Pubchem AID = " + pubchemAid + " for CAP project id = " + capProjectId);


            conn = CAPUtil.connectToBARD(CAPConstants.getBardDBJDBCUrl());
            PreparedStatement pst = null;
            Statement st = conn.createStatement();
            ResultSet result = st.executeQuery("select bard_proj_id, name, description from bard_project where cap_proj_id=" + capProjectId);
            bardProjId = -1;
            while (result.next()) bardProjId = result.getInt(1);
            result.close();

            if (bardProjId == -1) {
                log.info("Will insert new project for CAP project id = " + capProjectId);
                pst = conn.prepareStatement(
                        "insert into bard_project (cap_proj_id, name, description, pubchem_aid) values (?,?,?,?)",
                        Statement.RETURN_GENERATED_KEYS);
                pst.setInt(1, capProjectId);
                pst.setString(2, project.getProjectName());
                pst.setString(3, project.getDescription());
                pst.setInt(4, pubchemAid);
                int insertedRows = pst.executeUpdate();
                if (insertedRows == 0) {
                    log.error("Could not insert project into bard_project for CAP project id = " + capProjectId);
                    throw new SQLException();
                }
                result = pst.getGeneratedKeys();
                while (result.next()) bardProjId = result.getInt(1);
                result.close();
                pst.close();
                log.info("Inserted CAP project id " + capProjectId + " as BARD project id " + bardProjId);
            } else {
                projectExists = true;
                log.info("Will do an update for the CAP project id = " + project.getProjectId());

                // set the updated field even if none of the core entity fields change.
                setEntityUpdateField(bardProjId, CAPConstants.CapResource.PROJECT);

                pst = conn.prepareStatement("update bard_project set name=?, description=?, pubchem_aid=? where cap_proj_id = ?");
                pst.setString(1, project.getProjectName());
                pst.setString(2, project.getDescription());
                pst.setInt(3, pubchemAid);
                pst.setInt(4, capProjectId);
                pst.executeUpdate();
            }

            //  at this point we have a valid bard project id, lets insert all the extra stuff


            List<CAPAnnotation> annos = new ArrayList<CAPAnnotation>();

            // deal with project annotations
            annos.addAll(processAnnotations(project));

            // handle project documents - we'll get back documents as CAPAnnotation objects
            annos.addAll(processDocuments(project));

            // deal with targets
            processTargets(project);

            // load expts
            processExperiments(project, pubchemAid);

            // handle project steps and include anny anno's we get from this, possibly empty
            annos.addAll(processProjectSteps(project));

            /*
             *  The sections below clear project annoations and probe info
             */
            
            // delete all the annos for the project before reload...
            pst = conn.prepareStatement("delete from cap_project_annotation where bard_proj_id = ?");
            pst.setLong(1, bardProjId);
            pst.executeUpdate();
            pst.close();
            
            //we need to clear probe information before storing. An update will not remove probes that have been removed.
            //its assumed that a CAP project record is complete, not incremental.
            pst = conn.prepareStatement("delete from project_probe where bard_proj_id = ?");
            pst.setLong(1, bardProjId);
            pst.execute();
            pst.close();
            
            // store the annotations we've collected
            if (annos.size() > 0) {

                // now lets insert them all
                PreparedStatement pstAnnot = conn.prepareStatement("replace into cap_project_annotation (bard_proj_id, cap_proj_id, source, entity, anno_id, anno_key, anno_value, anno_display, related, context_name, display_order, url) values (?,?,?,?,?,?,?,?,?,?,?,?)");
                for (CAPAnnotation anno : annos) {
                    pstAnnot.setInt(1, anno.entityId); // for project this is bard_project.bardProjId, for project-step this is project_step.stepId
                    pstAnnot.setInt(2, project.getProjectId().intValue());
                    pstAnnot.setString(3, anno.source);

                    if (anno.entity.equals("project-step")) {
                        pstAnnot.setString(4, "project-step");
                    } else {
                        pstAnnot.setString(4, "project");
                    }

                    pstAnnot.setInt(5, anno.id);
                    pstAnnot.setString(6, anno.key); // anno_value_text
                    pstAnnot.setString(7, anno.value);
                    pstAnnot.setString(8, anno.display); // context_name
                    pstAnnot.setString(9, anno.related); // put into related field
                    pstAnnot.setString(10, anno.contextRef);
                    pstAnnot.setInt(11, anno.displayOrder);
                    pstAnnot.setString(12, anno.url);
                    try {
                        pstAnnot.executeUpdate();
                    } catch (com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException e) {
                        log.info("Got a integrity violation constraint. Probably this anno " + anno.id + " already exists");
                    }
                }
                conn.commit();
                pstAnnot.close();
                log.info("\tLoaded " + annos.size() + " annotations (from " + annos.size() + " CAP annotations) for cap project id " + project.getProjectId());
            }
            
            updateProbeLinks(annos, (long) bardProjId);

            conn.commit();
            st.close();
            pst.close();
            conn.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (ParsingException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (ClassNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    void updateProbeLinks(List<CAPAnnotation> annos, Long bardProjId) {

        // we look at biology for this project and just pull out protein targets
        List<String> targetAccs = new ArrayList<String>();
        try {
            PreparedStatement targetPst = conn.prepareStatement("select ext_id from bard_biology where entity = 'project' and biology_dict_id = 1398 and entity_id = ?");
            targetPst.setLong(1, bardProjId);
            ResultSet rs = targetPst.executeQuery();
            while (rs.next()) targetAccs.add(rs.getString(1));
            rs.close();
            targetPst.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        CAPDictionary dict = CAPConstants.getDictionary();
        Map<Integer, List<CAPAnnotation>> annoGroups = groupAnnotationsByAnnoId(annos);
        for (Integer annoId : annoGroups.keySet()) {
            List<CAPAnnotation> grp = annoGroups.get(annoId);

            boolean isProbeContext = false;
            for (CAPAnnotation anno : grp) {
                if (Util.isNumber(anno.key) && Integer.parseInt(anno.key) == 1776) {
                    isProbeContext = true;
                    break;
                }
            }
            if (!isProbeContext) continue;

            Long cid = null, sid = null;
            String mlid = null, mlidurl = null;
            // pull out cid,sid,mlid for this probe context
            for (CAPAnnotation anno : grp) {
                if (Util.isNumber(anno.key) && anno.display != null) {
                    if (dict.getNode(new BigInteger(anno.key)).getLabel().toLowerCase().contains("pubchem cid"))
                        cid = Long.parseLong(anno.extValueId);
                    else if (dict.getNode(new BigInteger(anno.key)).getLabel().toLowerCase().contains("pubchem sid")) {
                        sid = Long.parseLong(anno.extValueId.split(" ")[0]);
                    } else if (dict.getNode(new BigInteger(anno.key)).getLabel().toLowerCase().contains("probe report")) {
                        mlid = anno.display;
                        mlidurl = anno.url;
                    }
                }
            }

            PreparedStatement pst;
            try {

                // fill in cid, if we didn't get it
                if (cid == null && sid != null) {
                    pst = conn.prepareStatement("select distinct cid from cid_sid where sid = ?");
                    pst.setLong(1, sid);
                    ResultSet rs = pst.executeQuery();
                    while (rs.next()) cid = rs.getLong("cid");
                    rs.close();
                    pst.close();
                }

                // if there's still no cid, skip it
                if (cid == null) {
                    log.warn("No CID for SID " + sid + " in BARD project ID " + bardProjId + ", probe id " + mlid);
                    continue;
                }

                pst = conn.prepareStatement("select * from project_probe where bard_proj_id = ? and cid = ?");
                pst.setLong(1, bardProjId);
                pst.setLong(2, cid);
                ResultSet rs = pst.executeQuery();
                boolean linkExists = false;
                while (rs.next()) linkExists = true;
                rs.close();
                pst.close();

                if (linkExists) { //  will this ever happen?
                    pst = conn.prepareStatement("update project_probe set probe_id = ? where bard_proj_id = ? and cid = ?");
                    pst.setString(1, mlid);
                    pst.setLong(2, bardProjId);
                    pst.setLong(3, cid);
                    pst.executeUpdate();
                    log.info("Updated probe-project link for BARD project id " + bardProjId + " and probe id " + mlid + " CID " + cid);
                } else {
                    pst = conn.prepareStatement("insert into project_probe (bard_proj_id, cid, sid, probe_id, bard_expt_id) values (?,?,null,?, -1)");
                    pst.setLong(1, bardProjId);
                    pst.setLong(2, cid);
                    pst.setString(3, mlid);
                    try {
                        pst.executeUpdate();
                    } catch (com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException e) {
                    }
                    log.info("Made probe-project link for BARD project id " + bardProjId + " and probe id " + mlid + " CID " + cid);
                }
                pst.close();

                // next we update the compound table
                pst = conn.prepareStatement("update compound set compound_class = 'ML Probe', probe_id = ?, url = ?, updated = now() where cid = ?");
                pst.setString(1, mlid);
                pst.setString(2, mlidurl);
                pst.setLong(3, cid);
                pst.executeUpdate();
                pst.close();
                log.info("Updated compound class for CID " + cid);

                // finally update the compound target table by assuming that all project targets
                // are also probe targets
                pst = conn.prepareStatement("insert into compound_target (cid, target_acc, evidence) values (?, ?, 'probe')");
                for (String acc : targetAccs) {
                    pst.setLong(1, cid);
                    pst.setString(2, acc);
                    try {
                        pst.executeUpdate();
                    } catch (com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException e) {
                    }
                    pst.clearParameters();
                    log.info("Updated compound target for CID " + cid + " with Uniprot " + acc);
                }
                pst.close();

            } catch (SQLException e) {
                e.printStackTrace();
            }


        }
    }

    Map<Integer, List<CAPAnnotation>> groupAnnotationsByAnnoId(List<CAPAnnotation> annos) {
        Map<Integer, List<CAPAnnotation>> ret = new HashMap<Integer, List<CAPAnnotation>>();
        for (CAPAnnotation anno : annos) {
            Integer id = anno.id;
            List<CAPAnnotation> l;
            if (ret.containsKey(id)) {
                l = ret.get(id);
            } else {
                l = new ArrayList<CAPAnnotation>();
            }
            l.add(anno);
            ret.put(id, l);
        }
        return ret;
    }

    List<CAPAnnotation> processAnnotations(Project project) {
        List<CAPAnnotation> annos = new ArrayList<CAPAnnotation>();
        CAPDictionary dict = CAPConstants.getDictionary();
        Contexts contexts = project.getContexts();

        // if there are contexts, process them, else skip processing
        // and return the empty annotation list. Contexts may be missing.
        if (contexts != null) {
            List<ContextType> contextTypes = contexts.getContext();
            for (ContextType contextType : contextTypes) {
                int contextId = contextType.getId().intValue();
                String contextName = contextType.getContextName();
                String contextGroup = contextType.getContextGroup();

                ContextType.ContextItems contextItems = contextType.getContextItems();
                if (contextItems == null) {
                    log.warn("Context ID " + contextId + " for CAP project " + project.getProjectId() + " was null (ie had no context items)");
                    continue;
                }

                for (ContextItemType contextItemType : contextItems.getContextItem()) {

                    // dict id for the annotation key
                    String key = null;
                    AbstractContextItemType.AttributeId attr = contextItemType.getAttributeId();
                    if (attr != null) {
                        key = Util.getEntityIdFromUrl(attr.getLink().getHref());
                    }

                    // dict id for the annotation value
                    String valueUrl = null;
                    String value = null;

                    String extValueId = contextItemType.getExtValueId();
                    String valueDisplay = contextItemType.getValueDisplay();
                    String related = null;

                    AbstractContextItemType.ValueId vc = contextItemType.getValueId();
                    if (vc != null) {
                        value = Util.getEntityIdFromUrl(vc.getLink().getHref());
                        String dictUrl = dict.getNode(vc.getLabel()).getExternalUrl();
                        if (dictUrl != null && !dictUrl.equals("null") && extValueId != null)
                            valueUrl = dictUrl + extValueId;
                    } else {
                        // if there is no valueId field and there is an extValueId field, we
                        // construct the valueUrl from the key + extValueId
                        if (extValueId != null) {
                            CAPDictionaryElement dictNode = dict.getNode(new BigInteger(key));
                            valueUrl = dictNode.getExternalUrl() == null ? "" : dictNode.getExternalUrl() + extValueId;
                        }
                    }

                    // hack so that CID gets displayed rather than IUPAC name due to weird inconsistency in CAP annotations
                    if (attr != null && attr.getLabel().contains("CID") && extValueId != null)
                        valueDisplay = extValueId;

                    annos.add(new CAPAnnotation(contextId, bardProjId, valueDisplay, contextName, key, value,
                            contextItemType.getExtValueId(), "cap-context", valueUrl,
                            contextItemType.getDisplayOrder(), "project", related, contextGroup));
                }
            }
        }
        return annos;
    }

    void processTargets(Project project) throws SQLException, ClassNotFoundException, IOException {

        // pull all biologies out
        CAPDictionary dict = CAPUtil.getCAPDictionary();
        List<BiologyInfo> bioInfo = new ArrayList<BiologyInfo>();

        Contexts contexts = project.getContexts();
        if (contexts == null) return;

        List<ContextType> contextTypes = contexts.getContext();
        if (contextTypes == null) return;

        // Loop over all contexts
        for (ContextType contextType : contextTypes) {
            String contextName = contextType.getContextName();
            ContextType.ContextItems contextItems = contextType.getContextItems();
            if (contextItems == null) continue;

            // is this a biology context?
            boolean isBiologyContext = false;
            for (ContextItemType contextItemType : contextItems.getContextItem()) {
                AbstractContextItemType.AttributeId attr = contextItemType.getAttributeId();
                String dictId = Util.getEntityIdFromUrl(attr.getLink().getHref());
                if (dictId != null && dictId.equals("541")) {
                    isBiologyContext = true;
                    break;
                }
            }
            isBiologyContext = isBiologyContext || contextName.equals("biology");
            if (!isBiologyContext) continue;

            List<Integer> targetDictIds = Arrays.asList(new Integer[]{
                    525, 507, 1419, 885, 1795, 880, 881, 882, 883, 1398, 1504
            });
            for (ContextItemType contextItemType : contextItems.getContextItem()) {
                AbstractContextItemType.AttributeId attrid = contextItemType.getAttributeId();
                String dictId = Util.getEntityIdFromUrl(attrid.getLink().getHref());
                if (Util.isNumber(dictId) && targetDictIds.contains(Integer.parseInt(dictId))) {
                    CAPDictionaryElement node = dict.getNode(new BigInteger(dictId));
                    String dictLabel = node.getLabel();
                    String extId = contextItemType.getExtValueId();
                    String description = contextItemType.getValueDisplay();
                    BiologyInfo bi = new BiologyInfo(dictLabel, Integer.parseInt(dictId), extId, null, description);
                    bioInfo.add(bi);
                }
            }
        }

        // delete pre-existing biology for this id
        PreparedStatement pst = conn.prepareStatement("delete from bard_biology where entity = 'project' and entity_id = ?");
        pst.setInt(1, bardProjId);
        pst.executeUpdate();
        pst.close();

        // lets dump to db
        PreparedStatement pstTarget =
                conn.prepareStatement("insert into bard_biology (biology, biology_dict_id, biology_dict_label, description, entity, entity_id, ext_id, ext_ref) " +
                        " values (?,?,?,?,?,?,?,?)");
        for (BiologyInfo abi : bioInfo) {
            String biology = Biology.BiologyType.getBiologyTypeFromDictId(abi.dictId).toString();
            pstTarget.setString(1, biology);
            pstTarget.setInt(2, abi.dictId);
            pstTarget.setString(3, abi.dictLabel);
            pstTarget.setString(4, abi.description);
            pstTarget.setString(5, "project");
            pstTarget.setInt(6, bardProjId);
            pstTarget.setString(7, abi.extId);
            pstTarget.setString(8, abi.extRef);
            try {
                pstTarget.executeUpdate();
            } catch (com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException e) {
            }
            pstTarget.clearParameters();
        }
        pstTarget.close();
        log.info("Inserted " + bioInfo.size() + " target entries for BARD project id = " + bardProjId);
    }

    void processExperiments(Project project, int pubchemAid) throws SQLException, IOException {
        Project.ProjectExperiments projexpt = project.getProjectExperiments();
        if (projexpt == null) {
            log.warn("ProjectExperiment for CAP project id " + project.getProjectId() + " was null. No experiments to process");
            return;
        }
        List<ProjectExperiment> experiments = projexpt.getProjectExperiment();

        //We could update existing pe's by usint 'replace' over 'insert' BUT
        //suppose some pe's are removed.
        //Solution? - delete existing pe records for the incoming project
        Statement stmt = conn.createStatement();
        stmt.execute("delete from bard_project_experiment where bard_proj_id = " + bardProjId);

        PreparedStatement pstProjExpt = conn.prepareStatement("insert into bard_project_experiment (bard_proj_id, bard_expt_id, pubchem_aid, expt_type, pubchem_summary_aid) values (?,?,?,?,?)");
        for (ProjectExperiment experiment : experiments) {

            String exptType;
            ProjectExperiment.StageRef stageRef = experiment.getStageRef();
            exptType = stageRef == null ? null : stageRef.getLabel();

            Link exptLink = experiment.getExperimentRef().getLink();
            CAPConstants.CapResource res = CAPConstants.getResource(exptLink.getType());
            if (res != CAPConstants.CapResource.EXPERIMENT) continue;
            ICapResourceHandler handler = CapResourceHandlerRegistry.getInstance().getHandler(res);
            if (handler != null) {
                handler.process(exptLink.getHref(), res);
                int bardExptId = ((ExperimentHandler) handler).getBardExptId();
                int exptPubchemAid = ((ExperimentHandler) handler).getPubchemAid();
                if (bardExptId == -1) continue;
                pstProjExpt.setInt(1, bardProjId);
                pstProjExpt.setInt(2, bardExptId);
                pstProjExpt.setInt(3, exptPubchemAid);
                pstProjExpt.setString(4, exptType);
                pstProjExpt.setInt(5, pubchemAid);
                pstProjExpt.addBatch();
            }
        }
        int[] rowsInserted = pstProjExpt.executeBatch();
        conn.commit();
        pstProjExpt.close();
        log.info("Inserted " + rowsInserted.length + " project-experiment entries");
    }

    List<CAPAnnotation> processDocuments(Project project) throws SQLException, IOException, ParsingException {
        List<CAPAnnotation> annos = new ArrayList<CAPAnnotation>();

        PreparedStatement pstDoc = conn.prepareStatement("insert into cap_document (cap_doc_id, type, name, url) values (?, ?, ?, ?)");
        boolean runPst = false;
        for (Link link : project.getLink()) {
            CAPConstants.CapResource res = CAPConstants.getResource(link.getType());
            if (res != CAPConstants.CapResource.PROJECTDOC) continue;

            // for some reason unmarshalling doesn't work properly on assayDocument docs
            JAXBElement jaxbe = getResponse(link.getHref(), CAPConstants.getResource(link.getType()));
            DocumentType doc = (DocumentType) jaxbe.getValue();

            String docContent = doc.getDocumentContent();
            String docType = doc.getDocumentType(); // Description, Protocol, Comments, Paper, External URL, Other
            String docName = doc.getDocumentName();

            if ("Description".equals(docType)) {
            } else if ("Protocol".equals(docType)) {
            } else if ("Comments".equals(docType)) {
            } else {
                // hack to add cap project documents as annotations on a project
                int docId = Integer.parseInt(Util.getEntityIdFromUrl(link.getHref()));

                // check to see if document in cap_document
                // query the table by cap_doc_id
                boolean hasDoc = false;
                Statement query = conn.createStatement();
                query.execute("select cap_doc_id from cap_document where cap_doc_id=" + docId);
                ResultSet rs = query.getResultSet();
                while (rs.next()) {
                    hasDoc = true;
                }
                rs.close();
                query.close();

                if (!hasDoc) {
                    pstDoc.setInt(1, docId);
                    pstDoc.setString(2, docType);
                    pstDoc.setString(3, docName);
                    pstDoc.setString(4, docContent);
                    pstDoc.addBatch();
                    runPst = true;
                }

                // add annotation for document back to project
                annos.add(new CAPAnnotation(docId, bardProjId, docName, docType, "doc", docContent,
                        docContent, "cap-doc", link.getHref(), 0, "project", null, null));

                // see if we can insert a PubMed paper
                if (docType.equals("Paper") && docContent.startsWith("http://www.ncbi.nlm.nih.gov/pubmed")) {
                    String pmid = Util.getEntityIdFromUrl(docContent);
                    boolean status = CAPUtil.insertPublication(conn, pmid);
                    if (status) log.info("Inserted Pubmed publication " + pmid);

                    // see if we should make a link in project_pub
                    PreparedStatement pstPub = conn.prepareStatement("select * from project_pub where bard_proj_id = ?");
                    pstPub.setInt(1, bardProjId);
                    ResultSet prs = pstPub.executeQuery();
                    boolean linkExists = false;
                    while (prs.next()) linkExists = true;
                    pstPub.close();
                    if (!linkExists) {
                        pstPub = conn.prepareStatement("insert into project_pub (bard_proj_id, pmid) values (?,?)");
                        pstPub.setInt(1, bardProjId);
                        pstPub.setInt(2, Integer.parseInt(pmid));
                        pstPub.execute();
                        pstPub.close();
                    }
                }
            }
        }
        if (runPst)
            pstDoc.execute();
        conn.commit();
        pstDoc.close();
        return annos;
    }

    List<CAPAnnotation> processProjectSteps(Project project) throws SQLException {

        PreparedStatement exptLookup = conn.prepareStatement("select bard_expt_id from bard_experiment where cap_expt_id = ?");
        ResultSet rs;

        // are there any experiments to process?
        if (project.getProjectExperiments() == null) {
            exptLookup.close();
            log.warn("ProjectExperiment for CAP project id " + project.getProjectId() + " was null. No experiment steps to process");
            return new ArrayList<CAPAnnotation>();
        }

        // first go through projectExperiment elements and build a map of
        // pprojectExperiment id <-> bard experiment id
        Map<Integer, Integer> map = new HashMap<Integer, Integer>();
        List<ProjectExperiment> pes = project.getProjectExperiments().getProjectExperiment();
        for (ProjectExperiment pe : pes) {
            int peId = pe.getProjectExperimentId().intValue();
            int capExptid = Integer.parseInt(Util.getEntityIdFromUrl(pe.getExperimentRef().getLink().getHref()));
            exptLookup.setLong(1, capExptid);
            rs = exptLookup.executeQuery();
            int bardExptId = -1;
            while (rs.next()) bardExptId = rs.getInt(1);
            if (bardExptId == -1) {
                log.warn("Could not get a bard expt id for cap expt id = " + capExptid + " from projectExperiment id " + peId + " from bard project id " + bardProjId);
            } else map.put(peId, bardExptId);
            exptLookup.clearParameters();
            rs.close();
        }
        exptLookup.close();

        List<CAPAnnotation> annos = new ArrayList<CAPAnnotation>();

        PreparedStatement pstep = conn.prepareStatement("insert into project_step(bard_proj_id, step_id, prev_bard_expt_id, next_bard_expt_id, edge_name) " +
                " values (?,?,?,?,?)");

        //We're building project steps for the projects, we could update the project steps but we should
        //delete and rebuild to eliminate links that might be missing.
        Statement stmt = conn.createStatement();
        stmt.execute("delete from project_step where bard_proj_id=" + bardProjId);

        //if there are no project steps or there are 0 project steps, trunter the empty annos for project steps
        if (project.getProjectSteps() == null || project.getProjectSteps().getProjectStep().size() == 0)
            return annos;  //empty annos

        List<ProjectStep> steps = project.getProjectSteps().getProjectStep();
        for (ProjectStep step : steps) {
            int stepId = step.getProjectStepId().intValue();
            Integer nextBardExptId = map.get(step.getNextProjectExperimentRef().intValue());
            Integer prevBardExptId = map.get(step.getPrecedingProjectExperimentRef().intValue());
            ElementNSImpl o = (ElementNSImpl) step.getEdgeName();
            //have to deal with null edge names
            String ename = null;
            Node childNode = null;
            if (o != null) {
                childNode = o.getFirstChild();
                if (childNode != null) {
                    ename = childNode.getNodeValue();
                }
            }

            if (nextBardExptId == null || prevBardExptId == null) continue;

            pstep.setLong(1, bardProjId);
            pstep.setLong(2, stepId);
            pstep.setLong(3, prevBardExptId);
            pstep.setLong(4, nextBardExptId);
            //have to deal with possibly not having edge names
            if (ename != null) {
                pstep.setString(5, ename);
            } else {
                pstep.setNull(5, java.sql.Types.VARCHAR);
            }
            pstep.addBatch();

            // handle the annotation(s) for this step
            CAPDictionary dict = CAPConstants.getDictionary();
            Contexts contexts = step.getContexts();
            //may not have contexts
            if (contexts != null) {
                List<ContextType> contextTypes = contexts.getContext();
                for (ContextType contextType : contextTypes) {
                    int contextId = contextType.getId().intValue();
                    String contextName = contextType.getContextName();
                    String contextGroup = contextType.getContextGroup();

                    ContextType.ContextItems contextItems = contextType.getContextItems();
                    for (ContextItemType contextItemType : contextItems.getContextItem()) {

                        // dict id for the annotation key
                        String key = null;
                        AbstractContextItemType.AttributeId attr = contextItemType.getAttributeId();
                        if (attr != null) {
                            key = Util.getEntityIdFromUrl(attr.getLink().getHref());
                        }

                        // dict id for the annotation value
                        String valueUrl = null;
                        String value = null;
                        AbstractContextItemType.ValueId vc = contextItemType.getValueId();
                        if (vc != null) {
                            value = Util.getEntityIdFromUrl(vc.getLink().getHref());
                            valueUrl = dict.getNode(vc.getLabel()).getExternalUrl() + contextItemType.getExtValueId();
                        }
                        String valueDisplay = contextItemType.getValueDisplay();
                        String related = String.valueOf(bardProjId);

                        annos.add(new CAPAnnotation(contextId, stepId, valueDisplay,
                                contextName, key, value, contextItemType.getExtValueId(),
                                "cap-context", valueUrl, contextItemType.getDisplayOrder(),
                                "project-step", related, contextGroup));
                    }
                }
            }
        }
        int[] rowsAdded = pstep.executeBatch();
        conn.commit();
        pstep.close();
        log.info("Added " + rowsAdded.length + " project steps for BARD project id " + bardProjId + " CAP project id " + project.getProjectId());
        return annos;
    }

    class BiologyInfo {
        String dictLabel, extId, extRef, description;
        Integer dictId;

        BiologyInfo(String dictLabel, Integer dictId, String extId, String extRef, String description) {
            this.dictLabel = dictLabel;
            this.extId = extId;
            this.extRef = extRef;
            this.description = description;
            this.dictId = dictId;
        }
    }

}
