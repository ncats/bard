package gov.nih.ncgc.bard.tools;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Vector;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import net.sf.ehcache.CacheManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CacheFlushManager {

    Logger log = LoggerFactory.getLogger(this.getClass());
    ScheduledExecutorService threadPool = Executors.newScheduledThreadPool(10);
    CacheManager cacheManager;
    String connContext;

    public CacheFlushManager (CacheManager cacheManager, String cacheConnContext) {
	this.cacheManager = cacheManager;
	this.connContext = cacheConnContext;
    }

    public void manage (Vector <String> cachePrefixList, long rate) {
	log.info("Cache Manager startign management of caches.");
	threadPool.scheduleWithFixedDelay(new FlushWorker (cachePrefixList), 0, rate, TimeUnit.SECONDS);
    }

    public void shutdown () {
	threadPool.shutdownNow(); // clean up
    }


    class FlushWorker implements Runnable {
	Vector <String> cachePrefixList;
	Connection conn;
	Timestamp localUpdateTime;
	PreparedStatement ps;
	boolean init;

	FlushWorker (Vector <String> cachePrefixList) {
	    this.cachePrefixList = cachePrefixList;
	    init = initialize();
	}

	public void run () {
	    //check status
	    checkCacheFlushState();
	}

	private boolean initialize() {
	    ResultSet rs = null;
	    boolean initialized = false;
	    connect();
	    try {
		ps = conn.prepareStatement("select updated from bard_update_time");
		rs = ps.executeQuery();
		if(rs.next()) {
		    localUpdateTime = rs.getTimestamp(1);
		    initialized = true;
		}
	    } catch (SQLException e) {
		e.printStackTrace();
	    } finally {
		try {
		    if(ps != null) ps.close();
		    if(rs != null) rs.close();
		} catch (SQLException e) {  }
	    }
	    return initialized;
	}

	private void checkCacheFlushState() {
	    Statement stmt = null;
	    ResultSet rs = null;
	    try {
		stmt = conn.createStatement();
		rs = stmt.executeQuery("select updated from bard_update_time");
		if(rs.next()) {
		    log.info("Check managed cache and DB Update.");
		    System.out.println("Check managed cache and DB Update.");
		    Timestamp ts = rs.getTimestamp(1);

		    log.info("Check managed cache and DB Update. (db_ts="+ts+" cache_ts="+localUpdateTime+")");
		    System.out.println("Check managed cache and DB Update. (db_ts="+ts+" cache_ts="+localUpdateTime+")");

		    // if the timestamp has changed, set the new timestamp and flush managed cache
		    if(!ts.equals(localUpdateTime)) {
			//set the current update timestamp
			localUpdateTime = ts;
			
			// call the method to flush caches
			for(String cachePrefix : cachePrefixList)
			    cacheManager.clearAllStartingWith(cachePrefix);
			
			log.info("Flushed DBUtils managed caches!");
			System.out.println("Flushed DBUtils managed caches!");
		    }
		}
	    } catch (SQLException e) {
		e.printStackTrace();
		shutdown();
	    } finally {
		try {
		    if(stmt != null) stmt.close();
		    if(rs != null) rs.close();
		    shutdown();
		} catch (SQLException sqle) {  }
	    }
	}

	public void connect() {
	    javax.naming.Context initContext;
	    try {
		log.info("Connecting for Cache management. connContext="+connContext);
		System.out.println("Connecting for Cache management. connContext="+connContext);
		initContext = new javax.naming.InitialContext();
		DataSource ds = (javax.sql.DataSource) initContext.lookup("java:comp/env/"+DBUtils.getDataSourceContext());
			//initContext.lookup("java:comp/env/"+connContext);
		conn = ds.getConnection();
		conn.setAutoCommit(false);
	    }
	    catch (Exception ex) {
		// try 
		try {
		    initContext = new javax.naming.InitialContext();
		    DataSource ds = (javax.sql.DataSource)
			    initContext.lookup(connContext);
		    conn = ds.getConnection();
		    conn.setAutoCommit(false);
		} catch (Exception e) {
		    System.err.println("Not running in Tomcat/Jetty/Glassfish or other app container?");
		    e.printStackTrace();
		}
	    }
	}
    }    
}
