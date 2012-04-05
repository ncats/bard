package gov.nih.ncgc.bard.entity;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;

/**
 * Represents Pubchem substances and includes the related CID's
 *
 * @author Rajarshi Guha
 */
public class Substance implements BardEntity {
    String sid = null;
    List<String> cid;

    public Substance(String sid, List<String> cid) {
        this.sid = sid;
        this.cid = cid;
    }

    public Substance() {
    }

    public String getSid() {
        return sid;
    }

    public List<String> getCid() {
        return cid;
    }

    public String toJson() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        Writer writer = new StringWriter();
        mapper.writeValue(writer, this);
        return writer.toString();
    }
}
