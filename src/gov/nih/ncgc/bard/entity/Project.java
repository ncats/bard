package gov.nih.ncgc.bard.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import gov.nih.ncgc.bard.rest.BARDConstants;
import gov.nih.ncgc.bard.tools.BARDJsonRequired;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A representation of a project.
 *
 * A project will refer to one or more experiments and assays as well as meta-data
 * about the project itself.
 *
 * @author Rajarshi Guha
 */
public class Project extends BaseEntity{

    /**
     * Numeric project identifier.
     */
    @BARDJsonRequired
    Long bardProjectId;

    /**
     * CAP project identifier.
     */
    Long capProjectId;

    /**
     * A map of experiment id to the experiment type.
     */
    Map<Long, String> experimentTypes;

    int category, type, classification;
    String name, description, source;
    float score;

    // annotations
    @JsonIgnore
    List<String> gobp_id, gobp_term, gomf_term, gomf_id, gocc_id, gocc_term;

    @JsonIgnore
    List<String> av_dict_label, ak_dict_label;
    @JsonIgnore
    List<String> kegg_disease_names, kegg_disease_cat;

    /**
     * Grant number for this project (if any).
     */
    String grantNo;

    /**
     * Date deposited.
     */
    Date deposited;

    /**
     * Date update;
     */
    Date updated;

    List<Compound> probes;
    List<Long> probeIds;

    /**
     * A list of BARD experiment ids
     */
    List<Long> eids;

    /**
     * A list of BARD assay ids
     */
    List<Long> aids;

    /**
     * A list of Pubmed IDs associated with the project.
     */
    List<Long> publications;

    /**
     * A list of target biologies associated with the project
     *
     * @see Biology
     */
    List<Biology> targets;

    public float getScore() {
        return score;
    }

    public void setScore(float score) {
        this.score = score;
    }

    public Map<Long, String> getExperimentTypes() {
        return experimentTypes;
    }

    public void setExperimentTypes(Map<Long, String> experimentTypes) {
        this.experimentTypes = experimentTypes;
    }

    public Long getCapProjectId() {
        return capProjectId;
    }

    public void setCapProjectId(Long capProjectId) {
        this.capProjectId = capProjectId;
    }

    public List<Compound> getProbes() {
        return probes;
    }

    public void setProbes(List<Compound> probes) {
        this.probes = probes;
    }

    public List<String> getGobp_id() {
        return gobp_id;
    }

    public void setGobp_id(List<String> gobp_id) {
        this.gobp_id = gobp_id;
    }

    public List<String> getGobp_term() {
        return gobp_term;
    }

    public void setGobp_term(List<String> gobp_term) {
        this.gobp_term = gobp_term;
    }

    public List<String> getGomf_term() {
        return gomf_term;
    }

    public void setGomf_term(List<String> gomf_term) {
        this.gomf_term = gomf_term;
    }

    public List<String> getGomf_id() {
        return gomf_id;
    }

    public void setGomf_id(List<String> gomf_id) {
        this.gomf_id = gomf_id;
    }

    public List<String> getGocc_id() {
        return gocc_id;
    }

    public void setGocc_id(List<String> gocc_id) {
        this.gocc_id = gocc_id;
    }

    public List<String> getGocc_term() {
        return gocc_term;
    }

    public void setGocc_term(List<String> gocc_term) {
        this.gocc_term = gocc_term;
    }

    public List<String> getAv_dict_label() {
        return av_dict_label;
    }

    public void setAv_dict_label(List<String> av_dict_label) {
        this.av_dict_label = av_dict_label;
    }

    public List<String> getAk_dict_label() {
        return ak_dict_label;
    }

    public void setAk_dict_label(List<String> ak_dict_label) {
        this.ak_dict_label = ak_dict_label;
    }

    public List<String> getKegg_disease_names() {
        return kegg_disease_names;
    }

    public void setKegg_disease_names(List<String> kegg_disease_names) {
        this.kegg_disease_names = kegg_disease_names;
    }

    public List<String> getKegg_disease_cat() {
        return kegg_disease_cat;
    }

    public void setKegg_disease_cat(List<String> kegg_disease_cat) {
        this.kegg_disease_cat = kegg_disease_cat;
    }

    public String toString() {
        return bardProjectId + "[" + name + "]";
    }

    public List<Long> getProbeIds() {
        return probeIds;
    }

    public void setProbeIds(List<Long> probeIds) {
        this.probeIds = probeIds;
    }

    public Project() {
        eids = new ArrayList<Long>();
        probeIds = new ArrayList<Long>();
    }

    public int getExperimentCount() {
        return eids.size();
    }

    public int getClassification() {
        return classification;
    }

    public void setClassification(int classification) {
        this.classification = classification;
    }

    public List<Long> getEids() {
        return eids;
    }

    public void setEids(List<Long> eids) {
        this.eids = eids;
    }

    public List<Long> getAids () {
        return aids;
    }
    public void setAids (List<Long> aids) {
        this.aids = aids;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public List<Long> getPublications() {
        return publications;
    }

    public void setPublications(List<Long> publications) {
        this.publications = publications;
    }

    public List<Biology> getTargets() {
        return targets;
    }

    public void setTargets(List<Biology> targets) {
        this.targets = targets;
    }

    public Long getBardProjectId() {
        return bardProjectId;
    }

    public void setBardProjectId(Long bardProjectId) {
        this.bardProjectId = bardProjectId;
    }

    public Integer getCategory() {
        return category;
    }

    public void setCategory(Integer category) {
        this.category = category;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getGrantNo() {
        return grantNo;
    }

    public void setGrantNo(String grantNo) {
        this.grantNo = grantNo;
    }

    public Date getDeposited() {
        return deposited;
    }

    public void setDeposited(Date deposited) {
        this.deposited = deposited;
    }

    public Date getUpdated() {
        return updated;
    }

    public void setUpdated(Date updated) {
        this.updated = updated;
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
        return BARDConstants.API_BASE + "/projects/" + bardProjectId;
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
