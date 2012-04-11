package gov.nih.ncgc.bard.entity;

/**
 * Represents a publication.
 * <p/>
 * Most will be derived from Pubmed, though arbitrary publications can be
 * represented, especially if a DOI is provided.
 *
 * @author Rajarshi Guha
 */
public class Publication implements BardEntity {
    String title, doi, abs;
    Long pubmedId;

    public Publication() {
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDoi() {
        return doi;
    }

    public void setDoi(String doi) {
        this.doi = doi;
    }

    public String getAbs() {
        return abs;
    }

    public void setAbs(String abs) {
        this.abs = abs;
    }

    public Long getPubmedId() {
        return pubmedId;
    }

    public void setPubmedId(Long pubmedId) {
        this.pubmedId = pubmedId;
    }

}
