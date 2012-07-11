package gov.nih.ncgc.bard.capextract;

import gov.nih.ncgc.bard.capextract.handler.AssaysHandler;
import gov.nih.ncgc.bard.capextract.handler.BardexportHandler;
import gov.nih.ncgc.bard.capextract.handler.ProjectsHandler;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

/**
 * Example code to play with the Broad CAP Data Export API.
 *
 * @author Rajarshi Guha
 */
public class CAPExtractor {
    private CapResourceHandlerRegistry registry;

    public CAPExtractor() {
    }

    public void run() throws IOException, NoSuchAlgorithmException {
        registry.getHandler(CAPConstants.CapResource.BARDEXPORT).process(CAPConstants.CAP_ROOT, CAPConstants.CapResource.BARDEXPORT);
    }

    public void setHandlers() {
        registry = CapResourceHandlerRegistry.getInstance();
        registry.setHandler(CAPConstants.CapResource.PROJECTS, new ProjectsHandler());
        registry.setHandler(CAPConstants.CapResource.ASSAYS, new AssaysHandler());
        registry.setHandler(CAPConstants.CapResource.BARDEXPORT, new BardexportHandler());
    }


    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
        CAPExtractor c = new CAPExtractor();

        // before running the extractor, lets set our handlers
        c.setHandlers();

        // lets start pulling
        c.run();
    }


}
