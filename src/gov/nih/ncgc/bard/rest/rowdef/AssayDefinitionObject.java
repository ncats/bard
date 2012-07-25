package gov.nih.ncgc.bard.rest.rowdef;

/**
 * A one line summary.
 *
 * @author Rajarshi Guha
 */
public class AssayDefinitionObject {
    String tid, name, description, type, unit, transform;
    boolean activeConcentration, activeConc;
    Float[] testConcValue, testConcentration;

    String testConcUnit;
    Integer numActivityDataPoints;

    public String getTid() {
        return tid;
    }

    public void setTid(String tid) {
        this.tid = tid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getTransform() {
        return transform;
    }

    public void setTransform(String transform) {
        this.transform = transform;
    }

    public boolean isActiveConcentration() {
        return activeConcentration;
    }

    public void setActiveConcentration(boolean activeConcentration) {
        this.activeConcentration = activeConcentration;
    }

    public boolean isActiveConc() {
        return activeConc;
    }

    public void setActiveConc(boolean activeConc) {
        this.activeConc = activeConc;
    }

    public Float[] getTestConcValue() {
        return testConcValue;
    }

    public void setTestConcValue(Float[] testConcValue) {
        this.testConcValue = testConcValue;
    }

    public Float[] getTestConcentration() {
        return testConcentration;
    }

    public void setTestConcentration(Float[] testConcentration) {
        this.testConcentration = testConcentration;
    }

    public String getTestConcUnit() {
        return testConcUnit;
    }

    public void setTestConcUnit(String testConcUnit) {
        this.testConcUnit = testConcUnit;
    }

    public Integer getNumActivityDataPoints() {
        return numActivityDataPoints;
    }

    public void setNumActivityDataPoints(Integer numActivityDataPoints) {
        this.numActivityDataPoints = numActivityDataPoints;
    }

    public AssayDefinitionObject() {

    }
}
