package gov.nih.ncgc.bard.capextract.handler;

import gov.nih.ncgc.bard.capextract.CAPConstants;
import gov.nih.ncgc.bard.capextract.CAPUtil;
import gov.nih.ncgc.bard.capextract.ICapResourceHandler;
import gov.nih.ncgc.bard.capextract.jaxb.Project;
import gov.nih.ncgc.bard.capextract.jaxb.Project.ProjectContextItems.ProjectContextItem;
import gov.nih.ncgc.bard.capextract.jaxb.Project.ProjectContextItems.ProjectContextItem.Attribute;
import gov.nih.ncgc.bard.capextract.jaxb.Project.ProjectContextItems.ProjectContextItem.ValueControlled;

import java.io.IOException;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

/**
 * A one line summary.
 *
 * @author Rajarshi Guha
 */
public class ProjectHandler extends CapResourceHandler implements ICapResourceHandler {

    public ProjectHandler() {
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
        if (resource != CAPConstants.CapResource.PROJECT) return;

        // get the Projects object here
        Project project = getResponse(url, resource);

        String readyToXtract = project.getReadyForExtraction();
        String title = project.getProjectName();
        BigInteger pid = project.getProjectId();

        log.info("\taurl = [" + readyToXtract + "] for " + title + " pid " + pid);
        if (readyToXtract.equals("Ready")) {
            log.info("\tExtracting " + title);
            process(project);
        }
    }
    
    public void process(Project project) {
	String readyToXtract = project.getReadyForExtraction();
	if (!"Ready".equals(readyToXtract)) log.error("Proceeding even though project not ready: "+readyToXtract);        
	String groupType = project.getGroupType();
	if (!"Project".equals(groupType)) log.error("Group type other than Project: "+groupType);
	
	// project steps should have already been loaded by hand !!!
	// do not create project if it has no experiments or steps
	if (project.getProjectSteps() == null) return;
	if (project.getProjectSteps().getProjectStep().size() == 0) return;
	
	try {
	    Connection conn = CAPUtil.connectToBARD();
	    Statement st = conn.createStatement();
	    ResultSet result = st.executeQuery("select bard_proj_id, name, description from bard_project where cap_proj_id="+project.getProjectId());
	    if (result.next()) {
		int bardProjId = result.getInt(1);
		//String name = result.getString(2);
		//String description = result.getString(3);
		result.close();

		
//		if (name.equals("CAP")) { // hack to initially update project info from CAP
//	            PreparedStatement pst = conn.prepareStatement("update bard_project set name=?, description=? where cap_proj_id="+project.getProjectId());
//	            pst.setString(1, project.getProjectName());
//	            pst.setString(2, project.getDescription());
//	            pst.addBatch();
//	            pst.executeBatch();
//	            log.debug("Updated project name for bard project "+bardProjId+": "+project.getProjectName());
//		}

		
		// get annotation groupings
	        Map<String, String> parentGroups = new HashMap<String, String>();
		if (project.getProjectContextItems() != null)
		for (ProjectContextItem pci: project.getProjectContextItems().getProjectContextItem()) {
		    BigInteger parentGroup = pci.getParentGroup();
		    BigInteger id = pci.getProjectContextItemId();
		    if (parentGroup != null)
			if (parentGroups.containsKey(parentGroup.toString())) {
			    parentGroups.put(parentGroup.toString(), parentGroups.get(parentGroup.toString())+","+id);
			} else {
			    parentGroups.put(parentGroup.toString(), id.toString());
			}
		}
	        
	        // add project annotations
		PreparedStatement pst = conn.prepareStatement("insert into cap_project_annotations (bard_proj_id, cap_proj_id, source, entity, anno_id, anno_key, anno_value, anno_display, related) values (?,?,?,?,?,?,?,?,?)");

		if (project.getProjectContextItems() != null)
		for (ProjectContextItem pci: project.getProjectContextItems().getProjectContextItem()) {
		    pst.setInt(1, bardProjId);
		    pst.setInt(2, project.getProjectId().intValue());
		    pst.setString(3, "cap");
		    pst.setString(4, "project");
		    pst.setString(5, pci.getProjectContextItemId().toString());
		    
		    String key = null;
		    Attribute attr = pci.getAttribute();
		    if (attr != null) {
			attr.getLabel();
			String[] toks = attr.getLink().getHref().split("/");
			key = toks[toks.length - 1];
		    }
		    pst.setString(6, key);

		    String value = null;
		    ValueControlled vc = pci.getValueControlled();
		    if (vc != null) {
			vc.getLabel();
			String[] toks = vc.getLink().getHref().split("/");
			value = toks[toks.length - 1];
		    }
		    pst.setString(7, value);
		    
		    String valueDisplay = pci.getValueDisplay();
		    pst.setString(8, valueDisplay);

		    BigInteger parentGroup = pci.getParentGroup();		    
		    String related = parentGroup == null ? pci.getProjectContextItemId().toString() : parentGroups.get(parentGroup.toString());
		    String extValueId = pci.getExtValueId();		    
		    if (extValueId != null) related += "|" + extValueId;
		    pst.setString(9, related);
		    
		    pst.addBatch();
		}
		//pst.executeBatch();

	    } else {
		log.error("Database has no project with cap_proj_id="+project.getProjectId());
	    }
	    conn.commit();
	    st.close();
	    conn.close();
	} catch (SQLException ex) {
	    log.error("Failed to update database with cap_proj_id="+project.getProjectId());
	    ex.printStackTrace();
	}
    }
    
}
