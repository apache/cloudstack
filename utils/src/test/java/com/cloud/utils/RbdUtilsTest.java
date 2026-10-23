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

package com.cloud.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class RbdUtilsTest {

    @Test
    public void testRbdMonitorsWithoutPort() {
        assertEquals("10.0.0.1", RbdUtils.getRbdMonitors("10.0.0.1", 0));
        assertEquals("10.0.0.1,10.0.0.2,10.0.0.3", RbdUtils.getRbdMonitors("10.0.0.1,10.0.0.2,10.0.0.3", 0));
        assertEquals("fd00::1,fd00::2", RbdUtils.getRbdMonitors("fd00::1,fd00::2", 0));
    }

    @Test
    public void testRbdMonitorsIpv4WithPort() {
        assertEquals("10.0.0.1:6789", RbdUtils.getRbdMonitors("10.0.0.1", 6789));
        assertEquals("10.0.0.1:3300,10.0.0.2:3300,10.0.0.3:3300", RbdUtils.getRbdMonitors("10.0.0.1,10.0.0.2,10.0.0.3", 3300));
    }

    @Test
    public void testRbdMonitorsIpv6WithPort() {
        assertEquals("[fd00::1]:3300", RbdUtils.getRbdMonitors("fd00::1", 3300));
        assertEquals("[fd00::1]:3300,[fd00::2]:3300", RbdUtils.getRbdMonitors("fd00::1,fd00::2", 3300));
        // already enclosed in square brackets
        assertEquals("[fd00::1]:3300,[fd00::2]:3300", RbdUtils.getRbdMonitors("[fd00::1],[fd00::2]", 3300));
    }

    @Test
    public void testRbdMonitorsMixedIpv4AndIpv6WithPort() {
        assertEquals("10.0.0.1:3300,[fd00::1]:3300,[fd00::2]:3300,mon4.example.com:3300",
                RbdUtils.getRbdMonitors("10.0.0.1, fd00::1,[fd00::2] ,mon4.example.com", 3300));
    }

    @Test
    public void testRbdMonitorsMultipleIpv4AndIpv6WithoutPort() {
        // no port: the default Ceph port is used, so the monitors are passed as they are
        assertEquals("[fd00::1],[fd00::2],[fd00::3]", RbdUtils.getRbdMonitors("[fd00::1],[fd00::2],[fd00::3]", 0));
        assertEquals("10.0.0.1,10.0.0.2,fd00::1,[fd00::2]", RbdUtils.getRbdMonitors("10.0.0.1,10.0.0.2,fd00::1,[fd00::2]", 0));
        assertEquals("10.0.0.1,fd00::1", RbdUtils.getRbdMonitors("10.0.0.1,fd00::1", -1));
    }

    @Test
    public void testRbdMonitorsMultipleIpv4AndIpv6WithPort() {
        assertEquals("10.0.0.1:6789,10.0.0.2:6789,[fd00::1]:6789,[fd00::2]:6789",
                RbdUtils.getRbdMonitors("10.0.0.1,10.0.0.2,fd00::1,[fd00::2]", 6789));
        assertEquals("[fd00::1]:6789,10.0.0.1:6789,[fd00::2]:6789,10.0.0.2:6789",
                RbdUtils.getRbdMonitors("fd00::1,10.0.0.1,[fd00::2],10.0.0.2", 6789));
    }

    @Test
    public void testRbdMonitorsAreTrimmedAndEmptyEntriesSkipped() {
        assertEquals("10.0.0.1,10.0.0.2,fd00::1", RbdUtils.getRbdMonitors(" 10.0.0.1, 10.0.0.2,,fd00::1 ,", 0));
        assertEquals("10.0.0.1:6789,10.0.0.2:6789,[fd00::1]:6789", RbdUtils.getRbdMonitors(" 10.0.0.1, 10.0.0.2,,fd00::1 ,", 6789));
    }

    @Test
    public void testRbdMonitorsWhichHaveAPortAlready() {
        // the monitors keep their port, with or without a port of the pool
        String withPorts = "10.0.0.1:6789,mon.example.com:3300,[fd00::1]:6789";
        assertEquals(withPorts, RbdUtils.getRbdMonitors(withPorts, 0));
        assertEquals(withPorts, RbdUtils.getRbdMonitors(withPorts, 6789));
        assertEquals(withPorts, RbdUtils.getRbdMonitors(withPorts, 3300));
    }

    @Test
    public void testRbdMonitorsWithAndWithoutAPortAreMixed() {
        assertEquals("10.0.0.1:6789,10.0.0.2:3300,mon.example.com:3300,[fd00::1]:3300,[fd00::2]:6789,[fd00::3]:3300",
                RbdUtils.getRbdMonitors("10.0.0.1:6789,10.0.0.2,mon.example.com,fd00::1,[fd00::2]:6789,[fd00::3]", 3300));
    }

    @Test
    public void testAddPortToRbdMonitor() {
        assertEquals("10.0.0.1:6789", RbdUtils.addPortToRbdMonitor("10.0.0.1", 6789));
        assertEquals("10.0.0.1:3300", RbdUtils.addPortToRbdMonitor("10.0.0.1:3300", 6789));
        assertEquals("mon.example.com:6789", RbdUtils.addPortToRbdMonitor("mon.example.com", 6789));
        assertEquals("[fd00::1]:6789", RbdUtils.addPortToRbdMonitor("fd00::1", 6789));
        assertEquals("[fd00::1]:6789", RbdUtils.addPortToRbdMonitor("[fd00::1]", 6789));
        assertEquals("[fd00::1]:3300", RbdUtils.addPortToRbdMonitor("[fd00::1]:3300", 6789));
    }

    @Test
    public void testComposeOptionForMonHostsEscapesThePortSeparator() {
        // ':' separates the options of an rbd: string, so the one before the port is escaped.
        assertEquals("ceph-monitor\\:8000", RbdUtils.composeOptionForMonHosts("ceph-monitor", 8000));
    }

    @Test
    public void testComposeOptionForMonHostsJoinsMonitorsWithAnEscapedSemicolon() {
        assertEquals("mon1\\:3300\\;mon2\\:3300\\;mon3\\:3300",
                RbdUtils.composeOptionForMonHosts("mon1,mon2,mon3", 3300));
    }

    @Test
    public void testComposeOptionForMonHostsEscapesEveryColonOfAnIpv6Monitor() {
        // Without this the address' own colons would read as option separators.
        assertEquals("[fc00\\:1234\\:\\:1]\\:3300",
                RbdUtils.composeOptionForMonHosts("[fc00:1234::1]", 3300));
    }

    @Test
    public void testComposeOptionForMonHostsWithoutAPortLeavesTheMonitorAlone() {
        assertEquals("ceph-monitor", RbdUtils.composeOptionForMonHosts("ceph-monitor", 0));
        assertEquals("fd00\\:\\:1", RbdUtils.composeOptionForMonHosts("fd00::1", 0));
    }

    @Test
    public void testComposeOptionForMonHostsMixesIpv4AndIpv6() {
        assertEquals("10.0.0.1\\:3300\\;[fc00\\:1234\\:\\:1]\\:3300",
                RbdUtils.composeOptionForMonHosts("10.0.0.1,[fc00:1234::1]", 3300));
    }

    @Test
    public void testComposeOptionForMonHostsBracketsABareIpv6MonitorBeforeEscapingIt() {
        // Inherited from getRbdMonitors: without the brackets the address' own colons would be
        // indistinguishable from the one before the port.
        assertEquals("[fd00\\:\\:1]\\:3300",
                RbdUtils.composeOptionForMonHosts("fd00::1", 3300));
    }

    @Test
    public void testComposeOptionForMonHostsLeavesAMonitorWhichHasAPortAlready() {
        // Inherited from getRbdMonitors: the pool's port is not appended a second time.
        assertEquals("mon1\\:6789", RbdUtils.composeOptionForMonHosts("mon1:6789", 3300));
    }
}
