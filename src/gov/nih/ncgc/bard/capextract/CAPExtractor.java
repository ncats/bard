package gov.nih.ncgc.bard.capextract;

import gov.nih.ncgc.bard.capextract.handler.AssayHandler;
import gov.nih.ncgc.bard.capextract.handler.AssaysHandler;
import gov.nih.ncgc.bard.capextract.handler.BardexportHandler;
import gov.nih.ncgc.bard.capextract.handler.DictionaryHandler;
import gov.nih.ncgc.bard.capextract.handler.ProjectHandler;
import gov.nih.ncgc.bard.capextract.handler.ProjectsHandler;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

/**
 * Example code to play with the Broad CAP Data Export API.
 * <p/>
 * CAP data export API defined at <a href="https://github.com/broadinstitute/BARD/wiki/BARD-Data-Export-API">
 * https://github.com/broadinstitute/BARD/wiki/BARD-Data-Export-API</a>. This class is primarily a driver
 * and all the details of individual entities coming from the CAP are processed by individual handlers
 * (such as {@link ProjectHandler}, {@link AssayHandler}, etc.). Depending on the nature of the entity, a
 * handler may handle just the entity and invoke a different handler for its child entities, or else may
 * choose to handle all the child entities.
 *
 * @author Rajarshi Guha
 * @see CapResourceHandlerRegistry
 * @see DictionaryHandler
 * @see BardexportHandler
 * @see AssayHandler
 * @see AssaysHandler
 * @see ProjectHandler
 * @see ProjectsHandler
 */
public class CAPExtractor {
    private CapResourceHandlerRegistry registry;


    public void run() throws IOException, NoSuchAlgorithmException {
        registry.getHandler(CAPConstants.CapResource.BARDEXPORT).process(CAPConstants.CAP_ROOT, CAPConstants.CapResource.BARDEXPORT);
    }

    public void setHandlers() {
        registry = CapResourceHandlerRegistry.getInstance();
//        registry.setHandler(CAPConstants.CapResource.PROJECTS, new ProjectsHandler());
//        registry.setHandler(CAPConstants.CapResource.PROJECT, new ProjectHandler());
//        registry.setHandler(CAPConstants.CapResource.ASSAYS, new AssaysHandler());
//        registry.setHandler(CAPConstants.CapResource.ASSAY, new AssayHandler());
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
