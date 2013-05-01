package gov.nih.ncgc.bard.resourcemgr.util;

import gov.nih.ncgc.bard.resourcemgr.BardExternalResource;
import gov.nih.ncgc.bard.resourcemgr.BardResourceService;
import gov.nih.ncgc.bard.resourcemgr.BardResourceService.CompressionTypes;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class BardServiceParser {

    public BardServiceParser() { }
    
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
		    buffer += line+"|";		    
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
	String [] toks = data.split("\\|");
	String [] keyVal;
	for(String tok : toks) {
	    keyVal = tok.split("\t");
	    
	    System.out.println("keyval length="+keyVal.length);

	    if(keyVal.length == 2) {
		if(keyVal[0].equals(BardResourceService.ServiceFields.SERVICE_KEY.name())) {
		    service.setServiceKey(keyVal[1].trim());
		} else if(keyVal[0].equals(BardResourceService.ServiceFields.SERVICE_NAME.name())) {
		    service.setServiceName(keyVal[1].trim());
		} else if(keyVal[0].equals(BardResourceService.ServiceFields.SERVICE_DESCR.name())) {
		    service.setServiceDescr(keyVal[1].trim());
		} else if(keyVal[0].equals(BardResourceService.ServiceFields.EXT_RESOURCE.name())) {
		    System.out.println("Hey, processing ext resource");
		    service.addExtResource(processExternalResourceSpec(keyVal[1].trim()));
		} else if(keyVal[0].equals(BardResourceService.ServiceFields.LOCAL_RESOURCE_PATH.name())) {
		    service.setLocalResPath(keyVal[1].trim());
		} else if(keyVal[0].equals(BardResourceService.ServiceFields.LOADER_CLASS.name())) {
		    service.setLoaderClass(keyVal[1].trim());
		} else if(keyVal[0].equals(BardResourceService.ServiceFields.DB_URL.name())) {
		    service.setDbURL(keyVal[1].trim());
		}
	    }
	}
	return service;
    }
    
    private BardExternalResource processExternalResourceSpec(String resourceSpec) {
	BardExternalResource resource =new BardExternalResource();
	
	// fixed arrangement
	//[rsc_key][rsc_protocol(FTP|HTTP)][file_name][file_comression(NONE|GZIP|ZIP|TAR_GZIP|TAR_ZIP)][rsc_uri]
	
	String [] toks = resourceSpec.split("]");

	if(toks.length < 8) {
	    System.out.println("External resource has fewer than 8 tokens");
	    return null;    
	}
	
	int itemCnt = 0;
	for(String tok : toks) {
	    tok = tok.replace("[", "").trim();
	    
	    // ass|u|me the fields are in the proper order.
	    if(itemCnt == 0) {
		resource.setResourceKey(tok);
	    } else if (itemCnt == 1) {
		if(tok.equals(BardResourceService.ResourceProtocolTypes.FTP.name())) {
		    resource.setResourceProtocolType(BardResourceService.ResourceProtocolTypes.FTP.ordinal());
		} else {
		    resource.setResourceProtocolType(BardResourceService.ResourceProtocolTypes.HTTP.ordinal());
		}
	    } else if (itemCnt == 2) {
		resource.setFileName(tok);
	    } else if (itemCnt == 3) {
		CompressionTypes [] compType = BardResourceService.CompressionTypes.values();
		for(int i = 0; i < compType.length; i++) {
		    if(compType[i].name().equals(tok)) {
			resource.setCompressionType(compType[i].ordinal());
		    }
		}
	    } else if (itemCnt == 4) {
		resource.setResourceServer(tok);
	    } else if (itemCnt == 5) {
		resource.setResourcePath(tok);
	    } else if (itemCnt == 6) {
		resource.setResourceUserName(tok);
	    } else if (itemCnt == 7) {
		resource.setResourcePassword(tok);
	    }
	    itemCnt++;
	}
	
	return resource;
    }
    
}
