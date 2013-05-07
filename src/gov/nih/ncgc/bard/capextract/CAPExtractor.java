package gov.nih.ncgc.bard.capextract;

//import gov.nih.ncgc.bard.capextract.handler.AssayHandler;
//import gov.nih.ncgc.bard.capextract.handler.AssaysHandler;

import gov.nih.ncgc.bard.capextract.handler.*;
import gov.nih.ncgc.bard.capextract.jaxb.Link;
import gov.nih.ncgc.bard.capextract.jaxb.Projects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

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
    private Logger log;
    
    public CAPExtractor() {
        log = LoggerFactory.getLogger(this.getClass());
    }
    
    public <T> int getRelatedCount(Vector<T> vec) {
        int count = 0;
        if (vec == null) return count;
        for (T t : vec) {
            try {
                Method getLinkList = t.getClass().getMethod("getLink", (Class<?>[]) null);
                @SuppressWarnings("unchecked")
                List<Link> links = (List<Link>) getLinkList.invoke(t, (Object[]) null);
                for (Link link : links)
                    if (link.getRel().equals("related"))
                        count++;
            } catch (Exception e) {
                ;
            }
        }
        return count;
    }

    public void run() throws IOException, NoSuchAlgorithmException {
        registry.getHandler(CAPConstants.CapResource.BARDEXPORT).process(CAPConstants.CAP_ROOT, CAPConstants.CapResource.BARDEXPORT);
    }

    public void poll() throws IOException {
        Logger log = LoggerFactory.getLogger(this.getClass());

        Vector<Projects> projects = registry.getHandler(CAPConstants.CapResource.PROJECTS).
                poll(CAPConstants.CAP_ROOT + "/projects", CAPConstants.CapResource.PROJECTS);
        log.info("Project count: " + projects.get(0).getCount().toString());

        // each project is obtained via a link
        for (Projects aProjects : projects) {
            List<Link> projectLinks = aProjects.getLink();
            for (Link projectLink : projectLinks) {
                CAPConstants.CapResource res = CAPConstants.getResource(projectLink.getType());
                if (res == CAPConstants.CapResource.PROJECT) {
                    ICapResourceHandler handler = CapResourceHandlerRegistry.getInstance().getHandler(res);
                    handler.process(projectLink.getHref(), res);
                }
            }
        }
    }

    // To do a test for a specific assay id, first mark it as READY
    // AssayHandler h = new AssayHandler();
    // h.setExtractionStatus("Ready", "https://bard.broadinstitute.org/dataExport/api/assays/1587",CAPConstants.CapResource.ASSAY);

    public void setHandlers() {
        registry = CapResourceHandlerRegistry.getInstance();
        registry.setHandler(CAPConstants.CapResource.PROJECTS, new ProjectsHandler());
        registry.setHandler(CAPConstants.CapResource.PROJECT, new ProjectHandler());
        registry.setHandler(CAPConstants.CapResource.PROJECTDOC, new ProjectDocHandler());
        registry.setHandler(CAPConstants.CapResource.ASSAYS, new AssaysHandler());
        registry.setHandler(CAPConstants.CapResource.ASSAY, new AssayHandler());
        registry.setHandler(CAPConstants.CapResource.EXPERIMENTS, new ExperimentsHandler());
        registry.setHandler(CAPConstants.CapResource.EXPERIMENT, new ExperimentHandler());
        registry.setHandler(CAPConstants.CapResource.RESULT, new ResultHandler());
        registry.setHandler(CAPConstants.CapResource.BARDEXPORT, new BardexportHandler());
        registry.setHandler(CAPConstants.CapResource.DICTIONARY, new DictionaryHandler());
        registry.setHandler(CAPConstants.CapResource.EXTREF, new ExternalReferenceHandler());
        registry.setHandler(CAPConstants.CapResource.EXTSYS, new ExternalSystemHandler());
        registry.setHandler(CAPConstants.CapResource.RESULT_JSON, new ExperimentResultHandler());

        //        registry.setHandler(CAPConstants.CapResource.RESULTS, new ResultsHandler());
    }

    public boolean evaluateAndSetLoadLockState(String lockFilePath, boolean lock) throws IOException {
	boolean load = false;
	log.info("Checking and setting load lock file: "+lockFilePath);
	Properties loadProps = new Properties();
	FileReader fr = new FileReader(lockFilePath);
	loadProps.load(fr);
	fr.close();
	if(loadProps.getProperty("load.state").equals("IDLE")) {
	    //if idle and want to set the lock, set state to LOADING
	    if(lock) {
		loadProps.setProperty("load.state", "LOADING");
		FileWriter writer = new FileWriter(lockFilePath);
		loadProps.store(writer, "Load State");
		writer.close();
		load = true;
		log.info("STARTING LOAD. Setting load state to LOADING.");
	    }
	} else {
	    //load state is LOADING

	    //if trying to lock, return false to indicate existing load holds the lock
	    if(lock) {
		load = false;
		log.info("ABORTING LOAD, another load process is in progress.");
	    } else {
		//if not locking, then the intention is to unlock, set state back to IDLE
		loadProps.setProperty("load.state", "IDLE");
		FileWriter writer = new FileWriter(lockFilePath);
		loadProps.store(writer, "Load State");
		writer.close();
		load = true;
		log.info("FINISHED LOAD, setting load state to IDLE.");
	    }
	}
	
	return load;
    }

    public static void main(String[] args) throws Exception {

	CAPExtractor c = new CAPExtractor();

	// make sure we have a load state lock file path, or exit
	if(args.length == 0) {
	    System.err.println("LOAD Terminatd: Process requires a load state lock file path to determine if a load is in progress.");
	    System.err.println("USAGE: java -cp <lib-path> -Xmx<mem-alloc> -DCAP_API_KEY=<api-key> -DBARD_SCRATCH_DIR=<scratch-dir-path> -DBARD_DB_URL=<db-url> gov.nih.ncgc.bard.capextract.CAPExtractor <load-state-file-path>");
	    System.err.println("The load state file is a text properties file with a single property load.state:<IDLE|LOADING>");
	    System.exit(1);
	}

	// check the load state and begin load
	try {
	    // returns true if state was IDLE and the lock is set and state set to LOADING, Ready to load
	    if(c.evaluateAndSetLoadLockState(args[0], true)) {
		// before running the extractor, lets set our handlers
		c.setHandlers();
		// lets start pulling
		c.run();	    
		// set the load state back to IDLE at the end of the load
		c.evaluateAndSetLoadLockState(args[0], false);
	    } 
	} catch (Exception e) {
	    // on any terminal error set load state to IDLE
	    c.evaluateAndSetLoadLockState(args[0], false);
	    e.printStackTrace();
	}

    }


}
