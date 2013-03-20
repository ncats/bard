package gov.nih.ncgc.bard.capextract.resultextract;

import java.util.ArrayList;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * This class is used to format the bard experiment data response JSON
 * 
 * @author braistedjc
 *
 */
public class BardExptDataResponse {
    
    @JsonIgnore
    private Integer responseType;
    private String responseClass;

    //experiment ids
    private Long bardExptId;    
    private Long capExptId;
    
    //assay ids
    private Long bardAssayId;
    private Long capAssayId;
    
    //substance and compound ids
    private Long sid;
    private Long cid;
    
    //Result sets for priority and other root elements, project ids
    private ArrayList <BardResultType> priorityElements;
    private ArrayList <BardResultType> rootElements;
    private ArrayList <ProjectIdPair> projects;
    
    //These are used to collect these values for fast retrieval
    @JsonIgnore
    private Double potency;
    @JsonIgnore
    private Double score;
    @JsonIgnore
    private Integer outcome;
        
    /**
     * Default constructor
     */
    public BardExptDataResponse() {
	responseType = new Integer(2);
	priorityElements = new ArrayList<BardResultType>();
	rootElements = new ArrayList<BardResultType>();
	projects = new ArrayList<ProjectIdPair>();
    }

    /**
     * Adds a priority element
     * @param resultType a priority result type
     */
    public void addPriorityElement(BardResultType resultType) {
	priorityElements.add(resultType);
    }
    
    /**
     * Adds a root element
     * @param resultType a root result type
     */
    public void addRootElement(BardResultType resultType) {
	rootElements.add(resultType);
    }
    
    public ArrayList<BardResultType> getPriorityElements() {
        return priorityElements;
    }

    public void setPriorityElements(ArrayList<BardResultType> priorityElements) {
        this.priorityElements = priorityElements;
    }

    public ArrayList<BardResultType> getRootElements() {
        return rootElements;
    }

    public void setRootElements(ArrayList<BardResultType> rootElements) {
        this.rootElements = rootElements;
    }

    public Long getBardExptId() {
        return bardExptId;
    }

    public void setBardExptId(Long bardExptId) {
        this.bardExptId = bardExptId;
    }
    
    public Long getCapExptId() {
        return capExptId;
    }

    public void setCapExptId(Long capExptId) {
        this.capExptId = capExptId;
    }

    public Long getBardAssayId() {
        return bardAssayId;
    }

    public void setBardAssayId(Long bardAssayId) {
        this.bardAssayId = bardAssayId;
    }

    public Long getCapAssayId() {
        return capAssayId;
    }

    public void setCapAssayId(Long capAssayId) {
        this.capAssayId = capAssayId;
    }

    public Long getSid() {
        return sid;
    }

    public void setSid(Long sid) {
        this.sid = sid;
    }

    public Long getCid() {
        return cid;
    }

    public void setCid(Long cid) {
        this.cid = cid;
    }

    public Integer getResponseType() {
        return responseType;
    }

    /**
     * Sets the response class. 0-SP, 1-CR_SER, 2-UNCLASS, 3-MULTCONC, 4-CR_NO_SER
     * @param responseType
     */
    public void setResponseType(Integer responseType) {
	if(responseType == 0) {
	    this.responseClass = "SP";
	} else if(responseType == 1) {
	    this.responseClass = "CR_SER";	    
	} else if(responseType == 2) {
	    this.responseClass = "UNCLASS";
	} else if(responseType == 3) {
	    this.responseClass = "MULTCONC";
	} else if(responseType == 4) {
	    this.responseClass = "CR_NO_SER";
	}
        this.responseType = responseType;
    }

    public String getResponseClass() {
        return responseClass;
    }

    public void setResponseClass(String responseClass) {
        this.responseClass = responseClass;
    }
        
    public ArrayList<ProjectIdPair> getProjects() {
        return projects;
    }

    public void setProjects(ArrayList<ProjectIdPair> projects) {
        this.projects = projects;
    }    
    
    public Double getPotency() {
        return potency;
    }

    public void setPotency(Double potency) {
        this.potency = potency;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public Integer getOutcome() {
        return outcome;
    }

    public void setOutcome(Integer outcome) {
        this.outcome = outcome;
    }

    public void addProjectPair(Long bardProjId, Long capProjId) {
	this.projects.add(new ProjectIdPair(bardProjId, capProjId));
    }

    /**
     * holds a bard id and an external project id (specifically cap project, for now)
     * 
     * @author braistedjc
     */
    public class ProjectIdPair {
	
	private Long bardProjId;
	private Long capProjId;
	
	public ProjectIdPair() { }
	
	public ProjectIdPair(Long bardProjId, Long capProjId) {
	    this.bardProjId = bardProjId;
	    this.capProjId = capProjId;
	}
	
	public Long getBardProjId() {
	    return bardProjId;
	}
	public void setBardProjId(Long bardProjId) {
	    this.bardProjId = bardProjId;
	}
	public Long getCapProjId() {
	    return capProjId;
	}
	public void setCapProjId(Long capProjId) {
	    this.capProjId = capProjId;
	}	
    }
}
