package gov.nih.ncgc.bard.capextract;

/**
 * A representation of an annotation.
 * <p/>
 * This is class is generic and meant to handle arbitrary annotations on arbitrary entities. Currently
 * it is primarily derived from the CAP Assay annotations, but in the future will probably be modified
 * to support non-CAP annotations.
 *
 * @author Rajarshi Guha
 */
public class CAPAnnotation {

    public Integer entityId = null; // not sure what this is
    public String entity;

    public String source;
    public Integer id;
    public String display;
    public String contextRef = null;
    public String contextGroup = null;
    public String key, value; // refers to a dict element
    public String extValueId = null; // when dict element points to ext resource (e.g. Entrez Gene) this is the identifier within that resource
    public String url = null; // when an annotation points to external object, given by a URL
    public int displayOrder;
    public String related;

    public CAPAnnotation(Integer id, Integer entityId,
                         String display, String contextRef,
                         String attrId, String valueId,
                         String extValueId, String source, String contextGroup) {
	this(id, entityId, display, contextRef, attrId, valueId, extValueId, source, null, 0, null, null, contextGroup);
    }

    public CAPAnnotation(Integer id, Integer entityId,
                         String display, String contextRef,
                         String attrId, String valueId,
                         String extValueId, String source,
                         String url,
                         int displayOrder, String entity, String related,
                         String contextGroup) {
        this.id = id;
        this.entityId = entityId;
        this.entity = entity;

        this.display = display;
        this.contextRef = contextRef;
        this.key = attrId;
        this.value = valueId;
        this.extValueId = extValueId;
        this.source = source;
        this.url = url;
        this.displayOrder = displayOrder;
        this.related = related;
        this.contextGroup = contextGroup;
    }

    public CAPAnnotation cloneObject () {
        return new CAPAnnotation (id, entityId, display, contextRef,
                                       key, value, extValueId, source, url, displayOrder, entity, related, contextGroup);
    }

    CAPAnnotation() {
    }

    @Override
    public String toString() {
        return "CAPAssayAnnotation{" +
                "id='" + id + '\'' +
                ", entityId='" + entityId + '\'' +
                ", display='" + display + '\'' +
                ", contextRef='" + contextRef + '\'' +
                ", contextGroup='" + contextGroup + '\'' +
                ", key='" + key + '\'' +
                ", value='" + value + '\'' +
                ", related='" + related + '\'' +
                '}';
    }
}
