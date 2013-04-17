package gov.nih.ncgc.bard.resourcemgr.extresource.pubchem;

import gov.nih.ncgc.bard.capextract.resultextract.BardExptDataResponse;
import gov.nih.ncgc.bard.resourcemgr.BardDBUtil;
import gov.nih.ncgc.bard.resourcemgr.BardExtResourceLoader;
import gov.nih.ncgc.bard.resourcemgr.BardExternalResource;
import gov.nih.ncgc.bard.resourcemgr.IBardExtResourceLoader;

import java.io.IOException;
import java.sql.Blob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Set;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class CIDSIDMappingLoader extends BardExtResourceLoader implements
IBardExtResourceLoader {

    private long mapSize = 0;
    private long deltaMapSize = 0;
    
    @Override
    public boolean load() {
	boolean loaded = false;;
	if(service != null) {
	    log.info("Loading CID SID Mapping");
	    //fetch file
	    fetchExternalResource();
	    //rebuild CID_SID
	    deltaMapSize = rebuildCIDSIDViaTempLoad();
	    //set response message
	    statusText = "Completed CID SID Mapping Refresh. Mapping entries have increased by "+deltaMapSize;	   
	    //backfill missing cids in data
	    long updates = backfillNullCIDsInData();
	    statusText += " ("+updates+" data CIDs backfilled)";    
	    loaded = true;
	} else {
	    return false;
	}
	return loaded;
    }

    @Override
    public String getLoadStatusReport() {
	// TODO Auto-generated method stub
	return null;
    }

    @Override
    public void setLoaderProps(Properties loaderProps) {
	// TODO Auto-generated method stub

    }


    private long rebuildCIDSIDViaTempLoad() {

	long deltaMapCnt = 0;
	long initCnt = 0;
	long newMapCnt = 0;
	
	try {

	    initCnt = BardDBUtil.getTableRowCount("cid_sid", service.getDbURL());

	    conn = BardDBUtil.connect(service.getDbURL());
	    
	    BardExternalResource resource = service.getExtResources().get(0);
	    //unzip the file
	    String cidsidGZIPPath = service.getLocalResPath()+"/"+resource.getFileName();
	    String cidsidPath = cidsidGZIPPath.replace(".gz", "");
	    gunZip(cidsidGZIPPath, cidsidPath);

	    //make the temp_cid_sid and truncate
	    Statement stmt = conn.createStatement();
	    stmt.execute("create table if not exists temp_cid_sid like cid_sid");
	    stmt.execute("truncate table temp_cid_sid");

	    //load the temp table
	    stmt.execute("load data infile '"+cidsidPath+"' into table temp_cid_sid (cid, sid, rel_type)");

	    newMapCnt = BardDBUtil.getTableRowCount("temp_cid_sid", service.getDbURL());

	    deltaMapCnt = newMapCnt - initCnt;

	    //swap tables iff larger or nearly the same size. Allow from some contraction due to deleted substances. 
	    BardDBUtil.swapTempTableToProductionIfPassesSizeDelta("temp_cid_sid", "cid_sid", 0.95, service.getDbURL());

	    conn.close();

	} catch (SQLException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	} catch (ClassNotFoundException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}

	return deltaMapCnt;
    }
    
    private long backfillNullCIDsInData() {
	long updates = 0;
	try {
	    conn = BardDBUtil.connect(service.getDbURL());
	    conn.setAutoCommit(false);
	    
	    Statement stmt = conn.createStatement();
	    stmt.setFetchSize(Integer.MIN_VALUE);
	    ResultSet rs = stmt.executeQuery("select expt_data_id, sid from bard_experiment_data " +
	    		"where cid=0");
	    Hashtable <Long, ArrayList<Long>> sidToDataIdListTable = new Hashtable <Long, ArrayList<Long>>();
	    ArrayList<Long> idList;
	    while(rs.next()) {
		idList = sidToDataIdListTable.get(rs.getLong(2));
		if(idList == null) {
		    idList = new ArrayList<Long>();
		    idList.add(rs.getLong(1));
		    sidToDataIdListTable.put(rs.getLong(2), idList);
		} else {
		    idList.add(rs.getLong(1));
		}
	    }
	    rs.close();
	    stmt.close();
	    
	    //now query to find the sids that have cids.
	    Set <Long> sids = sidToDataIdListTable.keySet();
	    ArrayList <Long> smartSIDs = new ArrayList<Long>();
	    Hashtable <Long, Long> sidCidHash = new Hashtable<Long,Long>();
	    PreparedStatement ps = conn.prepareStatement("select cid from cid_sid where sid = ? and rel_type=1");
	    long cid;

	    for(Long sid : sids) {
		ps.setLong(1, sid);
		rs = ps.executeQuery();
		if(rs.next()) {
		    cid = rs.getLong(1);
		    if(cid != 0) {
			sidCidHash.put(sid,cid);
		    }
		}
	    }
	    
	    ps.close();
	    
	    //sids to work on, they are missing cids in data and now have cids	    
	    sids = sidCidHash.keySet();
	    
	    PreparedStatement psDataUpdate = conn.prepareStatement("update bard_experiment_data set cid = ? where expt_data_id=?");
	    PreparedStatement psGetJson = conn.prepareStatement("select json_response from bard_experiment_result where expt_data_id=?");
	    PreparedStatement psResultUpdate = conn.prepareStatement("update bard_experiment_result set json_response=? where expt_data_id=?");
	    ObjectMapper mapper = new ObjectMapper();
	    BardExptDataResponse response;
	    Blob blob;
	    boolean ready = false;
	    byte [] buffer;
	    for(Long sid : sids) {
		idList = sidToDataIdListTable.get(sid);
		for(Long dataId : idList) {
		    ready = false;
		    psDataUpdate.setLong(2, dataId);
		    psDataUpdate.setLong(1, sidCidHash.get(sid));
		    psDataUpdate.executeUpdate();      
		    
		    //now the fun part, updating the json
		    psGetJson.setLong(1, dataId);
		    rs = psGetJson.executeQuery();

		    if(rs.next()) {
			blob = rs.getBlob(1);
			buffer = blob.getBytes(1, (int) blob.length());
			String s = new String(buffer);
			try {
			    response = mapper.readValue(buffer, BardExptDataResponse.class);
			    if(response.getCid() == null || response.getCid() == 0) {
				response.setCid(sidCidHash.get(sid));
				psResultUpdate.setLong(2, dataId);
				psResultUpdate.setString(1, mapper.writeValueAsString(response));
				psResultUpdate.executeUpdate();
				ready = true;
			    }			
			} catch (JsonParseException e) {
			    e.printStackTrace();
			    continue;
			} catch (JsonMappingException e) {
			    e.printStackTrace();
			    continue;
			} catch (IOException e) {
			    e.printStackTrace();
			    continue;
			}
			
			if(ready) {
			    conn.commit();
			    updates++;
			} else {
			    conn.rollback();
			}
			
		    } else {
			ready = false;
			log.warning("Failed to backfill expt_data_id="+dataId+" no result found to modify.");
		    }
		}	
	    }
	    
	} catch (ClassNotFoundException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	} catch (SQLException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
	return updates;
    }
    

}
