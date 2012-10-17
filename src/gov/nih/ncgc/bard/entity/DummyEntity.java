package gov.nih.ncgc.bard.entity;

/**
 * A place holder entity, that does nothing and stores nothing.
 *
 * This is useful for implementing resources that do not necessarily
 * correspond to a specific type of object. An example is the
 * {@link gov.nih.ncgc.bard.rest.BARDMonitorResource} which does not
 * refer to a specific entity.
 *
 * @author Rajarshi Guha
 */
public class DummyEntity implements BardEntity {
    /**
     * Return the path for this resource in the REST API.
     * <p/>
     * The actual resource can be accessed by prepending the hostname of the server
     * hosting the REST API.
     *
     * @return The path to this resource. <code>null</code> if the object is not meant
     *         to be publically available via the REST API
     */
    @Override
    public String getResourcePath() {
        return "";
    }

    /**
     * Set the resource path.
     * <p/>
     * In most cases, this can be an empty function as its primary purpose
     * is to allow Jackson to deserialize a JSON entity to the relevant Java
     * entity.
     *
     * @param resourcePath the resource path for this entity
     */
    @Override
    public void setResourcePath(String resourcePath) {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
