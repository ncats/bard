package gov.nih.ncgc.bard.entity;

import java.io.Serializable;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nih.ncgc.bard.tools.BARDJsonRequired;
import gov.nih.ncgc.bard.rest.BARDConstants;

public class Scaffold implements BardEntity, Serializable {
    static private final long serialVersionUID = 0xe44d3f1f7a3396efl;

    Long scaffoldId;
    Integer instances; // number of instances of this scaffold
    String smiles; // scaffold structure
    Integer acount; // atom count
    Integer bcount; // bond count
    Integer complexity; // some measure of complexity
    Integer symmetry; 
    Double snr; // signal to noise ratio
    Double apt; // average pairwise tanimoto
    Double score; // scaffold-force field
    Integer activeCount; 
    Integer activityCount;
    
    public Scaffold () {}
    public Scaffold (Long scaffoldId) { setScaffoldId (scaffoldId); }
    
    public void setScaffoldId (Long scaffoldId) { 
        this.scaffoldId = scaffoldId;
    }
    public Long getScaffoldId () { return scaffoldId; }
    
    public void setInstances (Integer instances) {
        this.instances = instances;
    }
    public Integer getInstances () { return instances; }

    public void setSmiles (String smiles) { this.smiles = smiles; }
    public String getSmiles () { return smiles; }

    public void setAtomCount (Integer acount) { this.acount = acount; }
    public Integer getAtomCount () { return acount; }

    public void setBondCount (Integer bcount) { this.bcount = bcount; }
    public Integer getBondCount () { return bcount; }

    public void setComplexity (Integer complexity) { 
        this.complexity = complexity; 
    }
    public Integer getComplexity () { return complexity; }

    public void setSymmetry (Integer symmetry) { this.symmetry = symmetry; }
    public Integer getSymmetry () { return symmetry; }

    public void setSNR (Double snr) { this.snr = snr; }
    public Double getSNR () { return snr; }

    public void setAPT (Double apt) { this.apt = apt; }
    public Double getAPT () { return apt; }

    public void setScore (Double score) { this.score = score; }
    public Double getScore () { return score; }

    public void setActiveCount (Integer activeCount) {
        this.activeCount = activeCount;
    }
    public Integer getActiveCount () { return activeCount; }

    public void setActivityCount (Integer activityCount) {
        this.activityCount = activityCount;
    }
    public Integer getActivityCount () { return activityCount; }

    public String getResourcePath() {
        return BARDConstants.API_BASE + "/scaffolds/" + scaffoldId;
    }

    // this is a dummy setter, so that Jackson is happy during deserialization
    public void setResourcePath(String resourcePath) {
    }
}
