package gov.nih.ncgc.bard.capextract.resultextract;

import java.util.ArrayList;

/**
 * This class represents the base experiment result from CAP data export API.
 * The result contains perhaps multiple CAPResultMeasures.
 * 
 * @author braistedjc
 *
 */
public class CAPExperimentResult {

    private Long sid;    
    private ArrayList <CAPResultMeasure> rootElem;

    public CAPExperimentResult() { }
    
    public Long getSid() {
        return sid;
    }
    
    public void setSid(Long sid) {
        this.sid = sid;
    }

    public ArrayList<CAPResultMeasure> getRootElem() {
        return rootElem;
    }

    public void setRootElem(ArrayList<CAPResultMeasure> rootElem) {
        this.rootElem = rootElem;
    }
    
}
