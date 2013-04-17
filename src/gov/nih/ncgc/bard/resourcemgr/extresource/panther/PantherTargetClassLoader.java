package gov.nih.ncgc.bard.resourcemgr.extresource.panther;

import gov.nih.ncgc.bard.resourcemgr.BardDBUtil;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Properties;
import java.util.logging.Logger;

public class PantherTargetClassLoader {

    Logger logger = Logger.getLogger(PantherTargetClassLoader.class.getName());
    private Connection conn;
    
    
    
    public boolean loadPantherClassInfo(Properties loaderProps) throws ClassNotFoundException, SQLException, IOException {
	
	boolean loaded = false;
	
	conn = BardDBUtil.connect(
		loaderProps.getProperty("bard.db.connection.url"), 
		loaderProps.getProperty("bard.db.driver.name"),
		loaderProps.getProperty("bard.db.user"),
		loaderProps.getProperty("bard.db.pw")
		);
	
	String classFilePath = loaderProps.getProperty("bard.filepath.panther.proteinclass");

	PreparedStatement ps = conn.prepareStatement("replace into panther_class " +
			"values (?,?,?,?,?,?)");
	
	    BufferedReader br = new BufferedReader(new FileReader(classFilePath));
	    String line;
	    String [] toks;
	    String acc, pClass;
	
	    while((line = br.readLine()) != null) {
		toks = line.split("\t");
		if(toks.length > 3) {
		    ps.setString(1, toks[0].trim());
		    ps.setString(2, toks[1].trim());
		    ps.setInt(3, (getDepth(toks[1])));
		    ps.setString(4, toks[2]);
		    ps.setString(5, toks[3]);
		    ps.setNull(6, java.sql.Types.TIMESTAMP);
		    ps.execute();
		}
	    }
	
	conn.close();
	
	return loaded;
    }
    
    public int getDepth(String nodeLoc) {
	int index = nodeLoc.indexOf("00");
	if(index == -1)
	    return 4;
	else 
	    return (index/3);
    }
    
    public boolean loadPantherDBResouces(Properties loaderProps) throws ClassNotFoundException, SQLException, IOException {
	boolean loaded = false;

	conn = BardDBUtil.connect(
		loaderProps.getProperty("bard.db.connection.url"), 
		loaderProps.getProperty("bard.db.driver.name"),
		loaderProps.getProperty("bard.db.user"),
		loaderProps.getProperty("bard.db.pw")
		);
	
	conn.setAutoCommit(true);
	
	PreparedStatement ps = conn.prepareStatement("insert into panther_uniprot_map " +
			"values (?, ?, ?, ?)");
	
	String panterTermFilePath = loaderProps.getProperty("bard.filepath.panther.terms");	
	
	String humanMappingPath = loaderProps.getProperty("bard.filepath.panther.protein2class.human");
	String mouseMappingPath = loaderProps.getProperty("bard.filepath.panther.protein2class.mouse");
	String ratMappingPath = loaderProps.getProperty("bard.filepath.panther.protein2class.rat");
	
	ArrayList <String> files = new ArrayList<String>();
	ArrayList <Integer> taxonIds = new ArrayList<Integer>();
	
	if(humanMappingPath != null) {
	    files.add(humanMappingPath);
	    taxonIds.add(9606);
	}
	if(mouseMappingPath != null) {
	    files.add(mouseMappingPath);
	    taxonIds.add(10090);
	}
	if(ratMappingPath != null) {
	    files.add(ratMappingPath);
	    taxonIds.add(10116);
	}
	int taxonId;
	int fileCnt = 0;
	for(String file : files) {
	    BufferedReader br = new BufferedReader(new FileReader(file));
	    String line;
	    String [] toks;
	    String acc, pClass;
	    int uIndex = -1;	    
	    boolean haveAcc, havePClass;
	    Hashtable <String, ArrayList<String>> pantherMap = new Hashtable <String, ArrayList<String>>();
	    ArrayList <String> pantherIds;
	    while((line = br.readLine()) != null) {
		toks = line.split("\t");
		haveAcc = false;
		havePClass = false;
		if(toks.length > 3) {
		    acc = toks[0];
		    uIndex = acc.indexOf("UniProtKB");
		    if(uIndex > -1) {
			acc = acc.substring(uIndex);
			acc = acc.split("=")[1].trim();		    
			if(acc.length()>0)
			    haveAcc = true;
		    }

		    if(haveAcc && toks.length > 8) {
			
			pClass = toks[8];
			if(pClass.length() > 0) {
			    
			    toks = pClass.split(";");
			    
			    for(String t : toks) {
				pClass = t.split("#")[1];
				pantherIds = pantherMap.get(acc);
				    if(pantherIds == null) {
					pantherIds = new ArrayList<String>();
					pantherIds.add(pClass);
					pantherMap.put(acc, pantherIds);
				    } else {
					pantherIds.add(pClass);
				    }
			    }			    
			    
			    
			}
		    }
		}	    
	    }
	    
	    taxonId = taxonIds.get(fileCnt);
	    ArrayList <String> pClassIds;
	    for(String accKey : pantherMap.keySet()) {
		
		for(String pclass : pantherMap.get(accKey)) {
		 //   System.out.println(accKey + " " + pclass);
		    ps.setString(1, accKey);
		    ps.setInt(2, taxonId);
		    ps.setString(3, pclass);
		    ps.setNull(4, java.sql.Types.TIMESTAMP);
		    ps.execute();
		}		
	    }
	    
	    fileCnt++;
	}
	
	conn.close();
	
	return loaded;
    }
    
    public void testParse(String humanMappingPath) throws IOException {
	BufferedReader br = new BufferedReader(new FileReader(humanMappingPath));
	String line;
	String [] toks;
	String acc, pClass;
	int uIndex = -1;
	boolean haveAcc, havePClass;
	Hashtable <String, ArrayList<String>> pantherMap = new Hashtable <String, ArrayList<String>>();
	ArrayList <String> pantherIds;
	int lineCnt = 1;
	while((line = br.readLine()) != null) {
	    System.out.println("process cnt"+lineCnt++);
	    
	    toks = line.split("\t");
	    

	    
	    haveAcc = false;
	    havePClass = false;
	    if(toks.length > 3) {


		System.out.println("process cnt="+lineCnt++ + " tok cnt=" + toks.length);

		acc = toks[0];
		uIndex = acc.indexOf("UniProtKB");
		if(uIndex > -1) {
		    acc = acc.substring(uIndex);
		    acc = acc.split("=")[1].trim();		    
		    if(acc.length()>0)
			haveAcc = true;
	//	    System.out.println("acc ="+acc);
		}
		
		if(haveAcc) {
		    pClass = toks[2].split(":")[0].trim();
		    if(pClass.length() > 0) {
			System.out.println("acc="+ acc + " class="+pClass);
			pantherIds = pantherMap.get(acc);
			if(pantherIds == null) {
			    pantherIds = new ArrayList<String>();
			    pantherIds.add(pClass);
			    pantherMap.put(acc, pantherIds);
			} else {
			    pantherIds.add(pClass);
			}
		    }
		}
	    }	    
	}
	
	for(String accKey : pantherMap.keySet()) {
	    for(String pclass : pantherMap.get(accKey)) {
		System.out.println(accKey + " "+pclass);
	    }
	}
	
    }
    
    public static void main(String [] args) {
	PantherTargetClassLoader loader = new PantherTargetClassLoader();
	try {
	    
	    Properties loaderProps = new Properties();

	    loaderProps.setProperty("bard.db.connection.url", "jdbc:mysql://protein.nhgri.nih.gov:3306/bard3?zeroDateTimeBehavior=convertToNull");
	    loaderProps.setProperty("bard.db.driver.name", "com.mysql.jdbc.Driver");
	    loaderProps.setProperty("bard.db.user", "bard_manager");
	    loaderProps.setProperty("bard.db.pw", "bard_manager");

//	    loaderProps.setProperty("bard.filepath.panther.protein2class.human", "C:/Users/braistedjc/Desktop/PTHR8.0_human");
//	    loaderProps.setProperty("bard.filepath.panther.protein2class.mouse", "C:/Users/braistedjc/Desktop/PTHR8.0_mouse");
//	    loaderProps.setProperty("bard.filepath.panther.protein2class.rat", "C:/Users/braistedjc/Desktop/PTHR8.0_rat");

	    loaderProps.setProperty("bard.filepath.panther.protein2class.human", "/ifs/prod/bard/resource_mgr/bard-scratch/Panther/PTHR8.0_human");
	    loaderProps.setProperty("bard.filepath.panther.protein2class.mouse", "/ifs/prod/bard/resource_mgr/bard-scratch/Panther/PTHR8.0_mouse");
	    loaderProps.setProperty("bard.filepath.panther.protein2class.rat", "/ifs/prod/bard/resource_mgr/bard-scratch/Panther/PTHR8.0_rat");

	    
	   // loader.loadPantherDBResouces(loaderProps);

	   // loader.testParse("C:/Users/braistedjc/Desktop/PTHR8.0_human");

	    loaderProps.setProperty("bard.filepath.panther.proteinclass", "C:/Users/braistedjc/Desktop/Protein_Class_7.0");
//
	    loader.loadPantherClassInfo(loaderProps);


	} catch (IOException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	} catch (ClassNotFoundException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	} catch (SQLException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
    }
    
    
}
;