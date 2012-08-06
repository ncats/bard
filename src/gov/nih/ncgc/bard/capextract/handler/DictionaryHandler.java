package gov.nih.ncgc.bard.capextract.handler;

import gov.nih.ncgc.bard.capextract.CAPConstants;
import gov.nih.ncgc.bard.capextract.CAPDictionary;
import gov.nih.ncgc.bard.capextract.CAPDictionaryElement;
import gov.nih.ncgc.bard.capextract.CAPUtil;
import gov.nih.ncgc.bard.capextract.ICapResourceHandler;
import gov.nih.ncgc.bard.capextract.jaxb.Dictionary;
import gov.nih.ncgc.bard.capextract.jaxb.Element;

import java.io.IOException;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
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
            dict.addNode(new CAPDictionaryElement(elem));
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

        // serialize this to the db
        Connection conn = null;
        PreparedStatement pst;
        java.util.Date today = null;
        try {
            conn = CAPUtil.connectToBARD();
            pst = conn.prepareStatement("INSERT INTO cap_dict(ins_date, dict) VALUES (?, ?)");
            today = new java.util.Date();
            pst.setDate(1, new java.sql.Date(today.getTime()));
            pst.setObject(2, dict);
            pst.executeUpdate();
            pst.close();
            conn.commit();
            conn.close();
            log.info("Serialized dictionary to database");
        } catch (com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException e) {
            if (e.getMessage().indexOf("Duplicate entry") >= 0) {
                log.warn("Already have a serialized dictionary for " + today + ", so not inserting");
            }
        } catch (SQLException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        // TODO should handle resultType, units and descriptors
    }

    private BigInteger getElementId(String url) {
        String[] comps = url.split("/");
        return new BigInteger(comps[comps.length - 1]);
    }
}
