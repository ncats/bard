package gov.nih.ncgc.bard.search;

import org.apache.solr.common.SolrDocument;

import java.util.List;

/**
 * A one line summary.
 *
 * @author Rajarshi Guha
 */
public class SearchResult {
    List<SolrDocument> docs;
    SearchMeta metaData;
    String link = null;

    public SearchResult() {
    }

    public List<SolrDocument> getDocs() {
        return docs;
    }

    public SearchMeta getMetaData() {
        return metaData;
    }

    public void setDocs(List<SolrDocument> docs) {
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

}