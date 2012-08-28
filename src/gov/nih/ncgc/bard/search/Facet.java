package gov.nih.ncgc.bard.search;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A one line summary.
 *
 * @author Rajarshi Guha
 */
public class Facet {
    String facetName;
    Map<String, Integer> counts;

    public Facet() {
    }

    public Facet(String facetName) {
        this.facetName = facetName;
        counts = new LinkedHashMap<String, Integer>();
    }

    @JsonIgnore
    public void addFacetValue(String value) {
        if (counts.containsKey(value)) {
            int n = counts.get(value);
            counts.put(value, ++n);
        } else {
            counts.put(value, 1);
        }
    }

    public String getFacetName() {
        return facetName;
    }

    public Map<String, Integer> getCounts() {
        return counts;
    }

    public void setFacetName(String facetName) {
        this.facetName = facetName;
    }

    public void setCounts(Map<String, Integer> counts) {
        this.counts = counts;
    }
}
