package gov.nih.ncgc.bard.service;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.logging.Logger;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.spi.ObjectFactory;

public class CachingServiceFactory implements ObjectFactory {
    static final Logger logger = 
	Logger.getLogger(CachingServiceFactory.class.getName());

    CachingService service;

    public CachingServiceFactory () {
    }

    // ObjectFactory interface
    public Object getObjectInstance
        (Object obj, Name name, Context ctx, Hashtable environment)
        throws NamingException {

        if (service == null) {
            Reference ref = (Reference)obj;
            Enumeration addrs = ref.getAll();
            
            int maxCacheSize = -1; // use default
            while (addrs.hasMoreElements()) {
                RefAddr addr = (RefAddr) addrs.nextElement();
                String type = addr.getType();
                String value = (String) addr.getContent();
                
                if (type.equals("max-cache-size")) {
                    try {
                        maxCacheSize= Integer.parseInt(value);
                        logger.info("max-cache-size: "+maxCacheSize);
                    }
                    catch (NumberFormatException e) {
                        logger.warning("Bogus max-cache-size: "+value);
                    }
                }
                else {
                    logger.warning("Unknown parameter: "+type);
                }
            }
        
            service = new CachingService (maxCacheSize);
            logger.info("** CachingService "+service+" ready; maxCacheSize="
                        +service.getMaxCacheSize());
        }

        return service; 
    }
}
