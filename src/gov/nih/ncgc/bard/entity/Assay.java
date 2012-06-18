package gov.nih.ncgc.bard.entity;

import gov.nih.ncgc.bard.rest.BARDConstants;
import gov.nih.ncgc.bard.tools.Util;

import java.security.NoSuchAlgorithmException;
import java.sql.Date;
import java.util.List;

/**
 * A representation of an assay.
 *
 * @author Rajarshi Guha
 */
public class Assay implements BardEntity {
    Long aid, bardAid;
    int category, type, summary, assays, classification;
    String name, description, source, grantNo, protocol, comments;
    Date deposited, updated;

    List<Publication> publications;
    List<ProteinTarget> targets;

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public Long getBardAid() {
        return bardAid;
    }

    public void setBardAid(Long bardAid) {
        this.bardAid = bardAid;
    }

    public String toString() {
        return aid + "[" + name + "]";
    }

    public Assay() {
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


    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public List<Publication> getPublications() {
        return publications;
    }

    public void setPublications(List<Publication> publications) {
        this.publications = publications;
    }

    public List<ProteinTarget> getTargets() {
        return targets;
    }

    public void setTargets(List<ProteinTarget> targets) {
        this.targets = targets;
    }

    public Long getAid() {
        return aid;
    }

    public void setAid(Long aid) {
        this.aid = aid;
    }

    public Integer getCategory() {
        return category;
    }

    public void setCategory(Integer category) {
        this.category = category;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public Integer getSummary() {
        return summary;
    }

    public void setSummary(Integer summary) {
        this.summary = summary;
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

    /**
     * Return the path for this resource in the REST API.
     * <p/>
     * The actual resource can be accessed by prepending the hostname of the server
     * hosting the REST API.
     *
     * @return The path to this resource
     */
    public String getResourcePath() {
        return BARDConstants.API_BASE + "/assays/" + aid;
    }

    public String getEntityTag() {
        StringBuilder sb = new StringBuilder();
        sb.append(aid).append(category).append(type).append(summary).append(assays).append(classification);
        sb.append(name).append(grantNo).append(description).append(source);
        sb.append(deposited).append(updated);
        for (ProteinTarget t : targets) sb.append(t.getAcc());
        for (Publication p : publications) sb.append(p.getPubmedId());
        try {
            byte[] digest = Util.getMD5(sb.toString());
            return new String(digest);
        } catch (NoSuchAlgorithmException e) {
        }
        return null;
    }
}
