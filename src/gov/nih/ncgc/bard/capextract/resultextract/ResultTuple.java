package gov.nih.ncgc.bard.capextract.resultextract;

public class ResultTuple {

    private Integer dictId;
    private boolean atRoot;
    private Integer parentDictId;
    private Integer statsModifierId;

    public ResultTuple() {	}

    public ResultTuple(Integer dictId, Integer parentDictId, Integer statsModifierId, boolean atRoot) {
	this.dictId = dictId;
	this.parentDictId = parentDictId;
	this.statsModifierId = statsModifierId;	 
	this.atRoot = atRoot;
    }

    //performs tuple comparison to check equality. In practice, this is used to find priority elements
    //in CAP JSON based on CAP experiment XML experimentMeasure hierarchy
    public boolean equalsResultType(BardResultType result) {

	//first check that the root status of the two elements matches , both at root or both nested
	if((atRoot && result.getParentElement() != null)
		|| (!atRoot && result.getParentElement() == null)) {
	    return false;
	}
	
	if(atRoot) {
	    //now check parity only based on dict id and stats modifier, no parent for parent dict id test
	    if(dictId.equals(result.getDictElemId())) {
		//dict id matches, check stats modifier
		if((statsModifierId == null && result.getStatsModifierId() == null)
			|| (statsModifierId.equals(result.getStatsModifierId()))) {
		    return true;
		} 
	    }
	    
	    //both at root but fall through because of dict id or stats mod not matching		
	} else {
	    //we're not at the root so we can check all fields
	    if(parentDictId.equals(result.getParentElement().getDictElemId()) 
		    && dictId.equals(result.getDictElemId())) {
		//parent matches and dictionary id matches, now just check the stats modifier
		if((statsModifierId == null && result.getStatsModifierId() == null)
			|| (statsModifierId.equals(result.getStatsModifierId()))) {
		    return true;
		} 
	    }
	    //neither at root but fall through because of parent dict id, dict id, or stats modifier
	}	        
   
	return false;
    }


    public Integer getDictId() {
	return dictId;
    }	
    public void setDictId(Integer dictId) {
	this.dictId = dictId;
    }
    public Integer getParentDictId() {
	return parentDictId;
    }
    public void setParentDictId(Integer parentDictId) {
	this.parentDictId = parentDictId;
    }
    public Integer getStatsModifier() {
	return statsModifierId;
    }
    public void setStatsModifier(Integer statsModifier) {
	this.statsModifierId = statsModifier;
    }
    public boolean isAtRoot() {
	return atRoot;
    }
    public void setAtRoot(boolean atRoot) {
	this.atRoot = atRoot;
    }
    
    public String toString() {
	return "ResultTuple: ("+dictId+", "+statsModifierId+", "+atRoot+", "+parentDictId+")"; 
    }
}
