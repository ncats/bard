package gov.nih.ncgc.bard.capextract;

import jdistlib.disttest.NormalityTest;
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

    final Float MAX_VALUE = 1e38f;
    Long currentEid = -1l;

    public ResultHistogram() {
        log = LoggerFactory.getLogger(this.getClass());
    }

    public void generateHistogram(Long bardExptId) throws SQLException {
        boolean useLog = false;
        currentEid = bardExptId;
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

            pst = conn.prepareStatement("select value from exploded_results where bard_expt_id = ? and display_name = ?");
            pst.setLong(1, bardExptId);
            pst.setString(2, resultType);
            rs = pst.executeQuery();
            List<Float> values = new ArrayList<Float>();
            while (rs.next()) values.add(rs.getFloat(1));
            rs.close();
            pst.close();

            // get the log10 version of the values
            double[] vals = new double[values.size()];
            for (int i = 0; i < values.size(); i++) vals[i] = Math.log10(values.get(i));

            double statistic = NormalityTest.anderson_darling_statistic(vals);
            double pvalue = NormalityTest.anderson_darling_pvalue(statistic, vals.length);
            if (Double.isNaN(pvalue) || pvalue < 0.05) {  // log10 data is normal, so replace original values with the log10 values
                log.info("BARD experiment id " + bardExptId + "/" + resultType + " is log normal. Histogramming log10 values");
                for (int i = 0; i < values.size(); i++) values.set(i, (float) vals[i]);
                useLog = true;
            }

            List<HBin> bins = calculateHistogram(values);

            // get rid of pre-existing histogram
            pst = conn.prepareStatement("delete from exploded_histograms where bard_expt_id = ? and display_name = ?");
            pst.setLong(1, bardExptId);
            pst.setString(2, resultType);
            pst.executeUpdate();
            pst.close();

            // put in new histogram
            pst = conn.prepareStatement("insert into exploded_histograms (bard_expt_id, display_name, l, u, n) values (?,?,?,?,?)");
            for (HBin bin : bins) {
                pst.setLong(1, bardExptId);
                pst.setString(2, resultType);
                if (useLog) {
                    pst.setFloat(3, (float) Math.pow(10, bin.l));
                    pst.setFloat(4, (float) Math.pow(10, bin.u));
                } else {
                    pst.setFloat(3, bin.l);
                    pst.setFloat(4, bin.u);
                }
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
        log.info("EID " + currentEid + ": sd = " + sd + ", h = " + h + ", k = " + k + " (will use " + (int) Math.ceil(k) + " bins)");

        // given the number of bins from the bin width algo, we make a set of pretty breaks
        // based on pretty.R and pretty.c from R 3.0.1
        double[] prettyBounds = pretty(minval, maxval, (int) Math.ceil(k), 1, true);
        minval = (float) prettyBounds[0];
        maxval = (float) prettyBounds[1];
        // recalculate h based on pretty bounds
        h = (float) ((maxval - minval) / (prettyBounds[2]));

        float lower = minval;
        int nbin = (int) prettyBounds[2] + 1;
        for (int i = 1; i <= nbin; i++) {
            HBin bin = new HBin(lower, lower + h, 0);
            bins.add(bin);
            lower += h;
        }

        // raw breaks - not pretty
//        float lower = minval;
//        for (int i = 1; i <= Math.floor(k)+1; i++) {
//            HBin bin = new HBin(lower, lower + h, 0);
//            bins.add(bin);
//            lower += h;
//        }

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
        for (int i = bins.size() - 1; i >= 0; i--) {
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

    private static float calculateMachineEpsilonFloat() {
        float machEps = 1.0f;

        do
            machEps /= 2.0f;
        while ((float) (1.0 + (machEps / 2.0)) != 1.0);

        return machEps;
    }

    double[] pretty(double lo, double up, int ndiv, int min_n, boolean return_bounds) {
        double dx, cell, unit, base, U;
        double ns, nu;
        int k;
        boolean i_small;
        double shrink_sml = 0.75;
        double h = 1.5;
        double h5 = .5 + 1.5 * h;
        int eps_correction = 0;
        double rounding_eps = 1e-7;
        double DBL_EPSILON = calculateMachineEpsilonFloat();


        dx = up - lo;
        if (dx == 0 && up == 0) { /*  up == lo == 0  */
            cell = 1;
            i_small = true;
        } else {
            cell = Math.max(Math.abs(lo), Math.abs(up));
            /* U = upper bound on cell/unit */
//            U = (1 + (h5 >= 1.5*h+.5)) ? 1/(1+h) : 1.5/(1+h5);
            U = 1 / (1 + h);
            /* added times 3, as several calculations here */
            i_small = dx < cell * U * Math.max(1, ndiv) * DBL_EPSILON * 3;
        }

        if (i_small) {
            if (cell > 10)
                cell = 9 + cell / 10;
            cell *= shrink_sml;
            if (min_n > 1) cell /= min_n;
        } else {
            cell = dx;
            if (ndiv > 1) cell /= ndiv;
        }

        if (cell < 20 * Double.MIN_VALUE) {
            cell = 20 * Double.MIN_VALUE;
        } else if (cell * 10 > Double.MAX_VALUE) {
            cell = .1 * Double.MAX_VALUE;
        }
        base = Math.pow(10., Math.floor(Math.log10(cell))); /* base <= cell < 10*base */

        unit = base;
        if ((U = 2 * base) - cell < h * (cell - unit)) {
            unit = U;
            if ((U = 5 * base) - cell < h5 * (cell - unit)) {
                unit = U;
                if ((U = 10 * base) - cell < h * (cell - unit)) unit = U;
            }
        }

        ns = Math.floor(lo / unit + rounding_eps);
        nu = Math.ceil(up / unit - rounding_eps);

        if (eps_correction == 1 && (eps_correction > 1 || !i_small)) {
            if (lo != 0) lo *= (1 - DBL_EPSILON);
            else lo = -Double.MIN_VALUE;
            if (up != 0) up *= (1 + DBL_EPSILON);
            else up = +Double.MIN_VALUE;
        }

        while (ns * unit > lo + rounding_eps * unit) ns--;
        while (nu * unit < up - rounding_eps * unit) nu++;
        k = (int) (0.5 + nu - ns);
        if (k < min_n) {
            k = min_n - k;
            if (ns >= 0.) {
                nu += k / 2;
                ns -= k / 2 + k % 2;/* ==> nu-ns = old(nu-ns) + min_n -k = min_n */
            } else {
                ns -= k / 2;
                nu += k / 2 + k % 2;
            }
            ndiv = min_n;
        } else {
            ndiv = k;
        }
        if (return_bounds) { /* if()'s to ensure that result covers original range */
            if (ns * unit < lo) lo = ns * unit;
            if (nu * unit > up) up = nu * unit;
        } else {
            lo = ns;
            up = nu;
        }

        return new double[]{lo, up, ndiv};
    }


    public static void main(String[] args) throws SQLException {
        ResultHistogram r = new ResultHistogram();
        r.generateHistogram(830L);

//        Connection conn = CAPUtil.connectToBARD(CAPConstants.getBardDBJDBCUrl());
//        PreparedStatement pst = conn.prepareStatement("select distinct bard_expt_id from exploded_results");
//        ResultSet rs = pst.executeQuery();
//        while (rs.next()) r.generateHistogram(rs.getLong(1));
//        conn.close();

    }
}
