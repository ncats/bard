package gov.nih.ncgc.bard.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nih.ncgc.bard.capextract.CAPAssayAnnotation;
import gov.nih.ncgc.bard.capextract.CAPDictionary;
import gov.nih.ncgc.bard.entity.Assay;
import gov.nih.ncgc.bard.entity.BardEntity;
import gov.nih.ncgc.bard.entity.Compound;
import gov.nih.ncgc.bard.entity.Experiment;
import gov.nih.ncgc.bard.entity.ExperimentData;
import gov.nih.ncgc.bard.entity.Project;
import gov.nih.ncgc.bard.entity.ProteinTarget;
import gov.nih.ncgc.bard.entity.Publication;
import gov.nih.ncgc.bard.entity.Substance;
import gov.nih.ncgc.bard.rest.rowdef.AssayDefinitionObject;
import gov.nih.ncgc.bard.rest.rowdef.DataResultObject;
import gov.nih.ncgc.bard.rest.rowdef.DoseResponseResultObject;

import javax.sql.DataSource;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Reader;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility methods to interact with the database backend.
 *
 * @author Rajarshi Guha
 */
public class DBUtils {
    Connection conn;
    Map<Class, Query> fieldMap;

    class Query {
        List<String> validFields;
        String orderField, tableName, idField;

        Query(List<String> validFields, String orderField, String idField, String tableName) {
            this.validFields = validFields;
            this.orderField = orderField;
            this.tableName = tableName;

            if (idField == null) this.idField = orderField;
        }

        public List<String> getValidFields() {
            return validFields;
        }

        public String getOrderField() {
            return orderField;
        }

        public String getTableName() {
            return tableName;
        }

        public String getIdField() {
            return idField;
        }
    }

    public DBUtils() {
        final List<String> publicationFields = Arrays.asList("pmid", "title", "abstract", "doi");
        final List<String> projectFields = Arrays.asList("name", "description");
        final List<String> targetFields = Arrays.asList("accession", "name", "description", "uniprot_status");
        final List<String> experimentFields = Arrays.asList("name", "description", "source", "grant_no");
        final List<String> compoundFields = Arrays.asList("url");
        final List<String> substanceFields = Arrays.asList("url");
        final List<String> assayFields = Arrays.asList("name", "description", "protocol", "comemnt", "source", "grant_no");
        final List<String> edFields = Arrays.asList();

        fieldMap = new HashMap<Class, Query>() {{
            put(Publication.class, new Query(publicationFields, "pmid", null, "publication"));
            put(Project.class, new Query(projectFields, "proj_id", null, "project"));
            put(ProteinTarget.class, new Query(targetFields, "accession", null, "protein_target"));
            put(Experiment.class, new Query(experimentFields, "expt_id", null, "experiment"));
            put(Compound.class, new Query(compoundFields, "cid", null, "compound"));
            put(Substance.class, new Query(substanceFields, "sid", null, "substance"));
            put(Assay.class, new Query(assayFields, "assay_id", null, "assay"));
            put(ExperimentData.class, new Query(edFields, "expt_data_id", null, "experiment_data"));
        }};

        conn = getConnection();
    }

    /**
     * Indicates whether the database connection is ready / valid.
     *
     * @return <code>true</code> if there is a valid connection to the database, otherwise false
     */
    public boolean ready() {
        return conn != null;
    }

    public void closeConnection() throws SQLException {
        if (!conn.isClosed())
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
            System.err.println("Not running in Tomcat/Jetty/Glassfish or other app container?");
            return null;
        }
    }

    protected void setConnection(Connection conn) {
        this.conn = conn;
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

    /**
     * Obtain compounds based on their CIDs.
     *
     * @param cids one or more CIDs. If any CID is null, then the function returns null
     * @return a list of {@link Compound} objects
     * @throws SQLException
     */
    public List<Compound> getCompoundsByCid(Long... cids) throws SQLException {
        if (cids == null || cids.length < 0) return null;
        for (Long acid : cids) {
            if (acid == null) return null;
        }
        List<List<Long>> chunks = Util.chunk(cids, 100);
        List<Compound> compounds = new ArrayList<Compound>();
        for (List<Long> chunk : chunks) {

            String cidClause = Util.join(chunk, ",");
            String sql = "select c.*, s.sid from compound c, cid_sid s where c.cid in (" + cidClause + ") and c.cid = s.cid order by c.cid";
            PreparedStatement pst = conn.prepareStatement(sql);
            ResultSet rs = pst.executeQuery();

            List<Compound> tmp = new ArrayList<Compound>();
            Compound c = new Compound();
            List<Long> sids = new ArrayList<Long>();
            Long oldCid = -1L;
            boolean first = true;
            while (rs.next()) {
                Long cid = rs.getLong("cid");
                String probeId = rs.getString("probe_id");
                String smiles = rs.getString("iso_smiles");

                if (!first && !cid.equals(oldCid)) {
                    oldCid = cid;
                    c.setSids(sids);
                    tmp.add(c);
                    sids = new ArrayList<Long>();
                    c = new Compound();
                }
                c.setCid(cid);
                c.setProbeId(probeId);
                c.setSmiles(smiles);
                sids.add(rs.getLong("sid"));

                if (first) {
                    oldCid = cid;
                    first = false;
                }
            }

            if (sids.size() > 0) {
                c.setSids(sids);
                tmp.add(c);
            }

            pst.close();
            compounds.addAll(tmp);
        }
        return compounds;
    }

    /**
     * Get {@link Compound} instances based on names.
     * <p/>
     * <b>TODO</b>
     * In this case, we have to perform an SQL query for each supplied name as
     * we are employing the full text query facility. It might be possible to
     * enhance the performance by OR'ing the supplied names together.
     *
     * @param names an array of names
     * @return a list of {@link Compound} objects
     * @throws SQLException
     */

    public List<Compound> getCompoundsByName(String... names) throws SQLException {
        if (names == null || names.length == 0) return null;
        List<Compound> cmpds = new ArrayList<Compound>();
        PreparedStatement pst = conn.prepareStatement("select distinct id from synonyms where type = 1 and match(syn) against (? in boolean mode)");
        ResultSet rs;
        for (String name : names) {
            pst.setString(1, name);
            rs = pst.executeQuery();
            while (rs.next()) cmpds.addAll(getCompoundsByCid(new Long[]{rs.getLong(1)}));
        }
        return cmpds;
    }

    public List<Compound> getCompoundsBySid(Long... sids) throws SQLException {
        if (sids == null || sids.length == 0) return null;
        List<List<Long>> chunks = Util.chunk(sids, 100);
        List<Compound> cmpds = new ArrayList<Compound>();
        for (List<Long> chunk : chunks) {
            String sidClause = Util.join(chunk, ",");
            String sql = "select cid from cid_sid s where s.sid in (" + sidClause + ")";
            PreparedStatement pst = conn.prepareStatement(sql);
            List<Long> cids = new ArrayList<Long>();
            ResultSet rs = pst.executeQuery();
            while (rs.next()) cids.add(rs.getLong("cid"));
            cmpds.addAll(getCompoundsByCid(cids.toArray(new Long[]{})));
            pst.close();
        }
        return cmpds;
    }

    public List<Compound> getCompoundsByProbeId(String... probeids) throws SQLException {
        if (probeids == null || probeids.length == 0) return null;
        List<List<String>> chunks = Util.chunk(probeids, 100);
        List<Compound> compounds = new ArrayList<Compound>();
        for (List<String> chunk : chunks) {
            List<String> qprobeids = new ArrayList<String>();
            for (String pid : probeids) qprobeids.add("'" + pid + "'");
            String probeidClause = Util.join(qprobeids, ",");
            String sql = "select c.*, s.sid from compound c, cid_sid s where probe_id in (" + probeidClause + ") and c.cid = s.cid order by c.cid";
            PreparedStatement pst = conn.prepareStatement(sql);
            ResultSet rs = pst.executeQuery();

            List<Compound> tmp = new ArrayList<Compound>();
            Compound c = new Compound();
            List<Long> sids = new ArrayList<Long>();
            Long oldCid = -1L;
            boolean first = true;
            while (rs.next()) {
                Long cid = rs.getLong("cid");
                String probeId = rs.getString("probe_id");
                String smiles = rs.getString("iso_smiles");

                if (!first && !cid.equals(oldCid)) {
                    oldCid = cid;
                    c.setSids(sids);
                    tmp.add(c);
                    sids = new ArrayList<Long>();
                    c = new Compound();
                }
                c.setCid(cid);
                c.setProbeId(probeId);
                c.setSmiles(smiles);
                sids.add(rs.getLong("sid"));

                if (first) {
                    oldCid = cid;
                    first = false;
                }
            }

            if (sids.size() > 0) {
                c.setSids(sids);
                tmp.add(c);
            }

            pst.close();
            compounds.addAll(tmp);
        }
        return compounds;
    }

    /**
     * Extract the measured results for a substance in an experiment.
     * <p/>
     * The identifier will usually be obtained via {@link #getExperimentData(Long, int, int)} using the
     * experiment identifier.
     * <p/>
     * This method returns an {@link ExperimentData} object that contains the high level summary of the
     * result (score, potency, outcome etc) as well as the actual measured data which may be single
     * point or dose response.
     *
     * @param edid The experiment data identifier
     * @return
     * @throws SQLException
     */
    public ExperimentData getExperimentDataByDataId(Long edid) throws SQLException, IOException {
        if (edid == null || edid <= 0) return null;
        PreparedStatement pst = conn.prepareStatement("select * from experiment_data a, experiment_result b, experiment c where a.expt_data_id = ? and a.expt_data_id = b.expt_data_id and a.eid = c.expt_id");
        pst.setLong(1, edid);
        ResultSet rs = pst.executeQuery();
        ExperimentData ed = new ExperimentData();
        ed.setExptDataId(edid);
        while (rs.next()) {
            ed.setEid(rs.getLong("eid"));
            ed.setSid(rs.getLong("sid"));
            ed.setCid(rs.getLong("cid"));

            Integer classification = rs.getInt("classification");
            if (rs.wasNull()) classification = null;
            ed.setClassification(classification);

            ed.setUpdated(rs.getDate("updated"));
            ed.setOutcome(rs.getInt("outcome"));
            ed.setScore(rs.getInt("score"));

            Float potency = rs.getFloat("potency");
            if (rs.wasNull()) potency = null;
            ed.setPotency(potency);

            Blob blob = rs.getBlob("json_data_array");
            byte[] bytes = blob.getBytes(1, (int) blob.length());
            String s = new String(bytes);

            ObjectMapper mapper = new ObjectMapper();
            DataResultObject[] o = mapper.readValue(s, DataResultObject[].class);

            blob = rs.getBlob("assay_result_def");
            s = new String(blob.getBytes(1, (int) blob.length()));
            AssayDefinitionObject[] ado = mapper.readValue(s, AssayDefinitionObject[].class);

            DoseResponseResultObject[] dro = null;
            blob = rs.getBlob("json_dose_response");
            if (blob != null) {
                bytes = blob.getBytes(1, (int) blob.length());
                s = new String(bytes);
                dro = mapper.readValue(s, DoseResponseResultObject[].class);

                // for each dose-response 'layer', try and pull a layer label from the assay definition.
                for (DoseResponseResultObject adro : dro) {
                    String tid = adro.getTid();
                    for (AssayDefinitionObject aado : ado) {
                        if (aado.getTid().equals(tid)) {
                            adro.setLabel(aado.getName());
                            adro.setDescription(aado.getDescription());
                            adro.setConcUnit(aado.getTestConcUnit());
                        }
                    }
                }
            }
            ed.setDr(dro);
            ed.setResults(o);
        }
        pst.close();
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
    public List<ExperimentData> getExperimentData(Long eid, int skip, int top) throws SQLException, IOException {
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
     * Get a substance by its SID.
     *
     * @param sid the SID in question
     * @return a {@link Substance} object
     * @throws SQLException TODO Should include CID and also include SMILES from CID (rel_type=1)
     */
    public Substance getSubstanceBySid(Long sid) throws SQLException {
        if (sid == null || sid < 0) return null;
        PreparedStatement pst = conn.prepareStatement("select a.*, b.cid, c.iso_smiles from substance a, cid_sid b, compound c where a.sid = ? and a.sid = b.sid and b.rel_type = 1 and c.cid = b.cid");
//        PreparedStatement pst = conn.prepareStatement("select a.* from substance a where a.sid = ? ");
        pst.setLong(1, sid);
        ResultSet rs = pst.executeQuery();
        Substance s = new Substance();
        while (rs.next()) {
            s.setDepRegId(rs.getString("dep_regid"));
            s.setSourceName(rs.getString("source_name"));
            s.setUrl(rs.getString("substance_url"));
            s.setSid(sid);
            s.setDeposited(rs.getDate("deposited"));
            s.setUpdated(rs.getDate("updated"));
            String pidText = rs.getString("patent_ids");
            if (pidText != null) s.setPatentIds(pidText.split("\\s+"));

            s.setCid(rs.getLong("cid"));
            s.setSmiles(rs.getString("iso_smiles"));
        }
        return s;
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
    public List<ExperimentData> getSubstanceData(Long sid, int skip, int top) throws SQLException, IOException {
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
     * Return experiment ids for a compound.
     *
     * @param cid  The Pubchem CID
     * @param skip how many records to skip
     * @param top  how many records to return
     * @return
     * @throws SQLException
     */
    public List<Long> getCompoundExperimentIds(Long cid, int skip, int top) throws SQLException {
        if (cid == null || cid < 0) return null;

        String limitClause = "";
        if (skip != -1) {
            if (top <= 0) throw new SQLException("If skip != -1, top must be greater than 0");
            limitClause = "  limit " + skip + "," + top;
        }

        PreparedStatement pst = conn.prepareStatement("select distinct(eid) from experiment_data where cid = ? order by eid " + limitClause);
        pst.setLong(1, cid);
        ResultSet rs = pst.executeQuery();
        List<Long> ret = new ArrayList<Long>();
        while (rs.next()) ret.add(rs.getLong(1));
        pst.close();
        return ret;
    }

    /**
     * Return experiment ids for a substance.
     *
     * @param sid  The Pubchem SID
     * @param skip how many records to skip
     * @param top  how many records to return
     * @return
     * @throws SQLException
     */
    public List<Long> getSubstanceExperimentIds(Long sid, int skip, int top) throws SQLException {
        if (sid == null || sid < 0) return null;

        String limitClause = "";
        if (skip != -1) {
            if (top <= 0) throw new SQLException("If skip != -1, top must be greater than 0");
            limitClause = "  limit " + skip + "," + top;
        }

        PreparedStatement pst = conn.prepareStatement("select distinct(eid) from experiment_data where sid = ? order by eid " + limitClause);
        pst.setLong(1, sid);
        ResultSet rs = pst.executeQuery();
        List<Long> ret = new ArrayList<Long>();
        while (rs.next()) ret.add(rs.getLong(1));
        pst.close();
        return ret;
    }

    /**
     * Return experiment objects for a compound.
     *
     * @param cid  The Pubchem CID
     * @param skip how many records to skip
     * @param top  how many records to return
     * @return
     * @throws SQLException
     */
    public List<Experiment> getCompoundExperiment(Long cid, int skip, int top) throws SQLException {
        if (cid == null || cid < 0) return null;

        String limitClause = "";
        if (skip != -1) {
            if (top <= 0) throw new SQLException("If skip != -1, top must be greater than 0");
            limitClause = "  limit " + skip + "," + top;
        }

        PreparedStatement pst = conn.prepareStatement("select distinct(eid) from experiment_data where cid = ? order by eid " + limitClause);
        pst.setLong(1, cid);
        ResultSet rs = pst.executeQuery();
        List<Experiment> ret = new ArrayList<Experiment>();
        while (rs.next()) ret.add(getExperimentByExptId(rs.getLong(1)));
        pst.close();
        return ret;
    }

    /**
     * Return experiment objects for a subtstance.
     *
     * @param sid  The Pubchem SID
     * @param skip how many records to skip
     * @param top  how many records to return
     * @return
     * @throws SQLException
     */
    public List<Experiment> getSubstanceExperiment(Long sid, int skip, int top) throws SQLException {
        if (sid == null || sid < 0) return null;

        String limitClause = "";
        if (skip != -1) {
            if (top <= 0) throw new SQLException("If skip != -1, top must be greater than 0");
            limitClause = "  limit " + skip + "," + top;
        }

        PreparedStatement pst = conn.prepareStatement("select distinct(eid) from experiment_data where sid = ? order by eid " + limitClause);
        pst.setLong(1, sid);
        ResultSet rs = pst.executeQuery();
        List<Experiment> ret = new ArrayList<Experiment>();
        while (rs.next()) ret.add(getExperimentByExptId(rs.getLong(1)));
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
    public List<ExperimentData> getCompoundData(Long cid, int skip, int top) throws SQLException, IOException {
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
            ret.addAll(getCompoundsByCid(rs.getLong("cid")));
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
            // TODO should return a Substance entity
            ret.addAll(getCompoundsBySid(rs.getLong("sid")));
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
     * This method is limited and should not be used. Instead consider the <code>/search</code>
     * resource ({@link gov.nih.ncgc.bard.rest.BARDSearchResource})
     *
     * @param query the query to use
     * @return A list of {@link Assay} objects, whuich may be empty if no assays match the query.
     * @deprecated
     */
    public List<Assay> searchForAssay(String query) throws SQLException {
        boolean freeTextQuery = false;

        if (!query.contains("[")) freeTextQuery = true;

        PreparedStatement pst = null;
        if (freeTextQuery) {
            String q = "%" + query + "%";
            pst = conn.prepareStatement("select assay_id from assay where (name like ? or description like ? or source like ? or grant_no like ? or protocol like ?)");
            pst.setString(1, q);
            pst.setString(2, q);
            pst.setString(3, q);
            pst.setString(4, q);
            pst.setString(5, q);
        } else {
            String[] toks = query.split("\\[");
            String q = toks[0].trim();
            String field = toks[1].trim().replace("]", "");
            String sql = "select assay_id from assay where " + field + " like '%" + q + "%'";
            pst = conn.prepareStatement(sql);
        }

        ResultSet rs = pst.executeQuery();
        List<Assay> assays = new ArrayList<Assay>();
        while (rs.next()) {
            Long aid = rs.getLong("assay_id");
            assays.add(getAssayByAid(aid));
        }
        pst.close();

        return assays;
    }

    public List<Assay> getAssaysByTargetAccession(String acc) throws SQLException {
        PreparedStatement pst = conn.prepareStatement("select distinct b.aid from protein_target a, assay_target b where a.accession = ? and a.accession = b.accession");
        pst.setString(1, acc);
        List<Assay> assays = new ArrayList<Assay>();
        ResultSet rs = pst.executeQuery();
        while (rs.next()) assays.add(getAssayByAid(rs.getLong(1)));
        pst.close();
        return assays;
    }

    public List<Assay> getAssaysByTargetGeneid(Long geneid) throws SQLException {
        PreparedStatement pst = conn.prepareStatement("select distinct b.aid from protein_target a, assay_target b where a.gene_id = ? and a.accession = b.accession");
        pst.setLong(1, geneid);
        List<Assay> assays = new ArrayList<Assay>();
        ResultSet rs = pst.executeQuery();
        while (rs.next()) assays.add(getAssayByAid(rs.getLong(1)));
        pst.close();
        return assays;
    }

    public List<Long> getProjectIds() throws SQLException {
        PreparedStatement pst = conn.prepareStatement("select proj_id from project");
        ResultSet rs = pst.executeQuery();
        List<Long> ret = new ArrayList<Long>();
        while (rs.next()) {
            ret.add(rs.getLong(1));
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

    // TOOD handle depositor id - should resolve the integer to a string
    public Project getProject(Long projectId) throws SQLException {
        Project p = new Project();
        p.setProjectId(projectId);

        PreparedStatement pst = conn.prepareStatement("select * from project where proj_id = ?");
        pst.setLong(1, projectId);
        ResultSet rs = pst.executeQuery();
        while (rs.next()) {
            p.setDescription(rs.getString("description"));
            p.setName(rs.getString("name"));
            p.setDeposited(rs.getDate("create_date"));
        }
        pst.close();

        // find all experiments for this project
        pst = conn.prepareStatement("select expt_id from project_experiment where proj_id = ?");
        pst.setLong(1, projectId);
        rs = pst.executeQuery();
        List<Long> eids = new ArrayList<Long>();
        while (rs.next()) eids.add(rs.getLong(1));
        p.setEids(eids);
        pst.close();

        return p;
    }

    public List<Project> getProjects(Long... projectIds) throws SQLException {
        List<Project> p = new ArrayList<Project>();
        for (Long pid : projectIds) p.add(getProject(pid));
        return p;
    }

    /**
     * Returns a list of {@link Project} objects that are associated with an experiment.
     * <p/>
     * It is possible, that an experiment is not assigned to a project ("orphan"), in
     * which case the project id is -1.
     * <p/>
     *
     * @param eid The experiment id for the summary assay
     * @return A {@link Project} object
     * @throws SQLException
     */
    public List<Project> getProjectByExperimentId(Long eid) throws SQLException {

        PreparedStatement pst = conn.prepareStatement("select proj_id from project_experiment where expt_id = ?");
        pst.setLong(1, eid);
        ResultSet rs = pst.executeQuery();
        List<Project> ps = new ArrayList<Project>();
        while (rs.next()) {
            Long projectId = rs.getLong("proj_id");
            ps.add(getProject(projectId));
        }
        pst.close();
        return ps;
    }

    public List<Project> getProjectByCompoundId(Long cid) throws SQLException {
        PreparedStatement pst = conn.prepareStatement("select p.proj_id from project p, experiment e where e.expt_id in (select distinct ed.eid from experiment_data ed, experiment e, compound a where a.cid = ? and ed.cid = a.cid and ed.eid = e.expt_id) and e.proj_id = p.proj_id");
        pst.setLong(1, cid);
        ResultSet rs = pst.executeQuery();
        List<Long> pids = new ArrayList<Long>();
        while (rs.next()) pids.add(rs.getLong("proj_id"));
        pst.close();
        return getProjects(pids.toArray(new Long[]{}));
    }

    public List<Long> getProbesForProject(Long aid) throws SQLException {
        PreparedStatement pst = conn.prepareStatement("select a.cid from experiment_data a, compound b where b.probe_id is not null and a.eid = ? and a.cid = b.cid");
        pst.setLong(1, aid);
        ResultSet rs = pst.executeQuery();
        List<Long> probeids = new ArrayList<Long>();
        while (rs.next()) probeids.add(rs.getLong("cid"));
        return probeids;
    }

    /* ****************/
    /*  Query methods */
    /* ****************/

    public List<ExperimentData> searchForExperimentData(String query, int skip, int top) throws SQLException, IOException {
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
     * Get a count of the instances of an entity currently stored in the database.
     *
     * @param klass The class of the entity to be counted
     * @return the count of the instances present
     * @throws SQLException if there is an error in the query
     */
    public <T extends BardEntity> int getEntityCount(Class<T> klass) throws SQLException {
        Query queryParams;
        if (fieldMap.containsKey(klass)) queryParams = fieldMap.get(klass);
        else throw new IllegalArgumentException("Invalid entity class was specified");

        String sql = "select count(" + queryParams.getIdField() + ") from " + queryParams.getTableName();
        PreparedStatement pst = conn.prepareStatement(sql);
        ResultSet rs = pst.executeQuery();
        int n = 0;
        while (rs.next()) n = rs.getInt(1);
        pst.close();
        return (n);
    }

    /**
     * Search for entities.
     * <p/>
     * The <code>skip</code> and <code>top</code> arguments can be used to
     * implement paging. This implies that the SQL query orders the result
     * and this is currently pre-defined by the API.
     *
     * @param query The query string, possible suffixed by search fields. If <code>null</code>
     *              the method returns all entities available. In such a case, the <code>skip</code>
     *              and <code>top</code> parameters should be specified (though this is not currently
     *              enforce)
     * @param skip  How many entities to skip
     * @param top   How many entities to return
     * @param klass The class of the entity desired
     * @return A list of entities matching the query
     * @throws SQLException if there is an error during query
     * @throws IOException  if there is an error during query
     */
    public <T extends BardEntity> List<T> searchForEntity(String query, int skip, int top, Class<T> klass) throws SQLException, IOException {
        Query queryParams;
        if (fieldMap.containsKey(klass)) queryParams = fieldMap.get(klass);
        else return new ArrayList<T>();

        String limitClause = "";
        if (skip != -1) {
            if (top <= 0) throw new SQLException("If skip != -1, top must be greater than 0");
            limitClause = "  limit " + skip + "," + top;
        }

        PreparedStatement pst;
        String sql;
        if (query == null && top > 0) { // get all rows - caller had better implement paging
            sql = "select " + queryParams.getIdField() + " from " + queryParams.getTableName() + " order by " + queryParams.getOrderField() + " " + limitClause;
        } else if (!query.contains("[")) {
            String q = "'%" + query + "%' ";
            List<String> tmp = new ArrayList<String>();
            for (String s : queryParams.getValidFields()) tmp.add(s + " like " + q);
            String tmp2 = Util.join(tmp, " or ");

            sql = "select " + queryParams.getIdField() + " from " + queryParams.getTableName() + " where (" + tmp2 + ") order by " + queryParams.getOrderField() + " " + limitClause;
        } else {
            // TODO we currently only assume a single query field is specified
            String[] toks = query.split("\\[");
            String q = toks[0].trim();
            String field = toks[1].trim().replace("]", "");
            if (!queryParams.getValidFields().contains(field)) throw new SQLException("Invalid field was specified");
            sql = "select " + queryParams.getIdField() + " from " + queryParams.getTableName() + " where " + field + " like '%" + q + "%' order by " + queryParams.getOrderField() + "  " + limitClause;
        }
        pst = conn.prepareStatement(sql);
        ResultSet rs = pst.executeQuery();
        List<T> entities = new ArrayList<T>();
        while (rs.next()) {
            Object id = rs.getObject(queryParams.getIdField());
            Object entity = null;
            if (klass.equals(Publication.class)) entity = getPublicationByPmid((Long) id);
            else if (klass.equals(ProteinTarget.class)) entity = getProteinTargetByAccession((String) id);
            else if (klass.equals(Project.class)) entity = getProjectByExperimentId((Long) id);
            else if (klass.equals(Experiment.class)) entity = getExperimentByExptId((Long) id);
            else if (klass.equals(Compound.class)) entity = getCompoundsByCid((Long) id);
            else if (klass.equals(Assay.class)) entity = getAssayByAid((Long) id);
            if (entity != null) {
                if (entity instanceof List) entities.addAll((Collection<T>) entity);
                else if (entity instanceof BardEntity) entities.add((T) entity);
            }
        }
        pst.close();
        return entities;
    }

    /**
     * **********************************************************************
     * <p/>
     * CAP related methods (dictionary, annotations)
     * <p/>
     * ************************************************************************
     */

    public List<CAPAssayAnnotation> getAssayAnnotations(Long assayId) throws SQLException {
        PreparedStatement pst = conn.prepareStatement("select * from cap_annotation where assay_id = ?");
        pst.setLong(1, assayId);
        ResultSet rs = pst.executeQuery();
        List<CAPAssayAnnotation> annos = new ArrayList<CAPAssayAnnotation>();
        while (rs.next()) {
            String anno_id = rs.getString("anno_id");
            String anno_key = rs.getString("anno_key");
            String anno_value = rs.getString("anno_value");
            String anno_display = rs.getString("anno_display");
            String source = rs.getString("source");

            String related = rs.getString("related");
            String extValueId = null;
            if (related != null && !related.trim().equals("")) {
                String[] toks = related.split("\\|");
                if (toks.length == 2) extValueId = toks[1];
            }
            CAPAssayAnnotation anno = new CAPAssayAnnotation(anno_id, null, anno_display, null, anno_key, anno_value, extValueId, source);
            annos.add(anno);
        }
        pst.close();
        return annos;
    }

    public CAPDictionary getCAPDictionary() throws SQLException, IOException, ClassNotFoundException {
        PreparedStatement pst = conn.prepareStatement("select dict from cap_dict order by ins_date desc");
        ResultSet rs = pst.executeQuery();
        rs.next();
        byte[] buf = rs.getBytes(1);
        ObjectInputStream objectIn = null;
        if (buf != null)
            objectIn = new ObjectInputStream(new ByteArrayInputStream(buf));
        Object o = objectIn.readObject();
        pst.close();
        if (!(o instanceof CAPDictionary)) return null;
        return (CAPDictionary) o;
    }

    /**
     * Get an estimated of the rows to be returned by a query.
     * <p/>
     * This obtains a rough row count via MySQL's EXPLAIN functionality
     *
     * @param query The SQL query
     * @return the number of estimated rows
     * @throws SQLException
     */
    public int getEstimatedRowCount(String query) throws SQLException {
        PreparedStatement pst = conn.prepareStatement(query);
        ResultSet rs = pst.executeQuery();
        int nrow = -1;
        while (rs.next()) {
            nrow = rs.getInt("rows");
        }
        pst.close();
        return nrow;
    }

    /**
     * Get an estimated of the rows to be returned by a query.
     * <p/>
     * This obtains a rough row count via MySQL's EXPLAIN functionality
     *
     * @param query The SQL query
     * @return the number of estimated rows
     * @throws SQLException
     */
    public int getEstimatedRowCount(PreparedStatement query) throws SQLException {
        ResultSet rs = query.executeQuery();
        int nrow = -1;
        while (rs.next()) {
            nrow = rs.getInt("rows");
        }
        return nrow;
    }

}
