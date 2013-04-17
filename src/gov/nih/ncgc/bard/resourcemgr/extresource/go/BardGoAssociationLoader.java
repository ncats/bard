package gov.nih.ncgc.bard.resourcemgr.extresource.go;

import gov.nih.ncgc.bard.resourcemgr.BardDBUtil;
import gov.nih.ncgc.bard.resourcemgr.util.BardResourceFetch;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.logging.Logger;

public class BardGoAssociationLoader {

	private static Logger logger = Logger.getLogger(BardGoAssociationLoader.class.getName());
	
	private String inputFile;
	private String dbURL;
	private String driverName = "com.mysql.jdbc.Driver";
	
	private String sqlInsertGoAssociation = "insert into go_association values (?,?,?,?,?,?,?,?,?,?)";

	private String sqlInsertTempGoAssociation = "insert into temp_go_association values (?,?,?,?,?,?,?,?,?,?)";

	private String sqlCreateTempGoAssocation = "create table if not exists temp_go_association like go_association";
	
	private Connection conn;
	
	private long loadCnt;
	
	public BardGoAssociationLoader() {  }
	
	public BardGoAssociationLoader(String inputFile, String dbURL, String driverStr) {
		this.inputFile = inputFile;
		this.dbURL = dbURL;
		driverName = driverStr;
	}
	
	
	public long load() {

	    long entryCnt = 0;
	    try {
    
		conn = BardDBUtil.connect(this.dbURL, this.driverName);

		BufferedReader br = new BufferedReader(new FileReader(inputFile));
		String line = "";

		PreparedStatement ps = conn.prepareStatement(sqlInsertGoAssociation);
		String [] toks;
		while((line = br.readLine()) != null ) {
		    if(line.startsWith("!"))
			continue;

		    toks = line.split("\t");
		    if(toks.length < 15)
			continue;

		    ps.setInt(1, 0);

		    //source
		    ps.setString(2, toks[0].trim());
		    //accession
		    ps.setString(3, toks[1].trim());
		    //common_name
		    ps.setString(4, toks[9].trim());
		    //taxon
		    toks[12] = toks[12].substring(toks[12].indexOf(":")+1);
		    if(toks[12].contains("|"))
			toks[12] = toks[12].substring(0, toks[12].indexOf('|'));

		    //System.out.println("taxid=**"+toks[12]+"**");
		    ps.setString(5, toks[12].trim());


		    //term
		    ps.setString(6, toks[4].trim());
		    // term type
		    ps.setString(7, toks[8].trim());
		    //evidence
		    ps.setString(8, toks[6].trim());
		    //db_ref
		    ps.setString(9, toks[4].trim());
		    //association date
		    ps.setString(10, toks[13].trim());

		    ps.execute();

		    entryCnt++;				
		}

		ps.close();
		conn.close();


	    } catch (FileNotFoundException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	    } catch (IOException ioe) {
		// TODO Auto-generated catch block
		ioe.printStackTrace();
	    }  catch (SQLException sqle) {
		// TODO Auto-generated catch block
		sqle.printStackTrace();
	    } catch (ClassNotFoundException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	    }

	    return entryCnt;

	}

	private int getTermIdInt(String goTermId) {
	    int val = 0;
	    goTermId = goTermId.trim();
	    if(!goTermId.startsWith("GO:"))
		return -1;		
	    goTermId = goTermId.substring(goTermId.indexOf(':')+1);		
	    return Integer.parseInt(goTermId);
	}

	public long loadTempGoAssociation(Properties dbManagerProps)  {

	    long assocCnt = 0;
	    loadCnt = 0;
	    try {

		conn = BardDBUtil.connect();
		conn.setAutoCommit(false);

		//get current table size
		assocCnt = BardDBUtil.getTableRowCount("go_association");

		//create temp table that is empty
		BardDBUtil.cloneTableStructure("go_association", "temp_go_association");

		//need to load any fetched .gz files
		String baseGoDir = dbManagerProps.getProperty("bard.filepath.go.association.dir");
		File goDir = new File(baseGoDir);
		String [] fileList = goDir.list();
		String decompFileName;

		for(String fileName: fileList) {
		    if(fileName.endsWith(".gz")) {

			decompFileName = fileName.replace(".gz", "");

			logger.info("unzipping file:"+fileName);
			//gunzip
			BardResourceFetch.gunzipFile(baseGoDir+"/"+fileName, baseGoDir+"/"+decompFileName);
			logger.info("Loading go association file:"+decompFileName);
			//process file
			loadTempGoAssociation(baseGoDir+"/"+decompFileName);	
			logger.info("Finished load:"+decompFileName);

		    }
		}

		//get final count in temp table compared to go_association
		assocCnt = BardDBUtil.getTableRowCount("temp_go_association") - assocCnt;

		//swap tables from temp to production and back
		BardDBUtil.swapTempTableToProductionIfPassesSizeDelta("temp_go_association", "go_association", 0.99);

		conn.close();

	    } catch (SQLException e) {
		e.printStackTrace();
	    } catch (ClassNotFoundException e) {
		e.printStackTrace();
	    } catch (FileNotFoundException e) {
		e.printStackTrace();
	    } catch (IOException e) {
		e.printStackTrace();
	    }

	    return assocCnt;
	}


	public boolean updateGoTermTables(Properties props) {
	    boolean updated = true;
	    String goTermPath = props.getProperty("bard.filepath.go.termdb.dir");
	    String goTermTarGZIP = props.getProperty("bard.filename.go.termdb.targzip");
	    String goTermTar = goTermTarGZIP.replace(".gz", "");
	    try {
		//have the files, gunzip and untar
		BardResourceFetch.gunzipFile(goTermPath+"/"+goTermTarGZIP, goTermPath+"/"+goTermTar);
		BardResourceFetch.untarFile(goTermPath+"/"+goTermTar);

		//get the single directory present
		File dir = new File(goTermPath);
		String [] files = dir.list();
		String contentDir = "";
		for(String fileName : files) {
		    if(!fileName.endsWith(".gz") && !fileName.endsWith(".tar"))
			contentDir = fileName;
		}

		contentDir = goTermPath + "/" + contentDir;

		Connection conn = BardDBUtil.connect();
		Statement stmt = conn.createStatement();
		stmt.execute("create table if not exists term like go_term");
		stmt.execute("truncate table term");
		stmt.execute("load data infile \'"+contentDir+"/"+props.getProperty("bard.filename.go.term.txt")+"\' into table term");

		stmt.execute("create table if not exists term2term like go_term2term");
		stmt.execute("truncate table term2term");
		stmt.execute("load data infile \'"+contentDir+"/"+props.getProperty("bard.filename.go.term2term.txt")+"\' into table term2term");

		//swap temp and productin if the size is > 98% of previous, some terms may be lost but not many.
		BardDBUtil.swapTempTableToProductionIfPassesSizeDelta("term", "go_term", 0.98);
		BardDBUtil.swapTempTableToProductionIfPassesSizeDelta("term2term", "go_term2term", 0.98);

		conn.close();

	    } catch (FileNotFoundException e) {
		e.printStackTrace();
		return false;
	    } catch (IOException e) {
		e.printStackTrace();
		return false;
	    } catch (SQLException e) {
		e.printStackTrace();
		return false;
	    } catch (ClassNotFoundException e) {
		e.printStackTrace();
		return false;
	    }		
	    return updated;
	}



	private long loadTempGoAssociation(String file) {
	    long entryCnt = 0;
	    try {

		inputFile = file;

		BufferedReader br = new BufferedReader(new FileReader(inputFile));
		String line = "";

		PreparedStatement ps = conn.prepareStatement(sqlInsertTempGoAssociation);

		String [] toks;
		while((line = br.readLine()) != null ) {
		    if(line.startsWith("!"))
			continue;

		    toks = line.split("\t");
		    if(toks.length < 15)
			continue;

		    ps.setInt(1, 0);

		    //source
		    ps.setString(2, toks[0].trim());
		    //accession
		    ps.setString(3, toks[1].trim());
		    //common_name
		    ps.setString(4, toks[9].trim());
		    //taxon
		    toks[12] = toks[12].substring(toks[12].indexOf(":")+1);
		    if(toks[12].contains("|"))
			toks[12] = toks[12].substring(0, toks[12].indexOf('|'));

		    //System.out.println("taxid=**"+toks[12]+"**");
		    ps.setString(5, toks[12].trim());


		    //term
		    ps.setString(6, toks[4].trim());
		    // term type
		    ps.setString(7, toks[8].trim());
		    //evidence
		    ps.setString(8, toks[6].trim());
		    //db_ref
		    ps.setString(9, toks[5].trim());
		    //association date
		    ps.setString(10, toks[13].trim());

		    ps.addBatch();
		    loadCnt++;
		    if(loadCnt % 1000 == 0) {
			ps.executeBatch();
			conn.commit();
			logger.info("load count="+loadCnt);
		    }
		    entryCnt++;				
		}
		//finish current job
		ps.executeBatch();
		conn.commit();
		ps.close();

	    } catch (FileNotFoundException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	    } catch (IOException ioe) {
		// TODO Auto-generated catch block
		ioe.printStackTrace();
	    }  catch (SQLException sqle) {
		// TODO Auto-generated catch block
		sqle.printStackTrace();
	    }

	    return entryCnt;
	}


	public static void main(String [] args) {

	    //file, url, driver 
	    BardGoAssociationLoader gal = new BardGoAssociationLoader(args[0], args[1], args[2]);

	    System.out.println("load cnt="+gal.load());

	}
}
