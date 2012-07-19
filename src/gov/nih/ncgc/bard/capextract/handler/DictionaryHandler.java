package gov.nih.ncgc.bard.capextract.handler;

import gov.nih.ncgc.bard.capextract.CAPConstants;
import gov.nih.ncgc.bard.capextract.CAPDictionary;
import gov.nih.ncgc.bard.capextract.CAPDictionaryElement;
import gov.nih.ncgc.bard.capextract.ICapResourceHandler;
import gov.nih.ncgc.bard.capextract.jaxb.Dictionary;
import gov.nih.ncgc.bard.capextract.jaxb.Element;
import gov.nih.ncgc.bard.capextract.jaxb.Link;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;

/**
 * A one line summary.
 *
 * @author Rajarshi Guha
 */
public class DictionaryHandler extends CapResourceHandler implements ICapResourceHandler {

    public DictionaryHandler() {
        super();
    }

    /**
     * Process a CAP entity that is located at some URL.
     *
     * @param url      The URL from which to retrieve the entity fron
     * @param resource The CAP resource that is meant to be processed. An implementing class
     *                 can choose to proceed or not based on this parameter.
     */
    public void process(String url, CAPConstants.CapResource resource) throws IOException {
        if (resource != CAPConstants.CapResource.DICTIONARY) return;
        log.info("Processing " + resource + " from " + url);
        Dictionary d = getResponse(url, resource);
        log.info("\tUnmarshalled dictionary");

        CAPDictionary dict = new CAPDictionary();

        List<Element> elems = d.getElements().getElement();
        for (Element elem : elems) {
            CAPDictionaryElement delem = new CAPDictionaryElement();
            delem.setDescription(elem.getDescription());
            delem.setId(elem.getElementId());
            delem.setLabel(elem.getLabel());

            // TODO I'm assuming a single link is defined for a given dictionary element
            List<Link> links = elem.getLink();
            for (Link link : links) {
                delem.setLink(link);
            }
            dict.addNode(delem);
        }
        log.info("\tAdded " + dict.size() + " <element> entries");

        int nrel = 0;
        int nnoparent = 0;
        List<Dictionary.ElementHierarchies.ElementHierarchy> hierarchies = d.getElementHierarchies().getElementHierarchy();
        for (Dictionary.ElementHierarchies.ElementHierarchy h : hierarchies) {
            String relType = h.getRelationshipType();
            BigInteger childId = getElementId(h.getChildElement().getLink().getHref());
            CAPDictionaryElement childElem = dict.getNode(childId);

            // there may be an element with no parent
            if (h.getParentElement() != null) {
                BigInteger parentId = getElementId(h.getParentElement().getLink().getHref());
                CAPDictionaryElement parentElem = dict.getNode(parentId);
                dict.addOutgoingEdge(parentElem, childElem, null);
                dict.addIncomingEdge(childElem, parentElem, relType);
            } else nnoparent++;

            nrel++;
        }
        log.info("\tAdded " + nrel + " parent/child relationships with " + nnoparent + " elements having no parent");

        // ok'we got everything we need. Lets make it available globally
        CAPConstants.setDictionary(dict);

        // TODO should handle resultType, units and descriptors
    }

    private BigInteger getElementId(String url) {
        String[] comps = url.split("/");
        return new BigInteger(comps[comps.length - 1]);
    }
}
