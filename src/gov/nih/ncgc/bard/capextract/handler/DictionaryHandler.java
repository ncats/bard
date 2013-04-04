package gov.nih.ncgc.bard.capextract.handler;

import gov.nih.ncgc.bard.capextract.*;
import gov.nih.ncgc.bard.capextract.jaxb.Dictionary;
import gov.nih.ncgc.bard.capextract.jaxb.Element;

import java.io.IOException;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Vector;

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

        CAPDictionary dict = process(d);
        
        // serialize this to the db
        Connection conn = null;
        PreparedStatement pst;
        java.util.Date today = null;
        try {
            conn = CAPUtil.connectToBARD(CAPConstants.getBardDBJDBCUrl());
            pst = conn.prepareStatement("INSERT INTO cap_dict_obj(ins_date, dict) VALUES (?, ?)");
            today = new java.util.Date();
            pst.setDate(1, new java.sql.Date(today.getTime()));
            pst.setObject(2, dict);
            pst.executeUpdate();
            pst.close();
            conn.commit();
            log.info("\tSerialized dictionary object to database");

            // now we dump in the dict elements (a partial representation) that will be useful
            // for SQL queries. We're assuming for now that a dict elem is associated with
            // a single ontology
            pst = conn.prepareStatement("insert into cap_dict_elem (ins_date, dictid, label, description, abbreviation, ext_url, onto_name, onto_abbrv, onto_url, onto_id, element_status) values (?,?,?,?,?,?,  ?,?,?,?, ?)");
            for (CAPDictionaryElement elem : dict.getNodes()) {
                pst.setDate(1, new java.sql.Date(today.getTime()));
                pst.setInt(2, elem.getElementId().intValue());
                pst.setString(3, elem.getLabel());
                pst.setString(4, elem.getDescription());
                pst.setString(5, elem.getAbbreviation());
                pst.setString(6, elem.getExternalUrl());

                pst.setString(7, elem.getOnto_name());
                pst.setString(8, elem.getOnto_abbrv());
                pst.setString(9, elem.getOnto_url());
                pst.setString(10, elem.getOnto_id());

                pst.setString(11, elem.getElementStatus());

                pst.addBatch();
            }
            pst.executeBatch();
            conn.commit();
            log.info("\tStored (partial) dictionary elements to database");
            conn.close();
        } catch (com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException e) {
            if (e.getMessage().indexOf("Duplicate entry") >= 0) {
                log.warn("Already have a serialized dictionary for " + today + ", so not inserting");
            }
        } catch (SQLException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        // TODO should handle resultType, units and descriptors
    }

    private CAPDictionary process(Dictionary d) throws IOException {
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
        
        return dict;
    }
    	
    public Vector<Object> poll(String url, CAPConstants.CapResource resource, boolean skipPartial) throws IOException {
	Vector<Object> vec = new Vector<Object>();
	Dictionary d = getResponse(url, resource);
	process(d);
	vec.add(d);
	return vec;
    }
    
    private BigInteger getElementId(String url) {
        String[] comps = url.split("/");
        return new BigInteger(comps[comps.length - 1]);
    }
}
