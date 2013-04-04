package gov.nih.ncgc.bard.resourcemgr;


public class BardResourceService {

    private String serviceKey;
    private String serviceName;
    private String serviceDescr;
    private String ftpURL;
    private String localResPath;
    private String loaderClass;
    
    public enum ServiceFields {
	SERVICE_KEY,
	SERVICE_NAME,
	SERVICE_DESCR,
	FTP_URL,
	LOCAL_RESOURCE_PATH,
	LOADER_CLASS
    }

    public BardResourceService() { }

    public BardResourceService(String serviceKey, String serviceName, String serviceDescription, 
	    String ftpURL, String localResourcePath) {
	this.serviceKey = serviceKey;
	this.serviceName = serviceName;
	this.serviceDescr = serviceDescription;
	this.ftpURL = ftpURL;
	this.localResPath = localResourcePath;
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

    public String getFtpURL() {
	return ftpURL;
    }

    public void setFtpURL(String ftpURL) {
	this.ftpURL = ftpURL;
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


}