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
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
