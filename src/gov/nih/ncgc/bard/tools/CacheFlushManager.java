package gov.nih.ncgc.bard.tools;

import java.util.Vector;

import net.sf.ehcache.CacheManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.client.ClientConfig;
import com.hazelcast.client.HazelcastClient;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;

public class CacheFlushManager implements MessageListener <String> {

    private Logger log = LoggerFactory.getLogger(this.getClass());
    private CacheManager cacheManager;
    private Vector <String> cachePrefixList;
    private boolean flushAll;
    private HazelcastClient client;
    
    public CacheFlushManager (CacheManager cacheManager) {
	this.cacheManager = cacheManager;
    }

    /**
     * Sets the cache prefixes to manage
     * @param cachePrefixes
     * @param clusterIpList
     */
    public void manage(Vector <String> cachePrefixes, String clusterIpList, boolean flushAll) {
	    
	log.info("CacheFlushManager.manage(). Start");

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

	if(client == null) {
	    
	    log.info("CacheFlushManager.manage(), creating a client.");

	    //create a client, get or make the topic to subscribe to.
	    client = HazelcastClient.newHazelcastClient(new ClientConfig());
	    ITopic <String> topic = client.getTopic("FLUSH_BROADCAST");
	    topic.addMessageListener(this);
	     
	    log.info("CacheFlushManager.manage() Initialized topic FLUSH_BROADCAST");	    	    
	}
    }
    
    public void shutdown() {
	//shutdown the client
	if(client != null) {
	    client.shutdown();
	    client = null;
	}
	log.info("CacheFlushManager detected servlet context destoyed. Client has been shutdown.");
    }
    
    public void onMessage(Message <String> msg) {
	log.info("onMessage() in CacheFlushManager");
	System.out.println("onMessage() in CacheFlustManager");		
	msg.toString().equals("FLUSH");
	flushCache();
    } 
    
    //flush either all caches or selected managed caches.
    private void flushCache() {
	if(!flushAll) {
	    for(String cachePrefix : cachePrefixList)
		cacheManager.clearAllStartingWith(cachePrefix);
	} else {
	    cacheManager.clearAll();
	}
	log.info("Flushed DBUtils managed caches!");
	System.out.println("Flushed DBUtils managed caches!");
    }

}
