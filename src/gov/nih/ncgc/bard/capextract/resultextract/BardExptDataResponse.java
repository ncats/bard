package gov.nih.ncgc.bard.capextract.resultextract;

import java.util.ArrayList;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * This class is used to format the bard experiment data response JSON
 * 
 * @author braistedjc
 *
 */
public class BardExptDataResponse {
    
    public enum ResponseClass {
	SP,
	CR_SER,
	UNCLASS,
	MULCONC,
	CR_NO_SER,
	UNDEF;
    }
    
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
    
    //These are used to collect these values for fast retrieval
    private Double potency;
    @JsonIgnore
    private Double score;
    @JsonIgnore
    private Integer outcome;
    //conc unit for experiment
    @JsonInclude(Include.NON_NULL)
    private Double exptScreeningConc;
    @JsonInclude(Include.NON_NULL)
    private String exptConcUnit;
    
    //Result sets for priority and other root elements, project ids
    private ArrayList <BardResultType> priorityElements;
    private ArrayList <BardResultType> rootElements;
    private ArrayList <ProjectIdPair> projects;
    
        
    /**
     * Default constructor
     */
    public BardExptDataResponse() {	
	responseType = new Integer(ResponseClass.UNCLASS.ordinal());
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
	ResponseClass [] types = ResponseClass.values();	
	this.responseClass = types[responseType].name();
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

    public Double getExptScreeningConc() {
        return exptScreeningConc;
    }

    public void setExptScreeningConc(Double exptScreeningConc) {
        this.exptScreeningConc = exptScreeningConc;
    }

    public String getExptConcUnit() {
        return exptConcUnit;
    }

    public void setExptConcUnit(String exptConcUnit) {
        this.exptConcUnit = exptConcUnit;
    }

    public void addProjectPair(Long bardProjId, Long capProjId) {
	ProjectIdPair pp = new ProjectIdPair();
	pp.setBardProjId(bardProjId);
	pp.setCapProjId(capProjId);
	this.projects.add(pp);
    }

    /**
     * holds a bard id and an external project id (specifically cap project, for now)
     * 
     * @author braistedjc
     */
    //Note on reconstructing from JSON, it Jackson ObjectMapper required that the inner class be
    //declared as 'static'. If the class was external I think it would be fine.
    //I need to find out why this is the case.
    public static class ProjectIdPair {
	
	private Long bardProjId;
	private Long capProjId;
	
	public ProjectIdPair() { 
	    
	}
	
	public ProjectIdPair(Long bardProjId, Long capProjId) {
	    this.bardProjId = bardProjId;
	    this.capProjId = capProjId;
	}
	
	public ProjectIdPair(long bardProjId, long capProjId) {
	    this.bardProjId = bardProjId;
	    this.capProjId = capProjId;
	}
	
	public ProjectIdPair(String bardProjId, String capProjId) {
	    this.bardProjId = Long.parseLong(bardProjId);
	    this.capProjId = Long.parseLong(capProjId);
	}
	
	public ProjectIdPair(int bardProjId, int capProjId) {
	    this.bardProjId = Long.valueOf(bardProjId);
	    this.capProjId = Long.valueOf(capProjId);
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
