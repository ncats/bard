package gov.nih.ncgc.bard.resourcemgr;

import gov.nih.ncgc.bard.resourcemgr.util.BardResourceLoaderException;
import gov.nih.ncgc.bard.resourcemgr.util.BardServiceParser;

import java.util.ArrayList;
import java.util.Date;

public class BardResourceManager {
    
    private BardResourceLoaderFactory loaderFactory;
    private ArrayList <BardResourceService> services;
    
    public BardResourceManager() { }
    
    public BardResourceManager(String servicePath) {
	loadServices(servicePath);
	loaderFactory = new BardResourceLoaderFactory();	
    }
    
    /**
     * Runs the load service based on the supplied service key.
     * 
     * @param serviceKey the key for the service to run
     * @return returns true if service completed with intended outcome
     */
    public boolean runService(String serviceKey) throws BardResourceLoaderException {
	boolean loaded = false;
	long logId = -1;
	BardResourceService service = null;
	try {
	    
	    for(BardResourceService s : services) {
		s.dumpServiceVals();
	    }
	    service = findService(serviceKey);	
	    if(service != null) {	    
		logId = BardDBUpdateLogger.logStart(service.getServiceName(), service.getDbURL());
		IBardExtResourceLoader loader = loaderFactory.getLoader(service);
		loaded = loader.load();	
		loaded = true;
		BardDBUpdateLogger.logEnd(logId, 0, loader.getLoadStatusReport(), service.getDbURL());
		//remove ref to loader.
		loader = null;
	    }
	} catch (Exception e) {
	    if(logId != -1 && service != null) {
		BardDBUpdateLogger.logEnd(logId, 0, "Error/Exception during "+service.getServiceName(), service.getDbURL());
	    }
	    String msg = "BardResourceLoaderException: "+service.getServiceKey()+". Nested exception reports cause.";
	    BardResourceLoaderException brle = new BardResourceLoaderException(msg,e);
	    throw(brle);
	} 
	return loaded;	
    }
    
    /**
     * Runs all services in the service list
     * 
     * @return true if all run
     */
    public boolean runServices() {
	boolean loaded = false;
	for(BardResourceService service : services) {
	    IBardExtResourceLoader loader = loaderFactory.getLoader(service);
	    loaded = loader.load();	
	}
	return loaded;
    }
    
    //loads available services
    private void loadServices(String serviceFilePath) {
	BardServiceParser parser = new BardServiceParser();
	services = parser.parseServices(serviceFilePath);
    }
    
    //loads properties
//    private void loadProperties(String propertyPath) {
//	loaderProps = new Properties();
//	try {
//	    loaderProps.load(new FileInputStream(propertyPath));
//	} catch (FileNotFoundException e) {
//	    e.printStackTrace();
//	} catch (IOException e) {
//	    e.printStackTrace();
//	}
//    }
    
    private BardResourceService findService(String key) {
	for(BardResourceService service : services) {
	    if(service.equals(key))
		return service;
	}
	return null;
    }

    
    public static void main(String [] args) {
	
	if(args == null || args.length < 1) {
	    System.out.println("null args");
	}
	
	String configPath = null;
	String serviceFilePath = null;
	String serviceKey = null;
	String arg;
	boolean runAllServices = false;
	boolean argErr = false;
	
	for(int i = 0; i < args.length; i++) {
	    arg = args[i];
	    if(arg.equals("--run-all-services")) {
		runAllServices = true;;
	    } else if (arg.equals("--service-path")) {
		//next should be service path value
		i++;
		if(i > args.length-1) {
		    argErr = true;
		    break;
		} else {
		    serviceFilePath = args[i];
		}
	    } else if (arg.equals("--config-path")) {
		//next should be service path value
		i++;
		if(i > args.length-1) {
		    argErr = true;
		    break;
		} else {
		    configPath = args[i];
		}		
	    } else if (arg.equals("--service-key")) {
		i++;
		if(i > args.length-1) {
		    argErr = true;
		    break;
		} else {
		    serviceKey = args[i];
		}
	    }
	}
	
	if(argErr || serviceFilePath == null || serviceKey == null) {
	    System.err.println("ERROR Processing Resources, Date:"+new Date(System.currentTimeMillis()));
	    System.err.println("ERROR Parsing Launch Arguments for BardResourceManager");
	    System.out.println("ERROR Processing Resources, Date:"+new Date(System.currentTimeMillis()));
	    System.out.println("ERROR Parsing Launch Arguments for BardResourceManager");
	    System.exit(1);
	}
	
	System.out.println("\n\nService Key"+serviceKey);
	
	BardResourceManager manager = new BardResourceManager(serviceFilePath);
	if(runAllServices) {
	    manager.runServices();
	} else {
	    try {
		manager.runService(serviceKey);
	    } catch (Exception e) {	
		e.printStackTrace();
	    }
	}
	System.exit(0);
    }
}
