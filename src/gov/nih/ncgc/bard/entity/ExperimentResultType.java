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
    List<Float[]> histogram;

    public ExperimentResultType() {
        histogram = new ArrayList<Float[]>();
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
