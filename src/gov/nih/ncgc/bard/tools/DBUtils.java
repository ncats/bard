package gov.nih.ncgc.bard.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nih.ncgc.bard.capextract.CAPAssayAnnotation;
import gov.nih.ncgc.bard.capextract.CAPDictionary;
import gov.nih.ncgc.bard.entity.Assay;
import gov.nih.ncgc.bard.entity.BardEntity;
import gov.nih.ncgc.bard.entity.Compound;
import gov.nih.ncgc.bard.entity.ETag;
import gov.nih.ncgc.bard.entity.Experiment;
import gov.nih.ncgc.bard.entity.ExperimentData;
import gov.nih.ncgc.bard.entity.Project;
import gov.nih.ncgc.bard.entity.ProteinTarget;
import gov.nih.ncgc.bard.entity.Publication;
import gov.nih.ncgc.bard.entity.Substance;
import gov.nih.ncgc.bard.rest.rowdef.AssayDefinitionObject;
import gov.nih.ncgc.bard.rest.rowdef.DataResultObject;
import gov.nih.ncgc.bard.rest.rowdef.DoseResponseResultObject;
import gov.nih.ncgc.bard.search.Facet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Reader;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Utility methods to interact with the database backend.
 *
 * @author Rajarshi Guha
 */
public class DBUtils {
    /*
     * maximum size for an ETag
     */
    static final int MAX_ETAG_SIZE = 10000;
    static final int CHUNK_SIZE = 400;

    Logger log;
    Connection conn;
    Map<Class, Query> fieldMap;
    SecureRandom rand = new SecureRandom();

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
        log = LoggerFactory.getLogger(this.getClass());

        final List<String> publicationFields = Arrays.asList("pmid", "title", "abstract", "doi");
        final List<String> projectFields = Arrays.asList("name", "description");
        final List<String> targetFields = Arrays.asList("accession", "name", "description", "uniprot_status");
        final List<String> experimentFields = Arrays.asList("name", "description", "source", "grant_no");
        final List<String> compoundFields = Arrays.asList("url");
        final List<String> substanceFields = Arrays.asList("url");
        final List<String> assayFields = Arrays.asList("name", "description", "protocol", "comemnt", "source", "grant_no");
        final List<String> edFields = Arrays.asList();
        final List<String> etagFields = Arrays.asList("name", "type");

        fieldMap = new HashMap<Class, Query>() {{
            put(Publication.class, new Query(publicationFields, "pmid", null, "publication"));
            put(Project.class, new Query(projectFields, "proj_id", null, "project"));
            put(ProteinTarget.class, new Query(targetFields, "accession", null, "protein_target"));
            put(Experiment.class, new Query(experimentFields, "expt_id", null, "experiment"));
            put(Compound.class, new Query(compoundFields, "cid", null, "compound"));
            put(Substance.class, new Query(substanceFields, "sid", null, "substance"));
            put(Assay.class, new Query(assayFields, "assay_id", null, "assay"));
            put(ExperimentData.class, new Query(edFields, "expt_data_id", null, "experiment_data"));
            put(ETag.class, new Query(etagFields, "etag_id", null, "etag"));
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
        Connection con = null;
        try {
            initContext = new javax.naming.InitialContext();
            DataSource ds = (javax.sql.DataSource)
                    initContext.lookup("java:comp/env/jdbc/bardman");
            con = ds.getConnection();
            con.setAutoCommit(false);
        } catch (Exception ex) {
            // try 
            try {
                initContext = new javax.naming.InitialContext();
                DataSource ds = (javax.sql.DataSource)
                        initContext.lookup("jdbc/bardman");
                con = ds.getConnection();
                con.setAutoCommit(false);
            } catch (Exception e) {
                System.err.println("Not running in Tomcat/Jetty/Glassfish or other app container?");
                e.printStackTrace();
            }
        }
        return con;
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
        rs.close();
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
        rs.close();
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
        rs2.close();
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
        rs.close();
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
    public List<Compound> getCompoundsByCid(Long... cids)
            throws SQLException {

        if (cids == null || cids.length < 0) return null;
        for (Long acid : cids) {
            if (acid == null) return null;
        }
        List<List<Long>> chunks = Util.chunk(cids, CHUNK_SIZE);
        List<Compound> compounds = new ArrayList<Compound>();
        for (List<Long> chunk : chunks) {
            String cidClause = Util.join(chunk, ",");
            String sql = ("select a.*,b.* from compound a, compound_props b "
                    + "where a.cid in (" + cidClause + ") and "
                    + "b.pubchem_compound_cid = a.cid");
            Statement stm = conn.createStatement();
            try {
                ResultSet rs = stm.executeQuery(sql);
                while (rs.next()) {
                    Compound c = new Compound();
                    c.setCid(rs.getLong("cid"));
                    fillCompound(rs, c);
                    compounds.add(c);
                }
                rs.close();

                // get Sids and annotations
                for (Compound c : compounds) {
                    c.setSids(getSidsByCid(c.getCid()));
                    Map<String, String[]> annots = getCompoundAnnotations(c.getCid());
                    c.setAnno_key(annots.get("anno_key"));
                    c.setAnno_val(annots.get("anno_val"));
                }


            } finally {
                stm.close();
            }
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
            while (rs.next())
                cmpds.addAll(getCompoundsByCid(new Long[]{rs.getLong(1)}));
            rs.close();
        }
        return cmpds;
    }

    public List<Compound> getCompoundsBySid(Long... sids) throws SQLException {
        if (sids == null || sids.length == 0) return null;
        List<List<Long>> chunks = Util.chunk(sids, CHUNK_SIZE);
        List<Compound> cmpds = new ArrayList<Compound>();
        for (List<Long> chunk : chunks) {
            String sidClause = Util.join(chunk, ",");
            String sql = "select cid from cid_sid s where s.sid in (" + sidClause + ")";
            PreparedStatement pst = conn.prepareStatement(sql);
            List<Long> cids = new ArrayList<Long>();
            Set<Long> unique = new HashSet<Long>();
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                long cid = rs.getLong("cid");
                if (!unique.contains(cid)) {
                    unique.add(cid);
                    cids.add(cid);
                }
            }
            rs.close();
            cmpds.addAll(getCompoundsByCid(cids.toArray(new Long[]{})));
            pst.close();
        }
        return cmpds;
    }


    public String newETag(String name, String clazz) throws SQLException {
        if (clazz == null) {
            throw new IllegalArgumentException("Please specify the class!");
        }

        PreparedStatement pst = conn.prepareStatement
                ("insert into etag(etag_id,name,type,created,modified) "
                        + "values (?,?,?,?,?)");
        try {
            String etag = null;
            int tries = 0;
            do {
                try {
                    byte[] id = new byte[8];
                    rand.nextBytes(id);
                    etag = Util.toString(id);

                    pst.setString(1, etag);
                    pst.setString(2, name);
                    pst.setString(3, clazz);
                    Timestamp ts = new Timestamp
                            (new java.util.Date().getTime());
                    pst.setTimestamp(4, ts);
                    pst.setTimestamp(5, ts);

                    if (pst.executeUpdate() > 0) {
                    } else {
                        log.info("** Couldn't insert ETag " + etag);
                        etag = null;
                    }
                    break;
                } catch (SQLException ex) { // etag already exists
                    //ex.printStackTrace();
                    log.info("** ETag " + etag
                            + " already exists; generating a new one after "
                            + tries + " tries!");
                    etag = null;
                    ++tries;
                }
            }
            while (etag == null && tries < 5);

            if (etag != null) {
                conn.commit();
            }

            return etag;
        } finally {
            pst.close();
        }
    }

    public int createETagLinks(String etag, String... parents)
            throws SQLException {
        PreparedStatement pst = conn.prepareStatement
                ("insert into etag_link(etag_id, parent_id) values (?,?)");
        int links = 0;
        try {
            // should verify that both both parent and child are
            // of the same type

            pst.setString(1, etag);
            for (String p : parents) {
                pst.setString(2, p);
                if (pst.executeUpdate() > 0) {
                    ++links;
                }
            }

            return links;
        } finally {
            pst.close();
        }
    }

    public int putETag(String etag, Long... ids) throws SQLException {
        int cnt = 0;
        PreparedStatement pst = conn.prepareStatement
                ("select a.*,count(*) as size from etag a, etag_data b "
                        + "where a.etag_id = ? and a.etag_id = b.etag_id");
        try {
            pst.setString(1, etag);

            int size = 0;
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                String id = rs.getString("etag_id");
                size = rs.getInt("size");
                if (id == null) {
                    throw new IllegalArgumentException
                            ("Unknown ETag \"" + etag + "\"");
                }
            }
            rs.close();

            cnt = size;
            pst = conn.prepareStatement
                    ("insert into etag_data(etag_id, data_id) values (?,?)");
            pst.setString(1, etag);
            for (Long id : ids) {
                if (id != null && (cnt + 1) <= MAX_ETAG_SIZE) {
                    pst.setLong(2, id);
                    try {
                        if (pst.executeUpdate() > 0) {
                            ++cnt;
                        }
                    } catch (SQLException ex) {
                        // ignore dups...
                    }
                }
            }
            cnt -= size;

            if (cnt > 0) {
                conn.commit();

                pst = conn.prepareStatement
                        ("update etag set modified = ? where etag_id = ?");
                pst.setTimestamp(1, new java.sql.Timestamp
                        (new java.util.Date().getTime()));
                pst.setString(2, etag);
                pst.executeUpdate();
            }
        } finally {
            pst.close();
        }
        return cnt;
    }

    public void touchETag(String etag) throws SQLException {
        PreparedStatement pst = null;
        try {
            pst = conn.prepareStatement
                    ("update etag set accessed = ? where etag_id = ?");
            pst.setTimestamp(1, new java.sql.Timestamp
                    (new java.util.Date().getTime()));
            pst.setString(2, etag);
            if (pst.executeUpdate() > 0) {
            }
        } finally {
            if (pst != null) {
                pst.close();
            }
        }
    }

    public List<Compound> getCompoundsByProbeId(String... probeids)
            throws SQLException {

        if (probeids == null || probeids.length == 0) return null;
        List<List<String>> chunks = Util.chunk(probeids, CHUNK_SIZE);
        List<Compound> compounds = new ArrayList<Compound>();
        for (List<String> chunk : chunks) {
            List<String> qprobeids = new ArrayList<String>();
            for (String pid : probeids) qprobeids.add("'" + pid + "'");
            String probeidClause = Util.join(qprobeids, ",");
            String sql = "select * from compound a, compound_props b "
                    + "where probe_id in (" + probeidClause + ") "
                    + "and a.cid = b.pubchem_compound_cid";
            PreparedStatement pst = conn.prepareStatement(sql);
            try {
                ResultSet rs = pst.executeQuery();
                while (rs.next()) {
                    Compound c = new Compound();
                    fillCompound(rs, c);
                    compounds.add(c);
                }
                rs.close();
            } finally {
                pst.close();
            }
        }

        // get Sids
        for (Compound c : compounds) {
            c.setSids(getSidsByCid(c.getCid()));
        }

        return compounds;
    }

    public ETag getEtagByEtagId(String id) throws SQLException {
        PreparedStatement pst = conn.prepareStatement("select a.*, count(*) as cnt from etag a, etag_data b where a.etag_id = ? and a.etag_id = b.etag_id");
        ETag etag = new ETag();
        try {
            pst.setString(1, id);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                etag.setEtag(id);
                etag.setName(rs.getString("name"));
                etag.setType(rs.getString("type"));
                etag.setAccessed(rs.getDate("accessed"));
                etag.setCreated(rs.getDate("created"));
                etag.setModified(rs.getDate("modified"));
                etag.setCount(rs.getInt("cnt"));
            }
            rs.close();

            // pull in the children if any
            PreparedStatement pst2 = conn.prepareStatement("select * from etag_link where parent_id = ?");
            pst2.setString(1, id);
            ResultSet rs2 = pst2.executeQuery();
            List<ETag> linkedTags = new ArrayList<ETag>();
            while (rs2.next()) {
                ETag linkedTag = getEtagByEtagId(rs2.getString("etag_id"));
                if (linkedTag.getEtag() != null) linkedTags.add(linkedTag);
            }
            etag.setLinkedTags(linkedTags);
            rs2.close();
            pst2.close();
            return etag;
        } finally {
            pst.close();
        }
    }

    public Map getETagInfo(String etag) throws SQLException {
        PreparedStatement pst = conn.prepareStatement
                ("select a.*,count(*) as count from etag a, etag_data b "
                        + "where a.etag_id = ? and a.etag_id = b.etag_id");
        Map info = new HashMap();
        try {
            pst.setString(1, etag);
            ResultSet rs = pst.executeQuery();
            ResultSetMetaData meta = rs.getMetaData();
            while (rs.next()) {
                for (int c = 0; c < meta.getColumnCount(); ++c) {
                    int type = meta.getColumnType(c + 1);
                    String name = meta.getColumnName(c + 1);
                    info.put(name, rs.getString(c + 1));
                }
            }
            rs.close();

            return info;
        } finally {
            pst.close();
        }
    }

    public Facet getCompoundCollectionFacet(String etag)
            throws SQLException {
        PreparedStatement pst = conn.prepareStatement
                ("select val,count(*) as cnt from "
                        + "compound_annot a, etag_data b "
                        + "where annot_key = 'COLLECTION' "
                        + "and b.etag_id = ? "
                        + "and a.cid = b.data_id "
                        + "group by val "
                        // order don't matter because we use a hash below.. sigh
                        // +"order by cnt desc, val"
                );

        String[] wtf = new String[]{
                "NPC screening",
                "DrugBank v3.0",
                "NPC informatics",
                "INN"
        };

        try {
            pst.setString(1, etag);
            ResultSet rs = pst.executeQuery();
            Map<String, Integer> counts = new HashMap<String, Integer>();
            while (rs.next()) {
                String f = rs.getString(1);
                int cnt = rs.getInt(2);
                // sigh... 
                int n = 0;
                for (String s : wtf) {
                    if (f.startsWith(s)) {
                        ++n;
                        Integer c = counts.get(s);
                        counts.put(s, c != null ? (c + cnt) : cnt);
                    }
                }

                if (n == 0 && !f.startsWith("ChemIDPlus")) {
                    Integer c = counts.get(f);
                    counts.put(f, c != null ? (c + cnt) : cnt);
                }
            }
            rs.close();

            Facet facet = new Facet("COLLECTION");
            facet.setCounts(counts);

            return facet;
        } finally {
            pst.close();
        }
    }

    public List<Facet> getCompoundPropertyFacets(String etag)
            throws SQLException {
        Object[][] props = new Object[][]{
                {"xlogp", "PUBCHEM_XLOGP3_AA", 0},
                {"exact_mass", "PUBCHEM_EXACT_MASS", -2},
                {"mwt", "PUBCHEM_MOLECULAR_WEIGHT", -2},
                {"complexity", "PUBCHEM_CACTVS_COMPLEXITY", -2},
                {"hbond_acceptor", "PUBCHEM_CACTVS_HBOND_ACCEPTOR", 0},
                {"hbond_donnor", "PUBCHEM_CACTVS_HBOND_DONOR", 0},
                {"rotatable", "PUBCHEM_CACTVS_ROTATABLE_BOND", 0},
                {"tautomer", "PUBCHEM_CACTVS_TAUTO_COUNT", 0},
                {"tpsa", "PUBCHEM_CACTVS_TPSA", -1},
                {"mono_mwt", "PUBCHEM_MONOISOTOPIC_WEIGHT", -2}
        };

        List<Facet> facets = new ArrayList<Facet>();
        for (int i = 0; i < props.length; ++i) {
            try {
                Facet f = getCompoundPropertyFacet
                        (etag, (String) props[i][0], (String) props[i][1],
                                (Integer) props[i][2]);
                facets.add(f);
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }

        return facets;
    }

    public Facet getCompoundPropertyFacet
            (String etag, String name, String column, int precision)
            throws SQLException {
        PreparedStatement pst = conn.prepareStatement
                ("select round(" + column + "," + precision + ")  as bucket,\n"
                        + "count(*) as count\n"
                        + "from compound_props a, etag_data b\n"
                        + "where b.etag_id = ?\n"
                        + "and b.data_id = a.pubchem_compound_cid\n"
                        + "group by bucket\n"
                        + "order by bucket");

        try {
            pst.setString(1, etag);

            ResultSet rs = pst.executeQuery();
            List<Integer[]> buckets = new ArrayList<Integer[]>();
            while (rs.next()) {
                Integer range = rs.getInt("bucket");
                if (rs.wasNull()) {
                    range = null;
                }
                int count = rs.getInt("count");
                buckets.add(new Integer[]{range, count});
            }
            rs.close();

            //System.err.println(name+" => "+counts);

            Facet f = new Facet(name);
            Map<String, Integer> counts = new TreeMap<String, Integer>();
            if (!buckets.isEmpty()) {
                if (false) { // generate bins
                    for (int i = 0; i < buckets.size() - 1; ++i) {
                        Integer[] bin = buckets.get(i);
                        if (bin[0] == null) {
                            counts.put("", bin[1]);
                        } else {
                            Integer range = buckets.get(i + 1)[0];
                            counts.put("[" + bin[0] + ", " + range + ")", bin[1]);
                        }
                    }
                    Integer[] bin = buckets.get(buckets.size() - 1);
                    counts.put(">= " + bin[0], bin[1]);
                } else {
                    // return the lower range and let the client create 
                    //  the bins
                    for (Iterator<Integer[]> iter = buckets.iterator();
                         iter.hasNext(); ) {
                        Integer[] bin = iter.next();
                        counts.put(bin[0] != null
                                ? bin[0].toString() : "", bin[1]);
                    }
                }
            }
            f.setCounts(counts);

            return f;
        } finally {
            pst.close();
        }
    }

    public List<Facet> getCompoundFacets(String etag) throws SQLException {
        List<Facet> facets = new ArrayList<Facet>();

        facets.add(getCompoundCollectionFacet(etag));
        facets.addAll(getCompoundPropertyFacets(etag));

        return facets;
    }

    public List<Compound> getCompoundsByETag
            (int skip, int top, String etag) throws SQLException {

        Map info = getETagInfo(etag);
        if (!Compound.class.getName().equals(info.get("type"))) {
            throw new IllegalArgumentException
                    ("ETag " + etag + " is of type " + Compound.class.getName());
        }

        List<Compound> compounds = new ArrayList<Compound>();
        StringBuilder sql = new StringBuilder
                ("select c.*,d.* from compound c, compound_props d, etag e1, "
                        + "etag_data e2 where e1.etag_id = ? "
                        + "and e1.type = ? "
                        + "and e2.etag_id = e1.etag_id "
                        + "and c.cid = e2.data_id "
                        + "and d.pubchem_compound_cid = e2.data_id "
                        + "order by e2.index");
        if (skip >= 0 && top > 0) {
            sql.append(" limit " + skip + "," + top);
        } else if (top > 0) {
            sql.append(" limit " + top);
        }

        PreparedStatement pst1 = conn.prepareStatement(sql.toString());
        try {
            pst1.setString(2, Compound.class.getName());
            Set<Long> unique = new HashSet<Long>();
            pst1.setString(1, etag);

            ResultSet rs = pst1.executeQuery();
            while (rs.next()) {
                Long cid = rs.getLong("cid");
                if (!unique.contains(cid)) {
                    unique.add(cid);

                    Compound c = new Compound();
                    compounds.add(c);

                    c.setCid(cid);
                    fillCompound(rs, c);
                }
            }
            rs.close();

            touchETag(etag);
            for (Compound c : compounds) {
                c.setSids(getSidsByCid(c.getCid()));
            }

            return compounds;
        } finally {
            pst1.close();
        }
    }

    public Map<String, String[]> getCompoundAnnotations(Long cid) throws SQLException {
        PreparedStatement pst = conn.prepareStatement
                ("select * from compound_annot where cid = ?");
        try {
            pst.setLong(1, cid);

            List<String> keys = new ArrayList<String>();
            List<String> vals = new ArrayList<String>();

            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                String key = rs.getString("annot_key");
                String val = rs.getString("val");
                if (val != null) {
                    keys.add(key);
                    vals.add(val.trim());
                }
            }
            rs.close();

            Map<String, String[]> anno = new TreeMap();
            anno.put("anno_key", keys.toArray(new String[0]));
            anno.put("anno_val", vals.toArray(new String[0]));

            return anno;
        } finally {
            pst.close();
        }
    }

    protected void fillCompound(ResultSet rs, Compound c)
            throws SQLException {
        c.setCid(rs.getLong("cid"));
        c.setProbeId(rs.getString("probe_id"));
        c.setSmiles(rs.getString("iso_smiles"));
        // not what we want... place holder for now
        c.setName(rs.getString("pubchem_iupac_name"));
        c.setMwt(rs.getDouble("pubchem_molecular_weight"));
        if (rs.wasNull()) {
            c.setMwt(null);
        }
        c.setTpsa(rs.getDouble("pubchem_cactvs_tpsa"));
        if (rs.wasNull()) {
            c.setTpsa(null);
        }
        c.setExactMass(rs.getDouble("pubchem_exact_mass"));
        if (rs.wasNull()) {
            c.setExactMass(null);
        }
        c.setXlogp(rs.getDouble("pubchem_xlogp3_aa"));
        if (rs.wasNull()) {
            c.setXlogp(null);
        }
        c.setComplexity(rs.getInt("pubchem_cactvs_complexity"));
        if (rs.wasNull()) {
            c.setComplexity(null);
        }
        c.setRotatable(rs.getInt("pubchem_cactvs_rotatable_bond"));
        if (rs.wasNull()) {
            c.setRotatable(null);
        }
        c.setHbondAcceptor(rs.getInt("pubchem_cactvs_hbond_acceptor"));
        if (rs.wasNull()) {
            c.setHbondAcceptor(null);
        }
        c.setHbondDonor(rs.getInt("pubchem_cactvs_hbond_donor"));
        if (rs.wasNull()) {
            c.setHbondDonor(null);
        }
        c.setPreferredTerm(rs.getString("preferred_term"));
        if (rs.wasNull()) c.setPreferredTerm(null);
    }

    /**
     * Extract the measured results for a substance in an experiment.
     * <p/>
     * The identifier will usually be obtained via {@link #getExperimentData(Long, int, int)} using the
     * experiment identifier (bard_expt_id).
     * <p/>
     * This method returns an {@link ExperimentData} object that contains the high level summary of the
     * result (score, potency, outcome etc) as well as the actual measured data which may be single
     * point or dose response.
     *
     * @param edid The experiment data identifier  (of the form BARD_EXPT_ID.SID)
     * @return
     * @throws SQLException
     */
    public ExperimentData getExperimentDataByDataId(String edid) throws SQLException, IOException {
        if (edid == null || !edid.contains(".")) return null;

        String[] toks = edid.split("\\.");
        Long eid = Long.parseLong(toks[0]);
        Long sid = Long.parseLong(toks[1]);

        PreparedStatement pst = conn.prepareStatement("select * from bard_experiment_data a, bard_experiment_result b, bard_experiment c where a.eid = ? and a.sid = ? and a.expt_data_id = b.expt_data_id and a.bard_expt_id = c.bard_expt_id");
        ExperimentData ed = null;
        try {
            pst.setLong(1, eid);
            pst.setLong(2, sid);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                ed = getExperimentData(rs);
                ed.setExptDataId(edid);
            }
            rs.close();
        } finally {
            pst.close();
        }
        return ed;
    }

    /**
     * Helper method to get experiment data in chunks.
     * 
     * This method assumes that all the experiment data identifiers come from the same
     * experiment, allowing us to take a shortcut in the SQL. If this is not the case,
     * the results will be incomplete/
     * 
     * @param edids
     * @return
     * @throws SQLException
     * @throws IOException
     */
    List<ExperimentData> getExperimentDataByDataId(List<String> edids) throws SQLException, IOException {
        if (edids == null || edids.size() == 0) return null;
        List<ExperimentData> ret = new ArrayList<ExperimentData>();

        Long bardExptId = -1L;

        StringBuilder sb = new StringBuilder();
        sb.append("(");
        String sep = "";
        for (String edid : edids) {
            String[] toks = edid.split("\\.");
            bardExptId = Long.parseLong(toks[0]);
            Long sid = Long.parseLong(toks[1]);
            sb.append(sep).append(sid);
            sep = ",";
        }
        sb.append(")");

        String sql = "select * from bard_experiment_data a, bard_experiment_result b, bard_experiment c where a.bard_expt_id = " + bardExptId + " and a.sid in " + sb.toString() + " and a.expt_data_id = b.expt_data_id and a.bard_expt_id = c.bard_expt_id";
        PreparedStatement pst = conn.prepareStatement(sql);
        ExperimentData ed = null;
        try {
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                ed = getExperimentData(rs);
                ret.add(ed);
            }
            rs.close();
        } finally {
            pst.close();
        }
        return ret;
    }


    /**
     * Retrieves the experiment data by ETag, note the use of teh bardExptId as the experiment key
     * 
     * @param skip
     * @param top
     * @param bardExptId
     * @param etag
     * @return
     * @throws SQLException
     * @throws IOException
     */
    public List<ExperimentData> getExperimentDataByETag
            (int skip, int top, Long bardExptId, String etag)
            throws SQLException, IOException {
        List<ExperimentData> data = new ArrayList<ExperimentData>();

        Map info = getETagInfo(etag);
        String type = (String) info.get("type");

        log.info("## ETag=" + etag + " info=" + info);
        StringBuilder sql = null;
        if (type != null) {
            if (type.equals(Compound.class.getName())) {
                sql = new StringBuilder
                        ("select * from bard_experiment_data a, "
                                + "bard_experiment_result b, bard_experiment c, "
                                + "etag_data d where a.bard_expt_id = ? and "
                                + "d.etag_id = ? and "
                                + "a.cid = d.data_id and "
                                + "a.expt_data_id = b.expt_data_id and "
                                + "a.bard_expt_id = c.bard_expt_id order by d.index");
            } else if (type.equals(Substance.class.getName())) {
                sql = new StringBuilder
                        ("select * from bard_experiment_data a, "
                                + "bard_experiment_result b, bard_experiment c, "
                                + "etag_data d where a.bard_expt_id = ? and "
                                + "d.etag_id = ? and "
                                + "a.sid = d.data_id and "
                                + "a.expt_data_id = b.expt_data_id and "
                                + "a.bard_expt_id = c.bard_expt_id order by d.index");
            } else {
                log.error("Can't retrieve experiment data "
                        + "for etag of type: " + type);
            }
        } else {
            log.error("Invalid ETag " + etag);
        }

        if (sql == null) {
            return data;
        }

        if (skip >= 0 && top > 0) {
            sql.append(" limit " + skip + "," + top);
        } else if (top > 0) {
            sql.append(" limit " + top);
        }

        PreparedStatement pst = conn.prepareStatement(sql.toString());
        try {
            pst.setLong(1, bardExptId);
            pst.setString(2, etag);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                ExperimentData ed = getExperimentData(rs);
                ed.setExptDataId(ed.getEid() + "." + ed.getSid());
                data.add(ed);
            }
            rs.close();
        } finally {
            pst.close();
        }

        return data;
    }

    /**
     * Helper t build an <code>ExperimentData</code> object
     * @param rs
     * @return
     * @throws SQLException
     * @throws IOException
     */
    ExperimentData getExperimentData(ResultSet rs)
            throws SQLException, IOException {
        ExperimentData ed = new ExperimentData();
        ed.setEid(rs.getLong("eid"));
        //sets a new field for bard_expt_id
        ed.setBardExptId(rs.getLong("bard_expt_id"));
        ed.setSid(rs.getLong("sid"));
        ed.setCid(rs.getLong("cid"));
        ed.setExptDataId(ed.getEid()+"."+ed.getSid());

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
        ed.setDefs(ado);
        ed.transform();

        return ed;
    }

    /**
     * Retrieves the experiment result definition based on the bard_expt_id
     * 
     * @param bardExptId
     * @return
     * @throws SQLException
     */
    public String getExperimentMetadataByExptId(Long bardExptId)
            throws SQLException {

        if (bardExptId == null || bardExptId <= 0) return null;
        PreparedStatement pst = conn.prepareStatement
                ("select assay_result_def from bard_experiment where bard_expt_id = ?");
        pst.setLong(1, bardExptId);
        ResultSet rs = pst.executeQuery();

        String json = null;
        try {
            if (rs.next()) {
                Blob blob = rs.getBlob("assay_result_def");
                json = new String(blob.getBytes(1, (int) blob.length()));
            }

            return json;
        } finally {
            rs.close();
            pst.close();
        }
    }

    /**
     * Retrieves the experiment object based on bard_experiment_id
     * @param bardExptId
     * @return
     * @throws SQLException
     */
    public Experiment getExperimentByExptId(Long bardExptId) throws SQLException {
        if (bardExptId == null || bardExptId <= 0) return null;
        PreparedStatement pst = conn.prepareStatement("select * from bard_experiment where bard_expt_id = ?");
        pst.setLong(1, bardExptId);
        ResultSet rs = pst.executeQuery();
        Experiment e = new Experiment();
        while (rs.next()) {
            e.setExptId(bardExptId);
            e.setAssayId(rs.getLong("bard_assay_d"));

            //JCB need to select a collection of project ids for the specified experiment
            //e.setProjId(rs.getLong("proj_id"));
            //removed assignment of deprecated fields
            
            e.setName(rs.getString("name"));
            e.setDescription(rs.getString("description"));

            e.setCategory(rs.getInt("category"));
            e.setClassification(rs.getInt("classification"));
            e.setClassification(rs.getInt("type"));

            e.setDeposited(rs.getDate("deposited"));
            e.setUpdated(rs.getDate("updated"));

            e.setSubstances(rs.getInt("sample_count"));
            e.setCompounds(rs.getInt("cid_count"));

            e.setHasProbe(rs.getBoolean("have_probe"));
        }
        rs.close();
        pst.close();
        return e;
    }

    /**
     * Returns the list of experiments based on a given bard_assay_id (experiments that use the assay)
     * @param bardAssayId
     * @return
     * @throws SQLException
     */
    public List<Experiment> getExperimentByAssayId(Long bardAssayId) throws SQLException {
        if (bardAssayId == null || bardAssayId <= 0) return null;
        PreparedStatement pst = conn.prepareStatement("select bard_expt_id from bard_experiment where bard_assay_id = ?");
        pst.setLong(1, bardAssayId);
        ResultSet rs = pst.executeQuery();
        List<Experiment> experiments = new ArrayList<Experiment>();
        while (rs.next()) experiments.add(getExperimentByExptId(rs.getLong(1)));
        pst.close();
        return experiments;
    }

    /**
     * Returns the Assay for a given bard_assay_id 
     * @param bardAssayID
     * @return
     * @throws SQLException
     */
    public Assay getAssayByAid(Long bardAssayID) throws SQLException {
        if (bardAssayID == null || bardAssayID <= 0) return null;
        PreparedStatement pst = conn.prepareStatement("select * from bard_assay where bard_assay_id = ?");
        Assay a = null;
        try {
            pst.setLong(1, bardAssayID);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                a = getAssay(rs);
            }
            rs.close();
        } finally {
            pst.close();
        }
        return a;
    }

    /**
     * Helper method to build an Assay object based on a result set
     * 
     * @param rs
     * @return
     * @throws SQLException
     */
     
    Assay getAssay(ResultSet rs) throws SQLException {
        Assay a = new Assay();
        long aid = rs.getLong("assay_id");
        a.setAid(aid);

        long bardAssayId = rs.getLong("bard_assay_id");
        //add the bard assay id
        a.setBardAssayId(bardAssayId);
        //a.setAssays(rs.getInt("assays"));
        a.setCategory(rs.getInt("category"));
        //no classification for assay. Experiments are classified by type
        //a.setClassification(rs.getInt("classification"));
        a.setDeposited(rs.getDate("deposited"));
        a.setDescription(rs.getString("description"));
        
        //no grant number on assays
        //a.setGrantNo(rs.getString("grant_no"));

        a.setName(rs.getString("name"));
        a.setSource(rs.getString("source"));
        
        //jcb no summary field in bard_assay table
        //a.setSummary(rs.getInt("summary"));
        
        //jcb assay does not have a 'type', type is associated with Experiment
        //a.setType(rs.getInt("type"));
        
        a.setUpdated(rs.getDate("updated"));
        a.setComments(rs.getString("comment"));
        a.setProtocol(rs.getString("protocol"));

        // next we need to look up publications, targets, experiments, projects and data
        a.setPublications(getAssayPublications(aid));
        a.setTargets(getAssayTargets(aid));

        List<Experiment> expts = getExperimentByAssayId(aid);
        List<Project> projs = getProjectByAssayId(aid);

        List<Long> eids = new ArrayList<Long>();
        for (Experiment expt : expts) eids.add(expt.getExptId());
        a.setEids(eids);

        List<Long> pids = new ArrayList<Long>();
        for (Project proj : projs) pids.add(proj.getProjectId());
        a.setPids(pids);

        // put in annotations
        PreparedStatement pst = conn.prepareStatement("select * from go_assay where bard_assay_id = ? and go_type = 'P'");
        pst.setLong(1, bardAssayId);
        ResultSet resultSet = pst.executeQuery();
        List<String> l1 = new ArrayList<String>();
        List<String> l2 = new ArrayList<String>();
        while (resultSet.next()) {
            l1.add(resultSet.getString("go_id"));
            l2.add(resultSet.getString("go_term"));
        }
        a.setGobp_id(l1);
        a.setGobp_term(l2);
        pst.close();

        pst = conn.prepareStatement("select * from go_assay where bard_assay_id = ? and go_type = 'F'");
        pst.setLong(1, bardAssayId);
        resultSet = pst.executeQuery();
        l1 = new ArrayList<String>();
        l2 = new ArrayList<String>();
        while (resultSet.next()) {
            l1.add(resultSet.getString("go_id"));
            l2.add(resultSet.getString("go_term"));
        }
        a.setGomf_id(l1);
        a.setGomf_term(l2);
        pst.close();

        pst = conn.prepareStatement("select * from go_assay where bard_assay_id = ? and go_type = 'C'");
        pst.setLong(1, bardAssayId);
        resultSet = pst.executeQuery();
        l1 = new ArrayList<String>();
        l2 = new ArrayList<String>();
        while (resultSet.next()) {
            l1.add(resultSet.getString("go_id"));
            l2.add(resultSet.getString("go_term"));
        }
        a.setGocc_id(l1);
        a.setGocc_term(l2);
        pst.close();

        try {
            CAPDictionary dict = getCAPDictionary();
            
            //this is pulling from cap_annotations using pubchem aid via a cap to pubchem mapping table
            //this should be pulling based on a bard id since cap ids are not reliable.
            //the cap annotation table should hold an appropriate id for queries
            List<CAPAssayAnnotation> capannots = getAssayAnnotations(aid);
            l1 = new ArrayList<String>();
            l2 = new ArrayList<String>();
            for (CAPAssayAnnotation capannot : capannots) {
                l1.add(dict.getNode(new BigInteger(capannot.key)).getLabel());
                if (capannot.value != null) l2.add(dict.getNode(new BigInteger(capannot.value)).getLabel());
                else l2.add(capannot.display);
            }
            a.setAk_dict_label(l1);
            a.setAv_dict_label(l2);

        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (ClassNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        return a;
    }

    /**
     * Retruns a list of assay based on an etag.  
     * @param skip
     * @param top
     * @param etag
     * @return
     * @throws SQLException
     */
    public List<Assay> getAssaysByETag(int skip, int top, String etag)
            throws SQLException {
        Map info = getETagInfo(etag);
        if (!Assay.class.getName().equals(info.get("type"))) {
            throw new IllegalArgumentException
                    ("ETag " + etag + " not of type " + Assay.class.getName());
        }

        StringBuilder sql = new StringBuilder
                ("select a.* from bard_assay a, etag_data e where etag_id = ? "
                        + "and a.bard_assay_id = e.data_id order by e.index");

        if (skip >= 0 && top > 0) {
            sql.append(" limit " + skip + "," + top);
        } else if (top > 0) {
            sql.append(" limit " + top);
        }

        List<Assay> assays = new ArrayList<Assay>();
        PreparedStatement pst = conn.prepareStatement(sql.toString());
        try {
            pst.setString(1, etag);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                assays.add(getAssay(rs));
            }
            rs.close();
        } finally {
            pst.close();
        }
        return assays;
    }

    public List<Substance> getSubstanceByETag(int skip, int top, String etag) throws SQLException {
        ETag info = getEtagByEtagId(etag);
        if (!Substance.class.getName().equals(info.getType())) {
            throw new IllegalArgumentException
                    ("ETag " + etag + " not of type " + Substance.class.getName());
        }

        StringBuilder sql = new StringBuilder
                ("select a.* from  substance a, etag_data e where etag_id = ? "
                        + "and a.sid = e.data_id order by e.index");

        if (skip >= 0 && top > 0) {
            sql.append(" limit " + skip + "," + top);
        } else if (top > 0) {
            sql.append(" limit " + top);
        }

        ArrayList<Substance> substances = new ArrayList<Substance>();
        PreparedStatement pst = conn.prepareStatement(sql.toString());
        try {
            pst.setString(1, etag);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                substances.add(getSubstanceBySid(rs.getLong("sid")));
            }
            rs.close();
        } finally {
            pst.close();
        }
        return substances;
    }

    /**
     * Returns a list of Experiments based on etag.
     * @param skip
     * @param top
     * @param etag
     * @return
     * @throws SQLException
     */
    public List<Experiment> getExperimentsByETag(int skip, int top, String etag) throws SQLException {
        ETag info = getEtagByEtagId(etag);
        if (!Experiment.class.getName().equals(info.getType())) {
            throw new IllegalArgumentException
                    ("ETag " + etag + " not of type " + Experiment.class.getName());
        }

        StringBuilder sql = new StringBuilder
                ("select a.* from  bard_experiment a, etag_data e where etag_id = ? "
                        + "and a.bard_expt_id = e.data_id order by e.index");

        if (skip >= 0 && top > 0) {
            sql.append(" limit " + skip + "," + top);
        } else if (top > 0) {
            sql.append(" limit " + top);
        }

        ArrayList<Experiment> expts = new ArrayList<Experiment>();
        PreparedStatement pst = conn.prepareStatement(sql.toString());
        try {
            pst.setString(1, etag);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                expts.add(getExperimentByExptId(rs.getLong("bard_expt_id")));
            }
            rs.close();
        } finally {
            pst.close();
        }
        return expts;
    }


    public List<Assay> getAssays(Long... assayIds) throws SQLException {
        List<Assay> assays = new ArrayList<Assay>();
        for (Long aid : assayIds) assays.add(getAssayByAid(aid));
        return assays;
    }


    /**
     * Retrieve CIDs for compounds associated with an experiment (based on bard_expt_id).
     *
     * @param bardExptId  The experiment identifier
     * @param skip how many records to skip
     * @param top  how many records to return
     * @return A list of compound CIDs
     * @throws SQLException if an invalid limit specification is supplied or there is an error in the SQL query
     */
    public List<Long> getExperimentCids(Long bardExptId, int skip, int top) throws SQLException {
        if (bardExptId == null || bardExptId < 0) return null;

        String limitClause = "";
        if (skip != -1) {
            if (top <= 0) throw new SQLException("If skip != -1, top must be greater than 0");
            limitClause = "  limit " + skip + "," + top;
        }

        PreparedStatement pst = conn.prepareStatement("select distinct cid from bard_experiment_data where bard_expt_id = ? order by cid " + limitClause);
        pst.setLong(1, bardExptId);
        ResultSet rs = pst.executeQuery();
        List<Long> ret = new ArrayList<Long>();
        while (rs.next()) ret.add(rs.getLong("cid"));
        rs.close();
        pst.close();
        return ret;
    }

    /**
     * Return experiment data ids for an experiment.
     * <p/>
     * The identifiers used to refer to experiment data are a combination of the
     * experiment id and the substance identifier in the form <code>EXPT_ID.SID</code>.
     *
     * @param bardExptId  The experiment id (AKA Pubchem AID for experiments taken from Pubchem)
     * @param skip how many records to skip
     * @param top  how many records to return
     * @return
     * @throws SQLException
     */
    public List<String> getExperimentDataIds(Long bardExptId, int skip, int top) throws SQLException {
        if (bardExptId == null || bardExptId < 0) return null;

        String limitClause = "";
        if (skip != -1) {
            if (top <= 0) throw new SQLException("If skip != -1, top must be greater than 0");
            limitClause = "  limit " + skip + "," + top;
        }

        PreparedStatement pst = conn.prepareStatement("select concat(cast(bard_expt_id as char), '.', cast(sid as char)) as id from bard_experiment_data where bard_expt_id = ? order by id " + limitClause);
        pst.setLong(1, bardExptId);
        ResultSet rs = pst.executeQuery();
        List<String> ret = new ArrayList<String>();
        while (rs.next()) ret.add(rs.getString(1));
        rs.close();
        pst.close();
        return ret;
    }

    /**
     * Return experiment data objects for an experiment.
     *
     * @param bardExptId  The experiment id (AKA Pubchem AID for experiments taken from Pubchem)
     * @param skip how many records to skip
     * @param top  how many records to return
     * @return
     * @throws SQLException
     */
    public List<ExperimentData> getExperimentData(Long bardExptId, int skip, int top) throws SQLException, IOException {
        if (bardExptId == null || bardExptId < 0) return null;

        String limitClause = "";
        if (skip != -1) {
            if (top <= 0) throw new SQLException("If skip != -1, top must be greater than 0");
            limitClause = "  limit " + skip + "," + top;
        }

        PreparedStatement pst = conn.prepareStatement("select concat(cast(bard_expt_id as char), '.', cast(sid as char)) as id from bard_experiment_data where bard_expt_id = ? order by score desc, bard_expt_id, sid " + limitClause);
        pst.setLong(1, bardExptId);
        ResultSet rs = pst.executeQuery();
        List<ExperimentData> ret = new ArrayList<ExperimentData>();
        
        List<String> chunk = new ArrayList<String>();
        int chunkSize = 1000;
        int n = 0;
        while (rs.next()) {
            chunk.add(rs.getString(1));
            n++;
            if (n == chunkSize) {                
                ret.addAll(getExperimentDataByDataId(chunk));
                chunk.clear();
                n = 0;
            }
        }
        if (chunk.size() > 0) ret.addAll(getExperimentDataByDataId(chunk));
        rs.close();
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
    public List<String> getSubstanceDataIds(Long sid, int skip, int top) throws SQLException {
        if (sid == null || sid < 0) return null;

        String limitClause = "";
        if (skip != -1) {
            if (top <= 0) throw new SQLException("If skip != -1, top must be greater than 0");
            limitClause = "  limit " + skip + "," + top;
        }

        PreparedStatement pst = conn.prepareStatement("select concat(cast(bard_expt_id as char), '.', cast(sid as char)) as id from bard_experiment_data where sid = ? order by id " + limitClause);
        pst.setLong(1, sid);
        ResultSet rs = pst.executeQuery();
        List<String> ret = new ArrayList<String>();
        while (rs.next()) ret.add(rs.getString(1));
        rs.close();
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
        rs.close();
        pst.close();

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

        PreparedStatement pst = conn.prepareStatement("select concat(cast(bard_expt_id as char), '.', cast(sid as char)) as id from bard_experiment_data where sid = ? order by id " + limitClause);
        pst.setLong(1, sid);
        ResultSet rs = pst.executeQuery();
        List<ExperimentData> ret = new ArrayList<ExperimentData>();
        while (rs.next()) ret.add(getExperimentDataByDataId(rs.getString(1)));
        rs.close();
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
    public List<String> getCompoundDataIds(Long cid, int skip, int top) throws SQLException {
        if (cid == null || cid < 0) return null;

        String limitClause = "";
        if (skip != -1) {
            if (top <= 0) throw new SQLException("If skip != -1, top must be greater than 0");
            limitClause = "  limit " + skip + "," + top;
        }

        PreparedStatement pst = conn.prepareStatement("select concat(cast(bard_expt_id as char), '.', cast(sid as char)) as id from bard_experiment_data where cid = ? order by id " + limitClause);
        pst.setLong(1, cid);
        ResultSet rs = pst.executeQuery();
        List<String> ret = new ArrayList<String>();
        while (rs.next()) ret.add(rs.getString(1));
        rs.close();
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

        PreparedStatement pst = conn.prepareStatement("select distinct(bard_expt_id) from bard_experiment_data where cid = ? order by bard_expt_id " + limitClause);
        pst.setLong(1, cid);
        ResultSet rs = pst.executeQuery();
        List<Long> ret = new ArrayList<Long>();
        while (rs.next()) ret.add(rs.getLong(1));
        rs.close();
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

        PreparedStatement pst = conn.prepareStatement("select distinct(bard_expt_id) from bard_experiment_data where sid = ? order by bard_expt_id " + limitClause);
        pst.setLong(1, sid);
        ResultSet rs = pst.executeQuery();
        List<Long> ret = new ArrayList<Long>();
        while (rs.next()) ret.add(rs.getLong(1));
        rs.close();
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

        PreparedStatement pst = conn.prepareStatement("select distinct(bard_expt_id) from bard_experiment_data where cid = ? order by bard_expt_id " + limitClause);
        pst.setLong(1, cid);
        ResultSet rs = pst.executeQuery();
        List<Experiment> ret = new ArrayList<Experiment>();
        while (rs.next()) ret.add(getExperimentByExptId(rs.getLong(1)));
        rs.close();
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

        PreparedStatement pst = conn.prepareStatement("select distinct(bard_expt_id) from bard_experiment_data where sid = ? order by bard_expt_id " + limitClause);
        pst.setLong(1, sid);
        ResultSet rs = pst.executeQuery();
        List<Experiment> ret = new ArrayList<Experiment>();
        while (rs.next()) ret.add(getExperimentByExptId(rs.getLong(1)));
        rs.close();
        pst.close();
        return ret;
    }

    /**
     * Return {@link Assay} objects for a substance.
     *
     * @param sid  The Pubchem CID
     * @param skip how many records to skip
     * @param top  how many records to return
     * @return
     * @throws SQLException
     */
    public List<Assay> getSubstanceAssays(Long sid, int skip, int top) throws SQLException {
        if (sid == null || sid < 0) return null;

        String limitClause = "";
        if (skip != -1) {
            if (top <= 0) throw new SQLException("If skip != -1, top must be greater than 0");
            limitClause = "  limit " + skip + "," + top;
        }

        PreparedStatement pst = conn.prepareStatement("select distinct b.bard_assay_id from bard_experiment_data a, bard_experiment b where a.sid = ? and a.bard_expt_id = b.bard_expt_id  " + limitClause);
        pst.setLong(1, sid);
        ResultSet rs = pst.executeQuery();
        List<Assay> ret = new ArrayList<Assay>();
        while (rs.next()) ret.add(getAssayByAid(rs.getLong(1)));
        rs.close();
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

        PreparedStatement pst = conn.prepareStatement("select concat(cast(bard_expt_id as char), '.', cast(sid as char)) as id from bard_experiment_data where cid = ? order by expt_data_id " + limitClause);
        pst.setLong(1, cid);
        ResultSet rs = pst.executeQuery();
        List<ExperimentData> ret = new ArrayList<ExperimentData>();
        while (rs.next()) ret.add(getExperimentDataByDataId(rs.getString(1)));
        rs.close();
        pst.close();
        return ret;
    }


    /**
     * Retrieve SIDs for compounds associated with an experiment.
     *
     * @param bardExptId  The experiment identifier
     * @param skip how many records to skip
     * @param top  how many records to return
     * @return A list of compound SIDs
     * @throws SQLException if an invalid limit specification is supplied or there is an error in the SQL query
     */
    public List<Long> getExperimentSids(Long bardExptId, int skip, int top) throws SQLException {
        if (bardExptId == null || bardExptId < 0) return null;

        String limitClause = "";
        if (skip != -1) {
            if (top <= 0) throw new SQLException("If skip != -1, top must be greater than 0");
            limitClause = "  limit " + skip + "," + top;
        }

        PreparedStatement pst = conn.prepareStatement("select distinct sid from bard_experiment_data where bard_expt_id = ? order by sid " + limitClause);
        pst.setLong(1, bardExptId);
        ResultSet rs = pst.executeQuery();
        List<Long> ret = new ArrayList<Long>();
        while (rs.next()) ret.add(rs.getLong("sid"));
        rs.close();
        pst.close();
        return ret;
    }

    /**
     * Retrieve compounds associated with an experiment.
     *
     * @param bardExptId  The experiment identifier
     * @param skip how many records to skip
     * @param top  how many records to return
     * @return A list of {@link Compound} objects
     * @throws SQLException if an invalid limit specification is supplied or there is an error in the SQL query
     */
    public List<Compound> getExperimentCompounds(Long bardExptId, int skip, int top) throws SQLException {
        if (bardExptId == null || bardExptId < 0) return null;

        String limitClause = "";
        if (skip != -1) {
            if (top <= 0) throw new SQLException("If skip != -1, top must be greater than 0");
            limitClause = "  limit " + skip + "," + top;
        }

        PreparedStatement pst = conn.prepareStatement("select cid, sid from bard_experiment_data where bard_expt_id = ? order by sid " + limitClause);
        pst.setLong(1, bardExptId);
        ResultSet rs = pst.executeQuery();
        List<Compound> ret = new ArrayList<Compound>();

        while (rs.next()) {
            ret.addAll(getCompoundsByCid(rs.getLong("cid")));
        }
        rs.close();
        pst.close();
        return ret;
    }

    /**
     * Retrieve substances associated with an assay.
     *
     * @param bardExptId  The assay identifier
     * @param skip how many records to skip
     * @param top  how many records to return
     * @return A list of {@link Compound} objects
     * @throws SQLException if an invalid limit specification is supplied or there is an error in the SQL query
     */
    public List<Compound> getExperimentSubstances(Long bardExptId, int skip, int top) throws SQLException {
        if (bardExptId == null || bardExptId < 0) return null;

        String limitClause = "";
        if (skip != -1) {
            if (top <= 0) throw new SQLException("If skip != -1, top must be greater than 0");
            limitClause = "  limit " + skip + "," + top;
        }

        PreparedStatement pst = conn.prepareStatement("select cid, sid from bard_experiment_data where bard_expt_id = ? order by sid " + limitClause);
        pst.setLong(1, bardExptId);
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
     * Retrieve publications associated with a 'bard_assay_id'.
     * <p/>
     * This query requires that the publication details are available in the publication table.
     *
     * @param bardAssayId The bard assay id to query for
     * @return a List of {@link Publication} objects
     * @throws SQLException
     */
    public List<Publication> getAssayPublications(Long bardAssayId) throws SQLException {
        if (bardAssayId == null || bardAssayId <= 0) return null;
        PreparedStatement pst2 = conn.prepareStatement("select a.* from publication a, assay_pub b where b.bard_assay_id = ? and b.pmid = a.pmid");
        pst2.setLong(1, bardAssayId);
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
        rs2.close();
        pst2.close();
        return pubs;
    }

    /**
     * Return a list of protein targets based on on a bard assay id
     * @param bardAssayId
     * @return
     * @throws SQLException
     */
    public List<ProteinTarget> getAssayTargets(Long bardAssayId) throws SQLException {
        PreparedStatement pst2 = conn.prepareStatement("select a.* from protein_target a, assay_target b where b.bard_assay_id = ? and a.gene_id = b.gene_id");
        pst2.setLong(1, bardAssayId);
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
        rs2.close();
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
            pst = conn.prepareStatement("select bard_assay_id from bard_assay where (name like ? or description like ? or source like ? or protocol like ?)");
            pst.setString(1, q);
            pst.setString(2, q);
            pst.setString(3, q);
            pst.setString(4, q);
            pst.setString(5, q);
        } else {
            String[] toks = query.split("\\[");
            String q = toks[0].trim();
            String field = toks[1].trim().replace("]", "");
            String sql = "select bard_assay_id from bard_assay where " + field + " like '%" + q + "%'";
            pst = conn.prepareStatement(sql);
        }

        ResultSet rs = pst.executeQuery();
        List<Assay> assays = new ArrayList<Assay>();
        while (rs.next()) {
            Long aid = rs.getLong("bard_assay_id");
            assays.add(getAssayByAid(aid));
        }
        rs.close();
        pst.close();

        return assays;
    }


    /**
     * Returns assays for a given accession 
     * @param acc
     * @return
     * @throws SQLException
     */
    public List<Assay> getAssaysByTargetAccession(String acc) throws SQLException {
        PreparedStatement pst = conn.prepareStatement("select distinct b.bard_assay_id from protein_target a, assay_target b where a.accession = ? and a.accession = b.accession");
        pst.setString(1, acc);
        List<Assay> assays = new ArrayList<Assay>();
        ResultSet rs = pst.executeQuery();
        while (rs.next()) assays.add(getAssayByAid(rs.getLong(1)));
        pst.close();
        return assays;
    }

    /**
     * Returns assay for a given accession
     * @param geneid
     * @return
     * @throws SQLException
     */
    public List<Assay> getAssaysByTargetGeneid(Long geneid) throws SQLException {
        PreparedStatement pst = conn.prepareStatement("select distinct b.bard_assay_id from protein_target a, assay_target b where a.gene_id = ? and a.accession = b.accession");
        pst.setLong(1, geneid);
        List<Assay> assays = new ArrayList<Assay>();
        ResultSet rs = pst.executeQuery();
        while (rs.next()) assays.add(getAssayByAid(rs.getLong(1)));
        rs.close();
        pst.close();
        return assays;
    }

    
    public List<Long> getProjectIds() throws SQLException {
        PreparedStatement pst = conn.prepareStatement("select bard_proj_id from bard_project");
        ResultSet rs = pst.executeQuery();
        List<Long> ret = new ArrayList<Long>();
        while (rs.next()) {
            ret.add(rs.getLong(1));
        }
        rs.close();
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
        PreparedStatement pst = conn.prepareStatement("select bard_assay_id from bard_assay order by bard_assay_id");
        ResultSet rs = pst.executeQuery();
        List<Long> ret = new ArrayList<Long>();
        while (rs.next()) {
            ret.add(rs.getLong(1));
        }
        rs.close();
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
        PreparedStatement pst = conn.prepareStatement("select bard_expt_id from bard_experiment order by bard_expt_id");
        ResultSet rs = pst.executeQuery();
        List<Long> ret = new ArrayList<Long>();
        while (rs.next()) {
            ret.add(rs.getLong("bard_expt_id"));
        }
        rs.close();
        pst.close();
        return ret;
    }

    // TOOD handle depositor id - should resolve the integer to a string
    public Project getProject(Long bardProjId) throws SQLException {
        Project p = null;
        PreparedStatement pst = conn.prepareStatement("select * from bard_project where bard_proj_id = ?");
        try {
            pst.setLong(1, bardProjId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                p = new Project();
                p.setProjectId(bardProjId);
                p.setDescription(rs.getString("description"));
                p.setName(rs.getString("name"));
                p.setDeposited(rs.getDate("create_date"));
            }
            rs.close();
        } finally {
            pst.close();
        }

        if (p == null) {
            return p;
        }

        // find all experiments for this project
        pst = conn.prepareStatement("select bard_expt_id from bard_project_experiment where bard_proj_id = ?");
        try {
            pst.setLong(1, bardProjId);
            ResultSet rs = pst.executeQuery();
            List<Long> eids = new ArrayList<Long>();
            while (rs.next()) eids.add(rs.getLong(1));
            rs.close();
            p.setEids(eids);
        } finally {
            pst.close();
        }

        // find assays
        pst = conn.prepareStatement
        		("select a.bard_assay_id from bard_experiment a, bard_project_experiment b " +
        				"where a.bard_expt_id=b.bard_expt_id and b.bard_proj_id = ?");
        try {
            List<Long> aids = new ArrayList<Long>();
            pst.setLong(1, bardProjId);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                aids.add(rs.getLong(1));
            }
            rs.close();
            p.setAids(aids);
        } finally {
            pst.close();
        }

        // find targets; should be project_target table instead?
        pst = conn.prepareStatement("select * from project_target where proj_id=?");
        try {
            pst.setLong(1, bardProjId);
            ResultSet rs = pst.executeQuery();
            List<ProteinTarget> targets = new ArrayList<ProteinTarget>();
            while (rs.next()) {
                String acc = rs.getString("accession");
                targets.add(getProteinTargetByAccession(acc));
            }
            rs.close();
            p.setTargets(targets);
        } finally {
            pst.close();
        }


        return p;
    }

    public List<Project> getProjects(Long... projectIds) throws SQLException {
        List<Project> p = new ArrayList<Project>();
        for (Long pid : projectIds) {
            Project proj = getProject(pid);
            if (proj != null) {
                p.add(proj);
            }
        }
        return p;
    }

    /**
     * Returns a list of {@link Project} objects that are associated with an experiment.
     * <p/>
     * It is possible, that an experiment is not assigned to a project ("orphan"), in
     * which case the project id is -1.
     * <p/>
     *
     * @param bardExptId The experiment id
     * @return A {@link Project} object
     * @throws SQLException
     */
    public List<Project> getProjectByExperimentId(Long bardExptId) throws SQLException {

        PreparedStatement pst = conn.prepareStatement("select bard_proj_id from bard_project_experiment where bard_expt_id = ?");
        pst.setLong(1, bardExptId);
        ResultSet rs = pst.executeQuery();
        List<Project> ps = new ArrayList<Project>();
        while (rs.next()) {
            Long projectId = rs.getLong("bard_proj_id");
            ps.add(getProject(projectId));
        }
        rs.close();
        pst.close();
        return ps;
    }

    /**
     * Returns the bard_project_ids for projects based on a bard_assay_id
     * @param bardAssayId
     * @return
     * @throws SQLException
     */
    public List<Project> getProjectByAssayId(Long bardAssayId) throws SQLException {
        PreparedStatement pst = conn.prepareStatement("select distinct b.bard_proj_id from bard_experiment a, bard_project_experiment b where a.bard_assay_id = ? and a.bard_expt_id = b.bard_expt_id");
        pst.setLong(1, bardAssayId);
        ResultSet rs = pst.executeQuery();
        List<Long> pids = new ArrayList<Long>();
        while (rs.next()) pids.add(rs.getLong("proj_id"));
        rs.close();
        pst.close();
        return getProjects(pids.toArray(new Long[]{}));
    }

    public List<Long> getProbesForProject(Long bardExptId) throws SQLException {
        PreparedStatement pst = conn.prepareStatement("select a.cid from bard_experiment_data a, compound b where b.probe_id is not null and a.bard_expt_id = ? and a.cid = b.cid");
        pst.setLong(1, bardExptId);
        ResultSet rs = pst.executeQuery();
        List<Long> probeids = new ArrayList<Long>();
        while (rs.next()) probeids.add(rs.getLong("cid"));
        rs.close();
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

            String sql = "select concat(cast(bard_expt_id as char), '.', cast(sid as char)) as id from bard_experiment_data where " + field + " = " + q + "";
            pst = conn.prepareStatement(sql);
        }

        ResultSet rs = pst.executeQuery();
        List<ExperimentData> experimentData = new ArrayList<ExperimentData>();
        while (rs.next()) {
            String exptId = rs.getString(1);
            experimentData.add(getExperimentDataByDataId(exptId));
        }
        rs.close();
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
        rs.close();
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
            else if (klass.equals(Project.class)) entity = getProject((Long) id);
            else if (klass.equals(Experiment.class)) entity = getExperimentByExptId((Long) id);
            else if (klass.equals(Compound.class)) entity = getCompoundsByCid((Long) id);
            else if (klass.equals(Assay.class)) entity = getAssayByAid((Long) id);
            else if (klass.equals(ETag.class)) entity = getEtagByEtagId((String) id);
            if (entity != null) {
                if (entity instanceof List) entities.addAll((Collection<T>) entity);
                else if (entity instanceof BardEntity) entities.add((T) entity);
            }
        }
        rs.close();
        pst.close();
        return entities;
    }

    public <T extends BardEntity> List<T> getEntitiesByEtag(String etag, int skip, int top) throws SQLException {
        return getEntitiesByEtag(getEtagByEtagId(etag), skip, top);
    }

    public <T extends BardEntity> List<T> getEntitiesByEtag(ETag etag, int skip, int top) throws SQLException {
        List<T> entities = new ArrayList<T>();
        if (Assay.class.getName().equals(etag.getType()))
            entities = (List<T>) getAssaysByETag(skip, top, etag.getEtag());
        else if (Compound.class.getName().equals(etag.getType()))
            entities = (List<T>) getCompoundsByETag(skip, top, etag.getEtag());
        else if (Substance.class.getName().equals(etag.getType()))
            entities = (List<T>) getSubstanceByETag(skip, top, etag.getEtag());
        else if (Experiment.class.getName().equals(etag.getType()))
            entities = (List<T>) getExperimentsByETag(skip, top, etag.getEtag());
        return entities;
    }


    /**
     * **********************************************************************
     * <p/>
     * CAP related methods (dictionary, annotations)
     * <p/>
     * ************************************************************************
     */

    /**
     * Get annotations for an assay.
     * <p/>
     * The assay tables currently use Pubchem AID as the primary identifier, whereas
     * CAP annotations use the CAP assay ID to refer to assays. Thus when retrieving
     * annotations (at least from CAP annotations on CAP assays), we must map Pubchem
     * AID to CAP AID.
     * <p/>
     * Currently the annotations are restricted to CAP derived annotations only.
     *
     * @param assayId The assay identifier. This is currently a PubChem AID.
     * @return A list of assay annotations
     * @throws SQLException
     */
    public List<CAPAssayAnnotation> getAssayAnnotations(Long assayId) throws SQLException {
        PreparedStatement pst = conn.prepareStatement("select a.* from cap_annotation a, cap_pubchem_map b where b.pubchem_aid = ? and a.assay_id = b.bard_assay_id;");
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
        rs.close();
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
        rs.close();
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
        rs.close();
        pst.close();
        return nrow;
    }

    public <T> List<T> getEntitiesByCid(Long cid, Class<T> entity, Integer skip, Integer top) throws SQLException {
        String sql = null;
        PreparedStatement pst;

        if (cid == null || cid < 0) return null;
        String limitClause = "";
        if (skip == null) skip = -1;
        if (top == null) top = -1;
        if (skip != -1) {
            if (top <= 0) throw new SQLException("If skip != -1, top must be greater than 0");
            limitClause = "  limit " + skip + "," + top;
        }

        if (entity.isAssignableFrom(Assay.class)) {
            sql = "select distinct assay_id from experiment_data a, experiment b where a.cid = ? and a.eid = b.expt_id  " + limitClause;
        } else if (entity.isAssignableFrom(Project.class)) {
            sql = "select p.proj_id from project p, experiment e where e.expt_id in (select distinct ed.eid from experiment_data ed, experiment e, compound a where a.cid = ? and ed.cid = a.cid and ed.eid = e.expt_id) and e.proj_id = p.proj_id";
        } else if (entity.isAssignableFrom(Substance.class)) {
            sql = "select sid from cid_sid where cid = ?";
        }

        pst = conn.prepareStatement(sql);
        pst.setLong(1, cid);
        ResultSet rs = pst.executeQuery();
        List<T> ret = new ArrayList<T>();
        while (rs.next()) {
            if (entity.isAssignableFrom(Assay.class)) ret.add((T) getAssayByAid(rs.getLong(1)));
            else if (entity.isAssignableFrom(Project.class)) ret.add((T) getProject(rs.getLong(1)));
            else if (entity.isAssignableFrom(Substance.class)) ret.add((T) getSubstanceBySid(rs.getLong(1)));
        }
        rs.close();
        pst.close();
        return ret;
    }


    public static void main(String[] argv) throws Exception {
        if (argv.length == 0) {
            System.out.println("Usage: DBUtils URL");
            System.exit(1);
        }
        Class.forName("com.mysql.jdbc.Driver");

        DBUtils db = new DBUtils();
        Connection con = DriverManager.getConnection(argv[0]);
        con.setAutoCommit(false);
        db.setConnection(con);

        String etag = db.newETag("test", Compound.class.getName());
        int cnt = db.putETag(etag, 1l, 2l, 3l, 4l, 5l);
        System.out.println(etag + ": " + cnt);
        db.closeConnection();
    }
}
