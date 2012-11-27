package gov.nih.ncgc.bard.capextract;

import gov.nih.ncgc.bard.capextract.jaxb.Element;
import gov.nih.ncgc.bard.capextract.jaxb.Link;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.List;

/**
 * A one line summary.
 *
 * @author Rajarshi Guha
 */
public class CAPDictionaryElement implements Serializable {
    static final long serialVersionUID = -2501351369113941117L;

    private String abbreviation;
    private String description;
    private BigInteger elementId;
    private String externalUrl;
    private String label;
    transient private List<Link> link;
    private String elementStatus;
    transient private String readyForExtraction;
    private String synonyms;
//    private String unit;


    public String getAbbreviation() {
        return abbreviation;
    }

    public void setAbbreviation(String abbreviation) {
        this.abbreviation = abbreviation;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigInteger getElementId() {
        return elementId;
    }

    public void setElementId(BigInteger elementId) {
        this.elementId = elementId;
    }

    public String getExternalUrl() {
        return externalUrl;
    }

    public void setExternalUrl(String externalUrl) {
        this.externalUrl = externalUrl;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public List<Link> getLink() {
        return link;
    }

    public void setLink(List<Link> link) {
        this.link = link;
    }

    public String getElementStatus() {
        return elementStatus;
    }

    public void setElementStatus(String elementStatus) {
        this.elementStatus = elementStatus;
    }

    public String getReadyForExtraction() {
        return readyForExtraction;
    }

    public void setReadyForExtraction(String readyForExtraction) {
        this.readyForExtraction = readyForExtraction;
    }

    public String getSynonyms() {
        return synonyms;
    }

    public void setSynonyms(String synonyms) {
        this.synonyms = synonyms;
    }

//    public String getUnit() {
//        return unit;
//    }
//
//    public void setUnit(String unit) {
//        this.unit = unit;
//    }

    public CAPDictionaryElement(Element e) {
        abbreviation = e.getAbbreviation();
        description = e.getDescription();
        elementId = e.getElementId();
        externalUrl = e.getExternalUrl();
        label = e.getLabel();
        link = e.getLink();
        elementStatus = e.getElementStatus();
        readyForExtraction = e.getReadyForExtraction();
        abbreviation = e.getAbbreviation();
        synonyms = e.getSynonyms();
//        unit = e.getUnit();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof CAPDictionaryElement) {
            CAPDictionaryElement e = (CAPDictionaryElement) o;
            return e.getElementId().equals(elementId) && e.getLabel().equals(label);
        }
        return false;
    }

    @Override
    public String toString() {
        return label + "[" + elementId + "]";
    }
}