package gov.nih.ncgc.bard.entity;

import java.io.IOException;

/**
 * The interface that is implemented by all entities.
 * <p/>
 * Here an entity is essentially a database data type.
 *
 * @author Rajarshi Guha
 */
public interface BardEntity {
    public String toJson() throws IOException;
}
