package gov.nih.ncgc.bard.resourcemgr;

import java.util.Properties;

public abstract class BardExtResourceLoader implements IBardExtResourceLoader {

    private Properties loaderProps;
    private BardResourceService service;
    
    public BardExtResourceLoader() { }
    
    public BardExtResourceLoader(Properties loaderProps) {
	this.loaderProps = loaderProps;
    }
    
    public void setLoaderProps(Properties loaderProps) {
	this.loaderProps = loaderProps;
    }
    
    public void setService(BardResourceService service) {
	this.service = service;
    }

    protected boolean fetchExternalResource(String externalFtpUrl, String localScratch, 
	    int extResourceFormat) {
	boolean fetched = false;
	
	return fetched;	
    }

}
