/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.cloud.hypervisor.kvm.resource;

import com.cloud.vm.VmDetailConstants;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@RunWith(value = Parameterized.class)
public class LibvirtCpuTopologyTest {
    private final LibvirtComputingResource libvirtComputingResource = Mockito.spy(new LibvirtComputingResource());

    private final String desc;
    private final Integer coresPerSocket;
    private final Integer threadsPerCore;
    private final Integer totalVmCores;
    private final String expectedXml;

    public LibvirtCpuTopologyTest(String desc, Integer coresPerSocket, Integer threadsPerCore, Integer totalVmCores, String expectedXml) {
        this.desc = desc;
        this.coresPerSocket = coresPerSocket;
        this.threadsPerCore = threadsPerCore;
        this.totalVmCores = totalVmCores;
        this.expectedXml = expectedXml;
    }
    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                createTestData("8 cores, 2 per socket",2, null, 8, "<cpu><topology sockets='4' cores='2' threads='1' /></cpu>"),
                createTestData("8 cores, 4 per socket",4, null, 8, "<cpu><topology sockets='2' cores='4' threads='1' /></cpu>"),
                createTestData("8 cores, nothing specified",null, null, 8, "<cpu><topology sockets='2' cores='4' threads='1' /></cpu>"),
                createTestData("12 cores, nothing specified",null, null, 12, "<cpu><topology sockets='2' cores='6' threads='1' /></cpu>"),
                createTestData("8 cores, 2C per socket, 2TPC",2, 2, 8, "<cpu><topology sockets='2' cores='2' threads='2' /></cpu>"),
                createTestData("8 cores, 1C per socket, 2TPC",1, 2, 8, "<cpu><topology sockets='4' cores='1' threads='2' /></cpu>"),
                createTestData("8 cores, default CPS, 2TPC",null, 2, 8, "<cpu><topology sockets='4' cores='1' threads='2' /></cpu>"),
                createTestData("6 cores, default CPS, 2TPC",null, 2, 6, "<cpu><topology sockets='3' cores='1' threads='2' /></cpu>"),
                createTestData("12 cores, 2CPS, 2TPC",2, 2, 12, "<cpu><topology sockets='3' cores='2' threads='2' /></cpu>"),
                createTestData("6 cores, misconfigured cores, CPS, TPC, use default topology",2, 2, 6, "<cpu><topology sockets='1' cores='6' threads='1' /></cpu>"),
                createTestData("odd cores, nothing specified use default topology",null, null, 3, "<cpu><topology sockets='3' cores='1' threads='1' /></cpu>"),
                createTestData("odd cores, uneven CPS use default topology",2, null, 3, "<cpu><topology sockets='3' cores='1' threads='1' /></cpu>"),
                createTestData("8 cores, 2 CPS, odd threads use default topology", 2, 3, 8, "<cpu><topology sockets='2' cores='4' threads='1' /></cpu>"),
                createTestData("1 core, 2 CPS, odd threads use default topology", 2, 1, 1, "<cpu><topology sockets='1' cores='1' threads='1' /></cpu>")
        });
    }

    private static Object[] createTestData(String desc, Integer coresPerSocket, Integer threadsPerCore, int totalVmCores, String expected) {
        return new Object[] {desc, coresPerSocket, threadsPerCore, totalVmCores, expected};
    }

    @Test
    public void topologyTest() {
        LibvirtVMDef.CpuModeDef cpuModeDef = new LibvirtVMDef.CpuModeDef();
        Map<String, String> details = new HashMap<>();

        if (coresPerSocket != null) {
            details.put(VmDetailConstants.CPU_CORE_PER_SOCKET, coresPerSocket.toString());
        }

        if (threadsPerCore != null) {
            details.put(VmDetailConstants.CPU_THREAD_PER_CORE, threadsPerCore.toString());
        }

        if (coresPerSocket == null && threadsPerCore == null) {
            details = null;
        }

        libvirtComputingResource.setCpuTopology(cpuModeDef, totalVmCores, details);
        Assert.assertEquals(desc, expectedXml, cpuModeDef.toString());
    }
}
