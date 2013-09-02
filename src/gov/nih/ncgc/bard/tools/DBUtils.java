package gov.nih.ncgc.bard.tools;

import chemaxon.formats.MolFormatException;
import chemaxon.formats.MolImporter;
import chemaxon.struc.Molecule;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import gov.nih.ncgc.bard.capextract.CAPAnnotation;
import gov.nih.ncgc.bard.capextract.CAPDictionary;
import gov.nih.ncgc.bard.entity.Assay;
import gov.nih.ncgc.bard.entity.BardEntity;
import gov.nih.ncgc.bard.entity.Biology;
import gov.nih.ncgc.bard.entity.Compound;
import gov.nih.ncgc.bard.entity.ETag;
import gov.nih.ncgc.bard.entity.Experiment;
import gov.nih.ncgc.bard.entity.ExperimentData;
import gov.nih.ncgc.bard.entity.ExperimentResultType;
import gov.nih.ncgc.bard.entity.PantherClassification;
import gov.nih.ncgc.bard.entity.Project;
import gov.nih.ncgc.bard.entity.ProjectStep;
import gov.nih.ncgc.bard.entity.ProteinTarget;
import gov.nih.ncgc.bard.entity.Publication;
import gov.nih.ncgc.bard.entity.Substance;
import gov.nih.ncgc.bard.entity.TargetClassification;
import gov.nih.ncgc.bard.rest.rowdef.AssayDefinitionObject;
import gov.nih.ncgc.bard.rest.rowdef.DataResultObject;
import gov.nih.ncgc.bard.rest.rowdef.DoseResponseResultObject;
import gov.nih.ncgc.bard.search.Facet;
import gov.nih.ncgc.bard.search.SearchUtil;
import gov.nih.ncgc.bard.search.SolrField;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Reader;
import java.math.BigInteger;
import java.security.Principal;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;



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
    static CAPDictionary dict = null;

    static final String CACHE_PREFIX;
    static {
        SecureRandom rand = new SecureRandom ();
        byte[] id = new byte[8];
        rand.nextBytes(id);
        CACHE_PREFIX = Util.toString(id);

        System.err.println("** CACHE PREFIX: "+CACHE_PREFIX+" **");
    }

    static final int MAX_CACHE_SIZE = 10000;
    static final CacheManager cacheManager = CacheManager.getInstance();
    
    static synchronized Cache getCache (String name) {
        String cacheName = CACHE_PREFIX+"::"+name;

        Cache cache = cacheManager.getCache(cacheName);
        
        if (cache == null) {
            cache = new Cache (cacheName,
                               MAX_CACHE_SIZE,
                               false, // overflowToDisk
                               false, // eternal (never expire)
                               24*60*60, // time to live (seconds)
                               24*60*60 // time to idle (seconds)
                               );
            cacheManager.addCacheIfAbsent(cache);
            cache.setStatisticsEnabled(true);
        }
        return cache;
    }
    
    
    
    /*******
     * Cache Flush Handling methods (2)
     */
    static CacheFlushManager cacheFlushManager;
    static Vector <String> flushCachePrefixNames = null;

    /**
     * Initializes the list of cache prefixes to manage
     * This is only called when the container is initialized.
     * 
     * @param cachePrefixListCSV comma delimited list of cache prefixes;
     */
    static public void initializeManagedCaches(String cachePrefixListCSV, String cacheClusterNodes) {
	cacheFlushManager = new CacheFlushManager(cacheManager);

	//make the list of cache prefixes
	flushCachePrefixNames = new Vector<String>();
	String [] cachePrefixes = cachePrefixListCSV.split(",");
	for(String cachePrefix : cachePrefixes) {
	    flushCachePrefixNames.add(cachePrefix.trim());
	}

	//put the cache under management control
	//if the prefix names are empty or just one (empty string), set flush all boolean	
	cacheFlushManager.manage(flushCachePrefixNames, cacheClusterNodes, (flushCachePrefixNames.size() < 2));
    }
    
    /**
     * Called to shutdown cache management. This is typically called when the container is
     * destroyed to shutdown the manager gracefully.
     */
    static public void shutdownCacheFlushManager() {
	cacheFlushManager.shutdown();
    }

    
    static private String datasourceContext = "jdbc/bardman3";
    static public void setDataSourceContext (String context) {
        if (context == null) {
            throw new IllegalArgumentException ("Can't set null context!");
        }
        datasourceContext = context;
    }
    static public String getDataSourceContext () { return datasourceContext; }


    <T> T getCacheValue (Cache cache, Object key) {
        Element el = cache.get(key);
        if (el != null) {
            return (T) el.getObjectValue();
        }
        return null;
    }


    static Logger log;
    Connection conn = null;
    Map<Class, Query> fieldMap;
    SecureRandom rand = new SecureRandom();

    class Query {
        List<String> validFields;
        String orderField, tableName, idField, join;

        Query(List<String> validFields, String orderField,
              String idField, String tableName) {
            this (validFields, orderField, idField, tableName, null);
        }

        Query(List<String> validFields, String orderField,
              String idField, String tableName, String join) {
            this.validFields = validFields;
            this.orderField = orderField;
            this.tableName = tableName;
            this.join = join;

            if (idField == null) this.idField = orderField;
            else this.idField = idField;
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

        public String getJoin () { return join; }
    }

    public DBUtils() {
        log = LoggerFactory.getLogger(this.getClass());

        final List<String> publicationFields = Arrays.asList("pmid", "title", "abstract", "doi");
        final List<String> projectFields = Arrays.asList("name", "description");
        final List<String> targetFields = Arrays.asList("accession", "name", "description", "uniprot_status");
        final List<String> experimentFields = Arrays.asList("name", "description", "source", "grant_no");
        final List<String> compoundFields = new ArrayList<String>();//Arrays.asList("url");
        final List<String> substanceFields = Arrays.asList("substance_url", "source_name", "dep_regid");
        final List<String> assayFields = Arrays.asList("name", "description", "protocol", "comemnt", "source", "grant_no");
        final List<String> edFields = Arrays.asList();
        final List<String> etagFields = Arrays.asList("name", "type");
        final List<String> biologyFields = Arrays.asList("ext_id", "description");

        fieldMap = new HashMap<Class, Query>() {{
            put(Publication.class, new Query(publicationFields, "pmid", null, "publication"));
            put(Project.class, new Query(projectFields, "bard_proj_id", null, "bard_project"));
            put(ProteinTarget.class, new Query(targetFields, "accession", null, "protein_target"));
            put(Biology.class, new Query(biologyFields, "serial", null, "bard_biology"));
            put(Experiment.class, new Query(experimentFields, "bard_expt_id", null, "bard_experiment"));
            put(Compound.class, new Query(compoundFields, "druglike desc, activity desc", "cid", "compound_rank"));
            put(Substance.class, new Query(substanceFields, "sid", null, "substance"));
            put(Assay.class, new Query(assayFields, "bard_assay_id", null, "bard_assay"));
            put(ExperimentData.class, new Query(edFields, "expt_data_id", null, "bard_experiment_data"));
            put(ETag.class, new Query(etagFields, "etag_id", null, "etag", "status=1"));
        }};

//        conn = getConnection();
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
        if (conn != null && !conn.isClosed())
            conn.close();
    }

    private Connection getConnection() {
        javax.naming.Context initContext;
        Connection con = null;
        try {
            initContext = new javax.naming.InitialContext();
            DataSource ds = (javax.sql.DataSource)
                initContext.lookup("java:comp/env/"+getDataSourceContext ());
            con = ds.getConnection();
            con.setAutoCommit(false);
        }
        catch (Exception ex) {
            log.info(ex.toString());
            // try 
            try {
                initContext = new javax.naming.InitialContext();
                DataSource ds = (javax.sql.DataSource)
                    initContext.lookup(getDataSourceContext ());
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

    public Map<String, String> getCacheStatistics() {
        Map<String, String> statMap = new HashMap<String, String>();
        String[] cacheNames = cacheManager.getCacheNames();
        for (String cacheName : cacheNames) {
            statMap.put(cacheName, cacheManager.getCache(cacheName).getStatistics().toString());
        }
        return statMap;
    }

    public Publication getPublicationByPmid(Long pmid) throws SQLException, IOException {
        if (pmid == null) return null;
        Cache cache = getCache ("PublicationByPmidCache");
        try {
            Publication pub = (Publication)getCacheValue (cache, pmid);
            if (pub != null)
                return pub;
        }
        catch (ClassCastException ex) {
            //
        }

        if (conn == null) conn = getConnection();
        PreparedStatement pst = conn.prepareStatement("select * from publication where pmid = ?");
        try {
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
            cache.put(new Element (pmid, p));
            return p;
        }
        finally {
            pst.close();
        }
    }

    public Publication getPublicationByDoi(String doi) throws SQLException, IOException {
        if (doi == null || doi.trim().equals("")) return null;
        Cache cache = getCache ("PublicationByDoiCache");
        try {
            Publication pub = (Publication)getCacheValue (cache, doi);
            if (pub != null) {
                return pub;
            }
        }
        catch (ClassCastException ex) {
        }

        if (conn == null) conn = getConnection();
        PreparedStatement pst = conn.prepareStatement("select * from publication where doi = ?");
        try {
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
            cache.put(new Element (doi, p));
            return p;
        }
        finally {
            pst.close();
        }
    }

    public List<Publication> getProteinTargetPublications(String accession) throws SQLException {
        if (accession == null || accession.trim().equals("")) return null;

        Cache cache = getCache ("ProteinTargetPublicationsCache");
        try {
            List list = getCacheValue (cache, accession);
            if (list != null) {
                return list;
            }
        }
        catch (ClassCastException ex) {
        }

        if (conn == null) conn = getConnection();
        PreparedStatement pst2 = conn.prepareStatement("select a.* from publication a, target_pub b where b.accession = ? and b.pmid = a.pmid");
        try {
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
            cache.put(new Element (accession, pubs));
            return pubs;
        }
        finally {
            pst2.close();
        }
    }

    public ProteinTarget getProteinTargetByAccession(String accession) throws SQLException {
        if (accession == null || accession.trim().equals("")) return null;
        Cache cache = getCache ("ProteinTargetByAccessionCache");
        try {
            ProteinTarget value = getCacheValue (cache, accession);
            if (value != null)
                return value;
        }
        catch (ClassCastException ex) {}

        if (conn == null) conn = getConnection();
        PreparedStatement pst = conn.prepareStatement("select * from protein_target where accession  = ?");
        try {
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
                p.setClasses(getPantherClassesForAccession(accession));
            }
            rs.close();
            cache.put(new Element (accession, p));
            return p;
        }
        finally {
            pst.close();
        }
    }

    public ProteinTarget getProteinTargetByGeneid(Long geneId) throws SQLException {
        if (geneId == null) return null;

        Cache cache = getCache ("ProteinTargetByGeneIdCache");
        ProteinTarget p = null;
        try {
            p = getCacheValue (cache, geneId);
        }
        catch (ClassCastException ex) {}

        if (p == null) {
            if (conn == null) conn = getConnection();
            PreparedStatement pst = conn.prepareStatement
                ("select * from protein_target where gene_id = ?");
            try {
                pst.setLong(1, geneId);
                ResultSet rs = pst.executeQuery();
                p = new ProteinTarget();
                while (rs.next()) {
                    p.setAcc(rs.getString("accession"));
                    p.setDescription(rs.getString("description"));
                    p.setGeneId(rs.getLong("gene_id"));
                    p.setTaxId(rs.getLong("taxid"));
                    p.setName(rs.getString("name"));
                    p.setStatus(rs.getString("uniprot_status"));
                    p.setClasses(getPantherClassesForAccession(p.getAcc()));
                }
                rs.close();

                cache.put(new Element (geneId, p));
            }
            finally {
                pst.close();
            }
        }
        return p;
    }

    public Long getCidBySid (Long sid) throws SQLException {
        Cache cache = getCache ("CidBySidCache");
        try {
            Long value = getCacheValue (cache, sid);
            if (value != null) {
                return value;
            }
        }
        catch (ClassCastException ex) {}

        if (conn == null) conn = getConnection();
        PreparedStatement pst = conn.prepareStatement("select cid from cid_sid where sid = ?");
        try {
            pst.setLong(1, sid);
            ResultSet rs = pst.executeQuery();
            long cid = -1L;
            if (rs.next()) cid = rs.getLong(1);
            rs.close();
            cache.put(new Element (sid, cid));
            return cid;
        }
        finally {
            pst.close();
        }
    }

    public List<Long> getSidsByCid(Long cid) throws SQLException {
        Cache cache = getCache ("SidsByCidCache");
        try {
            List value = getCacheValue (cache, cid);
            if (value != null) {
                return value;
            }
        }
        catch (ClassCastException ex) {}

        List<Long> sids = new ArrayList<Long>();
        if (conn == null) conn = getConnection();
        PreparedStatement pst = conn.prepareStatement("select sid from cid_sid where cid = ?");
        try {
            pst.setLong(1, cid);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) sids.add(rs.getLong(1));
            rs.close();
            cache.put(new Element (cid, sids));
            return sids;
        }
        finally {
            pst.close();
        }
    }

    public Long[][] getSidsByCids(Long[] cids) throws SQLException {

        List<Long[]> list  = new ArrayList<Long[]>();

        Cache cache = getCache ("SidsByCidCache");
        List<Long> uncachedCids = new ArrayList<Long>();
        try {
            for (Long cid : cids) {
                List<Long> sids = getCacheValue(cache, cid);
                if (sids != null) {
                    for (Long sid : sids) list.add(new Long[]{cid, sid});
                } else uncachedCids.add(cid);
            }
        }
        catch (ClassCastException ex) {}

        // if there's nothing in uncachedCids, we got everything from the cache
        if (uncachedCids.size() == 0) return list.toArray(new Long[][]{});

        if (conn == null) conn = getConnection();
        List<List<Long>> chunks = Util.chunk(uncachedCids, CHUNK_SIZE);

        for (List<Long> chunk : chunks) {
            String cidClause = Util.join(chunk, ",");
            String sql = "select cid, sid from cid_sid where cid in ("+cidClause+") order by cid";
            Statement stm = conn.createStatement();
            try {
                ResultSet rs = stm.executeQuery(sql);

                List<Long> sids = new ArrayList<Long>();

                rs.next();
                Long oldCid = rs.getLong(1);
                Long sid = rs.getLong(2);
                list.add(new Long[]{oldCid, sid});
                sids.add(sid);

                while (rs.next()) {
                    Long cid = rs.getLong(1);
                    sid = rs.getLong(2);

                    list.add(new Long[]{cid, sid});

                    if (cid != oldCid) {
                        cache.put(new Element (oldCid, sids));
                        sids.clear();
                    }
                    sids.add(sid);
                    oldCid = cid;
                }
                cache.put(new Element (oldCid, sids));
                rs.close();
            }
            finally {
                stm.close();
            }
        }

        return list.toArray(new Long[][]{});
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

        Cache cache = getCache ("CompoundsByCidCache");
        List<Compound> compounds = new ArrayList<Compound>();
        List<Long> notcached = new ArrayList<Long>();

        for (Long acid : cids) {
            Compound value = null;
            try {
                value = getCacheValue (cache, acid);
            }
            catch (ClassCastException ex) {}

            if (value != null) {
                compounds.add(value);
            }
            else {
                notcached.add(acid);
            }
        }

        if (!notcached.isEmpty()) {
            if (conn == null) conn = getConnection();
            cids = notcached.toArray(new Long[0]);
            List<List<Long>> chunks = Util.chunk(cids, CHUNK_SIZE);

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

                        c.setNumAssay(getEntityCountByCid(c.getCid(), Assay.class));
                        c.setNumActiveAssay(getEntityCountByActiveCid(c.getCid(), Assay.class));

                        cache.put(new Element (c.getCid(), c));
                    }
                    rs.close();
                }
                finally {
                    stm.close();
                }
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

    public List<Compound> getCompoundsByName (String... names)
        throws SQLException {
        if (names == null || names.length == 0) return null;
        List<Compound> cmpds = new ArrayList<Compound>();
        List<String> notcached = new ArrayList<String>();

        Cache cache = getCache ("CompoundsByNameCache");
        for (String n : names) {
            List<Compound> value = null;
            try {
                value = getCacheValue (cache, n);
            }
            catch (ClassCastException ex) {}
            if (value != null) {
                cmpds.addAll(value);
            }
            else {
                notcached.add(n);
            }
        }

        if (conn == null) conn = getConnection();
        PreparedStatement pst = conn.prepareStatement("select distinct id from synonyms where type = 1 and match(syn) against (? in boolean mode)");
        try {
            ResultSet rs;
            for (String name : notcached) {
                pst.setString(1, name);
                rs = pst.executeQuery();
                List<Compound> c = new ArrayList<Compound>();
                while (rs.next()) {
                    c.addAll(getCompoundsByCid(rs.getLong(1)));
                }
                rs.close();

                cache.put(new Element (name, c));
                cmpds.addAll(c);
            }

            return cmpds;
        }
        finally {
            pst.close();
        }
    }

    public List<Compound> getCompoundsBySid(Long... sids) throws SQLException {
        if (sids == null || sids.length == 0) return null;

        List<Compound> cmpds = new ArrayList<Compound>();

        List<Long> notcached = new ArrayList<Long>();
        Cache cache = getCache("CompoundsBySidCache");
        for (Long sid : sids) {
            Compound value = null;
            value = getCacheValue(cache, sid);
            if (value != null) cmpds.add(value);
            else notcached.add(sid);
        }

        if (conn == null) conn = getConnection();
        List<List<Long>> chunks = Util.chunk(notcached, CHUNK_SIZE);
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

            List<Compound> notCachedCompounds = getCompoundsByCid(cids.toArray(new Long[]{}));
            for (Compound c : notCachedCompounds) {
                List<Long> notCachedSids = c.getSids();
                if (notCachedSids == null) continue;
                for (Long sid : notCachedSids) cache.put(new Element(sid, c));
            }
            cmpds.addAll(notCachedCompounds);
            pst.close();
        }

        return cmpds;
    }

    public String newETag(String name, String clazz) throws SQLException {
        return newETag(name, null, clazz);
    }

    public String newETag(String name, String url, String clazz) throws SQLException {
        if (clazz == null) {
            throw new IllegalArgumentException("Please specify the class!");
        }

        if (conn == null) conn = getConnection();
        PreparedStatement pst = conn.prepareStatement
            ("insert into etag(etag_id,name,type,created,modified,url) "
             + "values (?,?,?,?,?,?)");
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
                    pst.setString(6, url);

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
        if (conn == null) conn = getConnection();
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
        return putETag(etag, null, ids);
    }
    public int putETag (String etag, String name,  Long... ids) throws SQLException {
        int cnt = 0;
        if (conn == null) conn = getConnection();
        PreparedStatement pst = conn.prepareStatement
            ("select a.*,count(*) as size from etag a left join etag_data b "
             +"on a.etag_id = b.etag_id where a.etag_id = ? "
             +"group by a.etag_id");

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
            pst.close();

            if (name != null) {
                pst = conn.prepareStatement
                    ("update etag set name = ?, modified = ? where etag_id = ?");
                pst.setString(1, name);
                pst.setTimestamp(2, new java.sql.Timestamp
                                 (new java.util.Date().getTime()));
                pst.setString(3, etag);
                pst.executeUpdate();
                pst.close();
            }

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
            pst.close();
            cnt -= size;
            log.info("## "+cnt+" entries added for ETag "+etag);

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
            if (conn == null) conn = getConnection();
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
        if (conn == null) conn = getConnection();
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
        if (conn == null) conn = getConnection();
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
                etag.setUrl(rs.getString("url"));
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

    public Map getETagInfo (String etag) throws SQLException {
        if (conn == null) conn = getConnection();
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
                    info.put(name, rs.getObject(c + 1));
                }
            }
            rs.close();

            return info;
        } finally {
            pst.close();
        }
    }

    public Facet getCompoundCollectionFacet (String etag)
        throws SQLException {

        Map info = getETagInfo (etag);

        Cache cache = getCache ("CompoundCollectionFacetCache");
        Element el = cache.get(etag);
        if (el != null) {
            Timestamp ts = (Timestamp)info.get("accessed");
            if (ts.getTime() < el.getLastAccessTime()) {
                try {
                    Facet value = getCacheValue (cache, etag);
                    if (value != null)
                        return value;
                }
                catch (ClassCastException ex) {}
            }
        }

        if (conn == null) conn = getConnection();
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
            "INN", "Withdrawn"
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

            cache.put(new Element (etag, facet));

            return facet;
        } finally {
            pst.close();
        }
    }

    public List<String> getCompoundSynonyms (Long cid) throws SQLException {
        Cache cache = getCache ("CompoundSynonymsCache");
        try {
            List value = getCacheValue (cache, cid);
            if (value != null) {
                return value;
            }
        }
        catch (ClassCastException ex) {}

        if (conn == null) conn = getConnection();
        PreparedStatement pst = conn.prepareStatement
            ("select syn from synonyms where id = ? and type=1");
        List<String> syns = new ArrayList<String>();
        try {
            pst.setLong(1, cid);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                syns.add(rs.getString(1));
            }
            rs.close();

            cache.put(new Element (cid, syns));
        }
        finally {
            pst.close();
        }
        return syns;
    }

    public List<Facet> getCompoundPropertyFacets(String etag)
        throws SQLException {
        Object[][] props = new Object[][]{
            {"xlogp", "PUBCHEM_XLOGP3", 0},
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
        if (conn == null) conn = getConnection();
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

    public List<Facet> getProjectFacets(String etagId) throws SQLException {
        List<Facet> facets = new ArrayList<Facet>();

        ETag etag = getEtagByEtagId(etagId);
        if (etag == null) throw new IllegalArgumentException(etagId+" does not exist");
        if (etag.getType() == null) throw new IllegalArgumentException(etagId+" had a null type. Strange!");
        if (!etag.getType().equals("gov.nih.ncgc.bard.entity.Project"))
            throw new IllegalArgumentException("ETag " + etag + " is of type " + etag.getType());

        if (conn == null) conn = getConnection();
        PreparedStatement pst = conn.prepareStatement("select data_id from etag_data where etag_id = ?");
        try {
            pst.setString(1, etagId);
            ResultSet rs = pst.executeQuery();

            Map<String, Integer> tcounts = new HashMap<String, Integer>();
            Map<String, Integer> dcounts = new HashMap<String, Integer>();
            Map<String, Integer> ecounts = new HashMap<String, Integer>();

            while (rs.next()) {
                Long bardProjectId = rs.getLong(1);
                //                List<CAPAssayAnnotation> capannots = getProjectAnnotations(bardProjectId);
                //                for (CAPAssayAnnotation annot : capannots) {
                //                    if (annot.key.equals("detection_method_type")) {
                //                        if (dtcounts.containsKey(annot.value)) {
                //                            dtcounts.put(annot.value, dtcounts.get(annot.value) + 1);
                //                        } else dtcounts.put(annot.value, 1);
                //                    }
                //                }

                // target facet
                List<Biology> targets = getProjectTargets(bardProjectId);
                for (Biology t : targets) {
                    if (tcounts.containsKey(t.getName())) {
                        tcounts.put(t.getName(), tcounts.get(t.getName()) + 1);
                    } else tcounts.put(t.getName(), 1);
                }

                // disease facet
                PreparedStatement pst2 = conn.prepareStatement("select distinct b.* from  kegg_gene2disease b, bard_biology c where c.entity = 'project' and c.entity_id = ? and c.biology_dict_id = 880 and b.gene_id = c.ext_id");
                pst2.setLong(1, bardProjectId);
                ResultSet rs2 = pst2.executeQuery();
                while (rs2.next()) {
                    String dcat = rs2.getString(1);
                    if (dcounts.containsKey(dcat)) {
                        dcounts.put(dcat, dcounts.get(dcat) + 1);
                    } else dcounts.put(dcat, 1);
                }
                rs2.close();
                pst2.close();
            }
            rs.close();

            Facet facet = new Facet("target_name");
            facet.setCounts(tcounts);
            facets.add(facet);

            facet = new Facet("kegg_disease_cat");
            facet.setCounts(dcounts);
            facets.add(facet);

            //            facet = new Facet("detection_method_type");
            //            facet.setCounts(dtcounts);
            //            facets.add(facet);
        } finally {
            pst.close();
        }

        return facets;

    }

    public List<Facet> getAssayFacets(String etagId) throws SQLException {
        List<Facet> facets = new ArrayList<Facet>();

        ETag etag = getEtagByEtagId(etagId);
        if (!etag.getType().equals("gov.nih.ncgc.bard.entity.Assay"))
            throw new IllegalArgumentException("ETag " + etag + " is of type " + etag.getType());

        if (conn == null) conn = getConnection();
        PreparedStatement pst = conn.prepareStatement("select data_id from etag_data where etag_id = ?");
        try {
            pst.setString(1, etagId);
            ResultSet rs = pst.executeQuery();

            Map<String, Integer> tcounts = new HashMap<String, Integer>();
            Map<String, Integer> dcounts = new HashMap<String, Integer>();

            // detection_method_type
            Map<String, Integer> dtcounts = new HashMap<String, Integer>();
            Map<String, Integer> acrcounts = new HashMap<String, Integer>();

            while (rs.next()) {
                Long bardAssayId = rs.getLong(1);
                List<CAPAnnotation> capannots = getAssayAnnotations(bardAssayId);
                for (CAPAnnotation annot : capannots) {
                    if (annot.key == null) {
                        // source = 'cap-measure' doesn't have anno_key
                    }
                    else if (annot.key.equals("detection_method_type")) {
                        if (dtcounts.containsKey(annot.value)) {
                            dtcounts.put(annot.value, dtcounts.get(annot.value)+1);
                        } else dtcounts.put(annot.value, 1);
                    }
                    else if (annot.key.equals("assay_component_role")) {
                        if (acrcounts.containsKey(annot.value)) {
                            acrcounts.put(annot.value, acrcounts.get(annot.value)+1);
                        } else acrcounts.put(annot.value, 1);
                    }

                }
                // target facet
                List<ProteinTarget> targets = getAssayTargets(bardAssayId);
                for (ProteinTarget t : targets) {
                    if (tcounts.containsKey(t.getName())) {
                        tcounts.put(t.getName(), tcounts.get(t.getName())+1);
                    } else tcounts.put(t.getName(), 1);
                }

                // disease facet
                PreparedStatement pst2 = conn.prepareStatement("select disease_category from bard_assay a, kegg_gene2disease b, assay_target c where a.bard_assay_id = ? and a.bard_assay_id=c.bard_assay_id and c.gene_id=b.gene_id");
                pst2.setLong(1, bardAssayId);
                ResultSet rs2 = pst2.executeQuery();
                while (rs2.next()) {
                    String dcat = rs2.getString(1);
                    if (dcounts.containsKey(dcat)) {
                        dcounts.put(dcat, dcounts.get(dcat) + 1);
                    } else dcounts.put(dcat, 1);
                }
                rs2.close();
                pst2.close();
            }
            rs.close();

            Facet facet = new Facet("target_name");
            facet.setCounts(tcounts);
            facets.add(facet);

            facet = new Facet("kegg_disease_cat");
            facet.setCounts(dcounts);
            facets.add(facet);

            facet = new Facet("detection_method_type");
            facet.setCounts(dtcounts);
            facets.add(facet);

            facet = new Facet("assay_component_role");
            facet.setCounts(dtcounts);
            facets.add(facet);

        } finally {
            pst.close();
        }

        return facets;
    }

    public List<Facet> getCompoundFacets(String etag) throws SQLException {
        List<Facet> facets = new ArrayList<Facet>();

        facets.add(getCompoundCollectionFacet(etag));
        facets.addAll(getCompoundPropertyFacets(etag));

        return facets;
    }

    public List<Compound> getEqvCompounds (Long cid) throws SQLException {
        Cache cache = getCache ("CompoundsEqvClassCache");

        List value = getCacheValue (cache, cid);
        if (value != null) {
            return value;
        }

        if (conn == null) conn = getConnection ();
        // for extra safe measure we use an additional hash 1
        PreparedStatement pstm1, pstm2 = null;
        pstm1 = conn.prepareStatement
            ("select hash1, hash4 from bard2.compound_molfile where cid = ?");

        try {
            List<Compound> compounds = new ArrayList<Compound>();

            pstm1.setLong(1, cid);
            ResultSet rset = pstm1.executeQuery();
            if (rset.next()) {
                String h1 = rset.getString(1);
                String h4 = rset.getString(2);

                pstm2 = conn.prepareStatement
                    ("select * from compound a, bard2.compound_molfile b, "
                     +"compound_props c where a.cid = b.cid "
                     +"and a.cid = c.pubchem_compound_cid "
                     +"and b.hash1 = binary(?) and b.hash4 = binary(?) "
                     +"order by a.cid");
                pstm2.setString(1, h1);
                pstm2.setString(2, h4);

                ResultSet rs = pstm2.executeQuery();
                while (rs.next()) {
                    Compound c = new Compound();
                    fillCompound(rs, c);
                    c.setNumAssay(getEntityCountByCid
                                  (c.getCid(), Assay.class));
                    c.setNumActiveAssay
                        (getEntityCountByActiveCid(c.getCid(), Assay.class));
                    compounds.add(c);
                }
                rs.close();
            }
            rset.close();

            cache.put(new Element (cid, compounds));
            return compounds;
        }
        finally {
            if (pstm2 != null)
                pstm2.close();
            pstm1.close();
        }
    }

    public List<Compound> getCompoundsByHash
        (String h1, String h2, String h3, String h4) throws SQLException {
        StringBuilder sql = new StringBuilder
            ("select * from compound a, bard2.compound_molfile b, "
             +"compound_props c where a.cid = b.cid "
             +"and a.cid = c.pubchem_compound_cid");

        String hash = "";

        List<String> args = new ArrayList<String>();
        if (h1 != null) {
            sql.append(" and hash1 = binary(?)");
            args.add(h1);
            hash += h1;
        }
        if (h2 != null) {
            sql.append(" and hash2 = binary(?)");
            args.add(h2);
            hash += h2;
        }
        if (h3 != null) {
            sql.append(" and hash3 = binary(?)");
            args.add(h3);
            hash += h3;
        }
        if (h4 != null) {
            sql.append(" and hash4 = binary(?)");
            args.add(h4);
            hash += h4;
        }

        Cache cache = getCache ("CompoundsByHashCache");
        List value = getCacheValue (cache, hash);
        if (value != null) {
            return value;
        }

        log.info("HASH: "+hash);
        log.info("SQL: "+sql);

        if (conn == null) conn = getConnection ();
        PreparedStatement pstm = conn.prepareStatement(sql.toString());
        try {
            for (int i = 0; i < args.size(); ++i) {
                pstm.setString(i+1, args.get(i));
            }

            List<Compound> compounds = new ArrayList<Compound>();

            ResultSet rset = pstm.executeQuery();
            while (rset.next()) {
                Compound c = new Compound();
                fillCompound(rset, c);
                c.setNumAssay(getEntityCountByCid(c.getCid(), Assay.class));
                c.setNumActiveAssay
                    (getEntityCountByActiveCid(c.getCid(), Assay.class));
                compounds.add(c);
            }
            rset.close();

            cache.put(new Element (hash, compounds));
            return compounds;
        }
        finally {
            pstm.close();
        }
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
        /*
         * for some reaon, mysql takes much longer when limit <= 5.
         * perhaps a bug in the query planner. we simply guard it here
         * but not sure if we should do it elsewhere too!
         */
        if (skip >= 0 && top > 0) {
            sql.append(" limit " + skip + "," + Math.max(6, top));
        } else if (top > 0) {
            sql.append(" limit " + Math.max(6, top));
        }

        if (conn == null) conn = getConnection();
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
                    c.setNumAssay(getEntityCountByCid(cid, Assay.class));
                    c.setNumActiveAssay
                        (getEntityCountByActiveCid(cid, Assay.class));

                    fillCompound(rs, c);
                }
            }
            rs.close();

            if (top > 0 && top < 6) {
                // truncate it to fit the desired size
                compounds = compounds.subList(0, top);
            }

            //touchETag(etag);
            for (Compound c : compounds) {
                c.setSids(getSidsByCid(c.getCid()));
                Map<String, String[]> annots =
                    getCompoundAnnotations(c.getCid());
                c.setAnno_key(annots.get("anno_key"));
                c.setAnno_val(annots.get("anno_val"));
            }

            return compounds;
        } finally {
            pst1.close();
        }
    }

    public Map<String, String[]> getFilteredCompoundAnnotations(Long cid, List<String> annoKeys) throws SQLException {
        Cache cache = getCache ("CompoundAnnotationCache");
        String cacheKey = "";
        Collections.sort(annoKeys);
        for (String annoKey : annoKeys) cacheKey += annoKey;
        try {
            Map<String, String[]> value = getCacheValue (cache, cacheKey);
            if (value != null) {
                return value;
            }
        }
        catch (ClassCastException ex) {}

        if (conn == null) conn = getConnection();


        StringBuffer sb = new StringBuffer();
        String delim = "";
        for (String key : annoKeys) {
            sb.append(delim).append("'").append(key).append("'");
            delim = ",";
        }
        String filterClause;
        if (annoKeys.size() >0) filterClause = " and annot_key in ("+sb.toString()+") ";
        else filterClause = "";

        PreparedStatement pst = conn.prepareStatement
                ("select * from compound_annot where cid = ? "+filterClause);
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

            Map<String, String[]> anno = new TreeMap<String, String[]>();
            anno.put("anno_key", keys.toArray(new String[keys.size()]));
            anno.put("anno_val", vals.toArray(new String[vals.size()]));

            cache.put(new Element (cacheKey, anno));

            return anno;
        }
        finally {
            pst.close();
        }

    }

    public Map<String, String[]> getCompoundAnnotations
        (Long cid) throws SQLException {

        Cache cache = getCache ("CompoundAnnotationCache");
        try {
            Map<String, String[]> value = getCacheValue (cache, cid);
            if (value != null) {
                return value;
            }
        }
        catch (ClassCastException ex) {}

        if (conn == null) conn = getConnection();
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

            Map<String, String[]> anno = new TreeMap<String, String[]>();
            anno.put("anno_key", keys.toArray(new String[keys.size()]));
            anno.put("anno_val", vals.toArray(new String[vals.size()]));

            cache.put(new Element (cid, anno));

            return anno;
        }
        finally {
            pst.close();
        }
    }

    protected void fillCompound(ResultSet rs, Compound c)
        throws SQLException {
        c.setCid(rs.getLong("cid"));
        c.setProbeId(rs.getString("probe_id"));
        c.setUrl(rs.getString("url"));

        // huh? why?
        try {
            Molecule m = MolImporter.importMol(rs.getString("iso_smiles"));
            c.setSmiles(m.toFormat("smiles"));
        } catch (MolFormatException e) {
            c.setSmiles(rs.getString("iso_smiles"));
        }

        String iupac = rs.getString("pubchem_iupac_name");
        String prefName = rs.getString("preferred_term");
        if (prefName != null) c.setName(prefName);
        else c.setName(iupac);
        c.setIupacName(iupac);

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

        c.setXlogp(rs.getDouble("pubchem_xlogp3"));
        if (rs.wasNull()) {
            c.setXlogp(rs.getDouble("pubchem_xlogp3_aa"));
            if (rs.wasNull()) {
                c.setXlogp(null);
            }
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
        c.setCompoundClass(rs.getString("compound_class"));
        if (rs.wasNull()) c.setCompoundClass(null);

        if (c.getProbeId() != null) {
            List<Long> projects = getProjectIdByProbeId(c.getProbeId());
            if (projects != null && projects.size() > 0) {
                Long id = projects.get(0);
                List<CAPAnnotation> annos = getProjectAnnotations(id);
                List props = new ArrayList();
                for (CAPAnnotation anno : annos) {
                    if (anno.contextRef == null || !anno.contextRef.equals("probe")) continue;
                    props.add(anno);
                }
                c.setProbeAnnotations(props);
            }
        }
    }

    /**
     * Extract the measured results for a substance in an experiment.
     * <p/>
     * The identifier will usually be obtained via {@link #getExperimentData(Long, int, int, String)} using the
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
    public ExperimentData getExperimentDataByDataId
        (String edid) throws SQLException {

        Cache cache = getCache ("ExperimentDataByDataIdCache");
        try {
            ExperimentData value = getCacheValue (cache, edid);
            if (value != null) {
                return value;
            }
        }
        catch (ClassCastException ex) {}

        if (edid == null || !edid.contains(".")) return null;

        String[] toks = edid.split("\\.");
        Long bardExptId = Long.parseLong(toks[0]);
        Long sid = Long.parseLong(toks[1]);

        if (conn == null) conn = getConnection();
        PreparedStatement pst = conn.prepareStatement("select      a . *, b . *, c . *, d.cap_assay_id as real_cap_assay_id from     bard_experiment_data a left join bard_experiment_result b on a.expt_data_id = b.expt_data_id left join bard_experiment c on a.bard_expt_id = c.bard_expt_id left join bard_assay d on c.bard_assay_id = d.bard_assay_id where a.bard_expt_id = ? and a.sid = ?");
        ExperimentData ed = null;
        try {
            pst.setLong(1, bardExptId);
            pst.setLong(2, sid);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                ed = getExperimentData(rs);
                ed.setExptDataId(edid);
            }
            rs.close();

            cache.put(new Element (edid, ed));
        }
        catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        finally {
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
    public List<ExperimentData> getExperimentDataByDataId(List<String> edids)
        throws SQLException, IOException {
        if (edids == null || edids.size() == 0) return null;

        Cache cache = getCache ("ExperimentDataByDataIdCache");
        List<ExperimentData> ret = new ArrayList<ExperimentData>();
        List<String> notcached = new ArrayList<String>();
        for (String edi : edids) {
            ExperimentData value = null;
            try {
                value = getCacheValue (cache, edi);
            }
            catch (ClassCastException ex) {}

            if (value != null) {
                ret.add(value);
            }
            else {
                notcached.add(edi);
            }
        }

        if (!notcached.isEmpty()) {
            if (conn == null) conn = getConnection();

            Long bardExptId = -1L;
            StringBuilder sbSid = new StringBuilder();
            StringBuilder sbEid = new StringBuilder();
            sbEid.append("(");
            sbSid.append("(");
            String sep = "";
            for (String edid : notcached) {
                String[] toks = edid.split("\\.");
                if (toks.length != 2) continue;
                bardExptId = Long.parseLong(toks[0]);
                Long sid = Long.parseLong(toks[1]);
                sbSid.append(sep).append(sid);
                sbEid.append(sep).append(bardExptId);
                sep = ",";
            }
            sbSid.append(")");
            sbEid.append(")");
            if (sbSid.toString().equals("()")) return ret;

//            String sql = "select a.*, b.*, c.*, d.bard_proj_id from bard_experiment_data a, bard_experiment_result b, bard_experiment c, bard_project_experiment d where d.bard_expt_id = " + bardExptId + " and a.bard_expt_id = " + bardExptId + " and a.sid in " + sb.toString() + " and a.expt_data_id = b.expt_data_id and a.bard_expt_id = c.bard_expt_id";
            String sql = "select      a . *, b . *, c . *, d.cap_assay_id as real_cap_assay_id from     bard_experiment_data a left join bard_experiment_result b on a.expt_data_id = b.expt_data_id left join bard_experiment c on a.bard_expt_id = c.bard_expt_id left join bard_assay d on c.bard_assay_id = d.bard_assay_id where a.bard_expt_id in " + sbEid.toString() + " and a.sid in " + sbSid.toString();
            PreparedStatement pst = conn.prepareStatement(sql);
            ExperimentData ed = null;
            try {
                ResultSet rs = pst.executeQuery();
                while (rs.next()) {
                    ed = getExperimentData(rs);
                    ret.add(ed);

                    cache.put(new Element (ed.getExptDataId(), ed));
                }
                rs.close();
            }
            finally {
                pst.close();
            }
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

        Map info = getETagInfo (etag);
        Cache cache = getCache ("ExperimentDataByETagCache");
        Object key = etag+"::"+bardExptId+"::"+skip+"::"+top;
        Element el = cache.get(key);
        if (el != null) {
            Timestamp ts = (Timestamp)info.get("accessed");
            if (ts.getTime() < el.getLastAccessTime()) {
                try {
                    List<ExperimentData> value = getCacheValue (cache, key);
                    if (value != null)
                        return value;
                }
                catch (ClassCastException ex) {}
            }
        }

        String type = (String) info.get("type");

        log.info("## ETag=" + etag + " info=" + info);
        StringBuilder sql = null;
        if (type != null) {
            if (type.equals(Compound.class.getName())) {
                sql = new StringBuilder
                    ("select *, e.cap_assay_id as real_cap_assay_id from bard_experiment_data a, "
                     + "bard_experiment_result b, bard_experiment c, bard_assay e, "
                     + "etag_data d where a.bard_expt_id = ? and "
                     + "d.etag_id = ? and "
                     + "a.cid = d.data_id and "
                     + "c.bard_assay_id = e.bard_assay_id and "
                     + "a.expt_data_id = b.expt_data_id and "
                     + "a.bard_expt_id = c.bard_expt_id order by d.index");
            } else if (type.equals(Substance.class.getName())) {
                sql = new StringBuilder
                    ("select *, e.cap_assay_id as real_cap_assay_id from bard_experiment_data a, "
                     + "bard_experiment_result b, bard_experiment c, bard_assay e, "
                     + "etag_data d where a.bard_expt_id = ? and "
                     + "d.etag_id = ? and "
                     + "a.sid = d.data_id and "
                     + "c.bard_assay_id = e.bard_assay_id and "
                     + "a.expt_data_id = b.expt_data_id and "
                     + "a.bard_expt_id = c.bard_expt_id order by d.index");
            } else {
                log.error("Can't retrieve experiment data "
                          + "for etag of type: " + type);
            }
        } else {
            log.error("Invalid ETag " + etag);
        }

        System.out.println("sql = " + sql);
        if (sql == null) {
            return data;
        }

        if (skip >= 0 && top > 0) {
            sql.append(" limit ").append(skip).append(",").append(top);
        } else if (top > 0) {
            sql.append(" limit ").append(top);
        }

        if (conn == null) conn = getConnection();
        PreparedStatement pst = conn.prepareStatement(sql.toString());
        try {
            pst.setLong(1, bardExptId);
            pst.setString(2, etag);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                ExperimentData ed = getExperimentData(rs);
                ed.setExptDataId(ed.getBardExptId() + "." + ed.getSid());
                data.add(ed);
            }
            rs.close();

            cache.put(new Element (key, data));
        }
        finally {
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
        //sets a new field for bard_expt_id
        ed.setBardExptId(rs.getLong("bard_expt_id"));
        ed.setSid(rs.getLong("sid"));
        ed.setCid(rs.getLong("cid"));
        ed.setExptDataId(ed.getBardExptId()+"."+ed.getSid());

        ed.setBardAssayId(rs.getLong("bard_assay_id"));
        ed.setCapAssayId(rs.getLong("real_cap_assay_id"));
        ed.setCapExptId(rs.getLong("cap_expt_id"));

        Integer classification = rs.getInt("classification");
        if (rs.wasNull()) classification = null;
        ed.setClassification(classification);

        ed.setUpdated(rs.getDate("updated"));
        ed.setOutcome(rs.getInt("outcome"));
        ed.setScore(rs.getInt("score"));

        Float potency = rs.getFloat("potency");
        if (rs.wasNull()) potency = null;
        ed.setPotency(potency);

        Blob blob = rs.getBlob("json_response");
        if (blob != null) {
            ed.setResultJson(new String(blob.getBytes(1, (int) blob.length())));
        }

        // pull in associated projects
        PreparedStatement ps = conn.prepareStatement("select a.bard_proj_id, b.cap_proj_id from bard_project_experiment a, bard_project b where a.bard_expt_id = ? and a.bard_proj_id = b.bard_proj_id");
        ps.setLong(1, ed.getBardExptId());
        ResultSet rs2 = ps.executeQuery();
        List<Long> bpids = new ArrayList<Long>();
        List<Long> cpids = new ArrayList<Long>();
        while (rs2.next()) {
            bpids.add(rs2.getLong(1));
            cpids.add(rs2.getLong(2));
        }
        ed.setBardProjId(bpids);
        ed.setCapProjId(cpids);
        rs2.close();
        ps.close();

        return ed;
    }

    ObjectNode getExperimentDataJsonByExperimentDataId(ExperimentData experimentData, String edid) throws Exception {

        String exptId = "";
        String[] tokens = edid.split("\\.");
        if (tokens.length < 2) {
            throw new Exception("Bogus experiment data id: " + edid);
        } else if (tokens.length == 2) {
            exptId = edid;
        } else {
            exptId = tokens[0] + "." + tokens[1];
        }
        if (experimentData == null || experimentData.getExptDataId() == null)
            throw new Exception("experimentData should not be null");

        //System.err.println("*** "+ Util.toJson(experimentData));

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root = mapper.createObjectNode();
        root.putPOJO("exptdata", experimentData);

        ArrayNode array = root.putArray("results");
        AssayDefinitionObject[] ado = experimentData.getDefs();
        DataResultObject[] results = experimentData.getResults();

        // check the tid; data tid are stored in column coordinate,
        //  so we need to offset by 8
        int tid = Integer.parseInt(tokens[2]);
        if (tid == 0) { // return all?
            for (AssayDefinitionObject d : ado) {
                if ("DoseResponse".equals(d.getType())) {
                    // ignore dose response
                    continue;
                }

                tid = Integer.parseInt(d.getTid());
                DataResultObject res = null;
                for (DataResultObject r : results) {
                    if (tid == r.getTid() - 7) {
                        res = r;
                        break;
                    }
                }

                ObjectNode node = array.addObject();
                node.putPOJO("result", d);
                Object value = res.getValue();
                if (value instanceof String) {
                    value = ((String) value).replaceAll("\"", "");
                    if ("".equals(value)) {
                        value = null;
                    }
                }
                node.putPOJO("value", value);
            }
        } else {
            AssayDefinitionObject def = null;
            for (AssayDefinitionObject d : ado) {
                if (tid == Integer.parseInt(d.getTid())) {
                    def = d;
                    break;
                }
            }

            DataResultObject res = null;
            for (DataResultObject r : results) {
                if (tid == r.getTid() - 7) {
                    res = r;
                    break;
                }
            }

            ObjectNode node = array.addObject();
            node.putPOJO("result", def);

            if ("DoseResponse".equals(def.getType())) {
                DoseResponseResultObject drObj = null;
                for (DoseResponseResultObject dr :
                         experimentData.getDr()) {
                    if (tid == Integer.parseInt(dr.getTid())) {
                        drObj = dr;
                        break;
                    }
                }

                node.putPOJO("value", drObj);
            } else {
                Object value = res.getValue();
                if (value instanceof String) {
                    value = ((String) value).replaceAll("\"", "");
                    if ("".equals(value)) {
                        value = null;
                    }
                }
                node.putPOJO("value", value);
            }
        }
        return root;

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

        Cache cache = getCache ("ExperimentMetadataByExptIdCache");
        try {
            String value = (String) getCacheValue (cache, bardExptId);
            if (value != null) {
                return value;
            }
        }
        catch (ClassCastException ex) {}

        if (conn == null) conn = getConnection();
        PreparedStatement pst = conn.prepareStatement
            ("select expt_result_def from bard_experiment where bard_expt_id = ?");
        pst.setLong(1, bardExptId);
        ResultSet rs = pst.executeQuery();
        String json = null;
        try {
            if (rs.next()) {
                Blob blob = rs.getBlob("expt_result_def");
                if (blob == null) json = "";
                else json = new String(blob.getBytes(1, (int) blob.length()));
            }
            rs.close();
            cache.put(new Element (bardExptId, json));

            return json;
        }
        finally {
            pst.close();
        }
    }

    /**
     * Retrieves the experiment object based on bard_experiment_id
     * @param bardExptId
     * @return
     * @throws SQLException
     */
    public Experiment getExperimentByExptId (Long bardExptId)
        throws SQLException {
        if (bardExptId == null || bardExptId <= 0) return null;

        Cache cache = getCache ("ExperimentByExptIdCache");
        try {
            Experiment value = getCacheValue (cache, bardExptId);
            if (value != null) {
                return value;
            }
        }
        catch (ClassCastException ex) {}

        if (conn == null) conn = getConnection();
        PreparedStatement pst = conn.prepareStatement
            ("select *, b.cap_assay_id as real_cap_assay_id from bard_experiment a, bard_assay b where bard_expt_id = ? and a.bard_assay_id = b.bard_assay_id");
        try {
            pst.setLong(1, bardExptId);
            ResultSet rs = pst.executeQuery();
            Experiment e = null;
            if (rs.next()) {
                e = getExperiment (rs);
            }
            rs.close();

            //JCB: capture all projects behind the experiment
            if (e != null) {
                List<Project> projects = getProjectByExperimentId (bardExptId);
                for (Project project : projects) {
                    Long projectId = project.getBardProjectId();
                    if (projectId != null)
                        e.addProjectID(project.getBardProjectId());
                }

                cache.put(new Element (bardExptId, e));
            }

            return e;
        }
        finally {
            pst.close();
        }
    }

    protected Experiment getExperiment (ResultSet rs) throws SQLException {
        Experiment e= new Experiment();
        e.setBardExptId(rs.getLong("bard_expt_id"));
        e.setBardAssayId(rs.getLong("bard_assay_id"));
        e.setCapExptId(rs.getLong("cap_expt_id"));
        e.setCapAssayId(rs.getLong("real_cap_assay_id"));
        e.setName(rs.getString("name"));
        e.setDescription(rs.getString("description"));
        e.setDeposited(rs.getDate("deposited"));
        e.setUpdated(rs.getDate("updated"));
        e.setSubstances(rs.getInt("sample_count"));
        e.setCompounds(rs.getInt("cid_count"));
        e.setHasProbe(rs.getBoolean("have_probe"));
        e.setPubchemAid(rs.getLong("pubchem_aid"));
        e.setConfidenceLevel(rs.getFloat("confidence_level"));
        e.setStatus(rs.getString("status"));

        e.setActiveCompounds(getExperimentCidCount(e.getBardExptId(), true));

        return e;
    }

    /**
     * Returns the list of experiments based on a given bard_assay_id (experiments that use the assay)
     * @param bardAssayId
     * @return
     * @throws SQLException
     */
    public List<Experiment> getExperimentByAssayId
        (Long bardAssayId) throws SQLException {
        if (bardAssayId == null || bardAssayId <= 0) return null;

        Cache cache = getCache ("ExperimentByAssayIdCache");
        try {
            List<Experiment> value = getCacheValue (cache, bardAssayId);
            if (value != null) {
                return value;
            }
        }
        catch (ClassCastException ex) {}

        if (conn == null) conn = getConnection();
        PreparedStatement pst = conn.prepareStatement
            ("select bard_expt_id from bard_experiment where bard_assay_id = ?");
        try {
            pst.setLong(1, bardAssayId);
            ResultSet rs = pst.executeQuery();
            List<Experiment> experiments = new ArrayList<Experiment>();
            while (rs.next()) {
                experiments.add(getExperimentByExptId(rs.getLong(1)));
            }
            rs.close();

            cache.put(new Element (bardAssayId, experiments));
            return experiments;
        }
        finally {
            pst.close();
        }
    }

    /**
     * Returns the Assay for a given bard_assay_id
     * @param bardAssayID
     * @return
     * @throws SQLException
     */
    public Assay getAssayByAid(Long bardAssayID) throws SQLException {
        if (bardAssayID == null || bardAssayID <= 0) return null;

        Cache cache = getCache ("AssayByAidCache");
        try {
            Assay value = (Assay) getCacheValue (cache, bardAssayID);
            if (value != null) {
                return value;
            }
        }
        catch (ClassCastException ex) {}

        if (conn == null) conn = getConnection();
        PreparedStatement pst = conn.prepareStatement
            ("select * from bard_assay where bard_assay_id = ?");
        Assay a = null;
        try {
            pst.setLong(1, bardAssayID);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                a = getAssay(rs);
            }
            rs.close();
            cache.put(new Element (bardAssayID, a));

            return a;
        }
        finally {
            pst.close();
        }
    }

    /**
     * Helper method to build an Assay object based on a result set
     *
     * @param rs
     * @return
     * @throws SQLException
     */

    Assay getAssay (ResultSet rs) throws SQLException {
        Assay a = new Assay();

        Long capAssayId = rs.getLong("cap_assay_id");
        a.setCapAssayId(capAssayId);

        long bardAssayId = rs.getLong("bard_assay_id");
        //add the bard assay id
        a.setBardAssayId(bardAssayId);
        a.setDeposited(rs.getDate("deposited"));
        a.setDescription(rs.getString("description"));
        a.setName(rs.getString("name"));
        a.setSource(rs.getString("source"));
        a.setDesignedBy(rs.getString("designed_by"));
        a.setUpdated(rs.getDate("updated"));
        a.setComments(rs.getString("comment"));
        a.setProtocol(rs.getString("protocol"));
        a.setTitle(rs.getString("title"));
        a.setAssayStatus(rs.getString("status"));
        a.setAssayType(rs.getString("assay_type"));
        a.setScore(rs.getFloat("score"));

        if (!a.getAssayStatus().equals("Approved"))
            a.setStatusWarning("THIS IS PREVIEW DATA that has not been curated yet, and may be revised or deprecated without notice. PLEASE USE WITH CAUTION");

        List<Long> pmids = new ArrayList<Long>();
        for (Publication pub : getAssayPublications(bardAssayId)) pmids.add(pub.getPubmedId());
        a.setDocuments(pmids);

        List<Biology> biologies = getBiologyByEntity("assay", (int) bardAssayId);
        List<Long> bioSerials = new ArrayList<Long>();
        for (Biology biology : biologies) bioSerials.add(biology.getSerial());
        a.setTargets(bioSerials);

        List<Experiment> expts = getExperimentByAssayId(bardAssayId);
        List<Project> projs = getProjectByAssayId(bardAssayId);

        List<Long> eids = new ArrayList<Long>();
        for (Experiment expt : expts) eids.add(expt.getBardExptId());
        a.setExperiments(eids);

        List<Long> pids = new ArrayList<Long>();
        for (Project proj : projs) pids.add(proj.getBardProjectId());
        a.setProjects(pids);

        if (dict == null) try {
            dict = getCAPDictionary();
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (ClassNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        List<CAPAnnotation> capannots = getAssayAnnotations(bardAssayId);
        List<String> l1 = new ArrayList<String>();
        List<String> l2 = new ArrayList<String>();
        for (CAPAnnotation capannot : capannots) {
            if (capannot.key != null && Util.isNumber(capannot.key) && dict.getNode(new BigInteger(capannot.key)) != null)
                l1.add(dict.getNode(new BigInteger(capannot.key)).getLabel());
            if (capannot.value != null && Util.isNumber(capannot.value) && dict.getNode(new BigInteger(capannot.value)) != null)
                l2.add(dict.getNode(new BigInteger(capannot.value)).getLabel());
            else l2.add(capannot.display);
        }
        a.setAk_dict_label(l1);
        a.setAv_dict_label(l2);

        try {
            a.setMinimumAnnotations(AnnotationUtils.getMinimumRequiredAssayAnnotations(bardAssayId, this));
        } catch (ClassNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        return a;
    }


    public List<Project> getProjectsByETag(int skip, int top, String etag) throws SQLException {
        Map info = getETagInfo(etag);

        Cache cache = getCache ("ProjectsByETagCache");
        Object key = etag+"::"+skip+"::"+top;
        Element el = cache.get(key);
        if (el != null) {
            Timestamp ts = (Timestamp)info.get("accessed");
            if (ts.getTime() < el.getLastAccessTime()) {
                try {
                    List<Project> value = (List)getCacheValue (cache, key);
                    if (value != null)
                        return value;
                }
                catch (ClassCastException ex) {}
            }
        }

        StringBuilder sql = null;
        Object type = info.get("type");
        if (Compound.class.getName().equals(type)) {
            sql = new StringBuilder
                    ("select * from bard_project where bard_proj_id in "
                            +"(select distinct bard_proj_id from bard_project_experiment a, "
                            +"bard_experiment_data b, etag_data c where etag_id = ? "
                            +"and a.bard_expt_id = b.bard_expt_id "
                            +"and b.cid = c.data_id)");
        }
        else if (Substance.class.getName().equals(type)) {
            sql = new StringBuilder
                    ("select * from bard_project where bard_proj_id in "
                            +"(select distinct bard_proj_id from bard_project_experiment a, "
                            +"bard_experiment_data b, etag_data c where etag_id = ? "
                            +"and a.bard_expt_id = b.bard_expt_id "
                            +"and b.sid = c.data_id)");
        }
        else if (Assay.class.getName().equals(type)) {
            sql = new StringBuilder
                    ("select * from bard_project where bard_proj_id in " +
                            " (select distinct bard_proj_id from bard_assay a, bard_experiment b, bard_project_experiment c "+
                            " etag_data e where etag_id = ? " +
                            " and a.bard_assay_id = e.data_id and a.bard_assay_id = b.bard_assay_id " +
                            " and b.bard_expt_id = c.bard_expt_id and d.bard_proj_id = c.bard_proj_id) ");
        } else if (Project.class.getName().equals(type)) {
            sql = new StringBuilder
                    ("select a.* from bard_project a, etag_data e where etag_id = ? "
                            + "and a.bard_proj_id = e.data_id order by e.index");
        }
        else {
            throw new IllegalArgumentException
                    ("Don't know how to get Projects's for etag "
                            +etag + " of type "+type+"!");
        }

        if (skip >= 0 && top > 0) {
            sql.append(" limit " + skip + "," + top);
        } else if (top > 0) {
            sql.append(" limit " + top);
        }

        List<Project> projects = new ArrayList<Project>();
        if (conn == null) conn = getConnection();
        PreparedStatement pst = conn.prepareStatement(sql.toString());
        try {
            pst.setString(1, etag);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                Long bardProjectId = rs.getLong("bard_proj_id");
                projects.add(getProject(bardProjectId));
            }
            rs.close();
            cache.put(new Element (key, projects));
        }
        finally {
            pst.close();
        }
        return projects;


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

        Cache cache = getCache ("AssaysByETagCache");
        Object key = etag+"::"+skip+"::"+top;
        Element el = cache.get(key);
        if (el != null) {
            Timestamp ts = (Timestamp)info.get("accessed");
            if (ts.getTime() < el.getLastAccessTime()) {
                try {
                    List<Assay> value = (List)getCacheValue (cache, key);
                    if (value != null)
                        return value;
                }
                catch (ClassCastException ex) {}
            }
        }

        StringBuilder sql = null;
        Object type = info.get("type");
        if (Compound.class.getName().equals(type)) {
            sql = new StringBuilder
                ("select * from bard_assay where bard_assay_id in "
                 +"(select distinct bard_assay_id from bard_experiment a, "
                 +"bard_experiment_data b, etag_data c where etag_id = ? "
                 +"and a.bard_expt_id = b.bard_expt_id "
                 +"and b.cid = c.data_id)");
        }
        else if (Substance.class.getName().equals(type)) {
            sql = new StringBuilder
                ("select * from bard_assay where bard_assay_id in "
                 +"(select distinct bard_assay_id from bard_experiment a, "
                 +"bard_experiment_data b, etag_data c where etag_id = ? "
                 +"and a.bard_expt_id = b.bard_expt_id "
                 +"and b.sid = c.data_id)");
        }
        else if (Assay.class.getName().equals(type)) {
            sql = new StringBuilder
                ("select a.* from bard_assay a, etag_data e where etag_id = ? "
                 + "and a.bard_assay_id = e.data_id order by e.index");
        }
        else {
            throw new IllegalArgumentException
                ("Don't know how to get Assay's for etag "
                 +etag + " of type "+type+"!");
        }

        if (skip >= 0 && top > 0) {
            sql.append(" limit " + skip + "," + top);
        } else if (top > 0) {
            sql.append(" limit " + top);
        }

        List<Assay> assays = new ArrayList<Assay>();
        if (conn == null) conn = getConnection();
        PreparedStatement pst = conn.prepareStatement(sql.toString());
        try {
            pst.setString(1, etag);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                assays.add(getAssay(rs));
            }
            rs.close();

            cache.put(new Element (key, assays));
        }
        finally {
            pst.close();
        }
        return assays;
    }

    public List<Substance> getSubstanceByETag(int skip, int top, String etag)
        throws SQLException {
        Map info = getETagInfo (etag);
        if (!Substance.class.getName().equals(info.get("type"))) {
            throw new IllegalArgumentException
                ("ETag " + etag + " not of type " + Substance.class.getName());
        }

        Cache cache = getCache ("SubstanceByETagCache");
        Object key = etag+"::"+skip+"::"+top;
        Element el = cache.get(etag);
        if (el != null) {
            Timestamp ts = (Timestamp)info.get("accessed");
            if (ts.getTime() < el.getLastAccessTime()) {
                try {
                    List<Substance> value = (List) getCacheValue (cache, key);
                    if (value != null)
                        return value;
                }
                catch (ClassCastException ex) {}
            }
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
        if (conn == null) conn = getConnection();
        PreparedStatement pst = conn.prepareStatement(sql.toString());
        try {
            pst.setString(1, etag);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                substances.add(getSubstanceBySid(rs.getLong("sid")));
            }
            rs.close();

            cache.put(new Element (key, substances));
            return substances;
        }
        finally {
            pst.close();
        }
    }

    /**
     * Returns a list of Experiments based on etag.
     * @param skip
     * @param top
     * @param etag
     * @return
     * @throws SQLException
     */
    public List<Experiment> getExperimentsByETag
        (int skip, int top, String etag) throws SQLException {

        Map info = getETagInfo (etag);
        Cache cache = getCache ("ExperimentsByETagCache");
        Object key = etag+"::"+skip+"::"+top;
        Element el = cache.get(key);
        if (el != null) {
            Timestamp ts = (Timestamp)info.get("accessed");
            if (ts.getTime() < el.getLastAccessTime()) {
                try {
                    List<Experiment> value = getCacheValue (cache, key);
                    if (value != null)
                        return value;
                }
                catch (ClassCastException ex) {}
            }
        }

        StringBuilder sql = null;
        Object type = info.get("type");
        if (Compound.class.getName().equals(type)) {
            sql = new StringBuilder
                ("select * from bard_experiment "
                 +"where bard_expt_id in (select distinct bard_expt_id "
                 +"from etag_data a, bard_experiment_data b "
                 +"where etag_id = ? "
                 +"and a.data_id = b.cid)");
        }
        else if (Substance.class.getName().equals(type)) {
            sql = new StringBuilder
                ("select * from bard_experiment "
                 +"where bard_expt_id in (select distinct bard_expt_id "
                 +"from etag_data a, bard_experiment_data b "
                 +"where etag_id = ? "
                 +"and a.data_id = b.sid)");
        }
        else if (Experiment.class.getName().equals(type)) {
            sql = new StringBuilder
                ("select a.* from  bard_experiment a, etag_data e "
                 +"where etag_id = ? and a.bard_expt_id = e.data_id "
                 +"order by e.index");
        }
        else {
            throw new IllegalArgumentException
                ("Don't know how to get Experiment's for etag "
                 +etag + " of type "+type+"!");
        }

        if (skip >= 0 && top > 0) {
            sql.append(" limit " + skip + "," + top);
        } else if (top > 0) {
            sql.append(" limit " + top);
        }

        ArrayList<Experiment> expts = new ArrayList<Experiment>();
        if (conn == null) conn = getConnection();
        PreparedStatement pst = conn.prepareStatement(sql.toString());
        try {
            pst.setString(1, etag);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                Experiment e = getExperiment (rs);
                if (e != null) {
                    expts.add(e);
                }
            }
            rs.close();

            cache.put(new Element (key, expts));
            return expts;
        }
        finally {
            pst.close();
        }
    }


    public List<Assay> getAssays(Long... assayIds) throws SQLException {
        List<Assay> assays = new ArrayList<Assay>();
        for (Long aid : assayIds) assays.add(getAssayByAid(aid));
        return assays;
    }

    public int getAssayCidCount(Long bardAssayId, boolean actives) throws SQLException {
        if (bardAssayId == null || bardAssayId < 0) return -1;
        PreparedStatement pst;
        if (conn == null) conn = getConnection();
        if (!actives)
            pst = conn.prepareStatement("select count(distinct cid) from bard_experiment_data a, bard_experiment c, bard_assay b " +
                    "where b.bard_assay_id = ? " +
                    "and b.bard_assay_id = c.bard_assay_id " +
                    "and c.bard_expt_id = a.bard_expt_id");
        else
            pst = conn.prepareStatement("select count(distinct cid) from bard_experiment_data a, bard_experiment c, bard_assay b " +
                    "where b.bard_assay_id = ? " +
                    "and b.bard_assay_id = c.bard_assay_id " +
                    "and c.bard_expt_id = a.bard_expt_id and a.outcome = 2");
        pst.setLong(1, bardAssayId);
        ResultSet rs = pst.executeQuery();
        rs.next();
        int n = rs.getInt(1);
        pst.close();
        return n;
    }

    public int getAssaySidCount(Long bardAssayId, boolean actives) throws SQLException {
        if (bardAssayId == null || bardAssayId < 0) return -1;
        PreparedStatement pst;
        if (conn == null) conn = getConnection();
        if (!actives)
            pst = conn.prepareStatement("select count(distinct sid) from bard_experiment_data a, bard_experiment c, bard_assay b " +
                    "where b.bard_assay_id = ? " +
                    "and b.bard_assay_id = c.bard_assay_id " +
                    "and c.bard_expt_id = a.bard_expt_id");
        else
            pst = conn.prepareStatement("select count(distinct sid) from bard_experiment_data a, bard_experiment c, bard_assay b " +
                    "where b.bard_assay_id = ? " +
                    "and b.bard_assay_id = c.bard_assay_id " +
                    "and c.bard_expt_id = a.bard_expt_id and a.outcome = 2");
        pst.setLong(1, bardAssayId);
        ResultSet rs = pst.executeQuery();
        rs.next();
        int n = rs.getInt(1);
        rs.close();
        pst.close();
        return n;
    }

    public int getExperimentCidCount(Long bardExptId, boolean actives) throws SQLException {
        if (bardExptId == null || bardExptId < 0) return -1;
        PreparedStatement pst;
        if (conn == null) conn = getConnection();
        if (!actives)
            pst = conn.prepareStatement("select count(distinct cid) from bard_experiment_data where bard_expt_id = ?");
        else
            pst = conn.prepareStatement("select count(distinct cid) from bard_experiment_data where bard_expt_id = ? and outcome = 2");
        pst.setLong(1, bardExptId);
        ResultSet rs = pst.executeQuery();
        rs.next();
        int n = rs.getInt(1);
        rs.close();
        pst.close();
        return n;
    }

    public int getExperimentSidCount(Long bardExptId, boolean actives) throws SQLException {
        if (bardExptId == null || bardExptId < 0) return -1;
        PreparedStatement pst;
        if (conn == null) conn = getConnection();
        if (!actives)
            pst = conn.prepareStatement("select count(distinct sid) from bard_experiment_data where bard_expt_id = ?");
        else
            pst = conn.prepareStatement("select count(distinct sid) from bard_experiment_data where bard_expt_id = ? and outcome = 2");
        pst.setLong(1, bardExptId);
        ResultSet rs = pst.executeQuery();
        rs.next();
        int n = rs.getInt(1);
        rs.close();
        pst.close();
        return n;
    }

    public List<Long> getAssaySids(Long bardAssayId, int skip, int top, boolean actives)
           throws SQLException {
           if (bardAssayId == null || bardAssayId < 0) return null;

           String cacheKey = bardAssayId + "#" + skip + "#" + top + "#" + actives;
           Cache cache = getCache ("AssaySidsCache");
           try {
               List<Long> value = (List) getCacheValue (cache, cacheKey);
               if (value != null) {
                   return value;
               }
           }
           catch (ClassCastException ex) {}

        String limitClause = generateLimitClause(skip, top);
           PreparedStatement pst;
        if (conn == null) conn = getConnection();
           if (!actives)
               pst = conn.prepareStatement("select distinct sid from bard_experiment_data a, bard_experiment c, bard_assay b  where b.bard_assay_id = ? and b.bard_assay_id = c.bard_assay_id and c.bard_expt_id = a.bard_expt_id order by sid " + limitClause);
           else
               pst = conn.prepareStatement("select distinct sid from bard_experiment_data a, bard_experiment c, bard_assay b  where b.bard_assay_id = ? and b.bard_assay_id = c.bard_assay_id and c.bard_expt_id = a.bard_expt_id and a.outcome = 2 order by sid " + limitClause);

           try {
               pst.setLong(1, bardAssayId);
               ResultSet rs = pst.executeQuery();
               List<Long> ret = new ArrayList<Long>();
               while (rs.next()) ret.add(rs.getLong("sid"));
               rs.close();
               cache.put(new Element (cacheKey, ret));
               return ret;
           }
           finally {
               pst.close();
           }
       }


    public List<Long> getAssayCids(Long bardAssayId, int skip, int top, boolean actives)
        throws SQLException {
        if (bardAssayId == null || bardAssayId < 0) return null;

        String cacheKey = bardAssayId + "#" + skip + "#" + top + "#" + actives;
        Cache cache = getCache ("AssayCidsCache");
        try {
            List<Long> value = (List) getCacheValue (cache, cacheKey);
            if (value != null) {
                return value;
            }
        }
        catch (ClassCastException ignored) {}

        String limitClause = generateLimitClause(skip, top);
        PreparedStatement pst;
        if (conn == null) conn = getConnection();
        if (!actives)
            pst = conn.prepareStatement("select distinct cid from bard_experiment_data a, bard_experiment c, bard_assay b  where b.bard_assay_id = ? and b.bard_assay_id = c.bard_assay_id and c.bard_expt_id = a.bard_expt_id order by cid " + limitClause);
        else
            pst = conn.prepareStatement("select distinct cid from bard_experiment_data a, bard_experiment c, bard_assay b  where b.bard_assay_id = ? and b.bard_assay_id = c.bard_assay_id and c.bard_expt_id = a.bard_expt_id and a.outcome = 2 order by cid " + limitClause);

        try {
            pst.setLong(1, bardAssayId);
            ResultSet rs = pst.executeQuery();
            List<Long> ret = new ArrayList<Long>();
            while (rs.next()) ret.add(rs.getLong("cid"));
            rs.close();
            cache.put(new Element (cacheKey, ret));
            return ret;
        }
        finally {
            pst.close();
        }
    }

    public List<Compound> getAssayCompounds(Long bardAssayId, int skip, int top, boolean actives)
         throws SQLException {
         if (bardAssayId == null || bardAssayId < 0) return null;

         String cacheKey = bardAssayId + "#" + skip + "#" + top + "#" + actives;
         Cache cache = getCache ("AssayCompoundsCache");
         try {
             List<Compound> value = (List<Compound>) getCacheValue (cache, cacheKey);
             if (value != null) {
                 return value;
             }
         }
         catch (ClassCastException ignored) {}

        String limitClause = generateLimitClause(skip, top);
        PreparedStatement pst;
        if (conn == null) conn = getConnection();
        if (!actives)
            pst = conn.prepareStatement("select distinct cid from bard_experiment_data a, bard_experiment c, bard_assay b  where b.bard_assay_id = ? and b.bard_assay_id = c.bard_assay_id and c.bard_expt_id = a.bard_expt_id order by cid " + limitClause);
        else
            pst = conn.prepareStatement("select distinct cid from bard_experiment_data a, bard_experiment c, bard_assay b  where b.bard_assay_id = ? and b.bard_assay_id = c.bard_assay_id and c.bard_expt_id = a.bard_expt_id and a.outcome = 2 order by cid " + limitClause);

        try {
            pst.setLong(1, bardAssayId);
            ResultSet rs = pst.executeQuery();
            List<Compound> ret = new ArrayList<Compound>();

            while (rs.next()) {
                ret.addAll(getCompoundsByCid(rs.getLong("cid")));
            }
            rs.close();
            cache.put(new Element(cacheKey, ret));
            return ret;
        } finally {
            pst.close();
        }
    }

    public List<Substance> getAssaySubstances(Long bardAssayId, int skip, int top, boolean actives)
            throws SQLException {
        if (bardAssayId == null || bardAssayId < 0) return null;

        String cacheKey = bardAssayId + "#" + skip + "#" + top + "#" + actives;
        Cache cache = getCache("AssaySubstancesCache");
        try {
            List<Substance> value = (List<Substance>) getCacheValue(cache, cacheKey);
            if (value != null) {
                return value;
            }
        } catch (ClassCastException ignored) {
        }

        String limitClause = generateLimitClause(skip, top);
        PreparedStatement pst;
        if (conn == null) conn = getConnection();
        if (!actives)
            pst = conn.prepareStatement("select distinct sid from bard_experiment_data a, bard_experiment c, bard_assay b  where b.bard_assay_id = ? and b.bard_assay_id = c.bard_assay_id and c.bard_expt_id = a.bard_expt_id order by cid " + limitClause);
        else
            pst = conn.prepareStatement("select distinct sid from bard_experiment_data a, bard_experiment c, bard_assay b  where b.bard_assay_id = ? and b.bard_assay_id = c.bard_assay_id and c.bard_expt_id = a.bard_expt_id and a.outcome = 2 order by cid " + limitClause);

        try {
            pst.setLong(1, bardAssayId);
            ResultSet rs = pst.executeQuery();
            List<Substance> ret = new ArrayList<Substance>();

            while (rs.next()) {
                ret.add(getSubstanceBySid(rs.getLong("sid")));
            }
            rs.close();
            cache.put(new Element(cacheKey, ret));
            return ret;
        } finally {
            pst.close();
        }
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
    public List<Long> getExperimentCids(Long bardExptId, int skip, int top, boolean actives)
        throws SQLException {
        if (bardExptId == null || bardExptId < 0) return null;

        String cacheKey = bardExptId + "#" + skip + "#" + top + "#" + actives;
        Cache cache = getCache ("ExperimentCidsCache");
        try {
            List<Long> value = (List) getCacheValue (cache, cacheKey);
            if (value != null) {
                return value;
            }
        }
        catch (ClassCastException ex) {}

        String limitClause = generateLimitClause(skip, top);
        PreparedStatement pst;
        if (conn == null) conn = getConnection();
        if (!actives)
            pst = conn.prepareStatement("select distinct cid from bard_experiment_data where bard_expt_id = ? order by cid " + limitClause);
        else
            pst = conn.prepareStatement("select distinct cid from bard_experiment_data where bard_expt_id = ? and outcome = 2 order by cid " + limitClause);

        try {
            pst.setLong(1, bardExptId);
            ResultSet rs = pst.executeQuery();
            List<Long> ret = new ArrayList<Long>();
            while (rs.next()) ret.add(rs.getLong("cid"));
            rs.close();

            cache.put(new Element (cacheKey, ret));
            return ret;
        }
        finally {
            pst.close();
        }
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
     * @return   a list of experiment data ids
     * @throws SQLException
     */
    public List<String> getExperimentDataIds(Long bardExptId, int skip, int top, String filter)
            throws SQLException {
        if (bardExptId == null || bardExptId < 0) return null;

        String cacheKey = bardExptId + "#" + skip + "#" + top + "#" + filter;
        Cache cache = getCache("ExperimentDataIdsCache");
        try {
            List<String> value = (List) getCacheValue(cache, cacheKey);
            if (value != null) {
                return value;
            }
        } catch (ClassCastException ex) {
        }

        if (conn == null) conn = getConnection();

        // does the exploded results table have any numeric values for this expt id?
        // if not we have to ignore some filter fields and avoid a simple join (and
        // a left outer join is prohibitive on expts from primary screens)
        PreparedStatement erpst = conn.prepareStatement("select * from exploded_results where bard_expt_id = ? limit 1");
        erpst.setLong(1, bardExptId);
        ResultSet errs = erpst.executeQuery();
        boolean hasExplodedResults = false;
        while (errs.next())  hasExplodedResults = true;
        errs.close();
        erpst.close();

        String limitClause = generateLimitClause(skip, top);

        String filterClause = "";
        String orderClause = "";
        if (filter != null) {

            // use result types for the experiment to create a list of SolrField
            // objects so that we can reuse our search filter parsing code
            List<ExperimentResultType> rtypes = getExperimentResultTypes(bardExptId, null);
            List<SolrField> fields = new ArrayList<SolrField>();
            for (ExperimentResultType rtype : rtypes) {
                fields.add(new SolrField(rtype.getName(), "float"));
            }
            fields.add(new SolrField("outcome", "text"));
            fields.add(new SolrField("order", "text"));

            Map<String, List<String>> fqs = SearchUtil.extractFilterQueries(filter, fields);
            for (String fieldName : fqs.keySet()) {
                List<String> vals = fqs.get(fieldName);
                if (vals.size() == 0) continue;

                // handle outcome specially
                if (fieldName.equals("outcome") && vals.size() == 1) {
                    if (vals.get(0).toLowerCase().contains("\"active\"")) filterClause += " and outcome = 2 ";
                    else if (vals.get(0).toLowerCase().contains("\"inactive\"")) filterClause += " and outcome = 1 ";
                } else if (hasExplodedResults && fieldName.equals("order") && vals.size() == 1) {
                    String val= vals.get(0).toLowerCase();
                    if (val.contains("\"asc")) orderClause = " order by value asc ";
                    else if (val.contains("\"desc")) orderClause = " order by value desc ";
                    else throw new SQLException("Invalid order specified. Must be asc or desc");
                } else if (hasExplodedResults) {
                    // now deal with individual result types
                    filterClause += " and display_name = '" + fieldName + "'";
                    if (!vals.get(0).contains("[")) filterClause += " and value = " + vals.get(0) + " ";
                    else { // provided a range
                        String[] toks = vals.get(0).replace("[", "").replace("]", "").split(" TO ");
                        String lower = toks[0];
                        String upper = toks[1];
                        if (lower.equals("*") && !upper.equals("*")) filterClause += " and value <= " + upper + " ";
                        else if (!lower.equals("*") && upper.equals("*"))
                            filterClause += " and value >= " + lower + " ";
                        else if (!lower.equals("*") && !upper.equals("*"))
                            filterClause += " and value >= " + lower + " and value <= " + upper + " ";
                    }
                }
            }
        }

        // TODO we join exploded_results and bard_experiment_data and then apply limits - might be better to
        // query exploded_results and bard_experiment_data in separate calls?
        PreparedStatement pst;

        if (hasExplodedResults) {
            pst = conn.prepareStatement("select distinct concat(cast(b.bard_expt_id as char), '.', cast(b.sid as char)) as id from " +
                    "exploded_results a, bard_experiment_data b " +
                    "where a.bard_expt_id = ? " +
                    filterClause +
                    "and a.expt_data_id = b.expt_data_id " +
                    orderClause +
                    limitClause);
        } else {
            pst = conn.prepareStatement("select distinct concat(cast(b.bard_expt_id as char), '.', cast(b.sid as char)) as id from " +
                    "bard_experiment_data b " +
                    "where b.bard_expt_id = ? " +
                    filterClause +
                    limitClause);
        }

        try {
            pst.setLong(1, bardExptId);
            ResultSet rs = pst.executeQuery();
            List<String> ret = new ArrayList<String>();
            while (rs.next()) ret.add(rs.getString(1));
            rs.close();

            cache.put(new Element (cacheKey, ret));
            return ret;
        }
        finally {
            pst.close();
        }
    }

    List<ExperimentData> getExperimentData(PreparedStatement pst) throws SQLException, IOException {
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
        return ret;
    }

    /**
     * Return experiment data objects for an experiment.
     *
     * @param bardExptId The experiment id (AKA Pubchem AID for experiments taken from Pubchem)
     * @param skip       how many records to skip
     * @param top        how many records to return
     * @return
     * @throws SQLException
     */
    public List<ExperimentData> getExperimentData
        (Long bardExptId, int skip, int top, String filter) throws SQLException, IOException {
        if (bardExptId == null || bardExptId < 0) return null;

        String cacheKey = bardExptId + "#" + skip + "#" + top + "#" + filter;
        Cache cache = getCache ("ExperimentDataCache");
        try {
            List<ExperimentData> value = getCacheValue (cache, cacheKey);
            if (value != null) {
                return value;
            }
        }
        catch (ClassCastException ex) {}

        String limitClause = generateLimitClause(skip, top);

        String filterClause = "";
        if (filter != null) {
            if (filter.toLowerCase().equals("active")) filterClause = " and outcome = 2 ";
            else if (filter.toLowerCase().equals("inactive")) filterClause = " and outcome = 1 ";
        }

        if (conn == null) conn = getConnection();
        PreparedStatement pst = conn.prepareStatement("select concat(cast(bard_expt_id as char), '.', cast(sid as char)) as id from bard_experiment_data where bard_expt_id = ? "+
                filterClause+" order by score desc, bard_expt_id, sid " + limitClause);
        try {
            pst.setLong(1, bardExptId);
            List<ExperimentData> ret = getExperimentData(pst);
            cache.put(new Element(cacheKey, ret));
            return ret;
        } finally {
            pst.close();
        }
    }

    /**
     * Return experiment data objects for an experiment.
     *
     * @param bardExptId The experiment id (AKA Pubchem AID for experiments taken from Pubchem)
     * @param skip       how many records to skip
     * @param top        how many records to return
     * @return
     * @throws SQLException
     */
    public List<ExperimentData> getActiveExperimentData
        (Long bardExptId, int skip, int top) throws SQLException, IOException {
        if (bardExptId == null || bardExptId < 0) return null;

        String cacheKey = bardExptId + "#" + skip + "#" + top;
        Cache cache = getCache ("ActiveExperimentDataCache");
        try {
            List<ExperimentData> value = getCacheValue (cache, cacheKey);
            if (value != null) {
                return value;
            }
        }
        catch (ClassCastException ex) {}

        String limitClause = generateLimitClause(skip, top);
        if (conn == null) conn = getConnection();
        PreparedStatement pst = conn.prepareStatement("select concat(cast(bard_expt_id as char), '.', cast(sid as char)) as id from bard_experiment_data where bard_expt_id = ? and outcome = 2 order by score desc, bard_expt_id, sid " + limitClause);
        try {
            pst.setLong(1, bardExptId);
            List<ExperimentData> ret = getExperimentData(pst);
            cache.put(new Element (cacheKey, ret));
            return ret;
        }
        finally {
            pst.close();
        }
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
    public List<String> getSubstanceDataIds(Long sid, int skip, int top, String filter)
        throws SQLException {
        if (sid == null || sid < 0) return null;

        String cacheKey = sid + "#" + skip + "#" + top + "#" + filter;
        Cache cache = getCache ("SubstanceDataIdsCache");
        try {
            List<String> value = getCacheValue (cache, cacheKey);
            if (value != null) {
                return value;
            }
        }
        catch (ClassCastException ex) {}

        String limitClause = generateLimitClause(skip, top);

        String filterClause = "";
        if (filter != null) {
            if (filter.toLowerCase().equals("active")) filterClause = " and outcome = 2 ";
            else if (filter.toLowerCase().equals("inactive")) filterClause = " and outcome = 1 ";
        }


        if (conn == null) conn = getConnection();
        PreparedStatement pst = conn.prepareStatement("select concat(cast(bard_expt_id as char), '.', cast(sid as char)) as id from bard_experiment_data where sid = ? " + filterClause +" order by classification desc, score desc " + limitClause);
        try {
            pst.setLong(1, sid);
            ResultSet rs = pst.executeQuery();
            List<String> ret = new ArrayList<String>();
            while (rs.next()) ret.add(rs.getString(1));
            rs.close();

            cache.put(new Element (cacheKey, ret));
            return ret;
        }
        finally {
            pst.close();
        }

    }

    /**
     * Get a substance by its SID.
     *
     * @param sid the SID in question
     * @return a {@link Substance} object
     * @throws SQLException TODO Should include CID and also include SMILES from CID (rel_type=1)
     */
    public Substance getSubstanceBySid (Long sid) throws SQLException {
        if (sid == null || sid < 0) return null;

        Cache cache = getCache ("SubstanceBySidCache");
        try {
            Substance value = getCacheValue (cache, sid);
            if (value != null) {
                return value;
            }
        }
        catch (ClassCastException ex) {}

        if (conn == null) conn = getConnection();
        PreparedStatement pst = conn.prepareStatement("select  a . *, b.cid, c.iso_smiles from substance a  left join cid_sid b on a.sid = b.sid  left join compound c on c.cid = b.cid where a.sid = ?");
        try {
            pst.setLong(1, sid);
            ResultSet rs = pst.executeQuery();
            Substance s = new Substance();
            if (rs.next()) {
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
            if (s.getSid() == null) return null;

            cache.put(new Element (sid, s));
            return s;
        }
        finally {
            pst.close();
        }
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
    public List<ExperimentData> getSubstanceData
        (Long sid, int skip, int top, String filter) throws SQLException, IOException {
        if (sid == null || sid < 0) return null;

        String cacheKey = sid + "#" + skip + "#" + top + "#" + filter;
        Cache cache = getCache ("SubstanceDataCache");
        try {
            List<ExperimentData> value = getCacheValue (cache, cacheKey);
            if (value != null) {
                return value;
            }
        }
        catch (ClassCastException ex) {}

        String limitClause = generateLimitClause(skip, top);

        String filterClause = "";
        if (filter != null) {
            if (filter.toLowerCase().equals("active")) filterClause = " and outcome = 2 ";
            else if (filter.toLowerCase().equals("inactive")) filterClause = " and outcome = 1 ";
        }

        if (conn == null) conn = getConnection();
        PreparedStatement pst = conn.prepareStatement("select concat(cast(bard_expt_id as char), '.', cast(sid as char)) as id from bard_experiment_data where sid = ? " +
                filterClause+" order by classification desc, score desc " + limitClause);
        try {
            pst.setLong(1, sid);
            ResultSet rs = pst.executeQuery();
            List<ExperimentData> ret = new ArrayList<ExperimentData>();
            while (rs.next())
                ret.add(getExperimentDataByDataId(rs.getString(1)));
            rs.close();

            cache.put(new Element (cacheKey, ret));
            return ret;
        }
        finally {
            pst.close();
        }
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
    public List<String> getCompoundDataIds(Long cid, int skip, int top, String filter)
        throws SQLException {
        if (cid == null || cid < 0) return null;

        String cacheKey = cid + "#" + skip + "#" + top + "#" + filter;
        Cache cache = getCache ("CompoundDataIdsCache");
        try {
            List<String> value = getCacheValue (cache, cacheKey);
            if (value != null) {
                return value;
            }
        }
        catch (ClassCastException ex) {}

        String limitClause = generateLimitClause(skip, top);

        String filterClause = "";
        if (filter != null) {
            if (filter.toLowerCase().equals("active")) filterClause = " and outcome = 2 ";
            else if (filter.toLowerCase().equals("inactive")) filterClause = " and outcome = 1 ";
        }

        if (conn == null) conn = getConnection();
        PreparedStatement pst = conn.prepareStatement("select concat(cast(bard_expt_id as char), '.', cast(sid as char)) as id from bard_experiment_data where cid = ? "+ filterClause + " order by classification desc, score desc " + limitClause);
        try {
            pst.setLong(1, cid);
            ResultSet rs = pst.executeQuery();
            List<String> ret = new ArrayList<String>();
            while (rs.next()) ret.add(rs.getString(1));
            rs.close();
            cache.put(new Element (cacheKey, ret));
            return ret;
        }
        finally {
            pst.close();
        }
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
    /*
      public List<Long> getCompoundExperimentIds(Long cid, int skip, int top)
      throws SQLException {
      if (cid == null || cid < 0) return null;

      Cache cache = getCache ("CompoundExperimentIdsCache");
      List<Long> value = (List) getCacheValue (cache, cid);
      if (value != null) {
      return value;
      }

      String limitClause = "";
      if (skip >= 0 && top > 0) {
      limitClause = "  limit " + skip + "," + top;
      }
      else if (top > 0) {
      limitClause = "  limit " + top;
      }
      else if (skip >= 0) {
      limitClause = " limit "+skip+","+CHUNK_SIZE;
      }

      PreparedStatement pst = conn.prepareStatement("select distinct(bard_expt_id) from bard_experiment_data where cid = ? order by classification desc, score desc " + limitClause);
      try {
      pst.setLong(1, cid);
      ResultSet rs = pst.executeQuery();
      List<Long> ret = new ArrayList<Long>();
      while (rs.next()) ret.add(rs.getLong(1));
      rs.close();

      cache.put(new Element (cid, ret));
      return ret;
      }
      finally {
      pst.close();
      }
      }
    */

    /**
     * Return experiment ids for a substance.
     *
     * @param sid  The Pubchem SID
     * @param skip how many records to skip
     * @param top  how many records to return
     * @return
     * @throws SQLException
     */
    public List<Long> getSubstanceExperimentIds(Long sid, int skip, int top)
        throws SQLException {
        if (sid == null || sid < 0) return null;

        String cacheKey = sid + "#" + skip + "#" + top;
        Cache cache = getCache ("SubstanceExperimentIdsCache");
        try {
            List<Long> value = getCacheValue (cache, cacheKey);
            if (value != null) {
                return value;
            }
        }
        catch (ClassCastException ex) {}

        String limitClause = generateLimitClause(skip, top);
        if (conn == null) conn = getConnection();
        PreparedStatement pst = conn.prepareStatement("select distinct(bard_expt_id) from bard_experiment_data where sid = ? order by classification desc, score desc " + limitClause);
        try {
            pst.setLong(1, sid);
            ResultSet rs = pst.executeQuery();
            List<Long> ret = new ArrayList<Long>();
            while (rs.next()) ret.add(rs.getLong(1));
            rs.close();
            cache.put(new Element (cacheKey, ret));
            return ret;
        }
        finally {
            pst.close();
        }
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
    /*
      public List<Experiment> getCompoundExperiment
      (Long cid, int skip, int top) throws SQLException {
      if (cid == null || cid < 0) return null;

      Cache cache = getCache ("CompoundExperimentCache");
      List<Experiment> value = (List) getCacheValue (cache, cid);
      if (value != null) {
      return value;
      }

      String limitClause = "";
      if (skip >= 0  && top > 0) {
      limitClause = "  limit " + skip + "," + top;
      }
      else if (top > 0) {
      limitClause = "  limit " + top;
      }
      else if (skip >= 0) {
      limitClause = " limit "+skip+","+CHUNK_SIZE;
      }

      PreparedStatement pst = conn.prepareStatement("select distinct(bard_expt_id) from bard_experiment_data where cid = ? order by classification desc,score desc  " + limitClause);
      try {
      pst.setLong(1, cid);
      ResultSet rs = pst.executeQuery();
      List<Experiment> ret = new ArrayList<Experiment>();
      while (rs.next()) ret.add(getExperimentByExptId(rs.getLong(1)));
      rs.close();
      cache.put(new Element (cid, ret));
      return ret;
      }
      finally {
      pst.close();
      }
      }
    */

    /**
     * Return experiment objects for a subtstance.
     *
     * @param sid  The Pubchem SID
     * @param skip how many records to skip
     * @param top  how many records to return
     * @return
     * @throws SQLException
     */
    public List<Experiment> getSubstanceExperiment
        (Long sid, int skip, int top) throws SQLException {
        if (sid == null || sid < 0) return null;

        String cacheKey = sid + "#" + skip + "#" + top;
        Cache cache = getCache ("SubstanceExperimentCache");
        try {
            List<Experiment> value = (List) getCacheValue (cache, cacheKey);
            if (value != null) {
                return value;
            }
        }
        catch (ClassCastException ex) {}

        String limitClause = generateLimitClause(skip, top);
        if (conn == null) conn = getConnection();
        PreparedStatement pst = conn.prepareStatement("select distinct(bard_expt_id) from bard_experiment_data where sid = ? order by classification desc, score desc " + limitClause);
        try {
            pst.setLong(1, sid);
            ResultSet rs = pst.executeQuery();
            List<Experiment> ret = new ArrayList<Experiment>();
            while (rs.next()) ret.add(getExperimentByExptId(rs.getLong(1)));
            rs.close();
            cache.put(new Element (cacheKey, ret));
            return ret;
        }
        finally {
            pst.close();
        }
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
    public List<Assay> getSubstanceAssays(Long sid, int skip, int top)
        throws SQLException {
        if (sid == null || sid < 0) return null;

        String cacheKey = sid + "#" + skip + "#" + top;
        Cache cache = getCache ("SubstanceAssaysCache");
        try {
            List<Assay> value = getCacheValue (cache, cacheKey);
            if (value != null) {
                return value;
            }
        }
        catch (ClassCastException ex) {}

        String limitClause = generateLimitClause(skip, top);
        if (conn == null) conn = getConnection();
        PreparedStatement pst = conn.prepareStatement("select distinct b.bard_assay_id from bard_experiment_data a, bard_experiment b where a.sid = ? and a.bard_expt_id = b.bard_expt_id  " + limitClause);
        try {
            pst.setLong(1, sid);
            ResultSet rs = pst.executeQuery();
            List<Assay> ret = new ArrayList<Assay>();
            while (rs.next()) ret.add(getAssayByAid(rs.getLong(1)));
            rs.close();
            cache.put(new Element (cacheKey, ret));
            return ret;
        }
        finally {
            pst.close();
        }
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
    public List<Long> getExperimentSids(Long bardExptId, int skip, int top, boolean actives)
        throws SQLException {
        if (bardExptId == null || bardExptId < 0) return null;

        String cacheKey = bardExptId + "#" + skip + "#" + top + "#" + actives;
        Cache cache = getCache ("ExperimentSidsCache");
        try {
            List<Long> value = getCacheValue (cache, cacheKey);
            if (value != null) {
                return value;
            }
        }
        catch (ClassCastException ex) {}

        String limitClause = generateLimitClause(skip, top);
        PreparedStatement pst;
        if (conn == null) conn = getConnection();
        if (!actives) pst = conn.prepareStatement("select distinct sid from bard_experiment_data where bard_expt_id = ? order by sid " + limitClause);
        else pst = conn.prepareStatement("select distinct sid from bard_experiment_data where bard_expt_id = ? and outcome = 2 order by sid " + limitClause);

        try {
            pst.setLong(1, bardExptId);
            ResultSet rs = pst.executeQuery();
            List<Long> ret = new ArrayList<Long>();
            while (rs.next()) ret.add(rs.getLong("sid"));
            rs.close();
            cache.put(new Element (cacheKey, ret));
            return ret;
        }
        finally {
            pst.close();
        }
    }

    private String generateLimitClause(Integer skip, Integer top) {
        if (skip == null) skip = -1;
        if (top == null) top = -1;

        String limitClause = "";
        if (skip >= 0 && top > 0) {
            limitClause = "  limit " + skip + "," + top;
        } else if (top > 0) {
            limitClause = "  limit " + top;
        } else if (skip >= 0) {
            limitClause = " limit " + skip + "," + CHUNK_SIZE;
        }
        return limitClause;
    }

    public List<Float[]> getExperimentResultTypeHistogram(Long bardExptId, String typeName, Integer nbin) throws SQLException {
        if (bardExptId == null || bardExptId < 0) return null;

        String cacheKey = bardExptId + "#" + typeName + "#" + nbin;
        Cache cache = getCache("ExperimentRTHistogramCache");
        try {
            List<Float[]> value = getCacheValue(cache, cacheKey);
            if (value != null) {
                return value;
            }
        } catch (ClassCastException ex) {
        }

        PreparedStatement pst;
        if (conn == null) conn = getConnection();
        List<Float[]> ret = new ArrayList<Float[]>();
        pst = conn.prepareStatement("select n, l, u from exploded_histograms where bard_expt_id = ? and display_name = ? order by l");
        pst.setLong(1, bardExptId);
        pst.setString(2, typeName);
        ResultSet rs = pst.executeQuery();
        while (rs.next()) ret.add(new Float[]{rs.getFloat("n"), rs.getFloat("l"), rs.getFloat("u")});
        rs.close();
        pst.close();

        if (nbin != null && nbin < ret.size()) { // collapse bins
            int chunkSize = (int) Math.ceil((double) ret.size() / nbin);
            List<Float[]> collapsed = new ArrayList<Float[]>();
            List<List<Float[]>> chunks = Lists.partition(ret, chunkSize);
            for (List<Float[]> chunk : chunks) {
                float count = 0;
                float l, u;
                l = chunk.get(0)[1];
                u = chunk.get(chunk.size()-1)[2];
                for (Float[] elem : chunk) count += elem[0];
                collapsed.add(new Float[]{count, l, u});
            }
            ret = collapsed;
        }
        cache.put(new Element(ret, cacheKey));
        return ret;
    }

    public List<Float[]> getExperimentResultTypeHistogram(Long bardExptId, String typeName) throws SQLException {
        return getExperimentResultTypeHistogram(bardExptId, typeName, null);
    }

    public List<ExperimentResultType> getExperimentResultTypes(Long bardExptId, Integer collapse) throws SQLException {
        if (bardExptId == null || bardExptId < 0) return null;
        PreparedStatement pst;

        String cacheKey = String.valueOf(bardExptId);
        Cache cache = getCache ("ExperimentResultTypeCache");
        try {
            List<ExperimentResultType> value = getCacheValue (cache, cacheKey);
            if (value != null) {
                return value;
            }
        }
        catch (ClassCastException ex) {}

        if (conn == null) conn = getConnection();
        List<ExperimentResultType> ret = new ArrayList<ExperimentResultType>();

        pst = conn.prepareStatement("select * from exploded_statistics where bard_expt_id = ?");
        pst.setLong(1, bardExptId);
        ResultSet rs = pst.executeQuery();
        while (rs.next()) {
            ExperimentResultType rtype = new ExperimentResultType();
            rtype.setName(rs.getString("display_name"));
            rtype.setMin(rs.getFloat("minval"));
            rtype.setMax(rs.getFloat("maxval"));
            rtype.setNum(rs.getLong("n"));
            rtype.setMean(rs.getFloat("mean"));
            rtype.setSd(rs.getFloat("sd"));
            rtype.setQ1(rs.getFloat("q1"));
            rtype.setQ2(rs.getFloat("q2"));
            rtype.setQ3(rs.getFloat("q3"));
            rtype.setHistogram(getExperimentResultTypeHistogram(bardExptId, rtype.getName(), collapse));
            ret.add(rtype);
        }
        pst.close();
        rs.close();
        cache.put(new Element(ret, cacheKey));
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
    public List<Compound> getExperimentCompounds
        (Long bardExptId, int skip, int top, boolean actives) throws SQLException {
        if (bardExptId == null || bardExptId < 0) return null;

        String cacheKey = bardExptId + "#" + skip + "#" + top + "#" + actives;
        Cache cache = getCache ("ExperimentCompoundsCache");
        try {
            List<Compound> value = getCacheValue (cache, cacheKey);
            if (value != null) {
                return value;
            }
        }
        catch (ClassCastException ex) {}

        String limitClause = generateLimitClause(skip, top);

        PreparedStatement pst;
        if (conn == null) conn = getConnection();
        if (!actives)
            pst = conn.prepareStatement("select cid, sid from bard_experiment_data where bard_expt_id = ? order by sid " + limitClause);
        else
            pst = conn.prepareStatement("select cid, sid from bard_experiment_data where bard_expt_id = ? and outcome = 2 order by sid " + limitClause);

        try {
            pst.setLong(1, bardExptId);
            ResultSet rs = pst.executeQuery();
            List<Compound> ret = new ArrayList<Compound>();

            while (rs.next()) {
                ret.addAll(getCompoundsByCid(rs.getLong("cid")));
            }
            rs.close();
            cache.put(new Element (cacheKey, ret));
            return ret;
        }
        finally {
            pst.close();
        }
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
    public List<Compound> getExperimentSubstances
        (Long bardExptId, int skip, int top, boolean actives) throws SQLException {
        if (bardExptId == null || bardExptId < 0) return null;

        String cacheKey = bardExptId + "#" + skip + "#" + top + "#" + actives;
        Cache cache = getCache ("ExperimentSubstancesCache");
        try {
            List<Compound> value = getCacheValue (cache, cacheKey);
            if (value != null) {
                return value;
            }
        }
        catch (ClassCastException ex) {}

        String limitClause = generateLimitClause(skip, top);
        PreparedStatement pst;
        if (conn == null) conn = getConnection();
        if (!actives) pst = conn.prepareStatement("select cid, sid from bard_experiment_data where bard_expt_id = ? order by sid " + limitClause);
        else pst = conn.prepareStatement("select cid, sid from bard_experiment_data where bard_expt_id = ? and outcome = 2 order by sid " + limitClause);

        try {
            pst.setLong(1, bardExptId);
            ResultSet rs = pst.executeQuery();
            List<Compound> ret = new ArrayList<Compound>();

            while (rs.next()) {
                // TODO should return a Substance entity
                ret.addAll(getCompoundsBySid(rs.getLong("sid")));
            }
            rs.close();
            cache.put(new Element (cacheKey, ret));
            return ret;
        }
        finally {
            pst.close();
        }
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

        Cache cache = getCache ("AssayPublicationsCache");
        try {
            List<Publication> value = getCacheValue (cache, bardAssayId);
            if (value != null) {
                return value;
            }
        }
        catch (ClassCastException ex) {}

        if (conn == null) conn = getConnection();
        PreparedStatement pst2 = conn.prepareStatement("select a.* from publication a, assay_pub b where b.bard_assay_id = ? and b.pmid = a.pmid");
        try {
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

            cache.put(new Element (bardAssayId, pubs));
            return pubs;
        }
        finally {
            pst2.close();
        }
    }

    /**
     * Return a list of protein targets based on on a bard assay id
     * @param bardAssayId
     * @return
     * @throws SQLException
     */
    public List<ProteinTarget> getAssayTargets(Long bardAssayId)
        throws SQLException {

        Cache cache = getCache ("AssayTargetsCache");
        try {
            List<ProteinTarget> value = getCacheValue (cache, bardAssayId);
            if (value != null) {
                return value;
            }
        }
        catch (ClassCastException ex) {}

        if (conn == null) conn = getConnection();
        PreparedStatement pst2 = conn.prepareStatement("select distinct a.* from protein_target a, assay_target b where b.bard_assay_id = ? and a.gene_id = b.gene_id");
        try {
            pst2.setLong(1, bardAssayId);
            ResultSet rs2 = pst2.executeQuery();
            List<ProteinTarget> targets = new ArrayList<ProteinTarget>();
            while (rs2.next()) {
                String acc = rs2.getString("accession");
//                targets.add(getProteinTargetByAccession(acc));
                ProteinTarget t = new ProteinTarget();
                t.setDescription(rs2.getString("description"));
                t.setGeneId(rs2.getLong("gene_id"));
                t.setName(rs2.getString("name"));
                t.setStatus(rs2.getString("uniprot_status"));
                t.setAcc(rs2.getString("accession"));
                t.setTaxId(rs2.getLong("taxid"));
                t.setClasses(getPantherClassesForAccession(t.getAcc()));
                targets.add(t);
            }
            rs2.close();
            cache.put(new Element (bardAssayId, targets));
            return targets;
        }
        finally {
            pst2.close();
        }
    }

    /**
     * Return a list of target biologies based on on a bard project id
     *
     * @param bardProjectid
     * @return
     * @throws SQLException
     */
    public List<Biology> getProjectTargets(Long bardProjectid)
            throws SQLException {
        Cache cache = getCache("ProjectTargetsCache");
        try {
            List<Biology> value = getCacheValue(cache, bardProjectid);
            if (value != null) {
                return value;
            }
        } catch (ClassCastException ex) {
        }
        List<Biology> targets = getBiologyByEntity("project", bardProjectid);
        cache.put(new Element(bardProjectid, targets));
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

        Cache cache = getCache ("SearchAssayCache");
        try {
            List<Assay> value = getCacheValue (cache, query);
            if (value != null) {
                return value;
            }
        }
        catch (ClassCastException ex) {}

        boolean freeTextQuery = false;

        if (!query.contains("[")) freeTextQuery = true;

        if (conn == null) conn = getConnection();
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
        try {
            ResultSet rs = pst.executeQuery();
            List<Assay> assays = new ArrayList<Assay>();
            while (rs.next()) {
                Long aid = rs.getLong("bard_assay_id");
                assays.add(getAssayByAid(aid));
            }
            rs.close();

            cache.put(new Element (query, assays));
            return assays;
        }
        finally {
            pst.close();
        }
    }


    public List<Assay> getAssaysByExperimentId(Long eid) throws SQLException {
        Cache cache = getCache ("AssaysByExperimentIdCache");
        try {
            List<Assay> value = getCacheValue (cache, eid);
            if (value != null) {
                return value;
            }
        }
        catch (ClassCastException ex) {}

        if (conn == null) conn = getConnection();
        PreparedStatement pst = conn.prepareStatement("select distinct bard_assay_id from bard_experiment where bard_expt_id = ?");
        try {
            pst.setLong(1, eid);
            List<Assay> assays = new ArrayList<Assay>();
            ResultSet rs = pst.executeQuery();
            while (rs.next()) assays.add(getAssayByAid(rs.getLong(1)));
            rs.close();
            cache.put(new Element (eid, assays));
            return assays;
        }
        finally {
            pst.close();
        }
    }

    /**
     * Returns assays for a given accession
     * @param acc
     * @return
     * @throws SQLException
     */
    public List<Assay> getAssaysByTargetAccession(String acc)
        throws SQLException {
        Cache cache = getCache ("AssaysByTargetAccessionCache");
        try {
            List<Assay> value = getCacheValue (cache, acc);
            if (value != null) {
                return value;
            }
        }
        catch (ClassCastException ex) {}

        if (conn == null) conn = getConnection();
        PreparedStatement pst = conn.prepareStatement("select distinct b.bard_assay_id from protein_target a, assay_target b where a.accession = ? and a.accession = b.accession");
        try {
            pst.setString(1, acc);
            List<Assay> assays = new ArrayList<Assay>();
            ResultSet rs = pst.executeQuery();
            while (rs.next()) assays.add(getAssayByAid(rs.getLong(1)));
            rs.close();
            cache.put(new Element (acc, assays));
            return assays;
        }
        finally {
            pst.close();
        }
    }

    /**
     * Returns assay for a given accession
     * @param geneid
     * @return
     * @throws SQLException
     */
    public List<Assay> getAssaysByTargetGeneid(Long geneid)
        throws SQLException {
        Cache cache = getCache ("AssaysByTargetGeneidCache");
        try {
            List<Assay> value = getCacheValue (cache, geneid);
            if (value != null) {
                return value;
            }
        }
        catch (ClassCastException ex) {}

        if (conn == null) conn = getConnection();
        PreparedStatement pst = conn.prepareStatement("select distinct b.bard_assay_id from protein_target a, assay_target b where a.gene_id = ? and a.accession = b.accession");
        try {
            pst.setLong(1, geneid);
            List<Assay> assays = new ArrayList<Assay>();
            ResultSet rs = pst.executeQuery();
            while (rs.next()) assays.add(getAssayByAid(rs.getLong(1)));
            rs.close();
            cache.put(new Element (geneid, assays));
            return assays;
        }
        finally {
            pst.close();
        }
    }


    public List<Long> getProjectIds() throws SQLException {
        Cache cache = getCache ("ProjectIdsCache");
        try {
            List<Long> value = getCacheValue (cache, "all");
            if (value != null) {
                return value;
            }
        }
        catch (ClassCastException ex) {}

        if (conn == null) conn = getConnection();
        PreparedStatement pst = conn.prepareStatement("select bard_proj_id from bard_project");
        try {
            ResultSet rs = pst.executeQuery();
            List<Long> ret = new ArrayList<Long>();
            while (rs.next()) {
                ret.add(rs.getLong(1));
            }
            rs.close();
            cache.put(new Element ("all", ret));
            return ret;
        }
        finally {
            pst.close();
        }
    }

    /**
     * Return a list of all aids.
     *
     * @return
     * @throws SQLException
     */
    public List<Long> getAssayCount() throws SQLException {
        Cache cache = getCache ("AssayCountCache");
        try {
            List<Long> value = getCacheValue (cache, "all");
            if (value != null) {
                return value;
            }
        }
        catch (ClassCastException ex) {}

        if (conn == null) conn = getConnection();
        PreparedStatement pst = conn.prepareStatement("select bard_assay_id from bard_assay order by bard_assay_id");
        try {
            ResultSet rs = pst.executeQuery();
            List<Long> ret = new ArrayList<Long>();
            while (rs.next()) {
                ret.add(rs.getLong(1));
            }
            rs.close();
            cache.put(new Element ("all", ret));
            return ret;
        }
        finally {
            pst.close();
        }
    }

    /**
     * Returns a list of all experiment ids.
     *
     * @return
     * @throws SQLException
     */
    /*
      public List<Long> getExperimentIds() throws SQLException {
      Cache cache = getCache ("ExperimentIdsCache");
      List<Long> value = (List) getCacheValue (cache, "all");
      if (value != null) {
      return value;
      }

      PreparedStatement pst = conn.prepareStatement("select bard_expt_id from bard_experiment order by bard_expt_id");
      try {
      ResultSet rs = pst.executeQuery();
      List<Long> ret = new ArrayList<Long>();
      while (rs.next()) {
      ret.add(rs.getLong("bard_expt_id"));
      }
      rs.close();
      cache.put(new Element ("all", ret));
      return ret;
      }
      finally {
      pst.close();
      }
      }
    */

    public Project getProject(Long bardProjId) throws SQLException {
        Cache cache = getCache ("ProjectCache");
        try {
            Project value =  getCacheValue (cache, bardProjId);
            if (value != null) {
                return value;
            }
        }
        catch (ClassCastException ex) {}

        Project p = null;
        if (conn == null) conn = getConnection();
        PreparedStatement pst = conn.prepareStatement("select a.*, b.name as source_name from bard_project a left join source b on a.depositor_id = b.source_id where a.bard_proj_id = ?");
        try {
            pst.setLong(1, bardProjId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                p = new Project();
                p.setBardProjectId(bardProjId);
                p.setDescription(rs.getString("description"));
                p.setName(rs.getString("name"));
                p.setDeposited(rs.getDate("deposited"));
                p.setSource(rs.getString("source_name"));
                p.setCapProjectId(rs.getLong("cap_proj_id"));
                p.setScore(rs.getFloat("score"));

                cache.put(new Element (bardProjId, p));
            }
            rs.close();
        } finally {
            pst.close();
        }

        if (p == null) {
            return p;
        }

        // publications
        pst = conn.prepareStatement("select pmid from project_pub where bard_proj_id = ?");
        pst.setLong(1, bardProjId);
        try {
            List<Long> pubs = new ArrayList<Long>();
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                pubs.add(rs.getLong(1));
            }
            rs.close();
            p.setPublications(pubs);
        } finally {
            pst.close();
        }


        // probe details
        List<Long> probeIds = getProbeCidsForProject(bardProjId);
        p.setProbeIds(probeIds);
        p.setProbes(getCompoundsByCid(probeIds.toArray(new Long[]{})));

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
            ("select distinct a.bard_assay_id from bard_experiment a, bard_project_experiment b " +
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

        //find targets, get collected bard_assay_ids, for each bard_assay_id under the project
        p.setTargets(getProjectTargets(bardProjId));

        // experiment types - this can't go in Experiment, since the 'type' of the
        // experiment depends on how it was used in a project
        pst = conn.prepareStatement("select * from bard_project_experiment where bard_proj_id = ?");
        try {
            pst.setLong(1, bardProjId);
            ResultSet rs = pst.executeQuery();
            Map<Long, String> etypes = new HashMap<Long, String>();
            while (rs.next()) etypes.put(rs.getLong("bard_expt_id"), rs.getString("expt_type"));
            rs.close();
            p.setExperimentTypes(etypes);
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
    public List<Project> getProjectByExperimentId(Long bardExptId)
        throws SQLException {

        Cache cache = getCache ("ProjectByExperimentIdCache");
        try {
            List<Project> value = getCacheValue (cache, bardExptId);
            if (value != null) {
                return value;
            }
        }
        catch (ClassCastException ex) {}

        if (conn == null) conn = getConnection();
        PreparedStatement pst = conn.prepareStatement("select bard_proj_id from bard_project_experiment where bard_expt_id = ?");
        try {
            pst.setLong(1, bardExptId);
            ResultSet rs = pst.executeQuery();
            List<Project> ps = new ArrayList<Project>();
            while (rs.next()) {
                Project project = getProject(rs.getLong("bard_proj_id"));
                if (project != null) ps.add(project);
            }
            rs.close();

            cache.put(new Element (bardExptId, ps));
            return ps;
        }
        finally {
            pst.close();
        }
    }

    public List<Long> getProjectIdByProbeId(String probeId) throws SQLException {
        List<Long> ids = new ArrayList<Long>();
        if (conn == null) conn = getConnection();
        PreparedStatement pst = conn.prepareStatement("select bard_proj_id from project_probe where probe_id = ?");
        pst.setString(1, probeId);
        ResultSet rs = pst.executeQuery();
        while (rs.next()) ids.add(rs.getLong(1));
        rs.close();
        pst.close();
        return ids;
    }

    public List<Project> getProjectByProbeId(String probeId)
            throws SQLException {

        Cache cache = getCache("ProjectByProbeIdCache");
        try {
            List<Project> value = getCacheValue(cache, probeId);
            if (value != null) {
                return value;
            }
        } catch (ClassCastException ex) {
        }

        if (conn == null) conn = getConnection();
        PreparedStatement pst = conn.prepareStatement("select bard_proj_id from project_probe where probe_id = ?");
        try {
            pst.setString(1, probeId);
            ResultSet rs = pst.executeQuery();
            List<Project> ps = new ArrayList<Project>();
            while (rs.next()) {
                Project project = getProject(rs.getLong("bard_proj_id"));
                if (project != null) ps.add(project);
            }
            rs.close();

            cache.put(new Element(probeId, ps));
            return ps;
        } finally {
            pst.close();
        }
    }

    /**
     * Returns the bard_project_ids for projects based on a bard_assay_id
     * @param bardAssayId
     * @return
     * @throws SQLException
     */
    public List<Project> getProjectByAssayId(Long bardAssayId)
        throws SQLException {
        Cache cache = getCache ("ProjectByAssayIdCache");
        try {
            List<Project> value = getCacheValue (cache, bardAssayId);
            if (value != null) {
                return value;
            }
        }
        catch (ClassCastException ex) {}

        if (conn == null) conn = getConnection();
        PreparedStatement pst = conn.prepareStatement("select distinct b.bard_proj_id from bard_experiment a, bard_project_experiment b where a.bard_assay_id = ? and a.bard_expt_id = b.bard_expt_id");
        try {
            pst.setLong(1, bardAssayId);
            ResultSet rs = pst.executeQuery();
            List<Long> pids = new ArrayList<Long>();
            while (rs.next()) pids.add(rs.getLong("bard_proj_id"));
            rs.close();

            List<Project> projs = getProjects
                (pids.toArray(new Long[pids.size()]));

            cache.put(new Element (bardAssayId, projs));
            return projs;
        }
        finally {
            pst.close();
        }
    }

    public List<Long> getProbeCidsForProject(Long bardProjectId)
        throws SQLException {
        Cache cache = getCache ("ProbesForProjectCache");
        try {
            List<Long> value = getCacheValue (cache, bardProjectId);
            if (value != null) {
                return value;
            }
        }
        catch (ClassCastException ex) {}

        if (conn == null) conn = getConnection();
        //        PreparedStatement pst = conn.prepareStatement("select a.cid from bard_experiment_data a, compound b where b.probe_id is not null and a.bard_expt_id = ? and a.cid = b.cid");
        PreparedStatement pst = conn.prepareStatement("select * from project_probe where bard_proj_id = ?");
        try {
            pst.setLong(1, bardProjectId);
            ResultSet rs = pst.executeQuery();
            List<Long> probeids = new ArrayList<Long>();
            while (rs.next()) probeids.add(rs.getLong("cid"));
            rs.close();
            cache.put(new Element (bardProjectId, probeids));
            return probeids;
        }
        finally {
            pst.close();
        }
    }

    /* ****************/
    /*  Query methods */
    /* ****************/

    public List<ExperimentData> searchForExperimentData(String query, int skip, int top) throws SQLException, IOException {

        Cache cache = getCache ("SearchForExperimentDataCache");
        try {
            List<ExperimentData> value = getCacheValue (cache, query);
            if (value != null) {
                return value;
            }
        }
        catch (ClassCastException ex) {}

        boolean freeTextQuery = false;

        if (!query.contains("[")) freeTextQuery = true;

        if (conn == null) conn = getConnection();
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
        cache.put(new Element (query, experimentData));
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
        if (queryParams.getJoin() != null) {
            sql += " where "+queryParams.getJoin();
        }
        if (conn == null) conn = getConnection();
        PreparedStatement pst = conn.prepareStatement(sql);
        try {
            ResultSet rs = pst.executeQuery();
            int n = 0;
            while (rs.next()) n = rs.getInt(1);
            rs.close();
            return (n);
        }
        finally {
            pst.close();
        }
    }

    public <T extends BardEntity> int getEntityCount(Class<T> klass, String query) throws SQLException {
        Query queryParams;
        if (fieldMap.containsKey(klass)) queryParams = fieldMap.get(klass);
        else throw new IllegalArgumentException("Invalid entity class was specified");

        String sql;
        if (query != null && !query.contains("[")) {
            String q = "'%" + query + "%' ";
            List<String> tmp = new ArrayList<String>();
            for (String s : queryParams.getValidFields())
                tmp.add(s + " like " + q);

            String tmp2 = "";
            if (!tmp.isEmpty())
                tmp2 = "(" + Util.join(tmp, " or ") + ")";

            sql = "select count(" + queryParams.getIdField() + ") from "
                    + queryParams.getTableName() + " where ";
            if (queryParams.getJoin() != null) {
                sql += queryParams.getJoin() + " AND ";
            }
            sql += tmp2;
        } else {
            // TODO we currently only assume a single query field is specified
            String[] toks = query.split("\\[");
            String q = toks[0].trim();
            String field = toks[1].trim().replace("]", "");
            if (!queryParams.getValidFields().contains(field))
                throw new SQLException("Invalid field was specified");

            sql = "select count(" + queryParams.getIdField() + ") from "
                    + queryParams.getTableName() + " where ";
            if (queryParams.getJoin() != null) {
                sql += queryParams.getJoin() + " AND ";
            }
            sql += field + " like '%" + q + "%'";
        }

        if (queryParams.getJoin() != null) {
            sql += " where " + queryParams.getJoin();
        }
        if (conn == null) conn = getConnection();
        PreparedStatement pst = conn.prepareStatement(sql);
        try {
            ResultSet rs = pst.executeQuery();
            int n = 0;
            while (rs.next()) n = rs.getInt(1);
            rs.close();
            return (n);
        }
        finally {
            pst.close();
        }
    }


    public int getCompoundTestCount() throws SQLException {
        if (conn == null) conn = getConnection();
        PreparedStatement pst = conn.prepareStatement("select count(distinct cid) from bard_experiment_data");
        ResultSet rs = pst.executeQuery();
        try {
            rs.next();
            int n = rs.getInt(1);
            rs.close();
            return n;
        }
        finally {
            pst.close();
        }
    }

    public int getCompoundActiveCount() throws SQLException {
        if (conn == null) conn = getConnection();
        PreparedStatement pst = conn.prepareStatement("select count(distinct cid) from bard_experiment_data where outcome = 2");
        try {
            ResultSet rs = pst.executeQuery();
            rs.next();
            int n = rs.getInt(1);
            rs.close();
            return n;
        }
        finally {
            pst.close();
        }
    }

    public int getSubstanceTestCount() throws SQLException {
        if (conn == null) conn = getConnection();
        PreparedStatement pst = conn.prepareStatement("select count(distinct sid) from bard_experiment_data");
        try {
            ResultSet rs = pst.executeQuery();
            rs.next();
            int n = rs.getInt(1);
            rs.close();
            return n;
        }
        finally {
            pst.close();
        }
    }

    public int getSubstanceActiveCount() throws SQLException {
        if (conn == null) conn = getConnection();
        PreparedStatement pst = conn.prepareStatement("select count(distinct sid) from bard_experiment_data where outcome = 2");
        try {
            ResultSet rs = pst.executeQuery();
            rs.next();
            int n = rs.getInt(1);
            rs.close();
            return n;
        }
        finally {
            pst.close();
        }
    }

    private List<String> getCompoundAnnotationKeys() throws SQLException {
        List<String> ret = new ArrayList<String>();
        if (conn == null) conn = getConnection();
        PreparedStatement pst = conn.prepareStatement("select distinct annot_key from compound_annot order by annot_key");
        try {
            ResultSet rs = pst.executeQuery();
            while (rs.next()) ret.add(rs.getString(1));
            rs.close();
            return ret;
        }
        finally {
            pst.close();
        }
    }

    /**
     * Return compounds based on a query.
     *
     * @param filter The filter, currently indicating active or tested compounds
     * @param skip Number of results to skip
     * @param top Number of results to return
     * @param hasAnno if <code>true</code> only return compounds that have annotations, otherwise
     *                return all compounds satisfying the query
     * @return a list of {@link Compound} objects
     * @throws SQLException
     */
    public List<Compound> searchForCompounds(String filter, int skip, int top, boolean hasAnno) throws SQLException {

        List<String> annokeys = getCompoundAnnotationKeys();
        List<SolrField> fields = new ArrayList<SolrField>();
        for (String annokey : annokeys) {
            fields.add(new SolrField(annokey, "string"));
        }
        fields.add(new SolrField("annotation", "text"));
        fields.add(new SolrField("active", "text"));
        fields.add(new SolrField("order", "text"));

        List<Compound> ret = new ArrayList<Compound>();
        String limitClause = generateLimitClause(skip, top);

        boolean filterForActives = false;

//        Map<String, List<String>> fqs = SearchUtil.extractFilterQueries(filter, fields);
//        for (String fieldName : fqs.keySet()) {
//            List<String> vals = fqs.get(fieldName);
//            if (vals.size() == 0) continue;
//
//            // handle outcome specially
//            if (fieldName.equals("active") && vals.size() == 1) {
//                filterForActives = true;
//            } else if (hasExplodedResults && fieldName.equals("order") && vals.size() == 1) {
//                String val= vals.get(0).toLowerCase();
//                if (val.contains("\"asc")) orderClause = " order by value asc ";
//                else if (val.contains("\"desc")) orderClause = " order by value desc ";
//                else throw new SQLException("Invalid order specified. Must be asc or desc");
//            } else if (hasExplodedResults) {
//                // now deal with individual result types
//                filterClause += " and display_name = '" + fieldName + "'";
//                if (!vals.get(0).contains("[")) filterClause += " and value = " + vals.get(0) + " ";
//                else { // provided a range
//                    String[] toks = vals.get(0).replace("[", "").replace("]", "").split(" TO ");
//                    String lower = toks[0];
//                    String upper = toks[1];
//                    if (lower.equals("*") && !upper.equals("*")) filterClause += " and value <= " + upper + " ";
//                    else if (!lower.equals("*") && upper.equals("*"))
//                        filterClause += " and value >= " + lower + " ";
//                    else if (!lower.equals("*") && !upper.equals("*"))
//                        filterClause += " and value >= " + lower + " and value <= " + upper + " ";
//                }
//            }
//        }

        String sql = "select distinct cid from bard_experiment_data order by cid " + limitClause;
        if (filter.contains("active"))
            sql = "select distinct cid from bard_experiment_data where outcome = 2 order by cid " + limitClause;

        Cache cache = getCache("SearchForCompoundCache");
        try {
            List<Compound> value = getCacheValue(cache, sql);
            if (value != null) {
                return value;
            }
        } catch (ClassCastException ex) {
        }

        log.info("## SQL: " + sql);

        if (conn == null) conn = getConnection();
        PreparedStatement pst = conn.prepareStatement(sql);
        try {
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                if (!hasAnno) ret.addAll(getCompoundsByCid(rs.getLong(1)));
                else {
                    List<Compound> cs = getCompoundsByCid(rs.getLong(1));
                    for (Compound c : cs) {
                        if (getCompoundAnnotations(c.getCid()).get("anno_key").length > 0) ret.add(c);
                    }
                }
            }
            rs.close();
            cache.put(new Element(sql, ret));
            return ret;
        }
        finally {
            pst.close();
        }
    }


    /**
     * Get substances based on query string.
     *
     * @param filter the query string. Currently can be "tested" or "active", with the
     *               default being "tested"
     * @param skip records to skip
     * @param top number of records to return
     * @param hasAnno ignored, since we don't deal with substance annotations
     * @return
     * @throws SQLException
     */
    public List<Substance> searchForSubstances(String filter, int skip, int top, boolean hasAnno) throws SQLException {
        List<Substance> ret = new ArrayList<Substance>();
        String limitClause = generateLimitClause(skip, top);
        String sql = "select distinct sid from bard_experiment_data order by sid " + limitClause;
        if (filter != null && filter.contains("active"))
            sql = "select distinct sid from bard_experiment_data where outcome = 2 order by sid " + limitClause;

        Cache cache = getCache("SearchForSubstanceCache");
        try {
            List<Substance> value = getCacheValue(cache, sql);
            if (value != null) {
                return value;
            }
        } catch (ClassCastException ex) {
        }

        log.info("## SQL: " + sql);

        if (conn == null) conn = getConnection();
        PreparedStatement pst = conn.prepareStatement(sql);
        try {
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                ret.add(getSubstanceBySid(rs.getLong(1)));
            }
            rs.close();
            cache.put(new Element(sql, ret));
        
            return ret;
        }
        finally {
            pst.close();
        }
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

        String limitClause = generateLimitClause(skip, top);

        PreparedStatement pst;
        String sql;
        if (query == null && top > 0) { // get all rows - caller had better implement paging
            sql = "select " + queryParams.getIdField()
                + " from " + queryParams.getTableName();
            if (queryParams.getJoin() != null) {
                sql += " where "+queryParams.getJoin();
            }
            sql += " order by " + queryParams.getOrderField()
                + " " + limitClause;
        } else if (query != null && !query.contains("[")) {
            String q = "'%" + query + "%' ";
            List<String> tmp = new ArrayList<String>();
            for (String s : queryParams.getValidFields())
                tmp.add(s + " like " + q);

            String tmp2 = "";
            if (!tmp.isEmpty())
                tmp2 = "(" + Util.join(tmp, " or ") + ")";

            sql = "select " + queryParams.getIdField() + " from "
                + queryParams.getTableName() + " where ";
            if (queryParams.getJoin() != null) {
                sql += queryParams.getJoin() + " AND ";
            }
            sql += tmp2 + " order by " + queryParams.getOrderField()
                + " " + limitClause;
        } else {
            // TODO we currently only assume a single query field is specified
            String[] toks = query.split("\\[");
            String q = toks[0].trim();
            String field = toks[1].trim().replace("]", "");
            if (!queryParams.getValidFields().contains(field))
                throw new SQLException("Invalid field was specified");

            sql = "select " + queryParams.getIdField() + " from "
                + queryParams.getTableName() + " where ";
            if (queryParams.getJoin() != null) {
                sql += queryParams.getJoin()+" AND ";
            }
            sql += field + " like '%" + q + "%' order by "
                + queryParams.getOrderField() + "  " + limitClause;
        }

        Cache cache = getCache ("SearchForEntityCache");
        try {
            List<T> value = getCacheValue (cache, sql);
            if (value != null) {
                return value;
            }
        }
        catch (ClassCastException ex) {}

        log.info("## SQL: "+sql);
        System.out.println("## searchForEntity SQL: "+sql);

        if (conn == null) conn = getConnection();
        pst = conn.prepareStatement(sql);
        ResultSet rs = pst.executeQuery();
        List<T> entities = new ArrayList<T>();
        while (rs.next()) {
            Object id = rs.getObject(queryParams.getIdField());
            Object entity = null;
            if (klass.equals(Publication.class)) entity = getPublicationByPmid((Long) id);
            if (klass.equals(Biology.class)) entity = getBiologyBySerial((Long) id);
            else if (klass.equals(ProteinTarget.class)) entity = getProteinTargetByAccession((String) id);
            else if (klass.equals(Project.class)) entity = getProject((Long) id);
            else if (klass.equals(Experiment.class)) entity = getExperimentByExptId((Long) id);
            else if (klass.equals(Compound.class)) entity = getCompoundsByCid((Long) id);
            else if (klass.equals(Substance.class)) entity = getSubstanceBySid((Long) id);
            else if (klass.equals(Assay.class)) entity = getAssayByAid((Long) id);
            else if (klass.equals(ETag.class)) entity = getEtagByEtagId((String) id);
            if (entity != null) {
                if (entity instanceof List) entities.addAll((Collection<T>) entity);
                else if (entity instanceof BardEntity) entities.add((T) entity);
            }
        }
        rs.close();
        pst.close();

        cache.put(new Element (sql, entities));
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
        else if (Project.class.getName().equals(etag.getType()))
            entities = (List<T>) getProjectsByETag(skip, top, etag.getEtag());
        return entities;
    }

    /*
     * Return all known ETag's for a given principal
     */
    public List<String> getETagsForEntity (int skip, int top,
                                           Principal principal,
                                           Class<? extends BardEntity> clazz)
        throws SQLException {

        String limits = "";
        if (skip >= 0 && top > 0) {
            limits = " limit "+skip+","+top;
        }
        else if (top > 0) {
            limits = " limit "+top;
        }

        /*
         * TODO: we should do proper checking of principal here!
         */
        String sql = "select etag_id from etag where status = 1";
        PreparedStatement pst;
        if (conn == null) conn = getConnection();
        if (clazz != null) {
            pst = conn.prepareStatement(sql+" and type = ?" + limits);
            pst.setString(1, clazz.getName());
        }
        else {
            pst = conn.prepareStatement(sql+limits);
        }

        try {
            List<String> etags = new ArrayList<String>();
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                String id = rs.getString(1);
                etags.add(id);
            }
            rs.close();

            return etags;
        }
        finally {
            pst.close();
        }
    }


    /**
     * **********************************************************************
     * <p/>
     * CAP related methods (dictionary, annotations)
     * <p/>
     * ************************************************************************
     */

    private List<CAPAnnotation> convertKeggToAnno(ResultSet rs, String entity, Integer entityId) throws SQLException {
        List<CAPAnnotation> annos = new ArrayList<CAPAnnotation>();
        while (rs.next()) {
            String[] toks;
            String diseaseName = rs.getString("disease_names");
            String diseaseCat = rs.getString("disease_category");
            String diseaseId = rs.getString("disease_id");
            String url = "http://www.kegg.jp/medicus-bin/search?q=" + diseaseId + "&display=disease&from=disease";

            CAPAnnotation anno = new CAPAnnotation(null,
                    entityId.intValue(),
                    diseaseName, null,
                    "keggdiseaseid", diseaseId,
                    diseaseId, "KEGG",
                    url, -1, entity,
                    diseaseCat, null);
            annos.add(anno);
        }
        return annos;
    }

    private List<CAPAnnotation> convertGoToAnno(ResultSet rs, String entity, Integer entityId) throws SQLException {
        List<CAPAnnotation> annos = new ArrayList<CAPAnnotation>();
        while (rs.next()) {
            String term = rs.getString("go_term");
            String goid = rs.getString("go_id");
            String gotype = rs.getString("go_type");
            String targetAcc = rs.getString("target_acc");
            String assoc = rs.getString("go_assoc_db_ref");
            String evCode = rs.getString("ev_code");

            assoc = assoc == null ? "" : assoc;
            String related = "target="+targetAcc+",gotype="+gotype+",evcode="+evCode+",ev="+assoc;

            // work out the direct parent of an annotation
            // In go_term2term, term1_id is id of the parent term and term2_id is id of the child term.
            // Since we want the parent of the current term, it is the child
            StringBuilder parentId = new StringBuilder();
//            String delim = "";
//            PreparedStatement pst = conn.prepareStatement("select acc from go_term where id in (select term1_id from go_term2term a, go_term b where b.acc = ? and a.term2_id = b.id)");
//            pst.setString(1, goid);
//            ResultSet trs = pst.executeQuery();
//            while (trs.next()) {
//                parentId.append(delim).append(trs.getString("acc"));
//                delim = ",";
//            }
//            pst.close();
            related += ",parentid="+parentId;

            CAPAnnotation anno = new CAPAnnotation(null,
                    entityId.intValue(),
                    term, null,
                    "goid", goid,
                    goid, "GO",
                    "http://amigo.geneontology.org/cgi-bin/amigo/term_details?term=" + goid, -1, entity,
                    related, null);
            annos.add(anno);
        }
        return annos;
    }

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
     * @param bardAssayId The assay identifier. This is currently a BARD assay identifier.
     * @return A list of assay annotations
     * @throws SQLException
     */
    public List<CAPAnnotation> getAssayAnnotations(Long bardAssayId) throws SQLException {
        Cache cache = getCache ("AssayAnnotationsCache");
        try {
            List<CAPAnnotation> value = getCacheValue
                (cache, bardAssayId);
            if (value != null) {
                return value;
            }
        }
        catch (ClassCastException ex) {}

        if (conn == null) conn = getConnection();
        PreparedStatement pst = conn.prepareStatement("select a.* from cap_annotation a where a.entity = 'assay' and a.entity_id = ?");
        PreparedStatement gopst = conn.prepareStatement("select * from go_assay where bard_assay_id = ? and implied = 0 order by go_type");
        PreparedStatement keggpst = conn.prepareStatement("select a.* from kegg_gene2disease a,  (select distinct um.acc as gene_id from bard_biology a, uniprot_map um  where a.entity = 'assay' and a.entity_id = ? and a.biology_dict_id = 1398 and um.uniprot_acc = a.ext_id and acc_type = 'GeneID' union select distinct ext_id as gene_id from bard_biology a  where a.entity = 'assay' and a.entity_id = ? and a.biology_dict_id = 880) t where t.gene_id = a.gene_id");
        try {
            pst.setLong(1, bardAssayId);
            ResultSet rs = pst.executeQuery();
            List<CAPAnnotation> annos = new ArrayList<CAPAnnotation>();
            while (rs.next()) {
                String anno_id = rs.getString("anno_id");
                String anno_key = rs.getString("anno_key");
                String anno_value = rs.getString("anno_value");
                String anno_display = rs.getString("anno_display");
                String anno_value_text = rs.getString("anno_value_text");
                int displayOrder = rs.getInt("display_order");
                String source = rs.getString("source");
                String entity = rs.getString("entity");
                String url = rs.getString("url");
                String contextName = rs.getString("context_name");
                String contextGroup = rs.getString("context_group");

                String related = rs.getString("related");
                String extValueId = null;
                if (related != null && !related.trim().equals("")) {
                    String[] toks = related.split("\\|");
                    if (toks.length == 2) extValueId = toks[1];
                }
                if (extValueId == null && anno_value_text != null) extValueId = anno_value_text;

                // TODO Updated the related annotations field to support grouping
                CAPAnnotation anno = new CAPAnnotation(Integer.parseInt(anno_id), null,
                        anno_display, contextName, anno_key, anno_value,
                        extValueId, source, url, displayOrder, entity, related, contextGroup);
                annos.add(anno);
            }
            rs.close();

            // now pull in GO annotations and create CAPAnnotation objects from them
            gopst.setLong(1, bardAssayId);
            rs = gopst.executeQuery();
            annos.addAll(convertGoToAnno(rs, "assay", bardAssayId.intValue()));
            rs.close();

            // pull in KEGG disease annotations
            keggpst.setLong(1, bardAssayId);
            keggpst.setLong(2, bardAssayId);
            rs = keggpst.executeQuery();
            annos.addAll(convertKeggToAnno(rs, "assay", bardAssayId.intValue()));
            rs.close();
            keggpst.close();

            cache.put(new Element (bardAssayId, annos));
            return annos;
        }
        finally {
            pst.close();
            gopst.close();
            keggpst.close();
        }
    }

    public List<CAPAnnotation> getExperimentAnnotations(Long bardExptId) throws SQLException {
        Cache cache = getCache ("ExperimentAnnotationsCache");
        try {
            List<CAPAnnotation> value = getCacheValue
                    (cache, bardExptId);
            if (value != null) {
                return value;
            }
        }
        catch (ClassCastException ex) {}

        if (conn == null) conn = getConnection();
        PreparedStatement pst = conn.prepareStatement("select a.* from cap_annotation a where a.entity = 'experiment' and a.entity_id = ?");
        try {
            pst.setLong(1, bardExptId);
            ResultSet rs = pst.executeQuery();
            List<CAPAnnotation> annos = new ArrayList<CAPAnnotation>();
            while (rs.next()) {
                String anno_id = rs.getString("anno_id");
                String anno_key = rs.getString("anno_key");
                String anno_value = rs.getString("anno_value");
                String anno_display = rs.getString("anno_display");
                String anno_value_text = rs.getString("anno_value_text");
                int displayOrder = rs.getInt("display_order");
                String source = rs.getString("source");
                String entity = rs.getString("entity");
                String url = rs.getString("url");
                String contextName = rs.getString("context_name");
                String contextGroup = rs.getString("context_group");

                String related = rs.getString("related");
                String extValueId = null;
                if (related != null && !related.trim().equals("")) {
                    String[] toks = related.split("\\|");
                    if (toks.length == 2) extValueId = toks[1];
                }
                if (extValueId == null && anno_value_text != null) extValueId = anno_value_text;

                // TODO Updated the related annotations field to support grouping
                CAPAnnotation anno = new CAPAnnotation(Integer.parseInt(anno_id), null,
                        anno_display, contextName, anno_key, anno_value,
                        extValueId, source, url, displayOrder, entity, related, contextGroup);
                annos.add(anno);
            }
            rs.close();
            cache.put(new Element (bardExptId, annos));
            return annos;
        }
        finally {
            pst.close();
        }
    }

    public List<CAPAnnotation> getProjectAnnotations(Long bardProjectId)
        throws SQLException {
        Cache cache = getCache ("ProjectAnnotationsCache");
        try {
            List<CAPAnnotation> value =
                getCacheValue (cache, bardProjectId);
            if (value != null) {
                return value;
            }
        }
        catch (ClassCastException ex) {}

        if (conn == null) conn = getConnection();
        PreparedStatement pst = conn.prepareStatement("select a.* from cap_project_annotation a, bard_project b where b.bard_proj_id = ? and a.cap_proj_id = b.cap_proj_id");
        PreparedStatement gopst = conn.prepareStatement("select * from go_project where bard_proj_id = ? and implied = 0 order by go_type");
        // ensure we select biologies that used Entrez Gene ID
        PreparedStatement keggpst = conn.prepareStatement("select a.* from kegg_gene2disease a,  (select distinct um.acc as gene_id from bard_biology a, uniprot_map um  where a.entity = 'project' and a.entity_id = ? and a.biology_dict_id = 1398 and um.uniprot_acc = a.ext_id and acc_type = 'GeneID' union select distinct ext_id as gene_id from bard_biology a  where a.entity = 'project' and a.entity_id = ? and a.biology_dict_id = 880) t where t.gene_id = a.gene_id");
        try {
            pst.setLong(1, bardProjectId);
            ResultSet rs = pst.executeQuery();
            List<CAPAnnotation> annos = new ArrayList<CAPAnnotation>();
            while (rs.next()) {
                String anno_id = rs.getString("anno_id");
                String anno_key = rs.getString("anno_key");
                String anno_value = rs.getString("anno_value");
                String anno_display = rs.getString("anno_display");
                String source = rs.getString("source");
                int displayOrder = rs.getInt("display_order");
                String entity = rs.getString("entity");
                String contextRef = rs.getString("context_name");
                String contextGroup = rs.getString("context_group");
                String url = rs.getString("url");

                String related = rs.getString("related");
                String extValueId = null;
                if (related != null && !related.trim().equals("")) {
                    String[] toks = related.split("\\|");
                    if (toks.length == 2) extValueId = toks[1];
                }
                CAPAnnotation anno = new CAPAnnotation(Integer.parseInt(anno_id), null,
                        anno_display, contextRef, anno_key, anno_value,
                        extValueId, source, url, displayOrder, entity, related, contextGroup);
                annos.add(anno);
            }
            rs.close();

            // now pull in GO annotations and create CAPAnnotation objects from them
            gopst.setLong(1, bardProjectId);
            rs = gopst.executeQuery();
            annos.addAll(convertGoToAnno(rs, "project", bardProjectId.intValue()));
            rs.close();

            // deal with KEGG annotations
            keggpst.setLong(1, bardProjectId);
            keggpst.setLong(2, bardProjectId);
            rs = keggpst.executeQuery();
            annos.addAll(convertKeggToAnno(rs, "project", bardProjectId.intValue()));
            rs.close();
            keggpst.close();

            cache.put(new Element (bardProjectId, annos));
            return annos;
        }
        finally {
            pst.close();
            gopst.close();
            keggpst.close();
        }
    }

    public CAPDictionary getCAPDictionary()
        throws SQLException, IOException, ClassNotFoundException {

        Cache cache = getCache ("CAPDictionaryCache");
        try {
            CAPDictionary cap = getCacheValue (cache, "cap");
            if (cap != null) {
                return cap;
            }
        }
        catch (ClassCastException ex) {
            log.warn("** Cache miss due to ClassLoader changed");
        }


        if (conn == null) conn = getConnection();
        PreparedStatement pst = conn.prepareStatement("select dict, ins_date from cap_dict_obj order by ins_date desc");
        try {
            ResultSet rs = pst.executeQuery();
            Object obj = null;

            if (rs.next()) {
                byte[] buf = rs.getBytes(1);
                log.info("Retrived CAP dictionary blob with ins_date = "
                         +rs.getDate(2));
                ObjectInputStream objectIn = null;
                if (buf != null) {
                    objectIn = new ObjectInputStream(new ByteArrayInputStream(buf));
                    obj = objectIn.readObject();
                }
            }
            rs.close();

            if (!(obj instanceof CAPDictionary)) return null;
                
            cache.put(new Element ("cap", obj));
            return (CAPDictionary)obj;
        }
        finally {
            pst.close();
        }
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
        if (conn == null) conn = getConnection();
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

    /**
     * Return a list of entities in which the specified CID is active.
     *
     * In general this is based on the experiment that the CID is active in
     *
     * @param cid
     * @param entity
     * @param skip
     * @param top
     * @param <T>
     * @return
     * @throws SQLException
     */
    public <T> List<T> getEntitiesByActiveCid
        (Long cid, Class<T> entity, Integer skip, Integer top)
        throws SQLException {
        String sql = null;
        PreparedStatement pst;

        if (cid == null || cid < 0) return null;
        String limitClause = generateLimitClause(skip, top);

        if (entity.isAssignableFrom(Assay.class)) {
            sql = "select distinct b.bard_assay_id from bard_experiment_data a, bard_experiment b where a.cid = ? and a.bard_expt_id = b.bard_expt_id  and a.outcome = 2 order by a.classification desc, score desc " + limitClause;
        } else if (entity.isAssignableFrom(Project.class)) {
            //			JB: original sql joined compound and used the proj_id in experiment which no longer exists since one experiment can have multiple projects
            //        	It also used a nested select.
            //			I'll leave this here for review in case the new query doesn't perform as expected
            //
            //            sql = "select p.bard_proj_id from project p, bard_experiment e where e.bard_expt_id in " +
            //            		"(select distinct ed.bard_expt_id from bard_experiment_data ed, bard_experiment e, compound a " +
            //            		"where a.cid = ? and ed.cid = a.cid and ed.bard_expt_id = e.bard_expt_id) and e.proj_id = p.proj_id";

            //new query: uses bard_project, doesn't join with compound
            sql = "select distinct(pe.bard_proj_id) from bard_experiment_data ed, bard_project_experiment pe " +
                "where ed.cid = ? and pe.bard_expt_id=ed.bard_expt_id and ed.outcome = 2 order by ed.classification desc, score desc "+limitClause;
        } else if (entity.isAssignableFrom(Substance.class)) {
            sql = "select a.sid from cid_sid a, bard_experiment_data b where a.cid = ? and a.cid = b.cid and b.outcome = 2 order by classification desc, score desc " + limitClause;
        } else if (entity.isAssignableFrom(ExperimentData.class)) {
            sql = "select concat(cast(bard_expt_id as char), '.', cast(sid as char)) as id from bard_experiment_data where cid = ? and outcome = 2 order by expt_data_id order by classification desc, score desc "+limitClause;
        } else if (entity.isAssignableFrom(Experiment.class)) {
            sql = "select distinct bard_expt_id from bard_experiment_data where cid = ? and outcome = 2 order by classification desc, score desc "+limitClause;
        }

        Cache cache = getCache ("EntitiesByActiveCid::"+entity.getClass());
        try {
            List<T> value = (List) getCacheValue (cache, cid);
            if (value != null) {
                return value;
            }
        }
        catch (ClassCastException ex) {}

        if (conn == null) conn = getConnection();
        pst = conn.prepareStatement(sql);
        try {
            pst.setLong(1, cid);
            ResultSet rs = pst.executeQuery();
            List<T> ret = new ArrayList<T>();
            while (rs.next()) {
                Long id = rs.getLong(1);
                if (entity.isAssignableFrom(Assay.class)) ret.add((T) getAssayByAid(id));
                else if (entity.isAssignableFrom(Project.class)) ret.add((T) getProject(id));
                else if (entity.isAssignableFrom(Substance.class)) ret.add((T) getSubstanceBySid(id));
                else if (entity.isAssignableFrom(Experiment.class)) ret.add((T) getExperimentByExptId(id));
                else if (entity.isAssignableFrom(ExperimentData.class)) ret.add((T) getExperimentDataByDataId(rs.getString(1)));
            }
            rs.close();
            cache.put(new Element (cid, ret));
            return ret;
        }
        finally {
            pst.close();
        }
    }

    public <T> List<T> getRecentEntities(Class<T> entity, Integer n) throws SQLException {
        if (n == null || n <= 0) n = 5;
        String sql = null;
        PreparedStatement pst;
        String limitClause = " limit " + n;
        if (entity.isAssignableFrom(Assay.class)) {
            sql = "select bard_assay_id from bard_assay order by updated desc ";
        } else if (entity.isAssignableFrom(Project.class)) {
            sql = "select bard_proj_id from bard_project order by updated desc ";
        } else if (entity.isAssignableFrom(Experiment.class)) {
            sql = "select bard_expt_id  from bard_experiment order by updated desc";
        } else if (entity.isAssignableFrom(Substance.class)) {
            sql = "select sid from substance order by updated desc";
        } else if (entity.isAssignableFrom(Biology.class)) {
            sql = "select serial from bard_biology order by updated desc";
        }
        sql += limitClause;
        if (conn == null) conn = getConnection();
        pst = conn.prepareStatement(sql);
        try {
            ResultSet rs = pst.executeQuery();
            List<T> ret = new ArrayList<T>();

            while (rs.next()) {
                Long id = rs.getLong(1);
                if (entity.isAssignableFrom(Assay.class)) ret.add((T) getAssayByAid(id));
                else if (entity.isAssignableFrom(Project.class)) ret.add((T) getProject(id));
                else if (entity.isAssignableFrom(Substance.class)) ret.add((T) getSubstanceBySid(id));
                else if (entity.isAssignableFrom(Experiment.class)) ret.add((T) getExperimentByExptId(id));
                else if (entity.isAssignableFrom(Biology.class)) ret.add((T) getBiologyBySerial(id).get(0));
            }
            rs.close();
            return ret;
        } finally {
            pst.close();
        }
    }

    public <T> List<T> getEntitiesByCid(Long cid, Class<T> entity, Integer skip, Integer top) throws SQLException {
        String sql = null;
        PreparedStatement pst;

        if (cid == null || cid < 0) return null;
        String limitClause = generateLimitClause(skip, top);

        if (entity.isAssignableFrom(Assay.class)) {
            sql = "select distinct b.bard_assay_id "
                +"from bard_experiment_data a, bard_experiment b "
                +"where a.cid = ? and a.bard_expt_id = b.bard_expt_id  "
                +"order by a.classification desc, score desc "
                + limitClause;
        } else if (entity.isAssignableFrom(Project.class)) {
            //			JB: original sql joined compound and used the proj_id in experiment which no longer exists since one experiment can have multiple projects
            //        	It also used a nested select.
            //			I'll leave this here for review in case the new query doesn't perform as expected
            //        	
            //            sql = "select p.bard_proj_id from project p, bard_experiment e where e.bard_expt_id in " +
            //            		"(select distinct ed.bard_expt_id from bard_experiment_data ed, bard_experiment e, compound a " +
            //            		"where a.cid = ? and ed.cid = a.cid and ed.bard_expt_id = e.bard_expt_id) and e.proj_id = p.proj_id";

            //new query: uses bard_project, doesn't join with compound
            sql = "select distinct(pe.bard_proj_id) "
                +"from bard_experiment_data ed,bard_project_experiment pe "
                +"where ed.cid = ? and pe.bard_expt_id=ed.bard_expt_id "
                +"and pe.bard_proj_id > 0 "
                +"order by ed.classification desc, score desc "
                +limitClause;
        } else if (entity.isAssignableFrom(Substance.class)) {
            sql = "select sid from cid_sid where cid = ? order by sid " + limitClause;
        } else if (entity.isAssignableFrom(ExperimentData.class)) {
            sql = "select concat(cast(bard_expt_id as char), '.', cast(sid as char)) as id from bard_experiment_data where cid = ? order by classification desc, score desc " +limitClause;
        } else if (entity.isAssignableFrom(Experiment.class)) {
            sql = "select distinct bard_expt_id from bard_experiment_data where cid = ? order by classification desc, score desc "+limitClause;
        }
        else {
            throw new IllegalArgumentException
                ("Unsupported entity class: "+entity);
        }

        Cache cache = getCache ("EntitiesByCidCache::"+entity.getName());
        try {
            List<T> value = (List<T>) getCacheValue (cache, cid);
            if (value != null) {
                return value;
            }
        }
        catch (ClassCastException ex) {}

        if (conn == null) conn = getConnection();
        pst = conn.prepareStatement(sql);
        try {
            pst.setLong(1, cid);
            ResultSet rs = pst.executeQuery();
            List<T> ret = new ArrayList<T>();
            while (rs.next()) {
                if (entity.isAssignableFrom(Assay.class)) ret.add((T) getAssayByAid(rs.getLong(1)));
                else if (entity.isAssignableFrom(Project.class)) ret.add((T) getProject(rs.getLong(1)));
                else if (entity.isAssignableFrom(Substance.class)) ret.add((T) getSubstanceBySid(rs.getLong(1)));
                else if (entity.isAssignableFrom(Experiment.class)) ret.add((T) getExperimentByExptId(rs.getLong(1)));
                else if (entity.isAssignableFrom(ExperimentData.class)) ret.add((T) getExperimentDataByDataId(rs.getString(1)));
            }
            rs.close();
            cache.put(new Element (cid, ret));
            return ret;
        }
        finally {
            pst.close();
        }
    }


    public <T> Integer getEntityCountByCid(Long cid, Class<T> entity) throws SQLException {
        String sql = null;
        PreparedStatement pst;

        if (cid == null || cid < 0) return null;

        if (entity.isAssignableFrom(Assay.class)) {
            sql = "select count(distinct b.bard_assay_id) "
                    + "from bard_experiment_data a, bard_experiment b "
                    + "where a.cid = ? and a.bard_expt_id = b.bard_expt_id  "
                    + "order by a.classification desc, score desc ";
        } else if (entity.isAssignableFrom(Project.class)) {
            sql = "select count(distinct(pe.bard_proj_id)) "
                    + "from bard_experiment_data ed,bard_project_experiment pe "
                    + "where ed.cid = ? and pe.bard_expt_id=ed.bard_expt_id "
                    + "and pe.bard_proj_id > 0 "
                    + "order by ed.classification desc, score desc ";
        } else if (entity.isAssignableFrom(Substance.class)) {
            sql = "select count(sid) from cid_sid where cid = ? order by sid ";
        } else if (entity.isAssignableFrom(ExperimentData.class)) {
            sql = "select count(concat(cast(bard_expt_id as char), '.', cast(sid as char))) as id from bard_experiment_data where cid = ? order by classification desc, score desc ";
        } else if (entity.isAssignableFrom(Experiment.class)) {
            sql = "select count(distinct bard_expt_id) from bard_experiment_data where cid = ? order by classification desc, score desc ";
        }

        String cacheName = "Class";
        if (entity.isAssignableFrom(Assay.class)) cacheName = "Assay";
        else if (entity.isAssignableFrom(Project.class)) cacheName = "Project";
        else if (entity.isAssignableFrom(Substance.class)) cacheName = "Substance";
        else if (entity.isAssignableFrom(Experiment.class)) cacheName = "Experiment";
        else if (entity.isAssignableFrom(ExperimentData.class)) cacheName = "ExperimentData";
        Cache cache = getCache("EntityCountByCidCache::" + cacheName);

        try {
            Integer value = getCacheValue(cache, cid);
            if (value != null) return value;
        } catch (ClassCastException ignored) {
        }

        if (conn == null) conn = getConnection();
        pst = conn.prepareStatement(sql);
        try {
            pst.setLong(1, cid);
            ResultSet rs = pst.executeQuery();
            rs.next();
            Integer ret = rs.getInt(1);
            rs.close();
            cache.put(new Element(cid, ret));
            return ret;
        } finally {
            pst.close();
        }
    }

    public <T> Integer getEntityCountByActiveCid(Long cid, Class<T> entity)
            throws SQLException {
        String sql = null;
        PreparedStatement pst;

        if (entity.isAssignableFrom(Assay.class)) {
            sql = "select count(distinct b.bard_assay_id) from bard_experiment_data a, bard_experiment b where a.cid = ? and a.bard_expt_id = b.bard_expt_id  and a.outcome = 2 order by a.classification desc, score desc ";
        } else if (entity.isAssignableFrom(Project.class)) {
            sql = "select count(distinct(pe.bard_proj_id)) from bard_experiment_data ed, bard_project_experiment pe " +
                    "where ed.cid = ? and pe.bard_expt_id=ed.bard_expt_id and ed.outcome = 2 order by ed.classification desc, score desc ";
        } else if (entity.isAssignableFrom(Substance.class)) {
            sql = "select count(a.sid) from cid_sid a, bard_experiment_data b where a.cid = ? and a.cid = b.cid and b.outcome = 2 order by classification desc, score desc ";
        } else if (entity.isAssignableFrom(ExperimentData.class)) {
            sql = "select count(concat(cast(bard_expt_id as char), '.', cast(sid as char))) as id from bard_experiment_data where cid = ? and outcome = 2 order by expt_data_id order by classification desc, score desc ";
        } else if (entity.isAssignableFrom(Experiment.class)) {
            sql = "select count(distinct bard_expt_id) from bard_experiment_data where cid = ? and outcome = 2 order by classification desc, score desc ";
        }

        Cache cache = getCache("EntityCountByActiveCid::" + entity.getClass());
        try {
            Integer value = getCacheValue(cache, cid);
            if (value != null) return value;
        } catch (ClassCastException ignored) {
        }

        if (conn == null) conn = getConnection();
        pst = conn.prepareStatement(sql);
        try {
            pst.setLong(1, cid);
            ResultSet rs = pst.executeQuery();
            rs.next();
            Integer ret = rs.getInt(1);

            rs.close();
            cache.put(new Element(cid, ret));
            return ret;
        } finally {
            pst.close();
        }
    }

    public Map<String, Object> getProjectSumary(Long projectId) throws SQLException {
        Cache cache = getCache ("ProjectSummaryCache");
        try {
            Map<String, Object> value = getCacheValue (cache, projectId);
            if (value != null) {
                return value;
            }
        }
        catch (ClassCastException ex) {}

        Project project = getProject(projectId);
        if (project == null || project.getBardProjectId() == null)
            return null;

        int pcount = 0, syncount = 0, nassay = 0;

        if (conn == null) conn = getConnection();
        PreparedStatement pst = conn.prepareStatement("select sum(a.purchased_count) as pcount, sum(a.synthesized_count) as scount from bard_experiment a join bard_project_experiment b on a.bard_expt_id=b.bard_expt_id where  b.bard_proj_id = ?");
        pst.setLong(1, projectId);
        ResultSet rs = pst.executeQuery();
        while(rs.next()) {
            pcount = rs.getInt("pcount");
            syncount = rs.getInt("scount");
        }
        rs.close();
        pst.close();

        List<Experiment> expts = new ArrayList<Experiment>();
        pst = conn.prepareStatement("select * from bard_project_experiment where bard_proj_id = ?");
        pst.setLong(1, projectId);
        rs = pst.executeQuery();
        while (rs.next()) expts.add(getExperimentByExptId(rs.getLong("bard_expt_id")));
        rs.close();
        pst.close();

        pst = conn.prepareStatement("select count(*) from bard_project_experiment a, bard_experiment b where a.bard_proj_id = ? and a.bard_expt_id = b.bard_expt_id");
        pst.setLong(1, projectId);
        rs = pst.executeQuery();
        rs.next();
        nassay = rs.getInt(1);
        rs.close();
        pst.close();

        List<Long> probeIds = getProbeCidsForProject(projectId);
        List<Compound> probes = getCompoundsByCid(probeIds.toArray(new Long[]{}));
        List<String> probeReports = new ArrayList<String>();
        for (Compound c : probes) {
            if (c.getProbeId()!= null) probeReports.add("http://www.ncbi.nlm.nih.gov/books/n/mlprobe/"+c.getProbeId()+"/");
        }
        Map<String, Object> ret = new HashMap<String, Object>();
        ret.put("name", project.getName());
        ret.put("probes", probes);
        ret.put("probe_reports", probeReports);
        ret.put("depositor", project.getSource());
        ret.put("description", project.getDescription());
        ret.put("targets", project.getTargets());
        ret.put("cmpd_purchase_count", pcount);
        ret.put("cmpd_synthesis_count", syncount);
        ret.put("assay_count", nassay);
        ret.put("experiment_count", expts.size());
        ret.put("experiments", expts);

        cache.put(new Element (projectId, ret));

        return ret;
    }

    public List<String> getChemblTargetClasses(List<String> accs, int level) throws SQLException {
        if (level < 1 || level > 8) throw new IllegalArgumentException("Level must be between 1 & 8, inclusive");
        if (accs.size() < 1) throw new IllegalArgumentException("Must provide at least one Uniprot accession");

        if (conn == null) conn = getConnection();
        StringBuilder sb = new StringBuilder("(");
        String delim = "";
        for (String acc : accs) {
            sb.append(delim).append("'").append(acc).append("'");
            delim = ",";
        }
        sb.append(")");
        PreparedStatement pst = conn.prepareStatement("select distinct a.accession, d.l1, d.l2, d.l3, d.l4, d.l5, d.l6, d.l7, d.l8 from assay_target a, gene2uniprot b, " +
                "chembl_13.target_dictionary c,chembl_13.target_class d " +
                "where a.gene_id = b.gene_id " +
                "and b.accession = c.protein_accession " +
                "and a.accession in " + sb.toString() +
                "and c.tid = d.tid");
        try {
            ResultSet rs = pst.executeQuery();
            Map<String, String> map = new HashMap<String, String>();
            while (rs.next()) {
                String acc = rs.getString(1);
            String tclass = rs.getString("l"+level);
            map.put(acc, tclass);
            }
            rs.close();
            List<String> ret = new ArrayList<String>();
            for (String acc : accs) ret.add(map.get(acc));
            return ret;
        }
        finally {
            pst.close();
        }
    }

    public List<ProjectStep> getProjectStepsByProjectId(Long projectId) throws SQLException {
        if (conn == null) conn = getConnection();
        PreparedStatement pst = conn.prepareStatement("select * from project_step where bard_proj_id = ?");
        try {
            pst.setLong(1, projectId);
            List<ProjectStep> steps = new ArrayList<ProjectStep>();
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                ProjectStep step = new ProjectStep();
                step.setStepId(rs.getLong("step_id"));
                step.setBardProjId(projectId);
                step.setEdgeName(rs.getString("edge_name"));
                step.setNextBardExpt(rs.getLong("next_bard_expt_id"));
                step.setPrevBardExpt(rs.getLong("prev_bard_expt_id"));
                step.setAnnotations(getProjectStepAnnotations(step.getStepId()));
                step.setPrevStageRef(getExperimentTypeByProject(projectId, step.getPrevBardExpt()));
                step.setNextStageRef(getExperimentTypeByProject(projectId, step.getNextBardExpt()));
                steps.add(step);
            }
            rs.close();
            return steps;
        }
        finally {
            pst.close();
        }
    }

    public String getExperimentTypeByProject(Long bardProjId, Long bardExptId) throws SQLException {
        if (conn == null) conn = getConnection();
        PreparedStatement pst = conn.prepareStatement("select expt_type from bard_project_experiment where bard_proj_id = ? and bard_expt_id = ?");
        try {
            pst.setLong(1, bardProjId);
            pst.setLong(2, bardExptId);
            ResultSet rs = pst.executeQuery();
            String ret = null;
            while (rs.next()) ret = rs.getString(1);
            rs.close();
            return ret;
        }
        finally {
            pst.close();
        }
    }

    public List<CAPAnnotation> getProjectStepAnnotations(Long projectStepId) throws SQLException {
        if (conn == null) conn = getConnection();

        Cache cache = getCache ("ProjectStepAnnotationsCache");
        try {
            List<CAPAnnotation> value = getCacheValue
                    (cache, projectStepId);
            if (value != null) {
                return value;
            }
        }
        catch (ClassCastException ex) {}

        if (conn == null) conn = getConnection();
        PreparedStatement pst = conn.prepareStatement("select a.* from cap_project_annotation a where a.bard_proj_id = ?");
        try {
            pst.setLong(1, projectStepId);
            ResultSet rs = pst.executeQuery();
            List<CAPAnnotation> annos = new ArrayList<CAPAnnotation>();
            while (rs.next()) {
                String anno_id = rs.getString("anno_id");
                String anno_key = rs.getString("anno_key");
                String anno_value = rs.getString("anno_value");
                String anno_display = rs.getString("anno_display");
                int displayOrder = rs.getInt("display_order");
                String source = rs.getString("source");
                String entity = rs.getString("entity");
                String contextName = rs.getString("context_name");
                String contextGroup = rs.getString("context_group");

                String related = rs.getString("related");
                String extValueId = null;
                if (related != null && !related.trim().equals("")) {
                    String[] toks = related.split("\\|");
                    if (toks.length == 2) extValueId = toks[1];
                }

                // TODO Updated the related annotations field to support grouping
                CAPAnnotation anno = new CAPAnnotation(Integer.parseInt(anno_id), projectStepId.intValue(),
                        anno_display, contextName, anno_key, anno_value,
                        extValueId, source, null, displayOrder, entity, related,contextGroup);
                annos.add(anno);
            }
            rs.close();
            cache.put(new Element (projectStepId, annos));
            return annos;
        }
        finally {
            pst.close();
        }
    }

    public List<TargetClassification> getPantherClassesForAccession(String acc) throws SQLException {
        if (conn == null) conn = getConnection();

        Cache cache = getCache("PantherClassesCache");
        try {
            List<TargetClassification> value = getCacheValue(cache, acc);
            if (value != null) return value;
        } catch (ClassCastException e) {
        }

        PreparedStatement pst = conn.prepareStatement("select b.* from panther_uniprot_map a, panther_class b where a.accession = ? and a.pclass_id = b.pclass_id order by node_level");
        try {
            pst.setString(1, acc);
            ResultSet rs = pst.executeQuery();
            List<TargetClassification> classes = new ArrayList<TargetClassification>();
            while (rs.next()) {
                PantherClassification pc = new PantherClassification();
                pc.setDescription(rs.getString("class_descr"));
                pc.setName(rs.getString("class_name"));
                pc.setId(rs.getString("pclass_id"));
                pc.setLevelIdentifier(rs.getString("node_code"));
                pc.setNodeLevel(rs.getInt("node_level"));
                classes.add(pc);
            }
            rs.close();

            cache.put(new Element (acc, classes));
            return classes;
        } finally {
            pst.close();
        }
    }
    public List<ProteinTarget> getProteinTargetsForPantherClassification(String clsid) throws SQLException {
        if (conn == null) conn = getConnection();

        Cache cache = getCache("TargetsForPantherClassCache");
        try {
            List<ProteinTarget> value = getCacheValue(cache, clsid);
            if (value != null) return value;
        } catch (ClassCastException e) {
        }

        PreparedStatement pst = conn.prepareStatement("select distinct a.accession from panther_uniprot_map a where a.pclass_id = ?");
        try {
            pst.setString(1, clsid);
            ResultSet rs = pst.executeQuery();
            List<ProteinTarget> targets = new ArrayList<ProteinTarget>();
            while (rs.next()) {
                String acc = rs.getString(1);
                targets.add(getProteinTargetByAccession(acc));
            }
            rs.close();

            cache.put(new Element(clsid, targets));
            return targets;
        } finally {
            pst.close();
        }
    }

    public List<Biology> getBiologyByDictId(String dictId) 
        throws SQLException {
        if (conn == null) conn = getConnection();
        PreparedStatement pst;
        pst = conn.prepareStatement("select * from bard_biology where biology_dict_id = ?");
        try {
            pst.setString(1, dictId);
            ResultSet rs = pst.executeQuery();
            List<Biology> bios = new ArrayList<Biology>();
            while (rs.next()) {
                Biology bio = new Biology();
                bio.setSerial(rs.getLong("serial"));
                bio.setBiology(Biology.BiologyType.fromString(rs.getString("biology")));
                bio.setName(rs.getString("description"));
                bio.setDictId(rs.getLong("biology_dict_id"));
                bio.setDictLabel(rs.getString("biology_dict_label"));
                bio.setEntity(rs.getString("entity"));
                bio.setEntityId(rs.getLong("entity_id"));
                bio.setExtId(rs.getString("ext_id"));
                bio.setExtRef(rs.getString("ext_ref"));
                bios.add(bio);
            }
            rs.close();
            return bios;
        }
        finally {
            pst.close();
        }
    }

    public List<Biology> getBiologyByType(String typeName, String extId) throws SQLException {
        if (conn == null) conn = getConnection();

        String cachKey = typeName;
        if (extId != null) cachKey = cachKey + "#" + extId;
        Cache cache = getCache("BiologyCache");
        try {
            List<Biology> value = getCacheValue(cache, cachKey);
            if (value != null) return value;
        } catch (ClassCastException e) {
        }


        PreparedStatement pst;
        if (extId == null) {
            pst = conn.prepareStatement("select * from bard_biology where biology = ?");
            pst.setString(1, typeName);
        } else {
            pst = conn.prepareStatement("select * from bard_biology where biology = ? and ext_id = ?");
            pst.setString(1, typeName);
            pst.setString(2, extId);
        }

        try {
            ResultSet rs = pst.executeQuery();
            List<Biology> bios = new ArrayList<Biology>();
            while (rs.next()) {
                Biology bio = new Biology();
                bio.setSerial(rs.getLong("serial"));
                bio.setBiology(Biology.BiologyType.fromString(rs.getString("biology")));
                bio.setName(rs.getString("description"));
                bio.setDictId(rs.getLong("biology_dict_id"));
                bio.setDictLabel(rs.getString("biology_dict_label"));
                bio.setEntity(rs.getString("entity"));
                bio.setEntityId(rs.getLong("entity_id"));
                bio.setExtId(rs.getString("ext_id"));
                bio.setExtRef(rs.getString("ext_ref"));
                bios.add(bio);
            }
            cache.put(new Element(bios, cachKey));
            rs.close();
            return bios;
        }
        finally {
            pst.close();
        }
    }

    public List<Biology> getBiologyByType(String typeName) throws SQLException {
        return getBiologyByType(typeName, null);
    }

    public List<Biology> getBiologyBySerial(Long serial) throws SQLException {
        if (conn == null) conn = getConnection();

        Cache cache = getCache("BiologyCache");
        try {
            List<Biology> value = getCacheValue(cache, serial);
            if (value != null) return value;
        } catch (ClassCastException e) {
        }

        PreparedStatement pst = conn.prepareStatement("select * from bard_biology where serial = ?");
        try {
            pst.setLong(1, serial);
            ResultSet rs = pst.executeQuery();
            List<Biology> bios = new ArrayList<Biology>();
            while (rs.next()) {
                Biology bio = new Biology();
                bio.setSerial(rs.getLong("serial"));
                bio.setBiology(Biology.BiologyType.fromString(rs.getString("biology")));
                bio.setName(rs.getString("description"));
                bio.setDictId(rs.getLong("biology_dict_id"));
                bio.setDictLabel(rs.getString("biology_dict_label"));
                bio.setEntity(rs.getString("entity"));
                bio.setEntityId(rs.getLong("entity_id"));
                bio.setExtId(rs.getString("ext_id"));
                bio.setExtRef(rs.getString("ext_ref"));
                bios.add(bio);
            }
            cache.put(new Element (bios, serial));
            rs.close();

            return bios;
        }
        finally {
            pst.close();
        }
    }

    public List<Biology> getBiologyByEntity(String entity, long entityId) throws SQLException {
        if (conn == null) conn = getConnection();
        Cache cache = getCache("BiologyCache");
        try {
            List<Biology> value = getCacheValue(cache, entity+"#"+entityId);
            if (value != null) return value;
        } catch (ClassCastException e) {
        }

        PreparedStatement pst = conn.prepareStatement("select * from bard_biology where entity = ? and entity_id = ?");
        try {
            pst.setString(1, entity);
            pst.setLong(2, entityId);
            ResultSet rs = pst.executeQuery();
            List<Biology> bios = new ArrayList<Biology>();
            while (rs.next()) {
                Biology bio = new Biology();
                bio.setSerial(rs.getLong("serial"));
                bio.setBiology(Biology.BiologyType.fromString(rs.getString("biology")));
                bio.setName(rs.getString("description"));
                bio.setDictId(rs.getLong("biology_dict_id"));
                bio.setDictLabel(rs.getString("biology_dict_label"));
                bio.setEntity(entity);
                bio.setEntityId(entityId);
                bio.setExtId(rs.getString("ext_id"));
                bio.setExtRef(rs.getString("ext_ref"));
                bios.add(bio);
            }
            cache.put(new Element (bios, entity+"#"+entityId));
            rs.close();
            return bios;
        }
        finally {
            pst.close();
        }
    }

    public List<Biology> getBiologyByEntity(String entity, long entityId, String typeName) throws SQLException {
        if (conn == null) conn = getConnection();
        Cache cache = getCache("BiologyCache");
        try {
            List<Biology> value = getCacheValue(cache, entity + "#" + entityId + "#" + typeName);
            if (value != null) return value;
        } catch (ClassCastException e) {
        }

        PreparedStatement pst = conn.prepareStatement("select * from bard_biology where entity = ? and entity_id = ?");
        try {
            pst.setString(1, entity);
            pst.setLong(2, entityId);
            ResultSet rs = pst.executeQuery();
            List<Biology> bios = new ArrayList<Biology>();
            while (rs.next()) {
                Biology bio = new Biology();
                bio.setSerial(rs.getLong("serial"));
                bio.setBiology(Biology.BiologyType.fromString(rs.getString("biology")));
                bio.setName(rs.getString("description"));
                bio.setDictId(rs.getLong("biology_dict_id"));
                bio.setDictLabel(rs.getString("biology_dict_label"));
                bio.setEntity(entity);
                bio.setEntityId(entityId);
                bio.setExtId(rs.getString("ext_id"));
                bio.setExtRef(rs.getString("ext_ref"));
                bios.add(bio);
            }
            cache.put(new Element (bios, entity + "#" + entityId + "#" + typeName));
            rs.close();
            return bios;
        }
        finally {
            pst.close();
        }
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

        System.out.println(db.getProjectStepsByProjectId(2l));
//        String etag = db.newETag("test", Compound.class.getName());
//        int cnt = db.putETag(etag, 1l, 2l, 3l, 4l, 5l);
//        System.out.println(etag + ": " + cnt);
        db.closeConnection();
    }
}
