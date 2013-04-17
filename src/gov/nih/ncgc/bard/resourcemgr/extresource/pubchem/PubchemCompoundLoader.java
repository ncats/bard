package gov.nih.ncgc.bard.resourcemgr.extresource.pubchem;

import ftp.FtpBean;
import ftp.FtpException;
import ftp.FtpListResult;
import gov.nih.ncgc.bard.resourcemgr.BardDBUtil;
import gov.nih.ncgc.bard.resourcemgr.BardExtResourceLoader;
import gov.nih.ncgc.bard.resourcemgr.BardExternalResource;
import gov.nih.ncgc.bard.resourcemgr.IBardExtResourceLoader;
import gov.nih.ncgc.util.MolFpFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;
import java.util.Vector;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

import chemaxon.formats.MolFormatException;
import chemaxon.formats.MolImporter;
import chemaxon.struc.Molecule;
import chemaxon.util.MolHandler;

public class PubchemCompoundLoader extends BardExtResourceLoader implements IBardExtResourceLoader {

    static final private Logger logger = 
	    Logger.getLogger(PubchemCompoundLoader.class.getName());

    private String SERVICE_KEY_FULL_LOAD = "COMPOUND-REFRESH-FULL";
    private String SERVICE_KEY_DAILY_LOAD = "COMPOUND-REFRESH-DAILY";
    private String SERVICE_KEY_SPECIFIC_LOAD = "COMPOUND-REFRESH-SPECIFIC";
    
    public final String cidKey = "PUBCHEM_COMPOUND_CID";
    public final String iupacNameKey = "PUBCHEM_IUPAC_NAME";
    public final String pubchemFormulaKey = "PUBCHEM_MOLECULAR_FORMULA";
    public final String pubchemCanSmilesKey = "PUBCHEM_OPENEYE_CAN_SMILES";
    public final String pubchemIsoSmilesKey = "PUBCHEM_OPENEYE_ISO_SMILES";
    public final String pubchemMwKey = "PUBCHEM_MOLECULAR_WEIGHT";
    public final String pubchemMonoisoMwKey = "PUBCHEM_MONOISOTOPIC_WEIGHT";

    //compound property keys and corresponding column types
    private Vector <String> propertyKeys;
    private Vector <String> types;

    //handle batches of 25 files at a time
    private int fileBatchSize = 10;

    //db connection
    private Connection conn;
    //molecule sdf file parser
    private MolImporter mi;

    private String sqlCompoundInsert = "insert into temp_compound (cid, formula, iupac_name, can_smiles, iso_smiles, mw, mw_monoiso)" +
	    "values (?,?,?,?,?,?,?)";
    private String sqlMolfileInsert = "insert into temp_compound_molfile (cid, molfile_mol) values (?,?)";
    private String sqlCompoundPropInsert = "insert into temp_compound_props values (";
    private String sqlCompoundFpInsert = "insert into temp_compound_fp values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

    private String sqlCompoundReplace = "replace into compound (cid, formula, iupac_name, can_smiles, iso_smiles, mw, mw_monoiso)" +
	    "values (?,?,?,?,?,?,?)";
    private String sqlMolfileReplace = "replace into compound_molfile (cid, molfile_mol) values (?,?)";
    private String sqlCompoundPropReplace = "replace into compound_props values (";
    private String sqlCompoundFpReplace = "replace into compound_fp values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

    private String updateCompoundDate = "update compound set creation = ? where cid = ?";

    private String sqlUpdateKilledCID = "update compound set is_killed = true where cid = ?";

    //prepared statements
    private PreparedStatement insertCompoundPS, insertCompoundPropsPS, insertMolfilePS, insertCompoundFpPS;
    private PreparedStatement replaceCompoundPS, replaceCompoundPropsPS, replaceMolfilePS, replaceCompoundFpPS;

    //connection and processing global variables
    private String dbURL, driver, user, pw;
    private MolFpFactory molFpFactory;
    private long molCount;
    private String path;
    private String tempPath;
    
    
    private File file, tempFile;
    private GZIPInputStream zis;
    private String tempFileName;
    private byte [] buffer;
    private int len;
    private FileOutputStream fos;

    private File directory;
    private String [] fileArr;
	
    private Molecule mol;
    private InputStream is;			
    private MolHandler mh; 
    
    private boolean insert = true;
    private boolean insertFP = false;

    private long cid = -1;
    private String type;
    private int [] fpArr;
    private int fpIndex;
    private String val;
	
    public PubchemCompoundLoader() { }


    @Override
    public boolean load() {
	//set local paths, the temp path is where subsets of the data are sent for extraction and loading
	//this enables batching
	path = service.getLocalResPath();
	tempPath = path + "/temp/";
	
	//need to fetch all resources under a given URL
	//resource fetch is pretty specfic for this load so it's handled within the loader
	try {
	    fetchResources();
	} catch (IOException e) {
	    e.printStackTrace();
	} catch (FtpException e) {
	    e.printStackTrace();
	}
	
	//kick off the batch loading process
	this.batchReplaceCompounds();
	
	return false;
    }

    private void fetchResources() throws IOException, FtpException {
	//evaluate the kind of load
	String commandKey = service.getServiceKey();
	FtpBean ftp = new FtpBean(); 

	ArrayList <BardExternalResource> resources = service.getExtResources();	
	if(resources == null || resources.size() < 1)
	    return;

	BardExternalResource resource = resources.get(0);
	String dest = service.getLocalResPath();

	//connect
	ftp.ftpConnect(resource.getResourceServer(), resource.getResourceUserName(), resource.getResourcePassword());

	if(commandKey.contains(SERVICE_KEY_FULL_LOAD)) {
	    //update the whole thing
	    ftp.setDirectory(resource.getResourcePath());
	    FtpListResult list = ftp.getDirectoryContent();		
	    while (list.next() ) {
		ftp.getBinaryFile(list.getName(), dest+"/"+list.getName());
	    }
	} else if(commandKey.contains(SERVICE_KEY_DAILY_LOAD)) {
	    //fetch today's latest resources
	    ftp.setDirectory(resource.getResourcePath());
	    FtpListResult list = ftp.getDirectoryContent();		
	    //collect the data directories
	    ArrayList<String> dateList = new ArrayList<String>();
	    while (list.next()) {
		dateList.add(list.getName());
	    }
	    String [] dateArr = new String[dateList.size()];
	    for(int i = 0; i < dateArr.length; i++) {
		dateArr[i] = dateList.get(i);
	    }
	    Arrays.sort(dateArr);
	    String dateDir = dateArr[dateArr.length-1];
	    ftp.setDirectory(resource.getResourcePath()+"/"+dateDir+"/SDF");
	    list = ftp.getDirectoryContent();		
	    while (list.next() ) {
		ftp.getBinaryFile(list.getName(), dest+"/"+list.getName());
	    }
	} else if(commandKey.contains(SERVICE_KEY_SPECIFIC_LOAD)) {
	    //fetch all resources within the external resource directory
	    ftp.setDirectory(resource.getResourcePath());
	    FtpListResult list = ftp.getDirectoryContent();		
	    while (list.next() ) {
		ftp.getBinaryFile(list.getName(), dest+"/"+list.getName());
	    }
	}	
	ftp.close();
    }

    @Override
    public String getLoadStatusReport() {

	return null;
    }

    /**
     * Loads compound files found at path supplied in constructor
     * and expects .sdf.gz files (sdf files in gzip format).
     */
    public void batchLoadCompounds(String filePath, String dbURL) {

	try {
	    path = filePath;
	    tempPath = path + "/temp_load/";
	    
	    //get connection
	    conn = BardDBUtil.connect(dbURL);
	    conn.setAutoCommit(false);
	    
	    //prepare sql statements
	    prepareSQLInsertStatements();
	    //get compound property column names and types to automate property load
	    getPropertyColumnNamesAndTypes();
	    //need to modify statement based on number of property columns in schema
	    prepareInsertPropertyStatement();
	    //make a vector of file name batches to process
	    Vector <Vector<String>> fileBatches = partitionFiles(path);
	    //set a molecule counter, create an ftp factory
	    this.molCount = 0;
	    molFpFactory = MolFpFactory.getInstance(16, 2, 6);
	    mh = new MolHandler(mol);

	    //iterate over file batches
	    for(Vector<String> files: fileBatches) {
		copyAndUnzipFilesToTemp(files);
		loadCompounds();
		this.deleteFilesInTemp();				
	    }

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
	} catch (ClassNotFoundException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
    }


    /**
     * Loads compound files found at path supplied in constructor
     * and expects .sdf.gz files (sdf files in gzip format).
     */
    public void batchReplaceCompounds() {

	try {
	    //get connection
	    conn = BardDBUtil.connect(service.getDbURL());
	    conn.setAutoCommit(false);
	    //prepare sql statements
	    prepareReplaceSQLInsertStatements();
	    //get compound property column names and types to automate property load
	    getPropertyColumnNamesAndTypes();
	    //need to modify statement based on number of property columns in schema
	    prepareReplacePropertyStatement();
	    //make a vector of file name batches to process
	    Vector <Vector<String>> fileBatches = partitionFiles(path);
	    //set a molecule counter, create an ftp factory
	    this.molCount = 0;
	    molFpFactory = MolFpFactory.getInstance(16, 2, 6);
	    
	    //iterate over file batches
	    for(Vector<String> files: fileBatches) {
		copyAndUnzipFilesToTemp(files);
		insertOrUpdateCompounds();
		this.deleteFilesInTemp();				
	    }

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
	} catch (ClassNotFoundException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
    }



    /**
     * Loads compound files found at path supplied in constructor
     * and expects .sdf.gz files.
     */
    public long batchReplaceCompounds(Properties loaderProps) {

	long newCmpdCnt = -1;
	try {

	    //get connection
	    conn = BardDBUtil.connect();
	    conn.setAutoCommit(false);

	    newCmpdCnt = getCompoundCount();

	    //prepare sql statements
	    prepareReplaceSQLInsertStatements();
	    //get compound property column names and types to automate property load
	    getPropertyColumnNamesAndTypes();
	    //need to modify statement based on number of property columns in schema
	    prepareReplacePropertyStatement();

	    //make a vector of file name batches to process
	    path = loaderProps.getProperty("bard.loader.scratch.dir") + "/Compound";
	    tempPath = path+"/temp/";

	    //make file list partition
	    Vector <Vector<String>> fileBatches = partitionFiles(path);

	    //make temp dir
	    File tempDir = new File(tempPath);
	    tempDir.mkdir();

	    //set a molecule counter, create an ftp factory
	    this.molCount = 0;
	    molFpFactory = MolFpFactory.getInstance(16, 2, 6);

	    //iterate over file batches
	    for(Vector<String> files: fileBatches) {
		copyAndUnzipFilesToTemp(files);
		insertOrUpdateCompounds();
		deleteFilesInTemp();				
	    }

	    logger.info("load complete");
	    conn.commit();

	    //Now update the killed cid
	    updateKilledCIDFlags(
		    loaderProps.getProperty("bard.filepath.pubhchem.compound.killedcids")+
		    "/"+loaderProps.getProperty("bard.filename.pubchem.compound.killedcids"));

	    conn.commit();
	    conn.close();
	    logger.info("closed connection");

	    newCmpdCnt = getCompoundCount() - newCmpdCnt;

	} catch (FileNotFoundException e) {
	    e.printStackTrace();
	} catch (SQLException e) {
	    e.printStackTrace();
	} catch (ClassNotFoundException e) {
	    e.printStackTrace();
	} catch (IOException e) {
	    e.printStackTrace();
	}

	return newCmpdCnt;
    }



    /*
     * Precondition: Connection conn is connected and autocommit is false.
     * @param killedCIDFileName full path to filename
     * @throws IOException
     * @throws SQLException
     */
    private void updateKilledCIDFlags(String killedCIDFilePath) throws IOException, SQLException {
	logger.info("Updating Killed CID Flag in Compound");
	File file = new File(killedCIDFilePath);
	if(file.exists()) {
	    BufferedReader br = new BufferedReader(new FileReader(file));
	    String line = null;			
	    Vector <String> cidList = new Vector<String>();

	    while((line = br.readLine()) != null) {
		line = line.trim();
		if(line != "") {
		    cidList.add(line);
		}
	    }

	    br.close();

	    long cidLong;

	    PreparedStatement ps = conn.prepareStatement(sqlUpdateKilledCID);

	    for(String cid : cidList) {
		try {
		    cidLong = Long.parseLong(cid);
		    ps.setLong(1, cidLong);
		    ps.execute();
		    conn.commit();					
		} catch (NumberFormatException nfe) {
		    nfe.printStackTrace();
		    continue;
		}
	    }
	}
    }


    /**
     * Loads compound files found at path supplied in constructor
     * and expects .sdf.gz files (sdf files in gzip format).
     */
    public void batchLoadCompoundFp() {
	try {
	    //get connection
	    connect();
	    //prepare sql statements
	    prepareSQLInsertStatements();
	    //get compound property column names and types to automate property load
	    getPropertyColumnNamesAndTypes();
	    //need to modify statement based on number of property columns in schema
	    prepareInsertPropertyStatement();
	    //make a vector of file name batches to process
	    Vector <Vector<String>> fileBatches = partitionFiles(path);

	    //iterate over file batches
	    for(Vector<String> files: fileBatches) {				
		copyAndUnzipFilesToTemp(files);
		loadCompoundFp();
		this.deleteFilesInTemp();				
	    }

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
	}
    }

    public long getCompoundCount() {
	long compoundCnt = 0;

	try {
	    Connection connection = BardDBUtil.connect();
	    Statement stmt = connection.createStatement();
	    ResultSet rs = stmt.executeQuery("select count(cid) from compound");
	    if(rs.next()) {
		compoundCnt = rs.getLong(1);
	    }
	    connection.close();
	} catch (ClassNotFoundException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	} catch (SQLException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}

	return compoundCnt;
    }


    private void connect() {
	try {
	    Class.forName(driver);
	    conn = DriverManager.getConnection(dbURL, user, pw);
	    conn.setAutoCommit(false);
	} catch (ClassNotFoundException e) {
	    e.printStackTrace();
	    logger.warning("Error: No driver class found for driver="+driver);
	} catch (SQLException e) {
	    e.printStackTrace();
	}		
    }

    private void prepareSQLInsertStatements() {
	try {
	    insertCompoundPS = conn.prepareStatement(sqlCompoundInsert);
	    insertMolfilePS = conn.prepareStatement(sqlMolfileInsert);
	    insertCompoundFpPS = conn.prepareStatement(sqlCompoundFpInsert);
	} catch (SQLException e) {
	    e.printStackTrace();
	}
    }

    private void prepareReplaceSQLInsertStatements() {
	try {
	    replaceCompoundPS = conn.prepareStatement(sqlCompoundReplace);
	    replaceMolfilePS = conn.prepareStatement(sqlMolfileReplace);
	    replaceCompoundFpPS = conn.prepareStatement(sqlCompoundFpReplace);
	} catch (SQLException e) {
	    e.printStackTrace();
	}
    }

    //populates the property keys and types
    private void getPropertyColumnNamesAndTypes() throws SQLException {
	propertyKeys = new Vector <String> ();
	types = new Vector <String> ();

	DatabaseMetaData metadata = conn.getMetaData();
	ResultSet rs = metadata.getColumns(null,null, "compound_props", null);
	while(rs.next()) {
	    propertyKeys.add(rs.getString("COLUMN_NAME"));
	    types.add(rs.getString("TYPE_NAME"));
	}
	//dump key names
	for(String key: propertyKeys) {
	    logger.info("key ="+key);
	}
	for(String type:types) {
	    logger.info("type ="+type);			
	}
    }

    //augment the property insert with the appropreiate number of fields according to schema	
    //requires that propertyKeys is populated
    private void prepareInsertPropertyStatement() throws SQLException {
	for(int i = 0; i < propertyKeys.size()-1; i++) {
	    sqlCompoundPropInsert += "?,";
	}
	sqlCompoundPropInsert += "?)";
	insertCompoundPropsPS = conn.prepareStatement(sqlCompoundPropInsert);	
    }


    private void prepareReplacePropertyStatement() throws SQLException {
	for(int i = 0; i < propertyKeys.size()-1; i++) {
	    sqlCompoundPropReplace += "?,";
	}
	sqlCompoundPropReplace += "?)";
	replaceCompoundPropsPS = conn.prepareStatement(sqlCompoundPropReplace);	
    }

    public void loadCompounds() throws SQLException {

	directory = new File(tempPath);

	fileArr = directory.list();

	int fileCount = 1;
	for(String fileName : fileArr) {
	    if(fileName.endsWith(".sdf")) {
		loadCompound(tempPath+fileName);
		fileCount++;
	    }
	    logger.info("loaded file "+fileCount+"  name= "+fileName);
	}
    }

    private void insertOrUpdateCompounds() {

	File directory = new File(tempPath);

	String [] files = directory.list();

	int fileCount = 1;
	for(String fileName : files) {
	    if(fileName.endsWith(".sdf")) {
		insertOrUpdateCompound(tempPath+"/"+fileName);
		fileCount++;
	    }
	    logger.info("loaded file "+fileCount+"  name= "+fileName);
	}
    }

    public void loadCompoundFp() throws SQLException {
	File directory = new File(tempPath);

	String [] files = directory.list();
	
	int fileCount = 1;
	for(String fileName : files) {
	    if(fileName.endsWith(".sdf")) {
		loadCompoundFp(tempPath+fileName);
		fileCount++;
	    }
	    logger.info("loaded file "+fileCount+"  name= "+fileName);
	}
    }


    private void loadCompoundFp(String filePath) {

	InputStream is;
	try {
	    is = new FileInputStream(filePath);

	    MolImporter mi = new MolImporter (is);
	    String cidStr;
	    long cid;
	    int fpIndex;
	    int [] fpArr;
	    for (Molecule mol = new Molecule (); mi.read(mol); ) {	

		cidStr = mol.getProperty("PUBCHEM_COMPOUND_CID");

		if(cidStr != null) {
		    cid = Long.parseLong(cidStr);
		    this.insertCompoundFpPS.setLong(1, cid);
		    MolHandler mh = new MolHandler(mol);
		    mh.aromatize(); //aromatize BEFORE fingerprinting
		    fpArr = mh.generateFingerprintInInts(16,2,6);

		    fpIndex = 2;
		    for(int fp : fpArr) {
			this.insertCompoundFpPS.setLong(fpIndex, convertToUnsignedInt(fp));
			fpIndex++;
		    }
		}

		insertCompoundFpPS.execute();

		molCount++;
		if(molCount % 1000 == 0) {
		    conn.commit();
		}		
	    }

	    is.close();

	} catch (FileNotFoundException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	} catch (SQLException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	} catch (NumberFormatException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	} catch (IOException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
    }


    private void loadCompound(String filePath) {

	insert = true;
	insertFP = false;
	cid = -1;

	try {

	    is = new FileInputStream(filePath);			
	    mi = new MolImporter (is);
	    
	    for (mol = new Molecule (); mi.read(mol); ) {	
		molCount++;
		fpArr = null;
		insertFP = false;
		int keyIndex = 1;
		for(String key : propertyKeys) {
		    type = types.get(keyIndex-1);
		    try {

			val = null;
			val = mol.getProperty(key);

			if(type.equalsIgnoreCase("varchar")) {
			    if(mol.getProperty(key) != null) {
				this.insertCompoundPropsPS.setString(keyIndex, mol.getProperty(key));
			    } else {
				this.insertCompoundPropsPS.setNull(keyIndex, java.sql.Types.VARCHAR);
			    }
			} else if(type.equalsIgnoreCase("int")) {
			    if(mol.getProperty(key) != null) {
				this.insertCompoundPropsPS.setInt(keyIndex, Integer.parseInt(mol.getProperty(key)));
			    } else {
				this.insertCompoundPropsPS.setNull(keyIndex, java.sql.Types.INTEGER);
			    }			
			} else if(type.equalsIgnoreCase("bigint")) {
			    if(mol.getProperty(key) != null) {
				this.insertCompoundPropsPS.setLong(keyIndex, Long.parseLong(mol.getProperty(key)));
			    } else { 
				this.insertCompoundPropsPS.setNull(keyIndex, java.sql.Types.BIGINT);
			    }							
			} else if(type.equalsIgnoreCase("float")) {
			    if(mol.getProperty(key) != null) {
				this.insertCompoundPropsPS.setFloat(keyIndex, Float.parseFloat(mol.getProperty(key)));
			    } else {
				this.insertCompoundPropsPS.setNull(keyIndex, java.sql.Types.FLOAT);
			    }							
			}  else if(type.equalsIgnoreCase("text")) {
			    if(mol.getProperty(key) != null) {
				this.insertCompoundPropsPS.setString(keyIndex, mol.getProperty(key));
			    } else {
				this.insertCompoundPropsPS.setNull(keyIndex, java.sql.Types.VARCHAR);
			    }							
			}
		    } catch (Exception e) {
			System.out.println("key numberFormatException: "+key);
			e.printStackTrace();	
			keyIndex++;
			continue;
		    }

		    keyIndex++;
		}		

		//now take care of compound
		cid = -1;
		//cid
		if(mol.getProperty(cidKey) != null) {
		    insert = true;
		    insertCompoundPS.setLong(1, Long.parseLong(mol.getProperty(cidKey)));
		    cid = Long.parseLong(mol.getProperty(cidKey));
		} else {
		    insert = false;  //don't enter if there's no clear cid
		    logger.warning("Entry didn't load, no CID, filepath: "+filePath);
		}
		//formula	
		insertCompoundPS.setString(2, mol.getFormula());
		//IUPAC name
		insertCompoundPS.setString(3, mol.getProperty(this.iupacNameKey));			
		//can smiles	
		insertCompoundPS.setString(4, mol.getProperty(this.pubchemCanSmilesKey));												
		//iso smiles
		insertCompoundPS.setString(5, mol.getProperty(this.pubchemIsoSmilesKey));
		//mw
		if(mol.getProperty(this.pubchemMwKey) != null) {
		    insertCompoundPS.setFloat(6, Float.parseFloat(mol.getProperty(this.pubchemMwKey)));
		} else {
		    insertCompoundPS.setNull(6, java.sql.Types.FLOAT);
		}
		//monoisotopic mw
		if(mol.getProperty(this.pubchemMonoisoMwKey) != null) {
		    insertCompoundPS.setFloat(7, Float.parseFloat(mol.getProperty(this.pubchemMonoisoMwKey)));
		} else {
		    insertCompoundPS.setNull(7, java.sql.Types.FLOAT);
		}


		//Insert the compoundFP
		fpArr = molFpFactory.generate(mol); 
		fpIndex = 2;
		
		mh.setMolecule(mol); 
		mh.aromatize(); //aromatize BEFORE fingerprinting
		fpArr = mh.generateFingerprintInInts(16,2,6);

		if(fpArr != null && insert) {

		    if(fpArr.length == 16)
			insertFP = true;

		    this.insertCompoundFpPS.setLong(1, cid);

		    for(int fp : fpArr) {
			this.insertCompoundFpPS.setLong(fpIndex, convertToUnsignedInt(fp));
			fpIndex++;
		    }
		}

		if(insert) {
		    //insert the compound
		    insertCompoundPS.addBatch();
		    //insert the compound properties
		    insertCompoundPropsPS.addBatch();
		    //insert the molfile
		    loadMolFile(cid, mol);
		    //insert fp
		    if(insertFP)
			insertCompoundFpPS.addBatch();
		} else {
		    logger.warning("Didn't Insert CID="+cid);
		}

		//check for commit
		if(molCount % 1000 == 0) {
		    insertCompoundPS.executeBatch();
		    insertCompoundPropsPS.executeBatch();
		    insertCompoundFpPS.executeBatch();
		    insertMolfilePS.executeBatch();
		    conn.commit();
		    if(molCount % 100000 == 0) {
			logger.info("Mol Commit Total ="+molCount);
		    }
		}

	    }
	    
	    //execute and commit the end of the file
	    insertCompoundPS.executeBatch();
	    insertCompoundPropsPS.executeBatch();
	    insertCompoundFpPS.executeBatch();
	    insertMolfilePS.executeBatch();
	    conn.commit();

	    is.close();
	    mi.close();
	    is=null;
	    mi=null;

	} catch (MolFormatException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	} catch (IOException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	} catch (SQLException e) {
	    // TODO Auto-generated catch block
	    logger.warning("Didn't insert in tables CID="+cid);
	    e.printStackTrace();
	}
    }

    private void insertOrUpdateCompound(String filePath) {

	boolean insert = true;
	boolean insertFP = false;

	long cid = -1;
	String type;
	int [] fpArr;
	int fpIndex;
	String val;

	try {

	    InputStream is = new FileInputStream(filePath);			
	    MolImporter mi = new MolImporter (is);

	    for (Molecule mol = new Molecule (); mi.read(mol); ) {	
		molCount++;
		fpArr = null;
		insertFP = false;
		int keyIndex = 1;
		for(String key : propertyKeys) {
		    type = types.get(keyIndex-1);
		    try {

			val = null;
			val = mol.getProperty(key);

			if(type.equalsIgnoreCase("varchar")) {
			    if(mol.getProperty(key) != null) {
				this.replaceCompoundPropsPS.setString(keyIndex, mol.getProperty(key));
			    } else {
				this.replaceCompoundPropsPS.setNull(keyIndex, java.sql.Types.VARCHAR);
			    }
			} else if(type.equalsIgnoreCase("int")) {
			    if(mol.getProperty(key) != null) {
				this.replaceCompoundPropsPS.setInt(keyIndex, Integer.parseInt(mol.getProperty(key)));
			    } else {
				this.replaceCompoundPropsPS.setNull(keyIndex, java.sql.Types.INTEGER);
			    }			
			} else if(type.equalsIgnoreCase("bigint")) {
			    if(mol.getProperty(key) != null) {
				this.replaceCompoundPropsPS.setLong(keyIndex, Long.parseLong(mol.getProperty(key)));
			    } else { 
				this.replaceCompoundPropsPS.setNull(keyIndex, java.sql.Types.BIGINT);
			    }							
			} else if(type.equalsIgnoreCase("float")) {
			    if(mol.getProperty(key) != null) {
				this.replaceCompoundPropsPS.setFloat(keyIndex, Float.parseFloat(mol.getProperty(key)));
			    } else {
				this.replaceCompoundPropsPS.setNull(keyIndex, java.sql.Types.FLOAT);
			    }							
			}
			else if(type.equalsIgnoreCase("text")) {
			    if(mol.getProperty(key) != null) {
				this.replaceCompoundPropsPS.setString(keyIndex, mol.getProperty(key));
			    } else {
				this.replaceCompoundPropsPS.setNull(keyIndex, java.sql.Types.VARCHAR);
			    }							
			}
		    } catch (Exception e) {
			System.out.println("key numberFormatException: "+key);
			e.printStackTrace();	
			keyIndex++;
			continue;
		    }

		    keyIndex++;
		}		



		//now take care of compound
		cid = -1;
		//cid
		if(mol.getProperty(cidKey) != null) {
		    insert = true;
		    replaceCompoundPS.setLong(1, Long.parseLong(mol.getProperty(cidKey)));
		    cid = Long.parseLong(mol.getProperty(cidKey));
		} else {
		    insert = false;  //don't enter if there's no clear cid
		    logger.warning("Entry didn't load, no CID, filepath: "+filePath);
		}
		//formula	
		replaceCompoundPS.setString(2, mol.getFormula());
		//IUPAC name
		replaceCompoundPS.setString(3, mol.getProperty(this.iupacNameKey));			
		//can smiles	
		replaceCompoundPS.setString(4, mol.getProperty(this.pubchemCanSmilesKey));												
		//iso smiles
		replaceCompoundPS.setString(5, mol.getProperty(this.pubchemIsoSmilesKey));
		//mw
		if(mol.getProperty(this.pubchemMwKey) != null) {
		    replaceCompoundPS.setFloat(6, Float.parseFloat(mol.getProperty(this.pubchemMwKey)));
		} else {
		    replaceCompoundPS.setNull(6, java.sql.Types.FLOAT);
		}
		//monoisotopic mw
		if(mol.getProperty(this.pubchemMonoisoMwKey) != null) {
		    replaceCompoundPS.setFloat(7, Float.parseFloat(mol.getProperty(this.pubchemMonoisoMwKey)));
		} else {
		    replaceCompoundPS.setNull(7, java.sql.Types.FLOAT);
		}


		//Insert the compoundFP
		fpArr = molFpFactory.generate(mol);
		fpIndex = 2;

		MolHandler mh = new MolHandler(mol);
		mh.aromatize(); //aromatize BEFORE fingerprinting
		fpArr = mh.generateFingerprintInInts(16,2,6);

		if(fpArr != null && insert) {

		    if(fpArr.length == 16)
			insertFP = true;

		    this.replaceCompoundFpPS.setLong(1, cid);

		    for(int fp : fpArr) {
			this.replaceCompoundFpPS.setLong(fpIndex, convertToUnsignedInt(fp));
			fpIndex++;
		    }
		}

		if(insert) {
		    //insert the compound
		    replaceCompoundPS.execute();
		    //insert the compound properties
		    replaceCompoundPropsPS.execute();
		    //insert the molfile
		    insertOrUpdateMolFile(cid, mol);
		    //insert fp
		    if(insertFP)
			replaceCompoundFpPS.execute();
		} else {
		    logger.warning("Didn't Insert CID="+cid);
		}

		//check for commit
		if(molCount % 1000 == 0) {
		    conn.commit();
		    logger.info("Mol Commit Total ="+molCount);
		}

	    }

	    is.close();

	} catch (MolFormatException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	} catch (IOException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	} catch (SQLException e) {
	    // TODO Auto-generated catch block
	    logger.warning("Didn't insert in tables CID="+cid);
	    e.printStackTrace();
	}
    }

    public void copyAndUnzipFilesToTemp(Vector <String> fileNameV) {

	buffer = new byte[1024];
	
	int len;
	
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

//	if(!tempPath.endsWith("temp/")) {
//	    logger.info("Didn't delete files. Wrong DIR");
//	}

	File dir = new File(tempPath);
	String [] fileNames = dir.list();
	File file;
	for(String fileName : fileNames) {
	    file = new File(tempPath+fileName);
	    file.delete();
	}
	logger.info("Deleted Temp Files");
    }


    private void loadMolFile(long cid, Molecule mol) {
	try {
	    insertMolfilePS.setLong(1, cid);
	    insertMolfilePS.setString(2, mol.toFormat("mol"));			
	    insertMolfilePS.addBatch();
	} catch (SQLException e) {
	    e.printStackTrace();
	}
    }

    private void insertOrUpdateMolFile(long cid, Molecule mol) {
	try {
	    replaceMolfilePS.setLong(1, cid);
	    replaceMolfilePS.setString(2, mol.toFormat("mol"));			
	    replaceMolfilePS.execute();
	} catch (SQLException e) {
	    e.printStackTrace();
	}
    }

    public void closeConnections() {
	try {
	    System.out.println("close conn and factory");
	    molFpFactory = null;
	    if(conn != null && !conn.isClosed())
		conn.close();
	} catch (SQLException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
    }

    /*
     * Makes collections of file names to run as loading batches.
     */
    private Vector <Vector<String>> partitionFiles(String path) throws FileNotFoundException {
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

    public void fp() {

	try {
	    //360022
	    String filePath = "C:/Test_Data/compound/test/temp/ChEBI_50252.mol";
	    InputStream is = new FileInputStream(filePath);			
	    mi = new MolImporter (is);

	    MolFpFactory molFpFactory = MolFpFactory.getInstance(16,2,5);

	    int [] fpArr;
	    int [] fpMolArr;

	    int fpIndex;
	    String cidStr;
	    for (Molecule mol = new Molecule (); mi.read(mol); ) {
		MolHandler mh = new MolHandler(mol);
		mh.aromatize();
		System.out.println("finished aromatize");
		fpArr = mh.generateFingerprintInInts(16,2,6);
		for(int i = 0; i < fpArr.length; i++) {
		    long longVal;
		    longVal = (convertToUnsignedInt((fpArr[i])));
		    System.out.print(longVal+"\t");		
		}
	    }
	} catch (Exception e) {

	}
    }

    public static long convertToUnsignedInt(int input) {  
	return input & 0xFFFFFFFFL;  

    } 

    public boolean updateCompoundCreateDate(Properties loaderProps) {
	try {

	    String cidDateFilePath = loaderProps.getProperty("bard.loader.scratch.dir") + "/Compound-Extras/CID-Date.gz";

	    //if it's a gzip, gunzip
	    if(cidDateFilePath.endsWith(".gz")) {
		GZIPInputStream gzis = new GZIPInputStream(new FileInputStream(cidDateFilePath));
		byte [] buffer = new byte[1024];
		cidDateFilePath = cidDateFilePath.replace(".gz", "");
		FileOutputStream fos = new FileOutputStream(cidDateFilePath);
		int len;
		while((len = gzis.read(buffer)) > 0) {
		    fos.write(buffer, 0, len);
		}
		gzis.close();
		fos.close();				
	    }

	    conn = BardDBUtil.connect();
	    conn.setAutoCommit(false);

	    PreparedStatement ps = conn.prepareStatement(updateCompoundDate);
	    BufferedReader bw = new BufferedReader(new FileReader(cidDateFilePath));
	    String line;
	    String [] toks;
	    long updateCnt = 0;
	    while( (line = bw.readLine()) != null) {

		toks = line.split("\t");

		if(toks.length < 2) 
		    continue;

		try  {			
		    ps.setLong(2, Long.parseLong(toks[0]));
		    ps.setDate(1 , Date.valueOf(toks[1]));

		    ps.addBatch();

		    updateCnt++;

		    if(updateCnt % 1000 == 0) {
			ps.executeBatch();
			conn.commit();
		    }

		    if(updateCnt % 100000 == 0) {
			logger.info("Compound Date update cnt="+updateCnt);					
		    }

		} catch (Exception e) {
		    e.printStackTrace();
		    continue;
		}
	    }

	    ps.executeUpdate();
	    conn.commit();
	    conn.close();
	    logger.info("Compound Date update complete.  Update Count="+updateCnt);	
	    return true;
	}catch (SQLException sqle)  {
	    sqle.printStackTrace();
	    return false;
	} catch (IOException e) {
	    e.printStackTrace();
	    return false;
	} catch (ClassNotFoundException e) {
	    e.printStackTrace();
	    return false;
	} 		
    }


    public boolean updateCompoundRank(Properties loaderProps) {
	try {
	    Connection conn = BardDBUtil.connect(loaderProps.getProperty("bard.db.connection.url"),
		    loaderProps.getProperty("bard.db.driver.name"), "bard_manager", "bard_manager");
	    Statement stmt = conn.createStatement();
	    logger.info("Start compound rank update.");
	    stmt.execute("create table if not exists temp_compound_rank like compound_rank");
	    logger.info("Created temp_compound_rank.");		
	    stmt.execute("truncate table temp_compound_rank");
	    stmt.execute("insert into temp_compound_rank select * from compound_rank");
	    logger.info("Inserted compound_rank data into temp_compound_rank. Starting popularity update.");		

	    stmt.execute("insert into temp_compound_rank (cid, popularity) "+ 
		    "select id,@syn_count:=count(id) from synonyms where type = 1 "+
		    "group by id ON DUPLICATE KEY UPDATE popularity=@syn_count");

	    logger.info("Starting sid count rank update.");		

	    stmt.execute("insert into temp_compound_rank (cid, sid_count) "+
		    "select cid, @sid_c := count(sid) from cid_sid " +
		    "group by cid  ON DUPLICATE KEY UPDATE sid_count=@sid_c");

	    logger.info("Finished temp rank update.");		

	    //do a table swap if passes delta
	    BardDBUtil.swapTempTableToProductionIfPassesSizeDelta("temp_compound_rank", "compound_rank", 0.98);

	    conn.close();				
	} catch (SQLException e) {
	    e.printStackTrace();
	    return false;
	} catch (ClassNotFoundException e) {
	    e.printStackTrace();
	    return false;
	}
	return true;
    }

    public void dumpPubchemXLOGP3(String dataDir, String stageFilePath) {

	this.path = dataDir;
	this.tempPath = dataDir+"/temp/";

	try {
	    Vector <Vector <String>>batches = this.partitionFiles(dataDir);
	    File tempDir = new File(this.tempPath);
	    String xlogStr;
	    String cid;
	    PrintWriter pw = new PrintWriter(new FileWriter(stageFilePath));
	    long cidsProcessed = 0;
	    long cidsWithXLogP = 0;

	    for(Vector <String> files : batches) {
		this.copyAndUnzipFilesToTemp(files);

		File [] sdfFiles = tempDir.listFiles();

		for(File file : sdfFiles) {

		    if(!file.getName().endsWith(".sdf"))
			continue;

		    InputStream is = new FileInputStream(file);			
		    MolImporter mi = new MolImporter (is);

		    for (Molecule mol = new Molecule (); mi.read(mol); ) {					

			cidsProcessed++;
			xlogStr = mol.getProperty("PUBCHEM_XLOGP3");
			cid = mol.getProperty("PUBCHEM_COMPOUND_CID");
			if(cid != null && xlogStr != null) {
			    pw.println(cid.trim()+"\t"+xlogStr.trim());
			    cidsWithXLogP++;
			}
			if(cidsProcessed % 100000 == 0) {
			    logger.info("Processed lines = "+cidsProcessed+" cids with xlogP "+cidsWithXLogP);
			}

		    }
		    mi.close();
		    is.close();
		}

		this.deleteFilesInTemp();
	    }
	    pw.flush();
	    pw.close();

	} catch (FileNotFoundException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	} catch (MolFormatException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	} catch (IOException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
    }

    public static void main(String [] args) {

	PubchemCompoundLoader loader = new PubchemCompoundLoader();
	//loader.dumpPubchemXLOGP3("/ifs/prod/braistedjc/db_scripts/pubchem_compound", "/ifs/prod/braistedjc/db_scripts/conf/cid_xlogp3_tmp.txt");

	loader.batchLoadCompounds("/ifs/prod/bard/resource_mgr/bard-scratch/Compound/", "jdbc:mysql://bohr.ncats.nih.gov:3306/bard3?zeroDateTimeBehavior=convertToNull");
	
	////		if(args.length != 3) {
	////			logger.warning("Need paramters: path=<path_to_sdf_files> url=<database_url>  driver=<driver_name>");
	////			System.exit(1);
	////		}
	//
	//		String arg, path, dbURL, driver, user, pw;
	//		path = dbURL = driver = user = pw = null;
	//		pw = "bard_manager";
	//		user = "bard_manager";
	//
	//		driver = "com.mysql.jdbc.Driver";
	//		dbURL = "jdbc:mysql://protein.nhgri.nih.gov:3306/bard2?zeroDateTimeBehavior=convertToNull";
	//		path = "/ifs/prod/braistedjc/db_scripts/pubchem_compound/Date/CID-Date";
	//		
	//		String [] toks;
	//
	////		for(int i = 0; i < args.length; i++) {
	////			arg = args[i];
	////			toks = arg.split("=");
	////			if(toks.length != 2) {
	////				logger.warning("Need paramters: path=<path_to_sdf_files> url=<database_url> driver=<driver_name>");				
	////				logger.warning("Make sure \'=\' is used for each parameter");
	////				System.exit(1);
	////			}
	////			if(toks[0].equals("path")) {
	////				path = toks[1].trim();						
	////			} else if(toks[0].equals("url")) {
	////				dbURL = toks[1].trim();						
	////			} else if(toks[0].equals("driver")) {
	////				driver = toks[1].trim();
	////			}
	////		}
	//		
	////		if(path == null || dbURL == null || driver == null ||user == null || pw == null) {
	////			logger.warning("Have a null param. Need paramters: path=<path_to_sdf_files> url=<database_url> driver=<driver_name>");				
	////			System.exit(1);
	////		}
	//
	//		//dbURL = "jdbc:mysql://protein.nhgri.nih.gov:3306/bard2?zeroDateTimeBehavior=convertToNull"
	//		//driver = "com.mysql.jdbc.Driver"
	//	MLBDCompoundLoader loader = new MLBDCompoundLoader(dbURL, driver, user, pw, path);
	//	//loader.batchReplaceCompounds();
	//loader.updateCompoundCreateDate(path);	
	//MLBDCompoundLoader loader = new MLBDCompoundLoader();
	//loader.fp();
	//MLBDCompoundLoader loader = new MLBDCompoundLoader();
	//	loader.batchLoadCompoundFp();

	System.exit(0);

    }


}
