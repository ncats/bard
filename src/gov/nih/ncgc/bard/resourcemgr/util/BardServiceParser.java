package gov.nih.ncgc.bard.resourcemgr.util;

import gov.nih.ncgc.bard.resourcemgr.BardResourceService;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class BardServiceParser {

    
    public ArrayList <BardResourceService> parseServices(String serviceFilePath) {
	ArrayList <BardResourceService> services  = new ArrayList <BardResourceService>();
	
	try {
	    BufferedReader br = new BufferedReader(new FileReader(serviceFilePath));
	    String line = "";
	    String buffer = "";
	    String lineDelim = "///";
	    BardResourceService service;
	    while((line = br.readLine())!= null) {
		if(line.contains(lineDelim)) {
		    service = parseService(buffer);
		    services.add(service);
		    buffer = "";
		} else if (!line.startsWith("#") && line.trim().length() > 0) {
		    buffer += line;		    
		}	    
	    }	    
	} catch (FileNotFoundException e) {
	    e.printStackTrace();
	} catch (IOException e) {
	    e.printStackTrace();
	}
	return services;
    }
    
    private BardResourceService parseService(String data) {
	BardResourceService service = new BardResourceService();
	String [] toks = data.split("\n");
	String [] keyVal;
	for(String tok : toks) {
	    tok = tok.trim();
	    keyVal = tok.split(":");
	    if(keyVal.length == 2) {
		if(keyVal[0].equals(BardResourceService.ServiceFields.SERVICE_KEY)) {
		    service.setServiceKey(keyVal[1].trim());
		} else if(keyVal[0].equals(BardResourceService.ServiceFields.SERVICE_NAME)) {
		    service.setServiceName(keyVal[1].trim());
		} else if(keyVal[0].equals(BardResourceService.ServiceFields.SERVICE_DESCR)) {
		    service.setServiceDescr(keyVal[1].trim());
		} else if(keyVal[0].equals(BardResourceService.ServiceFields.FTP_URL)) {
		    service.setFtpURL(keyVal[1].trim());
		} else if(keyVal[0].equals(BardResourceService.ServiceFields.LOCAL_RESOURCE_PATH)) {
		    service.setLocalResPath(keyVal[1].trim());
		} else if(keyVal[0].equals(BardResourceService.ServiceFields.SERVICE_KEY)) {
		    service.setLoaderClass(keyVal[1].trim());
		} 
	    }
	}
	return service;
    }
    
}
