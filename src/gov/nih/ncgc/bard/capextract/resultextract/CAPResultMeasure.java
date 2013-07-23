package gov.nih.ncgc.bard.capextract.resultextract;

import java.util.ArrayList;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * This class corresponds to a CAP measure.  It can contain context items and a collection of 
 * related measures.
 * 
 * @author braistedjc
 *
 */
@JsonInclude(Include.NON_NULL)
public class CAPResultMeasure {
    
    private String relationship;    
    private Integer replicateNumber;   
    private Integer resultTypeId;
    private String resultType;
    private Double valueNum;
    private Double valueMin;
    private Double valueMax;
    private Long resultId;  
    private String valueDisplay;
    private String qualifier;
    private Integer statsModifierId;
    private ArrayList <CAPMeasureContextItem> contextItems;
    private ArrayList <CAPResultMeasure> related;
    
    public CAPResultMeasure() { }
    
    public String getRelationship() {
        return relationship;
    }
    public void setRelationship(String relationship) {
        this.relationship = relationship;
    }
    public Integer getReplicateNumber() {
        return replicateNumber;
    }
    public void setReplicateNumber(Integer replicateNumber) {
        this.replicateNumber = replicateNumber;
    }
    public Integer getResultTypeId() {
        return resultTypeId;
    }
    public void setResultTypeId(Integer resultTypeId) {
        this.resultTypeId = resultTypeId;
    }
    public String getResultType() {
        return resultType;
    }
    public void setResultType(String resultType) {
        this.resultType = resultType;
    }
    public Double getValueNum() {
        return valueNum;
    }
    public void setValueNum(Double valueNum) {
        this.valueNum = valueNum;
    }
    public Long getResultId() {
        return resultId;
    }
    public void setResultId(Long resultId) {
        this.resultId = resultId;
    }
    public String getValueDisplay() {
        return valueDisplay;
    }
    public void setValueDisplay(String valueDisplay) {
        this.valueDisplay = valueDisplay;
    }
    public String getQualifier() {
        return qualifier;
    }
    public void setQualifier(String qualifier) {
        this.qualifier = qualifier;
    }
    public ArrayList<CAPMeasureContextItem> getContextItems() {
        return contextItems;
    }
    public void setContextItems(ArrayList<CAPMeasureContextItem> contextItems) {
        this.contextItems = contextItems;
    }
    public ArrayList<CAPResultMeasure> getRelated() {
        return related;
    }
    public void setRelated(ArrayList<CAPResultMeasure> related) {
        this.related = related;
    }
    public Integer getStatsModifierId() {
        return statsModifierId;
    }
    public void setStatsModifierId(Integer statsModifierId) {
        this.statsModifierId = statsModifierId;
    }

    public Double getValueMin() {
        return valueMin;
    }

    public void setValueMin(Double valueMax) {
        this.valueMin = valueMax;
    }

    public Double getValueMax() {
        return valueMax;
    }

    public void setValueMax(Double valueMax) {
        this.valueMax = valueMax;
    }
}
