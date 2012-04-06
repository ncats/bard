package gov.nih.ncgc.bard.entity;

import java.io.IOException;

/**
 * A one line summary.
 *
 * @author Rajarshi Guha
 */
public class SourceSubstance implements BardEntity {
    Long sourceId, sid;

    public SourceSubstance(Long sourceId, Long sid) {
        this.sourceId = sourceId;
        this.sid = sid;
    }

    public SourceSubstance() {
    }

    public Long getSourceId() {
        return sourceId;
    }

    public void setSourceId(Long sourceId) {
        this.sourceId = sourceId;
    }

    public Long getSid() {
        return sid;
    }

    public void setSid(Long sid) {
        this.sid = sid;
    }

    public String toJson() throws IOException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
