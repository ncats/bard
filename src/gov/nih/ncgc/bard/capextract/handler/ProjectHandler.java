package gov.nih.ncgc.bard.capextract.handler;

import gov.nih.ncgc.bard.capextract.CAPAnnotation;
import gov.nih.ncgc.bard.capextract.CAPConstants;
import gov.nih.ncgc.bard.capextract.CAPDictionary;
import gov.nih.ncgc.bard.capextract.CAPUtil;
import gov.nih.ncgc.bard.capextract.CapResourceHandlerRegistry;
import gov.nih.ncgc.bard.capextract.ICapResourceHandler;
import gov.nih.ncgc.bard.capextract.jaxb.AbstractContextItemType;
import gov.nih.ncgc.bard.capextract.jaxb.ContextItemType;
import gov.nih.ncgc.bard.capextract.jaxb.ContextType;
import gov.nih.ncgc.bard.capextract.jaxb.Contexts;
import gov.nih.ncgc.bard.capextract.jaxb.DocumentType;
import gov.nih.ncgc.bard.capextract.jaxb.ExternalSystems;
import gov.nih.ncgc.bard.capextract.jaxb.Link;
import gov.nih.ncgc.bard.capextract.jaxb.Project;
import gov.nih.ncgc.bard.capextract.jaxb.ProjectExperiment;
import gov.nih.ncgc.bard.tools.Util;
import nu.xom.ParsingException;

import javax.xml.bind.JAXBElement;
import java.io.IOException;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * A one line summary.
 *
 * @author Rajarshi Guha
 */
public class ProjectHandler extends CapResourceHandler implements ICapResourceHandler {
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

        String readyToXtract = project.getReadyForExtraction();
        String title = project.getProjectName();
        BigInteger pid = project.getProjectId();

        log.info("\taurl = [" + readyToXtract + "] for " + title + " pid " + pid);
        if (readyToXtract.equals("Ready")) {
            process(project);
        }
    }

    public void process(Project project) {
        String readyToXtract = project.getReadyForExtraction();
        if (!"Ready".equals(readyToXtract)) log.error("Proceeding even though project not ready: " + readyToXtract);

        String groupType = project.getGroupType();
        if (!"Project".equals(groupType)) log.error("Group type other than Project: " + groupType);

        ExternalReferenceHandler extrefHandler = new ExternalReferenceHandler();
        ExternalSystemHandler extsysHandler = new ExternalSystemHandler();

        // project steps should have already been loaded by hand !!!
        // do not create project if it has no experiments or steps
        if (project.getProjectSteps() == null) return;
        if (project.getProjectSteps().getProjectStep().size() == 0) return;

        int capProjectId = project.getProjectId().intValue();



        try {
            // look for a Pubchem AID (ie summary aid)
            int pubchemAid = -1;
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
                        ExternalSystems.ExternalSystem extsys = extsysHandler.getExtsys();
                        String source = extsys.getName() + "," + extsys.getOwner() + "," + extsys.getSystemUrl();
                        if (PUBCHEM.equals(source)) pubchemAid = Integer.parseInt(aid);
                    }
                }
            }
            log.info("Got Pubchem AID = " + pubchemAid + " for CAP project id = " + capProjectId);


            Connection conn = CAPUtil.connectToBARD();
            PreparedStatement pst = null;
            Statement st = conn.createStatement();
            ResultSet result = st.executeQuery("select bard_proj_id, name, description from bard_project where cap_proj_id=" + capProjectId);
            int bardProjId = -1;
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
                log.info("Will do an update for the CAP project id = " + project.getProjectId());
                pst = conn.prepareStatement("update bard_project set name=?, description=?, pubchem_aid=? where cap_proj_id = ?");
                pst.setString(1, project.getProjectName());
                pst.setString(2, project.getDescription());
                pst.setInt(3, pubchemAid);
                pst.setInt(4, capProjectId);
                pst.executeUpdate();
            }

            //  at this point we have a valid bard project id, lets insert all the extra stuff

            // deal with project annotations
            List<CAPAnnotation> annos = new ArrayList<CAPAnnotation>();
            CAPDictionary dict = CAPConstants.getDictionary();
            Contexts contexts = project.getContexts();
            List<ContextType> contextTypes = contexts.getContext();
            for (ContextType contextType : contextTypes) {
                int contextId = contextType.getId().intValue();
                String contextName = contextType.getContextName();

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
                    String related = null;

                    annos.add(new CAPAnnotation(contextId, bardProjId, valueDisplay, contextName, key, value, contextItemType.getExtValueId(), "cap-context", valueUrl, contextItemType.getDisplayOrder(), "project", related));
                }
            }

            // handle project steps


            // handle project documents
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
                    annos.add(new CAPAnnotation(docId, bardProjId, docName, docType, "doc", docContent, docContent, "cap-doc", link.getHref(), 0, "project", null));

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

            // store the annotations we've collected
            if (annos.size() > 0) {
                PreparedStatement pstAnnot = conn.prepareStatement("insert into cap_project_annotation (bard_proj_id, cap_proj_id, source, entity, anno_id, anno_key, anno_value, anno_display, related, context_name, display_order) values (?,?,?,?,?,?,?,?,?,?,?)");
                for (CAPAnnotation anno : annos) {
                    pstAnnot.setInt(1, bardProjId);
                    pstAnnot.setInt(2, project.getProjectId().intValue());
                    pstAnnot.setString(3, anno.source);
                    pstAnnot.setString(4, "project");
                    pstAnnot.setInt(5, anno.id);
                    pstAnnot.setString(6, anno.key); // anno_value_text
                    pstAnnot.setString(7, anno.value);
                    pstAnnot.setString(8, anno.display); // context_name
                    pstAnnot.setString(9, anno.related); // put into related field
                    pstAnnot.setString(10, anno.contextRef);
                    pstAnnot.setInt(11, anno.displayOrder);
                    pstAnnot.addBatch();
                }
                int[] updateCounts = pstAnnot.executeBatch();
                conn.commit();
                pstAnnot.close();
                log.info("\tLoaded " + updateCounts.length + " annotations (from " + annos.size() + " CAP annotations) for cap project id " + project.getProjectId());
            }

            // handle the experiments associated with this project
            List<ProjectExperiment> experiments = project.getProjectExperiments().getProjectExperiment();
            for (ProjectExperiment experiment : experiments) {
                Link exptLink = experiment.getExperimentRef().getLink();
                CAPConstants.CapResource res = CAPConstants.getResource(exptLink.getType());
                if (res != CAPConstants.CapResource.EXPERIMENT) continue;
                ICapResourceHandler handler = CapResourceHandlerRegistry.getInstance().getHandler(res);
                if (handler != null) handler.process(exptLink.getHref(), res);
            }

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
        }
    }

}
