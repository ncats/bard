package gov.nih.ncgc.bard.entity;

import java.io.IOException;

/**
 * A one line summary.
 *
 * @author Rajarshi Guha
 */
public class AssayConcentration implements BardEntity, Comparable<AssayConcentration> {
    Long assayDataId;
    float concentration, response;
    int order;

    public AssayConcentration(Long assayDataId, float concentration, float response, int order) {
        this.assayDataId = assayDataId;
        this.concentration = concentration;
        this.response = response;
        this.order = order;
    }

    public AssayConcentration() {
    }

    public Long getAssayDataId() {
        return assayDataId;
    }

    public void setAssayDataId(Long assayDataId) {
        this.assayDataId = assayDataId;
    }

    public float getConcentration() {
        return concentration;
    }

    public void setConcentration(float concentration) {
        this.concentration = concentration;
    }

    public float getResponse() {
        return response;
    }

    public void setResponse(float response) {
        this.response = response;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public String toJson() throws IOException {
        return null;
    }

    public int compareTo(AssayConcentration assayConcentration) {
        if (this.order < assayConcentration.getOrder()) return -1;
        else if (this.order > assayConcentration.getOrder()) return 1;
        return 0;
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
        return null;
    }
}
