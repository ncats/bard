package gov.nih.ncgc.bard.resourcemgr.extresource.ontology;

import java.io.Serializable;
import java.util.Vector;

public class OntologyNode implements Comparable, Serializable {
	
	private Object key;
	private Vector <OntologyNode> parentNodes;
	private Vector <OntologyNode> childNodes;
	private int associationCount;
	
	public OntologyNode() { 
		super();
	}
	
	public OntologyNode(boolean initialize) {
		super();
		if(initialize) {
			key = "";
			parentNodes = new Vector <OntologyNode>();
			childNodes = new Vector <OntologyNode>();
			associationCount = 0;
		}
	}
	
	public int getParenetCount() {
		if(parentNodes != null)
			return parentNodes.size();
		else
			return 0;
	}

	public int getChildCount() {
		if(childNodes != null)
			return childNodes.size();
		else
			return 0;
	}
	
	public void addParentNode(OntologyNode node) {
		if(parentNodes == null)
			parentNodes = new Vector <OntologyNode> ();
		if(!parentNodes.contains(node)) {
			parentNodes.add(node);
		}
	}
	
	public void addChildNode(OntologyNode node) {
		if(childNodes == null)
			childNodes = new Vector <OntologyNode> ();
		if(!childNodes.contains(node)) {
			childNodes.add(node);
		}
	}	
	
	//access methods
	public Vector<OntologyNode> getParentNodes() {
		return parentNodes;
	}
	public void setParentNodes(Vector<OntologyNode> parentNodes) {
		this.parentNodes = parentNodes;
	}
	public Vector<OntologyNode> getChildNodes() {
		return childNodes;
	}
	public void setChildNodes(Vector<OntologyNode> childNodes) {
		this.childNodes = childNodes;
	}
	
	/**
	 * Comparable stub, extending classes will override based on comparable criteria
	 */
	public int compareTo(Object other) {
		return 0;
	}
	
	/**
	 * Equals stub, extending classess will override based on criteria
	 */
	public boolean equals(Object other) {
		return false;
	}
}
