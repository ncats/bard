package gov.nih.ncgc.bard.rest.rowdef;

public class DataResultObject implements Comparable {
    private int tid;
    private String resultName;
    private Object value;

    public DataResultObject() {
    }

    public DataResultObject(int resultIndex, String resultName, Object val) {
        this.tid = resultIndex;
        this.resultName = resultName;
        this.value = val;
    }

    public int getTid() {
        return tid;
    }

    public void setTid(int tid) {
        this.tid = tid;
    }

    public String getResultName() {
        return resultName;
    }

    public void setResultName(String resultName) {
        this.resultName = resultName;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public int compareTo(Object other) {
        return this.tid = ((DataResultObject) other).getTid();
    }

}