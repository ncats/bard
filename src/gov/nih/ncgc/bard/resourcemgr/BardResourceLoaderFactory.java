package gov.nih.ncgc.bard.resourcemgr;


public class BardResourceLoaderFactory {

    public IBardExtResourceLoader getLoader(String serviceClass) {
	IBardExtResourceLoader loader = null;
	
	return loader; 
    }
    
    public IBardExtResourceLoader getLoader(BardResourceService service) {
	
	//IBardExtResourceLoader loader = (IBardExtResourceLoader) (ClassLoader.getSystemClassLoader().loadClass(service.getLoaderClass()));
	IBardExtResourceLoader loader = null;
	try {
	    loader = (IBardExtResourceLoader)(Class.forName(service.getLoaderClass()).newInstance());
	    //the service and loader props provide service info for the loader
	    loader.setService(service);
//	    loader.setLoaderProps(loaderProps);
	} catch (ClassNotFoundException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	} catch (InstantiationException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	} catch (IllegalAccessException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
	
	return loader;
    }
    
    
}
