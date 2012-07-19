package gov.nih.ncgc.bard.capextract;

import gov.nih.ncgc.bard.capextract.jaxb.Element;

/**
 * A one line summary.
 *
 * @author Rajarshi Guha
 */
public class CAPDictionaryElement extends Element {

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
        unit = e.getUnit();
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