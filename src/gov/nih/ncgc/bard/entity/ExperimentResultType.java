package gov.nih.ncgc.bard.entity;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Rajarshi Guha
 */
public class ExperimentResultType implements BardEntity {
    String name;
    double max, min;
    long num;
    double mean, sd, q1, q2, q3;
    List<Float[]> histogram;

    public ExperimentResultType() {
        histogram = new ArrayList<Float[]>();
    }

    public double getMean() {
        return mean;
    }

    public void setMean(double mean) {
        this.mean = mean;
    }

    public double getSd() {
        return sd;
    }

    public void setSd(double sd) {
        this.sd = sd;
    }

    public double getQ1() {
        return q1;
    }

    public void setQ1(double q1) {
        this.q1 = q1;
    }

    public double getQ2() {
        return q2;
    }

    public void setQ2(double q2) {
        this.q2 = q2;
    }

    public double getQ3() {
        return q3;
    }

    public void setQ3(double q3) {
        this.q3 = q3;
    }

    public List<Float[]> getHistogram() {
        return histogram;
    }

    public void setHistogram(List<Float[]> histogram) {
        this.histogram = histogram;
    }

    public ExperimentResultType(String name) {
        this.name = name;
    }

    public long getNum() {
        return num;
    }

    public void setNum(long num) {
        this.num = num;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getMax() {
        return max;
    }

    public void setMax(double max) {
        this.max = max;
    }

    public double getMin() {
        return min;
    }

    public void setMin(double min) {
        this.min = min;
    }

    @Override
    public String getResourcePath() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setResourcePath(String resourcePath) {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
