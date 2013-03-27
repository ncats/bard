package gov.nih.ncgc.bard.resourcemgr.extresource.pubchem;

import gov.nih.ncgc.bard.resourcemgr.BardDBUtil;
import gov.nih.ncgc.bard.resourcemgr.extresource.BardResourceFetch;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.logging.Logger;

public class BardCompoundPubchemExtrasLoader {

	static final private Logger logger = 
			Logger.getLogger(BardCompoundPubchemExtrasLoader.class.getName());
	
	static final String SQLInsertIntoCIDSID = "insert into cid_sid (cid, sid, rel_type) values (?,?,?)";
	
	static final String SQLInsertIntoTempCIDSID = "insert into temp_cid_sid (cid, sid, rel_type) values (?,?,?)";
	
	static final String SQLReplaceIntoCIDSID = "replace into cid_sid (cid, sid, rel_type) values (?,?,?)";
	
	static final String SQLSelectMatchCIDSIDTYPE = "select cid from cid_sid where rel_type =? and cid = ? and sid = ?";

	static final String SQLInsertCompoundSynonymIntoSynonym = "insert into synonyms (id, type, syn) values (?,?,?)";
	
	static final String SQLCreateTempCIDSID = "create table if not exists temp_cid_sid like cid_sid";
	
	PreparedStatement checkCIDSIDMatchPS;
	
	private Connection conn;
	
	private String databaseUrl;
	private String driverClassName;
	private String user;
	private String pw;
	private String dir = "/ifs/prod/braistedjc/db_scripts/pubchem_CID_SID/CID-SID";

	public BardCompoundPubchemExtrasLoader() { }
	
	public BardCompoundPubchemExtrasLoader(String databaseURL, String driverClassName) {
		this.databaseUrl = databaseURL;
		this.driverClassName = driverClassName;
	}
	
	/**
	 * Deprecated: This is OK for an initial load but not for generalized maintenance.
	 * This loads the sid to cid mappings
	 */	
	public void loadCIDSID() {

		connect(databaseUrl, driverClassName, "bard_manager", "bard_manager");
		
		try {
			
			BufferedReader br = new BufferedReader(new FileReader(dir));
			
			conn.setAutoCommit(false);
			
			//prepare the match PS
			checkCIDSIDMatchPS = conn.prepareStatement(this.SQLSelectMatchCIDSIDTYPE);
			
			PreparedStatement ps = conn.prepareStatement(SQLInsertIntoCIDSID);
			
			String line;
			String [] toks;
			long insertCount = 0;
			
			boolean executed = false;
			
			while((line = br.readLine()) != null) {
				toks = line.split("\t");
				executed = false;
				if(toks.length != 3)
					continue;
				
				ps.setLong(1, Long.parseLong(toks[0]));
				ps.setLong(2, Long.parseLong(toks[1]));
				ps.setInt(3, Integer.parseInt(toks[2]));
				
				ps.addBatch();
				
				insertCount++;
				if(insertCount % 50000 == 0) {
					ps.executeBatch();
					conn.commit();
					executed = true;
					logger.info("Insert Count = "+insertCount);
				}				
			}
			
			
			if(!executed)
				ps.executeBatch();
				
			conn.commit();
			conn.close();
			
			br.close();
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	

	/**
	 * This loads the sid to cid mappings, reports on the number of mappings reviewed.
	 * This uses replace syntax to maintain unique cid, sid, type mappings.
	 * The use of replace syntax requires a unique key on cid, sid, and type.
	 * 
	 */	
	public long updateCIDSID(Properties dbManagerProps) {
		
		long newMapCnt = 0;
		long initCnt = 0;
		
		try {
			
			initCnt = BardDBUtil.getTableRowCount("cid_sid");
			
			conn = BardDBUtil.connect();

			String cidsidGZIPPath = dbManagerProps.getProperty("bard.filepath.pubchem.cidsid.gzip");
			String cidsidPath = cidsidGZIPPath.replace(".gz", "");
			
			BardResourceFetch.gunzipFile(cidsidGZIPPath, cidsidPath);
			
			BufferedReader br = new BufferedReader(new FileReader(cidsidPath));
			
			conn.setAutoCommit(false);
			//prepare the match PS
			checkCIDSIDMatchPS = conn.prepareStatement(this.SQLSelectMatchCIDSIDTYPE);
			
			PreparedStatement ps = conn.prepareStatement(SQLInsertIntoCIDSID);
			
			String line;
			String [] toks;
			long insertCount = 0;
			long sid, cid;
			int type;
			
			boolean executed = false;
			
			while((line = br.readLine()) != null) {
				toks = line.split("\t");
				executed = false;
				if(toks.length != 3)
					continue;
				
				cid = Long.parseLong(toks[0]);
				sid = Long.parseLong(toks[1]);
				type = Integer.parseInt(toks[2]);
				
				//make sure it doesn't exist... costly but alternative is a unique key across all three fields.
				if(!this.haveCIDSIDMapping(type, cid, sid)) {

					ps.setLong(1, cid);
					ps.setLong(2, sid);
					ps.setInt(3, type);

					ps.addBatch();

					insertCount++;

					if(insertCount % 50000 == 0) {
						ps.executeBatch();
						conn.commit();
						executed = true;
						logger.info("Insert Count = "+insertCount);
					}				

				}
			}
			

			//execute any remaining batches
			ps.executeBatch();
				
			conn.commit();
			conn.close();
			
			br.close();
			
			newMapCnt = BardDBUtil.getTableRowCount("cid-sid") - initCnt;
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return newMapCnt;
	}
	
	
	
	
	/**
	 * This loads the sid to cid mappings, reports on the number of mappings reviewed.
	 * This uses replace syntax to maintain unique cid, sid, type mappings.
	 * The use of replace syntax requires a unique key on cid, sid, and type.
	 * 
	 */	
	public long rebuildCIDSID(Properties dbManagerProps) {
		
		long newMapCnt = 0;
		long initCnt = 0;
		
		try {
			
			initCnt = BardDBUtil.getTableRowCount("cid_sid");
			
			conn = BardDBUtil.connect();

			String cidsidGZIPPath = dbManagerProps.getProperty("bard.filepath.pubchem.cidsid.gzip");
			String cidsidPath = cidsidGZIPPath.replace(".gz", "");
			
			BardResourceFetch.gunzipFile(cidsidGZIPPath, cidsidPath);
			
			
			//make the temp_cid_sid
			Statement stmt = conn.createStatement();
			stmt.execute(this.SQLCreateTempCIDSID);
			stmt.execute("truncate table temp_cid_sid");
			
			
			BufferedReader br = new BufferedReader(new FileReader(cidsidPath));
			
			conn.setAutoCommit(false);
			//prepare the match PS

			PreparedStatement ps = conn.prepareStatement(SQLInsertIntoTempCIDSID);

			String line;
			String [] toks;
			long insertCount = 0;
			long sid, cid;
			int type;

			boolean executed = false;

			while((line = br.readLine()) != null) {
				toks = line.split("\t");
				executed = false;
				if(toks.length != 3)
					continue;

				cid = Long.parseLong(toks[0]);
				sid = Long.parseLong(toks[1]);
				type = Integer.parseInt(toks[2]);


				ps.setLong(1, cid);
				ps.setLong(2, sid);
				ps.setInt(3, type);

				ps.addBatch();

				insertCount++;

				if(insertCount % 50000 == 0) {
					ps.executeBatch();
					conn.commit();
					executed = true;
					logger.info("Insert Count = "+insertCount);
				}				

			}



			//execute any remaining batches
			ps.executeBatch();

			conn.commit();
			conn.close();
			
			br.close();
			
			newMapCnt = BardDBUtil.getTableRowCount("temp-cid-sid") - initCnt;
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return newMapCnt;
	}
	
	
	
	/**
	 * This loads the sid to cid mappings, reports on the number of mappings reviewed.
	 * This uses replace syntax to maintain unique cid, sid, type mappings.
	 * The use of replace syntax requires a unique key on cid, sid, and type.
	 * 
	 */	
	public long rebuildCIDSIDViaTempLoad(Properties dbManagerProps) {
		
		long deltaMapCnt = 0;
		long initCnt = 0;
		long newMapCnt = 0;
		try {
			
			initCnt = BardDBUtil.getTableRowCount("cid_sid");
			
			conn = BardDBUtil.connect();

			String cidsidGZIPPath = dbManagerProps.getProperty("bard.filepath.pubchem.cidsid.gzip");
			String cidsidPath = cidsidGZIPPath.replace(".gz", "");
			
			BardResourceFetch.gunzipFile(cidsidGZIPPath, cidsidPath);
						
			//make the temp_cid_sid
			Statement stmt = conn.createStatement();
			stmt.execute(this.SQLCreateTempCIDSID);
			stmt.execute("truncate table temp_cid_sid");
			
			//load the temp table
			stmt.execute("load data infile '"+cidsidPath+"' into table temp_cid_sid (cid, sid, rel_type)");

			newMapCnt = BardDBUtil.getTableRowCount("temp_cid_sid");

			deltaMapCnt = newMapCnt - initCnt;

			//swap tables iff larger or nearly the same size. Allow from some contraction due to deleted substances. 
			BardDBUtil.swapTempTableToProductionIfPassesSizeDelta("temp_cid_sid", "cid_sid", 0.98);
			
			conn.close();

			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return deltaMapCnt;
	}
	
	
	/**
	 * This loads the compound synonyms, reports on the number of synonyms added.
	 */	
	public long updateCompoundSynonyms(Properties dbManagerProps) {
		
		long newMapCnt = 0;
		long initCnt = 0;
		
		try {
			
			initCnt = BardDBUtil.getTableRowCount("synonyms");
			
			conn = BardDBUtil.connect();

			String cidsynGZIPPath = dbManagerProps.getProperty("bard.filepath.pubchem.cidfilteredsynonyms.gzip");
			String cidsynPath = cidsynGZIPPath.replace(".gz", "");
			
			BardResourceFetch.gunzipFile(cidsynGZIPPath, cidsynPath);
			
			BufferedReader br = new BufferedReader(new FileReader(cidsynPath));
			
			conn.setAutoCommit(false);
			
			PreparedStatement ps = conn.prepareStatement(SQLInsertCompoundSynonymIntoSynonym);
			
			String line;
			String [] toks;
			long insertCount = 0;
			long cid;
			int type = 1;
			String syn;
			
			boolean executed = false;
			
			while((line = br.readLine()) != null) {
				toks = line.split("\t");
				executed = false;
				if(toks.length != 3)
					continue;
				
				cid = Long.parseLong(toks[0].trim());
				syn = toks[1].trim();
				
				//make sure it doesn't exist... costly but alternative is a unique key across all three fields.
				if(!this.haveCompoundSynonym(type, cid, syn)) {

					ps.setLong(1, cid);
					ps.setInt(2, type);
					ps.setString(3, syn);

					ps.addBatch();

					insertCount++;

					if(insertCount % 50000 == 0) {
						ps.executeBatch();
						conn.commit();
						executed = true;
						logger.info("Insert Count = "+insertCount);
					}				
				}
			}
			

			//execute any remaining batches
			ps.executeBatch();
				
			conn.commit();
			conn.close();
			
			br.close();
			
			newMapCnt = BardDBUtil.getTableRowCount("synonyms") - initCnt;
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return newMapCnt;
	}
	
	
	
	/**
	 * Precondition that the connection fields is initialized
	 * @param cid
	 * @param sid
	 * @param type
	 * @return
	 * @throws SQLException 
	 */
	private boolean haveCIDSIDMapping(int type, long cid, long sid) throws SQLException {
		boolean match = false;
		checkCIDSIDMatchPS.setInt(1, type);
		checkCIDSIDMatchPS.setLong(2, cid);
		checkCIDSIDMatchPS.setLong(3, sid);
		if(checkCIDSIDMatchPS.executeQuery().next()) {
			match = true;
		}
		return match;
	}

	private boolean haveCompoundSynonym(int type, long cid, String syn) throws SQLException {
		boolean match = false;
		checkCIDSIDMatchPS.setInt(1, type);
		checkCIDSIDMatchPS.setLong(2, cid);
		checkCIDSIDMatchPS.setString(3, syn);
		if(checkCIDSIDMatchPS.executeQuery().next()) {
			match = true;
		}
		return match;
	}
	
	private void connect(String dataBaseUrl, String driverClassName, String user, String pw) {
		try {			
			Class.forName(driverClassName);						
			conn = DriverManager.getConnection(dataBaseUrl, user, pw);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			System.out.println("Error connecting! Exiting.");
			e.printStackTrace();
			System.exit(1);			
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			System.out.println("Can't resolve driver class:"+driverClassName);
			e.printStackTrace();
			System.exit(1);
		}
	}
	

	
	public static void main(String [] args) {
		
		String url = null, driver = null, user = null, pw = null;
		
//		if(args.length != 4) {
//			System.out.println("Error: Parameter list is incorrect. \n" +
//					"Parameters: url=\'<db_url>\' driver=\'<qualified_driver_class_name>\' user=\'<user_name>\' pw=\'<user_password>\' ");
//			System.exit(1);
//		}
//			
//		String [] toks;
//		for(String arg : args) {
//			toks = arg.split("=");
//			if(toks.length != 2) {
//				System.out.println("Error: Parameter list is incorrect. \n" +
//						"Parameters: url=\'<db_url>\' driver=\'<qualified_driver_class_name>\' user=\'<user_name>\' pw=\'<user_password>\' ");
//				System.exit(1);
//			}
//			
//			if(toks[0].equals("url")) {
//				url = toks[1];
//			} else if(toks[0].equals("driver")) {
//				driver = toks[1];
//			} else if(toks[0].equals("user")) {
//				user = toks[1];
//			} else if(toks[0].equals("pw")) {
//				pw = toks[1];
//			}
//		}
//		
//		if(url == null || driver == null || user == null || pw == null) {
//			System.out.println("Error: Missing parameter.  Check keys and values.\n" +
//					"Parameters: url=\'<db_url>\' driver=\'<qualified_driver_class_name>\' user=\'<user_name>\' pw=\'<user_password>\' ");
//			System.exit(1);
//		}
		url = "jdbc:mysql://protein.nhgri.nih.gov:3306/bard2?zeroDateTimeBehavior=convertToNull";
		driver = "com.mysql.jdbc.Driver";
			
		BardCompoundPubchemExtrasLoader r = new BardCompoundPubchemExtrasLoader(url, driver);
		r.loadCIDSID();
	}

}
