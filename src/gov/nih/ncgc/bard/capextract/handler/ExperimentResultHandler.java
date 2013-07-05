package gov.nih.ncgc.bard.capextract.handler;

import gov.nih.ncgc.bard.capextract.CAPConstants;
import gov.nih.ncgc.bard.capextract.CAPUtil;
import gov.nih.ncgc.bard.capextract.ICapResourceHandler;
import gov.nih.ncgc.bard.capextract.ResultExploder;
import gov.nih.ncgc.bard.capextract.ResultHistogram;
import gov.nih.ncgc.bard.capextract.ResultStatistics;
import gov.nih.ncgc.bard.capextract.SslHttpClient;
import gov.nih.ncgc.bard.capextract.jaxb.Contexts;
import gov.nih.ncgc.bard.capextract.jaxb.Experiment;
import gov.nih.ncgc.bard.capextract.resultextract.BardExptDataResponse;
import gov.nih.ncgc.bard.capextract.resultextract.BardResultFactory;
import gov.nih.ncgc.bard.capextract.resultextract.BardResultType;
import gov.nih.ncgc.bard.capextract.resultextract.CAPExperimentResult;
import gov.nih.ncgc.bard.resourcemgr.BardDBUtil;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Vector;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;


/**
 * This class reads in cap experiment data and formats json responses based on 
 * the BardExptResultResponse class.  The process stages data in cap_expt_result which 
 * can be advanced on to bard_experiment_data and bard_experiment_result tables.
 * 
 * @author braistedjc
 *
 */
public class ExperimentResultHandler extends CapResourceHandler implements ICapResourceHandler {

    private Logger logger = Logger.getLogger(ExperimentResultHandler.class.getName());
  
    private static int RESPONSE_CLASS_SAMPLE_SIZE = 100;
    
    private Connection conn;
    private Connection conn2;
    private ArrayList<Long> tempProjectList;
    

    /**
     * This method pulls result data from CAP using the supplied CAP experiment id and saves it in a
     * local file in bard-scratch.  The data file is read and the transformed data is loaded into cap_expt_result.
     * 
     * @param url The URL for the experiment object. 
     * @param resource the JSON Result resource
     */
    public void process(String url, CAPConstants.CapResource resource) {
	try {
	    
	    long capExptId = Long.parseLong(url.substring(url.lastIndexOf('/')+1));    
	    long start = System.currentTimeMillis();

	    logger.info("Starting load for CAP Expt ID="+capExptId+" DBserverHost="+CAPConstants.getBardDBJDBCUrl());
	    //open connection
	    conn = CAPUtil.connectToBARD(CAPConstants.getBardDBJDBCUrl());
	    conn.setAutoCommit(false);
	    conn2 = CAPUtil.connectToBARD(CAPConstants.getBardDBJDBCUrl());
	    conn2.setAutoCommit(false);

	    Statement stmt = conn.createStatement();
	    ResultSet rs;

	    //first clear the data for the experiment in the staging table
	    stmt.execute("delete from cap_expt_result where cap_expt_id="+capExptId);

	    //dump the cap data into a file in bard-scratch
	    String stageFile = CAPConstants.getBardScratchDir()+"/result_load_cap_expt_"+capExptId+".txt";
	    this.stageDataToFile(url, resource, capExptId, stageFile);

	    logger.info("Data is staged in file: "+stageFile);

	    start = System.currentTimeMillis();

	    //get related entity ids for the capExptId
	    Hashtable <String, Long> ids = getIds(capExptId);	    

	    //we need to verify that we have a corresponding bard expt id, if not log warning and get out.
	    Long bardExptId = ids.get("bardExptId");
	    if(bardExptId == null) {
		logger.warning("A bardExtId does not exist corresponding to capExptId:"+capExptId+". Experiment data load aborted. Load experiment first.");
		return;
	    }
	    
	    //get project ids for the cap experiment
	    ArrayList <ArrayList<Long>> projIds = getProjectIds(ids.get("bardExptId"));

	    //construct a result factory and initialize with entity ids common to all responses
	    BardResultFactory resultFactory = new BardResultFactory();

	    logger.info("CAP Expt ID="+capExptId+" ResultFactory Initialized, entity ID's Exist.");  

	    //prepare for insert of staged data
	    PreparedStatement insertPS = conn.prepareStatement("insert into cap_expt_result set seq_result_id = ?, cap_expt_id = ?, sid = ?, cid = ?, outcome = ?, score=?, potency=?, cap_json = ?, bard_json = ?");

	    ObjectMapper mapper = new ObjectMapper();
	    long procCnt = 0;
	    String capData;
	    Long cid;
	    Double score, potency;
	    Integer outcome;
	    BufferedReader br = new BufferedReader(new FileReader(stageFile));
	    
	    //here we need to determine the response class by polling a collection of input responses from CAP
	    //use the BufferedReader to iterate, then reset the BR for processing
	    BardExptDataResponse sampleResponse = determineResultClass(capExptId, br);
	    //now pass the experiment level response class on to the response factory when initializing
	    resultFactory.initialize(ids.get("bardExptId"), capExptId, ids.get("bardAssayId"), ids.get("capAssayId"), projIds, fetchContexts(capExptId),
		    sampleResponse.getResponseType(), sampleResponse.getExptScreeningConc(), sampleResponse.getExptConcUnit());
	    
	    
	    br = new BufferedReader(new FileReader(stageFile));
	    
	    //process each result (fore each substance). The helper class just acts as a container.
	    while((capData = br.readLine()) != null) {
		capData = capData.trim();
		if(capData.length() > 0) {  //skip empty lines

		    //build the CAP expt result object
		    CAPExperimentResult result = mapper.readValue(capData, CAPExperimentResult.class);

		    //get the cid for the sid
		    rs = stmt.executeQuery("select cid from cid_sid where rel_type = 1 and sid ="+result.getSid());
		    if(rs.next()) {
			cid = rs.getLong(1);
		    } else {
			cid = null;
		    }

		    //convert CAP result object to BardExptResultResponse class
		    //the BARDResultFactory takes care of formatting and hierarchy.
		    //teh factory already has the entity ids
		    BardExptDataResponse bardResponse = resultFactory.processCapResult(result);

		    //set cid, if the cid was null, the staged value is '0'. 
		    //we need to check for cid = 0 and set cid to NULL in the response's Long cid field.
		    if(cid != null && cid > 0)
			bardResponse.setCid(cid);
		    else
			bardResponse.setCid(null);

		    //set insert data
		    insertPS.setLong(1, procCnt);
		    insertPS.setLong(2, capExptId);
		    insertPS.setLong(3, bardResponse.getSid());
		    if(cid != null)
			insertPS.setLong(4, cid);
		    else
			insertPS.setNull(4, java.sql.Types.INTEGER);
		    
		    outcome = bardResponse.getOutcome();
		    score = bardResponse.getScore();
		    potency = bardResponse.getPotency();
		    if(outcome != null && outcome != 0)
			insertPS.setInt(5, outcome);
		    else
			insertPS.setNull(5, java.sql.Types.INTEGER);
		    if(score != null) 
			insertPS.setDouble(6, score);
		    else
			insertPS.setNull(6,java.sql.Types.DOUBLE);
		    if(potency != null)
			insertPS.setDouble(7, potency);
		    else
			insertPS.setNull(7, java.sql.Types.DOUBLE);
		    insertPS.setString(8, capData);
		    insertPS.setString(9, mapper.writeValueAsString(bardResponse));
		    
		    insertPS.addBatch();
		    //update and commit each 100
		    if(procCnt % 100 == 0) {
			insertPS.executeBatch();
			conn.commit();
		    }
		    procCnt++;
		}
	    }

	    //last batch insert and commmit
	    insertPS.executeBatch();
	    conn.commit();	
	    
	    //lets gzip the file in bard-scratch
	    gzipStagingFile(stageFile);
	    
	    //verify result staging load, count in table vs. process count
	    if(verifyStagedLoad(procCnt, capExptId)) {
			
		//delete results if they exist
		stmt.execute("delete from bard_experiment_data where bard_expt_id ="+bardExptId);
		stmt.execute("delete from bard_experiment_result where bard_expt_id ="+bardExptId);

		//load data tables (bard_experiment_data and bard_experiment_result)
		loadDataServingTables(capExptId, bardExptId);
	    }
	    
	    //now update test sid count, cid count, acitve count, and probe count, and has probe.
	    updateExperimentTestingStats(bardExptId);
	    
	    //close the connnection		
	    conn.close();
	    conn2.close();
	    
	    logger.info("Starting to explode data for BARD Experiment "+bardExptId);
	    ResultExploder re = new ResultExploder();
	    re.explodeResults(bardExptId);
	    logger.info("Finished exploding data for BARD Experiment "+bardExptId);
        ResultStatistics rstats = new ResultStatistics();
        rstats.generateStatistics(bardExptId);
        logger.info("Evaluated statistics for BARD Experiment "+bardExptId);
        ResultHistogram rh = new ResultHistogram();
        rh.generateHistogram(bardExptId);
        logger.info("Generated histograms for BARD Experiment "+bardExptId);

	    logger.info("Process time for CAP expt "+capExptId+" , BARD expt "+bardExptId+": "+(System.currentTimeMillis()-start));

	} catch (FileNotFoundException e) {
	    e.printStackTrace();
	} catch (IOException e) {
	    e.printStackTrace();
	} catch (SQLException e) {
	    e.printStackTrace();
	}	
    }

    private BardExptDataResponse determineResultClass(Long capExptId, BufferedReader bufferedReader) throws IOException {
	//set to unclassified first
	Integer resultType = new Integer(BardExptDataResponse.ResponseClass.UNCLASS.ordinal());

	String capData;
	ArrayList <BardExptDataResponse> respList = new ArrayList <BardExptDataResponse>();
	ArrayList <Integer> respSizeList = new ArrayList<Integer>();
	ArrayList <BardResultType> resultList;
	
	BardResultFactory resultFactory = new BardResultFactory();
	ObjectMapper mapper = new ObjectMapper();
	
	long ced, bed, cad, bad;
	ced = bed = cad = bad = 0l;
	ArrayList <ArrayList<Long>> pids = new ArrayList <ArrayList<Long>>();
	ArrayList <Long> pidList = new ArrayList<Long>();
	pidList.add(1l);
	pidList.add(2l);
	pids.add(pidList);
	Contexts contexts = this.fetchContexts(capExptId);
	
	CAPExperimentResult capResult;
	BardExptDataResponse response;
	int procCnt = 0;
	BardExptDataResponse maxResponse = null;
	while((capData = bufferedReader.readLine()) != null) {

	    //break if hit the sample limit
	    if(procCnt >= RESPONSE_CLASS_SAMPLE_SIZE)
		break;
	    
	    capData = capData.trim();
	    if(capData.length() > 0) {
		
		//track process count
		procCnt++;
		
		//need to initialize factory to make a new response object to pass out
		resultFactory.initialize(bed, ced, bad, cad, pids, contexts, BardExptDataResponse.ResponseClass.UNDEF.ordinal(),
			null, null);
		
		capResult = mapper.readValue(capData, CAPExperimentResult.class);
		
		//build a response **** Has a type determined
		response = resultFactory.processCapResult(capResult);
		resultList = resultFactory.getResultList();
		
		respList.add(response);
		//add the size
		if(resultList != null) {
		    respSizeList.add(resultList.size());
		} else {
		    respSizeList.add(0);
		}
	    }
	}

	if(respList.size() > 0) {
	    log.info("Sampling "+ respList.size()+ " responses to determine responseClass for capExptId"+capExptId);
	    int maxSize = 0;
	    maxResponse = respList.get(0);
	    for(int i = 0; i < respSizeList.size(); i++) {
		if(respSizeList.get(i) > maxSize) {
		    maxSize = respSizeList.get(i);
		    maxResponse = respList.get(i);
		}
	    }
	    //now finally, get the result class...
	    resultType = maxResponse.getResponseType();
	} else {
	    log.info("PROBLEM Trying to sample response class: NO responses available to determine responseClass for capExptId"+capExptId);
	}
	
	return maxResponse;
    }
    
    private void loadDataServingTables(long capExptId, long bardExptId) throws SQLException, JsonParseException, JsonMappingException, IOException {
	
	Statement stmt = conn2.createStatement();	
	long dataId;

	ResultSet rs = stmt.executeQuery("select max(expt_data_id) from bard_experiment_data");
	if(rs.next()) {
	    dataId = rs.getLong(1) + 1;
	} else {
	    log.warn("Abort result load for capExptID:"+capExptId+" Can't get max expt_data_id");
	    return;
	}

	stmt.setFetchSize(Integer.MIN_VALUE);
	ResultSet stagedDataRS = stmt.executeQuery("select sid, cid, outcome, score, potency, cap_json, bard_json from cap_expt_result where cap_expt_id="+capExptId);	

	/*
	 * bard_expt_data fields:
	 * expt_data_id (AI), bard_expt_id, eid (defunct), sid, cid, classification (defunct), updated (date),
	 * runset(varchar), outcome, score, potency, ADD efficacy and test conc?
	 * 
	 * bard_experiment_result fields:
	 * expt_result_id (AI), expt_data_id, bard_expt_id, replicate_id (defunct), eid (defunct),
	 * sid, json_data_array, json_dose_response (defunct), json_response. 
	 *  
	 */
	PreparedStatement insertDataPS = conn.prepareStatement("insert into bard_experiment_data " +
			"set bard_expt_id=?, sid=?, cid=?, updated=?, outcome=?, score=?, potency=?, expt_data_id=?");
	
	PreparedStatement insertResultPS = conn.prepareStatement("insert into bard_experiment_result " +
			"set expt_data_id=?, bard_expt_id=?, sid=?, json_data_array=?, json_response=?");
	Statement aiStmt = conn.createStatement();
	Long cid, sid;
	String bardData;
	String capData;
	BardExptDataResponse bardResponse;
	Double score, potency;
	Integer outcome;
	Date date = new Date(System.currentTimeMillis());
	long procCnt = 0;
	while(stagedDataRS.next()) {
	    procCnt++;
	    cid = stagedDataRS.getLong("cid");
	    capData = stagedDataRS.getString("cap_json");
	    bardData = stagedDataRS.getString("bard_json");
	    sid = stagedDataRS.getLong("sid");
	    
	    score = stagedDataRS.getDouble("score");
	    if(stagedDataRS.wasNull())
		score = null;
	    outcome = stagedDataRS.getInt("outcome");
	    if(stagedDataRS.wasNull())
		outcome = null;
	    potency = stagedDataRS.getDouble("potency");
	    if(stagedDataRS.wasNull())
		potency = null;

	    //bard_experiment_data fields
	    insertDataPS.setLong(1, bardExptId);
	    insertDataPS.setLong(2, sid);
	    if(cid != null)
		insertDataPS.setLong(3, cid);
	    else
		insertDataPS.setNull(3, java.sql.Types.INTEGER);
	    insertDataPS.setDate(4, date);
	    if(outcome != null)
		insertDataPS.setInt(5, outcome);
	    else
		insertDataPS.setNull(5, java.sql.Types.TINYINT);		
	    if(score != null)
		insertDataPS.setDouble(6, score);
	    else
		insertDataPS.setNull(6, java.sql.Types.DOUBLE);
	    if(potency != null)
		insertDataPS.setDouble(7, potency);
	    else
		insertDataPS.setNull(7, java.sql.Types.DOUBLE);
	    insertDataPS.setLong(8, dataId);

	    //bard_experiment_result fields
	    insertResultPS.setLong(1, dataId);
	    insertResultPS.setLong(2, bardExptId);
	    insertResultPS.setLong(3, sid);
	    insertResultPS.setString(4, capData);
	    insertResultPS.setString(5, bardData);
	    
	    insertDataPS.addBatch();
	    insertResultPS.addBatch();
	    //increment the shared data id
	    dataId++;
    
	    if(procCnt % 100 == 0) {
		insertDataPS.executeBatch();		
		insertResultPS.executeBatch();
		conn.commit();
	    }
	}
	insertDataPS.executeBatch();
	insertResultPS.executeBatch();
	conn.commit();    
    }
    
    /*
     * Verfies the count of staged data in the DB. 
     * Preconditions: Assumes an existing db connection (conn).
     */
    private boolean verifyStagedLoad(long dataCnt, long capExptId) throws SQLException {
	boolean verified = false;	
	Statement stmt = conn.createStatement();
	ResultSet rs = stmt.executeQuery("select count(*) from cap_expt_result where cap_expt_id = "+capExptId);
	if(rs.next()) {
	    if(rs.getLong(1) ==  dataCnt) {
		verified = true;
		log.info("Verified: File data count == DB Count ("+dataCnt+") for capExptId:"+capExptId);
	    } else {
		log.warn("Data Staging Verification FAILED: File data count:"+dataCnt+" != DB data count:"+rs.getLong(1)+" for capExptId:"+capExptId);
	    }
	}
	rs.close();
	return verified;
    }
    
    /*
     * Stages data into a given file name.
     */
    private long stageDataToFile(String url, CAPConstants.CapResource resource, long capExptId, String fileName) throws IOException, SQLException {

	long procCnt = 0;
	String urlStr = url+"/results";
	String mime = resource.getMimeType();
	HttpGet get = new HttpGet(urlStr);
	get.setHeader("Accept", mime);
	get.setHeader(CAPConstants.CAP_APIKEY_HEADER, CAPConstants.getApiKey());
	HttpClient client = SslHttpClient.getHttpClient();

	try {
	    HttpResponse response = client.execute(get);	   
	    BufferedReader br = new BufferedReader(new InputStreamReader(new GZIPInputStream(response.getEntity().getContent())));
	    PrintWriter bw = new PrintWriter(new FileWriter(fileName));
	    String entry;    	    
	    while((entry = br.readLine()) != null) {
		bw.println(entry);
	    }
	    br.close();
	    bw.close();
	} catch (ClientProtocolException e) {
	    e.printStackTrace();
	} catch (IOException e) {
	    e.printStackTrace();
	}
	return procCnt;
    }
    
    /*
     * Pulls entity ids related to this experiment
     */
    private Hashtable <String, Long> getIds(long capExptId) throws SQLException {
	Hashtable <String, Long> ids = new Hashtable <String, Long> ();
	
	Statement stmt = conn.createStatement();
	ResultSet rs = stmt.executeQuery("select a.bard_expt_id, b.bard_assay_id, b.cap_assay_id " +
			"from bard_experiment a, bard_assay b " +
			"where a.bard_assay_id = b. bard_assay_id and a.cap_expt_id = "+capExptId);
	if(rs.next()) {
	    Long beid = rs.getLong(1);
	    Long baid = rs.getLong(2);
	    Long caid = rs.getLong(3);
	    if(beid != null)
		ids.put("bardExptId", beid);
	    if(baid != null)
		ids.put("bardAssayId", baid);
	    if(caid != null)
		ids.put("capAssayId", caid);
	}
	rs.close();
	return ids;
    }
    
    /*
     * GZIPs the file in scratch
     */
    private void gzipStagingFile(String filePath) throws IOException {
	File file = new File(filePath);
	FileInputStream fis = new FileInputStream(file);
	File gzFile = new File(filePath+".gz");
	GZIPOutputStream gos = new GZIPOutputStream(new FileOutputStream(gzFile));	
	BufferedOutputStream bos = new BufferedOutputStream(gos);
	byte [] buffer = new byte[1024];
	int length;
	while((length = fis.read(buffer, 0, buffer.length)) != -1) {	    
	    bos.write(buffer, 0, length);
	}	
	fis.close();
	bos.flush();
	bos.close();
	file.delete();
    }
    
    /*
     * Helper method to pull project ids for the given bard experiment id
     */
    private ArrayList <ArrayList<Long>> getProjectIds(Long bardExptId) throws SQLException {
	ArrayList <ArrayList<Long>> ids = null;
	if(bardExptId != null) {
	    ids = new ArrayList <ArrayList<Long>> ();
	    Statement stmt = conn.createStatement();
	    ResultSet rs = stmt.executeQuery("select a.bard_proj_id, b.cap_proj_id from bard_project_experiment a, bard_project b where a.bard_expt_id = "+ bardExptId + " "
		    +"and a.bard_proj_id = b.bard_proj_id");
	    long capProjId;
	    ArrayList <Long> uniqueCapProjectIds = new ArrayList<Long>();
	    
	    while(rs.next()) {
		
		capProjId = rs.getLong(2);
		
		//if cap id is null (shouldn't be), just add the bard id and add the list to ids
		if(rs.wasNull()) {
		    tempProjectList = new ArrayList<Long>();		
		    tempProjectList.add(rs.getLong(1));
		    ids.add(tempProjectList);
		} else if(!uniqueCapProjectIds.contains(capProjId)) {
		    //if the cap id isn't null, then check that it hasn't been added before
		    tempProjectList = new ArrayList<Long>();
		    tempProjectList.add(rs.getLong(1));
		    tempProjectList.add(capProjId);
		    ids.add(tempProjectList);
		}
	    }
	}
	return ids;
    }
    
    
    /*
     * Helper method to pull the Contexts for th given CAP experiment ID
     */
    private Contexts fetchContexts(Long capExptId) {
	Contexts contexts = null;
	try {
	    Experiment w = getResponse(CAPConstants.CAP_ROOT+"/experiments/"+capExptId, 
		    CAPConstants.CapResource.EXPERIMENT);
	    if(w != null) {
		contexts = w.getContexts();				
	    }
	} catch (IOException e) {
	    e.printStackTrace();
	}
	return contexts;
    }
    
    
    public void updateExperimentTestingStats(Long bardExptId) throws SQLException {
	if(bardExptId != null) {
	    Statement stmt = conn.createStatement();
	    Long sampleCount = 0l;
	    Long cidCount = 0l;
	    Long activeCount = 0l; 
	    Long probeCount = 0l;
	    Boolean haveProbe = false;
	    ResultSet rs = stmt.executeQuery("select count(distinct(sid)) from bard_experiment_data where bard_expt_id="+bardExptId);
	    if(rs.next())
		sampleCount = rs.getLong(1);
	    
	    rs = stmt.executeQuery("select count(distinct(cid)) from bard_experiment_data where bard_expt_id ="+bardExptId);
	    if(rs.next())
		cidCount = rs.getLong(1);
	    
	    rs = stmt.executeQuery("select count(distinct(sid)) from bard_experiment_data where (outcome=2 or outcome=5) and bard_expt_id="+bardExptId);
	    if(rs.next())
		activeCount = rs.getLong(1);
	    
	    rs = stmt.executeQuery("select count(distinct(sid)) from bard_experiment_data where outcome=5 and bard_expt_id="+bardExptId);
	    if(rs.next())
		probeCount = rs.getLong(1);

	    haveProbe = (probeCount != null && probeCount > 0);
	    
	    stmt.executeUpdate("update bard_experiment set " +
	    		" sample_count="+sampleCount+
	    		", cid_count="+cidCount+
	    		", active_count="+activeCount+
	    		", probe_count="+probeCount+
	    		", have_probe="+haveProbe+
	    		" where bard_expt_id = "+bardExptId
	    		);
	    conn.commit();
	}
    }

    
    /*
     * Utility method to pull an example of each experiment result in the db.
     */
    public void testResultTypes(String serverURL) {
	try {
	    try {
		conn = BardDBUtil.connect(serverURL);
	    } catch (ClassNotFoundException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	    }
	    Statement stmt = conn.createStatement();
	    ResultSet rs = stmt.executeQuery("select distinct(bard_expt_id) from bard_experiment");
	    Vector <Long> v = new Vector<Long>();
	    while(rs.next()) {
		v.add(rs.getLong(1));
	    }
	    rs.close();

	    String activeSidSQL = "select sid from bard_experiment_data where (outcome = 2 or outcome = 5)" +
		    " and bard_expt_id=";
	    Vector <Long> totallyInactiveBeds = new Vector<Long>();
	    
	    String anySidSQL = "select sid from bard_experiment_data where bard_expt_id=";
	    Hashtable <Long, Long> bidSidHash = new Hashtable <Long, Long>();
	    int inactiveCnt = 0;
	    for(long bed : v) {
		rs = stmt.executeQuery(activeSidSQL+bed+" limit 1");
		if(rs.next()) {
		    bidSidHash.put(bed, rs.getLong(1));
		    System.out.println("sid ="+rs.getLong(1));
		} else {
		    rs = stmt.executeQuery(anySidSQL+bed+" limit 1");
		    if(rs.next()) {   
			bidSidHash.put(bed, rs.getLong(1));
			inactiveCnt++;
		    } else {			
			totallyInactiveBeds.add(bed);
		    }
		}
	    }
	    rs.close();
	    
	    System.out.println("No results for "+totallyInactiveBeds.size()+" experiments!");
	    System.out.println("Experiment count with no actives ="+inactiveCnt);
	    System.out.println("Number of bard expt ids="+v.size());

	    PrintWriter pw = new PrintWriter(new FileWriter("C:/Users/braistedjc/Desktop/json_response_samples_max_20130703.txt"));	    
	    
	    pw.println("RespClass\tCapExptId\tCapAssayId\tPubchemAID\tBardExptId\tsid\tisSIDActive?\tpubchem URL (Result Def)\tBARD REST URL\tBard JSON\tCAP Measure Cnt\tPubhem tid Cnt(+2)\tcapCnt/pubchemCnt");
	    
	    PreparedStatement ps = conn.prepareStatement("select b.cap_expt_id, a.json_response, a.sid, b.pubchem_aid, a.json_data_array from bard_experiment_result a, bard_experiment b " +
		    " where a.bard_expt_id=b.bard_expt_id and a.bard_expt_id = ? and a.sid = ? limit 1");	    
	    int progress = 0;
	    Blob response;
	    BufferedReader in;
	    String line;
	    long aid;
	    long sid;
	    long cid;
	    long pubchemAID;
	    Long activeSID;
	    int numWrites = 0;
	    int capCnt;
	    int pubchemTidCnt;
	    BardExptDataResponse bardResponse;
	    ObjectMapper mapper = new ObjectMapper();
	    for(Long bid : v) {
		ps.setLong(1, bid);
		activeSID = bidSidHash.get(bid);
		if(activeSID != null) {
		    ps.setLong(2, activeSID);
		    rs = ps.executeQuery();		
		    if(rs.next()) {
			progress++;
			aid = rs.getLong(1);
			response = rs.getBlob(2);
			sid = rs.getLong("sid");
			pubchemAID = rs.getLong(4);
			String responseStr = "";
			if(response != null) {
			    in = new BufferedReader(new InputStreamReader(response.getBinaryStream()));
			    numWrites++;
			    while((line = in.readLine()) != null) {				
				responseStr += line;
			    }
			    BardExptDataResponse r = mapper.readValue(responseStr, BardExptDataResponse.class);
			    pw.print(r.getResponseClass()+"\t");
			    pw.print(aid+"\t"+r.getCapAssayId()+"\t"+pubchemAID+"\t"+bid+"\t"+sid+"\t");
			    
			    if(responseStr.contains("value\":\"Active"))
				pw.print("Active\t");
			    else
				pw.print("Not Active\t");
			    
			    pw.print("=hyperlink(\"http://pubchem.ncbi.nlm.nih.gov/assay/assay.cgi?aid="+pubchemAID+"#aDefinitions\")\t");
			    pw.print("=hyperlink(\"http://bard.nih.gov/api/v17/exptdata/"+bid+"."+sid+"\")\t");
			    
			    //write bard response
			    pw.print(responseStr+"\t");

			    in.close();
			} else {
			    System.out.println("Null experiment response for expt: "+bid);
			}

			//cap data
			response = rs.getBlob(5);
			responseStr = "";
			if(response != null) {
			    in = new BufferedReader(new InputStreamReader(response.getBinaryStream()));
			    while((line = in.readLine()) != null) {
				responseStr += line;				
			    }
			} else {
			    System.out.println("Null CAP experiment response for expt: "+bid);
			}
			
			capCnt = getCAPMeasureCount(responseStr);
			
			//adding 2 for pubchem outcome and score which are measures in cap
			//so we're adding tid count + the 2 standard pubchem values
			pubchemTidCnt = this.getPubchemTIDCount(pubchemAID) + 2;
			
			pw.print(capCnt+"\t");
			pw.print(pubchemTidCnt+"\t");
			DecimalFormat format = new DecimalFormat("0.000");
			if(pubchemTidCnt > 0)
			    pw.println(format.format((float)capCnt/(float)pubchemTidCnt));
			else
			    pw.println("\t");

			//	responseStr = responseStr.replace("\n", "");
			//end the line
//	pw.println(responseStr);
			
			
			
			if(progress %100 == 0)
			    System.out.println("Progress = "+progress);
		    }
		} else {
		    System.out.println("No SID for expt = "+bid);
		}
	    }
	    System.out.println("num writes="+numWrites);
	    pw.flush();
	    pw.close();
	    conn.close();
	} catch (SQLException e) {
	    e.printStackTrace();
	} catch (IOException e) {
	    e.printStackTrace();
	}
    }
    
    public int getCAPMeasureCount(String capResponse) {
	int mCnt = 0;
	String key = "\"resultTypeId\":";
	mCnt = capResponse.split(key).length-1;
	return mCnt;
    }
    
    public int getPubchemTIDCount(long aid) {
	int tidCnt = 0;
	String url = "http://pubchem.ncbi.nlm.nih.gov/rest/pug/assay/aid/"+aid+"/description/JSON";
	
	String data = "";
	byte [] buff = new byte[1024];
	try {
	    URL pubchemURL = new URL(url);
	    InputStream is = pubchemURL.openStream();
	    BufferedReader br = new BufferedReader(new InputStreamReader(is));
	    String line;
	    while((line = br.readLine()) != null) {
		data += line.trim();
	    }
	    is.close();
	    tidCnt = data.split("\"tid\":").length-1;
	    if(aid == 624024) {
		//System.out.println(data);
		System.out.println("tid cnt ="+tidCnt);
	    }
	} catch (MalformedURLException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	} catch (IOException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}	
	return tidCnt;
    }
    
    public void updateExperimentTestStats(String dbURL) {
	try {
	    conn = CAPUtil.connectToBARD(dbURL);
	    Statement stmt = conn.createStatement();
	    ResultSet rs = stmt.executeQuery("select bard_expt_id from bard_experiment");
	    ArrayList <Long> beds = new ArrayList <Long>();
	    while(rs.next()) {
		beds.add(rs.getLong(1));		
	    }
	    rs.close();
	    stmt.close();
	    
	    for(Long bed : beds) {
		this.updateExperimentTestingStats(bed);
	    }
	    
	    conn.close();
	    
	} catch (SQLException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
	
    }
    
    public static void main(String [] args) {
	ExperimentResultHandler worker = new ExperimentResultHandler();
	long start = System.currentTimeMillis();
	//worker.getPubchemTIDCount(624024);
	worker.testResultTypes("jdbc:mysql://maxwell.ncats.nih.gov/bard3");
	
	//worker.updateExperimentTestStats("jdbc:mysql://maxwell.ncats.nih.gov/bard3");
	
	//worker.processCapExperimentResultViaFileCache(36, "jdbc:mysql://protein.nhgri.nih.gov/bard3", "/ifs/prod/bard/entity_mgr/bard-scratch/");	
	System.out.println("et="+((System.currentTimeMillis()-start)));		
    }
    
}
