package gov.nih.ncgc.bard.capextract.handler;

import gov.nih.ncgc.bard.capextract.CAPAssayAnnotation;
import gov.nih.ncgc.bard.capextract.CAPConstants;
import gov.nih.ncgc.bard.capextract.CAPUtil;
import gov.nih.ncgc.bard.capextract.ICapResourceHandler;
import gov.nih.ncgc.bard.capextract.jaxb.Assay;
import gov.nih.ncgc.bard.capextract.jaxb.AssayContextItems;
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

        String status = assay.getStatus();
        BigInteger aid = assay.getAssayId();
        String name = assay.getAssayName();
        String type = assay.getAssayType();
        String version = assay.getAssayVersion();
        List<AssayDocument> docs = assay.getAssayDocuments() != null ? assay.getAssayDocuments().getAssayDocument() : null;

        log.info("status for " + name + " = " + status + ", and has " + (docs != null ? docs.size() : 0) + " docs");

        /* Not sure what this is */
        if (assay.getAssayContexts() != null) {
            List<Assay.AssayContexts.AssayContext> mcs = assay.getAssayContexts().getAssayContext();
            for (Assay.AssayContexts.AssayContext mc : mcs) {
                String contextName = mc.getContextName();
//                System.out.println("contextName = " + contextName);
            }
        }

        /* This block extracts the annotations for the assay */
        List<AssayContextItems.AssayContextItem> mcis = null;
        Map<String, CAPAssayAnnotation> annos = new HashMap<String, CAPAssayAnnotation>();
        if (assay.getAssayContextItems() != null) {
            mcis = assay.getAssayContextItems().getAssayContextItem();
            for (AssayContextItems.AssayContextItem mci : mcis) {

                String valueid = null, attrid = null, attrtype = null;
                String displayValue = mci.getValueDisplay();
                BigInteger id = mci.getAssayContextItemId();
                String refid = mci.getAssayContextRef(); // may be null                
                String contextref = mci.getAssayContextRef(); //  may be null

                AssayContextItems.AssayContextItem.ValueId mcivid = mci.getValueId();
                if (mcivid != null) valueid = mcivid.getLink().getHref();
                AssayContextItems.AssayContextItem.AttributeId mciaid = mci.getAttributeId();
                if (mciaid != null) attrid = mciaid.getLink().getHref();
                String extid = mci.getExtValueId();

                CAPAssayAnnotation anno = new CAPAssayAnnotation(id.toString(), refid != null ? refid.toString() : null, displayValue, contextref, attrid, valueid, extid, "cap");
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

                String[] toks = anno.key.split("/");
                pst.setString(3, toks[toks.length - 1]);

                String value = null;
                if (anno.value != null) {
                    toks = anno.value.split("/");
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
                if (anno.extValueId != null) related = related + "|" + anno.extValueId;
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
                System.out.println("measure.getAssayContextRef() = " + measure.getAssayContextRef());
            }
        }
    }
}
