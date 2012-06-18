package gov.nih.ncgc.bard.tools;

import gov.nih.ncgc.bard.entity.Assay;
import gov.nih.ncgc.bard.entity.Compound;
import gov.nih.ncgc.bard.entity.Experiment;
import gov.nih.ncgc.bard.entity.ExperimentData;
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
import java.util.Arrays;
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

    public int getPublicationCount() throws SQLException, IOException {
        PreparedStatement pst = conn.prepareStatement("select count(pmid) as c from publication");
        ResultSet rs = pst.executeQuery();
        int n = 0;
        while (rs.next()) n = rs.getInt("c");
        pst.close();
        return n;
    }

    public List<Publication> getProteinTargetPublications(String accession) throws SQLException {
        if (accession == null || accession.trim().equals("")) return null;

        PreparedStatement pst2 = conn.prepareStatement("select a.* from publication a, target_pub b where b.accession = ? and b.pmid = a.pmid");
        pst2.setString(1, accession);
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

    public int getCompoundCount() throws SQLException {
        PreparedStatement pst = conn.prepareStatement("select count(cid) from compound");
        ResultSet rs = pst.executeQuery();
        int n = 0;
        while (rs.next()) n = rs.getInt(1);
        pst.close();
        return (n);
    }

    public Long getCidBySid(Long sid) throws SQLException {
        PreparedStatement pst = conn.prepareStatement("select cid from cid_sid where sid = ?");
        pst.setLong(1, sid);
        ResultSet rs = pst.executeQuery();
        long cid = -1L;
        while (rs.next()) cid = rs.getLong(1);
        pst.close();
        return cid;
    }

    public List<Long> getSidsByCid(Long cid) throws SQLException {
        List<Long> sids = new ArrayList<Long>();
        PreparedStatement pst = conn.prepareStatement("select sid from cid_sid where cid = ?");
        pst.setLong(1, cid);
        ResultSet rs = pst.executeQuery();
        while (rs.next()) sids.add(rs.getLong(1));
        pst.close();
        return sids;
    }

    public int getSubstanceCount() throws SQLException {
        PreparedStatement pst = conn.prepareStatement("select count(sid) from substance");
        ResultSet rs = pst.executeQuery();
        int n = 0;
        while (rs.next()) n = rs.getInt(1);
        pst.close();
        return (n);
    }

    public Compound getCompoundByCid(Long cid) throws SQLException {
        if (cid == null || cid < 0) return null;
        PreparedStatement pst = conn.prepareStatement("select c.*, s.sid from compound c, cid_sid s where c.cid = ? and c.cid = s.cid");
        pst.setLong(1, cid);
        ResultSet rs = pst.executeQuery();
        Compound c = new Compound();
        List<Long> sids = new ArrayList<Long>();
        while (rs.next()) {
            c.setCid(rs.getLong("cid"));
            c.setProbeId(rs.getString("probe_id"));
            c.setSmiles(rs.getString("iso_smiles"));
            sids.add(rs.getLong("sid"));
        }
        c.setSids(sids);
        pst.close();
        return c;
    }

    public Compound getCompoundBySid(Long sid) throws SQLException {
        if (sid == null || sid < 0) return null;
        PreparedStatement pst = conn.prepareStatement("select cid from cid_sid s where s.sid = ?");
        pst.setLong(1, sid);
        ResultSet rs = pst.executeQuery();
        Long cid = -1L;
        while (rs.next()) cid = rs.getLong("cid");
        pst.close();
        return getCompoundByCid(cid);
    }

    public Compound getCompoundByProbeId(String probeid) throws SQLException {
        if (probeid == null || probeid.trim().equals("")) return null;
        PreparedStatement pst = conn.prepareStatement("select c.*, s.sid from compound c, cid_sid s where probe_id = ? and c.cid = s.cid");
        pst.setString(1, probeid.trim());
        ResultSet rs = pst.executeQuery();
        Compound c = new Compound();
        List<Long> sids = new ArrayList<Long>();
        while (rs.next()) {
            c.setCid(rs.getLong("cid"));
            c.setProbeId(rs.getString("probe_id"));
            c.setSmiles(rs.getString("iso_smiles"));
            sids.add(rs.getLong("sid"));
        }
        c.setSids(sids);
        pst.close();
        return c;
    }

    public ExperimentData getExperimentDataByDataId(Long edid) throws SQLException {
        if (edid == null || edid <= 0) return null;
        PreparedStatement pst = conn.prepareStatement("select * from experiment_data where expt_data_id = ?");
        pst.setLong(1, edid);
        ResultSet rs = pst.executeQuery();
        ExperimentData ed = new ExperimentData();
        ed.setExptDataId(edid);
        while (rs.next()) {
            ed.setEid(rs.getLong("eid"));
            ed.setSid(rs.getLong("sid"));
            ed.setCid(rs.getLong("cid"));
            ed.setClassification(rs.getInt("classification"));
            ed.setUpdated(rs.getDate("updated"));
            ed.setOutcome(rs.getInt("outcome"));
            ed.setScore(rs.getInt("score"));
            ed.setPotency(rs.getFloat("potency"));
        }
        return ed;
    }

    public Experiment getExperimentByExptId(Long exptId) throws SQLException {
        if (exptId == null || exptId <= 0) return null;
        PreparedStatement pst = conn.prepareStatement("select * from experiment where expt_id = ?");
        pst.setLong(1, exptId);
        ResultSet rs = pst.executeQuery();
        Experiment e = new Experiment();
        while (rs.next()) {
            e.setExptId(exptId);
            e.setAssayId(rs.getLong("assay_id"));
            e.setProjId(rs.getLong("proj_id"));

            e.setName(rs.getString("name"));
            e.setDescription(rs.getString("description"));
            e.setSource(rs.getString("source"));
            e.setGrantNo(rs.getString("grant_no"));

            e.setCategory(rs.getInt("category"));
            e.setClassification(rs.getInt("classification"));
            e.setClassification(rs.getInt("type"));
            e.setSummary(rs.getInt("summary"));

            e.setDeposited(rs.getDate("deposited"));
            e.setUpdated(rs.getDate("updated"));

            e.setSubstances(rs.getInt("samples"));
            e.setCompounds(rs.getInt("cid_count"));

            e.setHasProbe(rs.getInt("have_probe") == 0 ? false : true);
        }
        pst.close();
        return e;
    }

    public List<Experiment> getExperimentByAssayId(Long aid) throws SQLException {
        if (aid == null || aid <= 0) return null;
        PreparedStatement pst = conn.prepareStatement("select expt_id from experiment where assay_id = ?");
        pst.setLong(1, aid);
        ResultSet rs = pst.executeQuery();
        List<Experiment> experiments = new ArrayList<Experiment>();
        while (rs.next()) experiments.add(getExperimentByExptId(rs.getLong(1)));
        pst.close();
        return experiments;
    }

    public Assay getAssayByAid(Long aid) throws SQLException {
        if (aid == null || aid <= 0) return null;
        PreparedStatement pst = conn.prepareStatement("select * from assay where assay_id = ?");
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
            a.setSource(rs.getString("source"));
            a.setSummary(rs.getInt("summary"));
            a.setType(rs.getInt("type"));
            a.setUpdated(rs.getDate("updated"));
            a.setComments(rs.getString("comment"));
            a.setProtocol(rs.getString("protocol"));

            // next we need to look up publications, targets and data
            a.setPublications(getAssayPublications(aid));
            a.setTargets(getAssayTargets(aid));
        }
        pst.close();
        return a;
    }

    /**
     * Retrieve CIDs for compounds associated with an experiment.
     *
     * @param eid  The experiment identifier
     * @param skip how many records to skip
     * @param top  how many records to return
     * @return A list of compound CIDs
     * @throws SQLException if an invalid limit specification is supplied or there is an error in the SQL query
     */
    public List<Long> getExperimentCids(Long eid, int skip, int top) throws SQLException {
        if (eid == null || eid < 0) return null;

        String limitClause = "";
        if (skip != -1) {
            if (top <= 0) throw new SQLException("If skip != -1, top must be greater than 0");
            limitClause = "  limit " + skip + "," + top;
        }

        PreparedStatement pst = conn.prepareStatement("select distinct cid from experiment_data where eid = ? order by cid " + limitClause);
        pst.setLong(1, eid);
        ResultSet rs = pst.executeQuery();
        List<Long> ret = new ArrayList<Long>();
        while (rs.next()) ret.add(rs.getLong("cid"));
        pst.close();
        return ret;
    }

    /**
     * Return experiment data ids for an experiment.
     *
     * @param eid  The experiment id (AKA Pubchem AID for experiments taken from Pubchem)
     * @param skip how many records to skip
     * @param top  how many records to return
     * @return
     * @throws SQLException
     */
    public List<Long> getExperimentDataIds(Long eid, int skip, int top) throws SQLException {
        if (eid == null || eid < 0) return null;

        String limitClause = "";
        if (skip != -1) {
            if (top <= 0) throw new SQLException("If skip != -1, top must be greater than 0");
            limitClause = "  limit " + skip + "," + top;
        }

        PreparedStatement pst = conn.prepareStatement("select expt_data_id from experiment_data where eid = ? order by expt_data_id " + limitClause);
        pst.setLong(1, eid);
        ResultSet rs = pst.executeQuery();
        List<Long> ret = new ArrayList<Long>();
        while (rs.next()) ret.add(rs.getLong("expt_data_id"));
        pst.close();
        return ret;
    }

    /**
     * Return experiment data objects for an experiment.
     *
     * @param eid  The experiment id (AKA Pubchem AID for experiments taken from Pubchem)
     * @param skip how many records to skip
     * @param top  how many records to return
     * @return
     * @throws SQLException
     */
    public List<ExperimentData> getExperimentData(Long eid, int skip, int top) throws SQLException {
        if (eid == null || eid < 0) return null;

        String limitClause = "";
        if (skip != -1) {
            if (top <= 0) throw new SQLException("If skip != -1, top must be greater than 0");
            limitClause = "  limit " + skip + "," + top;
        }

        PreparedStatement pst = conn.prepareStatement("select expt_data_id from experiment_data where eid = ? order by expt_data_id " + limitClause);
        pst.setLong(1, eid);
        ResultSet rs = pst.executeQuery();
        List<ExperimentData> ret = new ArrayList<ExperimentData>();
        while (rs.next()) ret.add(getExperimentDataByDataId(rs.getLong(1)));
        pst.close();
        return ret;
    }

    /**
     * Return experiment data ids for an substance.
     *
     * @param sid  The Pubchem SID
     * @param skip how many records to skip
     * @param top  how many records to return
     * @return
     * @throws SQLException
     */
    public List<Long> getSubstanceDataIds(Long sid, int skip, int top) throws SQLException {
        if (sid == null || sid < 0) return null;

        String limitClause = "";
        if (skip != -1) {
            if (top <= 0) throw new SQLException("If skip != -1, top must be greater than 0");
            limitClause = "  limit " + skip + "," + top;
        }

        PreparedStatement pst = conn.prepareStatement("select expt_data_id from experiment_data where sid = ? order by expt_data_id " + limitClause);
        pst.setLong(1, sid);
        ResultSet rs = pst.executeQuery();
        List<Long> ret = new ArrayList<Long>();
        while (rs.next()) ret.add(rs.getLong("expt_data_id"));
        pst.close();
        return ret;
    }

    /**
     * Return experiment data objects for a substance.
     *
     * @param sid  The Pubchem SID
     * @param skip how many records to skip
     * @param top  how many records to return
     * @return
     * @throws SQLException
     */
    public List<ExperimentData> getSubstanceData(Long sid, int skip, int top) throws SQLException {
        if (sid == null || sid < 0) return null;

        String limitClause = "";
        if (skip != -1) {
            if (top <= 0) throw new SQLException("If skip != -1, top must be greater than 0");
            limitClause = "  limit " + skip + "," + top;
        }

        PreparedStatement pst = conn.prepareStatement("select expt_data_id from experiment_data where sid = ? order by expt_data_id " + limitClause);
        pst.setLong(1, sid);
        ResultSet rs = pst.executeQuery();
        List<ExperimentData> ret = new ArrayList<ExperimentData>();
        while (rs.next()) ret.add(getExperimentDataByDataId(rs.getLong(1)));
        pst.close();
        return ret;
    }

    /**
     * Return experiment data ids for a compound.
     *
     * @param cid  The Pubchem CID
     * @param skip how many records to skip
     * @param top  how many records to return
     * @return
     * @throws SQLException
     */
    public List<Long> getCompoundDataIds(Long cid, int skip, int top) throws SQLException {
        if (cid == null || cid < 0) return null;

        String limitClause = "";
        if (skip != -1) {
            if (top <= 0) throw new SQLException("If skip != -1, top must be greater than 0");
            limitClause = "  limit " + skip + "," + top;
        }

        PreparedStatement pst = conn.prepareStatement("select expt_data_id from experiment_data where cid = ? order by expt_data_id " + limitClause);
        pst.setLong(1, cid);
        ResultSet rs = pst.executeQuery();
        List<Long> ret = new ArrayList<Long>();
        while (rs.next()) ret.add(rs.getLong("expt_data_id"));
        pst.close();
        return ret;
    }

    /**
     * Return experiment data objects for a compound.
     *
     * @param cid  The Pubchem CID
     * @param skip how many records to skip
     * @param top  how many records to return
     * @return
     * @throws SQLException
     */
    public List<ExperimentData> getCompoundData(Long cid, int skip, int top) throws SQLException {
        if (cid == null || cid < 0) return null;

        String limitClause = "";
        if (skip != -1) {
            if (top <= 0) throw new SQLException("If skip != -1, top must be greater than 0");
            limitClause = "  limit " + skip + "," + top;
        }

        PreparedStatement pst = conn.prepareStatement("select expt_data_id from experiment_data where cid = ? order by expt_data_id " + limitClause);
        pst.setLong(1, cid);
        ResultSet rs = pst.executeQuery();
        List<ExperimentData> ret = new ArrayList<ExperimentData>();
        while (rs.next()) ret.add(getExperimentDataByDataId(rs.getLong(1)));
        pst.close();
        return ret;
    }


    /**
     * Retrieve SIDs for compounds associated with an experiment.
     *
     * @param eid  The experiment identifier
     * @param skip how many records to skip
     * @param top  how many records to return
     * @return A list of compound SIDs
     * @throws SQLException if an invalid limit specification is supplied or there is an error in the SQL query
     */
    public List<Long> getExperimentSids(Long eid, int skip, int top) throws SQLException {
        if (eid == null || eid < 0) return null;

        String limitClause = "";
        if (skip != -1) {
            if (top <= 0) throw new SQLException("If skip != -1, top must be greater than 0");
            limitClause = "  limit " + skip + "," + top;
        }

        PreparedStatement pst = conn.prepareStatement("select distinct sid from experiment_data where eid = ? order by sid " + limitClause);
        pst.setLong(1, eid);
        ResultSet rs = pst.executeQuery();
        List<Long> ret = new ArrayList<Long>();
        while (rs.next()) ret.add(rs.getLong("sid"));
        pst.close();
        return ret;
    }

    /**
     * Retrieve compounds associated with an experiment.
     *
     * @param eid  The experiment identifier
     * @param skip how many records to skip
     * @param top  how many records to return
     * @return A list of {@link Compound} objects
     * @throws SQLException if an invalid limit specification is supplied or there is an error in the SQL query
     */
    public List<Compound> getExperimentCompounds(Long eid, int skip, int top) throws SQLException {
        if (eid == null || eid < 0) return null;

        String limitClause = "";
        if (skip != -1) {
            if (top <= 0) throw new SQLException("If skip != -1, top must be greater than 0");
            limitClause = "  limit " + skip + "," + top;
        }

        PreparedStatement pst = conn.prepareStatement("select cid, sid from experiment_data where eid = ? order by sid " + limitClause);
        pst.setLong(1, eid);
        ResultSet rs = pst.executeQuery();
        List<Compound> ret = new ArrayList<Compound>();

        while (rs.next()) {
            Compound c = getCompoundByCid(rs.getLong("cid"));
            ret.add(c);
        }
        pst.close();
        return ret;
    }

    /**
     * Retrieve substances associated with an assay.
     *
     * @param eid  The assay identifier
     * @param skip how many records to skip
     * @param top  how many records to return
     * @return A list of {@link Compound} objects
     * @throws SQLException if an invalid limit specification is supplied or there is an error in the SQL query
     */
    public List<Compound> getExperimentSubstances(Long eid, int skip, int top) throws SQLException {
        if (eid == null || eid < 0) return null;

        String limitClause = "";
        if (skip != -1) {
            if (top <= 0) throw new SQLException("If skip != -1, top must be greater than 0");
            limitClause = "  limit " + skip + "," + top;
        }

        PreparedStatement pst = conn.prepareStatement("select cid, sid from experiment_data where eid = ? order by sid " + limitClause);
        pst.setLong(1, eid);
        ResultSet rs = pst.executeQuery();
        List<Compound> ret = new ArrayList<Compound>();

        while (rs.next()) {
            Compound c = getCompoundBySid(rs.getLong("sid"));  // TODO should return a Substance entity
            ret.add(c);
        }
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
     * Retrieve experiment data based on a query.
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
     * @return A list of {@link gov.nih.ncgc.bard.entity.Experiment} objects, whuich may be empty if no experiments match the query.
     */
    public List<ExperimentData> searchForExperimentData(String query) throws SQLException {
        boolean freeTextQuery = false;

        if (!query.contains("[")) freeTextQuery = true;

        PreparedStatement pst = null;
        if (freeTextQuery) {
            return new ArrayList<ExperimentData>();
        } else {
            String[] toks = query.split("\\[");
            String q = toks[0].trim();
            String field = toks[1].trim().replace("]", "");

            String sql = "select expt_data_id from experiment_data where " + field + " = " + q + "";
            pst = conn.prepareStatement(sql);
        }

        ResultSet rs = pst.executeQuery();
        List<ExperimentData> experimentData = new ArrayList<ExperimentData>();
        while (rs.next()) {
            Long exptId = rs.getLong("expt_id");
            experimentData.add(getExperimentDataByDataId(exptId));
        }
        pst.close();
        return experimentData;
    }

    /**
     * Retrieve experiments based on query.
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
     * @return A list of {@link gov.nih.ncgc.bard.entity.Experiment} objects, whuich may be empty if no experiments match the query.
     */
    public List<Experiment> searchForExperiment(String query) throws SQLException {
        boolean freeTextQuery = false;

        if (!query.contains("[")) freeTextQuery = true;

        PreparedStatement pst = null;
        if (freeTextQuery) {
            String q = "%" + query + "%";
            pst = conn.prepareStatement("select expt_id from experiment where (name like ? or description like ? or source like ? or grant_no like ?)");
            pst.setString(1, q);
            pst.setString(2, q);
            pst.setString(3, q);
            pst.setString(4, q);
        } else {
            String[] toks = query.split("\\[");
            String q = toks[0].trim();
            String field = toks[1].trim().replace("]", "");
            String sql = "select expt_id from experiment where " + field + " like '%" + q + "%'";
            pst = conn.prepareStatement(sql);
        }

        ResultSet rs = pst.executeQuery();
        List<Experiment> experiments = new ArrayList<Experiment>();
        while (rs.next()) {
            Long exptId = rs.getLong("expt_id");
            experiments.add(getExperimentByExptId(exptId));
        }
        pst.close();

        return experiments;
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
        PreparedStatement pst = conn.prepareStatement("select assay_id from assay order by assay_id");
        ResultSet rs = pst.executeQuery();
        List<Long> ret = new ArrayList<Long>();
        while (rs.next()) {
            ret.add(rs.getLong(1));
        }
        pst.close();
        return ret;
    }

    /**
     * Returns a list of all experiment ids.
     *
     * @return
     * @throws SQLException
     */
    public List<Long> getExperimentIds() throws SQLException {
        PreparedStatement pst = conn.prepareStatement("select expt_id from experiment order by expt_id");
        ResultSet rs = pst.executeQuery();
        List<Long> ret = new ArrayList<Long>();
        while (rs.next()) {
            ret.add(rs.getLong("expt_id"));
        }
        pst.close();
        return ret;
    }

    /**
     * Return a count of all experiments.
     *
     * @return
     * @throws SQLException
     */
    public int getExperimentCount() throws SQLException {
        PreparedStatement pst = conn.prepareStatement("select count(distinct expt_id) from experiment;");
        ResultSet rs = pst.executeQuery();
        int n = 0;
        while (rs.next()) n = rs.getInt(1);
        pst.close();
        return n;
    }

    /**
     * Return a count of all experiment data values.
     *
     * @return
     * @throws SQLException
     */
    public int getExperimentDataCount() throws SQLException {
        PreparedStatement pst = conn.prepareStatement("select count(expt_data_id) from experiment_data;");
        ResultSet rs = pst.executeQuery();
        int n = 0;
        while (rs.next()) n = rs.getInt(1);
        pst.close();
        return n;
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
        PreparedStatement pst = conn.prepareStatement("select assay_id from assay where summary = ?");
        pst.setLong(1, aid);
        ResultSet rs = pst.executeQuery();
        List<Long> aids = new ArrayList<Long>();
        while (rs.next()) aids.add(rs.getLong(1));
        p.setAids(aids);

        // get probe ids
        List<Long> probeids = getProbesForProject(aid);
        p.setProbeIds(probeids);

        pst.close();
        return p;
    }

    public List<Long> getProbesForProject(Long aid) throws SQLException {
        PreparedStatement pst = conn.prepareStatement("select a.cid from experiment_data a, compound b where b.probe_id is not null and a.eid = ? and a.cid = b.cid");
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

    public List<Publication> searchForPublication(String query, int skip, int top) throws SQLException, IOException {
        List<String> validFields = Arrays.asList("pmid", "title", "abstract", "doi");
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
            pst = conn.prepareStatement("select pmid from publication where (pmid like ? or title like ? or abstract like ? or doi like ?) order by pmid " + limitClause);
            pst.setString(1, q);
            pst.setString(2, q);
            pst.setString(3, q);
            pst.setString(4, q);
        } else {
            String[] toks = query.split("\\[");
            String q = toks[0].trim();
            String field = toks[1].trim().replace("]", "");
            if (!validFields.contains(field)) throw new SQLException("Invalid field was specified");
            String sql = "select pmid from publication  " + field + " like '%" + q + "%' order by pmid " + limitClause;
            pst = conn.prepareStatement(sql);
        }

        ResultSet rs = pst.executeQuery();
        List<Publication> publications = new ArrayList<Publication>();
        while (rs.next()) {
            Long pmid = rs.getLong("pmid");
            publications.add(getPublicationByPmid(pmid));
        }
        pst.close();
        return publications;
    }

}
