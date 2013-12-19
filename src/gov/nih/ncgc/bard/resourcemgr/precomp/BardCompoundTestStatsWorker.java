package gov.nih.ncgc.bard.resourcemgr.precomp;

import gov.nih.ncgc.bard.capextract.CAPUtil;
import gov.nih.ncgc.bard.resourcemgr.BardDBUtil;
import gov.nih.ncgc.bard.resourcemgr.extresource.pubchem.BardCompoundPubchemExtrasLoader;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import chemaxon.formats.MolFormatException;
import chemaxon.struc.Molecule;
import chemaxon.util.MolHandler;

public class BardCompoundTestStatsWorker {

    static final private Logger logger = 
		LoggerFactory.getLogger(BardCompoundPubchemExtrasLoader.class.getName());
    
    String sqlSelectCIDActiveData = "select cid,count(distinct(bard_expt_id)) from bard_experiment_data " +
    		"where (outcome = 2 or outcome = 5) group by cid";

    String sqlSelectCIDTestedData = "select cid,count(distinct(bard_expt_id)) from bard_experiment_data " +
		"group by cid";

    String sqlSelectCIDActiveAssayData = "select b.cid, count(distinct(a.bard_assay_id)) "+
    "from bard_experiment a, bard_experiment_data b " +
    "where a.bard_expt_id=b.bard_expt_id and (b.outcome = 2 or b.outcome = 5) group by b.cid";
    
    String sqlSelectCIDTestedAssayData = "select b.cid, count(distinct(a.bard_assay_id)) "+
    "from bard_experiment a, bard_experiment_data b " +
    "where a.bard_expt_id=b.bard_expt_id group by b.cid";
    
    String sqlUpdateCIDActiveCnt = "update compound set active_expt_cnt = ? where cid = ?";
    
    String sqlUpdateCIDTestedCnt = "update compound set tested_expt_cnt = ? where cid = ?";

    String sqlUpdateCIDAssayActiveCnt = "update compound set active_assay_cnt = ? where cid = ?";
    
    String sqlUpdateCIDAssayTestedCnt = "update compound set tested_assay_cnt = ? where cid = ?";

    String sqlSetActiveExptNullToZero = "update compound set active_expt_cnt = 0 where active_expt_cnt is null";
    String sqlSetTestedExptNullToZero = "update compound set tested_expt_cnt = 0 where tested_expt_cnt is null";
    String sqlSetActiveAssayNullToZero = "update compound set active_assay_cnt = 0 where active_assay_cnt is null";
    String sqlSetTestedAssayNullToZero = "update compound set tested_assay_cnt = 0 where tested_assay_cnt is null";
    
    private Connection conn;
    
    public void updateCompoundTestStatus(String serverURL) {
	try {
	    
	    logger.info("Starting compound test stat update");
	    
	    conn = CAPUtil.connectToBARD(serverURL);
	    conn.setAutoCommit(false);
	    
	    // zero out untested compounds
	    this.zeroUntestedCompounds();
	    
	    long start = System.currentTimeMillis();
	    updateCompoundStatus(sqlSelectCIDActiveData,sqlUpdateCIDActiveCnt, "active expt update");	
	    logger.info("ET active update = " + ((float)(System.currentTimeMillis()-start))/1000.0/60.0);
	    
	    conn.commit();
	    
	    start = System.currentTimeMillis();	    
	    updateCompoundStatus(sqlSelectCIDTestedData,sqlUpdateCIDTestedCnt, "tested expt update");
	    logger.info("ET tested update = " + ((float)(System.currentTimeMillis()-start))/1000.0/60.0);

	    conn.commit();

	    start = System.currentTimeMillis();	    
	    updateCompoundStatus(sqlSelectCIDActiveAssayData,sqlUpdateCIDAssayActiveCnt, "active assay update");
	    logger.info("ET active Assay update = " + ((float)(System.currentTimeMillis()-start))/1000.0/60.0);

	    conn.commit();

	    start = System.currentTimeMillis();	    
	    updateCompoundStatus(sqlSelectCIDTestedAssayData,sqlUpdateCIDAssayTestedCnt, "tested assay update");
	    logger.info("ET tested Assay update = " + ((float)(System.currentTimeMillis()-start))/1000.0/60.0);	    
	    
	    conn.commit();
	    logger.info("Finished compound tested state update");

	    conn.close();
	    
	} catch (SQLException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
    }
    
    private void zeroUntestedCompounds() {
	try {

	    logger.info("Begin Zero Untested Compounds");
	    
	    //collect tested cids
	    Statement stmt = conn.createStatement();
	    stmt.setFetchSize(Integer.MIN_VALUE); //stream
	    
	    ResultSet rs = stmt.executeQuery("select cid from bard_experiment_data");
	    HashSet <Long> testedCidSet = new HashSet <Long>();
	    
	    while(rs.next()) {
		testedCidSet.add(rs.getLong(1));
	    }
	    
	    rs.close();
	    
	    logger.info("Tested cid set size = "+testedCidSet.size());
	    
	    //don't loop through all cids, just check if any with tested stats are not in the tested cid list
	    
	    //collect tested cids in compound
	    rs = stmt.executeQuery("select cid from compound where tested_expt_cnt > 0");
	    ArrayList <Long> cidsToZero = new ArrayList <Long>();
	    while(rs.next()) {
		if(!testedCidSet.contains(rs.getLong(1))) {
		    cidsToZero.add(rs.getLong(1));
		}
	    }
	   
	    rs.close();
	    stmt.close();
	    testedCidSet = null;
	   
	    logger.info("Number of CIDs to zero (previously tested, now retired):"+cidsToZero.size());

	    PreparedStatement ps = conn.prepareStatement(
		    "update compound set tested_expt_cnt = 0, active_expt_cnt = 0, " +
		    "tested_assay_cnt = 0, active_assay_cnt = 0 where cid = ?");

	    long cnt = 0;
	    for(Long cid : cidsToZero) {
		ps.setLong(1, cid);
		ps.addBatch();
		if(cnt % 100 == 0) {
		    ps.executeBatch();
		    conn.commit();
		}
	    }
	    ps.executeBatch();
	    conn.commit();
	    ps.close();
	    
	    logger.info("Finished zeroing untested compounds");
	    
	} catch (SQLException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}	
    }

    
    public void updateCompoundStatus(String selectSQL, String updateSQL, String msg) {
	try {
	    PreparedStatement ps = conn.prepareStatement(selectSQL);
	    ps.setFetchSize(Integer.MIN_VALUE);
	    
	    ResultSet rs = ps.executeQuery();
	    
	    Hashtable <Long, Long> hash = new Hashtable<Long,Long>();
	    
	    while(rs.next()) {
		hash.put(rs.getLong(1), rs.getLong(2));
	    }
	    
	    rs.close();
	    
	    ps = conn.prepareStatement(updateSQL);
	    
	    Enumeration <Long> keys = hash.keys();
	    Long cid;
	    Long cnt;
	    long updateCnt = 0;
	    while(keys.hasMoreElements()) {
		cid = keys.nextElement();
		cnt = hash.get(cid);
		ps.setLong(1,cnt);
		ps.setLong(2,cid);

		ps.addBatch();
	    
		updateCnt++;
		if(updateCnt % 1000 == 0) {
		    ps.executeBatch();
		    conn.commit();
		    logger.info(msg+" Update CNT="+updateCnt);
		}	    
	    }
	    ps.executeBatch();
	    conn.commit();
	    ps.close();
	} catch (SQLException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	    logger.warn(e.getMessage());
	}
    }    
}
