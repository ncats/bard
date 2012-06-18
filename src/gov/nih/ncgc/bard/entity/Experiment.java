package gov.nih.ncgc.bard.entity;

import gov.nih.ncgc.bard.rest.BARDConstants;
import gov.nih.ncgc.bard.tools.Util;

import java.security.NoSuchAlgorithmException;
import java.sql.Date;

/**
 * A representation of an experiment.
 *
 * @author Rajarshi Guha
 */
public class Experiment implements BardEntity {
    Long exptId, projId, assayId;
    int category, type, summary, assays, classification, substances, compounds;
    String name, description, source, grantNo;
    Date deposited, updated;
    Boolean hasProbe;

    public Experiment() {
    }

    public Long getExptId() {
        return exptId;
    }

    public void setExptId(Long exptId) {
        this.exptId = exptId;
    }

    public Long getProjId() {
        return projId;
    }

    public void setProjId(Long projId) {
        this.projId = projId;
    }

    public Long getAssayId() {
        return assayId;
    }

    public void setAssayId(Long assayId) {
        this.assayId = assayId;
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
        return BARDConstants.API_BASE + "/experiments/" + exptId;
    }

    public String getEntityTag() {
        StringBuilder sb = new StringBuilder();
        sb.append(exptId).append(category).append(type).append(summary).append(assays).append(classification).append(substances);
        sb.append(name).append(grantNo).append(description).append(source);
        sb.append(deposited).append(updated);
        try {
            byte[] digest = Util.getMD5(sb.toString());
            return new String(digest);
        } catch (NoSuchAlgorithmException e) {
        }
        return null;
    }
}
