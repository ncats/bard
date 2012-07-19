package gov.nih.ncgc.bard.capextract;

import gov.nih.ncgc.bard.capextract.jaxb.Link;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * A one line summary.
 *
 * @author Rajarshi Guha
 */
public class CAPDictionaryElement {
    String label, description;
    Link link;
    BigInteger id;
    List<CAPDictionaryElement> children;
    List<CAPDictionaryElement> parents;

    public CAPDictionaryElement() {
        children = new ArrayList<CAPDictionaryElement>();
    }

    public boolean isLeaf() {
        return children.size() == 0;
    }

    public List<CAPDictionaryElement> getChildren() {
        return children;
    }

    public void addChild(CAPDictionaryElement element) {
        children.add(element);
    }

    public void addParent(CAPDictionaryElement element) {
        parents.add(element);
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
}