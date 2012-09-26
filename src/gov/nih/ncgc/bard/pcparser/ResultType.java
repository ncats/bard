package gov.nih.ncgc.bard.pcparser;
// $Id: ResultType.java 2278 2008-05-29 22:27:45Z nguyenda $

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class ResultType implements Constants, Serializable {
    private static final long serialVersionUID = 12242007;

    
    /*
      Tracking or Description Information
       Assay Result Field Type ID (TID)
    */
    protected int tid = 0;

    /*
      Result Field Name (short name for display)
    */
    protected String name; 

    /*
      Result Field Description
    */
    protected String description;

    /*
      Result Field type
    */
    protected Type type = Type.Unknown;

    /*
      Result Field unit (if any)
    */
    protected Unit unit = Unit.unspecified;

    /*
      Result Field transform (if any)
    */
    protected Transform transform = Transform.None;

    /*
      if true, indicates that this TID field 
      provides active concentration summary by 
      reporting the concentration which produces 
      50% of the maximum possible biological response
      such as IC50, EC50, AC50, GI50 etc. 
      or by reporting constant parameters such as Ki, 
      that based on which the activity outcome in this assay is called
     */
    protected boolean isActiveConc = false;

    /*
      tested concentration: 
      testConcValue.length > 1 for dose response
      testConcValue.length == 1 for single point activity
    */
    protected Double[] testConcValue = {};
    protected Unit testConcUnit = Unit.none;

    protected Constants.DataType dataType = Constants.DataType.Unknown;
    protected int dataTypeElem = Constants.DataType.Unknown.getElem();
    
    int contextGroup = -1;
    
    public ResultType () {}
    public ResultType (int tid) { this.tid = tid; }
    
    public int getTID () { return tid; }
    public void setTID (int tid) { this.tid = tid; }

    public String getName () { return name; }
    public void setName (String name) { this.name = name; }

    public String getDescription () { return description; }
    public void setDescription (String desc) { description = desc; }

    public Type getType () { return type; }
    public void setType (Type type) { this.type = type; }

    public Unit getUnit () { return unit; }
    public void setUnit (Unit unit) { this.unit = unit; }

    public Transform getTransform () { return transform; }
    public void setTransform (Transform transform) { 
	this.transform = transform; 
    }

    public boolean isActiveConcentration () { return isActiveConc; }
    public void setActiveConcentration (boolean active) {
	isActiveConc = active;
    }

    public int getNumActivityDataPoints () { return testConcValue.length; }
    public Double[] getTestConcentration () { return testConcValue; }
    public void setTestConcentration (Double[] testConcValue) {
	this.testConcValue = testConcValue; 
    }
    public Unit getTestConcUnit () { return testConcUnit; }
    public void setTestConcUnit (Unit unit) { testConcUnit = unit; }

    public Constants.DataType getDataType () { return dataType; }
    public void setDataType (Constants.DataType dataType) { this.dataType = dataType; dataTypeElem = dataType.getElem();}

    public int getDataTypeElem () { return dataType.getElem(); }
    public void setDataTypeElem (int elem) { this.dataType = DataType.getDataType(elem); dataTypeElem = elem;}

    public int getContextGroup () { return contextGroup; }
    public void setContextGroup (int contextGroup) { this.contextGroup = contextGroup; }

    public String toString () {
	StringBuffer sb = new StringBuffer
	    ("[TID="+tid+",DataType="+dataType+",ContextGroup="+contextGroup+",Name="+name+",Description="+description
	     +",Type="+type+",Unit="+unit+",Transform="+transform
	     +",IsActiveConc="+isActiveConc);
	if (testConcUnit != null) {
	    sb.append(",TestConcUnit="+testConcUnit
		      +",TestConcValue=[");
	    if (testConcValue.length > 0) {
		sb.append(testConcValue[0]);
		for (int i = 1; i < testConcValue.length; ++i) {
		    sb.append("," + testConcValue[i]);
		}
	    }
	    sb.append("]");
	}
	sb.append("]");
	return sb.toString();
    }

    public static String printHeader() {
	return "TID|DataType|DataTypeElem|ContextGroup|Name|Description|Type|Unit|Transform|IsActiveConc|TestConcUnit|TestConcValue";
    }
    
    public String print () {
	String desc = description.replace('\n', ' ');
	desc = desc.replace('\r', ' ');
	StringBuffer sb = new StringBuffer
	    (tid+"|"+dataType.getText()+"|"+dataType.getElem()+"|"+contextGroup+"|"+name+"|"+desc
	     +"|"+type+"|"+unit+"|"+transform
	     +"|"+isActiveConc+"|");
	if (testConcUnit != null)
	    sb.append(testConcUnit);
	sb.append("|");
	if (testConcUnit != null)
	    if (testConcValue.length > 0) {
		sb.append(testConcValue[0]);
		for (int i = 1; i < testConcValue.length; ++i) {
		    sb.append("," + testConcValue[i]);
		}
	    }
	return sb.toString();
    }

    private void writeObject (ObjectOutputStream out) throws IOException {
	out.writeInt(tid);
	out.writeObject(name);
	out.writeObject(description);
	out.writeInt(type.ordinal());
	out.writeInt(unit.ord());
	out.writeInt(transform.ordinal());
	out.writeBoolean(isActiveConc);
	out.writeObject(testConcValue);
	out.writeInt(testConcUnit.ord());
    }

    private void readObject (ObjectInputStream in) 
	throws IOException, ClassNotFoundException {
	tid = in.readInt();
	name = (String)in.readObject();
	description = (String)in.readObject();
	type = Type.getInstance(in.readInt());
	unit = Unit.getInstance(in.readInt());
	transform = Transform.getInstance(in.readInt());
	isActiveConc = in.readBoolean();
	testConcValue = (Double[])in.readObject();
	testConcUnit = Unit.getInstance(in.readInt());
    }
}
