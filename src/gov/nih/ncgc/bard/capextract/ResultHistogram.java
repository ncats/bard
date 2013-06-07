package gov.nih.ncgc.bard.capextract;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Rajarshi Guha
 */
public class ResultHistogram {
    protected Logger log;

    public ResultHistogram() {
        log = LoggerFactory.getLogger(this.getClass());
    }

    public void generateHistogram(Long bardExptId) throws SQLException {
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
            pst = conn.prepareStatement("delete from exploded_histograms where bard_expt_id = ? and display_name = ?");
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

            List<HBin> bins = calculateHistogram(values);
//            for (HBin b : bins) System.out.println(b.l + " - " + b.u + " => " + b.c);
            pst = conn.prepareStatement("insert into exploded_histograms (bard_expt_id, display_name, l, u, n) values (?,?,?,?,?)");
            for (HBin bin : bins) {
                pst.setLong(1, bardExptId);
                pst.setString(2, resultType);
                pst.setFloat(3, bin.l);
                pst.setFloat(4, bin.u);
                pst.setInt(5, bin.c);
                pst.executeUpdate();
                pst.clearParameters();
            }
            pst.close();
        }
        conn.commit();
        conn.close();
    }


    List<HBin> calculateHistogram(List<Float> values) {
        List<HBin> bins = new ArrayList<HBin>();
        if (values.size() < 3) return bins;

        float sd = CAPUtil.sd(values);
        if (sd == 0) return bins;

        // currently we use Scotts method to get optimal bin width/bin count
        float h = (float) (3.5 * sd / Math.pow((double) values.size(), 1.0 / 3.0));   // bin width
        float maxval = Float.MIN_VALUE;
        float minval = Float.MAX_VALUE;
        for (Float v : values) {
            if (v > maxval) maxval = v;
            if (v < minval) minval = v;
        }
        float k = (maxval - minval) / h;   // number of bins
        log.info("sd = " + sd + ", h = " + h + ", k = " + k + " (will use " + (int) Math.floor(k) + " bins)");

        float lower = minval;
        for (int i = 1; i <= Math.floor(k); i++) {
            HBin bin = new HBin(lower, lower + i * h, 0);
            bins.add(bin);
            lower += i * h;
        }

        for (Float v : values) {
            for (HBin bin : bins) {
                if (v >= bin.l && v < bin.u) {
                    bin.c++;
                    break;
                }
            }
        }

        // remove empty bins from the tail of the distribution
        List<Integer> removeIdx = new ArrayList<Integer>();
        for (int i = bins.size()-1; i >= 0; i--) {
            if (bins.get(i).c > 0) break;
            else removeIdx.add(i);
        }
        List<HBin> cleanBins = new ArrayList<HBin>();
        for (int i = 0; i < bins.size(); i++) {
            if (!removeIdx.contains(i)) cleanBins.add(bins.get(i));
        }

        return cleanBins;
    }

    class HBin {
        float l, u;
        int c;

        HBin(float l, float u, int c) {
            this.l = l;
            this.u = u;
            this.c = c;
        }
    }

    public static void main(String[] args) throws SQLException {
        ResultHistogram r = new ResultHistogram();
        r.generateHistogram(108L);
    }
}
