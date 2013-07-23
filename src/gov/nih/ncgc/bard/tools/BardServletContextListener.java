package gov.nih.ncgc.bard.tools;

import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * This class is specified in the container configuration file (web.xml) as a registered listener.
 * Methods handle initialization and destruction of application context.
 * 
 * @author braistedjc
 *
 */
public class BardServletContextListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent contextEvent) {
	// initialize cache management parameters
	ServletContext servletContext = contextEvent.getServletContext();
	String cachePrefixes = servletContext.getInitParameter("cache-management-cache-prefix-list");
	String cacheMgrNodes = servletContext.getInitParameter("cache-manager-cluster-nodes");
	if(cachePrefixes != null && cacheMgrNodes != null) {
	    DBUtils.initializeManagedCaches(cachePrefixes, cacheMgrNodes);
	} else {
	    // if we don't have context, then try to initialize with the assumption that the
	    // server host is a member of the HazelCast cluster, use the host ip or if that fails, localhost
	    try {
		String host = InetAddress.getLocalHost().getHostAddress();
		//if we don't have context, send empty cache prefix list, 
		DBUtils.initializeManagedCaches("", host);
	    } catch (UnknownHostException unknownHostExcept) {
		unknownHostExcept.printStackTrace();
		DBUtils.initializeManagedCaches("", "localhost");
	    }	    
	}

	// additional initialization can go here ...
    }

    @Override
    public void contextDestroyed(ServletContextEvent arg0) {
	//closes the cache manger
	DBUtils.shutdownCacheFlushManager();
	DBUtils.flushCachePrefixNames = null;
	
	// additional context shutdown/cleanup can go here ... 
    }
}
