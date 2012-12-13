package gov.nih.ncgc.bard.capextract;

import com.fasterxml.jackson.annotation.JsonIgnore;

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
    @JsonIgnore
    public String refId = null; // not sure what this is
    @JsonIgnore
    public String entity;

    public String source;
    public String id;
    public String display;
    public String contextRef = null;
    public String key, value; // refers to a dict element
    public String extValueId = null; // when dict element points to ext resource (e.g. Entrez Gene) this is the identifier within that resource
    public String url = null; // when an annotation points to external object, given by a URL

    public CAPAnnotation(String id, String refId, String display, String contextRef, String attrId, String valueId, String extValueId, String source) {
	this(id, refId, display, contextRef, attrId, valueId, extValueId, source, null);
    }

    public CAPAnnotation(String id, String refId, String display, String contextRef, String attrId, String valueId, String extValueId, String source, String url) {
        this.id = id;
        this.refId = refId;

        this.display = display;
        this.contextRef = contextRef;
        this.key = attrId;
        this.value = valueId;
        this.extValueId = extValueId;
        this.source = source;
        this.url = url;
    }

    public CAPAnnotation cloneObject () {
        return new CAPAnnotation (id, refId, display, contextRef, 
                                       key, value, extValueId, source, url);
    }

    CAPAnnotation() {
    }

    @Override
    public String toString() {
        return "CAPAssayAnnotation{" +
                "id='" + id + '\'' +
                ", refId='" + refId + '\'' +
                ", display='" + display + '\'' +
                ", contextRef='" + contextRef + '\'' +
                ", key='" + key + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}
