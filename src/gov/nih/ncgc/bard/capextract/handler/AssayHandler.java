package gov.nih.ncgc.bard.capextract.handler;

import com.sun.jersey.api.client.ClientResponse;
import gov.nih.ncgc.bard.capextract.CAPConstants;
import gov.nih.ncgc.bard.capextract.ICapResourceHandler;
import gov.nih.ncgc.bard.capextract.jaxb.Assay;
import gov.nih.ncgc.bard.capextract.jaxb.AssayDocument;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;

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

        ClientResponse response = getResponse(url, resource);
        if (response.getStatus() != 200)
            throw new IOException("Got HTTP " + response.getStatus() + " from CAP assays resource");

        // get the Assays object here
        Assay assay = response.getEntity(Assay.class);
        if (!assay.getReadyForExtraction().equals("Ready")) return;

        String status = assay.getStatus();
        BigInteger aid = assay.getAssayId();
        String name = assay.getAssayName();
        String type = assay.getAssayType();
        String version = assay.getAssayVersion();
        List<AssayDocument> docs = assay.getAssayDocuments().getAssayDocument();

        log.info("status for " + name + " = " + status + ", and has " + docs.size() + " docs");

        /* Not sure what this is */
        if (assay.getMeasureContexts() != null) {
            List<Assay.MeasureContexts.MeasureContext> mcs = assay.getMeasureContexts().getMeasureContext();
            for (Assay.MeasureContexts.MeasureContext mc : mcs) {
                String contextName = mc.getContextName();
                System.out.println("contextName = " + contextName);
            }
        }

        /* This block extracts the annotations for the assay */
        List<Assay.MeasureContextItems.MeasureContextItem> mcis = assay.getMeasureContextItems().getMeasureContextItem();
        for (Assay.MeasureContextItems.MeasureContextItem mci : mcis) {
            String extId = mci.getExtValueId();
            Assay.MeasureContextItems.MeasureContextItem.AttributeId attrid = mci.getAttributeId();
            System.out.println("key = [" + attrid.getLabel() + "," + attrid.getAttributeType() + "," + attrid.getLink() + "] [extid = " + extId + "] value = " + mci.getValueDisplay());
        }

        /* looking at measures */
        Assay.Measures measures = assay.getMeasures();
        if (measures != null) {
            for (Assay.Measures.Measure measure : measures.getMeasure()) {
                System.out.println("measure.getMeasureContextRef() = " + measure.getMeasureContextRef());
            }
        }
    }
}
