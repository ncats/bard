package gov.nih.ncgc.bard.entity;

/**
 * @author Rajarshi Guha
 */
public abstract class BaseEntity implements BardEntity {
    public String statusWarning = "THIS IS PREVIEW DATA that has not been curated yet, and may be revised or deprecated without notice. PLEASE USE WITH CAUTION";

    public String getStatusWarning() {
        return statusWarning;
    }

    public void setStatusWarning(String statusWarning) {
        this.statusWarning = statusWarning;
    }
}
