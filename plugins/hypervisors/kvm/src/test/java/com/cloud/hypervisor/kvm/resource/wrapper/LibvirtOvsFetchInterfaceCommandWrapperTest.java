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

package com.cloud.hypervisor.kvm.resource.wrapper;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.List;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.utils.StringUtils;
import com.cloud.utils.Ternary;

@RunWith(MockitoJUnitRunner.class)
public class LibvirtOvsFetchInterfaceCommandWrapperTest {

    @Spy
    LibvirtOvsFetchInterfaceCommandWrapper wrapper = new LibvirtOvsFetchInterfaceCommandWrapper();

    @Test
    public void testGetInterfaceDetailsValidValid() {
        String interfaceName = null;
        String ipAddress = null;
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while(interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (networkInterface.getInetAddresses().hasMoreElements() &&
                        networkInterface.getName().matches("^(eth|wl|en).*")) {
                    interfaceName = networkInterface.getName();
                    Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                    while(addresses.hasMoreElements()) {
                        InetAddress addr = addresses.nextElement();
                        if (addr instanceof Inet4Address) {
                            ipAddress  = addr.getHostAddress();
                            break;
                        };
                    }
                    if (StringUtils.isNotBlank(interfaceName) && StringUtils.isNotBlank(ipAddress)) {
                        break;
                    }
                }
            }
        } catch (SocketException ignored) {}
        Assume.assumeTrue(StringUtils.isNotBlank(interfaceName));
        Ternary<String, String, String> result = null;
        try {
            result = wrapper.getInterfaceDetails(interfaceName);
        } catch (SocketException e) {
            Assert.fail("Exception occurred: " + e.getMessage());
        }
        Assert.assertNotNull(result);
        Assert.assertEquals(ipAddress, result.first().trim());
    }

    private String getTempFilepath() {
        return String.format("%s/%s.txt", System.getProperty("java.io.tmpdir"), UUID.randomUUID());
    }

    private void runTestGetInterfaceDetailsForRandomInterfaceName(String arg) {
        try {
            Ternary<String, String, String> result = wrapper.getInterfaceDetails(arg);
            Assert.assertTrue(StringUtils.isAllEmpty(result.first(), result.second(), result.third()));
        } catch (SocketException e) {
            Assert.fail(String.format("Exception occurred: %s", e.getMessage()));
        }
    }

    @Test
    public void testGetInterfaceDetailsForRandomInterfaceName() {
        List<String> commandVariants = List.of(
                "';touch %s'",
                ";touch %s",
                "&& touch %s",
                "|| touch %s",
                UUID.randomUUID().toString());
        for (String cmd : commandVariants) {
            String filePath = getTempFilepath();
            String arg = String.format(cmd, filePath);
            runTestGetInterfaceDetailsForRandomInterfaceName(arg);
        }
    }
}
