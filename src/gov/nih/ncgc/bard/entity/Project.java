package gov.nih.ncgc.bard.entity;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

/**
 * A representation of a project.
 * <p/>
 * Currently a project is just a summary assay. In the future we expect
 * this class to represent both (old) summary assays and explict project
 * management meta-data.
 *
 * @author Rajarshi Guha
 */
public class Project implements BardEntity {
    Long aid;
    int category, type, classification;
    String name, description, source, grantNo;
    Date deposited, updated;

    List<Long> probeIds;
    List<Long> aids;
    List<Publication> publications;
    List<ProteinTarget> targets;


    public Project(Long aid, int category, int type, int classification, String name, String description, String source, String grantNo, Date deposited, Date updated) {
        this.aid = aid;
        this.category = category;
        this.type = type;
        this.classification = classification;
        this.name = name;
        this.description = description;
        this.source = source;
        this.grantNo = grantNo;
        this.deposited = deposited;
        this.updated = updated;

        aids = new ArrayList<Long>();
    }

    public String toString() {
        return aid + "[" + name + "]";
    }

    public List<Long> getProbeIds() {
        return probeIds;
    }

    public void setProbeIds(List<Long> probeIds) {
        this.probeIds = probeIds;
    }

    public Project() {
        aids = new ArrayList<Long>();
    }

    public int getAssayCount() {
        return aids.size();
    }

    public int getClassification() {
        return classification;
    }

    public void setClassification(int classification) {
        this.classification = classification;
    }

    public List<Long> getAids() {
        return aids;
    }

    public void setAids(List<Long> aids) {
        this.aids = aids;
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
}
