package gov.nih.ncgc.bard.capextract;

import gov.nih.ncgc.bard.capextract.handler.AssayHandler;
import gov.nih.ncgc.bard.capextract.handler.AssaysHandler;
import gov.nih.ncgc.bard.capextract.handler.BardexportHandler;
import gov.nih.ncgc.bard.capextract.handler.DictionaryHandler;
import gov.nih.ncgc.bard.capextract.handler.ExperimentHandler;
import gov.nih.ncgc.bard.capextract.handler.ExperimentsHandler;
import gov.nih.ncgc.bard.capextract.handler.ProjectHandler;
import gov.nih.ncgc.bard.capextract.handler.ProjectsHandler;
import gov.nih.ncgc.bard.capextract.handler.ResultHandler;
import gov.nih.ncgc.bard.capextract.handler.ResultsHandler;
import gov.nih.ncgc.bard.capextract.jaxb.Assay;
import gov.nih.ncgc.bard.capextract.jaxb.Assays;
import gov.nih.ncgc.bard.capextract.jaxb.Experiment;
import gov.nih.ncgc.bard.capextract.jaxb.Link;
import gov.nih.ncgc.bard.capextract.jaxb.Project;
import gov.nih.ncgc.bard.capextract.jaxb.Projects;
import gov.nih.ncgc.bard.capextract.jaxb.Results;

import java.io.IOException;
import java.lang.reflect.Method;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    public <T> int getRelatedCount(Vector<T> vec) {
	int count = 0;
	if (vec == null) return count;
	for (T t: vec) {
	    try {
		Method getLinkList = t.getClass().getMethod("getLink", (Class<?>[])null);
		List<Link> links = (List<Link>)getLinkList.invoke(t, (Object[])null);
		for (Link link: links)
		    if (link.getRel().equals("related"))
			count++;
	    } catch (Exception e) {;}	    
	}
	return count;
    }
    
    public void run() throws IOException, NoSuchAlgorithmException {
//        registry.getHandler(CAPConstants.CapResource.BARDEXPORT).process(CAPConstants.CAP_ROOT, CAPConstants.CapResource.BARDEXPORT);
        registry.getHandler(CAPConstants.CapResource.EXPERIMENTS).process(CAPConstants.CAP_ROOT+"/experiments", CAPConstants.CapResource.EXPERIMENTS);
//        registry.getHandler(CAPConstants.CapResource.PROJECTS).process(CAPConstants.CAP_ROOT+"/projects", CAPConstants.CapResource.PROJECTS);
        
        ((ExperimentHandler)registry.getHandler(CAPConstants.CapResource.EXPERIMENT)).printLookup();
    }
    
    public void poll() throws IOException {
        Logger log = LoggerFactory.getLogger(this.getClass());

        Vector<Projects> projects = registry.getHandler(CAPConstants.CapResource.PROJECTS).
        	poll(CAPConstants.CAP_ROOT+"/projects", CAPConstants.CapResource.PROJECTS);
        log.info("Project count: "+projects.get(0).getCount().toString());
        for (Project proj: projects.get(0).getProject()) {
            int expCount = 0;
            int resultCount = 0;
            if (proj.getProjectSteps() != null) {
        	for (Project.ProjectSteps.ProjectStep projStep: proj.getProjectSteps().getProjectStep()) {
        	    Link link = projStep.getLink().get(0);
        	    if (link.getType().equals(CAPConstants.CapResource.EXPERIMENT.getMimeType())) {
        		expCount++;
        		Vector<Results> res = registry.getHandler(CAPConstants.CapResource.RESULTS).
        			poll(link.getHref()+"/results", CAPConstants.CapResource.RESULTS);
        		resultCount += getRelatedCount(res);
        	    }
        	}
            }
            log.info("Project "+proj.getProjectName()+": "+expCount+" expts, "+resultCount+" results");
        }
        
        Vector<Assays> assays = registry.getHandler(CAPConstants.CapResource.ASSAYS).
        	poll(CAPConstants.CAP_ROOT+"/assays", CAPConstants.CapResource.ASSAYS);
        log.info("Assay count: "+assays.get(0).getCount());
        int assayCount = 0;
        int assayExptsResults = 0;
        for (Link link: assays.get(0).getLink()) {
            if (link.getType().equals(CAPConstants.CapResource.ASSAY.getMimeType())) {
        	Assay assay = (Assay)registry.getHandler(CAPConstants.CapResource.ASSAY).
        		poll(link.getHref(), CAPConstants.CapResource.ASSAY).get(0);
        	assayCount++;
        	if (assayCount%1 == 0)
        	    log.info("Assays polled: "+assayCount+" Assays with results: "+assayExptsResults);
        	for (Link link2: assay.getLink()) {
        	    if (link2.getType().equals(CAPConstants.CapResource.EXPERIMENT.getMimeType())) {
        		Vector<Results> res = registry.getHandler(CAPConstants.CapResource.RESULTS).
        			poll(link2.getHref()+"/results", CAPConstants.CapResource.RESULTS, true);
        		if (getRelatedCount(res) > 0)
        		    assayExptsResults++;
        	    }
        	}
            }
        }
        log.info("Assays with experiments and results: "+assayExptsResults);
    }

    public void setHandlers() {
        registry = CapResourceHandlerRegistry.getInstance();
        registry.setHandler(CAPConstants.CapResource.PROJECTS, new ProjectsHandler());
        registry.setHandler(CAPConstants.CapResource.PROJECT, new ProjectHandler());
        registry.setHandler(CAPConstants.CapResource.ASSAYS, new AssaysHandler());
        registry.setHandler(CAPConstants.CapResource.ASSAY, new AssayHandler());
        registry.setHandler(CAPConstants.CapResource.EXPERIMENTS, new ExperimentsHandler());
        registry.setHandler(CAPConstants.CapResource.EXPERIMENT, new ExperimentHandler());
        registry.setHandler(CAPConstants.CapResource.RESULTS, new ResultsHandler());
        registry.setHandler(CAPConstants.CapResource.RESULT, new ResultHandler());
        registry.setHandler(CAPConstants.CapResource.BARDEXPORT, new BardexportHandler());
        registry.setHandler(CAPConstants.CapResource.DICTIONARY, new DictionaryHandler());
    }


    public static void main(String[] args) throws Exception {
        CAPExtractor c = new CAPExtractor();

        // before running the extractor, lets set our handlers
        c.setHandlers();

        // lets start pulling
        //c.run();
        
        c.poll();
    }


}
