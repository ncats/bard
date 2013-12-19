package gov.nih.ncgc.bard.resourcemgr.precomp;

import gov.nih.ncgc.bard.capextract.CAPUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import org.eclipse.jetty.util.log.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import chemaxon.formats.MolFormatException;
import chemaxon.struc.Molecule;
import chemaxon.util.MolHandler;

public class CompoundSimilarityWorker {  
    
    private static Logger logger = LoggerFactory.getLogger(CompoundSimilarityWorker.class);
   
    static double tanimoto (int[] fp1, int[] fp2) {
	int a = 0, b = 0, c = 0;
	for (int i = 0; i < fp1.length; ++i) {
	    c += Integer.bitCount(fp1[i] & fp2[i]);
	    a += Integer.bitCount(fp1[i]);
	    b += Integer.bitCount(fp2[i]);
	}
	return (double)c/(a+b-c);
    }
    
    
    static double tanimoto (long[] fp1, long[] fp2) {
	int a = 0, b = 0, c = 0;
	for (int i = 0; i < fp1.length; ++i) {
	    c += Long.bitCount(fp1[i] & fp2[i]);
	    a += Long.bitCount(fp1[i]);
	    b += Long.bitCount(fp2[i]);
	}
	return (double)c/(a+b-c);
    }
    
    /**
     * Refreshes all druglike and druglike cids for ALL compounds in the compound table.
     * 
     * @param dbURL
     * @throws SQLException
     * @throws MolFormatException
     */
    public void refreshDruglikeInCompound(String dbURL) throws SQLException, MolFormatException {
	
	logger.info("Starting update of druglike in compound in:"+dbURL);
	Connection conn = CAPUtil.connectToBARD(dbURL);
	int maxRings = 10;
	
	Statement stmt = conn.createStatement();
	ResultSet rs = stmt.executeQuery("select b.iso_smiles, b.cid from compound_annot a, compound b where a.val like '%human approved drug%' and a.cid = b.cid");
	ArrayList <long []> drugFps = new ArrayList <long []>();
	ArrayList <Long> drugCIDs = new ArrayList <Long>();
	MolHandler mh;
	int [] fpArr;
	long [] fpArrUnsigned;
	while(rs.next()) {
	    drugCIDs.add(rs.getLong(2));
	    mh = new MolHandler(rs.getString(1));
	    mh.aromatize(); //aromatize BEFORE fingerprinting
	    fpArr = mh.generateFingerprintInInts(16,2,6);

	    fpArrUnsigned = new long[fpArr.length];
	    for(int i = 0; i < fpArr.length; i++) {
		fpArrUnsigned[i] = convertToUnsignedInt(fpArr[i]);
	    }
	    drugFps.add(fpArrUnsigned);
//	    drugFps.add(fpArr);
	    mh = null;
	}
	logger.info("Druglike: Have fp for all approved drugs. Drug cid cnt: "+drugFps.size());
	rs.close();
	rs = stmt.executeQuery("select count(cid) from compound");
	long totalCmpCnt = 0l;
	if(rs.next()) {
	    totalCmpCnt = rs.getLong(1);
	}
	rs.close();
	long nullDruglikeCnt = 0l;
	rs = stmt.executeQuery("select count(cid) from compound");
	if(rs.next()) {
	    nullDruglikeCnt = rs.getLong(1);
	}
	logger.info("Druglike: compound count: "+totalCmpCnt);
	logger.info("Druglike: compound count to find sim to drugs: "+nullDruglikeCnt);
	rs.close();
	
	int batchSize = 100000;
	int batchCnt = ((int)(nullDruglikeCnt/batchSize)) + 1;
	long currPos = 0l;
	
	logger.info("Druglike: Batch size 100000, number of batches:"+batchCnt);
	
	String sqlSelectBase = "select cid, iso_smiles from compound ";
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
	boolean testbreak = false;
	PreparedStatement updatePS = conn.prepareStatement("update compound set druglike = ?, druglike_cid = ? where cid = ?");

	for(int batch = 0; batch < batchCnt; batch++) {

	    if(testbreak) {
		break;
	    }
	    
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
		    fpArrUnsigned = new long[fp.length];
		    for(int i = 0; i < fp.length; i++) {
			fpArrUnsigned[i] = convertToUnsignedInt(fp[i]);
		    }
			
		    mh = null;

		    //find max sim
		    maxTaniSim = 0;
		    drugCnt = 0;
		    closestDrugCID = 0;

		    for(long [] drugFp : drugFps) {
			taniSim = CompoundSimilarityWorker.tanimoto(drugFp, fpArrUnsigned);
//			taniSim = CompoundSimilarityWorker.tanimoto(drugFp, fp);
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
		
		if(batch == 0 && i < 10) {
		    logger.info("unsigned test cid ="+cidMaxArray[i][0]+" tanimoto="+cidMaxArray[i][1]+" close_cid="+cidMaxArray[i][2]);
		}
		
		
		if(cnt % 10000 == 0) {
		    updatePS.executeBatch();
		    conn.commit();
		}
		
		if(totCnt % 1000000 == 0) {
		    logger.info("Druglike: total update cnt: "+totCnt);
		    
		    //test break
		    if(totCnt == 2000000) {
			updatePS.executeBatch();
			conn.commit();
			testbreak = true;
			break;
		    }
		}	
	    }
	    
	    updatePS.executeBatch();
	    conn.commit();
	    
	    //update current position
	    currPos += (batchSize + 1);
	}
	
	logger.info("Finished refresh of druglike in compound table in:"+dbURL);
    }
    
    public static long convertToUnsignedInt(int input) {  
	return input & 0xFFFFFFFFL;  
    } 
    
    public static void main(String [] args) {
	CompoundSimilarityWorker worker = new CompoundSimilarityWorker();
	String dbURL = "jdbc:mysql://bohr.fast.ncats.nih.gov:3306/bard3?zeroDateTimeBehavior=convertToNull";
//	try {
//	    worker.refreshDruglikeInCompound(dbURL);
//	} catch (MolFormatException e) {
//	    e.printStackTrace();
//	} catch (SQLException e) {
//	    e.printStackTrace();
//	}
	
	MolHandler mh;
	try {
	    mh = new MolHandler("CC1CN(C(C2=C1OC3=C2C=C(C=C3)N)C)C.C(=CC(=O)O)C(=O)O");
	    
	    mh.aromatize(); //aromatize BEFORE fingerprinting
	    int [] fp1 = mh.generateFingerprintInInts(16,2,6);
	
	    mh = new MolHandler("CN(C)CC1=CC2=C(O1)CC(NC2)CC3=CC=CC=C3");
	    mh.aromatize(); //aromatize BEFORE fingerprinting
	    int [] fp2 = mh.generateFingerprintInInts(16,2,6);	    
	    
	    System.out.println(CompoundSimilarityWorker.tanimoto(fp2, fp1));
	    
	} catch (MolFormatException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
	
	
    }
}
