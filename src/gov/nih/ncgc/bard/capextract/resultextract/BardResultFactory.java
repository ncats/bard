package gov.nih.ncgc.bard.capextract.resultextract;

import gov.nih.ncgc.bard.capextract.jaxb.ContextItemType;
import gov.nih.ncgc.bard.capextract.jaxb.ContextType;
import gov.nih.ncgc.bard.capextract.jaxb.ContextType.ContextItems;
import gov.nih.ncgc.bard.capextract.jaxb.Contexts;
import gov.nih.ncgc.bard.capextract.jaxb.Link;
import gov.nih.ncgc.bard.pcparser.Constants;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Vector;

/**
 * This class builds BardResultType starting with a variety of input formats.
 * CapResultCapsule being one type to convert.
 * 
 * The initialize method is to prepare for an experiment load.
 * Iterative calls to processCapResult return a BardExptDataResponse for each 
 * CapResult (collection of results for a tested substance)
 * 
 * @author braistedjc
 *
 */
public class BardResultFactory {
    
    // Element categories, these help to identify key element ids
    private Vector <Integer> highPriorityDataElemV;
    private Vector <Integer> lowPriorityDataElemV;
    private Vector <Integer> xx50DataElemV;
    private Vector <Integer> curveFitParameterElemV;
    private Vector <Integer> logXx50ParameterElemV;
    private Vector <Integer> efficacyDataElemV;

    // The returned response object, class level to prevent building multiple references.
    private BardExptDataResponse response;
    private BardResultType tempBardResult;
    // a utility list of BardResultType objects that help to 
    // fully traverse without having to cover hierarchy
    private ArrayList <BardResultType> resultList;

    private long processCnt;
    private String dummyStr;

    // Some experiment contexts capture information about the measures. e.g. single point test concentration is global.
    private Contexts exptContexts;

    private String exptConcUnit = null;
    
    private Integer attrId;
    private boolean haveConcAttr;

    /**
     * Default Constructor
     */
    public BardResultFactory() {
	initPriorityVectors();
    }
    
    /*
     * Collects vector elements from Constants class. These help to identify key result types.
     * Heuristics are require to identify the result format (SP, CR)
     */
    private void initPriorityVectors() {
	highPriorityDataElemV = new Vector <Integer>();
	for(Integer elem : Constants.HIGH_PRIORITY_DICT_ELEM) {
	    highPriorityDataElemV.add(elem);
	}
	
	lowPriorityDataElemV = new Vector <Integer>();	
	for(Integer elem : Constants.LOW_PRIORITY_DATA_ELEM) {
	    lowPriorityDataElemV.add(elem);
	}
	xx50DataElemV = new Vector <Integer>();
	for(Integer elem : Constants.XX50_DICT_ELEM) {
	    xx50DataElemV.add(elem);
	}
	curveFitParameterElemV = new Vector <Integer>();
	for(Integer elem : Constants.FIT_PARAM_DICT_ELEM) {
	    curveFitParameterElemV.add(elem);
	}
	logXx50ParameterElemV = new Vector <Integer>();
	for(Integer elem : Constants.LOG_XX50_DICT_ELEM) {
	    logXx50ParameterElemV.add(elem);
	}
	efficacyDataElemV = new Vector <Integer>();
	for(Integer elem : Constants.EFFICACY_PERCENT_MEASURES) {
	    efficacyDataElemV.add(elem);
	}
    }
    
    
    public void initialize(Long bardExptId, Long capExptId, Long bardAssayId, Long capAssayId,
	    ArrayList <ArrayList<Long>> projectIdList, Contexts contexts, Integer responseType,
	    Double exptScreeningConc, String exptScreeningConcUnit) {
	
	response = new BardExptDataResponse();
	response.setResponseType(responseType);
	
	response.setBardExptId(bardExptId);
	response.setBardAssayId(bardAssayId);
	response.setCapExptId(capExptId);
	response.setCapAssayId(capAssayId);
	
	response.setExptConcUnit(exptScreeningConcUnit);
	response.setExptScreeningConc(exptScreeningConc);
	
	for (ArrayList<Long> projIds :projectIdList) {
	    if(projIds.size() == 2) {
		response.addProjectPair(projIds.get(0), projIds.get(1));
	    } else {
		response.addProjectPair(projIds.get(0), null);
	    }
	}
	
	
	
	resultList = new ArrayList <BardResultType>();
	
	//provide context for the experiment
	exptContexts = contexts;
	
	processCnt = 0;		
    }
 

    
    public BardExptDataResponse processCapResult(CAPExperimentResult result) {
	//clear the result data
	response.getPriorityElements().clear();
	response.getRootElements().clear();
	//clear the result list
	resultList.clear();
	
	//the primary ids in the response are constant, no change there

	//set the sid for the response
	response.setSid(result.getSid());
	
	//build the basic response structure, note that this also builds the simple list of results 
	buildBasicResponseFromCapResult(result);
	
	// Typically the result type is determined during initialization
	// If it's set to UNDEF, it signals that we should determine it on the first response
	if(processCnt == 0 && this.response.getResponseType() == BardExptDataResponse.ResponseClass.UNDEF.ordinal()) {
	    evaluateResponseType();
	}
	
	//refine the structure in case of CR_SER, collapse to c/r objecs
	//this iterates over list to find cr root nodes, then collapses data nodes to cr objectx
	if(response.getResponseType() == 1) {
	    createConcResponseObjects();
	}
	
	//separate priority elements
	ArrayList <BardResultType> priElems = new ArrayList <BardResultType>();
	for(BardResultType resultElem : response.getRootElements()) {
	    if(resultElem.getDictElemId() != null) { 
		    if(this.highPriorityDataElemV.contains(resultElem.getDictElemId())) {
			priElems.add(resultElem);
		    } else if(response.getResponseType() == 0
			    && this.efficacyDataElemV.contains(resultElem.getDictElemId())) {
			priElems.add(resultElem);
		    }
	    }
	}
	
	response.getPriorityElements().addAll(priElems);
	response.getRootElements().removeAll(priElems);
	
	//set core result values outcome, score, and potency
	setCoreResultValues();
	
	processCnt++;
	
	return response;
    }
    
    public BardExptDataResponse buildBasicResponseFromCapResult(CAPExperimentResult capResult) {
		
	response.setSid(capResult.getSid());
	
	BardResultType bardResult;
	//create root elements
	for(CAPResultMeasure capsule : capResult.getRootElem()) {
	    bardResult = buildResultTypeFromCapResultCapsule(capsule);
	    //add to simple result list
	    resultList.add(bardResult);
	    //add as a root element in the response
	    response.addRootElement(bardResult);
	    //traverse to add children
	    processChildren(capsule, bardResult);
	}
	
	return response;	
    }
    
    /*
     * Recursive method to process children
     */
    private void processChildren(CAPResultMeasure capResult, BardResultType bardResult) {
	for(CAPResultMeasure child : capResult.getRelated()) {
	    tempBardResult = buildResultTypeFromCapResultCapsule(child);
	    //add to the basic result list
	    resultList.add(tempBardResult);
	    //add the child to the parent
	    bardResult.addChildResult(tempBardResult);
	    //process the children of the child if any
	    processChildren(child, tempBardResult);
	}
	
	for(CAPMeasureContextItem item : capResult.getContextItems()) {
	    processContextItem(item, bardResult);
	}
    }
    
    private void processContextItem(CAPMeasureContextItem item, BardResultType bardResult) {
	//context items either lead to children OR if 971's we have to grab the test conc.
	attrId = item.getAttributeId();
	haveConcAttr = false;
	//if it's a concentration attribute, add the value and base unit as fields of the result
	//rather than children.
	if(attrId != null) {
	    if(attrId == 971) {
		bardResult.setTestConc(item.getValueNum()); 
		bardResult.setTestConcUnit("uM");
		haveConcAttr = true;
	    } else if(attrId == 1950) {
		bardResult.setTestConc(item.getValueNum()); 
		bardResult.setTestConcUnit("mg/ml");
		haveConcAttr = true;
	    } else if(attrId == 1949) {
		bardResult.setTestConc(item.getValueNum()); 
		bardResult.setTestConcUnit("% (vol.)");
		haveConcAttr = true;
	    } else if(attrId == 1948) {
		bardResult.setTestConc(item.getValueNum()); 
		bardResult.setTestConcUnit("% (mass)");
		haveConcAttr = true;
	    } else if(attrId == 1943) {
		bardResult.setTestConc(item.getValueNum()); 
		haveConcAttr = true;
	    }	 	    
	} 
	//if it's not a concentration, add it as a child
	if(!haveConcAttr) {
	    bardResult.addChildResult(buildResultTypeFromContextItem(item));
	}
    }
    
    private BardResultType buildResultTypeFromContextItem(CAPMeasureContextItem contextItem) {
	BardResultType bardResult = new BardResultType();
	bardResult.setDictElemId(contextItem.getAttributeId());
	bardResult.setDisplayName(contextItem.getAttribute());
	bardResult.setValue((contextItem.getValueNum() != null) ? contextItem.getValueNum().toString() : contextItem.getValueDisplay());
	bardResult.setDictElemId(contextItem.getAttributeId());
	
	return bardResult;
    }
    
    private BardResultType buildResultTypeFromCapResultCapsule(CAPResultMeasure capResult) {	
	BardResultType result = new BardResultType();	
	//result type ID, dictionary id
	result.setDictElemId(capResult.getResultTypeId());
	//result type, dictionary label from cap
	result.setDisplayName(capResult.getResultType());
	//set value as the numeric value, or if numeric value is null, set to display value 
	result.setValue((capResult.getValueNum() != null) ? Double.toString(capResult.getValueNum()) : capResult.getValueDisplay());
	//take care of qualifer
	dummyStr = capResult.getQualifier();
	if(dummyStr != null && !dummyStr.equals("="))
	    result.setQualifierValue(capResult.getQualifier());	
	return result;
    }
    
    
    private void evaluateResponseType() {
	//check concentration response
	boolean haveType = false;
	boolean haveXX50 = false;
	HashSet <Double> concentrations = new HashSet <Double>();
	for(BardResultType result : resultList) {
	    if(haveConcResponse(result)) {
		//have a series, is the series in a root element
		//could check but the root might be a mean XX50 measurement
		response.setResponseType(BardExptDataResponse.ResponseClass.CR_SER.ordinal());		
		haveType = true;
		if(result.getTestConc() != null)
		    concentrations.add(result.getTestConc());
	    }
	}
	
	//concentrations array should have test concentrations
	//if not * check for experiment context for screening concentration(s)
	Double concCnt = -1d;
	if(concentrations.size() == 0) {
	    concCnt = resolveConcFromExperimentContext(concentrations);
	}
	
	//check for single point, one test concentration, have efficacy
	if(!haveType) {
	    if(concentrations.size() == 1 || (concCnt !=null && concCnt == 1)) {
		//check for efficacy in root elements
		for(BardResultType result : response.getRootElements()) {
		    if(result.getDictElemId() != null && this.efficacyDataElemV.contains(result.getDictElemId())) {

			//don't set test conc, just report global expt values
//			if(result.getTestConc() == null && ) {
//			    result.setTestConc(response.getExptScreeningConc());
//			    result.setTestConcUnit(response.getExptConcUnit());
//			}
			response.setResponseType(BardExptDataResponse.ResponseClass.SP.ordinal());
			haveType = true;
		    }
		}		
	    } else if(concentrations.size() > 1 || concentrations.size() == 0) {
		//have more than one concentration but no structure 
		//the ec50 doesn't have concentration points as children
		//or we have multiple concentrations but no ec50
		
		//if we have an AC50 we have CR_NO_SER, going to permit no concentrations
		//have XX50
		for(BardResultType result : response.getRootElements()) {
		    if(result.getDictElemId() != null && this.xx50DataElemV.contains(result.getDictElemId())) {
			response.setResponseType(BardExptDataResponse.ResponseClass.CR_NO_SER.ordinal());
			haveType = true;
			haveXX50 = true;
		    }
		}
		
		//have multiple concentrations but tno AC50 in root, type = MULTCONC
		if(concentrations.size() > 0 && !haveType && !haveXX50) {
		    response.setResponseType(BardExptDataResponse.ResponseClass.MULCONC.ordinal());
		    haveType = true;
		}
		
		if(!haveType) {
		  //fall through type = UNCLASS (2)
		    response.setResponseType(BardExptDataResponse.ResponseClass.UNCLASS.ordinal());
		}
		
	    } else {
		//fall through type = UNCLASS (2)
		response.setResponseType(BardExptDataResponse.ResponseClass.UNCLASS.ordinal());
	    }
	}
    }
    
    
    /*
     * Deeply nested info... single point results tend to have the single concentration value
     * set at the experiment level.
     * 
     * Even if we can't find a concentration, we look for a 650 element which is a concentration count.
     */
    private Double resolveConcFromExperimentContext(HashSet <Double> concentrations) {
	Double concCnt = null;
	Link link = null;
	String href;
	Integer linkId;
	Integer concLinkId;
	String concUnit = null;
	//try to add the concentration for this one
	if(exptContexts != null)  {
	    for(ContextType item : exptContexts.getContext()) {
		ContextItems ci = item.getContextItems();
		if(ci != null) {
		    for(ContextItemType type : ci.getContextItem()) {
			if(type.getAttributeId().getLabel().contains("screening concentration")) {
			    if(type.getValueNum() != null) {
				concentrations.add(type.getValueNum());
				link = type.getAttributeId().getLink();
				if(link != null) {
				    href = link.getHref();
				    concLinkId = this.getLinkId(href);
				    // 1950 = W/V (mg/ml)
				    // 971 = molar (uM)
				    // 1948 = %/mass (%)
				    // 1949 = %/vol (%)
				    // 1943 = screening conc no units known
				    if(concLinkId != null) {
					if(concLinkId == 971) {
					    concUnit = "uM";
					} else if(concLinkId == 1950) {
					    concUnit = "mg/ml";
					} else if(concLinkId == 1949) {
					    concUnit = "% (vol.)";
					} else if(concLinkId == 1948) {
					    concUnit = "% (mass)";
					} else if(concLinkId == 1943) {
					    concUnit = "Unspecified";
					}					
				    }
				}
			    }			    
			} else if(concCnt == null) {
			    link = type.getAttributeId().getLink();
			    if(link != null) {
				href = link.getHref();
				if(href != null) {
				    linkId = getLinkId(href);
				    if(linkId != null) {
					if(linkId == 650) {
					    concCnt = type.getValueNum();
					}
				    }
				}
			    }
			}
		    }
		}
	    }
	}
	
	//if there is one screening concentration, set the experiment level info
	if(concentrations.size() == 1) {
	    response.setExptScreeningConc(concentrations.iterator().next());
	    response.setExptConcUnit(concUnit);
	}
	
	return concCnt;
    }
    
    private Integer getLinkId(String href) {
	if(href == null) {
	    return null;
	}
	Integer id = null;
	String [] toks = href.split("/");
	String idStr;
	if(toks.length > 2) {
	    try {
		id = Integer.parseInt(toks[toks.length-1]);
	    } catch (NumberFormatException nfe) {
		return null;
	    }
	}	
	return id;
    }
    
    /*
     * Checks for C/R
     */
    private boolean haveConcResponse(BardResultType bardResultType) {
	Integer dictElemId = -1;

	if(bardResultType.getChildElements() == null || bardResultType.getChildElements().size() < 1)
	    return false;
	
	dictElemId = bardResultType.getDictElemId();
	
	//special case, activity response elements with children are mean values of their child elements
	//refine to use mean modifier.
	if(dictElemId != null && (dictElemId == 986 || dictElemId == 982 ||bardResultType.getTestConc() != null))
		return false;
	
	dictElemId = -1;
	boolean haveActivityMeasure = false;
	HashSet <Double> concentrations = new HashSet <Double>();
	for(BardResultType result : bardResultType.getChildElements()) {
	    dictElemId = result.getDictElemId();

	    if(dictElemId != null && (dictElemId == 986 || dictElemId == 982 || result.getTestConc() != null)) {
		haveActivityMeasure = true;
		if(result.getTestConc() != null) {
		    concentrations.add(result.getTestConc());
		}
	    }	    
	}
	return (haveActivityMeasure && concentrations.size() > 1);
    }
    
    
    private void createConcResponseObjects() {
	ArrayList <BardResultType> crPointsList;
	ArrayList <BardResultType> parameterList;
	for(BardResultType result : resultList) {
	    if(haveConcResponse(result)) {
		//create new lists 
		crPointsList = new ArrayList <BardResultType>();
		parameterList = new ArrayList <BardResultType>();
		
		//have a node, create a new series object
		BardConcResponseSeries series = new BardConcResponseSeries();
		
		//set the series for the parent result
		result.setConcResponseSeries(series);
		series.setParentElement(result);
		for(BardResultType child : result.getChildElements()) {
		    if(child.getTestConc() != null) {
			crPointsList.add(child);
		    } else if(child.getDictElemId() != null 
			    && this.curveFitParameterElemV.contains(child.getDictElemId())) {
			parameterList.add(child);
		    }		    
		}
		
		//set the lists
		series.setConcRespPoints(crPointsList);
		series.setParameterList(parameterList);
		
		//remove these lists from child array
		result.getChildElements().removeAll(crPointsList);
		result.getChildElements().removeAll(parameterList);
		
		//initialize the values for display
		series.initializeForDisplay();	   
		
		//get the parameters
		try {
		    series.reconcileParameters(logXx50ParameterElemV, xx50DataElemV);
		} catch (Exception e) {
		    e.printStackTrace();
		}		
	    }
	}
    }
    
    /*
     * Sets core values like score, outomce, and potency (when available)
     */
    private void setCoreResultValues() {
	String outcome;
	int outcomeIndex = -1;
	Double score;
	Integer dictId;

	boolean haveOutcome = false;
	boolean havePotency = false;
	boolean haveScore = false;

	response.setOutcome(null);
	response.setPotency(null);
	response.setScore(null);
	
	for(int i = 0; i < resultList.size() && !(haveOutcome && havePotency && haveScore); i++) {
	    tempBardResult = resultList.get(i);
	    
	    dictId = tempBardResult.getDictElemId();

	    if(dictId != null) {		
		if(!havePotency && xx50DataElemV.contains(tempBardResult.getDictElemId())) {
		    havePotency = true;
		    try {
			if(tempBardResult.getValue() != null) {
			    response.setPotency(Double.parseDouble(tempBardResult.getValue()));
			}
		    } catch (NumberFormatException nfe) {
			response.setPotency(null);		    
		    }		
		} else if(dictId == 896) {
		    haveOutcome = true;
		    outcome = tempBardResult.getValue();
		    
		    if(outcome.equalsIgnoreCase("Inactive"))
			outcomeIndex = 1;
		    else if(outcome.equalsIgnoreCase("Active"))
			outcomeIndex = 2;
		    else if(outcome.equalsIgnoreCase("Inconclusive"))
			outcomeIndex = 3;
		    else if(outcome.equalsIgnoreCase("Unspecified"))    
			outcomeIndex = 4;
		    else if(outcome.equalsIgnoreCase("Probe"))
			outcomeIndex = 5;
		    else {
			//fall through
			try {
			    outcomeIndex = 6;
			    //see if this works
			    outcomeIndex = (int)(Double.parseDouble(outcome));				
			} catch (NumberFormatException nfe) {
			    continue; //quietly
			}		    
		    }
		    response.setOutcome(outcomeIndex);
		} else if(dictId == 898) {
		    haveScore = true;
		    try {
			score = Double.parseDouble(tempBardResult.getValue());
			response.setScore(score);
		    } catch (NumberFormatException nfe) {
			response.setScore(null);
		    }			    
		}		
	    }
	}
    }

    public ArrayList<BardResultType> getResultList() {
        return resultList;
    }   

}
