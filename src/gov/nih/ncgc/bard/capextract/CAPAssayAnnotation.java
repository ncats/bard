package gov.nih.ncgc.bard.capextract;

/**
 * A one line summary.
 *
 * @author Rajarshi Guha
 */
public class CAPAssayAnnotation {
    public String id, refId = null, display;
    public String contextRef = null;
    public String attrId, valueId; // refers to a dict element
    public String extValueId = null; // when dict element points to ext resource (e.g. Entrez Gene) this is the identifier within that resource

    public CAPAssayAnnotation(String id, String refId, String display, String contextRef, String attrId, String valueId, String extValueId) {
        this.id = id;
        this.refId = refId;
        this.display = display;
        this.contextRef = contextRef;
        this.attrId = attrId;
        this.valueId = valueId;
        this.extValueId = extValueId;
    }

    CAPAssayAnnotation() {
    }

    @Override
    public String toString() {
        return "CAPAssayAnnotation{" +
                "id='" + id + '\'' +
                ", refId='" + refId + '\'' +
                ", display='" + display + '\'' +
                ", contextRef='" + contextRef + '\'' +
                ", attrId='" + attrId + '\'' +
                ", valueId='" + valueId + '\'' +
                '}';
    }
}
