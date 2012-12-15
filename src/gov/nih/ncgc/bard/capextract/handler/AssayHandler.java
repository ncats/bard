package gov.nih.ncgc.bard.capextract.handler;

import gov.nih.ncgc.bard.capextract.CAPConstants;
import gov.nih.ncgc.bard.capextract.ICapResourceHandler;

import java.io.IOException;

/**
 * Process CAP <code>Assay</code> elements.
 * <p/>
 * Currently, the class focuses on extracting and inserting assay annotations (and furthermore, ignores
 * those annotations that have non-null measureContext's associated with them).
 * <p/>
 * Since annotations from CAP contain more information that just key/value pairs, we dump the extra stuff
 * into the <code>related</code> field in the <code>cap_annotation</code> table. Specifically, the field is
 * a '|' separated string. The first element will be a comma separated list of related annotation id's (for
 * the given assay id) and the second element, if present, is an external identifier that is relevant for
 * dictionary elements that point to external resources. An example would be an external identifier of
 * <code>9606</code> that is associated with the dictionary element for taxon, which uses the Entrez T
 * Taxonomy database to resolve these identifiers.
 * <p/>
 * In general it appears that these external identifiers are associated with the key dictionary element.
 * <p/>
 * Thus annotation key/value identifiers should be resolved
 * using the {@link gov.nih.ncgc.bard.capextract.CAPDictionary}.
 *
 * @author Rajarshi Guha
 */
public class AssayHandler extends CapResourceHandler implements ICapResourceHandler {

    public AssayHandler() {
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
//        if (resource != CAPConstants.CapResource.ASSAY) return;
//        log.info("Processing " + resource);
//
//        // get the Assays object here
//        Assay assay = getResponse(url, resource);
//        if (!assay.getReadyForExtraction().equals("Ready")) return;
//
//        BigInteger capAssayId = assay.getAssayId();
//        String version = assay.getAssayVersion();
//        String type = assay.getAssayType(); // Regular, Panel - Array, Panel - Group
//        if (!"Regular".equals(type)) {
//            log.warn("Unable to process non-regular assays at the moment, assay:"+url+" "+type);
//            return;
//        }
//        String status = assay.getStatus(); // Pending, Active, Superceded, Retired
//        if (!"Active".equals(status)) {
//            log.warn("Unable to process non-active assays at the moment, assay:"+url+" "+status);
//            return;
//        }
//        String name = assay.getAssayName();
//        String title = assay.getAssayShortName();
//        String designedBy = assay.getDesignedBy(); // becomes source
//
//
//        ArrayList<CAPAnnotation> annos = new ArrayList<CAPAnnotation>();
//
//        /* save documents related to assay */
//        List<Link> links = assay.getLink();
//        for (Link link : links) {
//            String linkType = link.getType();
//        }
//
//        String description = null, protocol = null, comments = null;
//        List<AssayDocument> docs = null; //assay.getAssayDocuments() != null ? assay.getAssayDocuments().getAssayDocument() : new ArrayList<AssayDocument>();
//        try {
//            Connection conn = CAPUtil.connectToBARD();
//            PreparedStatement pstDoc = conn.prepareStatement("insert into cap_document (cap_doc_id, type, name, url) values (?, ?, ?, ?)");
//            boolean runPst = false;
//
//            for (AssayDocument doc : docs) {
//                String docType = doc.getDocumentType(); // Description, Protocol, Comments, Paper, External URL, Other
//                String docName = doc.getDocumentName();
//                Link docLink = doc.getLink();
//                String docContent = null;
//                if (docLink != null) {
//                    AssayDocument assayDoc = getResponse(docLink.getHref(), CAPConstants.getResource(docLink.getType()));
//                    docContent = assayDoc.getDocumentContent(); // is pubmed link for those with a pubmed ID
//                }
//                if ("Description".equals(docType)) description = docContent;
//                else if ("Protocol".equals(docType)) protocol = docContent;
//                else if ("Comments".equals(docType)) comments = docContent;
//                else {
//                    // hack to add cap assay documents as annotations on an assay
//                    if (docLink.getType().equals(CAPConstants.CapResource.ASSAYDOC.getMimeType())) {
//                        int docId = Integer.valueOf(docLink.getHref().substring(docLink.getHref().lastIndexOf("assayDocument/") + 14));
//                        // check to see if document in cap_document
//                        // query the table by cap_doc_id
//                        boolean hasDoc = false;
//                        Statement query = conn.createStatement();
//                        query.execute("select cap_doc_id from cap_document where cap_doc_id=" + docId);
//                        ResultSet rs = query.getResultSet();
//                        while (rs.next()) {
//                            hasDoc = true;
//                        }
//                        rs.close();
//                        query.close();
//
//                        if (!hasDoc) {
//                            pstDoc.setInt(1, docId);
//                            pstDoc.setString(2, docType);
//                            pstDoc.setString(3, docName);
//                            pstDoc.setString(4, docContent);
//                            pstDoc.addBatch();
//                            runPst = true;
//                        }
//
//                        // add annotation for document back to assay
//                        annos.add(new CAPAnnotation(null, null, docName, null, "doc", null, docContent, "cap-doc", docLink.getHref()));
//
//                    } else {
//                        log.warn("Assay Document link type not supported: " + docLink.getType());
//                    }
//                }
//            }
//            if (runPst)
//                pstDoc.execute();
//            conn.commit();
//            pstDoc.close();
//            conn.close();
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
//        log.info("status for " + name + " = " + status + ", and has " + docs.size() + " docs");
//
//        /* save measures for an assay */
//        List<Measure> measures = assay.getMeasures() != null ? assay.getMeasures().getMeasure() : new ArrayList<Measure>();
//        int measureCount = 0;
//        for (Measure m: measures) {
//            measureCount++;
//            String contextRef = m.getAssayContextRef();
//            if (contextRef == null) contextRef = "Measure "+measureCount;
//            BigInteger parent = m.getParentMeasure();
//            if (parent != null) {
//        	System.err.println(url);
//        	System.err.println("Parent measure not null: "+parent+": "+capAssayId);
//        	log.warn("Parent measure not null: "+parent+": "+capAssayId);
//        	//Trying to get to the bottom of what this should mean ...
//        	//System.exit(1);
//            }
//
//            // hack to add measures as annotations on an assay
//            ResultTypeRef rtr = m.getResultTypeRef();
//            if (rtr != null) {
//        	String resultTypeName = rtr.getLabel();
//        	Link resultTypeLink = rtr.getLink();
//        	String valueId = resultTypeLink.getHref().substring(resultTypeLink.getHref().lastIndexOf("/")+1);
//        	annos.add(new CAPAnnotation(null, null, resultTypeName, contextRef, "5", valueId, null, "cap-measure", null));
//            }
//            EntryUnit unit = m.getEntryUnit();
//            if (unit != null) {
//        	String unitName = unit.getUnit();
//        	Link unitLink = unit.getLink();
//        	String valueId = null;
//        	if (unitLink != null)
//        	    valueId = unitLink.getHref().substring(unitLink.getHref().lastIndexOf("/")+1);
//        	annos.add(new CAPAnnotation(null, null, unitName, contextRef, "5", valueId, null, "cap-measure", null));
//            }
//        }
//
//        CAPDictionary dict = CAPConstants.getDictionary();
//
//        /* save assay contexts and annotations */
//        List<Assay.AssayContexts.AssayContext> contexts = assay.getAssayContexts() != null ? assay.getAssayContexts().getAssayContext() : new ArrayList<Assay.AssayContexts.AssayContext>();
//        ArrayList<BigInteger> itemIds = new ArrayList<BigInteger>();
//        for (Assay.AssayContexts.AssayContext context: contexts) {
//            String contextName = context.getContextName();
//            List<AssayContextItem> items = context.getAssayContextItems() != null ? context.getAssayContextItems().getAssayContextItem() : new ArrayList<AssayContextItem>();
//            for (AssayContextItem aci: items) {
//        	BigInteger aciId = aci.getAssayContextItemId();
//        	itemIds.add(aciId);
//        	String aciRef = aci.getAssayContextRef();
//        	if (!aciRef.equals(contextName))
//        	    log.error("AssayContextRef is different than the contextName: "+aciRef+" : "+contextName);
//        	AttributeId attr = aci.getAttributeId();
////        	String[] types = {"Fixed", "Free", "Range"};
////        	String attrType = attr.getAttributeType(); // List, Range, Number, Fixed, Free
////        	if (Arrays.binarySearch(types, attrType) < 0) {
////        	    System.err.println(url);
////        	    System.err.println(attrType);
////        	    System.exit(1);
////        	}
//        	String attrLabel = attr.getLabel();
//        	Link attrLink = attr.getLink();
//        	String attrId = attrLink.getHref().substring(attrLink.getHref().lastIndexOf("/")+1);
//        	String valueExtValue = null;
//        	String valueUrl = null;
//        	if (dict.getNode(attrLabel).getExternalUrl() != null) {
//        	    valueExtValue = aci.getExtValueId();
//        	    valueUrl = dict.getNode(attrLabel).getExternalUrl()+valueExtValue;
//        	}
//
//        	String display = aci.getValueDisplay();
//
////        	// these are all redundant with display ... I guess the question is whether display is appropriately interpreted without these
////        	Double valueMax = aci.getValueMax();
////        	Double valueMin = aci.getValueMin();
////        	String qualifier = aci.getQualifier();
////        	Double valueNum = aci.getValueNum();
////        	if (valueNum != null && display.indexOf(valueNum.toString()) == -1) {
////        	    System.err.println(url);
////        	    System.err.println("Unexpected Num value: "+aciId);
////        	    System.exit(1);
////        	}
////        	if (valueMax != null || valueMin != null || qualifier != null) {
////        	    System.err.println(url);
////        	    System.err.println("Unexpected value: "+aciId);
////        	    System.exit(1);
////        	}
//
//               	ValueId value = aci.getValueId();
//               	String valueId = null;
//        	if (value != null) {
//        	    //String valueLabel = value.getLabel(); // redundant with display
//        	    Link valueLink = value.getLink();
//        	    valueId = valueLink.getHref().substring(valueLink.getHref().lastIndexOf("/")+1);
//        	}
//
//        	annos.add(new CAPAnnotation(aciId.toString(), null, display, contextName, attrId, valueId, valueExtValue, "cap-anno", valueUrl));
//
//            }
//        }
//        log.info("Got " + contexts.size() + " annotation groups");
//
////        /* get experiment links */
////        ArrayList<Integer> expts = new ArrayList<Integer>();
////        for (Link link: assay.getLink() == null ? new ArrayList<Link>(0) : assay.getLink()) {
////            if (link.getType().equals(CAPConstants.CapResource.EXPERIMENT.getMimeType()))
////        	expts.add(Integer.valueOf(link.getHref().substring(link.getHref().lastIndexOf("experiments/")+12)));
////        }
//
//        /* verify no items were missed */
//        if (assay.getAssayContextItems() != null) {
//            for (AssayContextItem aci: assay.getAssayContextItems().getAssayContextItem()) {
//        	if (!itemIds.contains(aci.getAssayContextItemId()))
//        	    log.error("AssayContextItem not imported from AssayContexts above:"+aci.getAssayContextItemId());
//            }
//        }
//        log.info("Got " + itemIds.size() + " annotations");
//
//        // at this point we can dump assays and annos to the db.
//        try {
//            int bardAssayId = -1;
//
//            Connection conn = CAPUtil.connectToBARD();
//
//            // query the table by cap_assay_id
//            Statement query = conn.createStatement();
//            query.execute("select bard_assay_id, cap_assay_id from cap_assay where cap_assay_id="+capAssayId);
//            ResultSet rs = query.getResultSet();
//            while (rs.next()) {
//        	bardAssayId = rs.getInt(1);
//            }
//            rs.close();
//            query.close();
//
//            // this is a new assay
//            PreparedStatement pstAssay = conn.prepareStatement("insert into cap_assay (bard_assay_id, cap_assay_id, version, title, name, description, protocol, comment, designed_by) values(?,?,?,?,?,?,?,?,?)");
//            if (bardAssayId == -1) {
//        	pstAssay.setInt(1, bardAssayId);
//        	pstAssay.setInt(2, capAssayId.intValue());
//        	pstAssay.setString(3, version);
//        	pstAssay.setString(4, title);
//        	pstAssay.setString(5, name);
//        	pstAssay.setString(6, description);
//        	pstAssay.setString(7, protocol);
//        	pstAssay.setString(8, comments);
//        	pstAssay.setString(9, designedBy);
//
//        	pstAssay.addBatch();
//            }
//
//            PreparedStatement pstAssayAnnot = conn.prepareStatement("insert into cap_annotation (source, entity, entity_id, anno_id, anno_key, anno_value, anno_value_text, anno_display, context_name, related, url) values(?,'assay',?,?,?,?,?,?,?,?,?)");
//            for (CAPAnnotation anno : annos) {
//        	pstAssayAnnot.setString(1, anno.source);
//                pstAssayAnnot.setInt(2, capAssayId.intValue());
//                pstAssayAnnot.setString(3, anno.id);
//                pstAssayAnnot.setString(4, anno.key);
//                pstAssayAnnot.setString(5, anno.value);
//                pstAssayAnnot.setString(6, anno.extValueId); // anno_value_text
//                pstAssayAnnot.setString(7, anno.display);
//                pstAssayAnnot.setString(8, anno.contextRef); // context_name
//                pstAssayAnnot.setString(9, null); // put into related field
//                pstAssayAnnot.setString(10, anno.url);
//
//                pstAssayAnnot.addBatch();
//            }
//            pstAssay.executeBatch();
//            int[] updateCounts = pstAssayAnnot.executeBatch();
//            conn.commit();
//            pstAssay.close();
//            pstAssayAnnot.close();
//            conn.close();
//            log.info("\tInserted " + updateCounts.length + " annotations (non context-ref) for aid " + capAssayId);
//        } catch (SQLException e) {
//            e.printStackTrace();
//            log.error("Error inserting annotations for aid " + capAssayId + "\n" + e.getMessage());
//        }
    }
}
