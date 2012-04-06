package gov.nih.ncgc.bard.entity;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.sql.Date;
import java.util.List;

/**
 * A representation of an assay.
 *
 * @author Rajarshi Guha
 */
public class Assay implements BardEntity {
    Long aid, sourceId;
    Integer category, type, summary;
    String name, description, grantNo;
    Date deposited, updated;

    List<AssayPub> publications;
    List<AssayTarget> targets;
    List<AssayData> data;

    public Assay(Long aid, Long sourceId, Integer category, Integer type, Integer summary, String name, String description, String grantNo, Date deposited, Date updated) {
        this.aid = aid;
        this.sourceId = sourceId;
        this.category = category;
        this.type = type;
        this.summary = summary;
        this.name = name;
        this.description = description;
        this.grantNo = grantNo;
        this.deposited = deposited;
        this.updated = updated;
    }

    public Assay() {
    }

    public List<AssayData> getData() {
        return data;
    }

    public void setData(List<AssayData> data) {
        this.data = data;
    }

    public List<AssayPub> getPublications() {
        return publications;
    }

    public void setPublications(List<AssayPub> publications) {
        this.publications = publications;
    }

    public List<AssayTarget> getTargets() {
        return targets;
    }

    public void setTargets(List<AssayTarget> targets) {
        this.targets = targets;
    }

    public Long getAid() {
        return aid;
    }

    public void setAid(Long aid) {
        this.aid = aid;
    }

    public Long getSourceId() {
        return sourceId;
    }

    public void setSourceId(Long sourceId) {
        this.sourceId = sourceId;
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

    public String toJson() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        Writer writer = new StringWriter();
        mapper.writeValue(writer, this);
        return writer.toString();
    }
}
