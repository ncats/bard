package gov.nih.ncgc.bard.resourcemgr.extresource.pubchem;

import gov.nih.ncgc.bard.resourcemgr.BardDBUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Vector;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

import chemaxon.formats.MolFormatException;
import chemaxon.formats.MolImporter;
import chemaxon.struc.Molecule;

/**
 * This class loads or updates the substance table and synonyms table.
 * First time batch loading requires download of all substance SDF file archives from pubmed and use of the full constructor 
 * that specifies db parameters and file path.
 * 
 * The call to batchLoadSubstances() will load the table using the file path specified in the constructor.
 * The process partitions the file names and moves them into a temp directory in batches for extraction and processing.
 * 
 * *****************
 * 
 * The class is also used for automatic updates of the sybstance and synonyms table.
 * Use the default no argument constructor. This assumes the loading is into bard2 and that the files have been fetched from 
 * pubmed ftp and placed in the bard-scratch area.
 * 
 * The call is to batchUpdateSubstance(Properties dbManagerProps)
 * 
 * This option uses the replace syntax to either update if the primary key exists or insert if it's a new primary key entry.
 * 
 * @author braistedjc
 *
 */
public class BardSubstanceLoader {

	static final private Logger logger = 
			Logger.getLogger(MLBDLoadAssay.class.getName());

	private static String sidKey = "PUBCHEM_SUBSTANCE_ID";
	private static String dataSourceNameKey = "PUBCHEM_EXT_DATASOURCE_NAME";
	private static String dataSourceRegidKey = "PUBCHEM_EXT_DATASOURCE_REGID";
	private static String patentIDKey = "PUBCHEM_PATENT_ID";
	private static String substanceURLKey = "PUBCHEM_EXT_SUBSTANCE_URL";
	private static String substanceSynonymKey = "PUBCHEM_SUBSTANCE_SYNONYM";
	
	private Hashtable <String, String> sourceNames;

	//handle batches of 25 files at a time
	private int fileBatchSize = 20;
	
	//db connection
	private Connection conn;
	private Connection connUpdate;
	
	//molecule sdf file parser
	private MolImporter mi;

	//patent id array
	String [] patentArr;
	
	//sql statements these are schema specific, perhpas a constant's class could old static versions of these test fields
	private String insertSubstanceSQL = "insert into substance (sid, dep_regid, source_name, substance_url, patent_ids)" +
			" values (?,?,?,?,?)";
	
	private String insertSynonymsSQL = "insert into synonyms (id, type, syn) values (?,?,?)";

	//sql statements these are schema specific, perhpas a constant's class could old static versions of these test fields
	private String replaceSubstanceSQL = "replace into substance (sid, dep_regid, source_name, substance_url, patent_ids)" +
			" values (?,?,?,?,?)";

	private String replaceSynonymsSQL = "replace into synonyms (id, type, syn) values (?,?,?)";

	private String checkSynonymExistsSQL = "select id from synonyms where type = ? and id = ? and syn = ?";
	
	//prepared statements
	private PreparedStatement insertSubstancePS, insertSynonymsPS, replaceSubstancePS, replaceSynonymsPS, checkSynonymExistsPS;

	private String getSubstanceCountSQL = "select count(sid) from substance";

	//connection and processing global variables
	private String dbURL, driver, user, pw;
	private long molCount, synonymCount;
	private String path;
	private String tempPath;

	public BardSubstanceLoader() {	}

	public BardSubstanceLoader(String dbURL, String driver, String user, String pw, String path) {	
		this.dbURL = dbURL;
		this.driver = driver;
		this.user = user;
		this.pw = pw;
		this.path = path;
		this.tempPath = path+"/temp/";
		this.molCount = 0;
		synonymCount = 0;
	}

	
	
	public long batchUpdateSubstances(Properties dbManagerProps) {

		long substanceCount = 0; 
		long substanceUpdateCount = 0;
		try {
			
			path = dbManagerProps.getProperty("bard.loader.scratch.dir");
			
			parseSourceNames(path+"/Substance-Extras/Source-Names");
			
			//get connection
			conn = BardDBUtil.connect();
			conn.setAutoCommit(false);

			substanceCount = getSubstanceCount();
			//prepare sql statements update/replace statement and exists query
			prepareSQLUpdateStatements();
			//make a vector of file name batches to process
			Vector <Vector<String>> fileBatches = partitionFiles(path+"/Substance/");
			
			//set temp path
			path = path +"/Substance/";
			tempPath = path + "temp/";

			//iterate over file batches
			for(Vector<String> files: fileBatches) {
				copyAndUnzipFilesToTemp(files);
				updateSubstances();
				this.deleteFilesInTemp();
			}
			
			//final inserts 
			replaceSubstancePS.executeBatch();
			replaceSynonymsPS.executeBatch();

			logger.info("load complete");
			conn.commit();
			
			//get new size
			substanceUpdateCount = getSubstanceCount() - substanceCount;			
			
			conn.close();
			logger.info("closed connection");
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return substanceUpdateCount;
	}
	

	/**
	 * Loads compound files found at path supplied in constructor
	 * and expects .sdf.gz files (sdf files in gzip format).
	 * @throws IOException 
	 */
	public void batchLoadSubstances() {

		try {
			this.parseSourceNames(path+"/Source-Names");
			//get connection
			connect();
			//prepare sql statements
			prepareSQLInsertStatements();
			//make a vector of file name batches to process
			Vector <Vector<String>> fileBatches = partitionFiles(path);

			//iterate over file batches
			for(Vector<String> files: fileBatches) {
				copyAndUnzipFilesToTemp(files);
				loadSubstances();
				this.deleteFilesInTemp();				
			}
			
			//final inserts 
			insertSubstancePS.executeBatch();
			insertSynonymsPS.executeBatch();

			logger.info("load complete");
			conn.commit();
			conn.close();
			logger.info("closed connection");

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	private void connect() {
		try {
			Class.forName(driver);
			conn = DriverManager.getConnection(dbURL, user, pw);
			conn.setAutoCommit(false);
			connUpdate = DriverManager.getConnection(dbURL, user, pw);
			connUpdate.setAutoCommit(false);
			
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			logger.warning("Error: No driver class found for driver="+driver);
		} catch (SQLException e) {
			e.printStackTrace();
		}		
	}

	private void prepareSQLInsertStatements() {
		try {
			insertSubstancePS = conn.prepareStatement(insertSubstanceSQL);
			insertSynonymsPS = conn.prepareStatement(insertSynonymsSQL);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	private void prepareSQLUpdateStatements() {
		try {
			replaceSubstancePS = conn.prepareStatement(replaceSubstanceSQL);
			replaceSynonymsPS = conn.prepareStatement(replaceSynonymsSQL);
			checkSynonymExistsPS = conn.prepareStatement(checkSynonymExistsSQL);
			insertSynonymsPS = conn.prepareStatement(insertSynonymsSQL);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}



	public void loadSubstances() throws SQLException {

		File directory = new File(tempPath);

		String [] files = directory.list();

		int fileCount = 1;
		for(String fileName : files) {
			if(fileName.endsWith(".sdf")) {
				loadSubstance(tempPath+fileName);
				fileCount++;
			}
			logger.info("loaded file "+fileCount+"  name= "+fileName);
		}
	}


	public void updateSubstances() throws SQLException {

		File directory = new File(tempPath);

		String [] files = directory.list();

		int fileCount = 1;
		for(String fileName : files) {
			if(fileName.endsWith(".sdf")) {
				replaceSubstance(tempPath+fileName);
				fileCount++;
			}
			logger.info("loaded file "+fileCount+"  name= "+fileName);
		}
	}

	
	private void loadSubstance(String filePath) {

		String sid = null;
		String sourceName;
		String displaySourceName;
		String regID;
		String patentID;
		String substanceURL;
		String substanceSynonym;
		
		try {

			InputStream is = new FileInputStream(filePath);			
			MolImporter mi = new MolImporter (is);

			for (Molecule mol = new Molecule (); mi.read(mol); ) {	
				molCount++;
				sid = sourceName = regID = patentID = substanceURL = displaySourceName = substanceSynonym = null;
				
				sid = mol.getProperty(sidKey).trim();
				sourceName = mol.getProperty(dataSourceNameKey);
				regID = mol.getProperty(dataSourceRegidKey);
				patentID = mol.getProperty(patentIDKey);
				substanceURL = mol.getProperty(substanceURLKey);
				
				if(sid != null) {
					this.insertSubstancePS.setLong(1, Long.parseLong(sid));
				} else {
					//move to next substance
					continue;
				}

				if(regID != null) {
					this.insertSubstancePS.setString(2, regID);
				} else {
					this.insertSubstancePS.setNull(2, java.sql.Types.VARCHAR);
				}
				
				if(sourceName != null) {
					//check for name mapping
					displaySourceName = this.sourceNames.get(sourceName);				
					if(displaySourceName != null)
						this.insertSubstancePS.setString(3, displaySourceName);
					else
						this.insertSubstancePS.setString(3, sourceName);
				} else {
					this.insertSubstancePS.setNull(3, java.sql.Types.VARCHAR);
				}


				if(substanceURL != null) {
					this.insertSubstancePS.setString(4, substanceURL);
				} else {
					this.insertSubstancePS.setNull(4, java.sql.Types.VARCHAR);
				}
				
				if(patentID != null) {
					
					this.insertSubstancePS.setString(5, patentID);
				} else {
					this.insertSubstancePS.setNull(5, java.sql.Types.VARCHAR);
				}
				
				
				//add the batch
				insertSubstancePS.addBatch();
				
				//now handle synonyms
				substanceSynonym = mol.getProperty(substanceSynonymKey);
				if(sid != null && substanceSynonym != null) {
					insertSynonyms(Long.parseLong(sid), substanceSynonym);
				}

				//execute on every 100
				if(molCount % 100 == 0) {
					insertSubstancePS.executeBatch();
				}
				
				//commit on every 1000
				if(molCount % 1000 == 0) {	
					conn.commit();
					logger.info("Subst Commit Total ="+molCount);
				}
			}
			conn.commit();
			is.close();

		} catch (MolFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void replaceSubstance(String filePath) {

		String sid = null;
		String sourceName;
		String displaySourceName;
		String regID;
		String patentID;
		String substanceURL;
		String substanceSynonym;
		
		try {

			InputStream is = new FileInputStream(filePath);			
			MolImporter mi = new MolImporter (is);

			for (Molecule mol = new Molecule (); mi.read(mol); ) {	
				molCount++;
				sid = sourceName = regID = patentID = substanceURL = displaySourceName = substanceSynonym = null;
				
				sid = mol.getProperty(this.sidKey).trim();
				sourceName = mol.getProperty(this.dataSourceNameKey);
				regID = mol.getProperty(this.dataSourceRegidKey);
				patentID = mol.getProperty(this.patentIDKey);
				substanceURL = mol.getProperty(this.substanceURLKey);
				
				if(sid != null) {
					replaceSubstancePS.setLong(1, Long.parseLong(sid));
				} else {
					//move to next substance
					continue;
				}

				if(regID != null) {
					replaceSubstancePS.setString(2, regID);
				} else {
					replaceSubstancePS.setNull(2, java.sql.Types.VARCHAR);
				}
				
				if(sourceName != null) {
					//check for name mapping
					displaySourceName = this.sourceNames.get(sourceName);				
					if(displaySourceName != null)
						replaceSubstancePS.setString(3, displaySourceName);
					else
						replaceSubstancePS.setString(3, sourceName);
				} else {
					replaceSubstancePS.setNull(3, java.sql.Types.VARCHAR);
				}


				if(substanceURL != null) {
					replaceSubstancePS.setString(4, substanceURL);
				} else {
					replaceSubstancePS.setNull(4, java.sql.Types.VARCHAR);
				}
				
				if(patentID != null) {
					//limit patentID's to 25 in db.
					
					patentArr = patentID.split("\n");

					if(patentArr.length <= 25) {
						replaceSubstancePS.setString(5, patentID);						
					} else {
						patentID = "";
						for (int i = 0; i < 25; i++) {
							patentID += patentArr[i].trim() + "\n";							
						}
						replaceSubstancePS.setString(5, patentID);						
					}
				} else {
					replaceSubstancePS.setNull(5, java.sql.Types.VARCHAR);
				}
				
				
				//add the batch
				replaceSubstancePS.addBatch();
				
				//now handle synonyms
				substanceSynonym = mol.getProperty(substanceSynonymKey);
				if(sid != null && substanceSynonym != null) {
					updateSynonyms(Long.parseLong(sid), substanceSynonym);
				}

				//execute on every 100
				if(molCount % 100 == 0) {
					replaceSubstancePS.executeBatch();
				}
				
				//commit on every 1000
				if(molCount % 1000 == 0) {	
					conn.commit();
					logger.info("Subst Commit Total ="+molCount);
				}
			}
			conn.commit();
			is.close();

		} catch (MolFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	private void insertSynonyms(long sid, String synStr) throws SQLException {
		String [] toks = synStr.split("\n");
		for(String syn : toks) {
			synonymCount++;			
			this.insertSynonymsPS.setLong(1, sid);
			this.insertSynonymsPS.setLong(2, 0);
			this.insertSynonymsPS.setString(3, syn.trim());
			this.insertSynonymsPS.addBatch();
			if(synonymCount % 100 == 0) {
				insertSynonymsPS.executeBatch();
			} 
			if(synonymCount % 1000 == 0) {
				conn.commit();
			}
		}
	}
	
	private void updateSynonyms(long sid, String synStr) throws SQLException {
		String [] toks = synStr.split("\n");
		for(String syn : toks) {
			synonymCount++;			
			this.insertSynonymsPS.setLong(1, sid);
			this.insertSynonymsPS.setLong(2, 0);
			this.insertSynonymsPS.setString(3, syn.trim());
			this.insertSynonymsPS.addBatch();
			if(synonymCount % 100 == 0) {
				insertSynonymsPS.executeBatch();
			} 
			if(synonymCount % 1000 == 0) {
				conn.commit();
			}
		}
	}
	
	/**
	 * Checks the table for the existence of a synonym.
	 * Only use for small updates.
	 * @return
	 * @throws SQLException 
	 */
	public boolean doesSynonymExist(long id, long type, String syn) throws SQLException {
		this.checkSynonymExistsPS.setLong(1, type);
		this.checkSynonymExistsPS.setLong(2, id);
		this.checkSynonymExistsPS.setString(3, syn);
		
		ResultSet rs = this.checkSynonymExistsPS.executeQuery();
		boolean exists = (rs.next());
		rs.close();
		
		return exists;
	}
	
	public void parseSourceNames(String sourceNameFileName) throws IOException {
		sourceNames = new Hashtable <String, String>();
		BufferedReader br = new BufferedReader(new FileReader(sourceNameFileName));
		String line = "";
		String [] toks;
		while((line = br.readLine()) != null) {
			toks = line.split("\t");
			if(toks.length > 1) {
				sourceNames.put(toks[0].trim(), toks[1].trim());
				logger.info(toks[0]+" ==> " + toks[1]);
			}
		}
		br.close();
	}

	private boolean isInteger(String s) {
		char [] cArr = s.toCharArray();
		for(char c : cArr) {
			if(!Character.isDigit(c))
				return false;
		}
		return true;
	}
	
	public void copyAndUnzipFilesToTemp(Vector <String> fileNameV) {
		File file, tempFile;
		GZIPInputStream zis;
		String tempFileName;
		byte [] buffer = new byte[1024];
		int len;
		FileOutputStream fos;
		for(String fileName: fileNameV) {
			System.out.println("file="+fileName);

			file = new File(path+"/"+fileName);
			
			if(!file.isDirectory()) {
				tempFileName = tempPath+fileName;

				tempFileName = tempFileName.substring(0, tempFileName.lastIndexOf(".gz"));
				tempFile = new File(tempFileName);
				try {
					zis = new GZIPInputStream(new FileInputStream(file));
					fos = new FileOutputStream(tempFile);
					while( (len = zis.read(buffer)) > 0 ) {
						fos.write(buffer, 0, len);
					}
					fos.flush();
					fos.close();
					zis.close();


				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	public void deleteFilesInTemp() {
		File dir = new File(tempPath);
		String [] fileNames = dir.list();
		File file;
		for(String fileName : fileNames) {
			file = new File(tempPath+fileName);
			file.delete();
			System.out.println("delete file + "+file.getPath());			
		}
		logger.info("Deleted Temp Files");
	}



	public void closeConnections() {
		try {
			System.out.println("close conn and factory");
			if(conn != null && !conn.isClosed())
				conn.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public Vector <Vector<String>> partitionFiles(String path) throws FileNotFoundException {
		File dir = new File(path);
		String [] files = dir.list();

		Vector <Vector<String>> fileBatches = new Vector <Vector<String>> ();

		Vector <String> fileVector = new Vector <String>();

		int fileCount = 0;

		boolean partialBatch = true;
		for(String fileName : files) {
			if(fileName.endsWith(".gz")) {
				fileVector.add(fileName);			
				fileCount++;
				partialBatch = true;
				if(fileCount % fileBatchSize == 0) {
					fileBatches.add(fileVector);
					fileVector = new Vector <String>();
					partialBatch = false;
				}
			}
		}

		if(partialBatch) {
			fileBatches.add(fileVector);
		}
		return fileBatches;
	}
	
	public void updateSubstanceDates() {
		
		String s1 = "select sid, deposited, updated from temp_subst_dates";
		String s2 = "update substance set deposited = ?, updated = ? where sid = ?";

		connect();
		
		try {
		
			PreparedStatement selectPS = conn.prepareStatement(s1);
			selectPS.setFetchSize(Integer.MIN_VALUE);
			
			PreparedStatement updatePS = connUpdate.prepareStatement(s2);
			//updatePS.setFetchSize(Integer.MIN_VALUE);
			
			ResultSet rs = selectPS.executeQuery();

			long sid;
			Date deposited, updated;
			long updateCount = 0;
			long start = System.currentTimeMillis();
			
			while (rs.next()) {
				updateCount++;
				
				sid = rs.getLong(1);
				deposited = rs.getDate(2);
				updated = rs.getDate(3);

				updatePS.setLong(3, sid);
				updatePS.setDate(1, deposited);
				updatePS.setDate(2, updated);

				updatePS.addBatch();
				
				if(updateCount % 10000 == 0) {
					updatePS.executeBatch();
					conn.commit();
				//	updatePS.clearBatch();
				}
			
				if(updateCount % 100000 == 0) {
					//conn.commit();
					System.out.println("count=" + updateCount + "  " + (System.currentTimeMillis()-start)/1000);
				}
			}
			updatePS.executeUpdate();
			conn.commit();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}

	private long getSubstanceCount() throws SQLException {
		long size = 0;
		Statement stmt = conn.createStatement();
		ResultSet rs = stmt.executeQuery(getSubstanceCountSQL);
		if(rs.next())
			size = rs.getLong(1);
		return size;		
	}
	
	public static void main(String [] args) {

//		if(args.length != 3) {
//			logger.warning("Need paramters: path=<path_to_sdf_files> url=<database_url>  driver=<driver_name>");
//			System.exit(1);
//		}

		String arg, path, dbURL, driver, user, pw;
		path = dbURL = driver = user = pw = null;
		pw = "bard_manager";
		user = "bard_manager";

		driver = "com.mysql.jdbc.Driver";
		dbURL = "jdbc:mysql://protein.nhgri.nih.gov:3306/bard2?zeroDateTimeBehavior=convertToNull";
		path = "/ifs/prod/braistedjc/db_scripts/pubchem_substance";
		
		String [] toks;

//		for(int i = 0; i < args.length; i++) {
//			arg = args[i];
//			toks = arg.split("=");
//			if(toks.length != 2) {
//				logger.warning("Need paramters: path=<path_to_sdf_files> url=<database_url> driver=<driver_name>");				
//				logger.warning("Make sure \'=\' is used for each parameter");
//				System.exit(1);
//			}
//			if(toks[0].equals("path")) {
//				path = toks[1].trim();						
//			} else if(toks[0].equals("url")) {
//				dbURL = toks[1].trim();						
//			} else if(toks[0].equals("driver")) {
//				driver = toks[1].trim();
//			}
//		}
		
//		if(path == null || dbURL == null || driver == null ||user == null || pw == null) {
//			logger.warning("Have a null param. Need paramters: path=<path_to_sdf_files> url=<database_url> driver=<driver_name>");				
//			System.exit(1);
//		}

		//dbURL = "jdbc:mysql://protein.nhgri.nih.gov:3306/bard2?zeroDateTimeBehavior=convertToNull"
		//driver = "com.mysql.jdbc.Driver"
	BardSubstanceLoader loader = new BardSubstanceLoader(dbURL, driver, user, pw, path);
		//loader.batchLoadSubstances();
	//loader.updateSubstanceDates();
		//MLBDSubstanceLoader loader = new MLBDSubstanceLoader();
		//loader.fp();
		//MLBDCompoundLoader loader = new MLBDCompoundLoader();
	//	loader.batchLoadCompoundFp();

		System.exit(0);

	}
}
