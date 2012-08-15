package gov.nih.ncgc.bard.search;

import java.util.HashMap;
import java.util.Map;

/**
 * A one line summary.
 *
 * @author Rajarshi Guha
 */
public class Facet {
    String facetName;
    Map<String, Integer> counts;

    public Facet(String facetName) {
        this.facetName = facetName;
        counts = new HashMap<String, Integer>();
    }

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
}
