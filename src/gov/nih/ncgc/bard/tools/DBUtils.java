package gov.nih.ncgc.bard.tools;

import chemaxon.formats.MolFormatException;
import chemaxon.formats.MolImporter;
import gov.nih.ncgc.bard.entity.Assay;
import gov.nih.ncgc.bard.entity.Compound;
import gov.nih.ncgc.bard.entity.Project;
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

    public void closeConnection() throws SQLException {
        conn.close();
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
     * Retrieve CIDs for compounds associated with an assay.
     *
     * @param aid The assay identifier
     * @return A list of compound CIDs
     * @throws SQLException
     */
    public List<Long> getAssayCompoundCids(Long aid) throws SQLException {
        if (aid == null || aid < 0) return null;
        PreparedStatement pst = conn.prepareStatement("select cid from assay_data where aid = ?");
        pst.setLong(1, aid);
        ResultSet rs = pst.executeQuery();
        List<Long> ret = new ArrayList<Long>();
        while (rs.next()) ret.add(rs.getLong("cid"));
        pst.close();
        return ret;
    }

    /**
     * Retrieve compounds associated with an assay.
     *
     * @param aid The assay identifier
     * @return A list of {@link Compound} objects
     * @throws SQLException
     */
    public List<Compound> getAssayCompounds(Long aid) throws SQLException {
        if (aid == null || aid < 0) return null;
        PreparedStatement pst = conn.prepareStatement("select cid from assay_data where aid = ?");
        pst.setLong(1, aid);
        ResultSet rs = pst.executeQuery();
        List<Compound> ret = new ArrayList<Compound>();
        while (rs.next()) ret.add(getCompoundByCid(rs.getLong("cid")));
        pst.close();
        return ret;
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

    /**
     * Retrieve assays based on query.
     * <p/>
     * Currently a crude query language is supported which requries you to specify the field
     * to be queried on or if no field is specified then a full text search is applied to all
     * text fields.
     * <p/>
     * Queries should in the form of query_string[field_name]
     * <p/>
     * The current implementation of free text search is pretty stupid. We should enable the
     * full text search functionality in the database.
     *
     * @param query the query to use
     * @return A list of {@link Assay} objects, whuich may be empty if no assays match the query.
     */
    public List<Assay> searchForAssay(String query) throws SQLException {
        boolean freeTextQuery = false;

        if (!query.contains("[")) freeTextQuery = true;

        PreparedStatement pst = null;
        if (freeTextQuery) {
            String q = "%" + query + "%";
            pst = conn.prepareStatement("select aid from assay where (name like ? or description like ? or source like ? or grant_no like ?)");
            pst.setString(1, q);
            pst.setString(2, q);
            pst.setString(3, q);
            pst.setString(4, q);
        } else {
            String[] toks = query.split("\\[");
            String q = toks[0].trim();
            String field = toks[1].trim().replace("]", "");
            String sql = "select aid from assay where " + field + " like '%" + q + "%'";
            pst = conn.prepareStatement(sql);
        }

        ResultSet rs = pst.executeQuery();
        List<Assay> assays = new ArrayList<Assay>();
        while (rs.next()) {
            Long aid = rs.getLong("aid");
            assays.add(getAssayByAid(aid));
        }
        pst.close();

        return assays;
    }

    /**
     * Retrieve targets based on query.
     * <p/>
     * Currently a crude query language is supported which requries you to specify the field
     * to be queried on or if no field is specified then a full text search is applied to all
     * text fields.
     * <p/>
     * Queries should in the form of query_string[field_name]
     * <p/>
     * The current implementation of free text search is pretty stupid. We should enable the
     * full text search functionality in the database.
     *
     * @param query the query to use
     * @return A list of {@link ProteinTarget} objects, whuich may be empty if no assays match the query.
     */
    public List<ProteinTarget> searchForTargets(String query, int skip, int top) throws SQLException {
        boolean freeTextQuery = false;

        if (!query.contains("[")) freeTextQuery = true;

        String limitClause = "";
        if (skip != -1) {
            if (top <= 0) throw new SQLException("If skip != -1, top must be greater than 0");
            limitClause = "  limit " + skip + "," + top;
        }

        PreparedStatement pst = null;
        if (freeTextQuery) {
            String q = "%" + query + "%";
            pst = conn.prepareStatement("select accession from protein_target where (accession like ? or gene_id like ? or name like ? or description like ? or uniprot_status like ?) order by accession" + limitClause);
            pst.setString(1, q);
            pst.setString(2, q);
            pst.setString(3, q);
            pst.setString(4, q);
            pst.setString(5, q);
        } else {
            String[] toks = query.split("\\[");
            String q = toks[0].trim();
            String field = toks[1].trim().replace("]", "");
            String sql = "select accession from protein_target where " + field + " like '%" + q + "%' order by accession " + limitClause;
            pst = conn.prepareStatement(sql);
        }

        ResultSet rs = pst.executeQuery();
        List<ProteinTarget> targets = new ArrayList<ProteinTarget>();
        while (rs.next()) {
            String accession = rs.getString("accession");
            targets.add(getProteinTargetByAccession(accession));
        }
        pst.close();

        return targets;
    }


    /**
     * Get a summary count of projects listing AID for project and number of assays associated with it.
     *
     * @return A list of Long[2], where the elements of the array are the summary AID and the number of
     *         assays associated with it, respectively.
     * @throws SQLException
     */
    public List<Long[]> getProjectCount() throws SQLException {
        PreparedStatement pst = conn.prepareStatement("select summary,count(1) as cnt from assay where summary is not null group by summary order by cnt desc");
        ResultSet rs = pst.executeQuery();
        List<Long[]> ret = new ArrayList<Long[]>();
        while (rs.next()) {
            ret.add(new Long[]{rs.getLong("summary"), rs.getLong("cnt")});
        }
        pst.close();
        return ret;
    }

    /**
     * Return a list of all aids.
     *
     * @return
     * @throws SQLException
     */
    public List<Long> getAssayCount() throws SQLException {
        PreparedStatement pst = conn.prepareStatement("select aid from assay order by aid");
        ResultSet rs = pst.executeQuery();
        List<Long> ret = new ArrayList<Long>();
        while (rs.next()) {
            ret.add(rs.getLong("aid"));
        }
        pst.close();
        return ret;
    }

    public int getTargetCount() throws SQLException {
        PreparedStatement pst = conn.prepareStatement("select count(accession) as c from protein_target");
        ResultSet rs = pst.executeQuery();
        int n = 0;
        while (rs.next()) n = rs.getInt("c");
        pst.close();
        return n;
    }

    /**
     * Return a project object based on its AID.
     * <p/>
     * Currently projects are simply summary assays and therefore are very similar
     * in structure to {@link Assay} objects. The key difference is that they will
     * list AID's of assays associated with it.
     *
     * @param aid The AID for the summary assay
     * @return A {@link Project} object
     * @throws SQLException
     */
    public Project getProjectByAid(Long aid) throws SQLException {
        Assay a = getAssayByAid(aid);
        Project p = new Project();
        p.setAid(a.getAid());
        p.setCategory(a.getCategory());
        p.setType(a.getType());
        p.setTargets(a.getTargets());
        p.setPublications(a.getPublications());
        p.setGrantNo(a.getGrantNo());
        p.setSource(a.getSource());
        p.setDescription(a.getDescription());
        p.setDeposited(a.getDeposited());
        p.setUpdated(a.getUpdated());

        // identify the assays that are part of this project
        PreparedStatement pst = conn.prepareStatement("select aid from assay where summary = ?");
        pst.setLong(1, aid);
        ResultSet rs = pst.executeQuery();
        List<Long> aids = new ArrayList<Long>();
        while (rs.next()) aids.add(rs.getLong("aid"));
        p.setAids(aids);

        // get probe ids
        List<Long> probeids = getProbesForProject(aid);
        p.setProbeIds(probeids);

        pst.close();
        return p;
    }

    public List<Long> getProbesForProject(Long aid) throws SQLException {
        PreparedStatement pst = conn.prepareStatement("select a.cid from assay_data a, compound b where b.probe_id is not null and a.aid = ? and a.cid = b.cid");
        pst.setLong(1, aid);
        ResultSet rs = pst.executeQuery();
        List<Long> probeids = new ArrayList<Long>();
        while (rs.next()) probeids.add(rs.getLong("cid"));
        return probeids;
    }

    /**
     * Retrieve projects based on query.
     * <p/>
     * Currently a crude query language is supported which requries you to specify the field
     * to be queried on or if no field is specified then a full text search is applied to all
     * text fields.
     * <p/>
     * Queries should in the form of query_string[field_name]
     * <p/>
     * The current implementation of free text search is pretty stupid. We should enable the
     * full text search functionality in the database.
     *
     * @param query the query to use
     * @return A list of {@link Project} objects, whuich may be empty if no assays match the query.
     */
    public List<Project> searchForProject(String query) throws SQLException {
        boolean freeTextQuery = false;

        if (!query.contains("[")) freeTextQuery = true;

        PreparedStatement pst = null;
        if (freeTextQuery) {
            String q = "%" + query + "%";
            pst = conn.prepareStatement("select aid from assay where type = 3 and (name like ? or description like ? or source like ? or grant_no like ?)");
            pst.setString(1, q);
            pst.setString(2, q);
            pst.setString(3, q);
            pst.setString(4, q);
        } else {
            String[] toks = query.split("\\[");
            String q = toks[0].trim();
            String field = toks[1].trim().replace("]", "");
            String sql = "select aid from assay where type = 3 and  " + field + " like '%" + q + "%'";
            pst = conn.prepareStatement(sql);
        }

        ResultSet rs = pst.executeQuery();
        List<Project> projects = new ArrayList<Project>();
        while (rs.next()) {
            Long aid = rs.getLong("aid");
            projects.add(getProjectByAid(aid));
        }
        pst.close();
        return projects;
    }
}
