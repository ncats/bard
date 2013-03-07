package gov.nih.ncgc.bard.entity;

/**
 * @author Rajarshi Guha
 */
public interface TargetClassification {
    String getSource();

    String getId();

    String getName();

    String getDescription();

    /**
     * A String representation of the nodes level in the classification hierarchy.
     * <p/>
     * This can be null or empty if the hierarchy does not provide such a descriptor.
     */
    String getLevelIdentifier();

    /**
     * Return the immediate children of this node in the classification hierarchy.
     */
    TargetClassification[] getChildren();
}
