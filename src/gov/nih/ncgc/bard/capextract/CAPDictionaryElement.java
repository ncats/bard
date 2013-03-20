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

    String onto_name, onto_abbrv, onto_url, onto_id;

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

        Element.Ontologies ontologies = e.getOntologies();
        if (ontologies != null) {
            int nonto = 0;
            for (Element.Ontologies.Ontology onto : ontologies.getOntology()) {
                nonto++;
                if (nonto > 1)
                    System.out.println("Element ID " + elementId + " was associated with " + nonto + " ontology terms");
                onto_name = onto.getName();
                onto_abbrv = onto.getAbbreviation();
                onto_url = onto.getSourceUrl();
                onto_id = null;
            }
        } else {
            System.out.println("Element ID " + elementId + " was associated with no ontologies");
        }
//        unit = e.getUnit();
    }

    public String getOnto_name() {
        return onto_name;
    }

    public void setOnto_name(String onto_name) {
        this.onto_name = onto_name;
    }

    public String getOnto_abbrv() {
        return onto_abbrv;
    }

    public void setOnto_abbrv(String onto_abbrv) {
        this.onto_abbrv = onto_abbrv;
    }

    public String getOnto_url() {
        return onto_url;
    }

    public void setOnto_url(String onto_url) {
        this.onto_url = onto_url;
    }

    public String getOnto_id() {
        return onto_id;
    }

    public void setOnto_id(String onto_id) {
        this.onto_id = onto_id;
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
        return label + "[" + elementId + "]: " + description + " | " + abbreviation + " " + synonyms;
    }
}