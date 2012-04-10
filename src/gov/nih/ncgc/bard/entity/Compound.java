package gov.nih.ncgc.bard.entity;

import com.fasterxml.jackson.databind.ObjectMapper;

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
    Long cid;
    String probeId = null;
    String url = null;

    public Compound(Long cid, String probeId, String url) {
        this.cid = cid;
        this.probeId = probeId;
        this.url = url;
    }

    public Compound() {
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
}
