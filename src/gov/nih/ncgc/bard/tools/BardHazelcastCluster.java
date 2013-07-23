package gov.nih.ncgc.bard.tools;

import java.util.Set;

import com.hazelcast.core.Cluster;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.Member;

public class BardHazelcastCluster {

    public static void main(String[] args) {
	
	//the cluster configuration is in an xml file used when launched and holds ip addresses for the cluster
	//and other details.
	
	HazelcastInstance inst = Hazelcast.newHazelcastInstance();
	Cluster cluster = inst.getCluster();
        Set <Member> members = cluster.getMembers();
        
        //verify cluster members
        for(Member member : members) {
            System.out.println("member:"+member.getInetSocketAddress().getAddress()+" port="+member.getInetSocketAddress().getPort());
        }
    }
}