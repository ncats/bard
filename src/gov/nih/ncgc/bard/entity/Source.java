package gov.nih.ncgc.bard.entity;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

/**
 * A one line summary.
 *
 * @author Rajarshi Guha
 */
public class Source implements BardEntity {
    String name;
    Long sourceId;

    public Source() {
    }

    public Source(Long sourceId, String name) {
        this.sourceId = sourceId;
        this.name = name;
    }

    public Long getSourceId() {
        return sourceId;
    }

    public void setSourceId(Long sourceId) {
        this.sourceId = sourceId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String toJson() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        Writer writer = new StringWriter();
        mapper.writeValue(writer, this);
        return writer.toString();
    }
}
