package gov.nih.ncgc.bard.entity;

import gov.nih.ncgc.bard.rest.BARDConstants;
import gov.nih.ncgc.bard.tools.BARDJsonRequired;

import java.sql.Timestamp;

/**
 * A representation of a target.
 * <p/>
 * This is a generic name of a target and allows one to represent genes, proteins, pathways
 * processes and so on as targets for a given entity.
 *
 * @author Rajarshi Guha
 */
public class Biology extends BaseEntity {
    public static enum BiologyType {
        AASUBSTITUTION("AASUBSTITUTION"),
        GENE("GENE"), PROTEIN("PROTEIN"),
        SEQUENCE("SEQUENCE"), DISEASE("SEQUENCE"),
        PATHWAY("PATHWAY"), PROCESS("PROCESS"),
        COMPONENT("COMPONENT"), FUNCTION("FUNCTION"), NCBI("NCBI"), GO("GO"),
        UNCLASSIFIED("UNCLASSIFIED");


        private String typeString;

        private BiologyType(String typeString) {
            this.typeString = typeString;
        }

        public static BiologyType fromString(String typeString) {
            for (BiologyType s : values()) {
                if (s.typeString.equals(typeString)) return s;
            }
            return UNCLASSIFIED;
        }

        public static BiologyType getBiologyTypeFromDictId(int dictId) {
            switch (dictId) {
                case 525:
                case 507:
                    return AASUBSTITUTION;
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
                    return UNCLASSIFIED;
            }
        }
    }

    @BARDJsonRequired
    BiologyType biology;

    @BARDJsonRequired
    Long entityId;

    String name, entity, extId, extRef, dictLabel;
    Long dictId, serial;
    Timestamp updated;

    public Biology() {
    }

    public Biology(String name, String entity, BiologyType biology, String ext_ref, Long entity_id, String ext_id, Timestamp updated, Long serial) {
        this.name = name;
        this.entity = entity;
        this.biology = biology;
        this.extRef = ext_ref;
        this.entityId = entity_id;
        this.extId = ext_id;
        this.updated = updated;
        this.serial = serial;
    }

    public Long getSerial() {
        return serial;
    }

    public void setSerial(Long serial) {
        this.serial = serial;
    }

    public String getDictLabel() {
        return dictLabel;
    }

    public void setDictLabel(String dictLabel) {
        this.dictLabel = dictLabel;
    }

    public Long getDictId() {
        return dictId;
    }

    public void setDictId(Long dictId) {
        this.dictId = dictId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEntity() {
        return entity;
    }

    public void setEntity(String entity) {
        this.entity = entity;
    }

    public BiologyType getBiology() {
        return biology;
    }

    public void setBiology(BiologyType biology) {
        this.biology = biology;
    }

    public String getExtRef() {
        return extRef;
    }

    public void setExtRef(String extRef) {
        this.extRef = extRef;
    }

    public Long getEntityId() {
        return entityId;
    }

    public void setEntityId(Long entity_id) {
        this.entityId = entity_id;
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
        return BARDConstants.API_BASE + "/biology/" + serial;
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
