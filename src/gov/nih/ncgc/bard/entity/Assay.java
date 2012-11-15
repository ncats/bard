package gov.nih.ncgc.bard.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import gov.nih.ncgc.bard.rest.BARDConstants;

import java.sql.Date;
import java.util.List;

/**
 * A representation of an assay.
 *
 * @author Rajarshi Guha
 */
public class Assay implements BardEntity {
    Long aid, bardAssayId, capAssayId;
    int category, type, summary, assays, classification;
    String name, source, grantNo;
    Date deposited, updated;

    List<Long> publications;
    List<String> targets; // Uniprot accession
    List<Long> experiments, projects; // experiments and projects

    @JsonIgnore
    String description, protocol, comments;


    @JsonIgnore
    List<String> gobp_id, gobp_term, gomf_term, gomf_id, gocc_id, gocc_term, av_dict_label, ak_dict_label;

    public List<Long> getExperiments() {
        return experiments;
    }    

    List<String> kegg_disease_names, kegg_disease_cat;

    public Long getCapAssayId() {
        return capAssayId;
    }

    public void setCapAssayId(Long capAssayId) {
        this.capAssayId = capAssayId;
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

    public void setExperiments(List<Long> experiments) {
        this.experiments = experiments;
    }

    public List<Long> getProjects() {
        return projects;
    }

    public void setProjects(List<Long> projects) {
        this.projects = projects;
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

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public Long getAid() {
        return aid;
    }

    public void setAid(Long aid) {
        this.aid = aid;
    }

    public Long getBardAssayId() {
		return bardAssayId;
	}

	public void setBardAssayId(Long bardAssayId) {
		this.bardAssayId = bardAssayId;
	}

	public String toString() {
        return bardAssayId + "[" + name + "]";
    }

    public Assay() {
    }

    public int getAssays() {
        return assays;
    }

    public void setAssays(int assays) {
        this.assays = assays;
    }

    public int getClassification() {
        return classification;
    }

    public void setClassification(int classification) {
        this.classification = classification;
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

    public List<String> getTargets() {
        return targets;
    }

    public void setTargets(List<String> targets) {
        this.targets = targets;
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

    public Integer getSummary() {
        return summary;
    }

    public void setSummary(Integer summary) {
        this.summary = summary;
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
     * @return The path to this resource
     */
    public String getResourcePath() {
        return BARDConstants.API_BASE + "/assays/" + bardAssayId;
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
