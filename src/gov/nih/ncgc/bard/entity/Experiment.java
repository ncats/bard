package gov.nih.ncgc.bard.entity;

import gov.nih.ncgc.bard.rest.BARDConstants;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

/**
 * A representation of an experiment.
 *
 * @author Rajarshi Guha
 */
public class Experiment implements BardEntity {
    Long bardExptId, capExptId, bardAssayId, pubchemAid;
    int category, type, summary, assays, classification, substances, compounds;
    String name, description, source, grantNo;
    Date deposited, updated;
    Boolean hasProbe;
    List<Long> projectIdList;
    
    public Experiment() {
    	projectIdList = new ArrayList<Long>();
    }

    public Long getPubchemAid() {
        return pubchemAid;
    }

    public void setPubchemAid(Long pubchemAid) {
        this.pubchemAid = pubchemAid;
    }

    public Long getBardExptId() {
        return bardExptId;
    }

    public void setBardExptId(Long bardExptId) {
        this.bardExptId = bardExptId;
    }

    public List<Long> getProjectIdList() {
		return projectIdList;
	}

	public void setProjectIdList(List<Long> projectIdList) {
		this.projectIdList = projectIdList;
	}

	public void addProjectID(Long projId) {
		this.projectIdList.add(projId);
	}
	
	public Long getBardAssayId() {
        return bardAssayId;
    }

    public void setBardAssayId(Long bardAssayId) {
        this.bardAssayId = bardAssayId;
    }

    public Long getCapExptId() {
        return capExptId;
    }

    public void setCapExptId(Long capExptId) {
        this.capExptId = capExptId;
    }

    public int getCategory() {
        return category;
    }

    public void setCategory(int category) {
        this.category = category;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getSummary() {
        return summary;
    }

    public void setSummary(int summary) {
        this.summary = summary;
    }

    public int getAssays() {
        return assays;
    }

    public void setAssays(int assays) {
        this.assays = assays;
    }

    public int getClassification() {
        return classification;
    }

    public void setClassification(int classification) {
        this.classification = classification;
    }

    public int getSubstances() {
        return substances;
    }

    public void setSubstances(int substances) {
        this.substances = substances;
    }

    public int getCompounds() {
        return compounds;
    }

    public void setCompounds(int compounds) {
        this.compounds = compounds;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getGrantNo() {
        return grantNo;
    }

    public void setGrantNo(String grantNo) {
        this.grantNo = grantNo;
    }

    public Date getDeposited() {
        return deposited;
    }

    public void setDeposited(Date deposited) {
        this.deposited = deposited;
    }

    public Date getUpdated() {
        return updated;
    }

    public void setUpdated(Date updated) {
        this.updated = updated;
    }

    public Boolean getHasProbe() {
        return hasProbe;
    }

    public void setHasProbe(Boolean hasProbe) {
        this.hasProbe = hasProbe;
    }

    /**
     * Return the path for this resource in the REST API.
     * <p/>
     * The actual resource can be accessed by prepending the hostname of the server
     * hosting the REST API.
     *
     * @return The path to this resource
     */
    public String getResourcePath() {
        return BARDConstants.API_BASE + "/experiments/" + bardExptId;
    }

    /**
     * Set the resource path.
     * <p/>
     * In most cases, this can be an empty function as its primary purpose
     * is to allow Jackson to deserialize a JSON entity to the relevant Java
     * entity.
     *
     * @param resourcePath the resource path for this entity
     */
    public void setResourcePath(String resourcePath) {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
