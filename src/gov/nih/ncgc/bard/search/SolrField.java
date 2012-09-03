package gov.nih.ncgc.bard.search;

/**
 * A representation of a Solr <code>Field</code>.
 * <p/>
 * Pretty much just stores the field name and field type
 *
 * @author Rajarshi Guha
 */
public class SolrField {
    String name, type;

    public SolrField(String name, String type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
