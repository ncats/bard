package gov.nih.ncgc.bard.entity;

import java.io.IOException;
import java.sql.Date;

/**
 * A one line summary.
 *
 * @author Rajarshi Guha
 */
public class AssayData implements BardEntity {
    Long assayDataId;
    Long aid, cid, sid;
    int classification;
    Date updated;
    String runset = "default";

    public AssayData(Long assayDataId, Long aid, Long cid, Long sid, int classification, Date updated, String runset) {
        this.assayDataId = assayDataId;
        this.aid = aid;
        this.cid = cid;
        this.sid = sid;
        this.classification = classification;
        this.updated = updated;
        this.runset = runset;
    }

    public AssayData() {
    }

    public Long getAssayDataId() {
        return assayDataId;
    }

    public void setAssayDataId(Long assayDataId) {
        this.assayDataId = assayDataId;
    }

    public Long getAid() {
        return aid;
    }

    public void setAid(Long aid) {
        this.aid = aid;
    }

    public Long getCid() {
        return cid;
    }

    public void setCid(Long cid) {
        this.cid = cid;
    }

    public Long getSid() {
        return sid;
    }

    public void setSid(Long sid) {
        this.sid = sid;
    }

    public int getClassification() {
        return classification;
    }

    public void setClassification(int classification) {
        this.classification = classification;
    }

    public Date getUpdated() {
        return updated;
    }

    public void setUpdated(Date updated) {
        this.updated = updated;
    }

    public String getRunset() {
        return runset;
    }

    public void setRunset(String runset) {
        this.runset = runset;
    }

    public String toJson() throws IOException {
        return null;
    }
}
