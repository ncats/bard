package gov.nih.ncgc.bard.resourcemgr.extresource.ontology.go;

import gov.nih.ncgc.bard.ontology.OntologyNode;

import java.util.Vector;

public class GONode extends OntologyNode {

	private int goID;
	private String goAccession;
	private String goName;
	private String goOntologyType;
	private String evCode;
	private boolean isObsolete;
	private boolean implied;
	
	//new fields related to go to accession info
	private String goAssociationDBRef;
	private String goAssignmentDate;
	
	private float score;
	private int associationCount;
	
	private Vector <GONode> directAssocNodes;
	
	public GONode() { }

	public GONode(boolean initialize) {
		super(initialize);
	}
	
	public GONode (int goID, String goAccession, String goName, String goOntologyType, String evCode) {
		this.goID = goID;
		this.goAccession = goAccession;
		this.goName = goName;
		this.goOntologyType = goOntologyType;
		this.evCode = evCode;
		score = 0.0f;
		associationCount = 0;
		directAssocNodes = new Vector<GONode>();
	}
	
	public int getGoID() {
		return goID;
	}

	public void setGoID(int goID) {
		this.goID = goID;
	}

	public String getGoAccession() {
		return goAccession;
	}

	public void setGoAccession(String goAccession) {
		this.goAccession = goAccession;
	}

	public String getGoName() {
		return goName;
	}

	public void setGoName(String goName) {
		this.goName = goName;
	}

	public String getGoOntologyType() {
		return goOntologyType;
	}

	public void setGoOntologyType(String goOntologyType) {
		this.goOntologyType = goOntologyType;
	}

	public boolean isObsolete() {
		return isObsolete;
	}

	public void setObsolete(boolean isObsolete) {
		this.isObsolete = isObsolete;
	}

	public float getScore() {
		return score;
	}

	public void setScore(float score) {
		this.score = score;
	}

	public int getAssociationCount() {
		return associationCount;
	}

	public void setAssociationCount(int associationCount) {
		this.associationCount = associationCount;
	}

	
	public String getEvCode() {
		return evCode;
	}

	public void setEvCode(String evCode) {
		this.evCode = evCode;
	}

	public int compareTo(Object other) {
		return this.goID - ((GONode)other).getGoID();
	}
	
	//equality on go accession
	public boolean equals(Object other) {
		return ( (goAccession+evCode).equals(((GONode)other).getGoAccession() + ((GONode)other).getEvCode()));
	}
	
	//hashcode also on accession
	public int hashCode() {
		return (goAccession+evCode).hashCode();
	}
	
	public boolean isImplied() {
		return implied;
	}

	public void setImplied(boolean implied) {
		this.implied = implied;
	}

	public String getGoAssociationDBRef() {
		return goAssociationDBRef;
	}

	public void setGoAssociationDBRef(String goAssociationDBRef) {
		this.goAssociationDBRef = goAssociationDBRef;
	}

	public String getGoAssignmentDate() {
		return goAssignmentDate;
	}

	public void setGoAssignmentDate(String goAssignmentDate) {
		this.goAssignmentDate = goAssignmentDate;
	}
	
	public Vector<GONode> getDirectAssocNodes() {
	    return directAssocNodes;
	}

	public void setDirectAssocNodes(Vector<GONode> directAssocNodes) {
	    this.directAssocNodes = directAssocNodes;
	}
	
	public void clearDirectAssocNodes() {
	    this.directAssocNodes.clear();
	}
	
	public void addDirectAssocNode(GONode directAssocNode) {
	    this.directAssocNodes.add(directAssocNode);
	}

	public String toString() {
		String lineSep = System.getProperty("line.separator");
		String value = "**************************" + lineSep;
		value += "GO ID = "+ getGoID() + lineSep;
		value += "GO Acc = "+ getGoAccession() + lineSep;
		value += "GO Term = "+ getGoName() + lineSep;
		value += "GO Type = "+ getGoOntologyType() + lineSep;
		value += "Number of Children = " + getChildCount() + lineSep;
		return value;
	}
	
	public String toStringVerbose(String spacer) {
		String lineSep = System.getProperty("line.separator");
		String value = spacer + "**************************" + lineSep;
		value += spacer + "GO ID = "+ getGoID() + lineSep;
		value += spacer + "GO Acc = "+ getGoAccession() + lineSep;
		value += spacer + "GO Term = "+ getGoName() + lineSep;
		value += spacer + "GO Type = "+ getGoOntologyType() + lineSep;
		value += spacer + "Number of Children = " + getChildCount() + lineSep;
		return value;
	}

	public String toString(String spacer) {
		//String lineSep = System.getProperty("line.separator");
		String value = spacer + "*****";
		//value += "("+ getGoID() + ")";
		value += " "+ getGoAccession();
		value += " "+ getGoName() + " (" + this.getAssociationCount() + ")";
		return value;
	}

	public void printTree(int level) {
		
		String spacer = "";
		for(int i = 0; i < level; i++)
			spacer +=  "   ";

		System.out.println(toString(spacer));

		for(int i = 0; i < this.getChildCount(); i++)
			( (GONode) (this.getChildNodes().get(i))).printTree(level + 1);
		
		
	}
	
	
}
