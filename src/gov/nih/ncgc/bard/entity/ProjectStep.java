package gov.nih.ncgc.bard.entity;

import gov.nih.ncgc.bard.capextract.CAPAnnotation;

import java.util.List;

/**
 * @author Rajarshi Guha
 */
public class ProjectStep implements BardEntity {
    Long prevBardExptId, nextBardExptId, bardProjId;
    Long stepId;
    String edgeName;
    List<CAPAnnotation> annotations;

    public ProjectStep() {
    }

    public ProjectStep(Long prevBardExptId, Long nextBardExptId, Long bardProjId, Long stepId, String edgeName) {
        this.prevBardExptId = prevBardExptId;
        this.nextBardExptId = nextBardExptId;
        this.bardProjId = bardProjId;
        this.stepId = stepId;
        this.edgeName = edgeName;
    }

    public List<CAPAnnotation> getAnnotations() {
        return annotations;
    }

    public void setAnnotations(List<CAPAnnotation> annotations) {
        this.annotations = annotations;
    }

    public Long getPrevBardExptId() {
        return prevBardExptId;
    }

    public void setPrevBardExptId(Long prevBardExptId) {
        this.prevBardExptId = prevBardExptId;
    }

    public Long getNextBardExptId() {
        return nextBardExptId;
    }

    public void setNextBardExptId(Long nextBardExptId) {
        this.nextBardExptId = nextBardExptId;
    }

    public Long getBardProjId() {
        return bardProjId;
    }

    public void setBardProjId(Long bardProjId) {
        this.bardProjId = bardProjId;
    }

    public Long getStepId() {
        return stepId;
    }

    public void setStepId(Long stepId) {
        this.stepId = stepId;
    }

    public String getEdgeName() {
        return edgeName;
    }

    public void setEdgeName(String edgeName) {
        this.edgeName = edgeName;
    }

    @Override
    public String getResourcePath() {
        return "";
    }

    @Override
    public void setResourcePath(String resourcePath) {
    }
}
