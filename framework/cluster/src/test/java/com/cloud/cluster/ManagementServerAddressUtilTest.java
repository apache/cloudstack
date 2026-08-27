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

package com.cloud.cluster;

import org.apache.cloudstack.config.ApiServiceConfiguration;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.junit.Test;

import java.lang.reflect.Field;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ManagementServerAddressUtilTest {

    private void setConfigValue(final String value) throws IllegalAccessException, NoSuchFieldException {
        final Field f = ConfigKey.class.getDeclaredField("_defaultValue");
        f.setAccessible(true);
        f.set(ApiServiceConfiguration.ManagementServerAddresses, value);
    }

    @Test
    public void testAllHostnames() throws Exception {
        setConfigValue("ms1.example.com,ms2.example.com,ms3.example.com");
        assertTrue(ManagementServerAddressUtil.isManagementServerAddressListUsingHostnames());
    }

    @Test
    public void testAllIPv4() throws Exception {
        setConfigValue("192.168.1.1,192.168.1.2,192.168.1.3");
        assertFalse(ManagementServerAddressUtil.isManagementServerAddressListUsingHostnames());
    }

    @Test
    public void testAllIPv6() throws Exception {
        setConfigValue("2001:db8::1,2001:db8::2,2001:db8::3");
        assertFalse(ManagementServerAddressUtil.isManagementServerAddressListUsingHostnames());
    }

    @Test
    public void testMixedHostnamesAndIPs() throws Exception {
        // Mixed format should return false
        setConfigValue("ms1.example.com,192.168.1.1");
        assertFalse(ManagementServerAddressUtil.isManagementServerAddressListUsingHostnames());
    }

    @Test
    public void testEmptyString() throws Exception {
        setConfigValue("");
        assertFalse(ManagementServerAddressUtil.isManagementServerAddressListUsingHostnames());
    }

    @Test
    public void testNull() throws Exception {
        setConfigValue(null);
        assertFalse(ManagementServerAddressUtil.isManagementServerAddressListUsingHostnames());
    }

    @Test
    public void testSingleHostname() throws Exception {
        setConfigValue("management-server.example.com");
        assertTrue(ManagementServerAddressUtil.isManagementServerAddressListUsingHostnames());
    }

    @Test
    public void testSingleIPv4() throws Exception {
        setConfigValue("10.0.0.1");
        assertFalse(ManagementServerAddressUtil.isManagementServerAddressListUsingHostnames());
    }

    @Test
    public void testHostnamesWithSpaces() throws Exception {
        setConfigValue("ms1.example.com, ms2.example.com, ms3.example.com");
        assertTrue(ManagementServerAddressUtil.isManagementServerAddressListUsingHostnames());
    }

    @Test
    public void testIPv4WithSpaces() throws Exception {
        setConfigValue("192.168.1.1, 192.168.1.2, 192.168.1.3");
        assertFalse(ManagementServerAddressUtil.isManagementServerAddressListUsingHostnames());
    }

    @Test
    public void testSimpleHostnames() throws Exception {
        setConfigValue("server1,server2,server3");
        assertTrue(ManagementServerAddressUtil.isManagementServerAddressListUsingHostnames());
    }

    @Test
    public void testFQDNWithHyphens() throws Exception {
        setConfigValue("ms-1.example.com,ms-2.example.com");
        assertTrue(ManagementServerAddressUtil.isManagementServerAddressListUsingHostnames());
    }

    @Test
    public void testHostnameWithNumbers() throws Exception {
        setConfigValue("ms1.example.com,ms2.example.com");
        assertTrue(ManagementServerAddressUtil.isManagementServerAddressListUsingHostnames());
    }

    @Test
    public void testInvalidHostname_TooLong() throws Exception {
        // Label longer than 63 characters should fail
        String longLabel = "a".repeat(64);
        setConfigValue(longLabel + ".example.com");
        assertFalse(ManagementServerAddressUtil.isManagementServerAddressListUsingHostnames());
    }

    @Test
    public void testInvalidHostname_InvalidCharacters() throws Exception {
        // Underscores are technically not valid in hostnames per RFC 1123
        setConfigValue("server_1.example.com");
        assertFalse(ManagementServerAddressUtil.isManagementServerAddressListUsingHostnames());
    }

    @Test
    public void testValidHostname_MaxLabelLength() throws Exception {
        // 63 characters is the max label length
        String maxLabel = "a".repeat(63);
        setConfigValue(maxLabel + ".example.com");
        assertTrue(ManagementServerAddressUtil.isManagementServerAddressListUsingHostnames());
    }

    @Test
    public void testLocalhostIP() throws Exception {
        setConfigValue("127.0.0.1");
        assertFalse(ManagementServerAddressUtil.isManagementServerAddressListUsingHostnames());
    }

    @Test
    public void testLocalhostHostname() throws Exception {
        setConfigValue("localhost");
        assertTrue(ManagementServerAddressUtil.isManagementServerAddressListUsingHostnames());
    }

    @Test
    public void testIPv6Localhost() throws Exception {
        setConfigValue("::1");
        assertFalse(ManagementServerAddressUtil.isManagementServerAddressListUsingHostnames());
    }
}
