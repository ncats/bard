package gov.nih.ncgc.bard.capextract.handler;

import gov.nih.ncgc.bard.capextract.CAPConstants;
import gov.nih.ncgc.bard.capextract.CAPUtil;
import gov.nih.ncgc.bard.capextract.ICapResourceHandler;
import gov.nih.ncgc.bard.capextract.jaxb.Assay;
import gov.nih.ncgc.bard.capextract.jaxb.AssayDocument;
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
 * A one line summary.
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

        String status = assay.getStatus();
        BigInteger aid = assay.getAssayId();
        String name = assay.getAssayName();
        String type = assay.getAssayType();
        String version = assay.getAssayVersion();
        List<AssayDocument> docs = assay.getAssayDocuments() != null ? assay.getAssayDocuments().getAssayDocument() : null;

        log.info("status for " + name + " = " + status + ", and has " + (docs != null ? docs.size() : 0) + " docs");

        /* Not sure what this is */
        if (assay.getMeasureContexts() != null) {
            List<Assay.MeasureContexts.MeasureContext> mcs = assay.getMeasureContexts().getMeasureContext();
            for (Assay.MeasureContexts.MeasureContext mc : mcs) {
                String contextName = mc.getContextName();
//                System.out.println("contextName = " + contextName);
            }
        }

        /* This block extracts the annotations for the assay */
        List<Assay.MeasureContextItems.MeasureContextItem> mcis = null;
        Map<String, CAPAssayAnnotation> annos = new HashMap<String, CAPAssayAnnotation>();
        if (assay.getMeasureContextItems() != null) {
            mcis = assay.getMeasureContextItems().getMeasureContextItem();
            for (Assay.MeasureContextItems.MeasureContextItem mci : mcis) {

                String valueid = null, attrid = null, attrtype = null;
                String displayValue = mci.getValueDisplay();
                BigInteger id = mci.getMeasureContextItemId();
                BigInteger refid = mci.getMeasureContextItemRef(); // may be null                
                String contextref = mci.getMeasureContextRef(); //  may be null

                Assay.MeasureContextItems.MeasureContextItem.ValueId mcivid = mci.getValueId();
                if (mcivid != null) valueid = mcivid.getLink().getHref();
                Assay.MeasureContextItems.MeasureContextItem.AttributeId mciaid = mci.getAttributeId();
                if (mciaid != null) attrid = mciaid.getLink().getHref();
                String extid = mci.getExtValueId();

                CAPAssayAnnotation anno = new CAPAssayAnnotation(id.toString(), refid != null ? refid.toString() : null, displayValue, contextref, attrid, valueid, extid);
                annos.put(id.toString(), anno);
            }
        }
        log.info("\tGot " + annos.size() + " annotations");

        // ok, we have a list of annotations, now we reconstruct the groups
        Map<String, List<String>> annogrps = new HashMap<String, List<String>>();
        for (CAPAssayAnnotation anno : annos.values()) {
            String id = anno.id;
            String refid = anno.refId;

            if (refid != null && refid.equals(id)) refid = null;

            if (refid != null) {
                if (annogrps.containsKey(refid)) {
                    List<String> tmp = annogrps.get(refid);
                    tmp.add(id);
                    annogrps.put(refid, tmp);
                } else { //  if we have a null refid, just add the id itself to the list of related ids
                    List<String> tmp = new ArrayList<String>();
                    tmp.add(id);
                    annogrps.put(refid, tmp);
                }
            } else {
                if (!annogrps.containsKey(id)) annogrps.put(id, new ArrayList<String>());
            }
        }
        log.info("\tReconstructed annotation groups and got " + annogrps.size() + " groups");
//        for (String key : annogrps.keySet()) {
//            System.out.print(key + ": ");
//            for (String s : annogrps.get(key)) System.out.print(s + " ");
//            System.out.println("\n");
//        }

        // at this stage we have a list of annotations and we have groups of annotations
        // as we write each annotation to the db, we want to list the other annotations in
        // its group (if any). This is obviously repetitive but lets us access related 
        // annots given any annot
        List<List<String>> flatgrps = new ArrayList<List<String>>();
        for (String key : annogrps.keySet()) {
            List<String> tmp = new ArrayList<String>();
            tmp.add(key);
            tmp.addAll(annogrps.get(key));
            flatgrps.add(tmp);
        }
//        for (List<String> ls : flatgrps) {
//            for (String s : ls) System.out.print(s + " ");
//            System.out.println();
//        }
        log.info("\tFlattened annotations into " + flatgrps.size() + " groups");

        // at this point we can dump annos to the db. Importantly, we store annotations
        // such that instead of the MeasureContextItemId identifiers, we resolve the
        // annotations to sets of dictionary identifiers. This means when recording related
        // annotations, we actually record related dictionary identifiers
        try {
            Connection conn = CAPUtil.connectToBARD();
            PreparedStatement pst = conn.prepareStatement("insert into cap_annotation (entity, source,  assay_id, anno_id, anno_key, anno_value, anno_display, related) values('assay', 'cap', ?,?,?, ?,?,?)");
            for (CAPAssayAnnotation anno : annos.values()) {
                if (anno.contextRef != null) continue;

                pst.setInt(1, aid.intValue());
                pst.setString(2, anno.id);

                String[] toks = anno.attrId.split("/");
                pst.setString(3, toks[toks.length - 1]);

                String value = null;
                if (anno.valueId != null) {
                    toks = anno.valueId.split("/");
                    value = toks[toks.length - 1];
                }
                pst.setString(4, value);

                pst.setString(5, anno.display);

                String related = null;
                for (List<String> ls : flatgrps) {
                    if (ls.contains(anno.id)) {
                        related = Util.join(ls, ",");
                        break;
                    }
                }
                pst.setString(6, related);

                pst.addBatch();
            }
            int[] updateCounts = pst.executeBatch();
            conn.commit();
            pst.close();
            conn.close();
            log.info("\tInserted " + updateCounts.length + " annotations (non context-ref) for aid " + aid);
        } catch (SQLException e) {
            e.printStackTrace();
            log.error("Error inserting annotations for aid " + aid + "\n" + e.getMessage());
        }


        /* looking at measures */
        Assay.Measures measures = assay.getMeasures();
        if (measures != null) {
            for (Assay.Measures.Measure measure : measures.getMeasure()) {
                System.out.println("measure.getMeasureContextRef() = " + measure.getMeasureContextRef());
            }
        }
    }


    static class CAPAssayAnnotation {
        String id, refId = null, display;
        String contextRef = null;
        String attrId, valueId; // refers to a dict element
        String extValueId = null; // when dict element points to ext resource (e.g. Entrez Gene) this is the identifier within that resource

        CAPAssayAnnotation(String id, String refId, String display, String contextRef, String attrId, String valueId, String extValueId) {
            this.id = id;
            this.refId = refId;
            this.display = display;
            this.contextRef = contextRef;
            this.attrId = attrId;
            this.valueId = valueId;
            this.extValueId = extValueId;
        }

        CAPAssayAnnotation() {
        }

        @Override
        public String toString() {
            return "CAPAssayAnnotation{" +
                    "id='" + id + '\'' +
                    ", refId='" + refId + '\'' +
                    ", display='" + display + '\'' +
                    ", contextRef='" + contextRef + '\'' +
                    ", attrId='" + attrId + '\'' +
                    ", valueId='" + valueId + '\'' +
                    '}';
        }
    }
}
