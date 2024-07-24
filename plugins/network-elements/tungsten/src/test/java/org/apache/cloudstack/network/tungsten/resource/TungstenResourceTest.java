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
package org.apache.cloudstack.network.tungsten.resource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


import net.juniper.tungsten.api.ApiObjectBase;
import net.juniper.tungsten.api.ApiPropertyBase;
import net.juniper.tungsten.api.ObjectReference;
import net.juniper.tungsten.api.types.AddressGroup;
import net.juniper.tungsten.api.types.ApplicationPolicySet;
import net.juniper.tungsten.api.types.Domain;
import net.juniper.tungsten.api.types.FirewallPolicy;
import net.juniper.tungsten.api.types.FirewallRule;
import net.juniper.tungsten.api.types.FloatingIp;
import net.juniper.tungsten.api.types.FloatingIpPool;
import net.juniper.tungsten.api.types.GlobalVrouterConfig;
import net.juniper.tungsten.api.types.InstanceIp;
import net.juniper.tungsten.api.types.Loadbalancer;
import net.juniper.tungsten.api.types.LoadbalancerHealthmonitor;
import net.juniper.tungsten.api.types.LoadbalancerListener;
import net.juniper.tungsten.api.types.LoadbalancerMember;
import net.juniper.tungsten.api.types.LoadbalancerPool;
import net.juniper.tungsten.api.types.LogicalRouter;
import net.juniper.tungsten.api.types.NetworkPolicy;
import net.juniper.tungsten.api.types.Project;
import net.juniper.tungsten.api.types.SecurityGroup;
import net.juniper.tungsten.api.types.ServiceGroup;
import net.juniper.tungsten.api.types.Tag;
import net.juniper.tungsten.api.types.TagType;
import net.juniper.tungsten.api.types.VirtualMachine;
import net.juniper.tungsten.api.types.VirtualMachineInterface;
import net.juniper.tungsten.api.types.VirtualNetwork;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.network.tungsten.agent.api.AddTungstenNetworkGatewayToLogicalRouterCommand;
import org.apache.cloudstack.network.tungsten.agent.api.AddTungstenNetworkSubnetCommand;
import org.apache.cloudstack.network.tungsten.agent.api.AddTungstenPolicyRuleCommand;
import org.apache.cloudstack.network.tungsten.agent.api.AddTungstenSecondaryIpAddressCommand;
import org.apache.cloudstack.network.tungsten.agent.api.AddTungstenSecurityGroupRuleCommand;
import org.apache.cloudstack.network.tungsten.agent.api.AddTungstenVmToSecurityGroupCommand;
import org.apache.cloudstack.network.tungsten.agent.api.ApplyTungstenNetworkPolicyCommand;
import org.apache.cloudstack.network.tungsten.agent.api.ApplyTungstenPortForwardingCommand;
import org.apache.cloudstack.network.tungsten.agent.api.ApplyTungstenTagCommand;
import org.apache.cloudstack.network.tungsten.agent.api.AssignTungstenFloatingIpCommand;
import org.apache.cloudstack.network.tungsten.agent.api.ClearTungstenNetworkGatewayCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenAddressGroupCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenApplicationPolicySetCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenDomainCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenFirewallPolicyCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenFirewallRuleCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenFloatingIpCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenFloatingIpPoolCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenLogicalRouterCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenNetworkCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenNetworkLoadbalancerCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenNetworkPolicyCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenPolicyCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenProjectCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenRoutingLogicalRouterCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenSecurityGroupCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenServiceGroupCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenTagCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenTagTypeCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenVirtualMachineCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenAddressGroupCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenApplicationPolicySetCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenDomainCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenFirewallPolicyCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenFirewallRuleCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenFloatingIpCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenFloatingIpPoolCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenLoadBalancerCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenLoadBalancerListenerCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenNetworkCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenNetworkPolicyCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenObjectCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenPolicyCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenProjectCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenRoutingLogicalRouterCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenSecurityGroupCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenServiceGroupCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenTagCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenTagTypeCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenVRouterPortCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenVmCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenVmInterfaceCommand;
import org.apache.cloudstack.network.tungsten.agent.api.GetTungstenFabricNetworkCommand;
import org.apache.cloudstack.network.tungsten.agent.api.GetTungstenFloatingIpsCommand;
import org.apache.cloudstack.network.tungsten.agent.api.GetTungstenLoadBalancerCommand;
import org.apache.cloudstack.network.tungsten.agent.api.GetTungstenNatIpCommand;
import org.apache.cloudstack.network.tungsten.agent.api.GetTungstenNetworkDnsCommand;
import org.apache.cloudstack.network.tungsten.agent.api.GetTungstenPolicyCommand;
import org.apache.cloudstack.network.tungsten.agent.api.GetTungstenSecurityGroupCommand;
import org.apache.cloudstack.network.tungsten.agent.api.ListTungstenAddressGroupCommand;
import org.apache.cloudstack.network.tungsten.agent.api.ListTungstenApplicationPolicySetCommand;
import org.apache.cloudstack.network.tungsten.agent.api.ListTungstenFirewallPolicyCommand;
import org.apache.cloudstack.network.tungsten.agent.api.ListTungstenFirewallRuleCommand;
import org.apache.cloudstack.network.tungsten.agent.api.ListTungstenNetworkCommand;
import org.apache.cloudstack.network.tungsten.agent.api.ListTungstenNicCommand;
import org.apache.cloudstack.network.tungsten.agent.api.ListTungstenPolicyCommand;
import org.apache.cloudstack.network.tungsten.agent.api.ListTungstenPolicyRuleCommand;
import org.apache.cloudstack.network.tungsten.agent.api.ListTungstenRoutingLogicalRouterCommand;
import org.apache.cloudstack.network.tungsten.agent.api.ListTungstenServiceGroupCommand;
import org.apache.cloudstack.network.tungsten.agent.api.ListTungstenTagCommand;
import org.apache.cloudstack.network.tungsten.agent.api.ListTungstenTagTypeCommand;
import org.apache.cloudstack.network.tungsten.agent.api.ListTungstenVmCommand;
import org.apache.cloudstack.network.tungsten.agent.api.ReleaseTungstenFloatingIpCommand;
import org.apache.cloudstack.network.tungsten.agent.api.RemoveTungstenNetworkGatewayFromLogicalRouterCommand;
import org.apache.cloudstack.network.tungsten.agent.api.RemoveTungstenNetworkSubnetCommand;
import org.apache.cloudstack.network.tungsten.agent.api.RemoveTungstenPolicyCommand;
import org.apache.cloudstack.network.tungsten.agent.api.RemoveTungstenPolicyRuleCommand;
import org.apache.cloudstack.network.tungsten.agent.api.RemoveTungstenSecondaryIpAddressCommand;
import org.apache.cloudstack.network.tungsten.agent.api.RemoveTungstenSecurityGroupRuleCommand;
import org.apache.cloudstack.network.tungsten.agent.api.RemoveTungstenTagCommand;
import org.apache.cloudstack.network.tungsten.agent.api.RemoveTungstenVmFromSecurityGroupCommand;
import org.apache.cloudstack.network.tungsten.agent.api.SetTungstenNetworkGatewayCommand;
import org.apache.cloudstack.network.tungsten.agent.api.TungstenAnswer;
import org.apache.cloudstack.network.tungsten.agent.api.TungstenCommand;
import org.apache.cloudstack.network.tungsten.agent.api.UpdateLoadBalancerServiceInstanceCommand;
import org.apache.cloudstack.network.tungsten.agent.api.UpdateTungstenDefaultSecurityGroupCommand;
import org.apache.cloudstack.network.tungsten.agent.api.UpdateTungstenLoadBalancerListenerCommand;
import org.apache.cloudstack.network.tungsten.agent.api.UpdateTungstenLoadBalancerMemberCommand;
import org.apache.cloudstack.network.tungsten.agent.api.UpdateTungstenLoadBalancerPoolCommand;
import org.apache.cloudstack.network.tungsten.agent.api.UpdateTungstenVrouterConfigCommand;
import org.apache.cloudstack.network.tungsten.model.TungstenLoadBalancerMember;
import org.apache.cloudstack.network.tungsten.model.TungstenNetworkPolicy;
import org.apache.cloudstack.network.tungsten.model.TungstenRule;
import org.apache.cloudstack.network.tungsten.model.TungstenTag;
import org.apache.cloudstack.network.tungsten.service.TungstenApi;
import org.apache.cloudstack.network.tungsten.service.TungstenVRouterApi;
import org.apache.cloudstack.network.tungsten.vrouter.Port;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.naming.ConfigurationException;

@RunWith(MockitoJUnitRunner.class)
public class TungstenResourceTest {
    @Mock
    TungstenApi tungstenApi;

    TungstenResource tungstenResource;

    MockedStatic<TungstenVRouterApi> tungstenVRouterApiMocked;

    AutoCloseable closeable;

    @Before
    public void setup() {
        closeable = MockitoAnnotations.openMocks(this);
        tungstenResource = new TungstenResource();
        tungstenResource.tungstenApi = tungstenApi;
        ReflectionTestUtils.setField(tungstenResource, "vrouterPort", "9091");
        tungstenVRouterApiMocked = Mockito.mockStatic(TungstenVRouterApi.class);

        Project project = mock(Project.class);
        when(project.getUuid()).thenReturn("065eab99-b819-4f3f-8e97-99c2ab22e6ed");
        when(tungstenApi.getTungstenProjectByFqn(any())).thenReturn(project);
    }

    @After
    public void tearDown() throws Exception {
        closeable.close();
        tungstenVRouterApiMocked.close();
    }

    @Test
    public void configureTest() throws ConfigurationException {
        Map<String, Object> map = new HashMap<>();
        map.put("name", "testName");
        map.put("guid", "097fe069-5a08-4fc5-a995-5d0f5e3685c6");
        map.put("zoneId", "1");
        map.put("hostname", "host1");
        map.put("port", "8082");
        map.put("vrouterPort", "9091");
        map.put("introspectPort", "8085");

        assertTrue(tungstenResource.configure("tungsten", map));
    }

    @Test
    public void configureFailTest() {
        Map<String, Object> map = new HashMap<>();
        assertThrows(ConfigurationException.class, () -> tungstenResource.configure("tungsten", map));
    }

    @Test
    public void getCurrentStatusSuccessTest() {
        assertNotNull(tungstenResource.getCurrentStatus(1L));
    }

    @Test
    public void getCurrentStatusFailTest() {
        doThrow(ServerApiException.class).when(tungstenApi).checkTungstenProviderConnection();
        assertNull(tungstenResource.getCurrentStatus(1L));
    }

    @Test
    public void executeRequestCreateTungstenNetworkCommandTest() {
        TungstenCommand command = new CreateTungstenNetworkCommand("e8281cd6-9078-4db1-9f47-e52f679e03d1",
            "testNetworkName", "testNetworkDisplayName", "projectFqn", false, false, "192.168.1.0", 24, "192.168.1.1",
            true, "192.168.1.253", "192.168.1.100", "192.168.1.200", false, false, "subnet");
        VirtualNetwork virtualNetwork = mock(VirtualNetwork.class);
        when(tungstenApi.createTungstenNetwork(anyString(), anyString(), anyString(), anyString(), anyBoolean(),
            anyBoolean(), anyString(), anyInt(), anyString(), anyBoolean(), anyString(), anyString(), anyString(),
            anyBoolean(), anyBoolean(), anyString())).thenReturn(virtualNetwork);
        TungstenAnswer answer = (TungstenAnswer) tungstenResource.executeRequest(command);
        assertTrue(answer.getResult());
        assertEquals(virtualNetwork, answer.getApiObjectBase());
    }

    @Test
    public void executeRequestDeleteTungstenNetworkCommandTest() {
        TungstenCommand command = new DeleteTungstenNetworkCommand("e8281cd6-9078-4db1-9f47-e52f679e03d1");

        when(tungstenApi.deleteTungstenObject(eq(VirtualNetwork.class), anyString())).thenReturn(true);

        TungstenAnswer answer = (TungstenAnswer) tungstenResource.executeRequest(command);
        assertTrue(answer.getResult());
    }

    @Test
    public void executeRequestDeleteTungstenVmInterfaceCommandTest() {
        TungstenCommand command = new DeleteTungstenVmInterfaceCommand("projectFqn", "vmi");
        VirtualMachineInterface virtualMachineInterface = mock(VirtualMachineInterface.class);

        when(tungstenApi.getTungstenObjectByName(eq(VirtualMachineInterface.class), any(), anyString())).thenReturn(
            virtualMachineInterface);
        when(tungstenApi.deleteTungstenVmInterface(any(VirtualMachineInterface.class))).thenReturn(true);

        TungstenAnswer answer = (TungstenAnswer) tungstenResource.executeRequest(command);
        assertTrue(answer.getResult());
    }

    @Test
    public void executeRequestDeleteTungstenVmCommandTest() {
        TungstenCommand command = new DeleteTungstenVmCommand("e8281cd6-9078-4db1-9f47-e52f679e03d1");

        when(tungstenApi.deleteTungstenObject(eq(VirtualMachine.class), anyString())).thenReturn(true);

        TungstenAnswer answer = (TungstenAnswer) tungstenResource.executeRequest(command);
        assertTrue(answer.getResult());
    }

    @Test
    public void executeRequestCreateTungstenLogicalRouterCommandTest() {
        TungstenCommand command = new CreateTungstenLogicalRouterCommand("logicalRouter", "projectFqn",
            "e8281cd6-9078-4db1-9f47-e52f679e03d1");
        LogicalRouter logicalRouter = mock(LogicalRouter.class);

        when(tungstenApi.createTungstenLogicalRouter(anyString(), anyString(), anyString())).thenReturn(logicalRouter);

        TungstenAnswer answer = (TungstenAnswer) tungstenResource.executeRequest(command);
        assertTrue(answer.getResult());
        assertEquals(logicalRouter, answer.getApiObjectBase());
    }

    @Test
    public void executeRequestCreateTungstenVirtualMachineCommandTest() {
        TungstenCommand command = new CreateTungstenVirtualMachineCommand("projectFqn",
            "e8281cd6-9078-4db1-9f47-e52f679e03d1", "5a27fad7-a8ca-4919-bfc3-cad26374b26a", "vmName",
            "fe832e34-1bbb-4f0b-9ced-0e7ae2218598", 1L, "192.168.100.1", "fd00::1", "1e:00:d2:00:00:06", "guest",
            "guest", "10.1.1.100", "10.1.1.1", true);
        VirtualMachineInterface virtualMachineInterface = mock(VirtualMachineInterface.class);
        VirtualMachine virtualMachine = mock(VirtualMachine.class);
        VirtualNetwork virtualNetwork = mock(VirtualNetwork.class);
        InstanceIp instanceIp = mock(InstanceIp.class);

        when(virtualMachineInterface.getUuid()).thenReturn("b604c7f7-1dbc-42d8-bceb-2c0898034a7a");
        when(tungstenApi.getTungstenObject(eq(VirtualNetwork.class), anyString())).thenReturn(virtualNetwork);
        when(tungstenApi.createTungstenVirtualMachine(anyString(), anyString())).thenReturn(virtualMachine);
        when(tungstenApi.createTungstenVmInterface(anyString(), anyString(), anyString(), anyString(), anyString(),
            anyString(), anyString(), anyBoolean())).thenReturn(virtualMachineInterface);
        when(tungstenApi.createTungstenInstanceIp(anyString(), anyString(), anyString(), anyString())).thenReturn(
            instanceIp);
        tungstenVRouterApiMocked.when(
                () -> TungstenVRouterApi.addTungstenVrouterPort(anyString(), anyString(), any(Port.class))
                                     ).thenReturn(true);

        TungstenAnswer answer = (TungstenAnswer) tungstenResource.executeRequest(command);
        assertTrue(answer.getResult());
        assertEquals(virtualMachine, answer.getApiObjectBase());
    }

    @Test
    public void executeRequestSetTungstenNetworkGatewayCommandTest() {
        TungstenCommand command = new SetTungstenNetworkGatewayCommand("projectFqn", "router", 1L,
            "b604c7f7-1dbc-42d8-bceb-2c0898034a7a", "192.168.1.1");
        LogicalRouter logicalRouter = mock(LogicalRouter.class);
        VirtualMachineInterface virtualMachineInterface = mock(VirtualMachineInterface.class);
        InstanceIp instanceIp = mock(InstanceIp.class);

        when(virtualMachineInterface.getUuid()).thenReturn("ac617be6-bf80-4086-9d6a-c05ff78e2264");
        when(tungstenApi.getTungstenObjectByName(eq(LogicalRouter.class), any(), anyString())).thenReturn(
            logicalRouter);
        when(tungstenApi.createTungstenGatewayVmi(anyString(), anyString(), anyString())).thenReturn(
            virtualMachineInterface);
        when(tungstenApi.createTungstenInstanceIp(anyString(), anyString(), anyString(), anyString())).thenReturn(
            instanceIp);
        when(tungstenApi.updateTungstenObject(any(LogicalRouter.class))).thenReturn(true);

        TungstenAnswer answer = (TungstenAnswer) tungstenResource.executeRequest(command);
        assertTrue(answer.getResult());
        assertEquals(virtualMachineInterface, answer.getApiObjectBase());
    }

    @Test
    public void executeRequestGetTungstenNetworkDnsCommandTest() {
        TungstenCommand command = new GetTungstenNetworkDnsCommand("ac617be6-bf80-4086-9d6a-c05ff78e2264", "subnet");

        when(tungstenApi.getTungstenNetworkDns(anyString(), anyString())).thenReturn("192.168.1.253");

        TungstenAnswer answer = (TungstenAnswer) tungstenResource.executeRequest(command);
        assertTrue(answer.getResult());
        assertEquals("192.168.1.253", answer.getDetails());
    }

    @Test
    public void executeRequestGetTungstenPolicyCommandTest() {
        TungstenCommand command = new GetTungstenPolicyCommand("projectFqn", "policy");
        NetworkPolicy networkPolicy = mock(NetworkPolicy.class);

        when(tungstenApi.getTungstenObjectByName(eq(NetworkPolicy.class), any(), anyString())).thenReturn(
            networkPolicy);

        TungstenAnswer answer = (TungstenAnswer) tungstenResource.executeRequest(command);
        assertTrue(answer.getResult());
        assertEquals(networkPolicy, answer.getApiObjectBase());
    }

    @Test
    public void executeRequestClearTungstenNetworkGatewayCommandTest() {
        TungstenCommand command = new ClearTungstenNetworkGatewayCommand("projectFqn", "router", 1L);

        LogicalRouter logicalRouter = mock(LogicalRouter.class);
        VirtualMachineInterface virtualMachineInterface = mock(VirtualMachineInterface.class);
        when(tungstenApi.getTungstenObjectByName(eq(LogicalRouter.class), any(), anyString())).thenReturn(
            logicalRouter);
        when(tungstenApi.deleteTungstenObject(any(LogicalRouter.class))).thenReturn(true);
        when(tungstenApi.getTungstenObjectByName(eq(VirtualMachineInterface.class), any(), anyString())).thenReturn(
            virtualMachineInterface);
        when(tungstenApi.deleteTungstenVmInterface(any(VirtualMachineInterface.class))).thenReturn(true);

        TungstenAnswer answer = (TungstenAnswer) tungstenResource.executeRequest(command);
        assertTrue(answer.getResult());
    }

    @Test
    public void executeRequestCreateTungstenFloatingIpPoolCommandTest() {
        TungstenCommand command = new CreateTungstenFloatingIpPoolCommand("ac617be6-bf80-4086-9d6a-c05ff78e2264",
            "fip");
        FloatingIpPool floatingIpPool = mock(FloatingIpPool.class);

        when(tungstenApi.createTungstenFloatingIpPool(anyString(), anyString())).thenReturn(floatingIpPool);

        TungstenAnswer answer = (TungstenAnswer) tungstenResource.executeRequest(command);
        assertTrue(answer.getResult());
        assertEquals(floatingIpPool, answer.getApiObjectBase());
    }

    @Test
    public void executeRequestCreateTungstenFloatingIpCommandTest() {
        TungstenCommand command = new CreateTungstenFloatingIpCommand("projectFqn",
            "ac617be6-bf80-4086-9d6a-c05ff78e2264", "fip", "fi", "192.168.1.100");
        FloatingIp floatingIp = mock(FloatingIp.class);

        when(tungstenApi.createTungstenFloatingIp(anyString(), anyString(), anyString(), anyString(),
            anyString())).thenReturn(floatingIp);

        TungstenAnswer answer = (TungstenAnswer) tungstenResource.executeRequest(command);
        assertTrue(answer.getResult());
        assertEquals(floatingIp, answer.getApiObjectBase());
    }

    @Test
    public void executeRequestAssignTungstenFloatingIpCommandTest() {
        TungstenCommand command = new AssignTungstenFloatingIpCommand("b604c7f7-1dbc-42d8-bceb-2c0898034a7a",
            "ac617be6-bf80-4086-9d6a-c05ff78e2264", "fip", "fi", "192.168.1.100");

        when(tungstenApi.assignTungstenFloatingIp(anyString(), anyString(), anyString(), anyString(),
            anyString())).thenReturn(true);

        TungstenAnswer answer = (TungstenAnswer) tungstenResource.executeRequest(command);
        assertTrue(answer.getResult());

    }

    @Test
    public void executeRequestReleaseTungstenFloatingIpCommandTest() {
        TungstenCommand command = new ReleaseTungstenFloatingIpCommand("b604c7f7-1dbc-42d8-bceb-2c0898034a7a", "fip",
            "fi");

        when(tungstenApi.releaseTungstenFloatingIp(anyString(), anyString(), anyString())).thenReturn(true);

        TungstenAnswer answer = (TungstenAnswer) tungstenResource.executeRequest(command);
        assertTrue(answer.getResult());
    }

    @Test
    public void executeRequestGetTungstenNatIpCommandTest() {
        TungstenCommand command = new GetTungstenNatIpCommand("projectFqn", "b604c7f7-1dbc-42d8-bceb-2c0898034a7a");

        when(tungstenApi.getTungstenNatIp(anyString(), anyString())).thenReturn("192.168.100.100");

        TungstenAnswer answer = (TungstenAnswer) tungstenResource.executeRequest(command);
        assertTrue(answer.getResult());
        assertEquals("192.168.100.100", answer.getDetails());
    }

    @Test
    public void executeRequestDeleteTungstenVRouterPortCommandTest() {
        TungstenCommand command = new DeleteTungstenVRouterPortCommand("10.0.0.10",
            "b604c7f7-1dbc-42d8-bceb-2c0898034a7a");

        tungstenVRouterApiMocked.when(
                () -> TungstenVRouterApi.deleteTungstenVrouterPort(anyString(), anyString(), anyString())
                                     ).thenReturn(true);

        TungstenAnswer answer = (TungstenAnswer) tungstenResource.executeRequest(command);
        assertTrue(answer.getResult());
    }

    @Test
    public void executeRequestDeleteTungstenFloatingIpCommandTest() {
        TungstenCommand command = new DeleteTungstenFloatingIpCommand("b604c7f7-1dbc-42d8-bceb-2c0898034a7a", "fip",
            "fi");
        VirtualNetwork virtualNetwork = mock(VirtualNetwork.class);
        FloatingIpPool floatingIpPool = mock(FloatingIpPool.class);
        FloatingIp floatingIp = mock(FloatingIp.class);

        when(tungstenApi.getTungstenObject(eq(VirtualNetwork.class), anyString())).thenReturn(virtualNetwork);
        when(tungstenApi.getTungstenObjectByName(eq(FloatingIpPool.class), any(), anyString())).thenReturn(
            floatingIpPool);
        when(tungstenApi.getTungstenObjectByName(eq(FloatingIp.class), any(), anyString())).thenReturn(floatingIp);
        when(tungstenApi.deleteTungstenObject(any(FloatingIp.class))).thenReturn(true);

        TungstenAnswer answer = (TungstenAnswer) tungstenResource.executeRequest(command);
        assertTrue(answer.getResult());
    }

    @Test
    public void executeRequestDeleteTungstenFloatingIpPoolCommandTest() {
        TungstenCommand command = new DeleteTungstenFloatingIpPoolCommand("b604c7f7-1dbc-42d8-bceb-2c0898034a7a",
            "fip");
        VirtualNetwork virtualNetwork = mock(VirtualNetwork.class);
        FloatingIpPool floatingIpPool = mock(FloatingIpPool.class);

        when(tungstenApi.getTungstenObject(eq(VirtualNetwork.class), anyString())).thenReturn(virtualNetwork);
        when(tungstenApi.getTungstenObjectByName(eq(FloatingIpPool.class), any(), anyString())).thenReturn(
            floatingIpPool);
        when(tungstenApi.deleteTungstenObject(any(FloatingIpPool.class))).thenReturn(true);

        TungstenAnswer answer = (TungstenAnswer) tungstenResource.executeRequest(command);
        assertTrue(answer.getResult());
    }

    @Test
    public void executeRequestCreateTungstenNetworkPolicyCommandest() {
        TungstenRule tungstenRule = mock(TungstenRule.class);
        List<TungstenRule> tungstenRuleList = List.of(tungstenRule);
        TungstenCommand command = new CreateTungstenNetworkPolicyCommand("policy", "projectFqn", tungstenRuleList);
        NetworkPolicy networkPolicy = mock(NetworkPolicy.class);

        when(tungstenApi.createOrUpdateTungstenNetworkPolicy(anyString(), anyString(), eq(tungstenRuleList))).thenReturn(networkPolicy);

        TungstenAnswer answer = (TungstenAnswer) tungstenResource.executeRequest(command);
        assertTrue(answer.getResult());
        assertEquals(networkPolicy, answer.getApiObjectBase());
    }

    @Test
    public void executeRequestDeleteTungstenNetworkPolicyCommandTest() {
        TungstenCommand command = new DeleteTungstenNetworkPolicyCommand("policy", "projectFqn",
            "b604c7f7-1dbc-42d8-bceb-2c0898034a7a");
        VirtualNetwork virtualNetwork = mock(VirtualNetwork.class);
        NetworkPolicy networkPolicy = mock(NetworkPolicy.class);

        when(tungstenApi.getTungstenObject(eq(VirtualNetwork.class), anyString())).thenReturn(virtualNetwork);
        when(tungstenApi.getTungstenObjectByName(eq(NetworkPolicy.class), any(), anyString())).thenReturn(
            networkPolicy);
        when(tungstenApi.updateTungstenObject(any(VirtualNetwork.class))).thenReturn(true);
        when(tungstenApi.deleteTungstenObject(any(NetworkPolicy.class))).thenReturn(true);

        TungstenAnswer answer = (TungstenAnswer) tungstenResource.executeRequest(command);
        assertTrue(answer.getResult());
    }

    @Test
    public void executeRequestGetTungstenFloatingIpsCommandTest() {
        TungstenCommand command = new GetTungstenFloatingIpsCommand("b604c7f7-1dbc-42d8-bceb-2c0898034a7a", "fip");
        FloatingIp floatingIp1 = mock(FloatingIp.class);
        FloatingIp floatingIp2 = mock(FloatingIp.class);
        List<? extends ApiObjectBase> floatingIpList = Arrays.asList(floatingIp1, floatingIp2);
        VirtualNetwork virtualNetwork = mock(VirtualNetwork.class);
        FloatingIpPool floatingIpPool = mock(FloatingIpPool.class);

        when(tungstenApi.getTungstenObject(eq(VirtualNetwork.class), anyString())).thenReturn(virtualNetwork);
        when(tungstenApi.getTungstenObjectByName(eq(FloatingIpPool.class), any(), anyString())).thenReturn(
            floatingIpPool);
        doReturn(floatingIpList).when(tungstenApi)
            .getTungstenListObject(eq(FloatingIp.class), any(FloatingIpPool.class), any());

        TungstenAnswer answer = (TungstenAnswer) tungstenResource.executeRequest(command);
        assertTrue(answer.getResult());
        assertEquals(floatingIpList, answer.getApiObjectBaseList());
    }

    @Test
    public void executeRequestApplyTungstenNetworkPolicyCommandTest() throws Exception {
        TungstenCommand command = new ApplyTungstenNetworkPolicyCommand("projectFqn", "policy",
            "b604c7f7-1dbc-42d8-bceb-2c0898034a7a", 1, 1);
        NetworkPolicy networkPolicy = mock(NetworkPolicy.class);
        VirtualNetwork virtualNetwork1 = mock(VirtualNetwork.class);
        VirtualNetwork virtualNetwork2 = mock(VirtualNetwork.class);
        List<VirtualNetwork> virtualNetworkList = Arrays.asList(virtualNetwork1, virtualNetwork2);

        when(networkPolicy.getUuid()).thenReturn("ac617be6-bf80-4086-9d6a-c05ff78e2264");
        when(tungstenApi.getTungstenObjectByName(eq(NetworkPolicy.class), any(), anyString())).thenReturn(
            networkPolicy);
        when(tungstenApi.applyTungstenNetworkPolicy(anyString(), anyString(), anyInt(), anyInt())).thenReturn(
            networkPolicy);
        doReturn(virtualNetworkList).when(tungstenApi).getNetworksFromNetworkPolicy(any(NetworkPolicy.class));

        TungstenAnswer answer = (TungstenAnswer) tungstenResource.executeRequest(command);
        assertTrue(answer.getResult());
        assertEquals(networkPolicy, ((TungstenNetworkPolicy) answer.getTungstenModel()).getNetworkPolicy());
    }

    @Test
    public void executeRequestGetTungstenFabricNetworkCommand() {
        TungstenCommand command = new GetTungstenFabricNetworkCommand();
        VirtualNetwork virtualNetwork = mock(VirtualNetwork.class);

        when(tungstenApi.getTungstenFabricNetwork()).thenReturn(virtualNetwork);

        TungstenAnswer answer = (TungstenAnswer) tungstenResource.executeRequest(command);
        assertTrue(answer.getResult());
        assertEquals(virtualNetwork, answer.getApiObjectBase());
    }

    @Test
    public void executeRequestCreateTungstenDomainCommandTest() {
        TungstenCommand command = new CreateTungstenDomainCommand("domainName", "ac617be6-bf80-4086-9d6a-c05ff78e2264");
        Domain domain = mock(Domain.class);

        when(tungstenApi.createTungstenDomain(anyString(), anyString())).thenReturn(domain);

        TungstenAnswer answer = (TungstenAnswer) tungstenResource.executeRequest(command);
        assertTrue(answer.getResult());
        assertEquals(domain, answer.getApiObjectBase());
    }

    @Test
    public void executeRequestCreateTungstenProjectCommandTest() {
        TungstenCommand command = new CreateTungstenProjectCommand("projectName",
            "ac617be6-bf80-4086-9d6a-c05ff78e2264", "d069ad89-b4d0-43fb-b75f-179ab3bfb03c", "domainName");
        Project project = mock(Project.class);

        when(tungstenApi.createTungstenProject(anyString(), anyString(), anyString(), anyString())).thenReturn(project);

        TungstenAnswer answer = (TungstenAnswer) tungstenResource.executeRequest(command);
        assertTrue(answer.getResult());
        assertEquals(project, answer.getApiObjectBase());
    }

    @Test
    public void executeRequestDeleteTungstenDomainCommandTest() {
        TungstenCommand command = new DeleteTungstenDomainCommand("ac617be6-bf80-4086-9d6a-c05ff78e2264");

        when(tungstenApi.deleteTungstenDomain(anyString())).thenReturn(true);

        TungstenAnswer answer = (TungstenAnswer) tungstenResource.executeRequest(command);
        assertTrue(answer.getResult());
    }

    @Test
    public void executeRequestDeleteTungstenProjectCommandTest() {
        TungstenCommand command = new DeleteTungstenProjectCommand("ac617be6-bf80-4086-9d6a-c05ff78e2264");

        when(tungstenApi.deleteTungstenProject(anyString())).thenReturn(true);

        TungstenAnswer answer = (TungstenAnswer) tungstenResource.executeRequest(command);
        assertTrue(answer.getResult());
    }

    @Test
    public void executeRequestCreateTungstenNetworkLoadbalancerCommandTest() {
        TungstenLoadBalancerMember tungstenMember1 = new TungstenLoadBalancerMember("member1", "192.168.100.100", 80,
            1);
        TungstenLoadBalancerMember tungstenMember2 = new TungstenLoadBalancerMember("member2", "192.168.100.101", 80,
            2);
        List<TungstenLoadBalancerMember> tungstenLoadBalancerMemberList = Arrays.asList(tungstenMember1,
            tungstenMember2);
        TungstenCommand command = new CreateTungstenNetworkLoadbalancerCommand("projectFqn",
            "d4e2fc35-085c-4c8a-b08b-4cd237c75704", "c33e7145-865c-490b-8099-5a0efbab7467", "ROUND_ROBIN", "lbName",
            "lbListenerName", "lbPoolName", "lbHealthMonitorName", "lbVmiName", "lbIiName", 1L,
            tungstenLoadBalancerMemberList, "tcp", 80, 80, "192.168.1.100", "fipName", "fiName", "PING", 3, 5, 5, "GET",
            "/url", "200");
        VirtualNetwork virtualNetwork = mock(VirtualNetwork.class);
        FloatingIpPool floatingIpPool = mock(FloatingIpPool.class);
        VirtualMachineInterface virtualMachineInterface = mock(VirtualMachineInterface.class);
        InstanceIp instanceIp = mock(InstanceIp.class);
        Loadbalancer loadbalancer = mock(Loadbalancer.class);
        LoadbalancerListener loadbalancerListener = mock(LoadbalancerListener.class);
        LoadbalancerHealthmonitor loadbalancerHealthmonitor = mock(LoadbalancerHealthmonitor.class);
        LoadbalancerPool loadbalancerPool = mock(LoadbalancerPool.class);
        LoadbalancerMember member1 = mock(LoadbalancerMember.class);
        LoadbalancerMember member2 = mock(LoadbalancerMember.class);

        when(virtualNetwork.getUuid()).thenReturn("ac617be6-bf80-4086-9d6a-c05ff78e2264");
        when(virtualMachineInterface.getUuid()).thenReturn("d069ad89-b4d0-43fb-b75f-179ab3bfb03c");
        when(loadbalancer.getUuid()).thenReturn("387cf015-44f7-48fa-bd6b-d2e3e14361de");
        when(loadbalancerHealthmonitor.getUuid()).thenReturn("f6517b1a-773c-46a2-ae50-e8b32d5023a1");
        when(loadbalancerListener.getUuid()).thenReturn("c877d37a-9ad4-4188-a09a-fb13f57f9be0");
        when(loadbalancerPool.getUuid()).thenReturn("baf714fa-80a1-454f-9c32-c4d4a6f5c5a4");
        when(tungstenApi.getTungstenObject(eq(VirtualNetwork.class), anyString())).thenReturn(virtualNetwork);
        when(tungstenApi.getSubnetUuid(anyString())).thenReturn("b604c7f7-1dbc-42d8-bceb-2c0898034a7a");
        when(tungstenApi.createTungstenLbVmi(anyString(), anyString(), anyString())).thenReturn(
            virtualMachineInterface);
        when(tungstenApi.createTungstenInstanceIp(anyString(), anyString(), anyString(), anyString(),
            anyString())).thenReturn(instanceIp);
        when(tungstenApi.assignTungstenFloatingIp(anyString(), anyString(), anyString(), anyString(),
            anyString())).thenReturn(true);
        when(tungstenApi.createTungstenLoadbalancer(anyString(), anyString(), anyString(), anyString(),
            anyString())).thenReturn(loadbalancer);
        when(tungstenApi.createTungstenLoadbalancerListener(anyString(), anyString(), anyString(), anyString(),
            anyInt())).thenReturn(loadbalancerListener);
        when(tungstenApi.createTungstenLoadbalancerHealthMonitor(anyString(), anyString(), anyString(), anyInt(),
            anyInt(), anyInt(), anyString(), anyString(), anyString())).thenReturn(loadbalancerHealthmonitor);
        when(tungstenApi.createTungstenLoadbalancerPool(anyString(), anyString(), anyString(), anyString(), anyString(),
            anyString())).thenReturn(loadbalancerPool);
        when(tungstenApi.createTungstenLoadbalancerMember(anyString(), anyString(), anyString(), anyString(), anyInt(),
            anyInt())).thenReturn(member1).thenReturn(member2);

        TungstenAnswer answer = (TungstenAnswer) tungstenResource.executeRequest(command);
        assertTrue(answer.getResult());
        assertEquals(loadbalancer, answer.getApiObjectBase());
    }

    @Test
    public void executeRequestUpdateLoadBalancerServiceInstanceCommandTest() {
        TungstenCommand command = new UpdateLoadBalancerServiceInstanceCommand("baf714fa-80a1-454f-9c32-c4d4a6f5c5a4",
            "fipName", "fiName");

        when(tungstenApi.updateLBServiceInstanceFatFlow(anyString(), anyString(), anyString())).thenReturn(true);

        TungstenAnswer answer = (TungstenAnswer) tungstenResource.executeRequest(command);
        assertTrue(answer.getResult());
    }

    @Test
    public void executeRequestDeleteTungstenLoadBalancerCommandTest() {
        TungstenCommand command = new DeleteTungstenLoadBalancerCommand("projectFqn",
            "d4e2fc35-085c-4c8a-b08b-4cd237c75704", "lbName", "lbHealthName", "lbVmiName", "fipName", "fiName");
        Loadbalancer loadbalancer = mock(Loadbalancer.class);
        LoadbalancerListener loadbalancerListener = mock(LoadbalancerListener.class);
        LoadbalancerPool loadbalancerPool = mock(LoadbalancerPool.class);
        LoadbalancerMember loadbalancerMember1 = mock(LoadbalancerMember.class);
        LoadbalancerMember loadbalancerMember2 = mock(LoadbalancerMember.class);
        VirtualMachineInterface virtualMachineInterface = mock(VirtualMachineInterface.class);
        ObjectReference<ApiPropertyBase> listerner = mock(ObjectReference.class);
        ObjectReference<ApiPropertyBase> pool = mock(ObjectReference.class);
        ObjectReference<ApiPropertyBase> member1 = mock(ObjectReference.class);
        ObjectReference<ApiPropertyBase> member2 = mock(ObjectReference.class);

        when(listerner.getUuid()).thenReturn("baf714fa-80a1-454f-9c32-c4d4a6f5c5a4");
        when(pool.getUuid()).thenReturn("6b062909-ba9d-4cf3-bbd3-7db93cf6b4fe");
        when(member1.getUuid()).thenReturn("7d5575eb-d029-467e-8b78-6056a8c94a71");
        when(member2.getUuid()).thenReturn("88729834-3ebd-413a-adf9-40aff73cf638");
        when(loadbalancer.getLoadbalancerListenerBackRefs()).thenReturn(List.of(listerner));
        when(loadbalancerListener.getLoadbalancerPoolBackRefs()).thenReturn(List.of(pool));
        when(loadbalancerPool.getLoadbalancerMembers()).thenReturn(Arrays.asList(member1, member2));
        when(tungstenApi.getTungstenObjectByName(eq(Loadbalancer.class), any(), anyString())).thenReturn(loadbalancer);
        when(tungstenApi.getTungstenObjectByName(eq(VirtualMachineInterface.class), any(), anyString())).thenReturn(
            virtualMachineInterface);
        when(tungstenApi.getTungstenObject(eq(LoadbalancerListener.class), anyString())).thenReturn(
            loadbalancerListener);
        when(tungstenApi.getTungstenObject(eq(LoadbalancerPool.class), anyString())).thenReturn(loadbalancerPool);
        when(tungstenApi.getTungstenObject(eq(LoadbalancerMember.class), anyString())).thenReturn(loadbalancerMember1)
            .thenReturn(loadbalancerMember2);
        when(tungstenApi.deleteTungstenObject(any())).thenReturn(true);
        when(tungstenApi.releaseTungstenFloatingIp(anyString(), anyString(), anyString())).thenReturn(true);
        when(tungstenApi.deleteTungstenVmInterface(any(VirtualMachineInterface.class))).thenReturn(true);

        TungstenAnswer answer = (TungstenAnswer) tungstenResource.executeRequest(command);
        assertTrue(answer.getResult());
    }

    @Test
    public void executeRequestDeleteTungstenLoadBalancerListenerCommandTest() {
        TungstenCommand command = new DeleteTungstenLoadBalancerListenerCommand("projectFqn", "lbListenerName");
        LoadbalancerListener loadbalancerListener = mock(LoadbalancerListener.class);

        when(tungstenApi.getTungstenObjectByName(eq(LoadbalancerListener.class), any(), anyString())).thenReturn(
            loadbalancerListener);
        when(tungstenApi.deleteTungstenObject(any(LoadbalancerListener.class))).thenReturn(true);
        TungstenAnswer answer = (TungstenAnswer) tungstenResource.executeRequest(command);
        assertTrue(answer.getResult());
    }

    @Test
    public void executeRequestUpdateTungstenLoadBalancerPoolCommandTest() {
        TungstenCommand command = new UpdateTungstenLoadBalancerPoolCommand("projectFqn", "lbPoolName", "lbMethod",
                "lbSessionPersistence", "lbPersistenceCookieName", "lbProtocol", true,
                "lbStatsPort", "lbStatsUri", "lbStatsAuth");
        when(tungstenApi.updateLoadBalancerPool(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(),
                anyBoolean(), anyString(), anyString(), anyString())).thenReturn(true);
        TungstenAnswer answer = (TungstenAnswer) tungstenResource.executeRequest(command);
        assertTrue(answer.getResult());
    }

    @Test
    public void executeRequestUpdateTungstenLoadBalancerListenerCommandTest() {
        TungstenCommand command = new UpdateTungstenLoadBalancerListenerCommand("projectFqn", "listenerName", "tcp",
                80, "url");
        when(tungstenApi.updateLoadBalancerListener(anyString(), anyString(), anyString(), anyInt(), anyString())).thenReturn(true);
        TungstenAnswer answer = (TungstenAnswer) tungstenResource.executeRequest(command);
        assertTrue(answer.getResult());
    }

    @Test
    public void executeRequestUpdateTungstenLoadBalancerMemberCommandTest() {
        TungstenCommand command = new UpdateTungstenLoadBalancerMemberCommand("projectFqn", "e8281cd6-9078-4db1-9f47-e52f679e03d1",
                "lbPoolName", new ArrayList<>());
        when(tungstenApi.getSubnetUuid(anyString())).thenReturn("4185e240-0fcd-11ec-82a8-0242ac130003");
        when(tungstenApi.updateLoadBalancerMember(anyString(), anyString(), anyList(), anyString())).thenReturn(true);
        TungstenAnswer answer = (TungstenAnswer) tungstenResource.executeRequest(command);
        assertTrue(answer.getResult());
    }

    @Test
    public void executeRequestGetTungstenLoadBalancerCommandTest() {
        TungstenCommand command = new GetTungstenLoadBalancerCommand("projectFqn", "lbName");
        Loadbalancer loadbalancer = mock(Loadbalancer.class);
        when(tungstenApi.getTungstenObjectByName(eq(Loadbalancer.class), anyList(), anyString())).thenReturn(loadbalancer);
        TungstenAnswer answer = (TungstenAnswer) tungstenResource.executeRequest(command);
        assertTrue(answer.getResult());
        assertEquals(loadbalancer, answer.getApiObjectBase());
    }

    @Test
    public void executeRequestApplyTungstenPortForwardingCommandTest() {
        TungstenCommand command = new ApplyTungstenPortForwardingCommand(true, "d4e2fc35-085c-4c8a-b08b-4cd237c75704",
                "floatingIpPoolName", "floatingIpName", "ac617be6-bf80-4086-9d6a-c05ff78e2264",
                "tcp", 80, 85);
        when(tungstenApi.applyTungstenPortForwarding(anyBoolean(), anyString(), anyString(), anyString(), anyString(), anyString(),
                anyInt(), anyInt())).thenReturn(true);
        TungstenAnswer answer = (TungstenAnswer) tungstenResource.executeRequest(command);
        assertTrue(answer.getResult());
    }

    @Test
    public void executeRequestDeleteTungstenObjectCommandTest() {
        ApiObjectBase apiObjectBase = mock(ApiObjectBase.class);
        TungstenCommand command = new DeleteTungstenObjectCommand(apiObjectBase);
        when(tungstenApi.deleteTungstenObject(any(ApiObjectBase.class))).thenReturn(true);
        TungstenAnswer answer = (TungstenAnswer) tungstenResource.executeRequest(command);
        assertTrue(answer.getResult());
    }

    @Test
    public void executeRequestAddTungstenNetworkSubnetCommandTest() {
        TungstenCommand command = new AddTungstenNetworkSubnetCommand("7ea93dd0-0fd1-11ec-82a8-0242ac130003", "10.0.0.0",
                24, "10.0.0.1",true, "10.0.0.253", "10.0.0.2", "10.0.0.100",
                true, "subnetName");
        when(tungstenApi.addTungstenNetworkSubnetCommand(anyString(), anyString(), anyInt(), anyString(), anyBoolean(), anyString(), anyString(),
                anyString(), anyBoolean(), anyString())).thenReturn(true);
        TungstenAnswer answer = (TungstenAnswer) tungstenResource.executeRequest(command);
        assertTrue(answer.getResult());
    }

    @Test
    public void executeRequestRemoveTungstenNetworkSubnetCommandTest() {
        TungstenCommand command = new RemoveTungstenNetworkSubnetCommand("7ea93dd0-0fd1-11ec-82a8-0242ac130003", "subnetName");
        when(tungstenApi.removeTungstenNetworkSubnetCommand(anyString(), anyString())).thenReturn(true);
        TungstenAnswer answer = (TungstenAnswer) tungstenResource.executeRequest(command);
        assertTrue(answer.getResult());
    }

    @Test
    public void executeRequestCreateTungstenSecurityGroupCommandTest() {
        SecurityGroup securityGroup = mock(SecurityGroup.class);
        TungstenCommand command = new CreateTungstenSecurityGroupCommand("004a8524-0fd2-11ec-82a8-0242ac130003",
                "securityGroupName", "securityGroupDescription", "projectFqn");
        when(tungstenApi.createTungstenSecurityGroup(anyString(), anyString(), anyString(), anyString())).thenReturn(securityGroup);
        TungstenAnswer answer = (TungstenAnswer) tungstenResource.executeRequest(command);
        assertTrue(answer.getResult());
        assertEquals(securityGroup, answer.getApiObjectBase());
    }

    @Test
    public void executeRequestDeleteTungstenSecurityGroupCommandTest() {
        TungstenCommand command = new DeleteTungstenSecurityGroupCommand("004a8524-0fd2-11ec-82a8-0242ac130003");
        when(tungstenApi.deleteTungstenObject(eq(SecurityGroup.class), anyString())).thenReturn(true);
        TungstenAnswer answer = (TungstenAnswer) tungstenResource.executeRequest(command);
        assertTrue(answer.getResult());
    }

    @Test
    public void executeRequestGetTungstenSecurityGroupCommandTest() {
        SecurityGroup securityGroup = mock(SecurityGroup.class);
        TungstenCommand command = new GetTungstenSecurityGroupCommand("004a8524-0fd2-11ec-82a8-0242ac130003");
        when(tungstenApi.getTungstenObject(eq(SecurityGroup.class), anyString())).thenReturn(securityGroup);
        TungstenAnswer answer = (TungstenAnswer) tungstenResource.executeRequest(command);
        assertTrue(answer.getResult());
        assertEquals(securityGroup, answer.getApiObjectBase());
    }

    @Test
    public void executeRequestAddTungstenSecurityGroupRuleCommandTest() {
        TungstenCommand command = new AddTungstenSecurityGroupRuleCommand("169486d0-0fd3-11ec-82a8-0242ac130003",
                "1ca20eee-0fd3-11ec-82a8-0242ac130003", "securityGroupRuleType", 80, 90,
                "target", "etherType", "tcp");
        when(tungstenApi.addTungstenSecurityGroupRule(anyString(), anyString(), anyString(), anyInt(), anyInt(), anyString(),
                anyString(), anyString())).thenReturn(true);
        TungstenAnswer answer = (TungstenAnswer) tungstenResource.executeRequest(command);
        assertTrue(answer.getResult());
    }

    @Test
    public void executeRequestAddTungstenVmToSecurityGroupCommandTest() {
        List<String> securityGroupUuidList = Arrays.asList("1ca20eee-0fd3-11ec-82a8-0242ac130003", "a38200a2-0fd5-11ec-82a8-0242ac130003");
        TungstenCommand command = new AddTungstenVmToSecurityGroupCommand("1ca20eee-0fd3-11ec-82a8-0242ac130003", securityGroupUuidList);
        when(tungstenApi.addInstanceToSecurityGroup(anyString(), eq(securityGroupUuidList))).thenReturn(true);
        TungstenAnswer answer = (TungstenAnswer) tungstenResource.executeRequest(command);
        assertTrue(answer.getResult());
    }

    @Test
    public void executeRequestRemoveTungstenVmFromSecurityGroupCommandTest() {
        List<String> securityGroupUuidList = Arrays.asList("1ca20eee-0fd3-11ec-82a8-0242ac130003", "a38200a2-0fd5-11ec-82a8-0242ac130003");
        TungstenCommand command = new RemoveTungstenVmFromSecurityGroupCommand("a38200a2-0fd5-11ec-82a8-0242ac130003", securityGroupUuidList);
        when(tungstenApi.removeInstanceFromSecurityGroup(anyString(), eq(securityGroupUuidList))).thenReturn(true);
        TungstenAnswer answer = (TungstenAnswer) tungstenResource.executeRequest(command);
        assertTrue(answer.getResult());
    }

    @Test
    public void executeRequestRemoveTungstenSecurityGroupRuleCommandTest() {
        TungstenCommand command = new RemoveTungstenSecurityGroupRuleCommand("63906a00-0fd6-11ec-82a8-0242ac130003",
                "6dd5ff84-0fd6-11ec-82a8-0242ac130003");
        when(tungstenApi.removeTungstenSecurityGroupRule(anyString(), anyString())).thenReturn(true);
        TungstenAnswer answer = (TungstenAnswer) tungstenResource.executeRequest(command);
        assertTrue(answer.getResult());
    }

    @Test
    public void executeRequestAddTungstenSecondaryIpAddressCommandTest() {
        TungstenCommand command = new AddTungstenSecondaryIpAddressCommand("15266358-0fd9-11ec-82a8-0242ac130003",
                "1ba019e0-0fd9-11ec-82a8-0242ac130003", "iiName", "address");
        when(tungstenApi.addSecondaryIpAddress(anyString(), anyString(), anyString(), anyString())).thenReturn(true);
        TungstenAnswer answer = (TungstenAnswer) tungstenResource.executeRequest(command);
        assertTrue(answer.getResult());
    }

    @Test
    public void executeRequestRemoveTungstenSecondaryIpAddressCommandTest() {
        TungstenCommand command = new RemoveTungstenSecondaryIpAddressCommand("iiName");
        when(tungstenApi.removeSecondaryIpAddress(anyString())).thenReturn(true);
        TungstenAnswer answer = (TungstenAnswer) tungstenResource.executeRequest(command);
        assertTrue(answer.getResult());
    }

    @Test
    public void executeRequestCreateTungstenPolicyCommandTest() {
        TungstenCommand command = new CreateTungstenPolicyCommand("name", "projectFqn");
        VirtualNetwork virtualNetwork1 = mock(VirtualNetwork.class);
        VirtualNetwork virtualNetwork2 = mock(VirtualNetwork.class);
        List<VirtualNetwork> virtualNetworks = Arrays.asList(virtualNetwork1, virtualNetwork2);
        when(tungstenApi.createTungstenPolicy(anyString(), anyString(), anyString())).thenReturn(new NetworkPolicy());
        when(tungstenApi.getNetworksFromNetworkPolicy(any(NetworkPolicy.class))).thenReturn(virtualNetworks);
        TungstenAnswer answer = (TungstenAnswer) tungstenResource.executeRequest(command);
        assertTrue(answer.getResult());
    }

    @Test
    public void executeRequestAddTungstenPolicyRuleCommandTest() {
        TungstenCommand command = new AddTungstenPolicyRuleCommand("bae19252-0fe0-11ec-82a8-0242ac130003", "pass", "<>",
                "tcp", "srcNetwork", "10.0.0.0", 24, 80, 90, "destNetwork",
                "10.1.0.0", 24, 80, 90);
        NetworkPolicy networkPolicy = mock(NetworkPolicy.class);
        when(tungstenApi.addTungstenPolicyRule(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString(),
                anyInt(), anyInt(), anyInt(), anyString(), anyString(), anyInt(), anyInt(), anyInt())).thenReturn(networkPolicy);
        TungstenAnswer answer = (TungstenAnswer) tungstenResource.executeRequest(command);
        assertTrue(answer.getResult());
    }

    @Test
    public void executeRequestRemoveTungstenPolicyRuleCommandTest() {
        TungstenCommand command = new RemoveTungstenPolicyRuleCommand("accdbdc0-0fe1-11ec-82a8-0242ac130003",
                "b1e71054-0fe1-11ec-82a8-0242ac130003");
        VirtualNetwork virtualNetwork1 = mock(VirtualNetwork.class);
        VirtualNetwork virtualNetwork2 = mock(VirtualNetwork.class);
        List<VirtualNetwork> virtualNetworks = Arrays.asList(virtualNetwork1, virtualNetwork2);
        when(tungstenApi.removeTungstenNetworkPolicyRule(anyString(), anyString())).thenReturn(new NetworkPolicy());
        when(tungstenApi.getNetworksFromNetworkPolicy(any(NetworkPolicy.class))).thenReturn(virtualNetworks);
        TungstenAnswer answer = (TungstenAnswer) tungstenResource.executeRequest(command);
        assertTrue(answer.getResult());
    }

    @Test
    public void executeRequestDeleteTungstenPolicyCommandTest() {
        TungstenCommand command = new DeleteTungstenPolicyCommand("accdbdc0-0fe1-11ec-82a8-0242ac130003");
        when(tungstenApi.getTungstenObject(eq(NetworkPolicy.class), anyString())).thenReturn(new NetworkPolicy());
        when(tungstenApi.deleteTungstenObject(any(ApiObjectBase.class))).thenReturn(true);
        TungstenAnswer answer = (TungstenAnswer) tungstenResource.executeRequest(command);
        assertTrue(answer.getResult());
    }

    @Test
    public void executeRequestListTungstenPolicyCommandTest() {
        TungstenCommand command = new ListTungstenPolicyCommand("projectFqn", "2e51abf8-1097-11ec-82a8-0242ac130003",
                null, null);
        NetworkPolicy networkPolicy1 = mock(NetworkPolicy.class);
        NetworkPolicy networkPolicy2 = mock(NetworkPolicy.class);
        Answer<List<ApiObjectBase>> networkPoliciesAnswer = setupApiObjectBaseListAnswer(networkPolicy1, networkPolicy2);
        VirtualNetwork virtualNetwork1 = mock(VirtualNetwork.class);
        VirtualNetwork virtualNetwork2 = mock(VirtualNetwork.class);
        List<VirtualNetwork> virtualNetworks = Arrays.asList(virtualNetwork1, virtualNetwork2);
        TungstenAnswer answer = (TungstenAnswer) tungstenResource.executeRequest(command);
        assertTrue(answer.getResult());
        assertNotNull(answer.getTungstenModelList());
    }

    @Test
    public void executeRequestListTungstenPolicyRuleCommandTest() {
        TungstenCommand command = new ListTungstenPolicyRuleCommand("accdbdc0-0fe1-11ec-82a8-0242ac130003");
        when(tungstenApi.getTungstenObject(eq(NetworkPolicy.class), anyString())).thenReturn(new NetworkPolicy());
        TungstenAnswer answer = (TungstenAnswer) tungstenResource.executeRequest(command);
        assertTrue(answer.getResult());
    }

    @Test
    public void executeRequestListTungstenNetworkCommandTest() {
        TungstenCommand command = new ListTungstenNetworkCommand("projectFqn", "2e51abf8-1097-11ec-82a8-0242ac130003");
        VirtualNetwork virtualNetwork1 = mock(VirtualNetwork.class);
        VirtualNetwork virtualNetwork2 = mock(VirtualNetwork.class);
        Answer<List<ApiObjectBase>> virtualNetworksAnswer = setupApiObjectBaseListAnswer(virtualNetwork1, virtualNetwork2);
        when(tungstenApi.listTungstenNetwork(anyString(), anyString())).thenAnswer(virtualNetworksAnswer);
        TungstenAnswer answer = (TungstenAnswer) tungstenResource.executeRequest(command);
        assertTrue(answer.getResult());
    }

    @Test
    public void executeRequestListTungstenVmCommandTest() {
        TungstenCommand command = new ListTungstenVmCommand("projectFqn", "ca86c658-1096-11ec-82a8-0242ac130003");
        VirtualMachine virtualMachine1 = mock(VirtualMachine.class);
        VirtualMachine virtualMachine2 = mock(VirtualMachine.class);
        Answer<List<ApiObjectBase>> virtualMachinesAnswer = setupApiObjectBaseListAnswer(virtualMachine1, virtualMachine2);
        when(tungstenApi.listTungstenVm(anyString(), anyString())).thenAnswer(virtualMachinesAnswer);
        TungstenAnswer answer = (TungstenAnswer) tungstenResource.executeRequest(command);
        assertTrue(answer.getResult());
    }

    @Test
    public void executeRequestListTungstenNicCommandTest() {
        TungstenCommand command = new ListTungstenNicCommand("projectFqn", "5b3ffba2-1096-11ec-82a8-0242ac130003");
        VirtualMachineInterface virtualMachineInterface1 = mock(VirtualMachineInterface.class);
        VirtualMachineInterface virtualMachineInterface2 = mock(VirtualMachineInterface.class);
        Answer<List<ApiObjectBase>> virtualMachineInterfacesAnswer = setupApiObjectBaseListAnswer(virtualMachineInterface1, virtualMachineInterface2);
        when(tungstenApi.listTungstenNic(anyString(), anyString())).thenAnswer(virtualMachineInterfacesAnswer);
        TungstenAnswer answer = (TungstenAnswer) tungstenResource.executeRequest(command);
        assertTrue(answer.getResult());
    }

    @Test
    public void executeRequestCreateTungstenTagCommandTest() {
        TungstenCommand command = new CreateTungstenTagCommand("tagType", "tagValue");
        Tag tag = mock(Tag.class);
        VirtualNetwork virtualNetwork1 = mock(VirtualNetwork.class);
        VirtualNetwork virtualNetwork2 = mock(VirtualNetwork.class);
        Answer<List<ApiObjectBase>> virtualNetworksAnswer = setupApiObjectBaseListAnswer(virtualNetwork1, virtualNetwork2);
        VirtualMachine virtualMachine1 = mock(VirtualMachine.class);
        VirtualMachine virtualMachine2 = mock(VirtualMachine.class);
        Answer<List<ApiObjectBase>> virtualMachinesAnswer = setupApiObjectBaseListAnswer(virtualMachine1, virtualMachine2);
        VirtualMachineInterface virtualMachineInterface1 = mock(VirtualMachineInterface.class);
        VirtualMachineInterface virtualMachineInterface2 = mock(VirtualMachineInterface.class);
        Answer<List<ApiObjectBase>> virtualMachineInterfacesAnswer = setupApiObjectBaseListAnswer(virtualMachineInterface1, virtualMachineInterface2);
        NetworkPolicy networkPolicy1 = mock(NetworkPolicy.class);
        NetworkPolicy networkPolicy2 = mock(NetworkPolicy.class);
        Answer<List<ApiObjectBase>> networkPoliciesAnswer = setupApiObjectBaseListAnswer(networkPolicy1, networkPolicy2);
        when(tungstenApi.createTungstenTag(anyString(), anyString(), anyString(), any())).thenReturn(tag);
        when(tungstenApi.getBackRefFromVirtualNetwork(eq(VirtualNetwork.class), anyList())).thenAnswer(virtualNetworksAnswer);
        when(tungstenApi.getBackRefFromVirtualMachine(eq(VirtualMachine.class), anyList())).thenAnswer(virtualMachinesAnswer);
        when(tungstenApi.getBackRefFromVirtualMachineInterface(eq(VirtualMachineInterface.class), anyList())).thenAnswer(virtualMachineInterfacesAnswer);
        when(tungstenApi.getBackRefFromNetworkPolicy(eq(NetworkPolicy.class), anyList())).thenAnswer(networkPoliciesAnswer);
        TungstenAnswer answer = (TungstenAnswer) tungstenResource.executeRequest(command);
        assertTrue(answer.getResult());
        assertNotNull(answer.getTungstenModel());
    }

    @Test
    public void executeRequestCreateTungstenTagTypeCommandTest() {
        TungstenCommand command = new CreateTungstenTagTypeCommand("TungstenTagTypeName");
        TagType tagType = mock(TagType.class);
        when(tungstenApi.createTungstenTagType(anyString(), anyString())).thenReturn(tagType);
        TungstenAnswer answer = (TungstenAnswer) tungstenResource.executeRequest(command);
        assertTrue(answer.getResult());
        assertNotNull(answer.getApiObjectBase());
    }

    @Test
    public void executeRequestDeleteTungstenTagCommandTest() {
        TungstenCommand command = new DeleteTungstenTagCommand("2e51abf8-1097-11ec-82a8-0242ac130003");
        Tag tag = mock(Tag.class);
        when(tungstenApi.getTungstenObject(eq(Tag.class), anyString())).thenReturn(tag);
        when(tungstenApi.deleteTungstenObject(any(Tag.class))).thenReturn(true);
        TungstenAnswer answer = (TungstenAnswer) tungstenResource.executeRequest(command);
        assertTrue(answer.getResult());
    }

    @Test
    public void executeRequestDeleteTungstenTagTypeCommandTest() {
        TungstenCommand command = new DeleteTungstenTagTypeCommand("22f28f5a-1099-11ec-82a8-0242ac130003");
        TagType tagType = mock(TagType.class);
        when(tungstenApi.getTungstenObject(eq(TagType.class), anyString())).thenReturn(tagType);
        when(tungstenApi.deleteTungstenObject(any(TagType.class))).thenReturn(true);
        TungstenAnswer answer = (TungstenAnswer) tungstenResource.executeRequest(command);
        assertTrue(answer.getResult());
    }

    @Test
    public void executeRequestListTungstenTagCommandTest() {
        TungstenCommand command = new ListTungstenTagCommand("948f421c-edde-4518-a391-09299cc25dc2",
            "8b4637b6-5629-46de-8fb2-d0b0502bfa85", "8d097a79-a38d-4db4-8a41-16f15d9c5afa",
            "a329662e-1805-4a89-9b05-2b818ea35978", "d5e3f5c5-97ed-41b6-9b6f-7f696b9eddeb"
        , "92e989bb-325e-4c16-9d32-ad89166feeab");
        Tag tag = mock(Tag.class);
        VirtualNetwork virtualNetwork = mock(VirtualNetwork.class);
        VirtualMachine virtualMachine = mock(VirtualMachine.class);
        VirtualMachineInterface virtualMachineInterface = mock(VirtualMachineInterface.class);
        NetworkPolicy networkPolicy = mock(NetworkPolicy.class);
        doReturn(List.of(tag)).when(tungstenApi).listTungstenTag(anyString(), anyString(), anyString(), anyString(), anyString(), anyString());
        doReturn(List.of(virtualNetwork)).when(tungstenApi).getBackRefFromVirtualNetwork(eq(VirtualNetwork.class), anyList());
        doReturn(List.of(virtualMachine)).when(tungstenApi).getBackRefFromVirtualMachine(eq(VirtualMachine.class), anyList());
        doReturn(List.of(virtualMachineInterface)).when(tungstenApi).getBackRefFromVirtualMachineInterface(eq(VirtualMachineInterface.class), anyList());
        doReturn(List.of(networkPolicy)).when(tungstenApi).getBackRefFromNetworkPolicy(eq(NetworkPolicy.class), anyList());

        TungstenAnswer answer = (TungstenAnswer) tungstenResource.executeRequest(command);
        assertTrue(answer.getResult());
        TungstenTag tungstenTag = (TungstenTag) answer.getTungstenModelList().get(0);
        assertEquals(virtualNetwork, tungstenTag.getVirtualNetworkList().get(0));
        assertEquals(virtualMachine, tungstenTag.getVirtualMachineList().get(0));
        assertEquals(virtualMachineInterface, tungstenTag.getVirtualMachineInterfaceList().get(0));
        assertEquals(networkPolicy, tungstenTag.getNetworkPolicyList().get(0));
    }

    @Test
    public void executeRequestListTungstenTagTypeCommandTest() {
        TungstenCommand command = new ListTungstenTagTypeCommand("22f28f5a-1099-11ec-82a8-0242ac130003");
        TagType tagType1 = mock(TagType.class);
        TagType tagType2 = mock(TagType.class);
        Answer<List<ApiObjectBase>> tagTypesAnswer = setupApiObjectBaseListAnswer(tagType1, tagType2);
        when(tungstenApi.listTungstenTagType(anyString())).thenAnswer(tagTypesAnswer);
        TungstenAnswer answer = (TungstenAnswer) tungstenResource.executeRequest(command);
        assertTrue(answer.getResult());
        assertNotNull(answer.getApiObjectBaseList());
    }

    @Test
    public void executeRequestApplyTungstenTagCommandTest() {
        TungstenCommand command = new ApplyTungstenTagCommand(null, null, null, "22f28f5a-1099-11ec-82a8-0242ac130003",
            null, "f5ba12c8-d4c5-4c20-a57d-67a9b6fca652");
        Tag tag = mock(Tag.class);
        VirtualNetwork virtualNetwork1 = mock(VirtualNetwork.class);
        VirtualNetwork virtualNetwork2 = mock(VirtualNetwork.class);
        Answer<List<ApiObjectBase>> virtualNetworksAnswer = setupApiObjectBaseListAnswer(virtualNetwork1, virtualNetwork2);
        VirtualMachine virtualMachine1 = mock(VirtualMachine.class);
        VirtualMachine virtualMachine2 = mock(VirtualMachine.class);
        Answer<List<ApiObjectBase>> virtualMachinesAnswer = setupApiObjectBaseListAnswer(virtualMachine1, virtualMachine2);
        VirtualMachineInterface virtualMachineInterface1 = mock(VirtualMachineInterface.class);
        VirtualMachineInterface virtualMachineInterface2 = mock(VirtualMachineInterface.class);
        Answer<List<ApiObjectBase>> virtualMachineInterfacesAnswer = setupApiObjectBaseListAnswer(virtualMachineInterface1, virtualMachineInterface2);
        NetworkPolicy networkPolicy1 = mock(NetworkPolicy.class);
        NetworkPolicy networkPolicy2 = mock(NetworkPolicy.class);
        Answer<List<ApiObjectBase>> networkPoliciesAnswer = setupApiObjectBaseListAnswer(networkPolicy1, networkPolicy2);
        ApplicationPolicySet applicationPolicySet1 = mock(ApplicationPolicySet.class);
        ApplicationPolicySet applicationPolicySet2 = mock(ApplicationPolicySet.class);
        Answer<List<ApiObjectBase>> applicationPoliciesAnswer = setupApiObjectBaseListAnswer(applicationPolicySet1, applicationPolicySet2);
        when(tungstenApi.applyTungstenPolicyTag(anyString(), anyString())).thenReturn(true);
        when(tungstenApi.getTungstenObject(eq(Tag.class), anyString())).thenReturn(tag);
        when(tungstenApi.getBackRefFromVirtualNetwork(eq(VirtualNetwork.class), anyList())).thenAnswer(virtualNetworksAnswer);
        when(tungstenApi.getBackRefFromVirtualMachine(eq(VirtualMachine.class), anyList())).thenAnswer(virtualMachinesAnswer);
        when(tungstenApi.getBackRefFromVirtualMachineInterface(eq(VirtualMachineInterface.class), anyList())).thenAnswer(virtualMachineInterfacesAnswer);
        when(tungstenApi.getBackRefFromNetworkPolicy(eq(NetworkPolicy.class), anyList())).thenAnswer(networkPoliciesAnswer);
        when(tungstenApi.getBackRefFromApplicationPolicySet(eq(ApplicationPolicySet.class), anyList())).thenAnswer(applicationPoliciesAnswer);
        TungstenAnswer answer = (TungstenAnswer) tungstenResource.executeRequest(command);
        assertTrue(answer.getResult());
        assertNotNull(answer.getTungstenModel());
    }

    @Test
    public void executeRequestRemoveTungstenPolicyCommandTest() {
        TungstenCommand command = new RemoveTungstenPolicyCommand("c8ed82ea-10a1-11ec-82a8-0242ac130003",
                "caa4d57a-10a6-11ec-82a8-0242ac130003");
        NetworkPolicy networkPolicy = mock(NetworkPolicy.class);
        VirtualNetwork virtualNetwork1 = mock(VirtualNetwork.class);
        VirtualNetwork virtualNetwork2 = mock(VirtualNetwork.class);
        List<VirtualNetwork> virtualNetworks = Arrays.asList(virtualNetwork1, virtualNetwork2);
        when(tungstenApi.removeTungstenPolicy(anyString(), anyString())).thenReturn(networkPolicy);
        when(tungstenApi.getNetworksFromNetworkPolicy(any(NetworkPolicy.class))).thenReturn(virtualNetworks);
        TungstenAnswer answer = (TungstenAnswer) tungstenResource.executeRequest(command);
        assertTrue(answer.getResult());
        assertNotNull(answer.getTungstenModel());
    }

    @Test
    public void executeRequestRemoveTungstenTagCommandTest() {
        List<String> networkUuids = Arrays.asList("91d32b4a-10a9-11ec-82a8-0242ac130003", "97007956-10a9-11ec-82a8-0242ac130003");
        List<String> vmUuids = List.of("a2f226ba-10a9-11ec-82a8-0242ac130003");
        List<String> nicUuids = Arrays.asList("af6478e4-10a9-11ec-82a8-0242ac130003", "b30ff54a-10a9-11ec-82a8-0242ac130003");
        TungstenCommand command = new RemoveTungstenTagCommand(networkUuids, vmUuids, nicUuids,
            "c8ed82ea-10a1-11ec-82a8-0242ac130003", "41c2e4a8-1553-4cbb-9d68-0c1173e18c7b",
            "caa4d57a-10a6-11ec-82a8-0242ac130003");
        Tag tag = mock(Tag.class);
        VirtualNetwork virtualNetwork1 = mock(VirtualNetwork.class);
        VirtualNetwork virtualNetwork2 = mock(VirtualNetwork.class);
        Answer<List<ApiObjectBase>> virtualNetworksAnswer = setupApiObjectBaseListAnswer(virtualNetwork1, virtualNetwork2);
        VirtualMachine virtualMachine1 = mock(VirtualMachine.class);
        VirtualMachine virtualMachine2 = mock(VirtualMachine.class);
        Answer<List<ApiObjectBase>> virtualMachinesAnswer = setupApiObjectBaseListAnswer(virtualMachine1, virtualMachine2);
        VirtualMachineInterface virtualMachineInterface1 = mock(VirtualMachineInterface.class);
        VirtualMachineInterface virtualMachineInterface2 = mock(VirtualMachineInterface.class);
        Answer<List<ApiObjectBase>> virtualMachineInterfacesAnswer = setupApiObjectBaseListAnswer(virtualMachineInterface1, virtualMachineInterface2);
        NetworkPolicy networkPolicy1 = mock(NetworkPolicy.class);
        NetworkPolicy networkPolicy2 = mock(NetworkPolicy.class);
        Answer<List<ApiObjectBase>> networkPoliciesAnswer = setupApiObjectBaseListAnswer(networkPolicy1, networkPolicy2);
        ApplicationPolicySet applicationPolicySet1 = mock(ApplicationPolicySet.class);
        ApplicationPolicySet applicationPolicySet2 = mock(ApplicationPolicySet.class);
        Answer<List<ApiObjectBase>> applicationPoliciesAnswer = setupApiObjectBaseListAnswer(applicationPolicySet1, applicationPolicySet2);
        when(tungstenApi.removeTungstenTag(anyList(), anyList(), anyList(), anyString(), anyString(), anyString())).thenReturn(tag);
        when(tungstenApi.getBackRefFromVirtualNetwork(eq(VirtualNetwork.class), anyList())).thenAnswer(virtualNetworksAnswer);
        when(tungstenApi.getBackRefFromVirtualMachine(eq(VirtualMachine.class), anyList())).thenAnswer(virtualMachinesAnswer);
        when(tungstenApi.getBackRefFromVirtualMachineInterface(eq(VirtualMachineInterface.class), anyList())).thenAnswer(virtualMachineInterfacesAnswer);
        when(tungstenApi.getBackRefFromNetworkPolicy(eq(NetworkPolicy.class), anyList())).thenAnswer(networkPoliciesAnswer);
        when(tungstenApi.getBackRefFromApplicationPolicySet(eq(ApplicationPolicySet.class), anyList())).thenAnswer(applicationPoliciesAnswer);
        TungstenAnswer answer = (TungstenAnswer) tungstenResource.executeRequest(command);
        assertTrue(answer.getResult());
        assertNotNull(answer.getTungstenModel());
    }

    @Test
    public void executeRequestCreateTungstenApplicationPolicySetCommandTest() {
        TungstenCommand command = new CreateTungstenApplicationPolicySetCommand("appPolicySetName");
        ApplicationPolicySet applicationPolicySet = mock(ApplicationPolicySet.class);
        when(tungstenApi.createTungstenApplicationPolicySet(anyString(), anyString())).thenReturn(applicationPolicySet);
        TungstenAnswer answer = (TungstenAnswer) tungstenResource.executeRequest(command);
        assertTrue(answer.getResult());
        assertNotNull(answer.getApiObjectBase());
    }

    @Test
    public void executeRequestCreateTungstenFirewallPolicyCommandTest() {
        TungstenCommand command = new CreateTungstenFirewallPolicyCommand("firewallPolicyName", "b30ff54a-10a9-11ec-82a8-0242ac130003", 1);
        FirewallPolicy firewallPolicy = mock(FirewallPolicy.class);
        when(tungstenApi.createTungstenFirewallPolicy(anyString(), anyString(), anyString(), anyInt())).thenReturn(firewallPolicy);
        TungstenAnswer answer = (TungstenAnswer) tungstenResource.executeRequest(command);
        assertTrue(answer.getResult());
        assertNotNull(answer.getApiObjectBase());
    }

    @Test
    public void executeRequestCreateTungstenFirewallRuleCommandTest() {
        TungstenCommand command = new CreateTungstenFirewallRuleCommand("f5ba12c8-d4c5-4c20-a57d-67a9b6fca652",
            "firewallRuleName", "allow", "b30ff54a-10a9-11ec-82a8-0242ac130003", "b220a9d0-10aa-11ec-82a8-0242ac130003",
            "ba7cc71c-10aa-11ec-82a8-0242ac130003", "92e989bb-325e-4c16-9d32-ad89166feeab", "<>",
            "d7723adc-10aa-11ec-82a8-0242ac130003", "dd8e0572-10aa-11ec-82a8-0242ac130003",
            "e4ca41cf-bc0a-4a62-b520-b7785f84b7a3", "e2e59350-10aa-11ec-82a8-0242ac130003", 1);
        FirewallRule firewallRule = mock(FirewallRule.class);
        when(tungstenApi.createTungstenFirewallRule(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString(),
                anyString(), anyString(), anyString(), anyInt())).thenReturn(firewallRule);
        TungstenAnswer answer = (TungstenAnswer) tungstenResource.executeRequest(command);
        assertTrue(answer.getResult());
        assertNotNull(answer.getApiObjectBase());
    }

    @Test
    public void executeRequestCreateTungstenServiceGroupCommandTest() {
        TungstenCommand command = new CreateTungstenServiceGroupCommand("serviceGroupName", "tcp", 80, 90);
        ServiceGroup serviceGroup = mock(ServiceGroup.class);
        when(tungstenApi.createTungstenServiceGroup(anyString(), anyString(), anyString(), anyInt(), anyInt())).thenReturn(serviceGroup);
        TungstenAnswer answer = (TungstenAnswer) tungstenResource.executeRequest(command);
        assertTrue(answer.getResult());
        assertNotNull(answer.getApiObjectBase());
    }

    @Test
    public void executeRequestCreateTungstenAddressGroupCommandTest() {
        TungstenCommand command = new CreateTungstenAddressGroupCommand("addressName", "10.0.0.0", 24);
        AddressGroup addressGroup = mock(AddressGroup.class);
        when(tungstenApi.createTungstenAddressGroup(anyString(), anyString(), anyString(), anyInt())).thenReturn(addressGroup);
        TungstenAnswer answer = (TungstenAnswer) tungstenResource.executeRequest(command);
        assertTrue(answer.getResult());
        assertNotNull(answer.getApiObjectBase());
    }

    @Test
    public void executeRequestListTungstenApplicationPolicySetCommandTest() {
        TungstenCommand command = new ListTungstenApplicationPolicySetCommand("d64e6082-10b3-11ec-82a8-0242ac130003");
        ApplicationPolicySet applicationPolicySet1 = mock(ApplicationPolicySet.class);
        ApplicationPolicySet applicationPolicySet2 = mock(ApplicationPolicySet.class);
        Answer<List<ApiObjectBase>> applicationPolicySetsAnswer = setupApiObjectBaseListAnswer(applicationPolicySet1, applicationPolicySet2);
        when(tungstenApi.listTungstenApplicationPolicySet(anyString())).thenAnswer(applicationPolicySetsAnswer);
        TungstenAnswer answer = (TungstenAnswer) tungstenResource.executeRequest(command);
        assertTrue(answer.getResult());
        assertNotNull(answer.getApiObjectBaseList());
    }

    @Test
    public void executeRequestListTungstenFirewallPolicyCommandTest() {
        TungstenCommand command = new ListTungstenFirewallPolicyCommand("d64e6082-10b3-11ec-82a8-0242ac130003",
                "db1f89d8-10b3-11ec-82a8-0242ac130003");
        FirewallPolicy firewallPolicy1 = mock(FirewallPolicy.class);
        FirewallPolicy firewallPolicy2 = mock(FirewallPolicy.class);
        Answer<List<ApiObjectBase>> firewallPoliciesAnswer = setupApiObjectBaseListAnswer(firewallPolicy1, firewallPolicy2);
        when(tungstenApi.listTungstenFirewallPolicy(anyString(), anyString())).thenAnswer(firewallPoliciesAnswer);
        TungstenAnswer answer = (TungstenAnswer) tungstenResource.executeRequest(command);
        assertTrue(answer.getResult());
        assertNotNull(answer.getApiObjectBaseList());
    }

    @Test
    public void executeRequestListTungstenFirewallRuleCommandTest() {
        TungstenCommand command = new ListTungstenFirewallRuleCommand("d64e6082-10b3-11ec-82a8-0242ac130003",
                "db1f89d8-10b3-11ec-82a8-0242ac130003");
        FirewallRule firewallRule1 = mock(FirewallRule.class);
        FirewallRule firewallRule2 = mock(FirewallRule.class);
        Answer<List<ApiObjectBase>> firewallRulesAnswer = setupApiObjectBaseListAnswer(firewallRule1, firewallRule2);
        when(tungstenApi.listTungstenFirewallRule(anyString(), anyString())).thenAnswer(firewallRulesAnswer);
        TungstenAnswer answer = (TungstenAnswer) tungstenResource.executeRequest(command);
        assertTrue(answer.getResult());
        assertNotNull(answer.getApiObjectBaseList());
    }

    @Test
    public void executeRequestListTungstenServiceGroupCommandTest() {
        TungstenCommand command = new ListTungstenServiceGroupCommand("d64e6082-10b3-11ec-82a8-0242ac130003");
        ServiceGroup serviceGroup1 = mock(ServiceGroup.class);
        ServiceGroup serviceGroup2 = mock(ServiceGroup.class);
        Answer<List<ApiObjectBase>> serviceGroupsAnswer = setupApiObjectBaseListAnswer(serviceGroup1, serviceGroup2);
        when(tungstenApi.listTungstenServiceGroup(anyString())).thenAnswer(serviceGroupsAnswer);
        TungstenAnswer answer = (TungstenAnswer) tungstenResource.executeRequest(command);
        assertTrue(answer.getResult());
        assertNotNull(answer.getApiObjectBaseList());
    }

    @Test
    public void executeRequestListTungstenAddressGroupCommandTest() {
        TungstenCommand command = new ListTungstenAddressGroupCommand("d64e6082-10b3-11ec-82a8-0242ac130003");
        AddressGroup addressGroup1 = mock(AddressGroup.class);
        AddressGroup addressGroup2 = mock(AddressGroup.class);
        Answer<List<ApiObjectBase>> addressGroupsAnswer = setupApiObjectBaseListAnswer(addressGroup1, addressGroup2);
        when(tungstenApi.listTungstenAddressGroup(anyString())).thenAnswer(addressGroupsAnswer);
        TungstenAnswer answer = (TungstenAnswer) tungstenResource.executeRequest(command);
        assertTrue(answer.getResult());
        assertNotNull(answer.getApiObjectBaseList());
    }

    @Test
    public void executeRequestDeleteTungstenApplicationPolicySetCommandTest() {
        TungstenCommand command = new DeleteTungstenApplicationPolicySetCommand("0b27c38c-10b6-11ec-82a8-0242ac130003");
        ApplicationPolicySet applicationPolicySet = mock(ApplicationPolicySet.class);
        when(tungstenApi.getTungstenObject(eq(ApplicationPolicySet.class), anyString())).thenReturn(applicationPolicySet);
        when(tungstenApi.deleteTungstenObject(any(ApplicationPolicySet.class))).thenReturn(true);
        TungstenAnswer answer = (TungstenAnswer) tungstenResource.executeRequest(command);
        assertTrue(answer.getResult());
    }

    @Test
    public void executeRequestDeleteTungstenFirewallPolicyCommandTest() {
        TungstenCommand command = new DeleteTungstenFirewallPolicyCommand("0b27c38c-10b6-11ec-82a8-0242ac130003");
        FirewallPolicy firewallPolicy = mock(FirewallPolicy.class);
        when(tungstenApi.getTungstenObject(eq(FirewallPolicy.class), anyString())).thenReturn(firewallPolicy);
        when(tungstenApi.deleteTungstenObject(any(FirewallPolicy.class))).thenReturn(true);
        TungstenAnswer answer = (TungstenAnswer) tungstenResource.executeRequest(command);
        assertTrue(answer.getResult());
    }

    @Test
    public void executeRequestDeleteTungstenFirewallRuleCommandTest() {
        TungstenCommand command = new DeleteTungstenFirewallRuleCommand("c2f8e1f8-10b6-11ec-82a8-0242ac130003");
        FirewallRule firewallRule = mock(FirewallRule.class);
        when(tungstenApi.getTungstenObject(eq(FirewallRule.class), anyString())).thenReturn(firewallRule);
        when(tungstenApi.deleteTungstenObject(any(FirewallRule.class))).thenReturn(true);
        TungstenAnswer answer = (TungstenAnswer) tungstenResource.executeRequest(command);
        assertTrue(answer.getResult());
    }

    @Test
    public void executeRequestDeleteTungstenServiceGroupCommandTest() {
        TungstenCommand command = new DeleteTungstenServiceGroupCommand("c2f8e1f8-10b6-11ec-82a8-0242ac130003");
        ServiceGroup serviceGroup = mock(ServiceGroup.class);
        when(tungstenApi.getTungstenObject(eq(ServiceGroup.class), anyString())).thenReturn(serviceGroup);
        when(tungstenApi.deleteTungstenObject(any(ServiceGroup.class))).thenReturn(true);
        TungstenAnswer answer = (TungstenAnswer) tungstenResource.executeRequest(command);
        assertTrue(answer.getResult());
    }

    @Test
    public void executeRequestDeleteTungstenAddressGroupCommandTest() {
        TungstenCommand command = new DeleteTungstenAddressGroupCommand("c2f8e1f8-10b6-11ec-82a8-0242ac130003");
        AddressGroup addressGroup = mock(AddressGroup.class);
        when(tungstenApi.getTungstenObject(eq(AddressGroup.class), anyString())).thenReturn(addressGroup);
        when(tungstenApi.deleteTungstenObject(any(AddressGroup.class))).thenReturn(true);
        TungstenAnswer answer = (TungstenAnswer) tungstenResource.executeRequest(command);
        assertTrue(answer.getResult());
    }

    @Test
    public void executeRequestUpdateTungstenVrouterConfigCommandTest() {
        TungstenCommand command = new UpdateTungstenVrouterConfigCommand("forwardingMode");
        GlobalVrouterConfig globalVrouterConfig = mock(GlobalVrouterConfig.class);
        when(tungstenApi.updateTungstenVrouterConfig(anyString())).thenReturn(globalVrouterConfig);
        TungstenAnswer answer = (TungstenAnswer) tungstenResource.executeRequest(command);
        assertTrue(answer.getResult());
        assertNotNull(answer.getApiObjectBase());
    }

    @Test
    public void executeRequestUpdateTungstenDefaultSecurityGroupCommandTest() {
        TungstenCommand command = new UpdateTungstenDefaultSecurityGroupCommand("projectFqn");
        when(tungstenApi.updateTungstenDefaultSecurityGroup(anyString())).thenReturn(true);
        TungstenAnswer answer = (TungstenAnswer) tungstenResource.executeRequest(command);
        assertTrue(answer.getResult());
    }

    @Test
    public void executeRequestCreateTungstenRoutingLogicalRouterCommandTest() {
        TungstenCommand command = new CreateTungstenRoutingLogicalRouterCommand("projectFqn", "logicalRouterName");
        LogicalRouter logicalRouter = mock(LogicalRouter.class);
        VirtualNetwork virtualNetwork1 = mock(VirtualNetwork.class);
        VirtualNetwork virtualNetwork2 = mock(VirtualNetwork.class);
        Answer<List<ApiObjectBase>> virtualNetworksAnswer = setupApiObjectBaseListAnswer(virtualNetwork1, virtualNetwork2);
        when(tungstenApi.createRoutingLogicalRouter(anyString(), anyString(), anyString())).thenReturn(logicalRouter);
        when(tungstenApi.listConnectedNetworkFromLogicalRouter(any(LogicalRouter.class))).thenAnswer(virtualNetworksAnswer);
        TungstenAnswer answer = (TungstenAnswer) tungstenResource.executeRequest(command);
        assertTrue(answer.getResult());
        assertNotNull(answer.getTungstenModel());
    }

    @Test
    public void executeRequestAddTungstenNetworkGatewayToLogicalRouterCommandTest() {
        TungstenCommand command = new AddTungstenNetworkGatewayToLogicalRouterCommand("c2f8e1f8-10b6-11ec-82a8-0242ac130003",
                "439a7efc-113e-11ec-82a8-0242ac130003", "10.0.0.0");
        LogicalRouter logicalRouter = mock(LogicalRouter.class);
        VirtualNetwork virtualNetwork1 = mock(VirtualNetwork.class);
        VirtualNetwork virtualNetwork2 = mock(VirtualNetwork.class);
        Answer<List<ApiObjectBase>> virtualNetworksAnswer = setupApiObjectBaseListAnswer(virtualNetwork1, virtualNetwork2);
        when(tungstenApi.addNetworkGatewayToLogicalRouter(anyString(), anyString(), anyString())).thenReturn(logicalRouter);
        when(tungstenApi.listConnectedNetworkFromLogicalRouter(any(LogicalRouter.class))).thenAnswer(virtualNetworksAnswer);
        TungstenAnswer answer = (TungstenAnswer) tungstenResource.executeRequest(command);
        assertTrue(answer.getResult());
        assertNotNull(answer.getTungstenModel());
    }

    @Test
    public void executeRequestRemoveTungstenNetworkGatewayFromLogicalRouterCommandTest() {
        TungstenCommand command = new RemoveTungstenNetworkGatewayFromLogicalRouterCommand("c2f8e1f8-10b6-11ec-82a8-0242ac130003",
                "439a7efc-113e-11ec-82a8-0242ac130003");
        LogicalRouter logicalRouter = mock(LogicalRouter.class);
        VirtualNetwork virtualNetwork1 = mock(VirtualNetwork.class);
        VirtualNetwork virtualNetwork2 = mock(VirtualNetwork.class);
        List<VirtualNetwork> virtualNetworks = Arrays.asList(virtualNetwork1, virtualNetwork2);
        when(tungstenApi.removeNetworkGatewayFromLogicalRouter(anyString(), anyString())).thenReturn(logicalRouter);
        when(tungstenApi.listConnectedNetworkFromLogicalRouter(any(LogicalRouter.class))).thenReturn(virtualNetworks);
        TungstenAnswer answer = (TungstenAnswer) tungstenResource.executeRequest(command);
        assertTrue(answer.getResult());
        assertNotNull(answer.getTungstenModel());
    }

    @Test
    public void executeRequestListTungstenRoutingLogicalRouterCommandTest() {
        TungstenCommand command = new ListTungstenRoutingLogicalRouterCommand(null, "c2f8e1f8-10b6-11ec-82a8-0242ac130003");
        LogicalRouter logicalRouter1 = mock(LogicalRouter.class);
        LogicalRouter logicalRouter2 = mock(LogicalRouter.class);
        Answer<List<ApiObjectBase>> logicalRoutersAnswer = setupApiObjectBaseListAnswer(logicalRouter1, logicalRouter2);
        VirtualNetwork virtualNetwork1 = mock(VirtualNetwork.class);
        VirtualNetwork virtualNetwork2 = mock(VirtualNetwork.class);
        List<VirtualNetwork> virtualNetworks = Arrays.asList(virtualNetwork1, virtualNetwork2);
        when(tungstenApi.listRoutingLogicalRouter(anyString())).thenAnswer(logicalRoutersAnswer);
        when(tungstenApi.listConnectedNetworkFromLogicalRouter(any(LogicalRouter.class))).thenReturn(virtualNetworks);
        TungstenAnswer answer = (TungstenAnswer) tungstenResource.executeRequest(command);
        assertTrue(answer.getResult());
        assertNotNull(answer.getTungstenModelList());
    }

    @Test
    public void executeRequestDeleteTungstenRoutingLogicalRouterCommandSuccessTest() {
        TungstenCommand command = new DeleteTungstenRoutingLogicalRouterCommand("c2f8e1f8-10b6-11ec-82a8-0242ac130003");
        LogicalRouter logicalRouter = mock(LogicalRouter.class);

        when(tungstenApi.getTungstenObject(eq(LogicalRouter.class), anyString())).thenReturn(logicalRouter);
        when(tungstenApi.deleteTungstenObject(any(LogicalRouter.class))).thenReturn(true);

        TungstenAnswer answer = (TungstenAnswer) tungstenResource.executeRequest(command);
        assertTrue(answer.getResult());
    }

    @Test
    public void executeRequestDeleteTungstenRoutingLogicalRouterCommandFailTest() {
        TungstenCommand command = new DeleteTungstenRoutingLogicalRouterCommand("c2f8e1f8-10b6-11ec-82a8-0242ac130003");
        LogicalRouter logicalRouter = mock(LogicalRouter.class);
        ObjectReference objectReference = mock(ObjectReference.class);
        VirtualMachineInterface virtualMachineInterface = mock(VirtualMachineInterface.class);

        when(tungstenApi.getTungstenObject(eq(LogicalRouter.class), anyString())).thenReturn(logicalRouter);
        when(tungstenApi.deleteTungstenObject(any(LogicalRouter.class))).thenReturn(true);

        TungstenAnswer answer = (TungstenAnswer) tungstenResource.executeRequest(command);
        assertTrue(answer.getResult());
    }


    private <N extends ApiObjectBase> Answer<List<N>> setupApiObjectBaseListAnswer(N... values) {

        final List<N> list = new ArrayList<>(Arrays.asList(values));

        return invocation -> list;
    }
}
