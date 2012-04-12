package gov.nih.ncgc.bard.entity;

/**
 * The interface that is implemented by all entities.
 * <p/>
 * Here an entity is essentially a database data type.
 *
 * @author Rajarshi Guha
 */
public interface BardEntity {

    /**
     * Return the path for this resource in the REST API.
     * <p/>
     * The actual resource can be accessed by prepending the hostname of the server
     * hosting the REST API.
     *
     * @return The path to this resource. <code>null</code> if the object is not meant
     *         to be publically available via the REST API
     */
    public String getResourcePath();
}
