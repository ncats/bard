package gov.nih.ncgc.bard.resourcemgr.extresource.ontology.go;

import gov.nih.ncgc.bard.resourcemgr.BardDBUtil;
import gov.nih.ncgc.bard.resourcemgr.extresource.ontology.OntologyNode;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import java.util.Stack;
import java.util.Vector;
import java.util.logging.Logger;

public class GOQueryWorker {

    static final private Logger logger = 
	    Logger.getLogger(GOQueryWorker.class.getName());


    //these will come from properties
    private String dbURL = "jdbc:mysql://protein.nhgri.nih.gov:3306/bard2";
    private String driverName = "com.mysql.jdbc.Driver";

    private Connection conn;

    private Hashtable <Integer, GONode> idNodeHash;
    private Hashtable <String, GONode> goAccNodeHash;



    public GOQueryWorker() { }	

    public void prepareStatements(String dbURL) {
	try {
	    conn = BardDBUtil.connect(dbURL);
	    parentPS = conn.prepareStatement("select term1_id from go_term2term where term2_id = ?");
	} catch (SQLException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	} catch (ClassNotFoundException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}

    }


    public void populateIdNodeHash() throws SQLException {
	idNodeHash = new Hashtable <Integer, GONode> ();
	PreparedStatement ps = conn.prepareStatement("select id from go_term");
	ResultSet rs = ps.executeQuery();
	while(rs.next()) {
	    idNodeHash.put(rs.getInt(1), this.getNodeForID(rs.getInt(1)));
	    //logger.info("id node hash for id:"+rs.getInt(1));
	}		
	rs.close();
    }

    public void populateNodeHashes() throws SQLException {
	populateIdNodeHash();
	populateGoAccNodeHashFromIdHash();
    }

    public void populateGoAccNodeHash() throws SQLException {
	goAccNodeHash = new Hashtable <String, GONode> ();
	PreparedStatement ps = conn.prepareStatement("select acc from go_term");
	ResultSet rs = ps.executeQuery();
	while(rs.next()) {
	    goAccNodeHash.put(rs.getString(1), getNodeForGoAcc(rs.getString(1)));
	}
	rs.close();
    }

    public void populateGoAccNodeHashFromIdHash() throws SQLException {
	goAccNodeHash = new Hashtable <String, GONode> ();

	Set <Integer> keys = idNodeHash.keySet();
	GONode currNode;
	for(int key : keys) {
	    currNode = idNodeHash.get(key);
	    goAccNodeHash.put(currNode.getGoAccession(), currNode);
	    //logger.info("pop go acc node hash from id hash "+currNode.getGoAccession());
	}		
    }

    public GONode appendChildNodesToNode(GONode node) {
	GONode root = node;

	return root;
    }

    public GONode appendParentNodesToNode(GONode node) {
	GONode root = node;

	try {
	    Vector <Integer> visitedAccList = new Vector <Integer>();
	    Vector <Integer> goAccList = new Vector <Integer>();
	    Vector <Integer> resultList = new Vector <Integer>();
	    Vector <OntologyNode> resultNodes = new Vector <OntologyNode>();

	    PreparedStatement idStmt = conn.prepareStatement("select id from go_term where acc = ?");

	    idStmt.setString(1, node.getGoAccession());
	    int goID = 0;

	    ResultSet idRS = idStmt.executeQuery();
	    if(idRS.next()) {
		goID = idRS.getInt(1);
	    } else {
		return null;
	    }


	    PreparedStatement childQuery = conn.prepareStatement("select term1_id from go_term2term where term2_id = ?");

	    //initialize goAccList
	    goAccList.add(goID);
	    ResultSet childRS;

	    //root node
	    GONode currentNode = createAndPopulateNodeFromID(goID);
	    root = currentNode;
	    resultNodes.add(currentNode);

	    int currentID;
	    int listIndex = 0;

	    //use a stack-pop approach to avoid recursion record overhead on stack memory
	    while (goAccList.size() > 0) {

		//	for(int listIndex = 0; listIndex < goAccList.size(); listIndex++) {
		goID = goAccList.get(0);

		//always get the first in queue
		currentNode = (GONode) resultNodes.get(0);

		childQuery.setInt(1, goID);
		childRS = childQuery.executeQuery();
		resultList.clear();
		while(childRS.next()) {
		    currentID = childRS.getInt(1);
		    resultList.add(currentID);
		}
		//the result list has the current id list
		//these are children of the current node in the go accession list
		//push the child nodes onto the goAccessionList
		goAccList.addAll(resultList);

		//register the current id as visited
		visitedAccList.add(goID);			

		//pop the current id from the source list				
		goAccList.remove(0);
		resultNodes.remove(0);

		//now add children
		addAncestorNodes(currentNode, resultList);

		//add children to resultNodes list
		if(currentNode.getParenetCount() > 0)
		    resultNodes.addAll(currentNode.getParentNodes());
		//}
	    }

	} catch (SQLException sqle) {
	    sqle.printStackTrace();
	}

	return root;
    }

    private PreparedStatement parentPS;


    public Vector <GONode> getPredNodes(GONode node) {

	Vector <GONode> nodeVector = new Vector <GONode>();

	//		logger.info("In query worker. Getting Ancestor Nodes for "+node.getGoID());


	HashSet <Integer> set = collectPredecessorNodesStack(node.getGoID());

	for(Integer id : set) {
	    nodeVector.add(this.createAndPopulateNodeFromID(id));
	}


	return nodeVector;
    }

    public void setAllNodeImplied(boolean implied) {
	Set <Integer> keys = idNodeHash.keySet();
	GONode currNode;
	for(int key : keys) {
	    currNode = idNodeHash.get(key);
	    currNode.setImplied(implied);
	}
    }

    public void clearDirectAssocNodes(boolean implied) {
	Set <Integer> keys = idNodeHash.keySet();
	GONode currNode;
	for(int key : keys) {
	    currNode = idNodeHash.get(key);
	    currNode.clearDirectAssocNodes();
	}
    }
    public Vector <GONode> getPredNodesFromHash(GONode node) {

	Vector <GONode> nodeVector = new Vector <GONode>();

	//		logger.info("In query worker. Getting Ancestor Nodes for "+node.getGoID());


	HashSet <Integer> set = collectPredecessorNodesStack(node.getGoID());

	for(Integer id : set) {
	    nodeVector.add(idNodeHash.get(id));
	}


	return nodeVector;
    }


    private void collectPredecessorNodes(int id, HashSet <Integer> set) {
	try {
	    parentPS.setInt(1, id);


	    //			logger.info("collecting pred nodes for id = "+id+" set size = " + set.size());

	    ResultSet rs = parentPS.executeQuery();
	    HashSet <Integer> newSet = new HashSet <Integer> ();
	    int rsInt;
	    while(rs.next()) {
		rsInt = rs.getInt(1);
		if(rsInt != 36808)
		    newSet.add(rsInt);			
	    }

	    rs.close();

	    for(int currID: newSet) {
		//skip if the parents have been retrieved already
		if(id != 36808 && !set.contains(currID))
		    collectPredecessorNodes(currID, set);
	    }

	    set.addAll(newSet);

	} catch (SQLException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
    }


    private HashSet <Integer> collectPredecessorNodesStack(int id) {

	HashSet <Integer> newSet = new HashSet <Integer> ();

	try {

	    int rsInt;

	    Stack <Integer> visitSet = new Stack <Integer> ();
	    visitSet.add(id);

	    while(!visitSet.isEmpty()) {
		parentPS.setInt(1, visitSet.pop());

		ResultSet rs = parentPS.executeQuery();

		while(rs.next()) {
		    rsInt = rs.getInt(1);
		    if(rsInt != 36808) {
			//just add novel nodes
			if(!newSet.contains(rsInt))
			    visitSet.push(rsInt);
			newSet.add(rsInt);
		    }
		}				
	    }

	} catch (SQLException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}

	return newSet;
    }



    private GONode buildRootAndChildren(String goAccession) {
	GONode root = null;

	try {
	    Vector <Integer> visitedAccList = new Vector <Integer>();
	    Vector <Integer> goAccList = new Vector <Integer>();
	    Vector <Integer> resultList = new Vector <Integer>();
	    Vector <OntologyNode> resultNodes = new Vector <OntologyNode>();

	    PreparedStatement idStmt = conn.prepareStatement("select id from go_term where acc = ?");

	    idStmt.setString(1, goAccession);
	    int goID = 0;

	    ResultSet idRS = idStmt.executeQuery();
	    if(idRS.next()) {
		goID = idRS.getInt(1);
	    } else {
		return null;
	    }


	    PreparedStatement childQuery = conn.prepareStatement("select term2_id from go_term2term where term1_id = ?");

	    //initialize goAccList
	    goAccList.add(goID);
	    ResultSet childRS;

	    //root node
	    GONode currentNode = createAndPopulateNodeFromID(goID);
	    root = currentNode;
	    resultNodes.add(currentNode);

	    int currentID;
	    int listIndex = 0;

	    //use a stack-pop approach to avoid recursion record overhead on stack memory
	    while (goAccList.size() > 0) {

		//	for(int listIndex = 0; listIndex < goAccList.size(); listIndex++) {
		goID = goAccList.get(0);

		//always get the first in queue
		currentNode = (GONode) resultNodes.get(0);

		childQuery.setInt(1, goID);
		childRS = childQuery.executeQuery();
		resultList.clear();
		while(childRS.next()) {
		    currentID = childRS.getInt(1);
		    resultList.add(currentID);
		}
		//the result list has the current id list
		//these are children of the current node in the go accession list
		//push the child nodes onto the goAccessionList
		goAccList.addAll(resultList);

		//register the current id as visited
		visitedAccList.add(goID);			

		//pop the current id from the source list				
		goAccList.remove(0);
		resultNodes.remove(0);

		//now add children
		addChildNodes(currentNode, resultList);

		//add children to resultNodes list
		if(currentNode.getChildCount() > 0)
		    resultNodes.addAll(currentNode.getChildNodes());
		//}
	    }

	} catch (SQLException sqle) {
	    sqle.printStackTrace();
	}

	return root;
    }


    private void addChildNodes(GONode rootNode, Vector <Integer> resultList) {
	GONode node;

	for(int i = 0; i < resultList.size(); i++) {
	    node = createAndPopulateNodeFromID(resultList.get(i));
	    if(node != null)
		rootNode.addChildNode(node);
	}
    }

    private void addAncestorNodes(GONode rootNode, Vector <Integer> resultList) {
	GONode node;

	for(int i = 0; i < resultList.size(); i++) {
	    node = createAndPopulateNodeFromID(resultList.get(i));
	    if(node != null)
		rootNode.addParentNode(node);
	}
    }

    public Connection connect(String dbURL, String driverName) {
	Connection conn = null;
	try {
	    Class.forName(driverName);
	    conn= DriverManager.getConnection(dbURL, "bard_manager", "bard_manager");
	} catch (SQLException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	} catch (ClassNotFoundException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
	return conn;
    }

    public GONode createAndPopulateNodeFromID(int goID) {
	GONode node = null;

	try {

	    PreparedStatement termQuery = conn.prepareStatement("select acc, name, term_type, is_obsolete from go_term where id = ?");

	    termQuery.setInt(1, goID);

	    ResultSet rs = termQuery.executeQuery();

	    if(rs.next()) {
		node = new GONode();
		node.setGoID(goID);
		node.setGoAccession(rs.getString(1));
		node.setGoName(rs.getString(2));
		node.setGoOntologyType(rs.getString(3));
		node.setObsolete(rs.getBoolean(4));
	    }

	    rs.close();

	    PreparedStatement assocQuery = conn.prepareStatement("select count(*) from go_association where term_acc = ?");

	    assocQuery.setString(1, node.getGoAccession());
	    rs = assocQuery.executeQuery();
	    if(rs.next()) {
		node.setAssociationCount(rs.getInt(1));
	    } else {
		node.setAssociationCount(0);
	    }

	} catch (SQLException sqle) {
	    sqle.printStackTrace();
	}

	return node;
    }

    public boolean populateNodeFromID(int goID, GONode node) {

	boolean populated = false;

	try {

	    PreparedStatement termQuery = conn.prepareStatement("select acc, name, term_type, is_obsolete from go_term where id = ?");

	    termQuery.setInt(1, goID);

	    ResultSet rs = termQuery.executeQuery();

	    if(rs.next()) {
		node.setGoID(goID);
		node.setGoAccession(rs.getString(1));
		node.setGoName(rs.getString(2));
		node.setGoOntologyType(rs.getString(3));
		node.setObsolete(rs.getBoolean(4));
		populated = true;
	    }

	} catch (SQLException sqle) {
	    sqle.printStackTrace();
	}

	return populated;
    }

    public GONode getNodeForID(int goID) {

	GONode node = null;

	try {

	    PreparedStatement termQuery = conn.prepareStatement("select acc, name, term_type, is_obsolete from go_term where id = ?");

	    termQuery.setInt(1, goID);

	    ResultSet rs = termQuery.executeQuery();

	    if(rs.next()) {
		node = new GONode();
		node.setGoID(goID);
		node.setGoAccession(rs.getString(1));
		node.setGoName(rs.getString(2));
		node.setGoOntologyType(rs.getString(3));
		node.setObsolete(rs.getBoolean(4));
	    }

	} catch (SQLException sqle) {
	    sqle.printStackTrace();
	}

	return node;
    }


    public GONode getNodeForGoAcc(String goAcc) {

	GONode node = null;

	try {

	    PreparedStatement termQuery = conn.prepareStatement("select id, name, term_type, is_obsolete from go_term where acc = ?");

	    termQuery.setString(1, goAcc);

	    ResultSet rs = termQuery.executeQuery();

	    if(rs.next()) {
		node = new GONode();
		node.setGoAccession(goAcc);
		node.setGoID(rs.getInt(1));
		node.setGoName(rs.getString(2));
		node.setGoOntologyType(rs.getString(3));
		node.setObsolete(rs.getBoolean(4));
	    }

	} catch (SQLException sqle) {
	    sqle.printStackTrace();
	}

	return node;
    }


    public Vector <String> collectUniqueProductAcc(GONode root) {
	Vector <String> accList = new Vector <String> ();

	Vector <OntologyNode> nodes = collectDescendentNodes(root);
	System.out.println("node count="+nodes.size());

	try {
	    PreparedStatement ps = conn.prepareStatement("select accession from go_association where term_acc = ?");

	    ResultSet rs;
	    for(int i = 0; i < nodes.size(); i++) {
		ps.setString(1, ((GONode)(nodes.get(i))).getGoAccession());
		rs = ps.executeQuery();
		while(rs.next()) {
		    accList.add(rs.getString(1));
		}				
	    }			

	} catch (SQLException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
	return accList;
    }


    public Vector <OntologyNode> collectDescendentNodes(OntologyNode root) {
	Vector <OntologyNode> nodeList = new Vector <OntologyNode> ();

	getDecendents(root, nodeList);

	return nodeList;
    }


    private void getDecendents(OntologyNode node,  Vector <OntologyNode> nodeList) {
	Vector <OntologyNode> children = node.getChildNodes();
	if(children != null) {
	    for(int i = 0; i < children.size(); i++) {
		getDecendents(children.get(i), nodeList);
	    }
	}
	if(!nodeList.contains(node))
	    nodeList.add(node);	
    }

    public HashSet <GONode> getGONodesForAccession(String accession) {

	String sql = "select a.term_acc, a.term_type, a.evidence, b.id, b.name, a.db_ref, a.assoc_date from go_association a, " +
		"go_term b where a.accession = ? and a.term_acc=b.acc";

	HashSet <GONode> nodes = new HashSet <GONode> ();

	try {

	    PreparedStatement ps = conn.prepareStatement(sql);

	    ps.setString(1, accession);
	    ResultSet rs = ps.executeQuery();

	    GONode node;
	    while(rs.next()) {
		node = new GONode();
		node.setGoAccession(rs.getString(1));
		node.setGoOntologyType(rs.getString(2));
		node.setEvCode(rs.getString(3));
		node.setGoID(rs.getInt(4));
		node.setGoName(rs.getString(5));
		node.setImplied(false);
		//build unique list of nodes for 
		nodes.add(node);
	    }
	    rs.close();

	    //this would append parents, let's not make this automatic
	    //			Set <GONode> set = nodeHash.keySet();
	    //			HashSet <GONode> uniqueSet = new HashSet <GONode> ();
	    //			for(GONode currNode : set) {
	    //				this.appendParentNodesToNode(currNode);						
	    //			}

	} catch (SQLException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}

	return nodes;
    }


    public HashSet <GONode> getGONodesForAccessionUsingHash(String accession) {

	String sql = "select a.term_acc, a.evidence, a.db_ref, a.assoc_date from go_association a where a.accession = ?";

	HashSet <GONode> nodes = new HashSet <GONode> ();

	try {

	    PreparedStatement ps = conn.prepareStatement(sql);

	    ps.setString(1, accession);
	    ResultSet rs = ps.executeQuery();

	    GONode node;
	    String acc;

	    while(rs.next()) {

		node = null;

		acc = rs.getString(1);

		node = goAccNodeHash.get(acc);

		if(node != null) {
		    node.setEvCode(rs.getString(2));
		    node.setImplied(false);
		    //build unique list of nodes for 
		    nodes.add(node);
		} else {
		    logger.info("No go node for accession = **"+acc+"**");
		}
	    }
	    rs.close();

	    //this would append parents, let's not make this automatic
	    //			Set <GONode> set = nodeHash.keySet();
	    //			HashSet <GONode> uniqueSet = new HashSet <GONode> ();
	    //			for(GONode currNode : set) {
	    //				this.appendParentNodesToNode(currNode);						
	    //			}

	} catch (SQLException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}

	return nodes;
    }


    public static void main(String [] args) {
	
	GOQueryWorker worker = new GOQueryWorker();
	//		GONode node = worker.createAndPopulateNodeFromID(5269);
	//		Vector <GONode> nodes = worker.getPredNodes(node);
	//		for(GONode n : nodes) {
	//			System.out.println(n);
	//		}
	//		System.out.println(nodes.size());
	GONode node = worker.buildRootAndChildren("GO:0006281");

	node.printTree(0);
	//		worker.collectUniqueProductAcc(node);
	//		
	//		Vector <String> accV = worker.collectUniqueProductAcc(node);
	//		System.out.println("unique accessions = " + accV.size());
	//
	//		String query = "select * from assay_target where accession in (";
	//
	//		Vector <String> v2 = new Vector <String>();
	//		for(int i = 0; i < accV.size(); i++) {
	//			if(!v2.contains(accV.get(i))) {
	//				v2.add(accV.get(i));
	//				query += "'"+accV.get(i)+"', ";
	//			}
	//		}
	//		
	//		System.out.println("unique accessions = " + v2.size());
	//		
	//		query = query.substring(0, query.length()-3);
	//		query += ")";
	//		
	//		System.out.println(query);
    }

}
