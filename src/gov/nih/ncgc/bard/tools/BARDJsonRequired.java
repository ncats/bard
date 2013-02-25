package gov.nih.ncgc.bard.tools;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation to indicate a field is required in the JSON schema for that class.
 *
 * There may be something in Jackson that already does this, but I couldn't find it.
 *
 * @author Rajarshi Guha
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface BARDJsonRequired {
}
