package gov.nih.ncgc.bard.capextract.resultextract;

/**
 * This class represents a CAP measure context. 
 * 
 * @author braistedjc
 *
 */
public class CAPMeasureContextItem {
    
    private String attribute;
    private Integer attributeId;
    private String valueDisplay;
    private Double valueNum;
    private String extValueId;
    private String qualifier;
    private Long itemId;
    
    /**
     * Default constructor
     */
    public CAPMeasureContextItem() { }
    
    public String getAttribute() {
        return attribute;
    }
    public void setAttribute(String attribute) {
        this.attribute = attribute;
    }
    public Integer getAttributeId() {
        return attributeId;
    }
    public void setAttributeId(Integer attributeId) {
        this.attributeId = attributeId;
    }

    public String getValueDisplay() {
        return valueDisplay;
    }
    public void setValueDisplay(String valueDisplay) {
        this.valueDisplay = valueDisplay;
    }
    public Double getValueNum() {
        return valueNum;
    }
    public void setValueNum(Double valueNum) {
        this.valueNum = valueNum;
    }
    public String getQualifier() {
        return qualifier;
    }
    public void setQualifier(String qualifier) {
        this.qualifier = qualifier;
    }
    public Long getItemId() {
        return itemId;
    }
    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }
    public String getExtValueId() {
        return extValueId;
    }
    public void setExtValueId(String extValueId) {
        this.extValueId = extValueId;
    }
}
