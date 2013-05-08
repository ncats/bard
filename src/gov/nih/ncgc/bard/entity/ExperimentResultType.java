package gov.nih.ncgc.bard.entity;

/**
 * @author Rajarshi Guha
 */
public class ExperimentResultType implements BardEntity {
    String name;
    double max, min;
    long num;

    public ExperimentResultType() {
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
