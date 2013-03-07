package gov.nih.ncgc.bard.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * @author Rajarshi Guha
 */
public class PantherClassification implements TargetClassification{
    String id, name, description, levelIdentifier;
    PantherClassification[] children;

    @JsonIgnore
    int nodeLevel;

    public int getNodeLevel() {
        return nodeLevel;
    }

    public void setNodeLevel(int nodeLevel) {
        this.nodeLevel = nodeLevel;
    }

    @Override
    public String getSource() {
        return "panther";
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getLevelIdentifier() {
        return levelIdentifier;
    }

    @Override
    public TargetClassification[] getChildren() {
        return children;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setLevelIdentifier(String levelIdentifier) {
        this.levelIdentifier = levelIdentifier;
    }

    public void setChildren(PantherClassification[] children) {
        this.children = children;
    }
}
