package gov.nih.ncgc.bard.capextract;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.sql.*;
import java.util.UUID;

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

    public static void main(String[] args) throws SQLException, IOException {
        ResultExploder re = new ResultExploder();
        re.explodeResults(102L);
    }
}
