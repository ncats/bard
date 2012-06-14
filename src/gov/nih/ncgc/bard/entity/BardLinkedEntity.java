package gov.nih.ncgc.bard.entity;

/**
 * A simple container to enclose a collection of resources plus a link to the next collection.
 * <p/>
 * This is primarily useful for paged collections of entities.
 *
 * @author Rajarshi Guha
 */
public class BardLinkedEntity {
    Object collection;
    String link;

    public BardLinkedEntity(Object collection, String link) {
        this.collection = collection;
        this.link = link;
    }

    public Object getCollection() {
        return collection;
    }

    public void setCollection(Object collection) {
        this.collection = collection;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }
}
