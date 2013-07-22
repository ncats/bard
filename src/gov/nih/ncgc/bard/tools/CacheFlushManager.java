package gov.nih.ncgc.bard.tools;

import java.util.Arrays;
import java.util.Vector;

import net.sf.ehcache.CacheManager;

import org.eclipse.jetty.util.log.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.client.ClientConfig;
import com.hazelcast.client.HazelcastClient;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;

/**
 * 
 * @author braistedjc
 *
 */
public class CacheFlushManager implements MessageListener <String> {

    private static CacheManager cacheManager;
    private Vector <String> cachePrefixList;
    private boolean flushAll;
    private static HazelcastClient client;
    private static Logger log;
    
    public CacheFlushManager (CacheManager cacheMgr) {
	cacheManager = cacheMgr;
	log = LoggerFactory.getLogger(CacheFlushManager.class.getName());
    }

    /**
     * Sets the cache prefixes to manage, delivers the Hazelcast cluster ip list, 
     * @param cachePrefixes
     * @param clusterIpList
     * @param flushAll
     */
    public void manage(Vector <String> cachePrefixes, String clusterIpList, boolean flushAll) {

	//set the scope of flushing.
	this.flushAll = flushAll;

	//prefixes to manager
	this.cachePrefixList = cachePrefixes;

	//parse the ip list
	String [] clusterIPs = clusterIpList.split(",");
	//client config, holds list of cluster ips (at least one node)
	ClientConfig config = new ClientConfig();
	for(String ip : clusterIPs)
	    config.addAddress(ip.trim());
	// retire an existing client if it exists.
	// this shouldn't happen since this method is called only when initializing the app/context
	if(client != null) {
	    client.shutdown();
	    client = null;
	}
	//create a client, get or make the topic to subscribe to.
	client = HazelcastClient.newHazelcastClient(new ClientConfig());
	ITopic <String> topic = client.getTopic("FLUSH_BROADCAST");
	topic.addMessageListener(this);
    }
    
    
    /**
     * Shuts down the client if exists
     */
    public void shutdown() {
	//shutdown the client
	if(client != null) {
	    client.shutdown();
	    client = null;
	}
    }
    
    /**
     * Listener method to respond a com.hazelcast.core.Message
     */
    public void onMessage(Message <String> msg) {	
	log.info("Cache flush manager received Hazelcast Message = "+msg);
	if(msg.toString().contains("FLUSH"))
	    flushCache();
    } 
    
    //flush either all caches or selected managed caches.
    private void flushCache() {
	if(!flushAll) {
	    for(String cachePrefix : cachePrefixList)
		cacheManager.clearAllStartingWith(DBUtils.CACHE_PREFIX+"::"+cachePrefix);
	} else {
	    cacheManager.clearAll();
	}
	String [] caches = cacheManager.getCacheNames();
	Arrays.sort(caches);
	String msg = "Cache Status:";
	for(String cache : caches) {
	    msg += cache + " " + cacheManager.getCache(cache).getSize() + "\n";
	}
	log.info("Cache Flush Executed");
	log.info(msg);
	log.info("DBUtils.CACHE_PREFIX="+DBUtils.CACHE_PREFIX+"  Flush all?="+flushAll);
    }
    
    public static void removeAllCachesForApplication() {
	String [] caches = cacheManager.getCacheNames();
	for(String cache : caches) {
	    cacheManager.removeCache(DBUtils.CACHE_PREFIX+"::"+cache);    
	}	
    }
}
