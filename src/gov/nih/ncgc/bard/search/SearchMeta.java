package gov.nih.ncgc.bard.search;

import java.util.List;

/**
 * A one line summary.
 *
 * @author Rajarshi Guha
 */
public class SearchMeta {
    Long nhit;
    List<Facet> facets;

    public SearchMeta() {
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
}
