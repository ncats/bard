package gov.nih.ncgc.bard.tools;

import chemaxon.formats.MolFormatException;
import chemaxon.formats.MolImporter;
import gov.nih.ncgc.bard.entity.Assay;
import gov.nih.ncgc.bard.entity.Compound;
import gov.nih.ncgc.bard.entity.ProteinTarget;
import gov.nih.ncgc.bard.entity.Publication;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.Reader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility methods to interact with the database backend.
 *
 * @author Rajarshi Guha
 */
public class DBUtils {
    Connection conn;

    public DBUtils() {
        conn = getConnection();
    }

    private Connection getConnection() {
        javax.naming.Context initContext;
        try {
            initContext = new javax.naming.InitialContext();
            javax.naming.Context envContext = (javax.naming.Context) initContext.lookup("java:/comp/env");
            DataSource ds = (javax.sql.DataSource) envContext.lookup("jdbc/bard");
            return ds.getConnection();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String readFromClob(Reader reader) throws IOException {
        StringBuilder sb = new StringBuilder();
        if (reader == null) {
            return null;
        }
        char[] buffer = new char[1];
        while (reader.read(buffer) > 0) {
            sb.append(buffer);
        }
        return sb.toString();
    }

    public Publication getPublicationByPmid(Long pmid) throws SQLException, IOException {
        if (pmid == null) return null;
        PreparedStatement pst = conn.prepareStatement("select * from publication where pmid = ?");
        pst.setLong(1, pmid);
        ResultSet rs = pst.executeQuery();
        Publication p = new Publication();
        while (rs.next()) {
            p.setTitle(rs.getString("title"));
            p.setDoi(rs.getString("doi"));
            p.setPubmedId(pmid);
            p.setAbs(readFromClob(rs.getCharacterStream("abstract")));
        }
        pst.close();
        return p;
    }

    public Publication getPublicationByDoi(String doi) throws SQLException, IOException {
        if (doi == null || doi.trim().equals("")) return null;
        PreparedStatement pst = conn.prepareStatement("select * from publication where doi = ?");
        pst.setString(1, doi);
        ResultSet rs = pst.executeQuery();
        Publication p = new Publication();
        while (rs.next()) {
            p.setTitle(rs.getString("title"));
            p.setDoi(rs.getString("doi"));
            p.setPubmedId(rs.getLong("pmid"));
            p.setAbs(readFromClob(rs.getCharacterStream("abstract")));
        }
        pst.close();
        return p;
    }

    public ProteinTarget getProteinTargetByAccession(String accession) throws SQLException {
        if (accession == null || accession.trim().equals("")) return null;
        PreparedStatement pst = conn.prepareStatement("select * from protein_target where accession  = ?");
        pst.setString(1, accession);
        ResultSet rs = pst.executeQuery();
        ProteinTarget p = new ProteinTarget();
        while (rs.next()) {
            p.setAcc(accession);
            p.setDescription(rs.getString("description"));
            p.setGeneId(rs.getLong("gene_id"));
            p.setTaxId(rs.getLong("taxid"));
            p.setName(rs.getString("name"));
            p.setStatus(rs.getString("uniprot_status"));
        }
        pst.close();
        return p;
    }

    public ProteinTarget getProteinTargetByGeneid(Long geneId) throws SQLException {
        if (geneId == null) return null;
        PreparedStatement pst = conn.prepareStatement("select * from protein_target where gene_id = ?");
        pst.setLong(1, geneId);
        ResultSet rs = pst.executeQuery();
        ProteinTarget p = new ProteinTarget();
        while (rs.next()) {
            p.setAcc(rs.getString("accession"));
            p.setDescription(rs.getString("description"));
            p.setGeneId(rs.getLong("gene_id"));
            p.setTaxId(rs.getLong("taxid"));
            p.setName(rs.getString("name"));
            p.setStatus(rs.getString("uniprot_status"));
        }
        pst.close();
        return p;
    }

    public Compound getCompoundByCid(Long cid) throws SQLException {
        if (cid == null || cid < 0) return null;
        PreparedStatement pst = conn.prepareStatement("select * from compound where cid = ?");
        pst.setLong(1, cid);
        ResultSet rs = pst.executeQuery();
        Compound c = new Compound();
        while (rs.next()) {
            c.setCid(rs.getLong("cid"));
            c.setProbeId(rs.getString("probe_id"));
            c.setUrl(rs.getString("url"));

            String smiles = null;
            String molfile = rs.getString("molfile");
            try {
                smiles = MolImporter.importMol(molfile).toFormat("smiles");
                c.setSmiles(smiles);
            } catch (MolFormatException e) {
                e.printStackTrace();
            }
        }
        pst.close();
        return c;
    }

    public Compound getCompoundByProbeId(String probeid) throws SQLException {
        if (probeid == null || probeid.trim().equals("")) return null;
        PreparedStatement pst = conn.prepareStatement("select * from compound where probe_id = ?");
        pst.setString(1, probeid.trim());
        ResultSet rs = pst.executeQuery();
        Compound c = new Compound();
        while (rs.next()) {
            c.setCid(rs.getLong("cid"));
            c.setProbeId(rs.getString("probe_id"));
            c.setUrl(rs.getString("url"));

            String smiles = null;
            String molfile = rs.getString("molfile");
            try {
                smiles = MolImporter.importMol(molfile).toFormat("smiles");
                c.setSmiles(smiles);
            } catch (MolFormatException e) {
                e.printStackTrace();
            }
        }
        pst.close();
        return c;
    }

    public Assay getAssayByAid(Long aid) throws SQLException {
        if (aid == null || aid <= 0) return null;
        PreparedStatement pst = conn.prepareStatement("select * from assay where aid = ?");
        pst.setLong(1, aid);
        ResultSet rs = pst.executeQuery();
        Assay a = new Assay();
        while (rs.next()) {
            a.setAid(aid);
            a.setAssays(rs.getInt("assays"));
            a.setCategory(rs.getInt("category"));
            a.setClassification(rs.getInt("classification"));
            a.setDeposited(rs.getDate("deposited"));
            a.setDescription(rs.getString("description"));
            a.setGrantNo(rs.getString("grant_no"));
            a.setName(rs.getString("name"));
            a.setSamples(rs.getInt("samples"));
            a.setSource(rs.getString("source"));
            a.setSummary(rs.getInt("summary"));
            a.setType(rs.getInt("type"));
            a.setUpdated(rs.getDate("updated"));

            // next we need to look up publications, targets and data
            a.setPublications(getAssayPublications(aid));
            a.setTargets(getAssayTargets(aid));
        }
        pst.close();
        return a;
    }

    /**
     * Retrieve publications associated with an assay id.
     * <p/>
     * This query requires that the publication details are available in the publication table.
     *
     * @param aid The assay id to query for
     * @return a List of {@link Publication} objects
     * @throws SQLException
     */
    public List<Publication> getAssayPublications(Long aid) throws SQLException {
        if (aid == null || aid <= 0) return null;
        PreparedStatement pst2 = conn.prepareStatement("select a.* from publication a, assay_pub b where b.aid = ? and b.pmid = a.pmid");
        pst2.setLong(1, aid);
        ResultSet rs2 = pst2.executeQuery();
        List<Publication> pubs = new ArrayList<Publication>();
        while (rs2.next()) {
            Publication p = new Publication();
            p.setDoi(rs2.getString("doi"));
            p.setTitle(rs2.getString("title"));
            p.setPubmedId(rs2.getLong("pmid"));
            p.setAbs(rs2.getString("abstract"));
            pubs.add(p);
        }
        pst2.close();
        return pubs;
    }

    public List<ProteinTarget> getAssayTargets(Long aid) throws SQLException {
        PreparedStatement pst2 = conn.prepareStatement("select a.* from protein_target a, assay_target b where b.aid = ? and a.gene_id = b.gene_id");
        pst2.setLong(1, aid);
        ResultSet rs2 = pst2.executeQuery();
        List<ProteinTarget> targets = new ArrayList<ProteinTarget>();
        while (rs2.next()) {
            ProteinTarget t = new ProteinTarget();
            t.setDescription(rs2.getString("description"));
            t.setGeneId(rs2.getLong("gene_id"));
            t.setName(rs2.getString("name"));
            t.setStatus(rs2.getString("uniprot_status"));
            t.setAcc(rs2.getString("accession"));
            t.setTaxId(rs2.getLong("taxid"));
            targets.add(t);
        }
        pst2.close();
        return targets;
    }

}
