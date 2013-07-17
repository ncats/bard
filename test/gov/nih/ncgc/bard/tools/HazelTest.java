package gov.nih.ncgc.bard.tools;

import java.util.Map;
import java.util.Queue;
import java.util.Set;

import com.hazelcast.core.Cluster;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.Member;

public class HazelTest {

    public static void main(String[] args) {
        Map<Integer, String> mapCustomers = Hazelcast.getMap("customers");
        Cluster cluster = Hazelcast.getCluster();
        mapCustomers.put(1, "Joe");
        mapCustomers.put(2, "Ali");
        mapCustomers.put(3, "Avi");

        System.out.println("Customer with key 1: "+ mapCustomers.get(1));
        System.out.println("Map Size:" + mapCustomers.size());

        Queue<String> queueCustomers = Hazelcast.getQueue("customers");
        queueCustomers.offer("Tom");
        queueCustomers.offer("Mary");
        queueCustomers.offer("Jane");
        System.out.println("First customer: " + queueCustomers.poll());
        System.out.println("Second customer: "+ queueCustomers.peek());
        System.out.println("Queue size: " + queueCustomers.size());
        
        Set <Member> members = cluster.getMembers();
        for(Member member : members) {
            System.out.println("member:"+member.getInetAddress()+" port="+member.getPort());
        }
    }
}