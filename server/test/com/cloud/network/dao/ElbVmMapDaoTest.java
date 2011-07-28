package com.cloud.network.dao;

import java.util.List;

import junit.framework.TestCase;

import com.cloud.network.ElasticLbVmMapVO;
import com.cloud.network.lb.dao.ElasticLbVmMapDaoImpl;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.vm.DomainRouterVO;

public class ElbVmMapDaoTest extends TestCase {
    public void testFindByIp() {
        ElasticLbVmMapDaoImpl dao = ComponentLocator.inject(ElasticLbVmMapDaoImpl.class);
        
        ElasticLbVmMapVO map = dao.findOneByIp(3);
        if (map == null) {
           System.out.println("Not Found");   
        } else {
            System.out.println("Found");
        }
    }
    public void testFindUnused() {
        ElasticLbVmMapDaoImpl dao = ComponentLocator.inject(ElasticLbVmMapDaoImpl.class);
        
        List<DomainRouterVO> map = dao.listUnusedElbVms();
        if (map == null) {
           System.out.println("Not Found");   
        } else {
            System.out.println("Found");
        }
    }
}
