package gov.nih.ncgc.bard.capextract;

import gov.nih.ncgc.bard.capextract.handler.AssaysHandler;
import gov.nih.ncgc.bard.capextract.handler.BardexportHandler;
import gov.nih.ncgc.bard.capextract.handler.DictionaryHandler;
import gov.nih.ncgc.bard.capextract.handler.ProjectHandler;
import gov.nih.ncgc.bard.capextract.handler.ProjectsHandler;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

/**
 * Example code to play with the Broad CAP Data Export API.
 * 
 * CAP data export API defined at https://github.com/broadinstitute/BARD/wiki/BARD-Data-Export-API
 *
 * @author Rajarshi Guha
 */
public class CAPExtractor {
    private CapResourceHandlerRegistry registry;


    public void run() throws IOException, NoSuchAlgorithmException {
        registry.getHandler(CAPConstants.CapResource.BARDEXPORT).process(CAPConstants.CAP_ROOT, CAPConstants.CapResource.BARDEXPORT);
    }

    public void setHandlers() {
        registry = CapResourceHandlerRegistry.getInstance();
        registry.setHandler(CAPConstants.CapResource.PROJECTS, new ProjectsHandler());
        registry.setHandler(CAPConstants.CapResource.PROJECT, new ProjectHandler());
        registry.setHandler(CAPConstants.CapResource.ASSAYS, new AssaysHandler());
        registry.setHandler(CAPConstants.CapResource.BARDEXPORT, new BardexportHandler());
        registry.setHandler(CAPConstants.CapResource.DICTIONARY, new DictionaryHandler());
    }


    public static void main(String[] args) throws Exception {
        CAPExtractor c = new CAPExtractor();

        // before running the extractor, lets set our handlers
        c.setHandlers();

        // lets start pulling
        c.run();
    }


}
