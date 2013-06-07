package gov.nih.ncgc.bard.capextract;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Rajarshi Guha
 */
public class ResultStatistics {

    protected Logger log;

    public ResultStatistics() {
        log = LoggerFactory.getLogger(this.getClass());
    }

    public void generateStatistics(Long bardExptId) throws SQLException {
        Connection conn = CAPUtil.connectToBARD(CAPConstants.getBardDBJDBCUrl());

        // first pull result types for this experiment
        PreparedStatement pst = conn.prepareStatement("select distinct display_name from exploded_results where bard_expt_id = ?");
        pst.setLong(1, bardExptId);
        List<String> resultTypes = new ArrayList<String>();
        ResultSet rs = pst.executeQuery();
        while (rs.next()) resultTypes.add(rs.getString(1));
        rs.close();
        pst.close();

        // for each result type, delete pre-existing histogram, make new histogram and insert it
        for (String resultType : resultTypes) {
            pst = conn.prepareStatement("delete from exploded_statistics where bard_expt_id = ? and display_name = ?");
            pst.setLong(1, bardExptId);
            pst.setString(2, resultType);
            pst.executeUpdate();
            pst.close();

            pst = conn.prepareStatement("select value from exploded_results where bard_expt_id = ? and display_name = ?");
            pst.setLong(1, bardExptId);
            pst.setString(2, resultType);
            rs = pst.executeQuery();
            List<Float> values = new ArrayList<Float>();
            while (rs.next()) values.add(rs.getFloat(1));
            rs.close();
            pst.close();

            Float[] stats = calculateStatistics(values);
            log.info("Got stats for " + bardExptId + "/" + resultType);

            pst = conn.prepareStatement("insert into exploded_statistics (bard_expt_id, display_name, n, minval, maxval, mean, sd, q1, q2, q3) " +
                    " values (?,?,?,?,?,?,?,?,?,?)");
            pst.setLong(1, bardExptId);
            pst.setString(2, resultType);
            for (int i = 0; i < stats.length; i++) {
                if (stats[i] == null
                        || stats[i].equals(Float.NaN)
                        || stats[i].equals(Float.NEGATIVE_INFINITY)
                        || stats[i].equals(Float.POSITIVE_INFINITY)) pst.setNull(3+i, Types.FLOAT);
                else pst.setFloat(3+i, stats[i]);
            }
            pst.executeUpdate();
            pst.close();
        }
        conn.commit();
        conn.close();
    }

    Float median(List<Float> values) {
        if (values.size() == 0) return null;
        int middle = values.size() / 2;
        if (values.size() % 2 == 1) return values.get(middle);
        return (values.get(middle - 1) + values.get(middle)) / 2.0f;
    }

    private Float[] calculateStatistics(List<Float> values) {
        if (values.size() == 0) return new Float[]{null, null, null, null, null, null, null, null};
        Float minval, maxval, mean, sd, q1, q2, q3;

        sd = CAPUtil.sd(values);
        mean = 0f;
        maxval = Float.MIN_VALUE;
        minval = Float.MAX_VALUE;
        for (Float v : values) {
            if (v > maxval) maxval = v;
            if (v < minval) minval = v;
            mean += v;
        }
        mean /= (float) values.size();

        // get quantiles
        Collections.sort(values);
        q2 = median(values);
        List<Float> half = new ArrayList<Float>();
        for (Float v : values) if (v <= q2) half.add(v);
        q1 = median(half);

        half = new ArrayList<Float>();
        for (Float v : values) if (v >= q2) half.add(v);
        if (half.size() == 0) q3 = null;
        else q3 = median(half);

        return new Float[]{(float) values.size(), minval, maxval, mean, sd, q1, q2, q3};
    }

    public static void main(String[] args) throws SQLException {
        ResultStatistics rstats = new ResultStatistics();

        Connection conn = CAPUtil.connectToBARD(CAPConstants.getBardDBJDBCUrl());
        PreparedStatement pst = conn.prepareStatement("select distinct bard_expt_id from exploded_results");
        ResultSet rs = pst.executeQuery();
        while (rs.next()) rstats.generateStatistics(rs.getLong(1));
        conn.close();
    }
}
