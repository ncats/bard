package gov.nih.ncgc.bard.capextract;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

/**
 * @author Rajarshi Guha
 */
public class ResultExploder {
    protected Logger log;

    public ResultExploder() {
        log = LoggerFactory.getLogger(this.getClass());
    }

    public void explodeResults(Long bardExptId) throws SQLException, IOException {
        String tmpFileName = "exploded" + UUID.randomUUID() + ".csv";

        Connection conn = CAPUtil.connectToBARD(CAPConstants.getBardDBJDBCUrl());
        ObjectMapper mapper = new ObjectMapper();
        BufferedWriter writer = new BufferedWriter(new FileWriter(tmpFileName));

        // to be safe we delete exploded results for this experiment id before we
        // do the explosion
        PreparedStatement pst = conn.prepareStatement("delete from exploded_results where bard_expt_id = ?");
        pst.setLong(1, bardExptId);
        pst.executeUpdate();
        pst.close();
        log.info("Deleted exploded results for BARD experiment " + bardExptId + " if already present");
        Statement pstReader = conn.createStatement(java.sql.ResultSet.TYPE_FORWARD_ONLY, java.sql.ResultSet.CONCUR_READ_ONLY);
        pstReader.setFetchSize(Integer.MIN_VALUE);
        ResultSet reader = pstReader.executeQuery("select bard_expt_id, expt_data_id, expt_result_id, json_response from bard_experiment_result where bard_expt_id = " + bardExptId);
        int nresult = 0;
        int nexplode = 0;
        while (reader.next()) {
            nresult++;
            Long exptDataId = reader.getLong(2);
            Long exptResultId = reader.getLong(3);
            String json = reader.getString(4);
            JsonNode node = mapper.readTree(json);
            
            //JB: add priority elements first
            JsonNode priorityElems = node.get("priorityElements");
            if (priorityElems instanceof ArrayNode) {
                ArrayNode anode = (ArrayNode) priorityElems;
                for (int i = 0; i < anode.size(); i++) {
                    String displayName = anode.get(i).get("displayName").textValue();
                    Double value = null;
                    JsonNode valueNode = anode.get(i).get("value");
                    if (valueNode != null && CAPUtil.isNumber(valueNode.textValue()))
                        value = Double.parseDouble(valueNode.textValue());
                    if (value != null) {
                        nexplode++;
                        writer.write(bardExptId + "," + exptDataId + "," + exptResultId + "," + displayName + "," + value + "\n");
                    }
                }
            }
            
            JsonNode rootElems = node.get("rootElements");
            if (rootElems instanceof ArrayNode) {
                ArrayNode anode = (ArrayNode) rootElems;
                for (int i = 0; i < anode.size(); i++) {
                    String displayName = anode.get(i).get("displayName").textValue();
                    Double value = null;
                    JsonNode valueNode = anode.get(i).get("value");
                    if (valueNode != null && CAPUtil.isNumber(valueNode.textValue()))
                        value = Double.parseDouble(valueNode.textValue());
                    if (value != null) {
                        nexplode++;
                        writer.write(bardExptId + "," + exptDataId + "," + exptResultId + "," + displayName + "," + value + "\n");
                    }
                }
            }
        }
        writer.close();
        reader.close();
        pstReader.close();
        log.info("Exploded " + nresult + " results to " + nexplode + " rows");

        // now read in our exploded rows and load in
        BufferedReader rows = new BufferedReader(new FileReader(tmpFileName));
        PreparedStatement pstWriter = conn.prepareStatement("insert into exploded_results (bard_expt_id, expt_data_id, expt_result_id, display_name, value) " +
                " values (?,?,?,?,?)");
        String line;
        int n = 0;
        while ((line = rows.readLine()) != null) {
            String[] toks = line.trim().split(",");
            pstWriter.setLong(1, Long.parseLong(toks[0]));
            pstWriter.setLong(2, Long.parseLong(toks[1]));
            pstWriter.setLong(3, Long.parseLong(toks[2]));
            pstWriter.setString(4, toks[3]);
            pstWriter.setDouble(5, Double.parseDouble(toks[4]));
            pstWriter.addBatch();

            n++;
            if (n % 1000 == 0) {
                pstWriter.executeBatch();
                log.info("\rWrote " + n + " exploded rows");
            }
        }
        pstWriter.executeBatch();
        conn.commit();
        pstWriter.close();
        conn.close();
        log.info("Loaded " + n + " exploded results into database");

        (new File(tmpFileName)).delete();
    }
    
    public void utilityLoadCurrentResultsIntoTempExplodedResults() {
	try {
	    log.info("Starting Utility Load of Current Results to Temp_Exploded_Results");
	    Connection conn = CAPUtil.connectToBARD(CAPConstants.getBardDBJDBCUrl());
	    Statement stmt = conn.createStatement();
	    
	    //specialty run, just start at end of data truncation error after bard_expt_id = 195
//	    stmt.execute("create table if not exists temp_exploded_results like exploded_results");
//	    stmt.execute("truncate table temp_exploded_results");
	    
	    //special run after 195:
	    stmt.execute("delete from temp_exploded_results where bard_expt_id = 196");
	    
	    ArrayList <Long> bardExptIdList = new ArrayList<Long>();

	    //***************************************
	    //special run starting after bard expt 195
	    ResultSet rs = stmt.executeQuery("select distinct(bard_expt_id) from bard_experiment_result where bard_expt_id > 195");
	    while(rs.next()) {
		bardExptIdList.add(rs.getLong(1));
	    }
	    rs.close();
	    stmt.close();
	    conn.close();
	    log.info("Starting load, (just > bard id 195) Experiment count="+bardExptIdList.size());
	    //load each experiment
	    int exptCnt = 0;
	    for(Long bardExptId : bardExptIdList) {
		//reconnects to DB for each bard experiment but probably OK.
		this.explodeResultsIntoTemp(bardExptId);
		exptCnt++;
		log.info("Finished Load of bardExptId="+bardExptId+" total load cnt="+exptCnt);
	    }
	} catch (SQLException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	} catch (IOException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	};
    }
    
    private void explodeResultsIntoTemp(Long bardExptId) throws SQLException, IOException {
        String tmpFileName = "exploded" + UUID.randomUUID() + ".csv";

        Connection conn = CAPUtil.connectToBARD(CAPConstants.getBardDBJDBCUrl());
        ObjectMapper mapper = new ObjectMapper();
        BufferedWriter writer = new BufferedWriter(new FileWriter(tmpFileName));

        Statement pstReader = conn.createStatement(java.sql.ResultSet.TYPE_FORWARD_ONLY, java.sql.ResultSet.CONCUR_READ_ONLY);
        pstReader.setFetchSize(Integer.MIN_VALUE);
        ResultSet reader = pstReader.executeQuery("select bard_expt_id, expt_data_id, expt_result_id, json_response from bard_experiment_result where bard_expt_id = " + bardExptId);
        int nresult = 0;
        int nexplode = 0;
        while (reader.next()) {
            nresult++;
            Long exptDataId = reader.getLong(2);
            Long exptResultId = reader.getLong(3);
            String json = reader.getString(4);
            JsonNode node = mapper.readTree(json);
            
            ArrayNode anode;
            String displayName;
            Double value;
            JsonNode valueNode;
            
            //JB: add priority elements first
            JsonNode priorityElems = node.get("priorityElements");
            if (priorityElems instanceof ArrayNode) {
                anode = (ArrayNode) priorityElems;
                for (int i = 0; i < anode.size(); i++) {
                    displayName = anode.get(i).get("displayName").textValue();
                    value = null;
                    valueNode = anode.get(i).get("value");
                    if (valueNode != null && CAPUtil.isNumber(valueNode.textValue()))
                        value = Double.parseDouble(valueNode.textValue());
                    if (value != null) {
                        nexplode++;
                        writer.write(bardExptId + "," + exptDataId + "," + exptResultId + "," + displayName + "," + value + "\n");
                    }
                }
            }
            
            JsonNode rootElems = node.get("rootElements");
            if (rootElems instanceof ArrayNode) {
                anode = (ArrayNode) rootElems;
                for (int i = 0; i < anode.size(); i++) {
                    displayName = anode.get(i).get("displayName").textValue();
                    value = null;
                    valueNode = anode.get(i).get("value");
                    if (valueNode != null && CAPUtil.isNumber(valueNode.textValue()))
                        value = Double.parseDouble(valueNode.textValue());
                    if (value != null) {
                        nexplode++;
                        writer.write(bardExptId + "," + exptDataId + "," + exptResultId + "," + displayName + "," + value + "\n");
                    }
                }
            }
        }
        writer.close();
        reader.close();
        pstReader.close();
        log.info("Exploded " + nresult + " results to " + nexplode + " rows");

        // now read in our exploded rows and load in
        BufferedReader rows = new BufferedReader(new FileReader(tmpFileName));
        PreparedStatement pstWriter = conn.prepareStatement("insert into temp_exploded_results (bard_expt_id, expt_data_id, expt_result_id, display_name, value) " +
                " values (?,?,?,?,?)");
        String line;
        int n = 0;
        while ((line = rows.readLine()) != null) {
            String[] toks = line.trim().split(",");
            pstWriter.setLong(1, Long.parseLong(toks[0]));
            pstWriter.setLong(2, Long.parseLong(toks[1]));
            pstWriter.setLong(3, Long.parseLong(toks[2]));
            pstWriter.setString(4, toks[3]);
            pstWriter.setDouble(5, Double.parseDouble(toks[4]));
            pstWriter.addBatch();

            n++;
            if (n % 1000 == 0) {
                pstWriter.executeBatch();
 //               log.info("\rWrote " + n + " exploded rows");
            }
        }
        pstWriter.executeBatch();
        conn.commit();
        pstWriter.close();
        conn.close();
        log.info("Loaded " + n + " exploded results into database");

        (new File(tmpFileName)).delete();
    }
    

    public static void main(String[] args) throws SQLException, IOException {
        ResultExploder re = new ResultExploder();
        //iterate over all results, truncates then loads into temp_exploded_results.
        re.utilityLoadCurrentResultsIntoTempExplodedResults();
    }
}
