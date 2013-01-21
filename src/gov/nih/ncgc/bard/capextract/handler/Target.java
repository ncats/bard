package gov.nih.ncgc.bard.capextract.handler;

/**
 * @author Rajarshi Guha
 */
public class Target {
    public String geneid, uniprot;

    public Target(String geneid, String uniprot) {
        this.geneid = geneid;
        this.uniprot = uniprot;
    }

    public String getGeneid() {
        return geneid;
    }

    public void setGeneid(String geneid) {
        this.geneid = geneid;
    }

    public String getUniprot() {
        return uniprot;
    }

    public void setUniprot(String uniprot) {
        this.uniprot = uniprot;
    }

    public int hashCode() {
        String hs = geneid + uniprot;
        return hs.hashCode();
    }

    public boolean equals(Object o) {
        if (o == null || !(o instanceof  Target)) return false;
        Target t = (Target) o;
        return !(t.geneid == null || t.uniprot == null) && t.geneid.equals(geneid) && t.uniprot.equals(uniprot);
    }
}