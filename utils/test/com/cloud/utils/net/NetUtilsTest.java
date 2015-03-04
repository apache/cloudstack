//
// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
//

package com.cloud.utils.net;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.math.BigInteger;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.junit.Test;

import com.googlecode.ipv6.IPv6Address;

public class NetUtilsTest {

    private static final Logger s_logger = Logger.getLogger(NetUtilsTest.class);

    @Test
    public void testGetRandomIpFromCidrWithSize24() throws Exception {
        String cidr = "192.168.124.1";
        int size = 24;
        int netCharacters = 12;

        long ip = NetUtils.getRandomIpFromCidr(cidr, size, new TreeSet<Long>());

        assertThat("The ip " + NetUtils.long2Ip(ip) + " retrieved must be within the cidr " + cidr + "/" + size, cidr.substring(0, netCharacters), equalTo(NetUtils.long2Ip(ip)
                .substring(0, netCharacters)));
    }

    @Test
    public void testGetRandomIpFromCidrWithSize16() throws Exception {
        String cidr = "192.168.124.1";
        int size = 16;
        int netCharacters = 8;

        long ip = NetUtils.getRandomIpFromCidr(cidr, 16, new TreeSet<Long>());

        assertThat("The ip " + NetUtils.long2Ip(ip) + " retrieved must be within the cidr " + cidr + "/" + size, cidr.substring(0, netCharacters), equalTo(NetUtils.long2Ip(ip)
                .substring(0, netCharacters)));
    }

    @Test
    public void testGetRandomIpFromCidrWithSize8() throws Exception {
        String cidr = "192.168.124.1";
        int size = 8;
        int netCharacters = 4;

        long ip = NetUtils.getRandomIpFromCidr(cidr, 16, new TreeSet<Long>());

        assertThat("The ip " + NetUtils.long2Ip(ip) + " retrieved must be within the cidr " + cidr + "/" + size, cidr.substring(0, netCharacters), equalTo(NetUtils.long2Ip(ip)
                .substring(0, netCharacters)));
    }

    @Test
    public void testGetRandomIpFromCidrUsignAvoid() throws Exception {
        String cidr = "192.168.124.1";
        int size = 30;

        SortedSet<Long> avoid = new TreeSet<Long>();
        long ip = NetUtils.getRandomIpFromCidr(cidr, size, avoid);
        assertThat("We should be able to retrieve an ip on the first call.", ip, not(equalTo(-1L)));
        avoid.add(ip);
        ip = NetUtils.getRandomIpFromCidr(cidr, size, avoid);
        assertThat("We should be able to retrieve an ip on the second call.", ip, not(equalTo(-1L)));
        assertThat("ip returned is not in the avoid list", avoid, not(contains(ip)));
        avoid.add(ip);
        ip = NetUtils.getRandomIpFromCidr(cidr, size, avoid);
        assertThat("We should be able to retrieve an ip on the third call.", ip, not(equalTo(-1L)));
        assertThat("ip returned is not in the avoid list", avoid, not(contains(ip)));
        avoid.add(ip);
        ip = NetUtils.getRandomIpFromCidr(cidr, size, avoid);
        assertEquals("This should be -1 because we ran out of ip addresses: " + ip, ip, -1);
    }

    @Test
    public void testIsValidS2SVpnPolicy() {
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

    @Test
    public void testGetIp6FromRange() {
        assertEquals(NetUtils.getIp6FromRange("1234:5678::1-1234:5678::1"), "1234:5678::1");
        for (int i = 0; i < 5; i++) {
            String ip = NetUtils.getIp6FromRange("1234:5678::1-1234:5678::2");
            assertThat(ip, anyOf(equalTo("1234:5678::1"), equalTo("1234:5678::2")));
            s_logger.info("IP is " + ip);
        }
        String ipString = null;
        IPv6Address ipStart = IPv6Address.fromString("1234:5678::1");
        IPv6Address ipEnd = IPv6Address.fromString("1234:5678::ffff:ffff:ffff:ffff");
        for (int i = 0; i < 10; i++) {
            ipString = NetUtils.getIp6FromRange(ipStart.toString() + "-" + ipEnd.toString());
            s_logger.info("IP is " + ipString);
            IPv6Address ip = IPv6Address.fromString(ipString);
            assertThat(ip, greaterThanOrEqualTo(ipStart));
            assertThat(ip, lessThanOrEqualTo(ipEnd));
        }
    }

    @Test
    public void testCountIp6InRange() {
        assertEquals(new BigInteger("2"), NetUtils.countIp6InRange("1234:5678::1-1234:5678::2"));
    }

    @Test
    public void testCountIp6InRangeWithInvalidRange() {
        assertEquals(null, NetUtils.countIp6InRange("1234:5678::2-1234:5678::0"));
    }

    @Test
    public void testCountIp6InRangeWithNullStart() {
        assertEquals(null, NetUtils.countIp6InRange("-1234:5678::0"));
    }

    @Test
    public void testCountIp6InRangeWithNoEnd() {
        assertEquals(new BigInteger("1"), NetUtils.countIp6InRange("1234:5678::2"));
    }

    @Test
    public void testGetIp6CidrSize() {
        assertEquals(NetUtils.getIp6CidrSize("1234:5678::1/32"), 32);
        assertEquals(NetUtils.getIp6CidrSize("1234:5678::1"), 0);
    }

    @Test
    public void testIsValidIp6Cidr() {
        assertTrue(NetUtils.isValidIp6Cidr("1234:5678::1/64"));
        assertFalse(NetUtils.isValidIp6Cidr("1234:5678::1"));
    }

    @Test
    public void testIsValidIpv6() {
        assertTrue(NetUtils.isValidIpv6("fc00::1"));
        assertFalse(NetUtils.isValidIpv6(""));
        assertFalse(NetUtils.isValidIpv6(null));
        assertFalse(NetUtils.isValidIpv6("1234:5678::1/64"));
    }

    @Test
    public void testIsIp6InRange() {
        assertTrue(NetUtils.isIp6InRange("1234:5678:abcd::1", "1234:5678:abcd::1-1234:5678:abcd::1"));
        assertFalse(NetUtils.isIp6InRange("1234:5678:abcd::1", "1234:5678:abcd::2-1234:5678:abcd::1"));
        assertFalse(NetUtils.isIp6InRange("1234:5678:abcd::1", null));
        assertTrue(NetUtils.isIp6InRange("1234:5678:abcd::1", "1234:5678::1-1234:5679::1"));
    }

    @Test
    public void testIsIp6InNetwork() {
        assertFalse(NetUtils.isIp6InNetwork("1234:5678:abcd::1", "1234:5678::/64"));
        assertTrue(NetUtils.isIp6InNetwork("1234:5678::1", "1234:5678::/64"));
        assertTrue(NetUtils.isIp6InNetwork("1234:5678::ffff:ffff:ffff:ffff", "1234:5678::/64"));
        assertTrue(NetUtils.isIp6InNetwork("1234:5678::", "1234:5678::/64"));
    }

    @Test
    public void testGetNextIp6InRange() {
        String range = "1234:5678::1-1234:5678::8000:0000";
        assertEquals(NetUtils.getNextIp6InRange("1234:5678::8000:0", range), "1234:5678::1");
        assertEquals(NetUtils.getNextIp6InRange("1234:5678::7fff:ffff", range), "1234:5678::8000:0");
        assertEquals(NetUtils.getNextIp6InRange("1234:5678::1", range), "1234:5678::2");
        range = "1234:5678::1-1234:5678::ffff:ffff:ffff:ffff";
        assertEquals(NetUtils.getNextIp6InRange("1234:5678::ffff:ffff:ffff:ffff", range), "1234:5678::1");
    }

    @Test
    public void testIsIp6RangeOverlap() {
        assertFalse(NetUtils.isIp6RangeOverlap("1234:5678::1-1234:5678::ffff", "1234:5678:1::1-1234:5678:1::ffff"));
        assertTrue(NetUtils.isIp6RangeOverlap("1234:5678::1-1234:5678::ffff", "1234:5678::2-1234:5678::f"));
        assertTrue(NetUtils.isIp6RangeOverlap("1234:5678::f-1234:5678::ffff", "1234:5678::2-1234:5678::f"));
        assertFalse(NetUtils.isIp6RangeOverlap("1234:5678::f-1234:5678::ffff", "1234:5678::2-1234:5678::e"));
        assertFalse(NetUtils.isIp6RangeOverlap("1234:5678::f-1234:5678::f", "1234:5678::2-1234:5678::e"));
    }

    @Test
    public void testStandardizeIp6Address() {
        assertEquals(NetUtils.standardizeIp6Address("1234:0000:0000:5678:0000:0000:ABCD:0001"), "1234::5678:0:0:abcd:1");
        assertEquals(NetUtils.standardizeIp6Cidr("1234:0000:0000:5678:0000:0000:ABCD:0001/64"), "1234:0:0:5678::/64");
    }

    @Test
    public void testGenerateUriForPvlan() {
        assertEquals("pvlan://123-i456", NetUtils.generateUriForPvlan("123", "456").toString());
    }

    @Test
    public void testGetPrimaryPvlanFromUri() {
        assertEquals("123", NetUtils.getPrimaryPvlanFromUri(NetUtils.generateUriForPvlan("123", "456")));
    }

    @Test
    public void testGetIsolatedPvlanFromUri() {
        assertEquals("456", NetUtils.getIsolatedPvlanFromUri(NetUtils.generateUriForPvlan("123", "456")));
    }

    @Test
    public void testIsValidCIDR() throws Exception {
        //Test to check IP Range of 2 CIDR
        String cidrFirst = "10.0.144.0/20";
        String cidrSecond = "10.0.151.0/20";
        String cidrThird = "10.0.144.0/21";

        assertTrue(NetUtils.isValidCIDR(cidrFirst));
        assertTrue(NetUtils.isValidCIDR(cidrSecond));
        assertTrue(NetUtils.isValidCIDR(cidrThird));
    }

    @Test
    public void testIsValidCidrList() throws Exception {
        String cidrFirst = "10.0.144.0/20,1.2.3.4/32,5.6.7.8/24";
        String cidrSecond = "10.0.151.0/20,129.0.0.0/4";
        String cidrThird = "10.0.144.0/21";

        assertTrue(NetUtils.isValidCidrList(cidrFirst));
        assertTrue(NetUtils.isValidCidrList(cidrSecond));
        assertTrue(NetUtils.isValidCidrList(cidrThird));
    }

    @Test
    public void testIsSameIpRange() {
        String cidrFirst = "10.0.144.0/20";
        String cidrSecond = "10.0.151.0/20";
        String cidrThird = "10.0.144.0/21";

        //Check for exactly same CIDRs
        assertTrue(NetUtils.isSameIpRange(cidrFirst, cidrFirst));
        //Check for 2 different CIDRs, but same IP Range
        assertTrue(NetUtils.isSameIpRange(cidrFirst, cidrSecond));
        //Check for 2 different CIDRs and different IP Range
        assertFalse(NetUtils.isSameIpRange(cidrFirst, cidrThird));
        //Check for Incorrect format of CIDR
        assertFalse(NetUtils.isSameIpRange(cidrFirst, "10.3.6.5/50"));
    }

    @Test
    public void testGenerateMacOnIncrease() {
        String mac = "06:01:23:00:45:67";
        assertEquals("06:01:25:00:45:67", NetUtils.generateMacOnIncrease(mac, 2));
        assertEquals("06:01:33:00:45:67", NetUtils.generateMacOnIncrease(mac, 16));
        mac = "06:ff:ff:00:45:67";
        assertEquals("06:00:00:00:45:67", NetUtils.generateMacOnIncrease(mac, 1));
        assertEquals("06:00:0f:00:45:67", NetUtils.generateMacOnIncrease(mac, 16));
    }

    @Test
    public void testGetLocalIPString() {
        assertNotNull(NetUtils.getLocalIPString());
    }

    @Test
    public void testSameIsolationId() {
        assertTrue(NetUtils.isSameIsolationId("1", "vlan://1"));
        assertTrue(NetUtils.isSameIsolationId("", null));
        assertTrue(NetUtils.isSameIsolationId("UnTagged", "vlan://uNtAGGED"));
        assertFalse(NetUtils.isSameIsolationId("2", "vlan://uNtAGGED"));
        assertFalse(NetUtils.isSameIsolationId("2", "vlan://3"));
        assertFalse(NetUtils.isSameIsolationId("bla", null));
    }

    @Test
    public void testValidateGuestCidr() throws Exception {
        String guestCidr = "192.168.1.0/24";

        assertTrue(NetUtils.validateGuestCidr(guestCidr));
    }
}
