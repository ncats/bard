package gov.nih.ncgc.bard.search;

import java.util.List;
import java.util.Map;

/**
 * A one line summary.
 *
 * @author Rajarshi Guha
 */
public class SearchMeta {
    Long nhit;
    List<Facet> facets;
    int queryTime;
    long elapsedTime;
    Map<String, Map<String, Object>> matchingFields;
    Map<String, Float> scores;

    public SearchMeta() {
    }

    public Map<String, Float> getScores() {
        return scores;
    }

    public void setScores(Map<String, Float> scores) {
        this.scores = scores;
    }

    public Map<String, Map<String, Object>> getMatchingFields() {
        return matchingFields;
    }

    public void setMatchingFields(Map<String, Map<String, Object>> matchingFields) {
        this.matchingFields = matchingFields;
    }

    public Long getNhit() {
        return nhit;
    }

    public void setNhit(Long nhit) {
        this.nhit = nhit;
    }

    public List<Facet> getFacets() {
        return facets;
    }

    public void setFacets(List<Facet> facets) {
        this.facets = facets;
    }

    public int getQueryTime() {
        return queryTime;
    }

    public void setQueryTime(int queryTime) {
        this.queryTime = queryTime;
    }

    public long getElapsedTime() {
        return elapsedTime;
    }

    public void setElapsedTime(long elapsedTime) {
        this.elapsedTime = elapsedTime;
    }
}
