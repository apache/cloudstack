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
package org.apache.cloudstack.utils.linux;

import org.apache.commons.lang.SystemUtils;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.libvirt.Connect;
import org.libvirt.NodeInfo;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.cloud.hypervisor.kvm.resource.LibvirtConnection;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class KVMHostInfoTest {
    @Test
    public void getCpuSpeed() {
        if (!System.getProperty("os.name").equals("Linux")) {
            return;
        }
        Assume.assumeTrue(SystemUtils.IS_OS_LINUX);
        NodeInfo nodeInfo = Mockito.mock(NodeInfo.class);
        nodeInfo.mhz = 1000;
        Assert.assertTrue(KVMHostInfo.getCpuSpeed(null, nodeInfo) > 0L);
    }

    @Test
    public void getCpuSpeedFromHostCapabilities() {
        String capabilities = "<host>\n" +
                "<uuid>8a330742-345f-b0df-7954-c9960b88116c</uuid>\n" +
                "  <cpu>\n" +
                "    <arch>x86_64</arch>\n" +
                "    <model>Opteron_G2</model>\n" +
                "    <vendor>AMD</vendor>\n" +
                "    <counter name='tsc' frequency='2350000000' scaling='no'/>\n" +
                "  </cpu>\n" +
                "</host>\n";
        Assert.assertEquals(2350L, KVMHostInfo.getCpuSpeedFromHostCapabilities(capabilities));
    }

    @Test
    public void manualCpuSpeedTest() throws Exception {
        if (!System.getProperty("os.name").equals("Linux")) {
            return;
        }
        try (MockedStatic<LibvirtConnection> ignored = Mockito.mockStatic(LibvirtConnection.class)) {
            Connect conn = Mockito.mock(Connect.class);
            NodeInfo nodeInfo = Mockito.mock(NodeInfo.class);
            nodeInfo.mhz = 1000;
            String capabilitiesXml = "<capabilities></capabilities>";

            Mockito.when(LibvirtConnection.getConnection()).thenReturn(conn);
            Mockito.when(conn.nodeInfo()).thenReturn(nodeInfo);
            Mockito.when(conn.getCapabilities()).thenReturn(capabilitiesXml);
            Mockito.when(conn.close()).thenReturn(0);
            int manualSpeed = 500;

            KVMHostInfo kvmHostInfo = new KVMHostInfo(10, 10, manualSpeed, 0);
            Assert.assertEquals(kvmHostInfo.getCpuSpeed(), manualSpeed);
        }
    }

    @Test
    public void reservedCpuCoresTest() throws Exception {
        if (!System.getProperty("os.name").equals("Linux")) {
            return;
        }
        try (MockedStatic<LibvirtConnection> ignored = Mockito.mockStatic(LibvirtConnection.class)) {
            Connect conn = Mockito.mock(Connect.class);
            NodeInfo nodeInfo = Mockito.mock(NodeInfo.class);
            nodeInfo.cpus = 10;
            String capabilitiesXml = "<capabilities></capabilities>";

            Mockito.when(LibvirtConnection.getConnection()).thenReturn(conn);
            Mockito.when(conn.nodeInfo()).thenReturn(nodeInfo);
            Mockito.when(conn.getCapabilities()).thenReturn(capabilitiesXml);
            Mockito.when(conn.close()).thenReturn(0);
            int manualSpeed = 500;

            KVMHostInfo kvmHostInfo = new KVMHostInfo(10, 10, 100, 2);
            Assert.assertEquals("reserve two CPU cores", 8, kvmHostInfo.getAllocatableCpus());

            kvmHostInfo = new KVMHostInfo(10, 10, 100, 0);
            Assert.assertEquals("no reserve CPU core setting", 10, kvmHostInfo.getAllocatableCpus());

            kvmHostInfo = new KVMHostInfo(10, 10, 100, 12);
            Assert.assertEquals("Misconfigured/too large CPU reserve", 0, kvmHostInfo.getAllocatableCpus());
        }
    }
}
