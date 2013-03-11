package gov.nih.ncgc.bard.resourcemgr.extresource.go;

import gov.nih.ncgc.bard.resourcemgr.extresource.ontology.go.GONode;
import gov.nih.ncgc.bard.resourcemgr.extresource.ontology.go.GOQueryWorker;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Logger;

public class BardGOEntityLoader {
	
	static final private Logger logger = 
			Logger.getLogger(BardGOEntityLoader.class.getName());

	private Connection conn;
	private String sqlSelectAccession = "select bard_assay_id, aid, accession from assay_target where accession is not null";
	private String sqlInsertAssayGO = "insert into temp_go_assay (bard_assay_id, pubchem_aid, target_acc, go_id, go_term, go_type, ev_code, implied)" +
			" values (?,?,?,?,?,?,?,?)";  //
	
	private String sqlUpdateAssayGoDBRefAndDate = 
			"update temp_go_assay a join go_association b on a.target_acc=b.accession and a.go_id=b.term_acc and a.ev_code=b.evidence " +
			"set a.go_assoc_db_ref=b.db_ref, a.assoc_date=b.assoc_date where a.implied = 0";
	
	private String sqlUpdateProjectGoDBRefAndDate = 
			"update temp_go_project a join go_association b on a.target_acc=b.accession and a.go_id=b.term_acc and a.ev_code=b.evidence " +
			"set a.go_assoc_db_ref=b.db_ref, a.assoc_date=b.assoc_date where a.implied = 0";
	
	private String sqlUpdateCompoundGoDBRefAndDate = 
			"update temp_go_compound a join go_association b on a.target_acc=b.accession and a.go_id=b.term_acc and a.ev_code=b.evidence " +
			"set a.go_assoc_db_ref=b.db_ref, a.assoc_date=b.assoc_date where a.implied = 0";
	
	private String dbURL = "jdbc:mysql://protein.nhgri.nih.gov:3306/bard3";
	private String driverName = "com.mysql.jdbc.Driver";
	
	private String sqlSelectProjectTargets = "select bard_proj_id, accession from project_target where accession is not null order by bard_proj_id asc";
	
	private String sqlInsertProjectGO = "insert into temp_go_project (bard_proj_id, target_acc, go_id, go_term, go_type, ev_code, implied)" +
			" values (?,?,?,?,?,?,?)";
	
	private String sqlInsertCompoundGO = "insert into temp_go_compound (cid, target_acc, go_id, go_term, go_type, ev_code, implied)" +
			" values (?,?,?,?,?,?,?)";
	
	PreparedStatement queryAccessionPS, insertGOPS;
	
	private String sqlSelectCompoundTarget = "select cid, val from compound_annot where annot_key ='TARGETS'";
	
	private long insertCnt;
	private long accessionCnt;
	
	public void loadGO() {

		try {
			conn = connect(dbURL, driverName);
			conn.setAutoCommit(false);
			
			insertCnt = 0;

			GOQueryWorker worker = new GOQueryWorker();
			worker.prepareStatements();
			
			queryAccessionPS = conn.prepareStatement(sqlSelectAccession);
			//queryAccessionPS.setFetchSize(Integer.MIN_VALUE);
			
			insertGOPS = conn.prepareStatement(this.sqlInsertAssayGO);

			ResultSet rs = queryAccessionPS.executeQuery();


			HashSet <GONode> set = new HashSet <GONode> ();
			long bardAssayID;
			long assayID;
			String accession;

			logger.info("accession result set");

			Vector <Long> bardAssayIdV = new Vector<Long>();
			Vector <Long> aids = new Vector<Long>();
			Vector <String> accV = new Vector<String>();
			//rather than collecting all go-nodes for all accessions, process each accession before building new.
			
			Hashtable <String, HashSet <Long>> accToAssayHash = new Hashtable <String, HashSet <Long>>();
			Hashtable <String, HashSet <Long>> accToBardIDHash = new Hashtable <String, HashSet <Long>>();

			while(rs.next()) {
			    	bardAssayID = rs.getLong(1);
				assayID = rs.getLong(2);
				accession = rs.getString(3);
			
				logger.info("aid capture, aid="+assayID);
				
				if(accToAssayHash.get(accession) == null) {
					HashSet <Long> v = new HashSet<Long>();
					v.add(assayID);
					accToAssayHash.put(accession, v);
					
					HashSet <Long> bardAssayV = new HashSet<Long>();
					bardAssayV.add(bardAssayID);
					accToBardIDHash.put(accession,  bardAssayV);
				} else {
					accToAssayHash.get(accession).add(assayID);
					accToBardIDHash.get(accession).add(assayID);
				}
				
				bardAssayIdV.add(bardAssayID);
				aids.add(assayID);
				accV.add(accession);
			}
			
			rs.close();
			
			int aidAccCnt = 0;
			
			//maybe collect all nodes into a hash or two that is keyed by go_id and go_acc
			//we can pull nodes from the hash as needed to support queries. 
			//we won't need to build and destroy nodes, just build references to the nodes
			
			//prepare the worker
			worker.populateNodeHashes();
			
			
			Set <String> accKeys = accToAssayHash.keySet();
			

			for(String accKey: accKeys) {
				set.clear();
				
				logger.info("process accession="+accKey);

				//reset implied to false
				worker.setAllNodeImplied(false);
				
				//reset direct 
				
				//get accessions nodes
				set.addAll(worker.getGONodesForAccessionUsingHash(accKey));
				//set.addAll(worker.getGONodesForAccession(accKey));
				
				HashSet <GONode> newSet = new HashSet <GONode>();

				//get accession's node's ancestors
				for(GONode node: set) {
					//go up the hierarchy to get implied for this accession
					Vector <GONode> nodes = worker.getPredNodesFromHash(node);		

					for(GONode n: nodes) {
						//don't overwrite if it exists (primary), if doesn't exist, it's implied
						if(!set.contains(n)) {
							n.setImplied(true);
							n.setEvCode("GO_ANCESTOR_TERM");
							newSet.add(n);
						}
					}
				}		
				
				newSet.addAll(set);
				
				int index = 0;
				Iterator <Long> bardIdEnum = accToBardIDHash.get(accKey).iterator();
				
				for(long aid:accToAssayHash.get(accKey)) {
				    
				    	bardAssayID = bardIdEnum.next();
					insertGOData(bardAssayID, aid, accKey, newSet);
					aidAccCnt++;
					index++;
				}
			}
			
//			
//			for(long aid:aids) {
//				accession = accV.get(aidAccCnt);
//				set.clear();
//				
//				//get accessions nodes
//				set.addAll(worker.getGONodesForAccession(accession));
//
//				HashSet <GONode> newSet = new HashSet <GONode>();
//						
//				//get accession's node's ancestors
//				for(GONode node: set) {
//					//go up the hierarchy to get implied for this accession
//					Vector <GONode> nodes = worker.getPredNodes(node);		
//
//					for(GONode n: nodes) {
//						//don't overwrite if it exists (primary), if doesn't exist, it's implied
//						if(!set.contains(n)) {
//							n.setImplied(true);
//							n.setEvCode("GO_ANCESTOR_TERM");
//							newSet.add(n);
//						}
//					}
//				}
//			
//				
//				newSet.addAll(set);
//				
//		//		logger.info("handling aid/accession="+aid+" "+accession);
//				//process just the inserts for this one accession
//				insertGOData(aid, accession, newSet);
//				aidAccCnt++;
//			}
			
			insertGOPS.executeBatch();
			conn.commit();
			
			//set details of assocation
			logger.info("update temp_go_assocation, assoc date and db_ref");
			Statement stmt = conn.createStatement();
			stmt.executeUpdate(sqlUpdateAssayGoDBRefAndDate);

			
			conn.close();
			logger.info("Done Load");
			
		} catch (SQLException sqle) {
			sqle.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	public void loadGOProject() {

		try {
			
			conn = connect(dbURL, driverName);
			conn.setAutoCommit(false);
			
			insertCnt = 0;

			GOQueryWorker worker = new GOQueryWorker();
			worker.prepareStatements();
			
			queryAccessionPS = conn.prepareStatement(this.sqlSelectProjectTargets);

			
			
			//queryAccessionPS.setFetchSize(Integer.MIN_VALUE);
			
			insertGOPS = conn.prepareStatement(sqlInsertProjectGO);

			ResultSet rs = queryAccessionPS.executeQuery();


			HashSet <GONode> set = new HashSet <GONode> ();
			long projectID;
			String accession;

			logger.info("accession result set");

			Vector <Long> aids = new Vector<Long>();
			Vector <String> accV = new Vector<String>();
			//rather than collecting all go-nodes for all accessions, process each accession before building new.
			
			Hashtable <String, HashSet <Long>> accToAssayHash = new Hashtable <String, HashSet <Long>>();
			
			while(rs.next()) {
				projectID = rs.getLong(1);
				accession = rs.getString(2);
			
				logger.info("aid capture, aid="+projectID);
				
				if(accToAssayHash.get(accession) == null) {
					HashSet <Long> v = new HashSet<Long>();
					v.add(projectID);
					accToAssayHash.put(accession, v);
				} else {
					accToAssayHash.get(accession).add(projectID);
				}
				
				aids.add(projectID);
				accV.add(accession);
			}
			
			rs.close();
			
			int aidAccCnt = 0;
			
			//maybe collect all nodes into a hash or two that is keyed by go_id and go_acc
			//we can pull nodes from the hash as needed to support queries. 
			//we won't need to build and destroy nodes, just build references to the nodes
			
			//prepare the worker
			worker.populateNodeHashes();
			
			
			Set <String> accKeys = accToAssayHash.keySet();
			

			for(String accKey: accKeys) {
				set.clear();
				
				logger.info("process accession="+accKey);

				//reset implied to false
				worker.setAllNodeImplied(false);
				
				//get accessions nodes
				set.addAll(worker.getGONodesForAccessionUsingHash(accKey));
				//set.addAll(worker.getGONodesForAccession(accKey));
				
				HashSet <GONode> newSet = new HashSet <GONode>();

				//get accession's node's ancestors
				for(GONode node: set) {
					//go up the hierarchy to get implied for this accession
					Vector <GONode> nodes = worker.getPredNodesFromHash(node);		

					for(GONode n: nodes) {
						//don't overwrite if it exists (primary), if doesn't exist, it's implied
						if(!set.contains(n)) {
							n.setImplied(true);
							n.setEvCode("GO_ANCESTOR_TERM");
							newSet.add(n);
						}
					}
				}		
				
				newSet.addAll(set);
				
				for(long projID:accToAssayHash.get(accKey)) {
					insertGODataForProject(projID, accKey, newSet);
					aidAccCnt++;
				}
				
				//logger.info("in gc()");
				//System.gc();
				

			}
			
//			
//			for(long aid:aids) {
//				accession = accV.get(aidAccCnt);
//				set.clear();
//				
//				//get accessions nodes
//				set.addAll(worker.getGONodesForAccession(accession));
//
//				HashSet <GONode> newSet = new HashSet <GONode>();
//						
//				//get accession's node's ancestors
//				for(GONode node: set) {
//					//go up the hierarchy to get implied for this accession
//					Vector <GONode> nodes = worker.getPredNodes(node);		
//
//					for(GONode n: nodes) {
//						//don't overwrite if it exists (primary), if doesn't exist, it's implied
//						if(!set.contains(n)) {
//							n.setImplied(true);
//							n.setEvCode("GO_ANCESTOR_TERM");
//							newSet.add(n);
//						}
//					}
//				}
//			
//				
//				newSet.addAll(set);
//				
//		//		logger.info("handling aid/accession="+aid+" "+accession);
//				//process just the inserts for this one accession
//				insertGOData(aid, accession, newSet);
//				aidAccCnt++;
//			}
			
			insertGOPS.executeBatch();
			conn.commit();
			
			//set details of assocation
			logger.info("update project assoc dbref and date");
			Statement stmt = conn.createStatement();
			stmt.executeUpdate(sqlUpdateProjectGoDBRefAndDate);

			
			conn.close();
			logger.info("Done Load");
			
		} catch (SQLException sqle) {
			sqle.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void loadGOCompound() {

		try {
			
			conn = connect(dbURL, driverName);
			conn.setAutoCommit(false);
			
			insertCnt = 0;

			GOQueryWorker worker = new GOQueryWorker();
			worker.prepareStatements();
			
			queryAccessionPS = conn.prepareStatement(sqlSelectCompoundTarget);
			
			insertGOPS = conn.prepareStatement(sqlInsertCompoundGO);

			ResultSet rs = queryAccessionPS.executeQuery();


			HashSet <GONode> set = new HashSet <GONode> ();
			long cid;
			String accession;

			logger.info("accession result set");

			Vector <Long> cids = new Vector<Long>();
			Vector <String> accV = new Vector<String>();
			//rather than collecting all go-nodes for all accessions, process each accession before building new.
			
			Hashtable <String, HashSet <Long>> accToAssayHash = new Hashtable <String, HashSet <Long>>();
			String [] toks;
			while(rs.next()) {
				cid = rs.getLong(1);
				toks = rs.getString(2).split("\\|");
				if(toks.length > 3) {
					accession = toks[3].trim();
					logger.info("Have Accession"+accession);
				} else
					continue;
				
				logger.info("cid capture, cid="+cid);
				
				if(accToAssayHash.get(accession) == null) {
					HashSet <Long> v = new HashSet<Long>();
					v.add(cid);
					accToAssayHash.put(accession, v);
				} else {
					accToAssayHash.get(accession).add(cid);
				}
				
				cids.add(cid);
				accV.add(accession);
			}
			
			rs.close();
			
			int aidAccCnt = 0;
			
			//maybe collect all nodes into a hash or two that is keyed by go_id and go_acc
			//we can pull nodes from the hash as needed to support queries. 
			//we won't need to build and destroy nodes, just build references to the nodes
			
			//prepare the worker
			worker.populateNodeHashes();
			
			
			Set <String> accKeys = accToAssayHash.keySet();
			

			for(String accKey: accKeys) {
				set.clear();
				
				logger.info("process accession="+accKey);

				//reset implied to false
				worker.setAllNodeImplied(false);
				
				//get accessions nodes
				set.addAll(worker.getGONodesForAccessionUsingHash(accKey));
				//set.addAll(worker.getGONodesForAccession(accKey));
				
				HashSet <GONode> newSet = new HashSet <GONode>();

				//get accession's node's ancestors
				for(GONode node: set) {
					//go up the hierarchy to get implied for this accession
					Vector <GONode> nodes = worker.getPredNodesFromHash(node);		

					for(GONode n: nodes) {
						//don't overwrite if it exists (primary), if doesn't exist, it's implied
						if(!set.contains(n)) {
							n.setImplied(true);
							n.setEvCode("GO_ANCESTOR_TERM");
							newSet.add(n);
						}
					}
				}		
				
				newSet.addAll(set);
				
				for(long aid:accToAssayHash.get(accKey)) {
					insertGODataForCompound(aid, accKey, newSet);
					aidAccCnt++;
				}
				
				//logger.info("in gc()");
				//System.gc();
				

			}
			
//			
//			for(long aid:aids) {
//				accession = accV.get(aidAccCnt);
//				set.clear();
//				
//				//get accessions nodes
//				set.addAll(worker.getGONodesForAccession(accession));
//
//				HashSet <GONode> newSet = new HashSet <GONode>();
//						
//				//get accession's node's ancestors
//				for(GONode node: set) {
//					//go up the hierarchy to get implied for this accession
//					Vector <GONode> nodes = worker.getPredNodes(node);		
//
//					for(GONode n: nodes) {
//						//don't overwrite if it exists (primary), if doesn't exist, it's implied
//						if(!set.contains(n)) {
//							n.setImplied(true);
//							n.setEvCode("GO_ANCESTOR_TERM");
//							newSet.add(n);
//						}
//					}
//				}
//			
//				
//				newSet.addAll(set);
//				
//		//		logger.info("handling aid/accession="+aid+" "+accession);
//				//process just the inserts for this one accession
//				insertGOData(aid, accession, newSet);
//				aidAccCnt++;
//			}
			
			insertGOPS.executeBatch();
			conn.commit();
			
			//set details of assocation
			logger.info("update compound assoc dbref and date");
			Statement stmt = conn.createStatement();
			stmt.executeUpdate(sqlUpdateCompoundGoDBRefAndDate);

			
			conn.close();
			logger.info("Done Load");
			
		} catch (SQLException sqle) {
			sqle.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	public Connection connect(String dbURL, String driverName) throws ClassNotFoundException {
		Connection conn = null;
		try {
			Class.forName(driverName);
			conn= DriverManager.getConnection(dbURL, "bard_manager", "bard_manager");
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return conn;
	}
	
	
	private void insertGOData(long bardAssayID, long assayID, String accession, Set <GONode> nodeSet) throws SQLException {
		String ontologyType;
//		logger.info(assayID+" "+accession+" set size="+nodeSet.size());
		for(GONode node : nodeSet) {
			insertCnt++;
			this.insertGOPS.setLong(1, bardAssayID);
			this.insertGOPS.setLong(2, assayID);
			this.insertGOPS.setString(3, accession);
			this.insertGOPS.setString(4, node.getGoAccession());
			this.insertGOPS.setString(5, node.getGoName());
			ontologyType = node.getGoOntologyType();
			if(ontologyType.equals("biological_process"))
				ontologyType = "P";
			if(ontologyType.equals("molecular_function"))
				ontologyType = "F";
			if(ontologyType.equals("cellular_component"))
				ontologyType = "C";			
			this.insertGOPS.setString(6, ontologyType);
			this.insertGOPS.setString(7, node.getEvCode());
			this.insertGOPS.setInt(8, node.isImplied() ? 1 : 0);	
			
			this.insertGOPS.addBatch();
			
			if(insertCnt % 10 == 0) {
				insertGOPS.executeBatch();
				insertGOPS.clearBatch();
				conn.commit();
				logger.info("Insert Count = "+insertCnt);
			}			
		}
	}
	
	
	private void insertGODataForProject(long projectID, String accession, Set <GONode> nodeSet) throws SQLException {
		String ontologyType;
//		logger.info(assayID+" "+accession+" set size="+nodeSet.size());
		for(GONode node : nodeSet) {
			insertCnt++;
			this.insertGOPS.setLong(1, projectID);
			this.insertGOPS.setString(2, accession);
			this.insertGOPS.setString(3, node.getGoAccession());
			this.insertGOPS.setString(4, node.getGoName());
			ontologyType = node.getGoOntologyType();
			if(ontologyType.equals("biological_process"))
				ontologyType = "P";
			if(ontologyType.equals("molecular_function"))
				ontologyType = "F";
			if(ontologyType.equals("cellular_component"))
				ontologyType = "C";			
			this.insertGOPS.setString(5, ontologyType);
			this.insertGOPS.setString(6, node.getEvCode());
			this.insertGOPS.setInt(7, node.isImplied() ? 1 : 0);	
			
			this.insertGOPS.addBatch();
			
			if(insertCnt % 10 == 0) {
				insertGOPS.executeBatch();
				insertGOPS.clearBatch();
				conn.commit();
				logger.info("Insert Count = "+insertCnt);
			}			
		}
	}
	
	private void insertGODataForCompound(long cid, String accession, Set <GONode> nodeSet) throws SQLException {
		String ontologyType;
//		logger.info(assayID+" "+accession+" set size="+nodeSet.size());
		for(GONode node : nodeSet) {
			insertCnt++;
			this.insertGOPS.setLong(1, cid);
			this.insertGOPS.setString(2, accession);
			this.insertGOPS.setString(3, node.getGoAccession());
			this.insertGOPS.setString(4, node.getGoName());
			ontologyType = node.getGoOntologyType();
			if(ontologyType.equals("biological_process"))
				ontologyType = "P";
			if(ontologyType.equals("molecular_function"))
				ontologyType = "F";
			if(ontologyType.equals("cellular_component"))
				ontologyType = "C";			
			this.insertGOPS.setString(5, ontologyType);
			this.insertGOPS.setString(6, node.getEvCode());
			this.insertGOPS.setInt(7, node.isImplied() ? 1 : 0);	
			
			this.insertGOPS.addBatch();
			
			if(insertCnt % 10 == 0) {
				insertGOPS.executeBatch();
				insertGOPS.clearBatch();
				conn.commit();
				logger.info("Insert Count = "+insertCnt);
			}			
		}
	}
	
	
	public static void main(String [] args) {
		BardGOEntityLoader loader = new BardGOEntityLoader();
		//loader.loadGOCompound();
		//loader.loadGO();
		loader.loadGOProject();
	}
	
}
