package com.cloud.network.dao;

import junit.framework.TestCase;

import com.cloud.network.ElasticLbVmMapVO;
import com.cloud.network.lb.dao.ElasticLbVmMapDaoImpl;
import com.cloud.utils.component.ComponentLocator;

public class ElbVmMapDaoTest extends TestCase {
    public void testTags() {
        ElasticLbVmMapDaoImpl dao = ComponentLocator.inject(ElasticLbVmMapDaoImpl.class);
        
        ElasticLbVmMapVO map = dao.findOneByIp(3);
        if (map == null) {
           System.out.println("Not Found");   
        } else {
            System.out.println("Found");
        }
    }
}
