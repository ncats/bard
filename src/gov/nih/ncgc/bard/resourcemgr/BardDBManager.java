package gov.nih.ncgc.bard.resourcemgr;

import gov.nih.ncgc.bard.resourcemgr.extresource.go.BardGoAssociationLoader;
import gov.nih.ncgc.bard.resourcemgr.extresource.kegg.BardKeggLoader;
import gov.nih.ncgc.bard.resourcemgr.extresource.pubchem.BardCompoundPubchemExtrasLoader;
import gov.nih.ncgc.bard.resourcemgr.extresource.pubchem.PubchemSubstanceLoader;
import gov.nih.ncgc.bard.resourcemgr.extresource.pubchem.PubchemCompoundLoader;
import gov.nih.ncgc.bard.resourcemgr.extresource.uniprot.BardUniprotProteinTargetLoader;
import gov.nih.ncgc.bard.resourcemgr.precomp.BardCompoundTestStatusUpdater;
import gov.nih.ncgc.bard.resourcemgr.util.BardResourceFetch;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;
import java.util.Vector;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

@Deprecated
public class BardDBManager {

    private static Logger logger = Logger.getLogger(BardDBManager.class.getName());

    private static Properties managerProps;

    private Vector <String> args;

    public static int COMPOUND_DAILY = 100;
    public static int COMPOUND_WEEKLY = 101;
    public static int COMPOUND_MONTHLY = 102;
    public static int SUBSTANCE_DAILY = 200;
    public static int SUBSTANCE_WEEKLY = 201;
    public static int SUBSTANCE_MONTHLY = 202;

    public static String COMPOUND_DAILY_UPDATE_COMMAND = "COMPOUND-DAILY-UPDATE";
    public static String COMPOUND_WEEKLY_UPDATE_COMMAND = "COMPOUND-WEEKLY-UPDATE";
    public static String COMPOUND_MONTHLY_UPDATE_COMMAND = "COMPOUND-MONTHLY-UPDATE";
    public static String COMPOUND_SPECIFIC_FTP_RESOURCE_UPDATE_COMMAND = "COMPOUND-SPECIFIC-FTP-UPDATE";

    public static String COMPOUND_CID_SID_MAPPING_UPDATE_COMMAND = "COMPOUND-CIDSID-MAPPING-UPDATE";
    public static String COMPOUND_CREATE_DATE_UPDATE_COMMAND = "COMPOUND-CREATE-DATE-UPDATE";
    public static String COMPOUND_SYNONYMS_SID_MAPPING_UPDATE_COMMAND = "COMPOUND-SYNONYMS-SID-MAPPING-UPDATE";

    public static String COMPOUND_RANK_UPDATE_COMMAND = "COMPOUND-RANK-UPDATE";

    public static String COMPOUND_TEST_STATUS_UPDATE_COMMAND = "COMPOUND-TEST-STATUS-UPDATE";

    public static String SUBSTANCE_DAILY_UPDATE_COMMAND = "SUBSTANCE-DAILY-UPDATE";
    public static String SUBSTANCE_WEEKLY_UPDATE_COMMAND = "SUBSTANCE-WEEKLY-UPDATE";
    public static String SUBSTANCE_MONTHLY_UPDATE_COMMAND = "SUBSTANCE-MONTHLY-UPDATE";
    public static String SUBSTANCE_SPECIFIC_FTP_RESOURCE_UPDATED_COMMAND = "SUBSTANCE-SPECIFIC-FTP-UPDATE";

    public static String COMPOUND_DATE_FILE_FETCH = "COMPOUND-DATE-FILE-FETCH";

    public static String KEGG_DISEASE_UPDATE_COMMAND = "KEGG-DISEASE-UPDATE";
    public static String KEGG_DISEASE_TO_ASSAY_ANN = "KEGG-DISEASE-TO-ASSAY-ANN";
    public static String UNIPROT_UPDATE_COMMAND = "UNIPROT-PROTEIN-TARGET-UPDATE";

    public static String GO_ASSOCIATION_UPDATE_COMMAND = "GO-ASSOCIATION-UPDATE";
    public static String GO_ONTOLOGY_TERM_UPDATE_COMMAND = "GO-ONTOLOGY-TERM-UPDATE";

    public static String MLPCN_ASSAY_UPDATE_COMMAND = "MLPCN-ASSAY-UPDATE";
    public static String ASSAY_ANNOTATION_UPDATE_FROM_ASSAY_TABLE = "ASSAY-ANNOTATION-TABLE-UPDATED-FROM-ASSAY-TABLE";

    public static String PUBCHEM_ASSAY_UPDATE_FROM_AID_LIST_COMMAND = "UPDATE-PUBCHEM-STAGING-TABLE-FROM-AID-LIST";

    public BardDBManager() { args = new Vector <String>();}

    public static String getProperty(String key) {
	return managerProps.getProperty(key);
    }

    public boolean configure(String confFile) {

	try {
	    managerProps = new Properties();
	    managerProps.load(new FileReader(confFile));
	    logger.addHandler(new FileHandler(managerProps.getProperty("db.manager.log"), true));
	    logger.info("======================================================");
	    logger.info("Bard DB Manager Launch and Configuration is Successful.");
	} catch (FileNotFoundException e) {
	    e.printStackTrace();
	    return false;
	} catch (IOException e) {
	    e.printStackTrace();
	    return false;
	}

	return true;
    }


    public void execute(String command) {
	long processID = System.currentTimeMillis();
	logger.info("DB Manager Excecuting: " + command + " with ProcessID: [" + processID + "]\n"
		+ "Process specific logs will capture process output.");
	long dbLogId;
	boolean haveFiles = false;
	long newCmpdCnt = 0;
	long updateCnt = 0;

	if(command.equals(COMPOUND_MONTHLY_UPDATE_COMMAND)) {
	    //fetch files
	    dbLogId = BardDBUpdateLogger.logStart("Fetch Files for command= "+command);
	    BardResourceFetch fetch = new BardResourceFetch();
	    haveFiles = fetch.fetchLatestCompoundResources(managerProps, COMPOUND_MONTHLY);
	    if(haveFiles) {
		BardDBUpdateLogger.logEnd(dbLogId, 0, "Finished File Fetch");

		//start update
		dbLogId = BardDBUpdateLogger.logStart("Update command= "+command);	
		PubchemCompoundLoader compoundLoader = new PubchemCompoundLoader ();
		newCmpdCnt = compoundLoader.batchReplaceCompounds(managerProps);
		BardDBUpdateLogger.logEnd(dbLogId, 0, "Finished Compound Update.  New Compound Cnt ="+newCmpdCnt);
	    } else {
		logger.info("FAILED to update compound due to file retrieve error.");
	    }
	} else if (command.equals(COMPOUND_WEEKLY_UPDATE_COMMAND)) {
	    //fetch files
	    dbLogId = BardDBUpdateLogger.logStart("Fetch Files for command= "+command);
	    BardResourceFetch fetch = new BardResourceFetch();
	    haveFiles = fetch.fetchLatestCompoundResources(managerProps, COMPOUND_WEEKLY);
	    if(haveFiles) {
		BardDBUpdateLogger.logEnd(dbLogId, 0, "Finished File Fetch");
		PubchemCompoundLoader compoundLoader = new PubchemCompoundLoader ();
		//start update
		dbLogId = BardDBUpdateLogger.logStart("Update command= "+command);	
		newCmpdCnt = compoundLoader.batchReplaceCompounds(managerProps);
		BardDBUpdateLogger.logEnd(dbLogId, 0, "Finished Compound Update.  New Compound Cnt ="+newCmpdCnt);
	    } else {
		logger.info("FAILED to update compound due to file retrieve error.");
	    }
	} else if (command.equals(COMPOUND_DAILY_UPDATE_COMMAND)) {
	    dbLogId = BardDBUpdateLogger.logStart("Fetch Files for command= "+command);
	    //fetch files
	    BardResourceFetch fetch = new BardResourceFetch();
	    haveFiles = fetch.fetchLatestCompoundResources(managerProps, COMPOUND_DAILY);
	    if(haveFiles) {
		BardDBUpdateLogger.logEnd(dbLogId, 0, "Finished File Fetch");

		dbLogId = BardDBUpdateLogger.logStart("Update command= "+command);	
		PubchemCompoundLoader compoundLoader = new PubchemCompoundLoader ();
		newCmpdCnt = compoundLoader.batchReplaceCompounds(managerProps);
		BardDBUpdateLogger.logEnd(dbLogId, 0, "Finished Compound Update.  New Compound Cnt ="+newCmpdCnt);
	    } else {
		logger.info("FAILED to update compound due to file retrieve error.");
	    }
	} else if (command.equals(KEGG_DISEASE_UPDATE_COMMAND)) {
	    dbLogId = BardDBUpdateLogger.logStart("Fetch Files for command= "+command);
	    BardResourceFetch fetch = new BardResourceFetch();
	    haveFiles = fetch.fetchKEGGDiseaseFile(managerProps);
//	    if(haveFiles) {
//		BardDBUpdateLogger.logEnd(dbLogId, 0, "Finished File Fetch for commnad="+command);
//		dbLogId = BardDBUpdateLogger.logStart("Update command= "+command);					
//		BardKeggLoader keggLoader = new BardKeggLoader();
//		updateCnt = keggLoader.loadKeggDisease(managerProps.getProperty("bard.loader.scratch.dir")+"/KEGG-Disease/"+managerProps.getProperty("kegg.disease.filename"));
//		BardDBUpdateLogger.logEnd(dbLogId, 0, "Finished KEGG Disease Update. New gene to disease mapping count ="+updateCnt);
//	    }
	} else if (command.equals(KEGG_DISEASE_TO_ASSAY_ANN)) {
//	    BardAssayAnnotationLoader loader = new BardAssayAnnotationLoader();
//	    loader.loadKEGGDiseaseToTempAssayAnn(false);
	} else if (command.equals(UNIPROT_UPDATE_COMMAND)) {
	    BardResourceFetch fetch = new BardResourceFetch();
	    haveFiles = fetch.fetchUniprotDatFile(managerProps);
	    if(haveFiles) {
		BardUniprotProteinTargetLoader loader = new BardUniprotProteinTargetLoader();
		loader.loadUniprotToProteinTarget(managerProps.getProperty("bard.loader.scratch.dir")+"/Uniprot/"+
			managerProps.getProperty("uniprot.data.file"), 
			managerProps.getProperty("bard.db.connection.url"),
			managerProps.getProperty("bard.db.driver.name"));
	    }
	} else if (command.equals(ASSAY_ANNOTATION_UPDATE_FROM_ASSAY_TABLE)) {
//	    BardAssayAnnotationLoader loader = new BardAssayAnnotationLoader();
//	    loader.updateAssayAnnotationFromAssayFields();
	} else if (command.equals(COMPOUND_CREATE_DATE_UPDATE_COMMAND)) {			
	    dbLogId = BardDBUpdateLogger.logStart("Fetch Files for command= "+command);
	    BardResourceFetch fetch = new BardResourceFetch();
	    haveFiles = fetch.fetchCompoundWeeklyExtraRsources(managerProps);		
	    if(haveFiles) {
		BardDBUpdateLogger.logEnd(dbLogId, 0, "Finished File Fetch (Current Extras)");
		//start update
		dbLogId = BardDBUpdateLogger.logStart("Update command= "+command+" (updating creation date)");					

		PubchemCompoundLoader compoundLoader = new PubchemCompoundLoader();
		compoundLoader.updateCompoundCreateDate(managerProps);
		BardDBUpdateLogger.logEnd(dbLogId, 0, "Finished Create Date Updates");
	    }
	} else if (command.equals(COMPOUND_SPECIFIC_FTP_RESOURCE_UPDATE_COMMAND)) {			
	    if(args.size() > 2) {
		dbLogId = BardDBUpdateLogger.logStart("Fetch Files for command= "+command+" FTP Dir= "+args.get(2));
		BardResourceFetch fetch = new BardResourceFetch();
		//the third argument is the ftp location of the resource
		haveFiles = fetch.fetchSpecificCompoundResources(managerProps, args.get(2));			
		if(haveFiles) {
		    BardDBUpdateLogger.logEnd(dbLogId, 0, "Finished File Fetch= "+args.get(2));
		    //start update
		    dbLogId = BardDBUpdateLogger.logStart("Update command= "+command+" FTP Dir= "+args.get(2));					
		    PubchemCompoundLoader compoundLoader = new PubchemCompoundLoader ();
		    newCmpdCnt = compoundLoader.batchReplaceCompounds(managerProps);
		    BardDBUpdateLogger.logEnd(dbLogId, 0, "Finished Specific Compound Update ("+args.get(2)+" New Compound Cnt ="+newCmpdCnt);
		}				
	    }
	} else if(command.equals(MLPCN_ASSAY_UPDATE_COMMAND)) {
//	    dbLogId = BardDBUpdateLogger.logStart("Fetch MLPCN Assay list for command= "+command);
//	    BardResourceFetch fetch = new BardResourceFetch();
//	    boolean haveGoodAssayList = fetch.fetchMLPCNAssayList(managerProps);
//
//	    //have equal or more assays than previous list, now get the xml files for processing
//	    if(haveGoodAssayList) {
//		BardDBUpdateLogger.logEnd(dbLogId, 0, "Have Assay list update (net increase or equal assay count, final assay count reported during update log).");			
//
//		dbLogId = BardDBUpdateLogger.logStart("Fetch Assay Description ZIP Files for command= "+command);				
//		haveFiles = fetch.fetchAssayDescriptionMetatdataXMLZips(managerProps);
//
//		//if we have the xml files for processing, re-build the mlp_assay staging table
//		if(haveFiles) {
//		    BardDBUpdateLogger.logEnd(dbLogId, 0, "Have Assay Description List Files for command="+command);	
//
//		    dbLogId = BardDBUpdateLogger.logStart("Update Process command= "+command);				
//		    MLBDLoadAssay loader = new MLBDLoadAssay();
//		    updateCnt = loader.updatePubchemAssayTable(managerProps);
//
//		    BardDBUpdateLogger.logEnd(dbLogId, 0, "Updated MLP_ASSAY ="+command+" Update Count = "+updateCnt);							
//
//		} else {
//		    BardDBUpdateLogger.logEnd(dbLogId, 1, "ERROR: MLP_ASSAY Failed Couldn't fetch Assay Description ZIP files.");					
//		}
//	    } else {
//		BardDBUpdateLogger.logEnd(dbLogId, 1, "ERROR: MLP_ASSAY Update Failed Couldn't build mlp assay_id list.");							
//	    }

	} else if (command.equals(PUBCHEM_ASSAY_UPDATE_FROM_AID_LIST_COMMAND)) {
//
//	    BardResourceFetch fetch = new BardResourceFetch();
//
//	    dbLogId = BardDBUpdateLogger.logStart("Fetch Assay Description ZIP Files for command= "+command);				
//	    haveFiles = fetch.fetchAssayDescriptionMetatdataXMLZips(managerProps);
//
//	    //if we have the xml files for processing, re-build the mlp_assay staging table
//	    if(haveFiles) {
//		BardDBUpdateLogger.logEnd(dbLogId, 0, "Have Assay Description List Files for command="+command);	
//
//		dbLogId = BardDBUpdateLogger.logStart("Update Process command= "+command);	
//
//		MLBDLoadAssay loader = new MLBDLoadAssay();
//		updateCnt = loader.updatePubchemAssayTable(managerProps);
//
//		BardDBUpdateLogger.logEnd(dbLogId, 0, "Updated PUBCHEM_ASSAY ="+command+" Update Count = "+updateCnt);							
//
//	    } else {
//		BardDBUpdateLogger.logEnd(dbLogId, 1, "ERROR: PUBCHEM_ASSAY Failed Couldn't fetch Assay Description ZIP files.");					
//	    }
	} else if(command.equals(SUBSTANCE_MONTHLY_UPDATE_COMMAND)) {
	    //fetch files
	    dbLogId = BardDBUpdateLogger.logStart("Fetch Files for command= "+command);
	    BardResourceFetch fetch = new BardResourceFetch();
	    haveFiles = fetch.fetchLatestSubstanceResources(managerProps, SUBSTANCE_MONTHLY);
	    if(haveFiles) {
		BardDBUpdateLogger.logEnd(dbLogId, 0, "Finished File Fetch");

		//start update
		dbLogId = BardDBUpdateLogger.logStart("Update command= "+command);	

		BardDBUpdateLogger.logEnd(dbLogId, 0, "Finished Substance Update.  New Substance Cnt ="+newCmpdCnt);
	    } else {
		logger.info("FAILED to update substance due to file retrieve error.");
	    }
	} else if (command.equals(SUBSTANCE_WEEKLY_UPDATE_COMMAND)) {
	    //fetch files
	    dbLogId = BardDBUpdateLogger.logStart("Fetch Files for command= "+command);
	    BardResourceFetch fetch = new BardResourceFetch();
	    haveFiles = fetch.fetchLatestSubstanceResources(managerProps, SUBSTANCE_WEEKLY);
	    if(haveFiles) {
		BardDBUpdateLogger.logEnd(dbLogId, 0, "Finished File Fetch");

		dbLogId = BardDBUpdateLogger.logStart("Update command= "+command);	

		BardDBUpdateLogger.logEnd(dbLogId, 0, "Finished Substance Update.  New Substance Cnt ="+newCmpdCnt);
	    } else {
		logger.info("FAILED to update substance due to file retrieve error.");
	    }
	} else if (command.equals(SUBSTANCE_DAILY_UPDATE_COMMAND)) {
//	    dbLogId = BardDBUpdateLogger.logStart("Fetch Files for command= "+command);
//	    //fetch files
//	    BardResourceFetch fetch = new BardResourceFetch();
//	    haveFiles = fetch.fetchLatestSubstanceResources(managerProps, SUBSTANCE_DAILY);
//	    if(haveFiles) {
//		BardDBUpdateLogger.logEnd(dbLogId, 0, "Finished File Fetch");
//		dbLogId = BardDBUpdateLogger.logStart("Update command= "+command);	
//		PubchemSubstanceLoader loader = new PubchemSubstanceLoader();
//		newCmpdCnt = loader.batchUpdateSubstances(managerProps);
//		BardDBUpdateLogger.logEnd(dbLogId, 0, "Finished Substance Update.  New Substance Cnt ="+newCmpdCnt);
//	    } else {
//		logger.info("FAILED to update substance due to file retrieve error.");
//	    }
	} else if (command.equals(SUBSTANCE_SPECIFIC_FTP_RESOURCE_UPDATED_COMMAND)) {			
//	    if(args.size() > 2) {
//		dbLogId = BardDBUpdateLogger.logStart("Fetch Files for command= "+command+" FTP Dir= "+args.get(2));
//		BardResourceFetch fetch = new BardResourceFetch();
//		//the third argument is the ftp location of the resource
//		haveFiles = fetch.fetchSpecificSubstanceResources(managerProps, args.get(2));			
//		if(haveFiles) {
//		    BardDBUpdateLogger.logEnd(dbLogId, 0, "Finished File Fetch= "+args.get(2));
//		    //start update
//		    dbLogId = BardDBUpdateLogger.logStart("Update command= "+command+" FTP Dir= "+args.get(2));					
//		    PubchemSubstanceLoader loader = new PubchemSubstanceLoader ();
//		    newCmpdCnt = loader.batchUpdateSubstances(managerProps);
//		    BardDBUpdateLogger.logEnd(dbLogId, 0, "Finished Specific Update ("+args.get(2)+" New Substance Cnt ="+newCmpdCnt);
//		}				
//	    }
	} else if (command.equals(COMPOUND_CID_SID_MAPPING_UPDATE_COMMAND)) {			

	    dbLogId = BardDBUpdateLogger.logStart("Fetch Files for command= "+command);
	    BardResourceFetch fetch = new BardResourceFetch();

	    //this pulls the compound-extras files for the latest week
	    haveFiles = fetch.fetchCompoundWeeklyExtraRsources(managerProps);			

	    if(haveFiles) {
		BardDBUpdateLogger.logEnd(dbLogId, 0, "Finished File Fetch");
		//start update
		dbLogId = BardDBUpdateLogger.logStart("Update command= "+command);					

		BardCompoundPubchemExtrasLoader loader = new BardCompoundPubchemExtrasLoader();
		updateCnt = loader.rebuildCIDSIDViaTempLoad(managerProps);
		BardDBUpdateLogger.logEnd(dbLogId, 0, "Finished CID->SID Update, New Substance Cnt ="+updateCnt);			
	    } else {
		BardDBUpdateLogger.logEnd(dbLogId, 1, "ERROR: FAILED File Fetch for CID-SID mapping update.");
	    }			
	} else if(command.equals(COMPOUND_TEST_STATUS_UPDATE_COMMAND)) {
	    String dbURL = managerProps.getProperty("bard.db.connection.url");
	    dbLogId = BardDBUpdateLogger.logStart("Begin Update Command= "+command, dbURL);
	    BardCompoundTestStatusUpdater compoundTestWorker = new BardCompoundTestStatusUpdater();
	    compoundTestWorker.updateCompoundTestStatus(managerProps);
	    BardDBUpdateLogger.logEnd(dbLogId, 0, "Finished Compound Test Status Update", dbURL);
	} else if (command.equals(COMPOUND_RANK_UPDATE_COMMAND)) {

	    dbLogId = BardDBUpdateLogger.logStart("Begin Update Command= "+command);
	    PubchemCompoundLoader loader = new PubchemCompoundLoader();
	    if(loader.updateCompoundRank(managerProps))
		BardDBUpdateLogger.logEnd(dbLogId,  0, "Finished Compound Rank Update");
	    else
		BardDBUpdateLogger.logEnd(dbLogId,  1, "Compound Rank Update FAILED. Check log for exceptions or error messages.");				

	} else if (command.equals(GO_ASSOCIATION_UPDATE_COMMAND)) {
//
//	    dbLogId = BardDBUpdateLogger.logStart("Fetch Files for command= "+command);
//	    BardResourceFetch fetch = new BardResourceFetch();
//	    haveFiles = fetch.fetchGOHTTPAssociationResources(managerProps);			
//
//	    if(haveFiles) {
//		BardDBUpdateLogger.logEnd(dbLogId, 0, "Finished File Fetch");
//		//start update
//		dbLogId = BardDBUpdateLogger.logStart("Update command= "+command);					
//
//		BardGoAssociationLoader loader = new BardGoAssociationLoader();
//		updateCnt = loader.loadTempGoAssociation(managerProps);
//		BardDBUpdateLogger.logEnd(dbLogId, 0, "Finished GO Association Update, Update Cnt ="+updateCnt);			
//	    } else {
//		BardDBUpdateLogger.logEnd(dbLogId, 1, "ERROR: FAILED File Fetch for GO Association Update.");
//	    }			
	} else if (command.equals(GO_ONTOLOGY_TERM_UPDATE_COMMAND)) {
//	    dbLogId = BardDBUpdateLogger.logStart("Fetch Files for command= "+command);
//	    BardResourceFetch fetch = new BardResourceFetch();
//	    haveFiles = fetch.fetchGOTermDBData(managerProps);			
//	    if(haveFiles) {
//		BardDBUpdateLogger.logEnd(dbLogId, 0, "Finished File Fetch");
//		dbLogId = BardDBUpdateLogger.logStart("Update command= "+command);					
//		BardGoAssociationLoader loader = new BardGoAssociationLoader();
//		loader.updateGoTermTables(managerProps);
//		BardDBUpdateLogger.logEnd(dbLogId, 0, "Finished GO Term Database Update");				
//	    } else {
//		BardDBUpdateLogger.logEnd(dbLogId, 1, "ERROR: Failed File Fetch for GO Term Database Update.");				
//	    }
	} 

	logger.info("DB Manager Completed: " + command + "\n ProcessID: [" + processID + "]");
    }



	public void setArgVector(Vector <String> args) {
		this.args = args;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		BardDBManager dbManager = new BardDBManager();
		if(!dbManager.configure(args[0])) {
			System.err.println("Error setting DBManager Configuration. Either the Conf file or the log file directory is not accessible.");
			System.exit(1);
		}
		Vector <String> argV = new Vector <String> ();
		for(String arg : args) {
			argV.add(arg);
		}
		dbManager.setArgVector(argV);
		dbManager.execute(args[1]);
		System.exit(0);
		
//		FtpBean bean = new FtpBean();
//		try {
//			System.out.println("connect to kegg");
//			bean.ftpConnect("ftp.ncbi.nlm.nih.gov", "anonymous", "john.braisted@nih.gov");
//			System.out.println(bean.getReply());
//			bean.setDirectory("/pubchem/");
//			System.out.println(bean.getReply());
//
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (FtpException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
	}

}
