package gov.nih.ncgc.bard.resourcemgr;

import ftp.FtpException;
import gov.nih.ncgc.bard.resourcemgr.util.BardResourceFetch;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Properties;
import java.util.logging.Logger;

public abstract class BardExtResourceLoader implements IBardExtResourceLoader {

    protected Logger log = Logger.getLogger(BardExtResourceLoader.class.getName());
    
    protected Properties loaderProps;
    protected BardResourceService service;
    protected Connection conn;
    protected String statusText;
    
    public BardExtResourceLoader() { 
	statusText = "Loading new CID_SID mappings.";
    }
    
    public BardExtResourceLoader(Properties loaderProps) {
	this.loaderProps = loaderProps;
    }
    
    public void setLoaderProps(Properties loaderProps) {
	this.loaderProps = loaderProps;
    }
    
    public void setService(BardResourceService service) {
	this.service = service;
    }

    private boolean fetchExternalResource(String server, String extPath, String localDir, int protocol,
	    int extResourceFormat, String uname, String pw) {
	boolean fetched = false;
	
	if(protocol == BardResourceService.ResourceProtocolTypes.FTP.ordinal()) {
	    BardResourceFetch retriever = new BardResourceFetch();
	    try {
		retriever.fetchFTPFileResource(server, uname, pw, extPath, localDir);		
	    } catch (IOException e) {
		e.printStackTrace();
	    } catch (FtpException e) {
		e.printStackTrace();
	    }
	}
		
	return fetched;	
    }
    
    protected boolean fetchExternalResource() {
	boolean fetched = true;
	
	ArrayList <BardExternalResource> resources = service.getExtResources();
	
	for(BardExternalResource resource : resources) {
	    log.info("fetching external resource");
	    
	    if(!fetchExternalResource(
		    resource.getResourceServer(), 
		    resource.getResourcePath()+"/"+resource.getFileName(), 
		    service.getLocalResPath()+"/"+resource.getFileName(), 
		    resource.getResourceProtocolType(),
		    resource.getCompressionType(),
		    resource.getResourceUserName(), 
		    resource.getResourcePassword())) {
		//only alter to false if not fetched
		fetched = false;
	    }
	}
	return fetched;	
    }
    
    protected boolean fetchExternalResourcesFromExtDir() {
	boolean fetched = true;
	
	ArrayList <BardExternalResource> resources = service.getExtResources();
	
	for(BardExternalResource resource : resources) {
	    log.info("fetching external resource");
	    
	    if(!fetchExternalResourcesFromDir(
		    resource.getResourceServer(), 
		    resource.getResourcePath()+"/"+resource.getFileName(), 
		    service.getLocalResPath()+"/"+resource.getFileName(), 
		    resource.getResourceProtocolType(),
		    resource.getCompressionType(),
		    resource.getResourceUserName(), 
		    resource.getResourcePassword())) {
		//only alter to false if not fetched
		fetched = false;
	    }
	}
	return fetched;	
    }
    
    private boolean fetchExternalResourcesFromDir(String server, String extPath, String localDir, int protocol,
	    int extResourceFormat, String uname, String pw) {
	boolean fetched = false;
	
	if(protocol == BardResourceService.ResourceProtocolTypes.FTP.ordinal()) {
	    BardResourceFetch retriever = new BardResourceFetch();
	    try {
		retriever.fetchFTPFileResource(server, uname, pw, extPath, localDir);		
	    } catch (IOException e) {
		e.printStackTrace();
	    } catch (FtpException e) {
		e.printStackTrace();
	    }
	}
		
	return fetched;	
    }
    protected void gunZip(String source, String dest) {
	try {
	    BardResourceFetch.gunzipFile(source, dest);
	} catch (FileNotFoundException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	} catch (IOException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
    }

}
