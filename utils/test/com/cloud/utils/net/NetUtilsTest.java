// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// the License.  You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.utils.net;

import java.math.BigInteger;
import java.util.SortedSet;
import java.util.TreeSet;

import junit.framework.TestCase;

import org.apache.log4j.Logger;
import org.junit.Test;

import com.googlecode.ipv6.IPv6Address;

public class NetUtilsTest extends TestCase {

    private static final Logger s_logger = Logger.getLogger(NetUtilsTest.class);
    
    @Test
    public void testGetRandomIpFromCidr() {
        String cidr = "192.168.124.1";
        long ip = NetUtils.getRandomIpFromCidr(cidr, 24, new TreeSet<Long>());
        assertEquals("The ip " + NetUtils.long2Ip(ip) + " retrieved must be within the cidr " + cidr + "/24", cidr.substring(0, 12), NetUtils.long2Ip(ip).substring(0, 12));

        ip = NetUtils.getRandomIpFromCidr(cidr, 16, new TreeSet<Long>());
        assertEquals("The ip " + NetUtils.long2Ip(ip) + " retrieved must be within the cidr " + cidr + "/16", cidr.substring(0, 8), NetUtils.long2Ip(ip).substring(0, 8));

        ip = NetUtils.getRandomIpFromCidr(cidr, 8, new TreeSet<Long>());
        assertEquals("The ip " + NetUtils.long2Ip(ip) + " retrieved must be within the cidr " + cidr + "/8", cidr.substring(0, 4), NetUtils.long2Ip(ip).substring(0, 4));

        SortedSet<Long> avoid = new TreeSet<Long>();
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

    @Test 
    public void testVpnPolicy() {
        assertTrue(NetUtils.isValidS2SVpnPolicy("aes128-sha1"));
        assertTrue(NetUtils.isValidS2SVpnPolicy("3des-sha1"));
        assertTrue(NetUtils.isValidS2SVpnPolicy("3des-sha1,aes256-sha1"));
        assertTrue(NetUtils.isValidS2SVpnPolicy("3des-md5;modp1024"));
        assertTrue(NetUtils.isValidS2SVpnPolicy("3des-sha1,aes128-sha1;modp1536"));
        assertFalse(NetUtils.isValidS2SVpnPolicy("des-md5;modp1024,aes128-sha1;modp1536"));
        assertFalse(NetUtils.isValidS2SVpnPolicy("des-sha1"));
        assertFalse(NetUtils.isValidS2SVpnPolicy("abc-123,ase-sha1"));
        assertFalse(NetUtils.isValidS2SVpnPolicy("de-sh,aes-sha1"));
        assertFalse(NetUtils.isValidS2SVpnPolicy(""));
        assertFalse(NetUtils.isValidS2SVpnPolicy(";modp1536"));
        assertFalse(NetUtils.isValidS2SVpnPolicy(",aes;modp1536,,,"));
    }
    
    public void testIpv6() {
    	assertTrue(NetUtils.isValidIpv6("fc00::1"));
    	assertFalse(NetUtils.isValidIpv6(""));
    	assertFalse(NetUtils.isValidIpv6(null));
    	assertFalse(NetUtils.isValidIpv6("1234:5678::1/64"));
    	assertTrue(NetUtils.isValidIp6Cidr("1234:5678::1/64"));
    	assertFalse(NetUtils.isValidIp6Cidr("1234:5678::1"));
    	assertEquals(NetUtils.getIp6CidrSize("1234:5678::1/32"), 32);
    	assertEquals(NetUtils.getIp6CidrSize("1234:5678::1"), 0);
    	BigInteger two = new BigInteger("2");
    	assertEquals(NetUtils.countIp6InRange("1234:5678::1-1234:5678::2"), two);
    	assertEquals(NetUtils.countIp6InRange("1234:5678::2-1234:5678::0"), null);
    	assertEquals(NetUtils.getIp6FromRange("1234:5678::1-1234:5678::1"), "1234:5678::1");
    	for (int i = 0; i < 5; i ++) {
    		String ip = NetUtils.getIp6FromRange("1234:5678::1-1234:5678::2");
    		assertTrue(ip.equals("1234:5678::1") || ip.equals("1234:5678::2"));
    		s_logger.info("IP is " + ip);
    	}
    	String ipString = null;
    	IPv6Address ipStart = IPv6Address.fromString("1234:5678::1");
    	IPv6Address ipEnd = IPv6Address.fromString("1234:5678::ffff:ffff:ffff:ffff");
    	for (int i = 0; i < 10; i ++) {
    		ipString = NetUtils.getIp6FromRange(ipStart.toString() + "-" + ipEnd.toString());
    		s_logger.info("IP is " + ipString);
    		IPv6Address ip = IPv6Address.fromString(ipString);
    		assertTrue(ip.compareTo(ipStart) >= 0);
    		assertTrue(ip.compareTo(ipEnd) <= 0);
    	}
    	//Test isIp6RangeOverlap
    	assertFalse(NetUtils.isIp6RangeOverlap("1234:5678::1-1234:5678::ffff", "1234:5678:1::1-1234:5678:1::ffff"));
    	assertTrue(NetUtils.isIp6RangeOverlap("1234:5678::1-1234:5678::ffff", "1234:5678::2-1234:5678::f"));
    	assertTrue(NetUtils.isIp6RangeOverlap("1234:5678::f-1234:5678::ffff", "1234:5678::2-1234:5678::f"));
    	assertFalse(NetUtils.isIp6RangeOverlap("1234:5678::f-1234:5678::ffff", "1234:5678::2-1234:5678::e"));
    	assertFalse(NetUtils.isIp6RangeOverlap("1234:5678::f-1234:5678::f", "1234:5678::2-1234:5678::e"));
    	//Test getNextIp6InRange
    	String range = "1234:5678::1-1234:5678::8000:0000";
    	assertEquals(NetUtils.getNextIp6InRange("1234:5678::8000:0", range), "1234:5678::1");
    	assertEquals(NetUtils.getNextIp6InRange("1234:5678::7fff:ffff", range), "1234:5678::8000:0");
    	assertEquals(NetUtils.getNextIp6InRange("1234:5678::1", range), "1234:5678::2");
    	range = "1234:5678::1-1234:5678::ffff:ffff:ffff:ffff";
    	assertEquals(NetUtils.getNextIp6InRange("1234:5678::ffff:ffff:ffff:ffff", range), "1234:5678::1");
    	//Test isIp6InNetwork
    	assertFalse(NetUtils.isIp6InNetwork("1234:5678:abcd::1", "1234:5678::/64"));
    	assertTrue(NetUtils.isIp6InNetwork("1234:5678::1", "1234:5678::/64"));
    	assertTrue(NetUtils.isIp6InNetwork("1234:5678::ffff:ffff:ffff:ffff", "1234:5678::/64"));
    	assertTrue(NetUtils.isIp6InNetwork("1234:5678::", "1234:5678::/64"));
    }
}
