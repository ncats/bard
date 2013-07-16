package gov.nih.ncgc.bard.tools;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class CacheContextListener implements  ServletContextListener{

    @Override
    public void contextDestroyed(ServletContextEvent arg0) {
	DBUtils.shutdownCacheFlushManager();
	DBUtils.flushCachePrefixNames = null;
    }

    @Override
    public void contextInitialized(ServletContextEvent arg0) {
	//nothing needed here...
    }

}
