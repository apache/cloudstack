package com.cloud.network.dao;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class LoadBalancerVOTest {
    @Test
    public void testSetCidrList() {
        LoadBalancerVO loadBalancer = new LoadBalancerVO();
        String cidrList = "192.168.1.0/24,10.0.0.0/16";
        loadBalancer.setCidrList(cidrList);
        assertEquals(cidrList, loadBalancer.getCidrList());
    }

    @Test
    public void testSetCidrListEmpty() {
        LoadBalancerVO loadBalancer = new LoadBalancerVO();
        loadBalancer.setCidrList("");
        assertEquals("", loadBalancer.getCidrList());
    }

    @Test
    public void testSetCidrListNull() {
        LoadBalancerVO loadBalancer = new LoadBalancerVO();
        loadBalancer.setCidrList(null);
        assertNull(loadBalancer.getCidrList());
    }
}
