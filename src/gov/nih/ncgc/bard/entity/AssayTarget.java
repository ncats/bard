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
