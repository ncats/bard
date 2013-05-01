package gov.nih.ncgc.bard.resourcemgr;

import java.util.ArrayList;


public class BardResourceService {

    private String serviceKey;
    private String serviceName;
    private String serviceDescr;
    private ArrayList <BardExternalResource> extResources;
    private String localResPath;
    private String loaderClass;
    private String dbURL;
    
    public enum ServiceFields {
	SERVICE_KEY,
	SERVICE_NAME,
	SERVICE_DESCR,
	EXT_RESOURCE,
	LOCAL_RESOURCE_PATH,
	LOADER_CLASS,
	DB_URL
    }
    
    public enum ResourceProtocolTypes {
	FTP,
	HTTP
    }
    
    public enum CompressionTypes {
	NONE,
	GZIP,
	TAR_GZIP,
	ZIP,
	TAR_ZIP,
	BZ2,
	SEVEN_ZIP,
	VARIOUS
    }

    public BardResourceService() { 
	extResources = new ArrayList<BardExternalResource>();
    }

    public boolean equals(Object obj) {
	if(obj instanceof String) {
	    return (serviceKey.equals((String)obj));
	} else if(obj instanceof BardResourceService) {
	    return (((BardResourceService)obj).getServiceKey().equals(serviceKey));
	}
	return false;
    }

    public String getServiceKey() {
	return serviceKey;
    }

    public void setServiceKey(String serviceKey) {
	this.serviceKey = serviceKey;
    }

    public String getServiceName() {
	return serviceName;
    }

    public void setServiceName(String serviceName) {
	this.serviceName = serviceName;
    }

    public String getServiceDescr() {
	return serviceDescr;
    }

    public void setServiceDescr(String serviceDescr) {
	this.serviceDescr = serviceDescr;
    }

    public String getLocalResPath() {
	return localResPath;
    }

    public void setLocalResPath(String localResPath) {
	this.localResPath = localResPath;
    }

    public String getLoaderClass() {
	return loaderClass;
    }

    public void setLoaderClass(String loaderClass) {
	this.loaderClass = loaderClass;
    }
    
    public ArrayList<BardExternalResource> getExtResources() {
        return extResources;
    }

    public void setExtResources(ArrayList<BardExternalResource> extResources) {
        this.extResources = extResources;
    }

    public void addExtResource(BardExternalResource resource) {
	this.extResources.add(resource);
    }
    
    public String getDbURL() {
        return dbURL;
    }

    public void setDbURL(String dbURL) {
        this.dbURL = dbURL;
    }
    
    public void dumpServiceVals() {
//	    private String serviceKey;
//	    private String serviceName;
//	    private String serviceDescr;
//	    private ArrayList <BardExternalResource> extResources;
//	    private String localResPath;
//	    private String loaderClass;
//	    private String dbURL;
	System.out.println("SERVICE_KEY:"+serviceKey);
	System.out.println("SERVICE_NAME:"+serviceName);
	System.out.println("SERVICE_DESC:"+serviceDescr);
	for(BardExternalResource res : extResources) {
	    res.dumpExtResourceVals();
	}
	System.out.println("LOCAL_RES_PATH:"+localResPath);
	System.out.println("LOADER_CLASS:"+loaderClass);
	System.out.println("DB_URL:"+dbURL);


    }

}