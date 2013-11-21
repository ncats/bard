package gov.nih.ncgc.bard.tools;

import gov.nih.ncgc.bard.rest.BARDResource;
import gov.nih.ncgc.bard.search.SolrSearch;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.util.logging.Logger;

/**
 * This class is specified in the container configuration file (web.xml) as a registered listener.
 * Methods handle initialization and destruction of application context.
 *
 * @author braistedjc
 */
public class BardServletContextListener implements ServletContextListener {
    static final Logger logger =
            Logger.getLogger(BardServletContextListener.class.getName());

    DBUtils db;

    @Override
    public void contextInitialized(ServletContextEvent contextEvent) {
        initContext (contextEvent.getServletContext());

        db = new DBUtils();
        BARDResource.setDb(db);
        SolrSearch.setDb(db);

        // should we initialize cache mgmt at all?
        String sym = contextEvent.getServletContext().getInitParameter("initHazelcast");
        boolean initHazelcast = true;
        if (sym != null) initHazelcast = sym.toLowerCase().equals("true");

        // initialize cache management parameters
        if (initHazelcast) {
            ServletContext servletContext = contextEvent.getServletContext();
            String cachePrefixes = servletContext.getInitParameter("cache-management-cache-prefix-list");
            String cacheMgrNodes = servletContext.getInitParameter("cache-manager-cluster-nodes");
            if (cachePrefixes != null && cacheMgrNodes != null) {
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
        }

        // additional initialization can go here ...
    }

    void initContext (ServletContext servletContext) {
        String value = servletContext.getInitParameter("datasource-selector");
        logger.info("## datasource-selector: "+value);

        if (value != null) {
            String selector = servletContext.getInitParameter(value);
            logger.info("## "+value+": "+selector);
            if (selector != null) {
                String[] sources = selector.split(",");
                DBUtils.setDataSources(sources);
            }
        }
        else {
            String ctx = servletContext.getInitParameter("datasource-context");
            logger.info("## datasource context: "+ctx);
            if (ctx != null) {
                DBUtils.setDataSources(ctx);
            }            
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent arg0) {
        //closes the cache manger
        DBUtils.shutdownCacheFlushManager();
        DBUtils.flushCachePrefixNames = null;
        try {
            db.closeConnection();
        } catch (SQLException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }
}
