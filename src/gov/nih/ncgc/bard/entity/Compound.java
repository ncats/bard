package gov.nih.ncgc.bard.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nih.ncgc.bard.rest.BARDConstants;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

/**
 * Represents a compound.
 * <p/>
 * Currently, a compound is simply referred to by its CID in the database. I think
 * we should at least include the SMILES.
 * <p/>
 * TODO Will we retrieve structural details from Pubchem, always?
 *
 * @author Rajarshi Guha
 */
public class Compound implements BardEntity {
    Long cid, sid;
    String probeId = null;
    String url = null;
    String smiles = null;

    public Compound(Long cid, String probeId, String url) {
        this.cid = cid;
        this.probeId = probeId;
        this.url = url;
    }

    public Compound() {
    }

    public Long getSid() {
        return sid;
    }

    public void setSid(Long sid) {
        this.sid = sid;
    }

    public String getSmiles() {
        return smiles;
    }

    public void setSmiles(String smiles) {
        this.smiles = smiles;
    }

    public Long getCid() {
        return cid;
    }

    public String getProbeId() {
        return probeId;
    }

    public String getUrl() {
        return url;
    }

    public void setCid(Long cid) {
        this.cid = cid;
    }

    public void setProbeId(String probeId) {
        this.probeId = probeId;
    }

    public void setUrl(String url) {
        this.url = url;
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
        return BARDConstants.API_BASE + "/compounds/" + cid;
    }
}
