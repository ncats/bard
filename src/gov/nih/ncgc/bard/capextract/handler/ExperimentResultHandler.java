package gov.nih.ncgc.bard.capextract.handler;

import gov.nih.ncgc.bard.capextract.CAPConstants;
import gov.nih.ncgc.bard.capextract.CAPUtil;
import gov.nih.ncgc.bard.capextract.ICapResourceHandler;
import gov.nih.ncgc.bard.capextract.SslHttpClient;
import gov.nih.ncgc.bard.capextract.jaxb.Contexts;
import gov.nih.ncgc.bard.capextract.jaxb.Experiment;
import gov.nih.ncgc.bard.capextract.resultextract.BardExptDataResponse;
import gov.nih.ncgc.bard.capextract.resultextract.BardResultFactory;
import gov.nih.ncgc.bard.capextract.resultextract.CAPExperimentResult;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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
  
    private Connection conn;
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

	    Statement stmt = conn.createStatement();
	    ResultSet cidRS;

	    //first clear the data for the experiment in the staging table
	    stmt.execute("delete from cap_expt_result where cap_expt_id="+capExptId);

	    //dump the cap data into a file in bard-scratch
	    String stageFile = CAPConstants.getBardScratchDir()+"/result_load_"+capExptId+".txt";
	    this.stageDataToFile(url, resource, capExptId, stageFile);

	    logger.info("et for FILE staging data="+(System.currentTimeMillis()-start));

	    start = System.currentTimeMillis();

	    //get related entity ids for the capExptId
	    Hashtable <String, Long> ids = getIds(capExptId);	    

	    //we need to verify that we have a corresponding bard expt id, if not log warning and get out.
	    if(ids.get("bardExptId") == null) {
		logger.warning("A bardExtId does not exist corresponding to capExptId:"+capExptId+". Experiment data load aborted. Load experiment first.");
		return;
	    }
	    
	    //get project ids for the cap experiment
	    ArrayList <ArrayList<Long>> projIds = getProjectIds(ids.get("bardExptId"));

	    //construct a result factory and initialize with entity ids common to all responses
	    BardResultFactory resultFactory = new BardResultFactory();
	    resultFactory.initialize(ids.get("bardExptId"), capExptId, ids.get("bardAssayId"), ids.get("capAssayId"), projIds, fetchContexts(capExptId));

	    logger.info("CAP Expt ID="+capExptId+" ResultFactory Initialized, entity ID's Exist.");  

	    //prepare for insert of staged data
	    PreparedStatement insertPS = conn.prepareStatement("insert into cap_expt_result set seq_result_id = ?, cap_expt_id = ?, cid = ?, cap_json = ?, bard_json = ?");

	    ObjectMapper mapper = new ObjectMapper();
	    long procCnt = 0;
	    String capData;
	    Long cid;
	    BufferedReader br = new BufferedReader(new FileReader(stageFile));

	    //process each result (fore each substance). The helper class just acts as a container.
	    while((capData = br.readLine()) != null) {
		capData = capData.trim();
		if(capData.length() > 0) {  //skip empty lines

		    //build the CAP expt result object
		    CAPExperimentResult result = mapper.readValue(capData, CAPExperimentResult.class);

		    //get the cid for the sid
		    cidRS = stmt.executeQuery("select cid from cid_sid where sid ="+result.getSid());
		    if(cidRS.next()) {
			cid = cidRS.getLong(1);
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
		    if(cid != null)
			insertPS.setLong(3, cid);
		    else
			insertPS.setNull(3, java.sql.Types.INTEGER);
		    insertPS.setString(4, capData);
		    insertPS.setString(5, mapper.writeValueAsString(bardResponse));
		    
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
	    
	    //verify result load, count in table vs. process count
	    
	    //check for the experiment results in the data tables
	    
	    //delete results if they exist
	    
	    //load data tables (bard_experiment_data and bard_experiment_result)
	    
	    //close the connnection
	    conn.close();

	    logger.info("Process time for expt "+capExptId+": "+(System.currentTimeMillis()-start));

	} catch (FileNotFoundException e) {
	    e.printStackTrace();
	} catch (IOException e) {
	    e.printStackTrace();
	} catch (SQLException e) {
	    e.printStackTrace();
	}	
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
	    while(rs.next()) {
		tempProjectList = new ArrayList<Long>();		
		tempProjectList.add(rs.getLong(1));
		capProjId = rs.getLong(2);
		if(!rs.wasNull())
		    tempProjectList.add(capProjId);
		ids.add(tempProjectList);
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
    
    
    /*
     * Utility method to pull an example of each experiment result in the db.
     */
    public void testResultTypes(String serverURL) {
	try {
	    conn = CAPUtil.connectToBARD(serverURL);
	    Statement stmt = conn.createStatement();
	    ResultSet rs = stmt.executeQuery("select distinct(bard_expt_id) from bard_experiment");
	    Vector <Long> v = new Vector<Long>();
	    while(rs.next()) {
		v.add(rs.getLong(1));
	    }
	    rs.close();
	    
	    PrintWriter pw = new PrintWriter(new FileWriter("C:/Users/braistedjc/Desktop/json_response_samples.txt"));	    
	    PreparedStatement ps = conn.prepareStatement("select eid, json_response from bard_experiment_result " +
	    		" where bard_expt_id = ? limit 1");	    
	    int progress = 0;
	    Blob response;
	    BufferedReader in;
	    String line;
	    long aid;
	    for(Long bid : v) {
		ps.setLong(1, bid);
		rs = ps.executeQuery();		
		if(rs.next()) {
		    progress++;
		    aid = rs.getLong(1);
		    response = rs.getBlob(2);
		    if(response != null) {
			in = new BufferedReader(new InputStreamReader(response.getBinaryStream()));
			while((line = in.readLine()) != null) {
			    pw.println(bid+"\t"+aid+"\t"+line);			    
			}
		    }
		    if(progress %100 == 0)
			System.out.println("Progress = "+progress);
		}
	    }	    
	    pw.flush();
	    pw.close();
	    conn.close();
	} catch (SQLException e) {
	    e.printStackTrace();
	} catch (IOException e) {
	    e.printStackTrace();
	}
    }
    
    
//    public static void main(String [] args) {
//	ExperimentResultHandler worker = new ExperimentResultHandler();
//	long start = System.currentTimeMillis();
//	//worker.testResultTypes();	
//	//worker.processCapExperimentResultViaFileCache(36, "jdbc:mysql://protein.nhgri.nih.gov/bard3", "/ifs/prod/bard/entity_mgr/bard-scratch/");	
//	System.out.println("et="+((System.currentTimeMillis()-start)));		
//    }
    
}
