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
        return null;
    }

    /**
     * Set the resource path.
     * <p/>
     * In most cases, this can be an empty function as its primary purpose
     * is to allow Jackson to deserialize a JSON entity to the relevant Java
     * entity.
     *
     * @param resourcePath the resource path for this entity
     */
    public void setResourcePath(String resourcePath) {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
