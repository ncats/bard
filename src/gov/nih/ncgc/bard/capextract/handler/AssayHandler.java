package gov.nih.ncgc.bard.capextract.handler;

import gov.nih.ncgc.bard.capextract.CAPAssayAnnotation;
import gov.nih.ncgc.bard.capextract.CAPConstants;
import gov.nih.ncgc.bard.capextract.CAPDictionary;
import gov.nih.ncgc.bard.capextract.CAPUtil;
import gov.nih.ncgc.bard.capextract.ICapResourceHandler;
import gov.nih.ncgc.bard.capextract.jaxb.Assay;
import gov.nih.ncgc.bard.capextract.jaxb.Assay.Measures.Measure;
import gov.nih.ncgc.bard.capextract.jaxb.Assay.Measures.Measure.EntryUnit;
import gov.nih.ncgc.bard.capextract.jaxb.Assay.Measures.Measure.ResultTypeRef;
import gov.nih.ncgc.bard.capextract.jaxb.AssayContextItems;
import gov.nih.ncgc.bard.capextract.jaxb.AssayContextItems.AssayContextItem;
import gov.nih.ncgc.bard.capextract.jaxb.AssayContextItems.AssayContextItem.AttributeId;
import gov.nih.ncgc.bard.capextract.jaxb.AssayContextItems.AssayContextItem.ValueId;
import gov.nih.ncgc.bard.capextract.jaxb.AssayDocument;
import gov.nih.ncgc.bard.capextract.jaxb.Link;
import gov.nih.ncgc.bard.tools.Util;

import java.io.IOException;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        if (resource != CAPConstants.CapResource.ASSAY) return;
        log.info("Processing " + resource);

        // get the Assays object here
        Assay assay = getResponse(url, resource);
        if (!assay.getReadyForExtraction().equals("Ready")) return;

        BigInteger aid = assay.getAssayId();
        String version = assay.getAssayVersion();
        String type = assay.getAssayType();
        String status = assay.getStatus();
        String name = assay.getAssayName();
        String title = assay.getAssayTitle();
        String designedBy = assay.getDesignedBy();
        
        /* save documents related to assay */
        List<AssayDocument> docs = assay.getAssayDocuments() != null ? assay.getAssayDocuments().getAssayDocument() : new ArrayList<AssayDocument>();
        for (AssayDocument doc: docs) {
            String docType = doc.getDocumentType(); // Description, Protocol, Comments, Paper, External URL, Other
            String docName = doc.getDocumentName();
            Link docLink = doc.getLink();
            String docContent;
            if (docLink != null) {
        	AssayDocument assayDoc = getResponse(docLink.getHref(), CAPConstants.getResource(docLink.getType()));
        	docContent = assayDoc.getDocumentContent(); // is pubmed link for those with a pubmed ID
            }
        }
        log.info("status for " + name + " = " + status + ", and has " + docs.size() + " docs");

        /* save measures for an assay */
        List<Measure> measures = assay.getMeasures() != null ? assay.getMeasures().getMeasure() : new ArrayList<Measure>();
        for (Measure m: measures) {
            String contextRef = m.getAssayContextRef();
            BigInteger parent = m.getParentMeasure();
            EntryUnit unit = m.getEntryUnit();
            if (unit != null) {
        	String unitName = unit.getUnit();
        	Link unitLink = unit.getLink();
            }
            ResultTypeRef rtr = m.getResultTypeRef();
            if (rtr != null) {
        	String resultTypeName = rtr.getLabel();
        	Link resultTypeLink = rtr.getLink();
            }
        }
        
        CAPDictionary dict = CAPConstants.getDictionary();

        /* save assay contexts and annotations */
        List<Assay.AssayContexts.AssayContext> contexts = assay.getAssayContexts() != null ? assay.getAssayContexts().getAssayContext() : new ArrayList<Assay.AssayContexts.AssayContext>();
        ArrayList<BigInteger> itemIds = new ArrayList<BigInteger>();
        for (Assay.AssayContexts.AssayContext context: contexts) {
            String contextName = context.getContextName();
            List<AssayContextItem> items = context.getAssayContextItems() != null ? context.getAssayContextItems().getAssayContextItem() : new ArrayList<AssayContextItem>();
            for (AssayContextItem aci: items) {
        	BigInteger aciId = aci.getAssayContextItemId();
        	itemIds.add(aciId);
        	String aciRef = aci.getAssayContextRef();
        	if (!aciRef.equals(contextName))
        	    log.error("AssayContextRef is different than the contextName: "+aciRef+" : "+contextName);
        	AttributeId attr = aci.getAttributeId();
        	String attrType = attr.getAttributeType(); // List, Range, Number, Fixed
        	String attrLabel = attr.getLabel();
        	Link attrLink = attr.getLink();
        	String display = aci.getValueDisplay();
        	Double valueMax = aci.getValueMax();
        	Double valueMin = aci.getValueMin();
               	String qualifier = aci.getQualifier();
               	Double valueNum = aci.getValueNum();
        	String valueExtValue = aci.getExtValueId();
        	ValueId value = aci.getValueId();
        	if (value != null) {
        	    String valueLabel = value.getLabel();
        	    Link valueLink = value.getLink();
        	}
            }
        }
        log.info("Got " + contexts.size() + " annotation groups");

        /* verify no items were missed */
        if (assay.getAssayContextItems() != null) {
            for (AssayContextItem aci: assay.getAssayContextItems().getAssayContextItem()) {
        	if (!itemIds.contains(aci.getAssayContextItemId()))
        	    log.error("AssayContextItem not imported from AssayContexts above:"+aci.getAssayContextItemId());
            }
        }
        log.info("Got " + itemIds.size() + " annotations");

//        // at this point we can dump annos to the db. Importantly, we store annotations
//        // such that instead of the MeasureContextItemId identifiers, we resolve the
//        // annotations to sets of dictionary identifiers. This means when recording related
//        // annotations, we actually record related dictionary identifiers
//        try {
//            Connection conn = CAPUtil.connectToBARD();
//            PreparedStatement pst = conn.prepareStatement("insert into cap_annotation (entity, source,  assay_id, anno_id, anno_key, anno_value, anno_display, related) values('assay', 'cap', ?,?,?, ?,?,?)");
//            for (CAPAssayAnnotation anno : annos.values()) {
////                if (anno.contextRef != null) continue;
//
//                pst.setInt(1, aid.intValue());
//                pst.setString(2, anno.id);
//
//                String[] toks = anno.key.split("/");
//                pst.setString(3, toks[toks.length - 1]);
//
//                String value = null;
//                if (anno.value != null) {
//                    toks = anno.value.split("/");
//                    value = toks[toks.length - 1];
//                }
//                pst.setString(4, value);
//
//                pst.setString(5, anno.display);
//
//                String related = null;
//                for (List<String> ls : flatgrps) {
//                    if (ls.contains(anno.id)) {
//                        related = Util.join(ls, ",");
//                        break;
//                    }
//                }
//                if (anno.extValueId != null) related = related + "|" + anno.extValueId;
//                pst.setString(6, related);
//
//                pst.addBatch();
//            }
//            int[] updateCounts = pst.executeBatch();
//            conn.commit();
//            pst.close();
//            conn.close();
//            log.info("\tInserted " + updateCounts.length + " annotations (non context-ref) for aid " + aid);
//        } catch (SQLException e) {
//            e.printStackTrace();
//            log.error("Error inserting annotations for aid " + aid + "\n" + e.getMessage());
//        }
    }
}
