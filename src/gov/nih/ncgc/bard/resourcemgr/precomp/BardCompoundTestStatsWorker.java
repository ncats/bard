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
	    
	    //logger.info("Building temp compound table.");
	    //make a copy of the current compound table
	    //Statement stmt = conn.createStatement();
	    //stmt.execute("drop table if exists temp_compound");
	    //stmt.execute("create table temp_compound like compound");
	    //conn.commit();
	    
	    //Why make a temp compound? The counts are not incremental
	    //Most efficient way is to zero out counts and do the updates off-line.
	    //
	    //Why zero? If an experiment is retired, the tested counts should drop.
	    //We don't want to deal with this in real time, removing counts for all dropped cids.
	    //Nightly rebuild based on data tables.
	    //
	    //We need to avoid long selects. 
	    //Insert into temp_compound gradually instead of insert... select
	
	    //stmt.execute("insert into temp_compound select * from compound");
	    //replace the insert... select... to do an staged update to temp_compound 
	    
	    //stageCopyToTable(conn, "compound", "temp_compound", 1000000);
	    //conn.commit();
	    
	    //logger.info("Completed temp_compound table creation. Starting zero of all stats.");
	    
	    //clear the current counters
	    //stmt.executeUpdate("update temp_compound set tested_expt_cnt = 0, active_expt_cnt = 0, " +
	    //		"tested_assay_cnt = 0, active_assay_cnt = 0");	    

	    //logger.info("Completed temp_compound zeroing of all stats. Starting stat updates.");

	    //conn.commit();

	    //now update all of these statuses
	    
	    //zero untested compounds in the compound table
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

	    //conn.setAutoCommit(true);
	    
//	    //zero the nulls, null means it's a new compound, 0 means that it's been assessed and is zero
//	    start = System.currentTimeMillis();	    
//	    logger.info("Setting active expt nulls to zero");
//	    zeroNullTestCnts(this.sqlSetActiveExptNullToZero);
//	    logger.info("ET zero active expt = " + ((float)(System.currentTimeMillis()-start))/1000.0/60.0);	    
//	    
//	    start = System.currentTimeMillis();	    
//	    logger.info("Setting tested expt nulls to zero");
//	    zeroNullTestCnts(this.sqlSetTestedExptNullToZero);
//	    logger.info("ET zero tested expt = " + ((float)(System.currentTimeMillis()-start))/1000.0/60.0);	    
//
//	    start = System.currentTimeMillis();	    
//	    logger.info("Setting active assay nulls to zero");
//	    zeroNullTestCnts(this.sqlSetActiveAssayNullToZero);
//	    logger.info("ET zero active assays = " + ((float)(System.currentTimeMillis()-start))/1000.0/60.0);	    
//	    
//	    start = System.currentTimeMillis();	    
//	    logger.info("Setting active assay nulls to zero");
//	    zeroNullTestCnts(this.sqlSetTestedAssayNullToZero);
//	    logger.info("ET zero active assays = " + ((float)(System.currentTimeMillis()-start))/1000.0/60.0);	    

	    conn.close();

	    
	    
	    //boolean swapped = BardDBUtil.swapTempTableToProductionIfPassesSizeDelta("temp_compound", "compound", 1.0, serverURL);
	    
	    //logger.info("Swapped temp_compound into compound, confirmation: "+swapped);
	    
	} catch (SQLException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
    }
    
    
    private void stageCopyToTable(Connection conn2, String sourceTable,
	    String destTable, int rowLimit) throws SQLException {

	logger.info("Begin Staging Data in "+destTable);
	long skip = 0;	
	Statement stmt = conn2.createStatement();
	
	stmt.execute("drop table if exists "+ destTable);
	stmt.execute("create table "+destTable+" like "+sourceTable);
	conn2.commit();    
	
	ResultSet rs = stmt.executeQuery("select count(*) from compound");
	long cidCount = 0l;
	
	if(rs.next()) {
	    cidCount = rs.getLong(1);
	}
	rs.close();
	
	long numberOfBatches = (cidCount/rowLimit);
	if(cidCount % rowLimit != 0)
	    numberOfBatches += 1;

	PreparedStatement ps = 
		conn2.prepareStatement("replace into "+destTable+" select * from "+ sourceTable +
			" limit ?,"+rowLimit);
	long cnt = 0;

	for(int batch = 0; batch < numberOfBatches; batch++) {
	    ps.setLong(1, cnt);
	    ps.execute();
	    conn2.commit();
	    cnt += rowLimit;
	    //interrupt selects on compound briefly
	    try {
		Thread.sleep(15);
	    } catch (InterruptedException e) {
		e.printStackTrace();
	    }
	}

	conn2.commit();
	logger.info("Finished staging data to "+destTable);
    }


    public void zeroUntestedCompounds() {
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
    
    
    public void updateDruglikeInTempCompound(String dbURL) throws SQLException, MolFormatException {
	
	logger.info("Starting update druglike in temp compound in:"+dbURL);
	Connection conn = CAPUtil.connectToBARD(dbURL);
	int maxRings = 10;
	
	Statement stmt = conn.createStatement();
	ResultSet rs = stmt.executeQuery("select b.iso_smiles, b.cid from compound_annot a, temp_compound b where a.val like '%human approved drug%' and a.cid = b.cid");
	ArrayList <int []> drugFps = new ArrayList <int []>();
	ArrayList <Long> drugCIDs = new ArrayList <Long>();
	MolHandler mh;
	int [] fpArr;
	long [] fpArrUnsigned;
	while(rs.next()) {
	    drugCIDs.add(rs.getLong(2));
	    mh = new MolHandler(rs.getString(1));
	    mh.aromatize(); //aromatize BEFORE fingerprinting
	    fpArr = mh.generateFingerprintInInts(16,2,6);
//	    fpArrUnsigned = new long[fpArr.length];
//	    for(int i = 0; i < fpArr.length; i++) {
//		fpArrUnsigned[i] = convertToUnsignedInt(fpArr[i]);
//	    }
//	    drugFps.add(fpArrUnsigned);
	    drugFps.add(fpArr);
	    mh = null;
	}
	logger.info("Druglike: Have fp for all approved drugs. Drug cid cnt: "+drugFps.size());
	rs.close();
	rs = stmt.executeQuery("select count(cid) from temp_compound");
	long totalCmpCnt = 0l;
	if(rs.next()) {
	    totalCmpCnt = rs.getLong(1);
	}
	rs.close();
	long nullDruglikeCnt = 0l;
	rs = stmt.executeQuery("select count(cid) from temp_compound where druglike is null");
	if(rs.next()) {
	    nullDruglikeCnt = rs.getLong(1);
	}
	logger.info("Druglike: compound count: "+totalCmpCnt);
	logger.info("Druglike: compound count to find sim to drugs: "+nullDruglikeCnt);
	rs.close();
	int batchCnt = ((int)(nullDruglikeCnt/100000)) + 1;
	long currPos = 0l;
	int batchSize = 100000;
	
	logger.info("Druglike: Batch size 100000, number of batches:"+batchCnt);
	
	String sqlSelectBase = "select cid, iso_smiles from temp_compound where druglike is null ";
	String sqlSelect = "";
	double maxTaniSim;
	double taniSim;
	double [][] cidMaxArray;
	int cnt = 0;
	int [] fp;
	Molecule mol;

	int rings;
	long totCnt = 0;
	long closestCid = 0;
	int drugCnt = 0;
	double closestDrugCID;
	PreparedStatement updatePS = conn.prepareStatement("update temp_compound set druglike = ?, druglike_cid = ? where cid = ?");
	for(int batch = 0; batch < batchCnt; batch++) {

	    logger.info("Druglike: Starting batch: "+(batch + 1));
	    //get a batch of cids and smiles
	    sqlSelect = sqlSelectBase + "limit " + currPos + ", " + batchSize;
	    rs = stmt.executeQuery(sqlSelect);
	    //for each cid, get the min tanimoto, break if tanimoto is 1.000
	    cidMaxArray = new double [batchSize][3];
	    cnt = 0;
	    
	    while(rs.next()) {
		
		//get the cid for the current compound
		cidMaxArray[cnt][0] = rs.getDouble(1);

		closestCid = (long)cidMaxArray[cnt][0];
		
		//generate the fp
		mh = new MolHandler(rs.getString(2));
		mh.aromatize(); //aromatize BEFORE fingerprinting
		mol = mh.getMolecule();
		rings = mol.getEdgeCount() - mol.getAtomCount() + 1;
		//skip highly ringed structures
		    
		if(rings < maxRings) {
		    fp = mh.generateFingerprintInInts(16,2,6);
//		    fpArrUnsigned = new long[fp.length];
//		    for(int i = 0; i < fp.length; i++) {
//			fpArrUnsigned[i] = convertToUnsignedInt(fp[i]);
//		    }
			
		    mh = null;

		    //find max sim
		    maxTaniSim = 0;
		    drugCnt = 0;
		    closestDrugCID = 0;

		    for(int [] drugFp : drugFps) {
//			taniSim = CompoundSimilarityWorker.tanimoto(drugFp, fpArrUnsigned);
			taniSim = CompoundSimilarityWorker.tanimoto(drugFp, fp);
			if(taniSim > maxTaniSim) {
			    maxTaniSim = taniSim;
			    closestDrugCID = (double)(drugCIDs.get((int)drugCnt));
			}
			if(maxTaniSim == 1.0) {
			    closestDrugCID = (double)(drugCIDs.get((int)drugCnt));			 
			    break;
			}
			drugCnt++;
		    }
		} else {
		    //highly ringed, expensive fp, set low sim, 0 cid.
		    maxTaniSim = 0.0;
		    closestDrugCID = 0;
		}
		cidMaxArray[cnt][1] = maxTaniSim;
		cidMaxArray[cnt][2] = closestDrugCID;
		cnt++;
	    }
	    
	    //close the result set
	    rs.close();
	    
	    //we have similarities for the batch
	    cnt = 0;
	    for(int i = 0; i < cidMaxArray.length ; i++) {
		cnt++;
		totCnt++;
		
		// update druglike max tanimoto
		updatePS.setDouble(1, cidMaxArray[i][1]);
		updatePS.setLong(2, (long)cidMaxArray[i][2]);
		// add cid to indicate record to update
		updatePS.setLong(3, (long)cidMaxArray[i][0]);
		
		updatePS.addBatch();
		if(cnt % 10000 == 0) {
		    updatePS.executeBatch();
		    conn.commit();
		}
		
		if(totCnt % 1000000 == 0) {
		    logger.info("Druglike: total update cnt: "+totCnt);
		}	
	    }
	    
	    updatePS.executeBatch();
	    conn.commit();
	    
	    //update current position
	    currPos += (batchSize + 1);
	}
    }
    
    public static long convertToUnsignedInt(int input) {  
	return input & 0xFFFFFFFFL;  
    } 
        
    private void zeroNullTestCnts(String sql) {
	try {
	    Statement stmt = conn.createStatement();
	    stmt.execute(sql);
	} catch (SQLException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
    }
    
    public static void main(String [] args) {
	BardCompoundTestStatsWorker worker = new BardCompoundTestStatsWorker();
	String serverURL = "jdbc:mysql://bohr.ncats.nih.gov:3306/bard3";
	
//	try {
	    //Connection conn = CAPUtil.connectToBARD(serverURL);
	    worker.updateCompoundTestStatus(serverURL);
	    //worker.stageCopyToTable(conn, "compound", "temp_compound", 1000000);
	    //conn.close();
//	} catch (SQLException e) {
//	    // TODO Auto-generated catch block
//	    e.printStackTrace();
//	}
	
	    //worker.updateDruglikeInTempCompound("jdbc:mysql://bohr.ncats.nih.gov:3306/bard3");
	
    }
    
}
