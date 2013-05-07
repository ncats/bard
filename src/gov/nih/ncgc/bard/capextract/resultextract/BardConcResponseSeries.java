package gov.nih.ncgc.bard.capextract.resultextract;

import java.util.ArrayList;
import java.util.Vector;
import java.util.logging.Logger;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * This class encapsulated concentration response data and it annotated to include specific fields in when converted to JSON.
 * THe standard output fields include concentration response points and fit parameters.
 * 
 * @author braistedjc
 *
 */
public class BardConcResponseSeries {
    
    @JsonIgnore
    Logger logger = Logger.getLogger(BardConcResponseSeries.class.getName());
        
    @JsonInclude(Include.NON_NULL)
    private String responseUnit;
    @JsonInclude(Include.NON_NULL)
    private String testConcUnit;
    @JsonInclude(Include.NON_NULL)
    private Integer crSeriesDictId;
    @JsonIgnore
    private BardResultType parentElement;
    @JsonIgnore
    private ArrayList <BardResultType> parameterList;
    @JsonInclude(Include.NON_NULL)
    private String readoutName;
    @JsonInclude(Include.NON_NULL)
    private HillParameters concRespParams;
    private ArrayList <BardResultType> concRespPoints;
    @JsonIgnore
    private BardResultType [] persistConcRespPointArr;
    
    @JsonInclude(Include.NON_NULL)
    private ArrayList <BardResultType> miscData;
    
    /**
     * Default Constructor
     */
    public BardConcResponseSeries() { 
	concRespParams = new HillParameters();
	parameterList = new ArrayList <BardResultType>();
	concRespPoints = new ArrayList <BardResultType>();
	miscData = new ArrayList <BardResultType>();
    }
    

    /**
     *  Makes an internal copy of all C/R points
     * @return
     */
    public ArrayList <BardResultType> cloneConcRespPoints() {
	ArrayList <BardResultType> newConcRespList = new ArrayList <BardResultType>();
	for(BardResultType point : concRespPoints)
	    newConcRespList.add(point);
	return newConcRespList;
    }
    
    /**
     * Looks for hill parameters among child data elements and builds the HillParameter class.
     * 
     * @param logXx50ParameterV List of log AC50-like measures (dictionary IDs)
     * @param xx50ParameterV List of AC50-like measures, dictionary ids
     * @throws Exception
     */
    public void reconcileParameters(Vector <Integer> logXx50ParameterV, Vector <Integer> xx50ParameterV) throws Exception {
	concRespParams = new HillParameters();
	Double value;
	String valueStr;
	if(parameterList.size() > 4) {
	    logger.warning("Potential Problem: parameterList (Hill Params) is too large, #params="+parameterList.size());
	    for(BardResultType p : parameterList) {
		System.out.println(p.getDictElemId()+"  "+p.getDisplayName());
	    }	
	}
	
	for(BardResultType param : parameterList) {
	    valueStr = param.getValue();
	    	    
	    if(valueStr == null || valueStr.trim().isEmpty())
		value = null;
	    else 
		value = Double.parseDouble(valueStr);

	    if(param.getDictElemId() == 920) {
		concRespParams.setS0(value);
	    } else if(param.getDictElemId() == 921) {
		concRespParams.setsInf(value);
	    } else if(param.getDictElemId() == 919) {
		concRespParams.setHillCoef(value);
	    } else if(logXx50ParameterV.contains(param.getDictElemId())) {
		concRespParams.setLogEc50(value);
	    }
	}
	
	//if ec50 is the parent node, then set the hill parameter by taking log(ec50) 
	
	if(parentElement.getValue() != null && concRespParams.getLogEc50() == null && xx50ParameterV.contains(parentElement.getDictElemId())) {
	   Double logEc50;
	   
	   try {	       
	       logEc50 = Double.parseDouble(parentElement.getValue());
	       if(logEc50 > 0d)
		   logEc50 = Math.log10(logEc50);
	       else
		   logEc50 = null;
	   } catch (NumberFormatException nfe) {
	       logEc50 = null;
	   }
	   concRespParams.setLogEc50(logEc50);
	}
    }

    /**
     * Initializes for display but setting extraneous fields to null.
     */
    public void initializeForDisplay() {
	String testUnit = null;
	String respUnit = null;
	Integer pointDictId = null;
	String readout = null;
	int ptCnt = 0;
	ArrayList <BardResultType> nullCRPoints = new ArrayList <BardResultType>();
	persistConcRespPointArr = new BardResultType[concRespPoints.size()];
	for(BardResultType result : concRespPoints) {
	    if(ptCnt < 1) {
		testUnit = result.getTestConcUnit();
		respUnit = (result.getResponseUnit() != null) ? result.getResponseUnit() : result.getDisplayName();
		pointDictId = result.getDictElemId();
		readout = result.getReadoutName();
	    }
	    result.setDisplayName(null);
	    result.setTestConcUnit(null);
	    result.setResponseUnit(null);
	    result.setDictElemId(null);
	    result.setReadoutName(null);
	    result.setQualifierValue(null);
	    
	    if(result.getValue() == null || result.getValue().equals("NA"))
		nullCRPoints.add(result);
	    
	    //add each cr point to the array
	    persistConcRespPointArr[ptCnt] = result;
	    ptCnt++;
	}
	
	concRespPoints.removeAll(nullCRPoints);
	
	this.responseUnit = respUnit;
	this.testConcUnit = testUnit;
	this.crSeriesDictId = pointDictId;
	this.readoutName = readout;
	
	if(miscData != null && miscData.size() == 0)
	    miscData = null;
	
	if(concRespParams != null && concRespParams.areAllParametersNull())
	    concRespParams = null;
    }
    
    /**
     * This method collapses data to minimize payload for rendering C/R series
     */
    public void prepareForDisplay() {
	//need to resptore possibly mising cr points
	concRespPoints.clear();
	for(BardResultType crPoint : persistConcRespPointArr) {
	    concRespPoints.add(crPoint);
	}
	
	ArrayList <BardResultType> nullCRPoints = new ArrayList <BardResultType>();

	if(concRespParams != null && concRespParams.areAllParametersNull())
	    concRespParams = null;
	
	if(concRespPoints != null) {
	    for(BardResultType result : concRespPoints) {	    
		if(result.getValue() == null || result.getValue().equals("NA"))
		    nullCRPoints.add(result);
	    }
	}
	concRespPoints.removeAll(nullCRPoints);
    }

    public ArrayList<BardResultType> getConcRespPoints() {
        return concRespPoints;
    }

    public void setConcRespPoints(ArrayList<BardResultType> concRespPoints) {
        this.concRespPoints = concRespPoints;
    }

    public ArrayList<BardResultType> getMiscData() {
        return miscData;
    }

    public void setMiscData(ArrayList<BardResultType> miscData) {
        this.miscData = miscData;
    }

    public String getResponseUnit() {
        return responseUnit;
    }

    public void setResponseUnit(String responseUnit) {
        this.responseUnit = responseUnit;
    }

    public String getTestConcUnit() {
        return testConcUnit;
    }

    public void setTestConcUnit(String testConcUnit) {
        this.testConcUnit = testConcUnit;
    }

    public Integer getCrSeriesDictId() {
        return crSeriesDictId;
    }

    public void setCrSeriesDictId(Integer crSeriesDictId) {
        this.crSeriesDictId = crSeriesDictId;
    }

    public BardResultType getParentElement() {
        return parentElement;
    }

    public void setParentElement(BardResultType parentNode) {
        this.parentElement = parentNode;
    }    
    
    public ArrayList<BardResultType> getParameterList() {
        return parameterList;
    }

    public void setParameterList(ArrayList<BardResultType> parameterList) {
        this.parameterList = parameterList;
    }

    public HillParameters getConcRespParams() {
        return concRespParams;
    }

    public void setConcRespParams(HillParameters concRespParams) {
        this.concRespParams = concRespParams;
    }


    /**
     * Hill Parameter Class
     * @author braistedjc
     *
     */
    private class HillParameters {

	private Double s0;
	private Double sInf;
	private Double hillCoef;
	private Double logEc50;
    
	public HillParameters() { }

	public boolean areAllParametersNull() {
	    return (s0 == null && sInf == null && hillCoef == null && logEc50 == null);
	}
	
	public Double getS0() {
	    return s0;
	}

	public void setS0(Double s0) {
	    this.s0 = s0;
	}

	public Double getsInf() {
	    return sInf;
	}

	public void setsInf(Double sInf) {
	    this.sInf = sInf;
	}

	public Double getHillCoef() {
	    return hillCoef;
	}

	public void setHillCoef(Double hillCoef) {
	    this.hillCoef = hillCoef;
	}

	public Double getLogEc50() {
	    return logEc50;
	}

	public void setLogEc50(Double logEc50) {
	    this.logEc50 = logEc50;
	}		
    }
}
