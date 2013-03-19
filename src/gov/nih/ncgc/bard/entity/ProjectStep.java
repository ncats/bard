package gov.nih.ncgc.bard.entity;

import gov.nih.ncgc.bard.capextract.CAPAnnotation;

import java.util.List;

/**
 * @author Rajarshi Guha
 */
public class ProjectStep implements BardEntity {
    Long prevBardExpt, nextBardExpt, bardProjId;
    String prevStageRef, nextStageRef;
    Long stepId;
    String edgeName;
    List<CAPAnnotation> annotations;

    public ProjectStep() {
    }

    public ProjectStep(Long prevBardExptId, Long nextBardExptId, Long bardProjId, Long stepId, String edgeName) {
        this.prevBardExpt = prevBardExptId;
        this.nextBardExpt = nextBardExptId;
        this.bardProjId = bardProjId;
        this.stepId = stepId;
        this.edgeName = edgeName;
    }

    public String getPrevStageRef() {
        return prevStageRef;
    }

    public void setPrevStageRef(String prevStageRef) {
        this.prevStageRef = prevStageRef;
    }

    public String getNextStageRef() {
        return nextStageRef;
    }

    public void setNextStageRef(String nextStageRef) {
        this.nextStageRef = nextStageRef;
    }

    public List<CAPAnnotation> getAnnotations() {
        return annotations;
    }

    public void setAnnotations(List<CAPAnnotation> annotations) {
        this.annotations = annotations;
    }

    public Long getPrevBardExpt() {
        return prevBardExpt;
    }

    public void setPrevBardExpt(Long prevBardExpt) {
        this.prevBardExpt = prevBardExpt;
    }

    public Long getNextBardExpt() {
        return nextBardExpt;
    }

    public void setNextBardExpt(Long nextBardExpt) {
        this.nextBardExpt = nextBardExpt;
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
