package gov.nih.ncgc.bard.entity;

/**
 * A representation of the models used to fit dose-response data.
 * <p/>
 * Currently, this class supports single point (i.e., no models) and
 * 4-parameter logistic (Hill models).
 *
 * @author Rajarshi Guha
 */
public class FitModel {
    String description;
    Double s0, sInf, hill, ac50;
    Double[][] cr;

    public FitModel() {
    }

    public FitModel(String description, Double s0, Double sInf, Double hill, Double ac50) {
        this.description = description;
        this.s0 = s0;
        this.sInf = sInf;
        this.hill = hill;
        this.ac50 = ac50;
    }

    public Double[][] getCr() {
        return cr;
    }

    public void setCr(Double[][] cr) {
        this.cr = cr;
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
