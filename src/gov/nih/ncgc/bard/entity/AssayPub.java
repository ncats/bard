package gov.nih.ncgc.bard.entity;

import java.io.IOException;

/**
 * Represents an association between a publication and an assay.
 *
 * @author Rajarshi Guha
 */
public class AssayPub implements BardEntity {
    Long aid, pubmedId;

    public AssayPub(Long aid, Long pubmedId) {
        this.aid = aid;
        this.pubmedId = pubmedId;
    }

    public AssayPub() {
    }

    public Long getAid() {
        return aid;
    }

    public void setAid(Long aid) {
        this.aid = aid;
    }

    public Long getPubmedId() {
        return pubmedId;
    }

    public void setPubmedId(Long pubmedId) {
        this.pubmedId = pubmedId;
    }

    public String toJson() throws IOException {
        return null;
    }
}
