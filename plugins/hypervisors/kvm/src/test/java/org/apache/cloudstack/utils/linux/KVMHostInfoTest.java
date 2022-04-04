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

import com.cloud.hypervisor.kvm.resource.LibvirtConnection;
import org.apache.commons.lang.SystemUtils;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.Assume;
import org.junit.Assert;
import org.junit.runner.RunWith;
import org.libvirt.Connect;
import org.mockito.Mockito;

import org.libvirt.NodeInfo;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(value = {LibvirtConnection.class})
@PowerMockIgnore({"javax.xml.*", "org.w3c.dom.*", "org.apache.xerces.*", "org.xml.*"})
public class KVMHostInfoTest {
    @Test
    public void getCpuSpeed() {
        Assume.assumeTrue(SystemUtils.IS_OS_LINUX);
        NodeInfo nodeInfo = Mockito.mock(NodeInfo.class);
        nodeInfo.mhz = 1000;
        Assert.assertThat(KVMHostInfo.getCpuSpeed(nodeInfo), Matchers.greaterThan(0l));
    }

    @Test
    public void manualCpuSpeedTest() throws Exception {
        PowerMockito.mockStatic(LibvirtConnection.class);
        Connect conn = Mockito.mock(Connect.class);
        NodeInfo nodeInfo = Mockito.mock(NodeInfo.class);
        nodeInfo.mhz = 1000;
        String capabilitiesXml = "<capabilities></capabilities>";

        PowerMockito.doReturn(conn).when(LibvirtConnection.class, "getConnection");
        PowerMockito.when(conn.nodeInfo()).thenReturn(nodeInfo);
        PowerMockito.when(conn.getCapabilities()).thenReturn(capabilitiesXml);
        PowerMockito.when(conn.close()).thenReturn(0);
        int manualSpeed = 500;

        KVMHostInfo kvmHostInfo = new KVMHostInfo(10, 10, manualSpeed);
        Assert.assertEquals(kvmHostInfo.getCpuSpeed(), manualSpeed);
    }
}