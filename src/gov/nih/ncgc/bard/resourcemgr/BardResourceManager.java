package gov.nih.ncgc.bard.resourcemgr;

import gov.nih.ncgc.bard.resourcemgr.util.BardServiceParser;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;

public class BardResourceManager {
    
    private BardResourceLoaderFactory loaderFactory;
    private ArrayList <BardResourceService> services;
    private Properties loaderProps;
    
    public BardResourceManager(String servicePath, String configPath) {
	loadServices(servicePath);
	loadProperties(configPath);
	loaderFactory = new BardResourceLoaderFactory();	
    }
    
    /**
     * Runs the load service based on the supplied service key.
     * 
     * @param serviceKey the key for the service to run
     * @return returns true if service completed with intended outcome
     */
    public boolean runService(String serviceKey) {
	boolean loaded = false;	
	BardResourceService service = findService(serviceKey);	
	IBardExtResourceLoader loader = loaderFactory.getLoader(service, loaderProps);
	loaded = loader.load();	
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
	    IBardExtResourceLoader loader = loaderFactory.getLoader(service, loaderProps);
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
    private void loadProperties(String propertyPath) {
	loaderProps = new Properties();
	try {
	    loaderProps.load(new FileInputStream(propertyPath));
	} catch (FileNotFoundException e) {
	    e.printStackTrace();
	} catch (IOException e) {
	    e.printStackTrace();
	}
    }
    
    private BardResourceService findService(String key) {
	for(BardResourceService service : services) {
	    if(service.equals(key))
		return service;
	}
	return null;
    }
    
    public static void main(String [] args) {
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
	    }
	}
	
	if(argErr && serviceFilePath != null && configPath != null) {
	    System.err.println("ERROR Processing Resources, Date:"+new Date(System.currentTimeMillis()));
	    System.err.println("ERROR Parsing Launch Arguments for BardResourceManager");
	    System.out.println("ERROR Processing Resources, Date:"+new Date(System.currentTimeMillis()));
	    System.out.println("ERROR Parsing Launch Arguments for BardResourceManager");
	    System.exit(1);
	}
	
	BardResourceManager manager = new BardResourceManager(serviceFilePath, configPath);
	if(runAllServices) {
	    manager.runServices();
	} else {
	    manager.runService(args[2]);
	}
	
    }
}
