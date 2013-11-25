package gov.nih.ncgc.bard.search;

import gov.nih.ncgc.bard.entity.Project;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Full text search for project entities.
 *
 * @author Rajarshi Guha
 */
public class ProjectSearch extends SolrSearch {
    private final String HL_FIELD = "text";
    private final String PKEY_PROJECT_DOC = "projectId";

    Logger log;

    public ProjectSearch(String query, String coreName) {
        super(query);
        CORE_NAME = coreName;
        log = LoggerFactory.getLogger(this.getClass());
    }

    public void run(boolean detailed, String filter, Integer top, Integer skip) throws MalformedURLException, SolrServerException {
        facets = new ArrayList<Facet>();
        results = new SearchResult();

        SolrServer solr = null;
        solr = new CommonsHttpSolrServer(getSolrURL() + CORE_NAME);

        QueryResponse response = null;

        SolrQuery sq = new SolrQuery(query);
        sq = setFilterQueries(sq, filter);
        sq.setShowDebugInfo(true);
        sq.setRows(10000);
        sq.setFacet(true);

        response = solr.query(sq);
        List<SolrDocument> docs = getHighlightedDocuments(response, PKEY_PROJECT_DOC, HL_FIELD);

        // field facets
        long start = System.currentTimeMillis();
        List<FacetField> solrf = response.getFacetFields();
        for (FacetField f : solrf) {
            Facet bardFacet = new Facet(f.getName());
            List<FacetField.Count> values = f.getValues();
            if (values != null) {
                for (FacetField.Count value : values) bardFacet.counts.put(value.getName(), (int) value.getCount());
            }
            facets.add(bardFacet);
        }

        // range facets
        Map<String, Integer> solrfq = response.getFacetQuery();
        Map<String, Facet> tmpFacets = new HashMap<String, Facet>();
        if (solrfq != null) {
            for (String solrFacetName : solrfq.keySet()) {
                Integer facetCount = solrfq.get(solrFacetName);
                String facetTitle = solrFacetName.split(":")[0];
                String facetlabel = solrFacetName.split(":")[1];
                if (tmpFacets.containsKey(facetTitle)) {
                    Facet bardFacet = tmpFacets.get(facetTitle);
                    bardFacet.counts.put(facetlabel, facetCount);
                } else {
                    Facet bardFacet = new Facet(facetTitle);
                    bardFacet.counts.put(facetlabel, facetCount);
                    tmpFacets.put(facetTitle, bardFacet);
                }
            }
        }
        for (Facet f : tmpFacets.values())
            facets.add(f);
        long end = System.currentTimeMillis();
        log.info("Facet summary calculated in " + (end - start) / 1000.0 + "s");

	List<Long> projIds = new ArrayList<Long>();
        for (SolrDocument doc : docs) {
            Object id = doc.getFirstValue(PKEY_PROJECT_DOC);
            try {
                long aid = Long.parseLong(id.toString());
                if (aid > 0l) {
                    projIds.add(aid);
                }
            } catch (Exception ex) {
                log.warn("Bogus bardProjid " + id);
            }
	}


        SearchMeta meta = new SearchMeta();
        meta.setNhit(response.getResults().getNumFound());
        meta.setFacets(facets);
        meta.setQueryTime(response.getQTime());
        meta.setElapsedTime(response.getElapsedTime());

        try {
            String etag = putEtag(projIds, Project.class);
            results.setETag(etag);
        } catch (Exception e) {
            log.error("Can't process ETag", e);
        }

        // only return the requested number of docs, from the requested starting point
        // and generate reduced representation if required
        //
        // Also extract the matching field names for the docs we do return
        Map<String, String> xplainMap = response.getExplainMap();
        Map<String, Map<String, Object>> matchFields = new HashMap<String, Map<String, Object>>();
        Map<String, Float> scores = new LinkedHashMap<String, Float>(); // to maintain doc id ordering

        // first set up field match details & document scores
        int size = Math.min(skip + top, docs.size());
        for (int i = skip; i < size; i++) {
            SolrDocument doc = docs.get(i);
            String projectId = (String) doc.getFieldValue(PKEY_PROJECT_DOC);
            Map<String, Object> value = new HashMap<String, Object>();
            List<String> fns = SearchUtil.getMatchingFieldNames(xplainMap.get(projectId));
            for (String fn : fns) {
                Object obj = doc.getFieldValue(fn);
                value.put(fn, obj);
            }
            matchFields.put(projectId, value);
            scores.put(projectId, (Float) doc.getFieldValue("score"));
        }
        meta.setMatchingFields(matchFields);
        meta.setScores(scores);

        List ret;
        if (!detailed) {
            ret = copyRange(docs, skip, top, detailed, PKEY_PROJECT_DOC, "name");
        } else {
            ret = new ArrayList();
            try {
                for (int i = skip; i < size; i++) {
                    SolrDocument doc = docs.get(i);
                    String projectId = (String) doc.getFieldValue(PKEY_PROJECT_DOC);
                    Project p = db.getProject(Long.parseLong(projectId));
                    if (p != null) ret.add(p);
                }
                db.closeConnection();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        results.setDocs(ret);
        results.setMetaData(meta);
    }

}
