package com.cloud.network.dao;

import junit.framework.TestCase;


public class NetworkDaoTest extends TestCase {
    public void testTags() {
//        NetworkDaoImpl dao = ComponentLocator.inject(NetworkDaoImpl.class);
//        
//        dao.expunge(1001l);
//        NetworkVO network = new NetworkVO(1001, TrafficType.Control, GuestType.Shared, Mode.Dhcp, BroadcastDomainType.Native, 1, 1, 1, 1, 1001, "Name", "DisplayText", false, true, true, null, null);
//        network.setGuruName("guru_name");
//        List<String> tags = new ArrayList<String>();
//
//        tags.add("a");
//        tags.add("b");
//        network.setTags(tags);
//
//        network = dao.persist(network);
//        List<String> saveTags = network.getTags();
//        Assert.assertTrue(saveTags.size() == 2 && saveTags.contains("a") && saveTags.contains("b"));
//
//        NetworkVO retrieved = dao.findById(1001l);
//        List<String> retrievedTags = retrieved.getTags();
//        Assert.assertTrue(retrievedTags.size() == 2 && retrievedTags.contains("a") && retrievedTags.contains("b"));
//        
//        List<String> updateTags = new ArrayList<String>();
//        updateTags.add("e");
//        updateTags.add("f");
//        retrieved.setTags(updateTags);
//        dao.update(retrieved.getId(), retrieved);
//        
//        retrieved = dao.findById(1001l);
//        retrievedTags = retrieved.getTags();
//        Assert.assertTrue("Unable to retrieve back the data updated", retrievedTags.size() == 2 && retrievedTags.contains("e") && retrievedTags.contains("f"));
//        
//        dao.expunge(1001l);
    }
    
    public void testListBy() {
//        NetworkDaoImpl dao = ComponentLocator.inject(NetworkDaoImpl.class);
//        
//        dao.listBy(1l, 1l, 1l, "192.168.192.0/24");
    }

}
