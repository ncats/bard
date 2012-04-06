package gov.nih.ncgc.bard.entity;

import java.io.IOException;

/**
 * A one line summary.
 *
 * @author Rajarshi Guha
 */
public class AssayTarget implements BardEntity {
    Long aid, acc;

    public AssayTarget(Long aid, Long acc) {
        this.aid = aid;
        this.acc = acc;
    }

    public AssayTarget() {
    }

    public Long getAid() {
        return aid;
    }

    public void setAid(Long aid) {
        this.aid = aid;
    }

    public Long getAcc() {
        return acc;
    }

    public void setAcc(Long acc) {
        this.acc = acc;
    }

    public String toJson() throws IOException {
        return null;
    }
}
