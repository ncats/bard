package gov.nih.ncgc.bard.capextract.handler;

import gov.nih.ncgc.bard.capextract.*;
import gov.nih.ncgc.bard.capextract.jaxb.*;
import gov.nih.ncgc.bard.entity.Biology;
import gov.nih.ncgc.bard.tools.Util;
import nu.xom.ParsingException;

import javax.xml.bind.JAXBElement;
import java.io.IOException;
import java.math.BigInteger;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Process CAP <code>Assay</code> elements.
 * <p/>
 * Currently, the class focuses on extracting and inserting assay annotations (and furthermore, ignores
 * those annotations that have non-null measureContext's associated with them).
 * <p/>
 * Since annotations from CAP contain more information that just key/value pairs, we dump the extra stuff
 * into the <code>related</code> field in the <code>cap_annotation</code> table. Specifically, the field is
 * a '|' separated string and the contents depends on the source of the annotation.
 * <p/>
 * For annotations with a source of <code>cap-context</code>, the field will be of the form <code>measureRefs:A,B,C</code>
 * where A, B and C represent measureRefs that the context (ie assay annotation) refers to. If the annotation does
 * not refer to any measureRef's, then this field is empty.
 * <p/>
 * For annotations with a source of <code>cap-measure</code>, the field can have two components. If <code>parentMeasure:A</code>
 * is present, A represents the measureRef that is the parent of the current measure. If this component is not present, then
 * the current measure represents the (a) root of the measure graph. If <code>assayContextRefs:A,B,C</code> is present
 * A, B, C, etc. refer to the context elements (ie annotations) that refer to the current measure.
 * <p/>
 * The url field wil represent the external URL for those annotations that refer to external databases. An example would
 * an annotation referring to the Entrez Taxonomy database, in which case the url field will resolve to the specified
 * taxon in this database.
 * <p/>
 * Annotation key/value identifiers should be resolved
 * using the {@link gov.nih.ncgc.bard.capextract.CAPDictionary}.
 *
 * @author Rajarshi Guha
 */
public class AssayHandler extends CapResourceHandler implements ICapResourceHandler {
    int bardAssayId = -1;

    public AssayHandler() {
        super();
    }

    public int getBardAssayId() {
        return bardAssayId;
    }

    /**
     * Process a CAP entity that is located at some URL.
     *
     * @param url      The URL from which to retrieve the entity fron
     * @param resource The CAP resource that is meant to be processed. An implementing class
     *                 can choose to proceed or not based on this parameter.
     */
    public void process(String url, CAPConstants.CapResource resource) throws IOException {
        if (resource != CAPConstants.CapResource.ASSAY) return;
        log.info("Processing " + resource);

        // get the Assay object here
        Assay assay = getResponse(url, resource);

        //JB: Assays that aren't 'Ready' will not be exposed by bard export
        //if (!assay.getReadyForExtraction().equals("Ready")) return;

        BigInteger capAssayId = assay.getAssayId();
        String version = assay.getAssayVersion();
        String type = assay.getAssayType(); // Regular, Panel - Array, Panel - Group
        if (!"Regular".equals(type)) {
            log.warn("Unable to process non-regular assays at the moment, assay:" + url + " " + type);
            return;
        }
        String status = assay.getStatus(); // Pending, Active, Superceded, Retired. Probably should do something with the status
//        if (!"Active".equals(status)) {
//            log.warn("Unable to process non-active assays at the moment, assay:" + url + " " + status);
//            return;
//        }
        String name = assay.getAssayName();
        String title = assay.getAssayShortName();
        String designedBy = assay.getDesignedBy(); // becomes source

        ArrayList<CAPAnnotation> annos = new ArrayList<CAPAnnotation>();

        /* save documents related to assay */
        String description = null, protocol = null, comments = null;
        List<Link> docLinks = assay.getLink();
        try {
            Connection conn = CAPUtil.connectToBARD(CAPConstants.getBardDBJDBCUrl());
            PreparedStatement pstDoc = conn.prepareStatement("insert into cap_document (cap_doc_id, type, name, url) values (?, ?, ?, ?)");
            boolean runPst = false;

            for (Link link : docLinks) {
                CAPConstants.CapResource res = CAPConstants.getResource(link.getType());
                if (res != CAPConstants.CapResource.ASSAYDOC) continue;

                // for some reason unmarshalling doesn't work properly on assayDocument docs
                JAXBElement jaxbe = getResponse(link.getHref(), CAPConstants.getResource(link.getType()));
                DocumentType doc = (DocumentType) jaxbe.getValue();

                String docContent = doc.getDocumentContent();
                String docType = doc.getDocumentType(); // Description, Protocol, Comments, Paper, External URL, Other
                String docName = doc.getDocumentName();

                if ("Description".equals(docType)) description = docContent;
                else if ("Protocol".equals(docType)) protocol = docContent;
                else if ("Comments".equals(docType)) comments = docContent;
                else {
                    // hack to add cap assay documents as annotations on an assay
                    String[] toks = link.getHref().split("/");
                    int docId = Integer.parseInt(toks[toks.length - 1]);

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

                    // add annotation for document back to assay
                    annos.add(new CAPAnnotation(docId, assay.getAssayId().intValue(), docName, docType, "doc", docContent, docContent, "cap-doc", null, 0, "assay", null));

                }
            }
            if (runPst)
                pstDoc.execute();
            conn.commit();
            pstDoc.close();
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        /* save measures for an assay */
        List<Assay.Measures.Measure> measures = assay.getMeasures() != null ? assay.getMeasures().getMeasure() : new ArrayList<Assay.Measures.Measure>();
        for (Assay.Measures.Measure m : measures) {
            // which assay contexts (aka annotations) refer to this measure
            String assayContextRefs = null;
            if (m.getAssayContextRefs() != null) {
                assayContextRefs = Util.join(m.getAssayContextRefs().getAssayContextRef(), ",");
            }

            // parent mesaure can be null - if so, this measure is the "root"
            // of the measure network
            BigInteger parent = m.getParentMeasureRef();

            // Kludge to store measures as annotations. In this approach
            // the resultTypeRef is the anno value and the entryUnitRef
            // is the anno key. Both are stored in terms of the CAP dict
            // element id. The 'display name' for the 'annotation' is the
            // label on the resultTypeRef
            Assay.Measures.Measure.ResultTypeRef resultTypeRef = m.getResultTypeRef();
            Assay.Measures.Measure.EntryUnitRef entryUnitRef = m.getEntryUnitRef();
            String displayName = null, valueId = null, keyId = null;
            if (resultTypeRef != null) {
                displayName = resultTypeRef.getLabel();
                String[] toks = resultTypeRef.getLink().getHref().split("/");
                valueId = toks[toks.length - 1];
            }
            if (entryUnitRef != null) {
                String[] toks = entryUnitRef.getLink().getHref().split("/");
                keyId = toks[toks.length - 1];
            }

            String related = "";
            if (assayContextRefs != null) related = "assayContextRefs:" + assayContextRefs;
            if (parent != null) related += "|parentMeasure:" + parent;

            annos.add(new CAPAnnotation(m.getMeasureId().intValue(), assay.getAssayId().intValue(),
                    displayName, null, keyId, valueId, null, "cap-measure", null, 0, "assay", related));
        }

        CAPDictionary dict = CAPConstants.getDictionary();
        List<String> gis = new ArrayList<String>();
        List<String> geneIds = new ArrayList<String>();
        /* save assay contexts (aka annotations) */
        List<AssayContexType> contexts = assay.getAssayContexts() != null ? assay.getAssayContexts().getAssayContext() : new ArrayList<AssayContexType>();
        for (AssayContexType context : contexts) {
            String contextName = context.getContextName();
            int contextId = context.getAssayContextId().intValue();

            // a context (ie annotation group) can refer to one or more measures (via measureRef tags)
            // we collect them and store them in the related column for each of the annotations
            // associated with this context. (Ideally we should run a sanity check to ensure that
            // measures referenced here actually exist for this assay)
            String related = null;
            if (context.getMeasureRefs() != null) {
                List<BigInteger> measureRefs = context.getMeasureRefs().getMeasureRef();
                if (measureRefs != null && measureRefs.size() > 0)
                    related = "measureRefs:" + Util.join(measureRefs, ",");
            }

            if (context.getAssayContextItems() == null) continue;

            // some special handling for targets
            if (contextName.equals("target")) {
                for (AssayContextItemType contextItem : context.getAssayContextItems().getAssayContextItem()) {
                    AbstractContextItemType.AttributeId attr = contextItem.getAttributeId();
                    if (attr != null && attr.getLabel().equals("gene")) geneIds.add(contextItem.getExtValueId());
                    else if (attr != null && attr.getLabel().equals("protein")) gis.add(contextItem.getExtValueId());
                }
            }

            for (AssayContextItemType contextItem : context.getAssayContextItems().getAssayContextItem()) {
                int displayOrder = contextItem.getDisplayOrder();
                String valueDisplay = contextItem.getValueDisplay();
                String extValueId = contextItem.getExtValueId();

                // dict id for the annotation key
                String key = null;
                AbstractContextItemType.AttributeId attr = contextItem.getAttributeId();
                if (attr != null) {
                    key = Util.getEntityIdFromUrl(attr.getLink().getHref());
                }

                // dict id for the annotation value
                String value = null;
                String valueUrl = null;
                AbstractContextItemType.ValueId vc = contextItem.getValueId();
                if (vc != null) {
                    String[] toks = vc.getLink().getHref().split("/");
                    value = toks[toks.length - 1];
                    if (extValueId != null) valueUrl = dict.getNode(vc.getLabel()).getExternalUrl() + extValueId;
                } else {
                    // if there is no valueId field and there is an extValueId field, we
                    // construct the valueUrl from the key + extValueId
                    if (extValueId != null) {
                        CAPDictionaryElement dictNode = dict.getNode(new BigInteger(key));
                        valueUrl = dictNode.getExternalUrl() + extValueId;
                    }
                }

                System.out.println(valueDisplay+" "+key+" "+valueUrl);
                annos.add(new CAPAnnotation(contextId, assay.getAssayId().intValue(), valueDisplay, contextName, key, value, extValueId, "cap-context", valueUrl, displayOrder, "assay", related));

            }
        }

        /*
           We collect experiment links and load them in after we have loaded the current assay
           (prevents us from entering an infinite loop, as experiment loading will check to
           see if the parent assay has been loaded
         */
        List<String> exptUrls = new ArrayList<String>();
        for (Link link : assay.getLink() == null ? new ArrayList<Link>() : assay.getLink()) {
            if (link.getType().equals(CAPConstants.CapResource.EXPERIMENT.getMimeType()))
                exptUrls.add(link.getHref());
        }

        // at this point we can dump assays and annos to the db.
        try {
            boolean assayExists = false;

            int qcapAssayId = -1;

            Connection conn = CAPUtil.connectToBARD(CAPConstants.getBardDBJDBCUrl());
            Statement query = conn.createStatement();

            // first see if we need to stage it
            query.execute("select cap_assay_id from cap_assay where cap_assay_id=" + capAssayId);
            ResultSet rs = query.getResultSet();
            while (rs.next()) qcapAssayId = rs.getInt(1);
            query.clearBatch();
            if (qcapAssayId == -1) {
                PreparedStatement pstAssay = conn.prepareStatement(
                        "insert into cap_assay (cap_assay_id, version, title, name, description, protocol, comment, designed_by) values(?,?,?,?,?,?,?,?)");
                pstAssay.setInt(1, capAssayId.intValue());
                pstAssay.setString(2, version);
                pstAssay.setString(3, title);
                pstAssay.setString(4, name);
                pstAssay.setString(5, description);
                pstAssay.setString(6, protocol);
                pstAssay.setString(7, comments);
                pstAssay.setString(8, designedBy);
                int insertedRows = pstAssay.executeUpdate();
                pstAssay.close();
                rs.close();
                conn.commit();
                if (insertedRows == 0) {
                    log.error("Could not insert new CAP assay id = " + capAssayId);
                    return;
                } else log.info("Staged CAP assay id = " + capAssayId);

            }

            // query bard_assay by cap_assay_id and see if we need to insert into production
            query.execute("select bard_assay_id from bard_assay where cap_assay_id=" + capAssayId);
            rs = query.getResultSet();
            int localBardAssayId = -1;
            while (rs.next()) localBardAssayId = rs.getInt(1);
            rs.close();
            query.close();
            bardAssayId = localBardAssayId;

            PreparedStatement pstAssay;
            if (bardAssayId == -1) {
                pstAssay = conn.prepareStatement(
                        "insert into bard_assay (cap_assay_id, title, name, description, protocol, comment, designed_by, assay_type, status) values(?,?,?,?,?,?,?,?,?)",
                        Statement.RETURN_GENERATED_KEYS);

                pstAssay.setInt(1, capAssayId.intValue());
                pstAssay.setString(2, title);
                pstAssay.setString(3, name);
                pstAssay.setString(4, description);
                pstAssay.setString(5, protocol);
                pstAssay.setString(6, comments);
                pstAssay.setString(7, designedBy);
                pstAssay.setString(8, type);
                pstAssay.setString(9, status);
                int insertedRows = pstAssay.executeUpdate();
                if (insertedRows == 0) {
                    log.error("Could not insert new CAP assay id = " + capAssayId + " into production");
                }
                rs = pstAssay.getGeneratedKeys();
                while (rs.next()) bardAssayId = rs.getInt(1);
                rs.close();
                pstAssay.close();
                log.info("Loaded CAP assay id = " + capAssayId + " into production as BARD assay id = " + bardAssayId);
            } else {
                log.info("CAP assay id = " + capAssayId + " already exists in production as BARD assay id = " + bardAssayId + ". Updating");
                assayExists = true;

                pstAssay = conn.prepareStatement(
                        "update bard_assay set title=?, name=?, description=?, protocol=?, comment=?, designed_by=?, assay_type=?, status=? where bard_assay_id = ?");
                pstAssay.setString(1, title);
                pstAssay.setString(2, name);
                pstAssay.setString(3, description);
                pstAssay.setString(4, protocol);
                pstAssay.setString(5, comments);
                pstAssay.setString(6, designedBy);
                pstAssay.setString(7, type);
                pstAssay.setString(8, status);
                pstAssay.setLong(9, bardAssayId);
                pstAssay.executeUpdate();
                pstAssay.close();
            }

            // get Biology information and load it into the bard_biology table
            Assay.AssayContexts assayContexts = assay.getAssayContexts();
            if (assayContexts != null) {
                // first delete all biology data for this assay
                PreparedStatement deletePst = conn.prepareStatement("delete from bard_biology where entity = 'assay' and entity_id = ?");
                deletePst.setLong(1, bardAssayId);
                deletePst.executeUpdate();
                conn.commit();
                deletePst.close();
                // now load in the new stuff
                List<BiologyInfo> bi = extractBiology(assayContexts.getAssayContext());
                PreparedStatement pstTarget =
                        conn.prepareStatement("insert into bard_biology (biology, biology_dict_id, biology_dict_label, description, entity, entity_id, ext_id, ext_ref) " +
                                " values (?,?,?,?,?,?,?,?)");
                for (BiologyInfo abi : bi) {
                    String biology = Biology.BiologyType.getBiologyTypeFromDictId(abi.dictId).toString();
                    pstTarget.setString(1, biology);
                    pstTarget.setInt(2, abi.dictId);
                    pstTarget.setString(3, abi.dictLabel);
                    pstTarget.setString(4, abi.description);
                    pstTarget.setString(5, "assay");
                    pstTarget.setInt(6, bardAssayId);
                    pstTarget.setString(7, abi.extId);
                    pstTarget.setString(8, abi.extRef);
                    try {
                        pstTarget.executeUpdate();
                    } catch (com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException e) {
                    }
                    pstTarget.clearParameters();
                }
                pstTarget.close();
                log.info("Inserted " + bi.size() + " biology entries for BARD assay id = " + bardAssayId);
            }

            // Delete pre-existing annotations
            PreparedStatement deletePst = conn.prepareStatement("delete from cap_annotation where entity_id = ?");
            deletePst.setLong(1, bardAssayId);
            deletePst.executeUpdate();
            conn.commit();
            deletePst.close();
            // Load in the new ones
            PreparedStatement pstAssayAnnot = conn.prepareStatement("insert into cap_annotation (source, entity, entity_id, anno_id, anno_key, anno_value, anno_value_text, anno_display, context_name, related, url, display_order) values(?,'assay',?,?,?,?,?,?,?,?,?,?)");
            for (CAPAnnotation anno : annos) {
                pstAssayAnnot.setString(1, anno.source);
                pstAssayAnnot.setInt(2, bardAssayId);
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

            // TODO this block implies we don't update pubs etc for pre-existing assays
            if (!assayExists) {
                // insert documents if need be
                PreparedStatement pstPub = conn.prepareStatement("select * from assay_pub where bard_assay_id = ?");
                PreparedStatement pstPubLink = conn.prepareStatement("insert into assay_pub (bard_assay_id, pmid) values (?,?)");
                for (CAPAnnotation anno : annos) {
                    if (anno.source.equals("cap-doc")) {
                        String docType = anno.contextRef;
                        String docContent = anno.value;
                        if (docType.equals("Paper") && docContent.startsWith("http://www.ncbi.nlm.nih.gov/pubmed")) {
                            String pmid = Util.getEntityIdFromUrl(docContent);
                            boolean insstatus = CAPUtil.insertPublication(conn, pmid);
                            if (insstatus) log.info("Inserted Pubmed publication " + pmid);

                            // see if we should make a link in assay_pub
                            pstPub.setInt(1, bardAssayId);
                            ResultSet prs = pstPub.executeQuery();
                            boolean linkExists = false;
                            while (prs.next()) linkExists = true;
                            pstPub.clearParameters();

                            if (!linkExists) {
                                pstPubLink.setInt(1, bardAssayId);
                                pstPubLink.setInt(2, Integer.parseInt(pmid));
                                pstPubLink.execute();
                                pstPubLink.addBatch();
                            }
                        }
                    }
                }
                pstPubLink.executeBatch();
                conn.commit();
                pstPubLink.close();
                pstPub.close();
                log.info("Inserted " + updateCounts.length + " annotations for CAP aid " + capAssayId);
                rs.close();
            }

            // now insert the experiment
            ExperimentHandler exptHandler = new ExperimentHandler();
            for (String exptUrl : exptUrls) {
                log.info("Loading linked experiment (CAP id = " + Util.getEntityIdFromUrl(exptUrl) + ")");
                exptHandler.process(exptUrl, CAPConstants.CapResource.EXPERIMENT);
            }

            log.info("Completed processing of ASSAY (CAP id = " + assay.getAssayId() + ", bard id = " + bardAssayId + ")");
        } catch (SQLException e) {
            e.printStackTrace();
            log.error("Error inserting annotations for CAP aid " + capAssayId + "\n" + e.getMessage());
        } catch (ParsingException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }


    List<BiologyInfo> extractBiology(List<AssayContexType> contexts) throws ClassNotFoundException, IOException, SQLException {
        CAPDictionary dict = CAPUtil.getCAPDictionary();

        if (contexts == null || contexts.size() == 0) return null;

        List<BiologyInfo> bioInfo = new ArrayList<BiologyInfo>();
        for (AssayContexType context : contexts) {
            if (context.getAssayContextItems() == null) continue;

            // lets see if this is a biology context
            boolean isBiologyContext = false;
            for (AssayContextItemType contextItem : context.getAssayContextItems().getAssayContextItem()) {
                AbstractContextItemType.AttributeId attrid = contextItem.getAttributeId();
                String dictId = Util.getEntityIdFromUrl(attrid.getLink().getHref());
                if (dictId != null && dictId.equals("541")) {
                    isBiologyContext = true;
                    break;
                }
            }
            if (!isBiologyContext) continue;

            /* ok, this is a biology context. Lets examine all the assayContextItems and see if they
               contain any ext refs. Depending on the valueId of the biology context this can indicate
               a nucleotide, peptide or biological process. It
                 (if it is a biological process then:)
                    GO biological process term (1419)
                    NCBI BioSystems term (885)
                 (if it is not a biological process then:)
                    NCBI accession number (1795)
                    gene Entrez GI (880)
                    GenBank ID (881)
                    protein Entrez GI (882)
                    REGID (883)
                    UniProt accession number (1398)
                    GO ID  (1504) (this is going to be renamed "GO gene product ID")
            */
            List<Integer> targetDictIds = Arrays.asList(new Integer[]{
                    1419, 885, 1795, 880, 881, 882, 883, 1398, 1504
            });
            for (AssayContextItemType contextItem : context.getAssayContextItems().getAssayContextItem()) {
                AbstractContextItemType.AttributeId attrid = contextItem.getAttributeId();
                String dictId = Util.getEntityIdFromUrl(attrid.getLink().getHref());
                if (Util.isNumber(dictId) && targetDictIds.contains(Integer.parseInt(dictId))) {
                    CAPDictionaryElement node = dict.getNode(new BigInteger(dictId));
                    String dictLabel = node.getLabel();
                    String extId = contextItem.getExtValueId();
                    String description = contextItem.getValueDisplay();
                    BiologyInfo bi = new BiologyInfo(dictLabel, Integer.parseInt(dictId), extId, null, description);
                    bioInfo.add(bi);
                }
            }

        }
        return bioInfo;
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
