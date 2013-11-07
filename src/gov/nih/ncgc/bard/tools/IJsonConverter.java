package gov.nih.ncgc.bard.tools;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * An interface for classes that will convert objects to custom JSON versions.
 * <p/>
 * If you need to perform custom JSON conversions (that are different from directly
 * using Jacksons classes) write a class that implements this interface.
 *
 * @author Rajarshi Guha
 */
public interface IJsonConverter<T> {
    public JsonNode convert(T o) throws Exception;
}
