package gov.nih.ncgc.bard.capextract;

import gov.nih.ncgc.bard.capextract.jaxb.Link;

import java.math.BigInteger;

/**
 * A one line summary.
 *
 * @author Rajarshi Guha
 */
public class CAPDictionaryElement {
    String label, description;
    Link link;
    BigInteger id;

    public CAPDictionaryElement() {

    }

    public BigInteger getId() {
        return id;
    }

    public void setId(BigInteger id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Link getLink() {
        return link;
    }

    public void setLink(Link link) {
        this.link = link;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof CAPDictionaryElement) {
            CAPDictionaryElement e = (CAPDictionaryElement) o;
            return e.getId().equals(id) && e.getLabel().equals(label);
        }
        return false;
    }

    @Override
    public String toString() {
        return label + "[" + id + "]";
    }
}