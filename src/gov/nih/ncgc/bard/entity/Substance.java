package gov.nih.ncgc.bard.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nih.ncgc.bard.rest.BARDConstants;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

/**
 * Represents Pubchem substances and includes the related CID's
 *
 * @author Rajarshi Guha
 */
public class Substance implements BardEntity {
    Long sid = null;
    Long cid;

    public Substance(Long sid, Long cid) {
        this.sid = sid;
        this.cid = cid;
    }

    public Substance() {
    }

    public Long getSid() {
        return sid;
    }

    public Long getCid() {
        return cid;
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
}
