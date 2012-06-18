package gov.nih.ncgc.bard.entity;

import java.util.List;

/**
 * A one line summary.
 *
 * @author Rajarshi Guha
 */
public class ExperimentResult {
    Long assayResultId, assayDataId;
    int replicateId, outcome, score;
    float potency, s0, sInf, hill, lac50;

    List<AssayConcentration> concs;

    public ExperimentResult(Long assayResultId, Long assayDataId, int replicateId, int outcome, int score, float potency, float s0, float sInf, float hill, float lac50, List<AssayConcentration> concs) {
        this.assayResultId = assayResultId;
        this.assayDataId = assayDataId;
        this.replicateId = replicateId;
        this.outcome = outcome;
        this.score = score;
        this.potency = potency;
        this.s0 = s0;
        this.sInf = sInf;
        this.hill = hill;
        this.lac50 = lac50;
        this.concs = concs;
    }

    public ExperimentResult() {
    }

    public Long getAssayResultId() {
        return assayResultId;
    }

    public void setAssayResultId(Long assayResultId) {
        this.assayResultId = assayResultId;
    }

    public Long getAssayDataId() {
        return assayDataId;
    }

    public void setAssayDataId(Long assayDataId) {
        this.assayDataId = assayDataId;
    }

    public int getReplicateId() {
        return replicateId;
    }

    public void setReplicateId(int replicateId) {
        this.replicateId = replicateId;
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

    public float getS0() {
        return s0;
    }

    public void setS0(float s0) {
        this.s0 = s0;
    }

    public float getsInf() {
        return sInf;
    }

    public void setsInf(float sInf) {
        this.sInf = sInf;
    }

    public float getHill() {
        return hill;
    }

    public void setHill(float hill) {
        this.hill = hill;
    }

    public float getLac50() {
        return lac50;
    }

    public void setLac50(float lac50) {
        this.lac50 = lac50;
    }

    public List<AssayConcentration> getConcs() {
        return concs;
    }

    public void setConcs(List<AssayConcentration> concs) {
        this.concs = concs;
    }
}
