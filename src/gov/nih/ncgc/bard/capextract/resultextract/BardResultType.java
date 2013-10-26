package gov.nih.ncgc.bard.capextract.resultextract;

import java.util.ArrayList;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * The Result class is a container for result definitions. Objects of this class can be
 * written as JSON with support for the Jackson ObjectMapper and is annotated to only
 * write non-null fields.
 *
 * @author braistedjc
 *
 */
@JsonInclude(Include.NON_NULL)
public class BardResultType {

    @JsonIgnore
    private int dataID;
    @JsonIgnore
    private Integer parentDataID;
    @JsonIgnore
    private Integer displayPriority;
    private String displayName;
    @JsonIgnore
    private String resultType;
    private Integer dictElemId;
    private String responseUnit;
    @JsonIgnore
    private Integer concSeriesId;
    private Double testConc;
    private String testConcUnit;
    private String value;
    private Double valueMax;
    private Double valueMin;
    private Boolean excludeDataPoint;
    @JsonIgnore
    private Boolean isActiveConc;
    @JsonIgnore
    private Integer qualifierId;
    private String extValueId;
    private String qualifierValue;
    private Integer statsModifierId;
    private String readoutName;

    //holds potential CR curve
    private BardConcResponseSeries concResponseSeries;

    // @JsonIgnore
    // private Integer contextGroupID;
    private ArrayList <BardResultType> primaryElements;
    private ArrayList <BardResultType> childElements;
    private BardResultType parentElement;

    public static enum DisplayPriorityLevels {
	/**
	 * <html>Highest of 3 display priorities,<br>
	 * key information ec50(s), efficacy, single point result.<html>
	 */
	@JsonIgnore
	DISPLAY_PRIORITY_PRIMARY_RESULT,
	/**
	 * <html>Middle (default level) of 3 display priorities,<br>
	 * includes C/R curve activities, curve fit parameters, fit scores such as r^2, etc..<html>
	 */	
	@JsonIgnore
	DISPLAY_PRIORITY_SECONDARY_RESULT,
	/**
	 * <html>Lowest of 3 display priorities,<br>
	 * this information is rarely required for data interpretation.<html>
	 */
	@JsonIgnore
	DISPLAY_PRIORITY_TERTIARY_RESULT
    }

    public BardResultType() {
	displayPriority = DisplayPriorityLevels.DISPLAY_PRIORITY_SECONDARY_RESULT.ordinal();
    }

    public BardResultType cloneResultTypeSkipCollections() {
	BardResultType result = new BardResultType();
	result.setConcSeriesId(this.getConcSeriesId());
	result.setDataID(this.getDataID());
	result.setDictElemId(this.getDictElemId());
	result.setDisplayName(this.getDisplayName());
	result.setDisplayPriority(this.getDisplayPriority());
	result.setExcludeDataPoint(this.getExcludeDataPoint());
	result.setParentDataID(this.getParentDataID());
	result.setResponseUnit(this.getResponseUnit());
	result.setResultType(this.getResultType());
	result.setTestConc(this.getTestConc());
	result.setTestConcUnit(this.getTestConcUnit());
	result.setIsActiveConc(this.getIsActiveConc());
	result.setQualifierId(this.getQualifierId());
	result.setQualifierValue(this.getQualifierValue());
	result.setReadoutName(this.getReadoutName());
	result.setValueMax(this.getValueMax());
	result.setValueMin(this.getValueMin());
	result.setValue(this.getValue());
	result.setStatsModifierId(this.getStatsModifierId());
	return result;
    }

    public void addChildResult(BardResultType child) {
	if(childElements == null)
	    childElements = new ArrayList <BardResultType>();
	childElements.add(child);
    }

    public void addPrimaryElement(BardResultType child) {
	if(primaryElements == null)
	    primaryElements = new ArrayList <BardResultType>();
	primaryElements.add(child);
    }

    public void removeChildElement(BardResultType child) {
	if(childElements != null) {
	    if(!childElements.remove(child)) {
		if(primaryElements != null) {
		    primaryElements.remove(child);
		}
	    }
	}
    }
    
    public BardResultType getResult(int searchDataId) {
	BardResultType result = null;
	boolean found = false;

	//break if found
	if(dataID == searchDataId)
	    return this;

	if(childElements != null) {
	    for(int i = 0; i < childElements.size() && !found; i++) {
		result = childElements.get(i).getResult(searchDataId);
		if(result != null)
		    found = true;
	    }
	}

	return result;
    }

    public int getDataID() {
	return dataID;
    }

    public void setDataID(int dataID) {
	this.dataID = dataID;
    }

    public String getDisplayName() {
	return displayName;
    }

    public void setDisplayName(String displayName) {
	this.displayName = displayName;
    }

    public String getResultType() {
	return resultType;
    }

    public void setResultType(String resultType) {
	this.resultType = resultType;
    }

    public Integer getDictElemId() {
	return dictElemId;
    }

    public void setDictElemId(Integer dictElemId) {
	this.dictElemId = dictElemId;
    }

    public Double getTestConc() {
	return testConc;
    }

    public void setTestConc(Double conc) {
	this.testConc = conc;
    }

    public String getTestConcUnit() {
	return testConcUnit;
    }

    public void setTestConcUnit(String concUnit) {
	this.testConcUnit = concUnit;
    }

    public Integer getParentDataID() {
	return parentDataID;
    }

    public void setParentDataID(Integer parentDataID) {
	this.parentDataID = parentDataID;
    }

    public Integer getDisplayPriority() {
	return displayPriority;
    }

    public void setDisplayPriority(Integer displayPriority) {
	this.displayPriority = displayPriority;
    }

    public String getResponseUnit() {
	return responseUnit;
    }

    public void setResponseUnit(String responseUnit) {
	this.responseUnit = responseUnit;
    }

    public String getValue() {
	return value;
    }

    public void setValue(String value) {
	this.value = value;
    }

    public BardConcResponseSeries getConcResponseSeries() {
	return concResponseSeries;
    }

    public void setConcResponseSeries(BardConcResponseSeries concResponseSeries) {
	this.concResponseSeries = concResponseSeries;
    }

    public Integer getConcSeriesId() {
	return concSeriesId;
    }

    public void setConcSeriesId(Integer concSeriesId) {
	this.concSeriesId = concSeriesId;
    }

    public Boolean getExcludeDataPoint() {
	return excludeDataPoint;
    }

    public void setExcludeDataPoint(Boolean excludeDataPoint) {
	this.excludeDataPoint = excludeDataPoint;
    }

    public ArrayList<BardResultType> getPrimaryElements() {
	return primaryElements;
    }

    public void setPrimaryElements(ArrayList<BardResultType> primaryElements) {
	this.primaryElements = primaryElements;
    }

    public ArrayList<BardResultType> getChildElements() {
	return childElements;
    }

    public void setChildElements(ArrayList<BardResultType> childElements) {
	this.childElements = childElements;
    }

    public Boolean getIsActiveConc() {
	return isActiveConc;
    }

    public void setIsActiveConc(Boolean isActiveConc) {
	this.isActiveConc = isActiveConc;
    }

    public Integer getQualifierId() {
	return qualifierId;
    }

    public void setQualifierId(Integer qualifierId) {
	this.qualifierId = qualifierId;
    }

    public String getQualifierValue() {
	return qualifierValue;
    }

    public void setQualifierValue(String qualifierValue) {
	this.qualifierValue = qualifierValue;
    }

    public String getReadoutName() {
	return readoutName;
    }

    public void setReadoutName(String readoutName) {
	this.readoutName = readoutName;
    }

    public String getExtValueId() {
	return extValueId;
    }

    public void setExtValueId(String extValueId) {
	this.extValueId = extValueId;
    }

    public Double getValueMax() {
	return valueMax;
    }

    public void setValueMax(Double valueMax) {
	this.valueMax = valueMax;
    }

    public Double getValueMin() {
	return valueMin;
    }

    public void setValueMin(Double valueMin) {
	this.valueMin = valueMin;
    }

    public Integer getStatsModifierId() {
	return statsModifierId;
    }

    public void setStatsModifierId(Integer statsModifierId) {
	this.statsModifierId = statsModifierId;
    }
    
    public BardResultType getParentElement() {
        return parentElement;
    }

    public void setParentElement(BardResultType parentElement) {
        this.parentElement = parentElement;
    }

    public boolean haveChildren() {
	if((childElements != null && childElements.size() > 0))
	    return true;
	if((primaryElements != null && primaryElements.size() > 0))
	    return true;
	if(this.concResponseSeries != null)
	    return true;
	return false;	
    }
    
    
}