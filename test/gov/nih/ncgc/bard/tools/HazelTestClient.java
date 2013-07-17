package gov.nih.ncgc.bard.tools;

import java.util.Map;

import com.hazelcast.client.ClientConfig;
import com.hazelcast.client.HazelcastClient;
import com.hazelcast.core.ITopic;

public class HazelTestClient {

    public void testAccess() {

        ClientConfig clientConfig = new ClientConfig();
        clientConfig.getGroupConfig().setName("dev").setPassword("dev-pass");
        clientConfig.addAddress("10.134.20.33", "10.134.20.95");
	HazelcastClient client = HazelcastClient.newHazelcastClient(clientConfig);
	Map <Integer, String> mapCustomers = client.getMap("customers");
        mapCustomers.put(51, "Joey");
        mapCustomers.put(52, "Olivia");
        mapCustomers.put(53, "Alvin");
        client.getMultiMap("customers");
        System.out.println("In client: Customer with key 1: "+ mapCustomers.get(1));
        System.out.println("Dump cache all customers");
        for(String val : mapCustomers.values()) {
            System.out.println("client values in cache ="+val);
        }        
        System.out.println("In client: Map Size:" + mapCustomers.size());
    
        Map <Integer, String> newCache = client.getMap("new_cache");
        newCache.put(101, "Lincoln");
        newCache.put(102, "Jefferson");
        System.out.println("New cache size="+newCache.size());        
    }
    
    
    public void testFlush() {
        ClientConfig clientConfig = new ClientConfig();
        //clientConfig.getGroupConfig().setName("dev").setPassword("dev-pass");
        clientConfig.addAddress("10.134.20.207");
	HazelcastClient client = HazelcastClient.newHazelcastClient(clientConfig);

        ITopic <String> topic = client.getTopic("FLUSH_BROADCAST");
        topic.publish("FLUSH");
        
        client.shutdown();
    }
    
    public static void main(String [] args) {
	HazelTestClient test = new HazelTestClient();
	test.testFlush();
    }
    
}
