package gov.nih.ncgc.bard.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nih.ncgc.bard.rest.BARDConstants;
import gov.nih.ncgc.bard.tools.BARDJsonRequired;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;

/**
 * Represents Uniprot protein targets.
 *
 * @author Rajarshi Guha
 */
public class ProteinTarget implements BardEntity {

    @BARDJsonRequired
    String acc;

    String name, description, status, url;
    Long geneId, taxId;

    List<TargetClassification> classes;

    public ProteinTarget(String acc, String name, String description, String status, Long geneId, Long taxId) {
        this.acc = acc;
        this.name = name;
        this.description = description;
        this.status = status;
        this.geneId = geneId;
        this.taxId = taxId;
        if (acc != null) this.url = "http://www.uniprot.org/uniprot/"+acc;
        else this.url = null;
    }

    public boolean equals(Object o) {
        return o instanceof ProteinTarget && ((ProteinTarget) o).getAcc().equals(acc);
    }

    public List<TargetClassification> getClasses() {
        return classes;
    }

    public void setClasses(List<TargetClassification> classes) {
        this.classes = classes;
    }

    public String getUrl() {
        if (url == null && acc != null) return "http://www.uniprot.org/uniprot/" + acc;
        else return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public int hashCode() {
        return acc.hashCode();
    }

    public ProteinTarget() {
    }

    public String getAcc() {
        return acc;
    }

    public void setAcc(String acc) {
        this.acc = acc;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getGeneId() {
        return geneId;
    }

    public void setGeneId(Long geneId) {
        this.geneId = geneId;
    }

    public Long getTaxId() {
        return taxId;
    }

    public void setTaxId(Long taxId) {
        this.taxId = taxId;
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
        return BARDConstants.API_BASE + "/targets/accession/" + acc;
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
