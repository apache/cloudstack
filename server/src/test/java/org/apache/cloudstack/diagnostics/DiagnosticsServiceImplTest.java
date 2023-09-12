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
package org.apache.cloudstack.diagnostics;

import java.util.HashMap;
import java.util.Map;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.command.admin.diagnostics.RunDiagnosticsCmd;
import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.routing.NetworkElementCommand;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.dao.VMInstanceDao;

import junit.framework.TestCase;

@RunWith(MockitoJUnitRunner.class)
public class DiagnosticsServiceImplTest extends TestCase {

    @Mock
    private AgentManager agentManager;
    @Mock
    private VMInstanceDao instanceDao;
    @Mock
    private RunDiagnosticsCmd runDiagnosticsCmd;
    @Mock
    private DiagnosticsCommand command;
    @Mock
    private VMInstanceVO vmInstanceVO;
    @Mock
    private VirtualMachineManager vmManager;
    @Mock
    private NetworkOrchestrationService networkManager;

    @InjectMocks
    private DiagnosticsServiceImpl serviceImpl = new DiagnosticsServiceImpl();

    @Before
    public void setUp() throws Exception {
        Mockito.when(runDiagnosticsCmd.getId()).thenReturn(1L);
        Mockito.when(runDiagnosticsCmd.getType()).thenReturn(DiagnosticsType.PING);
        Mockito.when(instanceDao.findByIdTypes(Mockito.anyLong(), Mockito.any(VirtualMachine.Type.class),
                Mockito.any(VirtualMachine.Type.class), Mockito.any(VirtualMachine.Type.class))).thenReturn(vmInstanceVO);
    }

    @After
    public void tearDown() throws Exception {
        Mockito.reset(runDiagnosticsCmd);
        Mockito.reset(agentManager);
        Mockito.reset(instanceDao);
        Mockito.reset(vmInstanceVO);
        Mockito.reset(command);
    }

    @Test
    public void testRunDiagnosticsCommandTrue() throws Exception {
        Mockito.when(runDiagnosticsCmd.getAddress()).thenReturn("8.8.8.8");
        Map<String, String> accessDetailsMap = new HashMap<>();
        accessDetailsMap.put(NetworkElementCommand.ROUTER_IP, "169.20.175.10");
        Mockito.when(networkManager.getSystemVMAccessDetails(Mockito.any(VMInstanceVO.class))).thenReturn(accessDetailsMap);
        final String details = "PING 8.8.8.8 (8.8.8.8) 56(84) bytes of data.\n" +
                "64 bytes from 8.8.8.8: icmp_seq=1 ttl=125 time=7.88 ms\n" +
                "64 bytes from 8.8.8.8: icmp_seq=2 ttl=125 time=251 ms\n" +
                "64 bytes from 8.8.8.8: icmp_seq=3 ttl=125 time=64.9 ms\n" +
                "64 bytes from 8.8.8.8: icmp_seq=4 ttl=125 time=50.7 ms\n" +
                "64 bytes from 8.8.8.8: icmp_seq=5 ttl=125 time=67.9 ms\n" +
                "\n" +
                "--- 8.8.8.8 ping statistics ---\n" +
                "5 packets transmitted, 5 received, 0% packet loss, time 4003ms\n" +
                "rtt min/avg/max/mdev = 7.881/88.587/251.410/84.191 ms&&\n" +
                "&&\n" +
                "0\n";

        Mockito.when(agentManager.easySend(Mockito.anyLong(), Mockito.any(DiagnosticsCommand.class))).thenReturn(new DiagnosticsAnswer(command, true, details));

        Map<String, String> detailsMap = serviceImpl.runDiagnosticsCommand(runDiagnosticsCmd);

        String stdout = "PING 8.8.8.8 (8.8.8.8) 56(84) bytes of data.\n" +
                "64 bytes from 8.8.8.8: icmp_seq=1 ttl=125 time=7.88 ms\n" +
                "64 bytes from 8.8.8.8: icmp_seq=2 ttl=125 time=251 ms\n" +
                "64 bytes from 8.8.8.8: icmp_seq=3 ttl=125 time=64.9 ms\n" +
                "64 bytes from 8.8.8.8: icmp_seq=4 ttl=125 time=50.7 ms\n" +
                "64 bytes from 8.8.8.8: icmp_seq=5 ttl=125 time=67.9 ms\n" +
                "\n" +
                "--- 8.8.8.8 ping statistics ---\n" +
                "5 packets transmitted, 5 received, 0% packet loss, time 4003ms\n" +
                "rtt min/avg/max/mdev = 7.881/88.587/251.410/84.191 ms";

        assertEquals(3, detailsMap.size());
        assertEquals("Mismatch between actual and expected STDERR", "", detailsMap.get(ApiConstants.STDERR));
        assertEquals("Mismatch between actual and expected EXITCODE", "0", detailsMap.get(ApiConstants.EXITCODE));
        assertEquals("Mismatch between actual and expected STDOUT", stdout, detailsMap.get(ApiConstants.STDOUT));
    }

    @Test
    public void testRunDiagnosticsCommandFalse() throws Exception {
        Mockito.when(runDiagnosticsCmd.getAddress()).thenReturn("192.0.2.2");

        Map<String, String> accessDetailsMap = new HashMap<>();
        accessDetailsMap.put(NetworkElementCommand.ROUTER_IP, "169.20.175.10");
        Mockito.when(networkManager.getSystemVMAccessDetails(Mockito.any(VMInstanceVO.class))).thenReturn(accessDetailsMap);

        String details = "PING 192.0.2.2 (192.0.2.2): 56 data bytes\n" +
                "76 bytes from 213.130.48.253: Destination Net Unreachable\n" +
                "--- 192.0.2.2 ping statistics ---\n" +
                "4 packets transmitted, 0 packets received, 100% packet loss&&\n" +
                "&&\n" +
                "1\n";
        String stdout = "PING 192.0.2.2 (192.0.2.2): 56 data bytes\n" +
                "76 bytes from 213.130.48.253: Destination Net Unreachable\n" +
                "--- 192.0.2.2 ping statistics ---\n" +
                "4 packets transmitted, 0 packets received, 100% packet loss";
        Mockito.when(agentManager.easySend(Mockito.anyLong(), Mockito.any(DiagnosticsCommand.class))).thenReturn(new DiagnosticsAnswer(command, true, details));

        Map<String, String> detailsMap = serviceImpl.runDiagnosticsCommand(runDiagnosticsCmd);

        assertEquals(3, detailsMap.size());
        assertEquals("Mismatch between actual and expected STDERR", "", detailsMap.get(ApiConstants.STDERR));
        assertTrue("Mismatch between actual and expected EXITCODE", !detailsMap.get(ApiConstants.EXITCODE).equalsIgnoreCase("0"));
        assertEquals("Mismatch between actual and expected STDOUT", stdout, detailsMap.get(ApiConstants.STDOUT));
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testRunDiagnosticsThrowsInvalidParamException() throws Exception {
        Mockito.when(runDiagnosticsCmd.getAddress()).thenReturn("");
        Mockito.when(instanceDao.findByIdTypes(Mockito.anyLong(), Mockito.any(VirtualMachine.Type.class),
                Mockito.any(VirtualMachine.Type.class), Mockito.any(VirtualMachine.Type.class))).thenReturn(null);

        serviceImpl.runDiagnosticsCommand(runDiagnosticsCmd);
    }

    @Test(expected = CloudRuntimeException.class)
    public void testVMControlIPisNull() throws Exception {
        Mockito.when(runDiagnosticsCmd.getAddress()).thenReturn("0.42.42.42");

        Map<String, String> accessDetailsMap = new HashMap<>();
        accessDetailsMap.put(NetworkElementCommand.ROUTER_IP, null);
        Mockito.when(networkManager.getSystemVMAccessDetails(Mockito.any(VMInstanceVO.class))).thenReturn(accessDetailsMap);

        serviceImpl.runDiagnosticsCommand(runDiagnosticsCmd);
    }

    @Test
    public void testInvalidCharsInParams() throws Exception {
        assertFalse(serviceImpl.hasValidChars("'\\''"));
        assertFalse(serviceImpl.hasValidChars("-I eth0 &"));
        assertFalse(serviceImpl.hasValidChars("-I eth0 ;"));
        assertFalse(serviceImpl.hasValidChars(" &2 > "));
        assertFalse(serviceImpl.hasValidChars(" &2 >> "));
        assertFalse(serviceImpl.hasValidChars(" | "));
        assertFalse(serviceImpl.hasValidChars("|"));
        assertFalse(serviceImpl.hasValidChars(","));
    }

    @Test
    public void testValidCharsInParams() throws Exception {
        assertTrue(serviceImpl.hasValidChars(""));
        assertTrue(serviceImpl.hasValidChars("."));
        assertTrue(serviceImpl.hasValidChars(" "));
        assertTrue(serviceImpl.hasValidChars("-I eth0 www.google.com"));
        assertTrue(serviceImpl.hasValidChars(" "));
        assertTrue(serviceImpl.hasValidChars(" -I cloudbr0 --sport "));
        assertTrue(serviceImpl.hasValidChars(" --back -m20 "));
        assertTrue(serviceImpl.hasValidChars("-c 5 -4"));
        assertTrue(serviceImpl.hasValidChars("-c 5 -4 -AbDfhqUV"));
    }

}
