package gov.nih.ncgc.bard.resourcemgr.extresource;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Properties;
import java.util.Vector;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

import ftp.FtpBean;
import ftp.FtpException;
import ftp.FtpListResult;
import gov.nih.ncgc.bard.resourcemgr.BardDBManager;

public class BardResourceFetch {

	private static Logger logger = Logger.getLogger(BardResourceFetch.class.getName());
	
	
	public boolean fetchFTPDirectoryResource(String server, String user, String pw, String pathToSourceDir, String dest) throws IOException, FtpException {
		boolean transfer = true;
		FtpBean ftpbean = new FtpBean();
		ftpbean.ftpConnect(server, user, pw);
		ftpbean.setDirectory(pathToSourceDir);
		FtpListResult list = ftpbean.getDirectoryContent();
		while (list.next() ) {
			ftpbean.getBinaryFile(list.getName(), dest+"/"+list.getName());
			if(!ftpbean.getReplyMessage().startsWith("2"))
				transfer = false;
		}
		return transfer;		
	}
	
	public boolean fetchFTPFileResource(String server, String user, String pw, String pathToFile, String dest) throws IOException, FtpException {
		boolean transfer = false;
		FtpBean ftpbean = new FtpBean();
		ftpbean.ftpConnect(server, user, pw);
		ftpbean.getBinaryFile(pathToFile, dest);
		return (ftpbean.getReplyMessage().startsWith("2"));		
	}
	
	public boolean fetchFTPCurrentDirectoryResource(String server, String user, String pw, String pathToSourceDir,String dest) throws IOException, FtpException {
		boolean transfer = true;
		FtpBean ftpbean = new FtpBean();
		ftpbean.ftpConnect(server, user, pw);
		ftpbean.setDirectory(pathToSourceDir);
		FtpListResult list = ftpbean.getDirectoryContent();
		while (list.next() ) {
			//if(!ftpbean.getReplyMessage().startsWith("2"))
			//	transfer = false;
		}
		ftpbean.close();
		return transfer;		
	}
	
	public String fetchLatestUpdateResources(String server, String user, String pw, String pathToSourceDir, String dest) throws IOException, FtpException {
		String resource = null;
		FtpBean ftpbean = new FtpBean();
		ftpbean.ftpConnect(server, user, pw);
		ftpbean.setDirectory(pathToSourceDir);
		FtpListResult list = ftpbean.getDirectoryContent();
		
		while (list.next() ) {
			System.out.println(list.getDate());
			System.out.println(list.getName());
			System.out.println(list.getType());
		}
		ftpbean.close();
		return resource;
	}

		
	public void clearBardScratch(String dirPath) {
		
		//quit if not a compound dir or a substance dir -- safety check before delete
		//kind of a hack but good to be careful about deleting directories on a server.
		if(!dirPath.endsWith("Compound/") 
				&& !dirPath.endsWith("Compound-Extras/")
				&& !dirPath.endsWith("Substance/")
				&& !dirPath.endsWith("Substance-Extras/")
				&& !dirPath.endsWith("KEGG-Disease")
				&& !dirPath.endsWith("GO-Associations")
				&& !dirPath.endsWith("GO-Term-Database")
				&& !dirPath.endsWith("assay_desc_zip")
				&& !dirPath.endsWith("Compound-Killed-CIDs/")) {
			
			logger.warning("Didn't clear scratch, incorrect dir:" + dirPath);
			return;
		}
		
		File dir = new File(dirPath);
		File file;
		logger.info("Deleting files in:"+dirPath);
		if(dir.isDirectory()) {
			String [] fileNames = dir.list();
			for(String fileName: fileNames) {
				file = new File(dirPath+"/"+fileName);
				if(file.isFile()) {
					file.delete();
					logger.info("Delete previous file = "+file.getName());
				}
			}
		}
	}
	
	public boolean fetchLatestCompoundResources(Properties props, int resourceUpdatePeriodKey){
		boolean loaded = true;
		
		//clear bard 
		try {
			
			String compoundBaseDir = props.getProperty("pubchem.compound.daily.dir");
			if(resourceUpdatePeriodKey == BardDBManager.COMPOUND_DAILY)
				compoundBaseDir = props.getProperty("pubchem.compound.daily.dir");
			else if(resourceUpdatePeriodKey == BardDBManager.COMPOUND_WEEKLY)
				compoundBaseDir = props.getProperty("pubchem.compound.weekly.dir");
			else if(resourceUpdatePeriodKey == BardDBManager.COMPOUND_MONTHLY)
				compoundBaseDir = props.getProperty("pubchem.compound.monthly.dir");
					
			String destDir = props.getProperty("bard.loader.scratch.dir");
			
			if(destDir == null || destDir == "")
				return false;
			
				
			destDir +="/Compound/";

			
			//clear the destination
			clearBardScratch(destDir);
			
			//make the compound dir
			File dest = new File(destDir);
			dest.mkdir();

			logger.info("Made the temp Compound Dir:"+destDir);
			
			FtpBean ftpbean = new FtpBean();
			ftpbean.ftpConnect(props.getProperty("ncbi.ftp.root"), props.getProperty("ncbi.ftp.user"), props.getProperty("ncbi.ftp.password"));
		
			logger.info("Established NCBI FTP Connection");
			
			ftpbean.setDirectory(compoundBaseDir);
			
			logger.info("Set FTP source directory. RESP:"+ftpbean.getReply());
			
			FtpListResult list = ftpbean.getDirectoryContent();
			Vector <String> dateDirNames = new Vector <String> ();
			
			// the folder names will reveal the latest file.
			while (list.next() ) {
				if(list.getType() == 1) {
					dateDirNames.add(list.getName());
				}
			}
			
			String [] dates = new String[dateDirNames.size()];
			
			for(int i = 0; i < dates.length; i++) {
				dates[i] = dateDirNames.get(i);
			}
			
			Arrays.sort(dates);
			
			ftpbean.setDirectory(compoundBaseDir+"/"+dates[dates.length-1]+"/SDF");

			logger.info("Set FTP LATEST source directory. RESP:"+ftpbean.getReply()+ "Dir = "+ftpbean.getDirectory());

			
			int cnt = 0;
			list = ftpbean.getDirectoryContent();
			//logger.info("File list size ="+list.getSize());
			
			while(list.next()) {
				if(list.getName().endsWith(".gz")) {
					ftpbean.getBinaryFile(list.getName(), destDir+"/"+list.getName());
					cnt++;
					logger.info("File #="+cnt+" source="+list.getName()+" dest="+destDir+"/"+list.getName());
				}
			}
			
			//get killed cids list
			logger.info("Retrieving Killed CID list.");
			String baseResourceDir = compoundBaseDir+"/"+dates[dates.length-1];
			ftpbean.setDirectory(baseResourceDir);
			logger.info("Current Remote Dir Changed. Reply = "+ftpbean.getReply());
			list = ftpbean.getDirectoryContent();
			
			destDir = props.getProperty("bard.filepath.pubhchem.compound.killedcids");
			
			//clear destination - Compound-Extras
			this.clearBardScratch(destDir);	
			
			while(list.next()) {
				if(list.getName().equalsIgnoreCase(props.getProperty("bard.filename.pubchem.compound.killedcids"))) {
					//have the file, send it to the killed cid directory
					ftpbean.getBinaryFile(list.getName(), destDir+list.getName());
				}
			}
			
			//now get the compound extras
			destDir = props.getProperty("bard.loader.scratch.dir")+"/Compound-Extras/";
			
			//make the compound extras dir
			dest = new File(destDir);
			dest.mkdir();
			
			//clear destination - Compound-Extras
			this.clearBardScratch(destDir);			
			
			ftpbean.setDirectory(props.getProperty("pubchem.compound.extras.dir"));
			list = ftpbean.getDirectoryContent();
			Vector <String> extras = new Vector <String> ();
			while(list.next()) {
				extras.add(list.getName());
			}

			int [] extraStatus = new int[3];
			
			//cid-date
			if(extras.contains("CID-Date.gz")) {
				ftpbean.getBinaryFile("CID-Date.gz", destDir+"/CID-Date.gz");
			} else {
				//continue but set status bit
				extraStatus[0]=1;
			}
			
			//cid-sid			
			if(extras.contains("CID-SID.gz")) {
				ftpbean.getBinaryFile("CID-SID.gz", destDir+"/CID-SID.gz");
			} else {
				//continue but set status bit
				extraStatus[1]=1;
			}
			
			//cid-synonyms
			if(extras.contains("CID-Synonym-filtered.gz")) {
				ftpbean.getBinaryFile("CID-Synonym-filtered.gz", destDir+"/CID-Synonym-filtered.gz");
			} else {
				//continue but set status bit
				extraStatus[2]=1;				
			}
			
			if(extraStatus[0] == 1 || extraStatus[1] == 1 || extraStatus[2] == 1) {
				String msg = "ERROR Couldn't Fetch Compound Extra Resources";
				if(extraStatus[0] == 1) {
					msg += " CID-Date.gz";
				}
				if(extraStatus[1] == 1) {
					msg += " CID-SID.gz";
				}
				if(extraStatus[2] == 1) {
					msg += " CID-Synonym-filtered.gz";
				}				
				logger.warning(msg);
				logger.info(msg);
				
				//return false if fetch fails even for extras
				return false;
			}
			
			ftpbean.close();
			
		} catch (FtpException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
				
		return true;
	}
	
	public boolean fetchGOHTTPAssociationResources(Properties dbManagerProps) {
		boolean haveFiles = true;
		
		String humanURL = dbManagerProps.getProperty("go.http.associations.human.file");
		String mouseURL = dbManagerProps.getProperty("go.http.associations.mouse.file");
		String ratURL = dbManagerProps.getProperty("go.http.associations.rat.file");
						
		String destPath = dbManagerProps.getProperty("bard.filepath.go.association.dir");
		
		//clear this scratch area
		clearBardScratch(destPath);
		
		try {
			
			getHttpFile(humanURL, destPath+"/human_go.gz");
			logger.info("Have human go gzip");
			getHttpFile(mouseURL, destPath+"/mouse_go.gz");
			logger.info("Have mosue go gzip");
			getHttpFile(ratURL, destPath+"/rat_go.gz");
			logger.info("Have rat go gzip");

		} catch (MalformedURLException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		
		return haveFiles;
	}
	
	public static void getHttpFile(String urlStr, String outputFilePath) throws IOException {
		boolean haveFile = true;

		URL url = new URL(urlStr);
		InputStream is = url.openStream();
		byte [] buff = new byte[2048];
		BufferedInputStream bis = new BufferedInputStream(is);
		FileOutputStream fos = new FileOutputStream(outputFilePath);
		int len;
		while((len = bis.read(buff)) > 0) {
			fos.write(buff, 0, len);
		}
		fos.close();
		bis.close();
	}
	
	public boolean fetchGOAssociationResources(Properties dbManagerProps) {
		boolean haveFiles = true;;
		
		
		try {
			
		String goRemoteFtpAssocDir = dbManagerProps.getProperty("go.ftp.associations.dir");
		String goLocalScratchDir = dbManagerProps.getProperty("bard.filepath.go.association.dir");
		String humanFile = dbManagerProps.getProperty("go.ftp.associations.human.file");
		String mouseFile = dbManagerProps.getProperty("go.ftp.associations.mouse.file");
		String ratFile = dbManagerProps.getProperty("go.ftp.associations.rat.file");
		
		FtpBean ftpbean = new FtpBean();

			ftpbean.ftpConnect(dbManagerProps.getProperty("go.ftp.server"), dbManagerProps.getProperty("go.ftp.user"));

			logger.info("Established go ftp connection with:"+dbManagerProps.getProperty("go.ftp.server"));
			
			ftpbean.setDirectory(goRemoteFtpAssocDir);
			
			logger.info("Set Remote FTP Directory:" + goRemoteFtpAssocDir);
			
			ftpbean.getBinaryFile(humanFile, goLocalScratchDir+"/"+humanFile);
			logger.info("Have file:"+humanFile);

			ftpbean.getBinaryFile(mouseFile, goLocalScratchDir+"/"+mouseFile);
			logger.info("Have file:"+mouseFile);

			ftpbean.getBinaryFile(ratFile, goLocalScratchDir+"/"+ratFile);
			logger.info("Have file:"+ratFile);

			ftpbean.close();
			
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		} catch (FtpException e) {
			e.printStackTrace();
			return false;
		}
		return haveFiles;
	}
	
	public boolean fetchGOTermDBData(Properties props) {
		boolean fetched = true;
		
		String goTermDBDir = props.getProperty("bard.filepath.go.termdb.dir");
		String goTermTarZip = props.getProperty("bard.filename.go.termdb.targzip");
		//clear bard scratch GO Term DB Dir
		clearBardScratch(goTermDBDir);
		
		//get file
		String goDBURL = props.getProperty("go.http.termdb.file");
		try {
			getHttpFile(goDBURL, goTermDBDir+"/"+goTermTarZip);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return fetched;
	}
	
	
	
	public boolean fetchSpecificCompoundResources(Properties props, String ftpResourceDir){
		boolean loaded = true;
		
		//clear bard 
		try {
			
			String compoundBaseDir = ftpResourceDir;
					
			String destDir = props.getProperty("bard.loader.scratch.dir");
			
			if(destDir == null || destDir == "")
				return false;
	
			destDir +="/Compound";
			
			//clear the destination
			clearBardScratch(destDir);
			
			//make the compound dir
			File dest = new File(destDir);
			dest.mkdir();

			logger.info("Made the temp Compound Dir:"+destDir);
			
			FtpBean ftpbean = new FtpBean();
			ftpbean.ftpConnect(props.getProperty("ncbi.ftp.root"), props.getProperty("ncbi.ftp.user"), props.getProperty("ncbi.ftp.password"));
		
			logger.info("Established NCBI FTP Connection");
			
			ftpbean.setDirectory(compoundBaseDir);
			
			logger.info("Set FTP source directory. RESP:"+ftpbean.getReply());

			int cnt = 0;
			FtpListResult list = ftpbean.getDirectoryContent();
			
			while(list.next()) {
				if(list.getName().endsWith(".gz")) {
					ftpbean.getBinaryFile(list.getName(), destDir+"/"+list.getName());
					cnt++;
					logger.info("File #="+cnt+" source="+list.getName()+" dest="+destDir+"/"+list.getName());
				}
			}
			
			//now get the compound extras
			destDir = props.getProperty("bard.loader.scratch.dir")+"/Compound-Extras/";
			
			//make the compound extras dir
			dest = new File(destDir);
			dest.mkdir();
			
			//clear destination - Compound-Extras
			this.clearBardScratch(destDir);			
			
			ftpbean.setDirectory(props.getProperty("pubchem.compound.extras.dir"));
			list = ftpbean.getDirectoryContent();
			Vector <String> extras = new Vector <String> ();
			while(list.next()) {
				extras.add(list.getName());
			}

			int [] extraStatus = new int[3];
			
			//cid-date
			if(extras.contains("CID-Date.gz")) {
				ftpbean.getBinaryFile("CID-Date.gz", destDir+"/CID-Date.gz");
			} else {
				//continue but set status bit
				extraStatus[0]=1;
			}
			
			//cid-sid			
			if(extras.contains("CID-SID.gz")) {
				ftpbean.getBinaryFile("CID-SID.gz", destDir+"/CID-SID.gz");
			} else {
				//continue but set status bit
				extraStatus[1]=1;
			}
			
			//cid-synonyms
			if(extras.contains("CID-Synonym-filtered.gz")) {
				ftpbean.getBinaryFile("CID-Synonym-filtered.gz", destDir+"/CID-Synonym-filtered.gz");
			} else {
				//continue but set status bit
				extraStatus[2]=1;				
			}
			
			if(extraStatus[0] == 1 || extraStatus[1] == 1 || extraStatus[2] == 1) {
				String msg = "ERROR Couldn't Fetch Compound Extra Resources";
				if(extraStatus[0] == 1) {
					msg += " CID-Date.gz";
				}
				if(extraStatus[1] == 1) {
					msg += " CID-SID.gz";
				}
				if(extraStatus[2] == 1) {
					msg += " CID-Synonym-filtered.gz";
				}				
				logger.warning(msg);
				logger.info(msg);
				
				//return false if fetch fails even for extras
				return false;
			}
			
			ftpbean.close();
			
		} catch (FtpException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
				
		return true;
	}
	
	public boolean fetchCompoundWeeklyExtraRsources(Properties props) {
		boolean loaded = true;
		
		//clear bard 
		try {
			
			String compoundWeeklyBaseDir = props.getProperty("pubchem.compound.weekly.dir");
					
			String destDir = props.getProperty("bard.loader.scratch.dir");
			
			if(destDir == null || destDir == "")
				return false;
	
			//the compound extras dir
			destDir = props.getProperty("bard.loader.scratch.dir")+"/Compound-Extras/";
			
			
			//clear the destination
			clearBardScratch(destDir);
			
			//make the compound dir
			File dest = new File(destDir);
			dest.mkdir();

			logger.info("Made the temp Compound Dir:"+destDir);
			
			FtpBean ftpbean = new FtpBean();
			ftpbean.ftpConnect(props.getProperty("ncbi.ftp.root"), props.getProperty("ncbi.ftp.user"), props.getProperty("ncbi.ftp.password"));
		
			logger.info("Established NCBI FTP Connection");
			
			ftpbean.setDirectory(compoundWeeklyBaseDir);
			
			logger.info("Set FTP to Weekly base directory. RESP:"+ftpbean.getReply());

			
			FtpListResult list = ftpbean.getDirectoryContent();
			Vector <String> dateDirNames = new Vector <String> ();
			
			// the folder names will reveal the latest file.
			while (list.next() ) {
				if(list.getType() == 1) {
					dateDirNames.add(list.getName());
				}
			}
			
			String [] dates = new String[dateDirNames.size()];
			
			for(int i = 0; i < dates.length; i++) {
				dates[i] = dateDirNames.get(i);
			}
			
			Arrays.sort(dates);
			
			//set latest weekly
			ftpbean.setDirectory(compoundWeeklyBaseDir+"/"+dates[dates.length-1]+"/Extras/");

			logger.info("Remote directory found: "+compoundWeeklyBaseDir+"/"+dates[dates.length-1]+"/Extras/");
			logger.info("Set FTP source directory. RESP:"+ftpbean.getReply());
			
			list = ftpbean.getDirectoryContent();
			Vector <String> extras = new Vector <String> ();
			while(list.next()) {
				extras.add(list.getName());
			}

			int [] extraStatus = new int[3];
			
			//cid-date
			if(extras.contains("CID-Date.gz")) {
				ftpbean.getBinaryFile("CID-Date.gz", destDir+"/CID-Date.gz");
			} else {
				//continue but set status bit
				extraStatus[0]=1;
			}
			
			//cid-sid			
			if(extras.contains("CID-SID.gz")) {
				ftpbean.getBinaryFile("CID-SID.gz", destDir+"/CID-SID.gz");
			} else {
				//continue but set status bit
				extraStatus[1]=1;
			}
			
			//cid-synonyms
			if(extras.contains("CID-Synonym-filtered.gz")) {
				ftpbean.getBinaryFile("CID-Synonym-filtered.gz", destDir+"/CID-Synonym-filtered.gz");
			} else {
				//continue but set status bit
				extraStatus[2]=1;				
			}
			
			if(extraStatus[0] == 1 || extraStatus[1] == 1 || extraStatus[2] == 1) {
				String msg = "ERROR Couldn't Fetch Compound Extra Resources";
				if(extraStatus[0] == 1) {
					msg += " CID-Date.gz";
				}
				if(extraStatus[1] == 1) {
					msg += " CID-SID.gz";
				}
				if(extraStatus[2] == 1) {
					msg += " CID-Synonym-filtered.gz";
				}				
				logger.warning(msg);
				logger.info(msg);
				
				//return false if fetch fails even for extras
				return false;
			}
			
			ftpbean.close();
			
		} catch (FtpException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}			
		return true;
	}

	
	public boolean fetchLatestSubstanceResources(Properties props, int resourceUpdatePeriodKey){
		boolean loaded = true;
		
		//clear bard 
		try {
			
			String substanceBaseDir = props.getProperty("pubchem.substance.daily.dir");
			if(resourceUpdatePeriodKey == BardDBManager.SUBSTANCE_DAILY)
				substanceBaseDir = props.getProperty("pubchem.substance.daily.dir");
			else if(resourceUpdatePeriodKey == BardDBManager.SUBSTANCE_WEEKLY)
				substanceBaseDir = props.getProperty("pubchem.substance.weekly.dir");
			else if(resourceUpdatePeriodKey == BardDBManager.SUBSTANCE_MONTHLY)
				substanceBaseDir = props.getProperty("pubchem.substance.monthly.dir");
					
			String destDir = props.getProperty("bard.loader.scratch.dir");
			
			if(destDir == null || destDir == "")
				return false;
			
				
			destDir +="/Substance/";

			
			//clear the destination
			clearBardScratch(destDir);
			
			//make the substance dir
			File dest = new File(destDir);
			dest.mkdir();

			logger.info("Made the temp Substance Dir:"+destDir);
			
			FtpBean ftpbean = new FtpBean();
			ftpbean.ftpConnect(props.getProperty("ncbi.ftp.root"), props.getProperty("ncbi.ftp.user"), props.getProperty("ncbi.ftp.password"));
		
			logger.info("Established NCBI FTP Connection");
			
			ftpbean.setDirectory(substanceBaseDir);
			
			logger.info("Set FTP source directory. RESP:"+ftpbean.getReply());
			
			FtpListResult list = ftpbean.getDirectoryContent();
			Vector <String> dateDirNames = new Vector <String> ();
			
			// the folder names will reveal the latest file.
			while (list.next() ) {
				if(list.getType() == 1) {
					dateDirNames.add(list.getName());
				}
			}
			
			String [] dates = new String[dateDirNames.size()];
			
			for(int i = 0; i < dates.length; i++) {
				dates[i] = dateDirNames.get(i);
			}
			
			Arrays.sort(dates);
			
			ftpbean.setDirectory(substanceBaseDir+"/"+dates[dates.length-1]+"/SDF");

			logger.info("Set FTP LATEST source directory. RESP:"+ftpbean.getReply()+ "Dir = "+ftpbean.getDirectory());

			
			int cnt = 0;
			list = ftpbean.getDirectoryContent();
			//logger.info("File list size ="+list.getSize());
			
			while(list.next()) {
				if(list.getName().endsWith(".gz")) {
					ftpbean.getBinaryFile(list.getName(), destDir+"/"+list.getName());
					cnt++;
					logger.info("File #="+cnt+" source="+list.getName()+" dest="+destDir+"/"+list.getName());
				}
			}
			
			//now get the compound extras
			destDir = props.getProperty("bard.loader.scratch.dir")+"/Substance-Extras/";
			
			//make the compound extras dir
			dest = new File(destDir);
			dest.mkdir();
			
			//clear destination - Compound-Extras
			this.clearBardScratch(destDir);			
			
			ftpbean.setDirectory(props.getProperty("pubchem.substance.extras.dir"));
			list = ftpbean.getDirectoryContent();
			Vector <String> extras = new Vector <String> ();
			while(list.next()) {
				extras.add(list.getName());
			}

			int [] extraStatus = new int[2];
			
			//cid-date
			if(extras.contains("SID-Date.gz")) {
				ftpbean.getBinaryFile("SID-Date.gz", destDir+"/SID-Date.gz");
			} else {
				//continue but set status bit
				extraStatus[0]=1;
			}
			
			
			//cid-synonyms
			if(extras.contains("Source-Names")) {
				ftpbean.getBinaryFile("Source-Names", destDir+"/Source-Names");
			} else {
				//continue but set status bit
				extraStatus[2]=1;				
			}
			
			if(extraStatus[0] == 1 || extraStatus[1] == 1) {
				String msg = "ERROR Couldn't Fetch Compound Extra Resources";
				if(extraStatus[0] == 1) {
					msg += " SID-Date.gz";
				}
				if(extraStatus[1] == 1) {
					msg += " Source-Names";
				}
	
				logger.warning(msg);
				logger.info(msg);
				
				//return false if fetch fails even for extras
				return false;
			}
			
			ftpbean.close();
			
		} catch (FtpException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
				
		return true;
	}
	
	
	
	public boolean fetchSpecificSubstanceResources(Properties props, String ftpResourcePath){
		boolean loaded = true;
		
		//clear bard 
		try {
			
			String substanceBaseDir = ftpResourcePath;

			String destDir = props.getProperty("bard.loader.scratch.dir");
			
			if(destDir == null || destDir == "")
				return false;
			
				
			destDir +="/Substance/";

			
			//clear the destination
			clearBardScratch(destDir);
			
			//make the substance dir
			File dest = new File(destDir);
			dest.mkdir();

			logger.info("Made the temp Substance Dir:"+destDir);
			
			FtpBean ftpbean = new FtpBean();
			ftpbean.ftpConnect(props.getProperty("ncbi.ftp.root"), props.getProperty("ncbi.ftp.user"), props.getProperty("ncbi.ftp.password"));
		
			logger.info("Established NCBI FTP Connection");
			
			ftpbean.setDirectory(substanceBaseDir);
			
			logger.info("Set FTP source directory. RESP:"+ftpbean.getReply());
			
			FtpListResult list = ftpbean.getDirectoryContent();
			//Vector <String> dateDirNames = new Vector <String> ();
			
			// the folder names will reveal the latest file.
		
			logger.info("Set FTP LATEST source directory. RESP:"+ftpbean.getReply()+ "Dir = "+ftpbean.getDirectory());

			
			int cnt = 0;
			list = ftpbean.getDirectoryContent();
			//logger.info("File list size ="+list.getSize());
			
			while(list.next()) {
				if(list.getName().endsWith(".gz")) {
					ftpbean.getBinaryFile(list.getName(), destDir+"/"+list.getName());
					cnt++;
					logger.info("File #="+cnt+" source="+list.getName()+" dest="+destDir+"/"+list.getName());
				}
			}
			
			//now get the compound extras
			destDir = props.getProperty("bard.loader.scratch.dir")+"/Substance-Extras/";
			
			//make the compound extras dir
			dest = new File(destDir);
			dest.mkdir();
			
			//clear destination - Compound-Extras
			this.clearBardScratch(destDir);			
			
			ftpbean.setDirectory(props.getProperty("pubchem.substance.extras.dir"));
			list = ftpbean.getDirectoryContent();
			Vector <String> extras = new Vector <String> ();
			while(list.next()) {
				extras.add(list.getName());
			}

			int [] extraStatus = new int[2];
			
			//cid-date
			if(extras.contains("SID-Date.gz")) {
				ftpbean.getBinaryFile("SID-Date.gz", destDir+"/SID-Date.gz");
			} else {
				//continue but set status bit
				extraStatus[0]=1;
			}
			
			
			//cid-synonyms
			if(extras.contains("Source-Names")) {
				ftpbean.getBinaryFile("Source-Names", destDir+"/Source-Names");
			} else {
				//continue but set status bit
				extraStatus[2]=1;				
			}
			
			if(extraStatus[0] == 1 || extraStatus[1] == 1) {
				String msg = "ERROR Couldn't Fetch Compound Extra Resources";
				if(extraStatus[0] == 1) {
					msg += " SID-Date.gz";
				}
				if(extraStatus[1] == 1) {
					msg += " Source-Names";
				}
	
				logger.warning(msg);
				logger.info(msg);
				
				//return false if fetch fails even for extras
				return false;
			}
			
			ftpbean.close();
			
		} catch (FtpException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
				
		return true;
	}
	
	
	
	public boolean fetchKEGGDiseaseFile(Properties props) {
		boolean haveFiles = false;
		FtpBean ftpbean = new FtpBean();
		try {
			ftpbean.ftpConnect(props.getProperty("kegg.ftp.server"), props.getProperty("kegg.ftp.user"));
			ftpbean.setDirectory(props.getProperty("kegg.disease.dir"));
			String destDir = props.getProperty("bard.loader.scratch.dir") + "/KEGG-Disease";
						
			File keggDir = new File(destDir);
			keggDir.mkdir();

			this.clearBardScratch(destDir);
			
			ftpbean.getBinaryFile(props.getProperty("kegg.disease.filename"), destDir + "/" +props.getProperty("kegg.disease.filename"));			
			if(ftpbean.getReply().trim().startsWith("2"))
				haveFiles = true;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		} catch (FtpException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		return haveFiles;
	}
	
//	public boolean fetchMLPCNAssayList(Properties props) {		
//		BardPullPubchemAIDListUtil aidPuller = new BardPullPubchemAIDListUtil();
//		Vector <Integer> assayIDs = aidPuller.buildAIDList(props);
//		long assayStartCount;
//		
//		try {
//			assayStartCount = BardDBUtil.getTableRowCount("mlp_assay");
//		} catch (ClassNotFoundException e) {
//			e.printStackTrace();
//			return false;
//		} catch (SQLException e) {
//			e.printStackTrace();
//			return false;
//		}
//		
//		double sizeRatio = ((double)assayIDs.size())/((double)assayStartCount);	
//		//the assay count ratio should be 1.0 or greater. Accounting for slight drop in case of deprecation of stale assays
//		//or other policies that might cause a slight (<2%) drop in assay count.
//		return (sizeRatio > 0.98);
//	}
	
	
	public boolean fetchAssayDescriptionMetatdataXMLZips(Properties props) {
		boolean haveFiles = true;

		String bioassayDescriptionBase = props.getProperty("pubchem.assay.description.root");				
		String localBioassayZipDir = props.getProperty("bard.filepath.mlpcn.assaydesczip");
		
		clearBardScratch(localBioassayZipDir);
		
		FtpBean ftpbean = new FtpBean();
		try {
			ftpbean.ftpConnect(props.getProperty("ncbi.ftp.root"), props.getProperty("ncbi.ftp.user"), props.getProperty("ncbi.ftp.password"));
		
			logger.info("Established NCBI FTP Connection");
			
			ftpbean.setDirectory(bioassayDescriptionBase);
			logger.info("Changed DIR, Reply: "+ftpbean.getReply());

			FtpListResult list = ftpbean.getDirectoryContent();
			String fileName = "";
			int zipCnt = 0;
			while(list.next()) {
				fileName = list.getName();
				if(fileName.endsWith(".zip")) {
					ftpbean.getBinaryFile(fileName, localBioassayZipDir+"/"+fileName);
					zipCnt++;
					logger.info("Download Assay XML ZIP File ("+zipCnt+"): "+fileName);
				}
			}
			
			logger.info("Finished Assay Description Download, File Count ="+zipCnt);
			
			//make sure we have at least one file
			//later if the assay update count is too low, an error will prevent update of production table
			haveFiles = (zipCnt > 0);
			
			ftpbean.close();		
		
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		} catch (FtpException e) {
			e.printStackTrace();
			return false;
		}
		
		return haveFiles;
	}
	
	public boolean fetchUniprotDatFile(Properties props) {
		boolean haveFiles = false;
		FtpBean ftpbean = new FtpBean();
		try {
			ftpbean.ftpConnect(props.getProperty("uniprot.ftp.server"), props.getProperty("uniprot.ftp.user"), "");
			ftpbean.setDirectory(props.getProperty("uniprot.data.current.dir"));
			
			logger.info("Uniprot Change Dir Response="+ftpbean.getReply());
			
			String destDir = props.getProperty("bard.loader.scratch.dir") + "/Uniprot";
			
			File uniprotDir = new File(destDir);
			
			if(!uniprotDir.exists())
			    uniprotDir.mkdir();
			
			this.clearBardScratch(destDir);
			logger.info("ftploc = "+ftpbean.getDirectory()+" file name ="+props.getProperty("uniprot.data.file"));
			
			ftpbean.getBinaryFile(props.getProperty("uniprot.data.file"), destDir + "/" +props.getProperty("uniprot.data.file"));			
			
			if(ftpbean.getReply().trim().startsWith("2"))
				haveFiles = true;
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		} catch (FtpException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		return haveFiles;
	}
	
	public static void gunzipFile(String sourcePath, String destPath) throws FileNotFoundException, IOException {
		GZIPInputStream gis = new GZIPInputStream(new FileInputStream(sourcePath));
		FileOutputStream fos = new FileOutputStream(destPath);
		byte [] buff = new byte[2048];
		int len;
		while((len = gis.read(buff)) > 0) {
			fos.write(buff, 0, len);
		}
		gis.close();
		fos.flush();
		fos.close();
	}
	
	public static void untarFile(String sourceFilePath) throws IOException {
		TarArchiveInputStream tis = new TarArchiveInputStream(new FileInputStream(sourceFilePath));
		TarArchiveEntry entry;
		String filename;

		byte [] buff = new byte [512];
		int len;
		File file = new File(sourceFilePath);
		String destPath = file.getParent();

		while((entry = tis.getNextTarEntry()) != null) {
			
			filename = entry.getName();
			file = new File(destPath, filename);

			if(entry.isDirectory()) {
				if(!file.exists()) {
					file.mkdirs();
				}				
			} else {

				FileOutputStream fos = new FileOutputStream(destPath+"/"+filename);

				while((len = tis.read(buff)) > 0) {
					fos.write(buff,0,len);
				}

				fos.close();
			}
		}	
		tis.close();
	}
	
	public boolean fetchFTPFileResource(String server, String pathToFile) {
		boolean transfer = false;
		
		return transfer;
	}
	
	public static void main(String [] args) {
		BardResourceFetch fetch = new BardResourceFetch();
		try {
			fetch.gunzipFile("C:/Temp/GO_test/tarziptest/testtar2.tar.gz", "C:/Temp/GO_test/tarziptest/testtar2.tar");
			fetch.untarFile("C:/Temp/GO_test/tarziptest/testtar2.tar");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
//		if(!fetch.fetchGOHTTPAssociationResources(new Properties()))
//			System.out.println("no files");
	}
	
}
