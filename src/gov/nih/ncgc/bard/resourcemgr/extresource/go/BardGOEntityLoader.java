package gov.nih.ncgc.bard.resourcemgr.extresource.go;

import gov.nih.ncgc.bard.resourcemgr.BardDBUtil;
import gov.nih.ncgc.bard.resourcemgr.BardExtResourceLoader;
import gov.nih.ncgc.bard.resourcemgr.IBardExtResourceLoader;
import gov.nih.ncgc.bard.resourcemgr.extresource.ontology.go.GONode;
import gov.nih.ncgc.bard.resourcemgr.extresource.ontology.go.GOQueryWorker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Logger;

public class BardGOEntityLoader extends BardExtResourceLoader implements IBardExtResourceLoader {

    static final private Logger logger = 
	    Logger.getLogger(BardGOEntityLoader.class.getName());

    private Connection conn;
    private String sqlSelectAccession = "select bard_assay_id, aid, accession from assay_target where accession is not null";
    private String sqlSelectAssayTargetFromBiology = "select entity_id, ext_id from bard_biology where biology_dict_id = 1398 and entity='assay'";
    private String sqlSelectGOAssayTargetFromBiology = "select entity_id, ext_id from bard_biology where biology_dict_id = 1419 and entity='assay'";

    private String sqlInsertAssayGO = "insert into temp_go_assay (bard_assay_id, target_acc, current_acc, go_id, go_term, go_type, ev_code, implied, " +
	    "go_assoc_db_ref, assoc_date)" +
	    " values (?,?,?,?,?,?,?,?,?,?)";  //

    private String sqlUpdateAssayGoDBRefAndDate = 
	    "update temp_go_assay a join go_association b on a.target_acc=b.accession and a.go_id=b.term_acc and a.ev_code=b.evidence " +
		    "set a.go_assoc_db_ref=b.db_ref, a.assoc_date=b.assoc_date where a.implied = 0";

    private String sqlUpdateProjectGoDBRefAndDate = 
	    "update temp_go_project a join go_association b on a.target_acc=b.accession and a.go_id=b.term_acc and a.ev_code=b.evidence " +
		    "set a.go_assoc_db_ref=b.db_ref, a.assoc_date=b.assoc_date where a.implied = 0";

    private String sqlUpdateCompoundGoDBRefAndDate = 
	    "update temp_go_compound a join go_association b on a.target_acc=b.accession and a.go_id=b.term_acc and a.ev_code=b.evidence " +
		    "set a.go_assoc_db_ref=b.db_ref, a.assoc_date=b.assoc_date where a.implied = 0";

    private String sqlSelectProjectTargets = "select bard_proj_id, accession from project_target where accession is not null order by bard_proj_id asc";

    private String sqlSelectProjectTargetFromBiology = "select entity_id, ext_id from bard_biology where biology_dict_id = 1398 and entity='project'";
    private String sqlSelectGOProjectTargetFromBiology = "select entity_id, ext_id from bard_biology where biology_dict_id = 1419 and entity='project'";

    private String sqlInsertProjectGO = "insert into temp_go_project (bard_proj_id, target_acc, current_acc, go_id, go_term, go_type, ev_code, implied, " +
	    "go_assoc_db_ref, assoc_date)" +
	    " values (?,?,?,?,?,?,?,?,?,?)";

    private String sqlInsertCompoundGO = "insert into temp_go_compound (cid, target_acc, go_id, go_term, go_type, ev_code, implied)" +
	    " values (?,?,?,?,?,?,?)";

    PreparedStatement queryAccessionPS, insertGOPS;

    private String sqlSelectCompoundTarget = "select cid, val from compound_annot where annot_key ='TARGETS'";

    private long insertCnt;
    private long accessionCnt;

    @Override
    public boolean load() {
	boolean loaded = false;
	log.info("In load() in BardGOEntityLoader. Reading service key.");

	try {
	    conn = BardDBUtil.connect(service.getDbURL());
	    conn.setAutoCommit(false);

	    if(service.getServiceKey().contains("GO-ENTITY-REFRESH")) {
		log.info("Starting GO Entity Refresh, first go_assay, then go_project.");
		//refresh go_assay and go_project
		loadGOAssay();
		//load additional direct go from bard_biology
		log.info("Starting GO Load from Biology");
		loadGoAssayFromBiology();
		log.info("Finished GO_ASSAY.  Starting on GO_PROJECT.");
		//refresh go project
		loadGOProject();
		log.info("Finished GO_PROJECT Load from Protein targets.");
		log.info("Starting GO PROJECT Load from Biology");
		loadGoProjectFromBiology();
		
		BardDBUtil.swapTempTableToProductionIfPassesSizeDelta("temp_go_assay", "go_assay", 0.90, service.getDbURL());			
		BardDBUtil.swapTempTableToProductionIfPassesSizeDelta("temp_go_project", "go_project", 0.90, service.getDbURL());			
		loaded = true;
	    }

	    conn.close();
	} catch (ClassNotFoundException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	} catch (SQLException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
	return loaded;
    }


    public void loadGOAssay() {

	try {
	    //conn = BardDBUtil.connect(service.getDbURL());
	    //conn.setAutoCommit(false);
	    log.info("Assay load connection established");

	    //set up the tables
	    Statement stmt = conn.createStatement();
	    stmt.execute("create table if not exists temp_go_assay like go_assay");
	    stmt.execute("truncate table temp_go_assay");
	    stmt.close();

	    log.info("Initialized temp_go_assay");

	    insertCnt = 0;

	    GOQueryWorker worker = new GOQueryWorker();
	    worker.prepareStatements(service.getDbURL());

	    queryAccessionPS = conn.prepareStatement(sqlSelectAssayTargetFromBiology);
	    //queryAccessionPS.setFetchSize(Integer.MIN_VALUE);

	    insertGOPS = conn.prepareStatement(this.sqlInsertAssayGO);

	    ResultSet rs = queryAccessionPS.executeQuery();


	    HashSet <GONode> set = new HashSet <GONode> ();
	    long bardAssayID;
	    long assayID;
	    String accession;

	    logger.info("accession result set");

	    Vector <Long> bardAssayIdV = new Vector<Long>();
	    Vector <Long> aids = new Vector<Long>();
	    Vector <String> accV = new Vector<String>();
	    //rather than collecting all go-nodes for all accessions, process each accession before building new.

	    //			Hashtable <String, HashSet <Long>> accToAssayHash = new Hashtable <String, HashSet <Long>>();
	    Hashtable <String, HashSet <Long>> accToBardIDHash = new Hashtable <String, HashSet <Long>>();
	    //capture the current uniprot accession
	    Hashtable <String, String> accToCurrAccHash = new Hashtable<String,String>();
	    String currAcc;

	    while(rs.next()) {
		bardAssayID = rs.getLong(1);
		accession = rs.getString(2).trim();

		//only go out to get current if we don't have it
		if(accToCurrAccHash.get(accession) == null) {
		    currAcc = getCurrentAccession(accession);
		    if(currAcc != null) {
			accToCurrAccHash.put(accession, currAcc);
			if(!accession.equals(currAcc)) {
			    log.info("Accession "+accession+" is not current. Updated accesion = "+currAcc);
			}
		    } else {
			log.warning("Couldn't retrieve current uniprot for acc="+accession+" bardAssayId="+bardAssayID);
			accToCurrAccHash.put(accession, accession);
		    }
		}

		if(accToBardIDHash.get(accession) == null) {					
		    HashSet <Long> bardAssayV = new HashSet<Long>();
		    bardAssayV.add(bardAssayID);
		    accToBardIDHash.put(accession,  bardAssayV);
		} else {
		    accToBardIDHash.get(accession).add(bardAssayID);
		}

		bardAssayIdV.add(bardAssayID);
		accV.add(accession);
	    }

	    rs.close();

	    log.info("Collected Targets: Accession count="+accV.size()+" Assay Count="+bardAssayIdV.size());

	    int aidAccCnt = 0;

	    //maybe collect all nodes into a hash or two that is keyed by go_id and go_acc
	    //we can pull nodes from the hash as needed to support queries. 
	    //we won't need to build and destroy nodes, just build references to the nodes

	    //prepare the worker
	    worker.populateNodeHashes();

	    Set <String> accKeys = accToBardIDHash.keySet();

	    for(String accKey: accKeys) {
		set.clear();
		currAcc = accToCurrAccHash.get(accKey);

		log.info("process accession="+accKey);

		//reset implied to false
		worker.setAllNodeImplied(false);

		//reset direct 

		//get accessions nodes
		set.addAll(worker.getGONodesForAccessionUsingHash(currAcc));
		//set.addAll(worker.getGONodesForAccession(accKey));
		
		HashSet <GONode> newSet = new HashSet <GONode>();

		//get accession's node's ancestors
		for(GONode node: set) {
		    //go up the hierarchy to get implied for this accession
		    Vector <GONode> nodes = worker.getPredNodesFromHash(node);		

		    for(GONode n: nodes) {
			//don't overwrite if it exists (primary), if doesn't exist, it's implied
			if(!set.contains(n)) {
			    n.setImplied(true);
			    n.setEvCode("GO_ANCESTOR_TERM");
			    newSet.add(n);
			}
		    }
		}		

		newSet.addAll(set);

		int index = 0;
		Iterator <Long> bardIdEnum = accToBardIDHash.get(accKey).iterator();

		for(long bad:accToBardIDHash.get(accKey)) {
		    bardAssayID = bardIdEnum.next();
		    insertGOData(bardAssayID, bad, accKey, accToCurrAccHash.get(accKey), newSet);
		    aidAccCnt++;
		    index++;
		}
	    }

	    insertGOPS.executeBatch();
	    conn.commit();

	    log.info("Finished Temp Load");

	    //set details of assocation
	    log.info("update association date and db ref in temp tables");
	    stmt = conn.createStatement();
	    stmt.executeUpdate(sqlUpdateAssayGoDBRefAndDate);

	    //swap tables
	    //BardDBUtil.swapTempTableToProductionIfPassesSizeDelta("temp_go_assay", "go_assay", 0.90, service.getDbURL());			

	    //conn.close();
	    log.info("Done Load");

	} catch (SQLException sqle) {
	    sqle.printStackTrace();
	}
    }
    
    private void loadGoAssayFromBiology() {
	try {	    
	    log.info("Loading GO ASSAY from Biology GO");
	    //conn = BardDBUtil.connect(service.getDbURL());
	    //conn.setAutoCommit(false);
	    Statement stmt = conn.createStatement();
	    ResultSet rs = stmt.executeQuery(sqlSelectGOAssayTargetFromBiology);
	    Hashtable <String, Vector<Long>> goToBardExptIdHash = new Hashtable<String, Vector<Long>>();
	    long bardExptId;
	    String goId;
	    Vector <Long> bardExptIdV = new Vector<Long>();
	    int goLength = 0;
	    while(rs.next()) {
		bardExptId = rs.getLong(1);
		goId = rs.getString(2);
	
		if(!goId.startsWith("GO:")) {
		    goLength = goId.length();
		    for(int i = 0; i < 7-goLength;i++)
			goId = "0"+goId;
		    goId = "GO:"+goId;
		}
		
		bardExptIdV = goToBardExptIdHash.get(goId);
		if(bardExptIdV == null) {
		    bardExptIdV = new Vector<Long>();
		    bardExptIdV.add(bardExptId);
		    goToBardExptIdHash.put(goId, bardExptIdV);
		} else {
		    bardExptIdV.add(bardExptId);
		}
	    }
	    stmt.close();
	    log.info("Have collected GO_IDs for all bard_expt_id in bard_biology.");
	    insertGOPS = conn.prepareStatement(this.sqlInsertAssayGO);
	    HashSet <GONode> primaryGoNodes = new HashSet<GONode>();
	    Set <String> goIdSet = goToBardExptIdHash.keySet();

	    GOQueryWorker worker = new GOQueryWorker();
	    worker.prepareStatements(service.getDbURL());
	    worker.populateNodeHashes();
	    GONode node = new GONode();
	    HashSet <GONode> impliedV = new HashSet<GONode>();
	    for(String directGoId : goIdSet) {
		worker.setAllNodeImplied(true);
		log.info("Processing direct GO for go_id="+directGoId);
		node = worker.getNodeForGoAcc(directGoId);	
		if(node == null) {
		    log.warning("GO BIOLOGY for assays update FAILED for CAP GO_ID (not a valid id)="+directGoId);
		    continue;
		}
		node.setImplied(false);
		
		impliedV.addAll(worker.getPredNodesFromHash(node));
		
		for(GONode n : impliedV) {
		    n.setEvCode("CAP_ANCESTOR_TERM");
		}
		node.setEvCode("CAP_DIRECT_TERM");
		impliedV.add(node);		
		bardExptIdV = goToBardExptIdHash.get(node.getGoAccession());
		for(long beid : bardExptIdV) {
		    insertGOData(beid, 0, "", "", impliedV);
		}
		insertGOPS.executeBatch();
		conn.commit();
		//swap tables
		//BardDBUtil.swapTempTableToProductionIfPassesSizeDelta("temp_go_assay", "go_assay", 0.90, service.getDbURL());			
		//conn.close();
		log.info("Done Load");
	    }	    
	} catch (SQLException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
    }

    public String getCurrentAccession(String acc) {
	String currAcc = null;
	String uniprotURLStr = "http://www.uniprot.org/uniprot/"+acc+".txt";
	boolean stopSearch = false;
	try {
	    URL uniprotURL = new URL(uniprotURLStr);
	    InputStream is = (InputStream)uniprotURL.getContent();
	    BufferedReader br = new BufferedReader(new InputStreamReader(is));
	    String line;

	    while((line = br.readLine()) != null && !stopSearch) {
		if(line.startsWith("AC")) {
		    line = line.substring(3);
		    currAcc = line.split(";")[0].trim();
		    stopSearch = true;
		}
	    }
	    br.close();
	    is.close();
	} catch (MalformedURLException e) {
	    log.warning("Malformed uniprot url:"+uniprotURLStr);
	    e.printStackTrace();
	    return null;
	} catch (IOException e) {
	    log.warning("IOException during retrieval of current accession from:"+uniprotURLStr);
	    e.printStackTrace();
	    return null;
	} 

	return currAcc;
    }


    public void loadGOProject() {

	try {

	    //conn = BardDBUtil.connect(service.getDbURL());
	   // conn.setAutoCommit(false);
	    log.info("Project load connection established");

	    //set up the tables
	    Statement stmt = conn.createStatement();
	    stmt.execute("create table if not exists temp_go_project like go_project");
	    stmt.execute("truncate table temp_go_project");
	    stmt.close();

	    log.info("Initialized temp_go_project");

	    insertCnt = 0;

	    GOQueryWorker worker = new GOQueryWorker();
	    worker.prepareStatements(service.getDbURL());

	    queryAccessionPS = conn.prepareStatement(sqlSelectProjectTargetFromBiology);

	    insertGOPS = conn.prepareStatement(sqlInsertProjectGO);

	    ResultSet rs = queryAccessionPS.executeQuery();

	    HashSet <GONode> set = new HashSet <GONode> ();
	    long projectID;
	    String accession;

	    logger.info("accession result set");

	    Vector <String> accV = new Vector<String>();
	    //rather than collecting all go-nodes for all accessions, process each accession before building new.

	    Hashtable <String, HashSet <Long>> accToProjectHash = new Hashtable <String, HashSet <Long>>();

	    //capture the current uniprot accession
	    Hashtable <String, String> accToCurrAccHash = new Hashtable<String,String>();
	    String currAcc;

	    while(rs.next()) {
		projectID = rs.getLong(1);
		accession = rs.getString(2).trim();

		//only go out to get current if we don't have it
		if(accToCurrAccHash.get(accession) == null) {
		    currAcc = getCurrentAccession(accession);
		    if(currAcc != null) {
			accToCurrAccHash.put(accession, currAcc);
			if(!accession.equals(currAcc)) {
			    log.info("Accession "+accession+" is not current. Updated accesion = "+currAcc);
			}
		    } else {
		    	log.warning("Couldn't retrieve current uniprot for acc="+accession+" bardProgId="+projectID);
			accToCurrAccHash.put(accession, accession);
		    }
		}

		if(accToProjectHash.get(accession) == null) {
		    HashSet <Long> v = new HashSet<Long>();
		    v.add(projectID);
		    accToProjectHash.put(accession, v);
		} else {
		    accToProjectHash.get(accession).add(projectID);
		}
		accV.add(accession);
	    }

	    rs.close();

	    int aidAccCnt = 0;

	    log.info("Collected Targets: Accession count="+accV.size()+" Assay Count="+accToProjectHash.size());

	    //maybe collect all nodes into a hash or two that is keyed by go_id and go_acc
	    //we can pull nodes from the hash as needed to support queries. 
	    //we won't need to build and destroy nodes, just build references to the nodes

	    //prepare the worker
	    worker.populateNodeHashes();


	    Set <String> accKeys = accToProjectHash.keySet();


	    for(String accKey: accKeys) {
		set.clear();
		currAcc = accToCurrAccHash.get(accKey);

		log.info("process accession="+accKey);

		//reset implied to false
		worker.setAllNodeImplied(false);

		//get accessions nodes
		set.addAll(worker.getGONodesForAccessionUsingHash(currAcc));
		//set.addAll(worker.getGONodesForAccession(accKey));

		HashSet <GONode> newSet = new HashSet <GONode>();

		//get accession's node's ancestors
		for(GONode node: set) {
		    //go up the hierarchy to get implied for this accession
		    Vector <GONode> nodes = worker.getPredNodesFromHash(node);		

		    for(GONode n: nodes) {
			//don't overwrite if it exists (primary), if doesn't exist, it's implied
			if(!set.contains(n)) {
			    n.setImplied(true);
			    n.setEvCode("GO_ANCESTOR_TERM");
			    newSet.add(n);
			}
		    }
		}		

		newSet.addAll(set);

		for(long projID:accToProjectHash.get(accKey)) {
		    insertGODataForProject(projID, accKey, accToCurrAccHash.get(accKey), newSet);
		    aidAccCnt++;
		}

		//logger.info("in gc()");
		//System.gc();


	    }

	    insertGOPS.executeBatch();
	    conn.commit();


	    log.info("Finished Temp Load");

	    //set details of assocation
	    log.info("update association date and db ref in temp tables");

	    stmt = conn.createStatement();
	    stmt.executeUpdate(sqlUpdateProjectGoDBRefAndDate);

	    //swap tables
	    //BardDBUtil.swapTempTableToProductionIfPassesSizeDelta("temp_go_project", "go_project", 0.90, service.getDbURL());

	    //conn.close();
	    logger.info("Done Load");

	} catch (SQLException sqle) {
	    sqle.printStackTrace();
	}
    }
    
    
    private void loadGoProjectFromBiology() {
	try {	    
	    log.info("Loading GO PROJECT from Biology GO");
//	    conn = BardDBUtil.connect(service.getDbURL());
//	    conn.setAutoCommit(false);
	    Statement stmt = conn.createStatement();
	    ResultSet rs = stmt.executeQuery(sqlSelectGOProjectTargetFromBiology);
	    Hashtable <String, Vector<Long>> goToBardExptIdHash = new Hashtable<String, Vector<Long>>();
	    long bardExptId;
	    String goId;
	    Vector <Long> bardExptIdV = new Vector<Long>();
	    int goLength = 0;
	    while(rs.next()) {
		bardExptId = rs.getLong(1);
		goId = rs.getString(2);
	
		if(!goId.startsWith("GO:")) {
		    goLength = goId.length();
		    for(int i = 0; i < 7-goLength;i++)
			goId = "0"+goId;
		    goId = "GO:"+goId;
		}
		
		bardExptIdV = goToBardExptIdHash.get(goId);
		if(bardExptIdV == null) {
		    bardExptIdV = new Vector<Long>();
		    bardExptIdV.add(bardExptId);
		    goToBardExptIdHash.put(goId, bardExptIdV);
		} else {
		    bardExptIdV.add(bardExptId);
		}
	    }
	    stmt.close();
	    log.info("Have collected GO_IDs for all bard_expt_id in bard_biology (go_project update).");
	    insertGOPS = conn.prepareStatement(this.sqlInsertProjectGO);
	    HashSet <GONode> primaryGoNodes = new HashSet<GONode>();
	    Set <String> goIdSet = goToBardExptIdHash.keySet();

	    GOQueryWorker worker = new GOQueryWorker();
	    worker.prepareStatements(service.getDbURL());
	    worker.populateNodeHashes();
	    GONode node = new GONode();
	    HashSet <GONode> impliedV = new HashSet<GONode>();
	    for(String directGoId : goIdSet) {
		worker.setAllNodeImplied(true);
		log.info("Processing direct GO for go_id="+directGoId);
		node = worker.getNodeForGoAcc(directGoId);	
		if(node == null) {
		    log.warning("GO BIOLOGY for projects update FAILED for CAP GO_ID (not a valid id)="+directGoId);
		    continue;
		}
		node.setImplied(false);
		
		impliedV.addAll(worker.getPredNodesFromHash(node));
		
		for(GONode n : impliedV) {
		    n.setEvCode("CAP_ANCESTOR_TERM");
		}
		node.setEvCode("CAP_DIRECT_TERM");
		impliedV.add(node);		
		bardExptIdV = goToBardExptIdHash.get(node.getGoAccession());
		for(long beid : bardExptIdV) {
		    insertGODataForProject(beid, "", "", impliedV);
		}
		insertGOPS.executeBatch();
		conn.commit();
		//swap tables
//		BardDBUtil.swapTempTableToProductionIfPassesSizeDelta("temp_go_project", "go_project", 0.90, service.getDbURL());			
//		conn.close();
		log.info("Done Load");
	    }	    
	} catch (SQLException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
    }
    
    

    public void loadGOCompound() {

	try {

	    conn = BardDBUtil.connect(service.getDbURL());
	    conn.setAutoCommit(false);

	    insertCnt = 0;

	    GOQueryWorker worker = new GOQueryWorker();
	    worker.prepareStatements(service.getDbURL());

	    queryAccessionPS = conn.prepareStatement(sqlSelectCompoundTarget);

	    insertGOPS = conn.prepareStatement(sqlInsertCompoundGO);

	    ResultSet rs = queryAccessionPS.executeQuery();


	    HashSet <GONode> set = new HashSet <GONode> ();
	    long cid;
	    String accession;

	    logger.info("accession result set");

	    Vector <Long> cids = new Vector<Long>();
	    Vector <String> accV = new Vector<String>();
	    //rather than collecting all go-nodes for all accessions, process each accession before building new.

	    Hashtable <String, HashSet <Long>> accToAssayHash = new Hashtable <String, HashSet <Long>>();
	    String [] toks;
	    while(rs.next()) {
		cid = rs.getLong(1);
		toks = rs.getString(2).split("\\|");
		if(toks.length > 3) {
		    accession = toks[3].trim();
		    logger.info("Have Accession"+accession);
		} else
		    continue;

		logger.info("cid capture, cid="+cid);

		if(accToAssayHash.get(accession) == null) {
		    HashSet <Long> v = new HashSet<Long>();
		    v.add(cid);
		    accToAssayHash.put(accession, v);
		} else {
		    accToAssayHash.get(accession).add(cid);
		}

		cids.add(cid);
		accV.add(accession);
	    }

	    rs.close();

	    int aidAccCnt = 0;

	    //maybe collect all nodes into a hash or two that is keyed by go_id and go_acc
	    //we can pull nodes from the hash as needed to support queries. 
	    //we won't need to build and destroy nodes, just build references to the nodes

	    //prepare the worker
	    worker.populateNodeHashes();


	    Set <String> accKeys = accToAssayHash.keySet();


	    for(String accKey: accKeys) {
		set.clear();

		logger.info("process accession="+accKey);

		//reset implied to false
		worker.setAllNodeImplied(false);

		//get accessions nodes
		set.addAll(worker.getGONodesForAccessionUsingHash(accKey));
		//set.addAll(worker.getGONodesForAccession(accKey));

		HashSet <GONode> newSet = new HashSet <GONode>();

		//get accession's node's ancestors
		for(GONode node: set) {
		    //go up the hierarchy to get implied for this accession
		    Vector <GONode> nodes = worker.getPredNodesFromHash(node);		

		    for(GONode n: nodes) {
			//don't overwrite if it exists (primary), if doesn't exist, it's implied
			if(!set.contains(n)) {
			    n.setImplied(true);
			    n.setEvCode("GO_ANCESTOR_TERM");
			    newSet.add(n);
			}
		    }
		}		

		newSet.addAll(set);

		for(long aid:accToAssayHash.get(accKey)) {
		    insertGODataForCompound(aid, accKey, newSet);
		    aidAccCnt++;
		}
	    }

	    //			
	    //			for(long aid:aids) {
	    //				accession = accV.get(aidAccCnt);
	    //				set.clear();
	    //				
	    //				//get accessions nodes
	    //				set.addAll(worker.getGONodesForAccession(accession));
	    //
	    //				HashSet <GONode> newSet = new HashSet <GONode>();
	    //						
	    //				//get accession's node's ancestors
	    //				for(GONode node: set) {
	    //					//go up the hierarchy to get implied for this accession
	    //					Vector <GONode> nodes = worker.getPredNodes(node);		
	    //
	    //					for(GONode n: nodes) {
	    //						//don't overwrite if it exists (primary), if doesn't exist, it's implied
	    //						if(!set.contains(n)) {
	    //							n.setImplied(true);
	    //							n.setEvCode("GO_ANCESTOR_TERM");
	    //							newSet.add(n);
	    //						}
	    //					}
	    //				}
	    //			
	    //				
	    //				newSet.addAll(set);
	    //				
	    //		//		logger.info("handling aid/accession="+aid+" "+accession);
	    //				//process just the inserts for this one accession
	    //				insertGOData(aid, accession, newSet);
	    //				aidAccCnt++;
	    //			}

	    insertGOPS.executeBatch();
	    conn.commit();

	    //set details of assocation
	    logger.info("update compound assoc dbref and date");
	    Statement stmt = conn.createStatement();
	    stmt.executeUpdate(sqlUpdateCompoundGoDBRefAndDate);


	    conn.close();
	    logger.info("Done Load");

	} catch (SQLException sqle) {
	    sqle.printStackTrace();
	} catch (ClassNotFoundException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
    }



    private void insertGOData(long bardAssayID, long assayID, String accession, String currAcc, Set <GONode> nodeSet) throws SQLException {
	String ontologyType;
	//		logger.info(assayID+" "+accession+" set size="+nodeSet.size());
	for(GONode node : nodeSet) {
	    insertCnt++;
	    this.insertGOPS.setLong(1, bardAssayID);
	    //this.insertGOPS.setLong(2, assayID);
	    this.insertGOPS.setString(2, accession);
	    this.insertGOPS.setString(3, currAcc);
	    this.insertGOPS.setString(4, node.getGoAccession());
	    this.insertGOPS.setString(5, node.getGoName());
	    ontologyType = node.getGoOntologyType();
	    if(ontologyType.equals("biological_process"))
		ontologyType = "P";
	    if(ontologyType.equals("molecular_function"))
		ontologyType = "F";
	    if(ontologyType.equals("cellular_component"))
		ontologyType = "C";			
	    this.insertGOPS.setString(6, ontologyType);
	    this.insertGOPS.setString(7, node.getEvCode());
	    this.insertGOPS.setInt(8, node.isImplied() ? 1 : 0);	
	    this.insertGOPS.setNull(9, java.sql.Types.VARCHAR);
	    this.insertGOPS.setNull(10, java.sql.Types.VARCHAR);

	    this.insertGOPS.addBatch();

	    if(insertCnt % 10 == 0) {
		insertGOPS.executeBatch();
		insertGOPS.clearBatch();
		conn.commit();
		logger.info("Insert Count = "+insertCnt);
	    }			
	}
    }


    private void insertGODataForProject(long projectID, String accession, String currAcc, Set <GONode> nodeSet) throws SQLException {
	String ontologyType;
	//		logger.info(assayID+" "+accession+" set size="+nodeSet.size());
	for(GONode node : nodeSet) {
	    insertCnt++;
	    this.insertGOPS.setLong(1, projectID);
	    this.insertGOPS.setString(2, accession);
	    this.insertGOPS.setString(3, currAcc);
	    this.insertGOPS.setString(4, node.getGoAccession());
	    this.insertGOPS.setString(5, node.getGoName());
	    ontologyType = node.getGoOntologyType();
	    if(ontologyType.equals("biological_process"))
		ontologyType = "P";
	    if(ontologyType.equals("molecular_function"))
		ontologyType = "F";
	    if(ontologyType.equals("cellular_component"))
		ontologyType = "C";			
	    this.insertGOPS.setString(6, ontologyType);
	    this.insertGOPS.setString(7, node.getEvCode());
	    this.insertGOPS.setInt(8, node.isImplied() ? 1 : 0);	
	    this.insertGOPS.setNull(9, java.sql.Types.VARCHAR);
	    this.insertGOPS.setNull(10, java.sql.Types.VARCHAR);

	    this.insertGOPS.addBatch();

	    if(insertCnt % 10 == 0) {
		insertGOPS.executeBatch();
		insertGOPS.clearBatch();
		conn.commit();
		logger.info("Insert Count = "+insertCnt);
	    }			
	}
    }

    private void insertGODataForCompound(long cid, String accession, Set <GONode> nodeSet) throws SQLException {
	String ontologyType;
	//		logger.info(assayID+" "+accession+" set size="+nodeSet.size());
	for(GONode node : nodeSet) {
	    insertCnt++;
	    this.insertGOPS.setLong(1, cid);
	    this.insertGOPS.setString(2, accession);
	    this.insertGOPS.setString(3, node.getGoAccession());
	    this.insertGOPS.setString(4, node.getGoName());
	    ontologyType = node.getGoOntologyType();
	    if(ontologyType.equals("biological_process"))
		ontologyType = "P";
	    if(ontologyType.equals("molecular_function"))
		ontologyType = "F";
	    if(ontologyType.equals("cellular_component"))
		ontologyType = "C";			
	    this.insertGOPS.setString(5, ontologyType);
	    this.insertGOPS.setString(6, node.getEvCode());
	    this.insertGOPS.setInt(7, node.isImplied() ? 1 : 0);	

	    this.insertGOPS.addBatch();

	    if(insertCnt % 10 == 0) {
		insertGOPS.executeBatch();
		insertGOPS.clearBatch();
		conn.commit();
		logger.info("Insert Count = "+insertCnt);
	    }			
	}
    }


    public static void main(String [] args) {
	BardGOEntityLoader loader = new BardGOEntityLoader();
	//loader.loadGOCompound();
	//loader.loadGO();
	System.out.println("**"+loader.getCurrentAccession("D3Z2V4")+"**");
    }

    @Override
    public String getLoadStatusReport() {
	// TODO Auto-generated method stub
	return null;
    }

}
