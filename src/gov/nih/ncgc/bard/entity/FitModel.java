package gov.nih.ncgc.bard.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * A representation of the readouts used to fit dose-response data.
 * <p/>
 * Currently, this class supports single point (i.e., no readouts) and
 * 4-parameter logistic (Hill readouts).
 *
 * @author Rajarshi Guha
 */
public class FitModel {
    String name;

    @JsonIgnore
    String description;

    Double s0 = null;
    Double sInf = null;
    Double hill = null;
    Double ac50 = null;

    Double[][] cr;
    Integer npoint;

    public FitModel() {
    }

    public FitModel(String description, Double s0, Double sInf, Double hill, Double ac50) {
        this.description = description;
        this.s0 = s0;
        this.sInf = sInf;
        this.hill = hill;
        this.ac50 = ac50;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double[][] getCr() {
        return cr;
    }

    public void setCr(Double[][] cr) {
        this.cr = cr;
        npoint = cr.length;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Double getS0() {
        return s0;
    }

    public void setS0(Double s0) {
        this.s0 = s0;
    }

    public Double getsInf() {
        return sInf;
    }

    public void setsInf(Double sInf) {
        this.sInf = sInf;
    }

    public Double getHill() {
        return hill;
    }

    public void setHill(Double hill) {
        this.hill = hill;
    }

    public Double getAc50() {
        return ac50;
    }

    public void setAc50(Double ac50) {
        this.ac50 = ac50;
    }
}
