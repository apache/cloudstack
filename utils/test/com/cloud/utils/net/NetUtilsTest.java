package com.cloud.utils.net;

import java.util.Set;
import java.util.TreeSet;

import junit.framework.TestCase;

import org.junit.Test;

public class NetUtilsTest extends TestCase {

    @Test
    public void testGetRandomIpFromCidr() {
        String cidr = "192.168.124.1";
        long ip = NetUtils.getRandomIpFromCidr(cidr, 24, new TreeSet<Long>());
        assertEquals("The ip " + NetUtils.long2Ip(ip) + " retrieved must be within the cidr " + cidr + "/24", cidr.substring(0, 12), NetUtils.long2Ip(ip).substring(0, 12));

        ip = NetUtils.getRandomIpFromCidr(cidr, 16, new TreeSet<Long>());
        assertEquals("The ip " + NetUtils.long2Ip(ip) + " retrieved must be within the cidr " + cidr + "/16", cidr.substring(0, 8), NetUtils.long2Ip(ip).substring(0, 8));

        ip = NetUtils.getRandomIpFromCidr(cidr, 8, new TreeSet<Long>());
        assertEquals("The ip " + NetUtils.long2Ip(ip) + " retrieved must be within the cidr " + cidr + "/8", cidr.substring(0, 4), NetUtils.long2Ip(ip).substring(0, 4));

        Set<Long> avoid = new TreeSet<Long>();
        ip = NetUtils.getRandomIpFromCidr(cidr, 30, avoid);
        assertTrue("We should be able to retrieve an ip on the first call.", ip != -1);
        avoid.add(ip);
        ip = NetUtils.getRandomIpFromCidr(cidr, 30, avoid);
        assertTrue("We should be able to retrieve an ip on the second call.", ip != -1);
        assertTrue("ip returned is not in the avoid list", !avoid.contains(ip));
        avoid.add(ip);
        ip = NetUtils.getRandomIpFromCidr(cidr, 30, avoid);
        assertTrue("We should be able to retrieve an ip on the third call.", ip != -1);
        assertTrue("ip returned is not in the avoid list", !avoid.contains(ip));
        avoid.add(ip);
        ip = NetUtils.getRandomIpFromCidr(cidr, 30, avoid);
        assertEquals("This should be -1 because we ran out of ip addresses: " + ip, ip, -1);
    }
}
