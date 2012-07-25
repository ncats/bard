package gov.nih.ncgc.bard.rest.rowdef;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * An internal class to hold the dose response data as extracted via Pubchem headers.
 * <p/>
 * This won't be directly exposed in the REST API.
 *
 * @author Rajarshi Guha
 */
public class DoseResponseResultObject {
    Double[] dose, response;
    Double hillCoef, zeroAct, infAct, ac50;

    String tid;
    @JsonIgnore
    String value;
    @JsonIgnore
    String logDoseResponse;
    @JsonIgnore
    String fittedDoseResponseCurve;

    String label, description;

    public DoseResponseResultObject() {
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Double[] getDose() {
        return dose;
    }

    public void setDose(Double[] dose) {
        this.dose = dose;
    }

    public Double[] getResponse() {
        return response;
    }

    public void setResponse(Double[] response) {
        this.response = response;
    }

    public Double getHillCoef() {
        return hillCoef;
    }

    public void setHillCoef(Double hillCoef) {
        this.hillCoef = hillCoef;
    }

    public Double getZeroAct() {
        return zeroAct;
    }

    public void setZeroAct(Double zeroAct) {
        this.zeroAct = zeroAct;
    }

    public Double getInfAct() {
        return infAct;
    }

    public void setInfAct(Double infAct) {
        this.infAct = infAct;
    }

    public Double getAc50() {
        return ac50;
    }

    public void setAc50(Double ac50) {
        this.ac50 = ac50;
    }

    public String getTid() {
        return tid;
    }

    public void setTid(String tid) {
        this.tid = tid;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getLogDoseResponse() {
        return logDoseResponse;
    }

    public void setLogDoseResponse(String logDoseResponse) {
        this.logDoseResponse = logDoseResponse;
    }

    public String getFittedDoseResponseCurve() {
        return fittedDoseResponseCurve;
    }

    public void setFittedDoseResponseCurve(String fittedDoseResponseCurve) {
        this.fittedDoseResponseCurve = fittedDoseResponseCurve;
    }
}
