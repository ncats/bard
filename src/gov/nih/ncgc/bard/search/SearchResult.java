package gov.nih.ncgc.bard.search;

import java.util.List;

/**
 * A one line summary.
 *
 * @author Rajarshi Guha
 */
public class SearchResult {
    List docs;
    SearchMeta metaData;
    String etag;
    String link = null;

    public SearchResult() {
    }

    public List getDocs() {
        return docs;
    }

    public SearchMeta getMetaData() {
        return metaData;
    }

    public void setDocs(List docs) {
        this.docs = docs;
    }

    public void setMetaData(SearchMeta metaData) {
        this.metaData = metaData;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getETag () { return etag; }
    public void setETag (String etag) { this.etag = etag; }
}
