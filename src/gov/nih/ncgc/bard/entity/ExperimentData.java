package gov.nih.ncgc.bard.entity;

import gov.nih.ncgc.bard.rest.BARDConstants;
import gov.nih.ncgc.bard.tools.DataResultObject;

import java.sql.Date;

/**
 * A representation of experiment data (ie measurements).
 *
 * @author Rajarshi Guha
 */
public class ExperimentData implements BardEntity {
    Long exptDataId;
    Long eid, cid, sid;
    Date updated;
    String runset = "default";
    int classification, outcome, score;
    float potency;

    DataResultObject[] results;

    public ExperimentData() {
    }

    public DataResultObject[] getResults() {
        return results;
    }

    public void setResults(DataResultObject[] results) {
        this.results = results;
    }

    public int getOutcome() {
        return outcome;
    }

    public void setOutcome(int outcome) {
        this.outcome = outcome;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public float getPotency() {
        return potency;
    }

    public void setPotency(float potency) {
        this.potency = potency;
    }

    public Long getExptDataId() {
        return exptDataId;
    }

    public void setExptDataId(Long exptDataId) {
        this.exptDataId = exptDataId;
    }

    public Long getEid() {
        return eid;
    }

    public void setEid(Long eid) {
        this.eid = eid;
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

    public String toString() {
        return "ExperimentData[" + exptDataId + ", outcome=" + outcome + ", score=" + score + ", potency=" + potency + "]";
    }

    /**
     * Return the path for this resource in the REST API.
     * <p/>
     * The actual resource can be accessed by prepending the hostname of the server
     * hosting the REST API.
     *
     * @return The path to this resource. <code>null</code> if the object is not meant
     *         to be publically available via the REST API
     */
    public String getResourcePath() {
        return BARDConstants.API_BASE + "/exptdata/" + exptDataId;
    }
}
