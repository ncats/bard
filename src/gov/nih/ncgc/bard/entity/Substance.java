package gov.nih.ncgc.bard.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nih.ncgc.bard.rest.BARDConstants;
import gov.nih.ncgc.bard.tools.BARDJsonRequired;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Date;

/**
 * Represents Pubchem substances and includes the related CID's
 *
 * @author Rajarshi Guha
 */
public class Substance implements BardEntity {

    @BARDJsonRequired
    Long sid = null;

    Long cid;

    String depRegId, sourceName, url;
    String[] patentIds;
    String smiles;

    Date deposited, updated;

    public Substance(Long sid, Long cid) {
        this.sid = sid;
        this.cid = cid;
    }

    public Substance() {
    }

    public String getSmiles() {
        return smiles;
    }

    public void setSmiles(String smiles) {
        this.smiles = smiles;
    }

    public Date getDeposited() {
        return deposited;
    }

    public void setDeposited(Date deposited) {
        this.deposited = deposited;
    }

    public Date getUpdated() {
        return updated;
    }

    public void setUpdated(Date updated) {
        this.updated = updated;
    }

    public Long getSid() {
        return sid;
    }

    public Long getCid() {
        return cid;
    }

    public void setSid(Long sid) {
        this.sid = sid;
    }

    public void setCid(Long cid) {
        this.cid = cid;
    }

    public String getDepRegId() {
        return depRegId;
    }

    public void setDepRegId(String depRegId) {
        this.depRegId = depRegId;
    }

    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String[] getPatentIds() {
        return patentIds;
    }

    public void setPatentIds(String[] patentIds) {
        this.patentIds = patentIds;
    }

    public String toJson() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        Writer writer = new StringWriter();
        mapper.writeValue(writer, this);
        return writer.toString();
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
        return BARDConstants.API_BASE + "/substances/" + sid;
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
