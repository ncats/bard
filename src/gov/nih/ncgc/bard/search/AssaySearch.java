package gov.nih.ncgc.bard.search;

import gov.nih.ncgc.bard.capextract.CAPDictionary;
import gov.nih.ncgc.bard.capextract.CAPDictionaryElement;
import gov.nih.ncgc.bard.tools.DBUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Full text search for assay entities.
 *
 * @author Rajarshi Guha
 */
public class AssaySearch extends SolrSearch {
    private final String SOLR_URL = SOLR_BASE + "/core-assay/";
    private final String HL_FIELD = "text";
    private final String PKEY_ASSAY_DOC = "assay_id";

    Logger log;

    String[] facetNames = {"assay component", "assay mode", "assay type", "Cell line", "detection method type", "target_name"};

    public AssaySearch(String query) {
        super(query);
        log = LoggerFactory.getLogger(this.getClass());

        // since we currently facte on dictionary terms, lets pre-populate the facet
        // values with the children of the terms, setting their counts to 0
        facets = new ArrayList<Facet>();
        DBUtils db = new DBUtils();
        CAPDictionary dict = null;
        try {
            dict = db.getCAPDictionary();
            db.closeConnection();
        } catch (SQLException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (ClassNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        if (dict != null) {
            log.info("Got CAP dictionary with " + dict.getNodes().size() + " elements");
            for (String name : facetNames) {
                Set<CAPDictionaryElement> childs = dict.getChildren(name);
                if (childs == null) childs = new HashSet<CAPDictionaryElement>();
                Map<String, Integer> counts = new HashMap<String, Integer>();
                for (CAPDictionaryElement child : childs) {
                    counts.put(child.getLabel(), 0);
                }

                Facet f = new Facet(name);
                f.setCounts(counts);
                facets.add(f);
            }
        } else log.error("CAP dictionary was null. Strange!");
    }

    public void run(boolean detailed, String filter, Integer top, Integer skip) throws MalformedURLException, SolrServerException {
        results = new SearchResult();

        SolrServer solr = new CommonsHttpSolrServer(SOLR_URL);

        SolrQuery sq = new SolrQuery(query);
        sq = setHighlighting(sq, filter == null ? HL_FIELD : HL_FIELD);
        sq = setFilterQueries(sq, filter);
        sq.setRows(10000);

        sq.setFacet(true);
        sq.addFacetField("target_name");

        QueryResponse response = solr.query(sq);
        List<SolrDocument> docs = getHighlightedDocuments(response, PKEY_ASSAY_DOC, HL_FIELD);

        // get facet counts
        long start = System.currentTimeMillis();

        // first pull in direct facet counts via Solr
        Facet f = null;
        for (Facet aFacet : facets) {
            if (aFacet.getFacetName().equals("target_name")) {
                f = aFacet;
                break;
            }
        }
        FacetField targetFacet = response.getFacetField("target_name");
        List<FacetField.Count> fcounts = targetFacet.getValues();
        if (fcounts != null) {
            for (FacetField.Count fcount : fcounts) {
                f.counts.put(fcount.getName(), (int) fcount.getCount());
            }
        }

        // now process annotations to get remaining facet counts
        for (SolrDocument doc : docs) {

            Collection<Object> keys = doc.getFieldValues("ak_dict_label");
            Collection<Object> values = doc.getFieldValues("av_dict_label");
            if (keys == null || values == null) continue;
//            if (keys.size() != values.size())
//                log.error("for assay_id = " + doc.getFieldValue("assay_id") + " keys had " + keys.size() + " elements and values had " + values.size() + " elements");

            List<Object> keyList = new ArrayList<Object>(keys);
            List<Object> valueList = new ArrayList<Object>(values);
            for (Facet facet : facets) {
                for (int i = 0; i < keyList.size(); i++) {
                    if (keyList.get(i).equals(facet.getFacetName()))
                        if (i < valueList.size()) {
                            facet.addFacetValue((String) valueList.get(i));
                        }
                }
            }

        }
        long end = System.currentTimeMillis();
        log.info("Facet summary calculated in " + (end - start) / 1000.0 + "s");

        SearchMeta meta = new SearchMeta();
        meta.setNhit(response.getResults().getNumFound());
        meta.setFacets(facets);

        // only return the requested number of docs, from the requested starting point
        // and generate reduced representation if required
        List<SolrDocument> ret = copyRange(docs, skip, top, detailed, "assay_id", "name", "highlight");
        results.setDocs(ret);
        results.setMetaData(meta);

    }
}
