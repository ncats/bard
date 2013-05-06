package gov.nih.ncgc.bard.entity;

import gov.nih.ncgc.bard.rest.BARDConstants;
import gov.nih.ncgc.bard.tools.BARDJsonRequired;

import java.sql.Timestamp;

/**
 * A representation of a target.
 * <p/>
 * This is a generic description of a target and allows one to represent genes, proteins, pathways
 * processes and so on as targets for a given entity.
 *
 * @author Rajarshi Guha
 */
public class Biology implements BardEntity {
    public static enum BiologyType {
        GENE, PROTEIN, SEQUENCE, PROCESS, COMPONENT, FUNCTION, NCBI, GO, UNKNOWN;

        public BiologyType getBiologyTypeFromDictId(int dictId) {
            switch (dictId) {
                case 1419:
                    return PROCESS;
                case 885:
                case 1795:
                    return NCBI;
                case 880:
                case 881:
                    return GENE;
                case 882:
                case 1398:
                    return PROTEIN;
                case 1504:
                    return GO;
                default:
                    return UNKNOWN;
            }
        }
    }

    @BARDJsonRequired
    String biology;

    @BARDJsonRequired
    Integer entity_id;

    String description, entity, extId, extRef, dictLabel;
    Integer dictId, serial;
    Timestamp updated;

    public Biology() {
    }

    public Biology(String description, String entity, String biology, String ext_ref, Integer entity_id, String ext_id, Timestamp updated, Integer serial) {
        this.description = description;
        this.entity = entity;
        this.biology = biology;
        this.extRef = ext_ref;
        this.entity_id = entity_id;
        this.extId = ext_id;
        this.updated = updated;
        this.serial = serial;
    }

    public Integer getSerial() {
        return serial;
    }

    public void setSerial(Integer serial) {
        this.serial = serial;
    }

    public String getDictLabel() {
        return dictLabel;
    }

    public void setDictLabel(String dictLabel) {
        this.dictLabel = dictLabel;
    }

    public Integer getDictId() {
        return dictId;
    }

    public void setDictId(Integer dictId) {
        this.dictId = dictId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getEntity() {
        return entity;
    }

    public void setEntity(String entity) {
        this.entity = entity;
    }

    public String getBiology() {
        return biology;
    }

    public void setBiology(String biology) {
        this.biology = biology;
    }

    public String getExtRef() {
        return extRef;
    }

    public void setExtRef(String extRef) {
        this.extRef = extRef;
    }

    public Integer getEntity_id() {
        return entity_id;
    }

    public void setEntity_id(Integer entity_id) {
        this.entity_id = entity_id;
    }

    public String getExtId() {
        return extId;
    }

    public void setExtId(String extId) {
        this.extId = extId;
    }

    public Timestamp getUpdated() {
        return updated;
    }

    public void setUpdated(Timestamp updated) {
        this.updated = updated;
    }

    /**
     * Return the path for this resource in the REST API.
     * <p/>
     * The actual resource can be accessed by prepending the hostname of the server
     * hosting the REST API.
     *
     * @return The path to this resource
     */
    public String getResourcePath() {
        return BARDConstants.API_BASE + "/biology/" + entity + "/" + entity_id;
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
