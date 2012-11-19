package gov.nih.ncgc.bard.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import gov.nih.ncgc.bard.rest.BARDConstants;
import gov.nih.ncgc.bard.rest.rowdef.AssayDefinitionObject;
import gov.nih.ncgc.bard.rest.rowdef.DataResultObject;
import gov.nih.ncgc.bard.rest.rowdef.DoseResponseResultObject;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A representation of experiment data (ie measurements).
 *
 * @author Rajarshi Guha
 */
public class ExperimentData implements BardEntity {
    String exptDataId;
    Long eid, cid, sid, bardExptId;

    @JsonIgnore
    Date updated;

    String runset = "default";
    int outcome, score;
    Integer classification;
    Float potency;

    List<FitModel> readouts;

    /**
     * Reperesents the context of <code>bard_experiment_data.json_data_array</code>.
     */
    @JsonIgnore
    DataResultObject[] results;

    /**
     * Reperesents the context of <code>bard_experiment_data.json_dose_response</code>.
     */
    @JsonIgnore
    DoseResponseResultObject[] dr = null;
    @JsonIgnore
    AssayDefinitionObject[] ado;

    public ExperimentData() {
    }

    /**
     * Convert the internal representation to a custom form, suitable for JSON output.
     */
    public void transform() {
        readouts = new ArrayList<FitModel>();
        if (dr != null) { // we have one or CRC layers
            for (DoseResponseResultObject dro : dr) {

                FitModel model = new FitModel("dose response", dro.getZeroAct(), dr[0].getInfAct(), dr[0].getHillCoef(), dr[0].getAc50());
                Double[][] cr = new Double[dr[0].getDose().length][2];
                for (int i = 0; i < dr[0].getDose().length; i++) {
                    cr[i][0] = dr[0].getDose()[i];
                    cr[i][1] = dr[0].getResponse()[i];
                }
                model.setCr(cr);
                model.setName(dro.getLabel());
                model.setDescription(dro.getDescription());
                model.setConcUnit(dro.getConcUnit());

                if (model.unfitted()) {
                    model.setS0(null);
                    model.setAc50(null);
                    model.setsInf(null);
                    model.setHill(null);
                }
                readouts.add(model);
            }

        } else {
            // probably a single point
            for (DataResultObject o : results) {

                // we're going to force all non-PubChem TID's into individual readouts.
                // ideally we should recognize relevant readouts, but that's not happening soon
                if (o.getTid() == -1) continue;

                FitModel model = new FitModel();
                model.setDescription("single point");
                model.setName(o.getResultName());
                Double[][] cr = new Double[1][2];
                cr[0][0] = null;
                cr[0][1] = null;

                String val = (String) o.getValue();
                Double dval = null;
                try {
                    if (val != null) {
                        cr[0][1] = Double.parseDouble(val);
                        String[] concAndUnit = getConcAndUnitsFromTitle(o.getResultName());
                        if (concAndUnit[0] != null) {
                            cr[0][0] = Double.parseDouble(concAndUnit[0]);
                            model.setConcUnit(concAndUnit[1]);
                        } else { // try pulling it from the result type definition
                            for (AssayDefinitionObject ao : ado) {
                                if (o.getTid() == Integer.parseInt(ao.getTid())) {
                                    if (ao.getTestConcUnit() != null && !ao.getTestConcUnit().equals("none"))
                                        model.setConcUnit(ao.getTestConcUnit());
                                    if (ao.getTestConcentration() != null && ao.getTestConcentration().length > 0) {
                                        cr[0][0] = (double) ao.getTestConcentration()[0];
                                    }
                                }
                            }
                        }
                    }
                } catch (NumberFormatException e) {

                }

                model.setCr(cr);
                readouts.add(model);
            }
        }
    }

    String[] getConcAndUnitsFromTitle(String title) {
        String conc = null, unit = null;

        Pattern uM = Pattern.compile("(\\d+\\.?\\d+?\\s?)uM");
        Pattern nM = Pattern.compile("(\\d+\\.?\\d+?\\s?)nM");
        Pattern M = Pattern.compile("(\\d+\\.?\\d+?\\s?)M");

        Matcher matcher = uM.matcher(title);
        if (matcher.find()) {
            conc = matcher.group(1);
            unit = "uM";
        }

        if (conc == null) {
            matcher = nM.matcher(title);
            if (matcher.find()) {
                conc = matcher.group(1);
                unit = "nM";
            }
        }

        if (conc == null) {
            matcher = M.matcher(title);
            if (matcher.find()) {
                conc = matcher.group(1);
                unit = "M";
            }
        }
        return new String[]{conc, unit};
    }


    public DoseResponseResultObject[] getDr() {
        return dr;
    }

    public void setDr(DoseResponseResultObject[] dr) {
        this.dr = dr;
    }

    public List<FitModel> getReadouts() {
        return readouts;
    }

    public void setReadouts(List<FitModel> readouts) {
        this.readouts = readouts;
    }

    /**
     * Reperesents the context of <code>bard_experiment_data.json_data_array</code>.
     *
     * @return
     */
    public DataResultObject[] getResults() {
        return results;
    }

    public void setResults(DataResultObject[] results) {
        this.results = results;
    }

    @JsonIgnore
    public AssayDefinitionObject[] getDefs() {
        return ado;
    }

    @JsonIgnore
    public void setDefs(AssayDefinitionObject[] ado) {
        this.ado = ado;
    }

    public int getOutcome() {
        return outcome;
    }

    public void setOutcome(int outcome) {
        this.outcome = outcome;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public Float getPotency() {
        return potency;
    }

    public void setPotency(Float potency) {
        this.potency = potency;
    }

    public String getExptDataId() {
        return exptDataId;
    }

    public void setExptDataId(String exptDataId) {
        this.exptDataId = exptDataId;
        if (exptDataId != null && exptDataId.contains(".")) {
            String[] toks = exptDataId.split("\\.");
            if (toks.length == 2) {
                bardExptId = Long.parseLong(toks[0]);
                sid = Long.parseLong(toks[1]);
            }
        }
    }

    public Long getEid() {
        return eid;
    }

    public void setEid(Long eid) {
        this.eid = eid;
    }

    public Long getBardExptId() {
        return bardExptId;
    }

    public void setBardExptId(Long bardExptId) {
        this.bardExptId = bardExptId;
    }

    public Long getCid() {
        return cid;
    }

    public void setCid(Long cid) {
        this.cid = cid;
    }

    public Long getSid() {
        return sid;
    }

    public void setSid(Long sid) {
        this.sid = sid;
    }

    public Integer getClassification() {
        return classification;
    }

    public void setClassification(Integer classification) {
        this.classification = classification;
    }

    public Date getUpdated() {
        return updated;
    }

    public void setUpdated(Date updated) {
        this.updated = updated;
    }

    public String getRunset() {
        return runset;
    }

    public void setRunset(String runset) {
        this.runset = runset;
    }

    public String toString() {
        return "ExperimentData[" + exptDataId + ", outcome=" + outcome + ", score=" + score + ", potency=" + potency + "]";
    }

    /**
     * Return the path for this resource in the REST API.
     * <p/>
     * The actual resource can be accessed by prepending the hostname of the server
     * hosting the REST API.
     *
     * @return The path to this resource. <code>null</code> if the object is not meant
     *         to be publically available via the REST API
     */
    public String getResourcePath() {
        return BARDConstants.API_BASE + "/exptdata/" + bardExptId + "." + sid;
    }

    /**
     * Set the resource path.
     * <p/>
     * In most cases, this can be an empty function as its primary purpose
     * is to allow Jackson to deserialize a JSON entity to the relevant Java
     * entity.
     *
     * @param resourcePath the resource path for this entity
     */
    public void setResourcePath(String resourcePath) {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
