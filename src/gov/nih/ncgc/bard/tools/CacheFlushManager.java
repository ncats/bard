package gov.nih.ncgc.bard.tools;

import java.util.Vector;

import net.sf.ehcache.CacheManager;

import com.hazelcast.client.ClientConfig;
import com.hazelcast.client.HazelcastClient;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;

public class CacheFlushManager implements MessageListener <String> {

    private CacheManager cacheManager;
    private Vector <String> cachePrefixList;
    private boolean flushAll;
    private static HazelcastClient client;
    
    public CacheFlushManager (CacheManager cacheManager) {
	this.cacheManager = cacheManager;
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
	if(msg.toString().equals("FLUSH"))
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
    }
}
