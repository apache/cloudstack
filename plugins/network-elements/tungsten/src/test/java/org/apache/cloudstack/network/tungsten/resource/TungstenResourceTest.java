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
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import net.juniper.tungsten.api.ApiObjectBase;
import net.juniper.tungsten.api.ApiPropertyBase;
import net.juniper.tungsten.api.ObjectReference;
import net.juniper.tungsten.api.types.Domain;
import net.juniper.tungsten.api.types.FloatingIp;
import net.juniper.tungsten.api.types.FloatingIpPool;
import net.juniper.tungsten.api.types.InstanceIp;
import net.juniper.tungsten.api.types.Loadbalancer;
import net.juniper.tungsten.api.types.LoadbalancerHealthmonitor;
import net.juniper.tungsten.api.types.LoadbalancerListener;
import net.juniper.tungsten.api.types.LoadbalancerMember;
import net.juniper.tungsten.api.types.LoadbalancerPool;
import net.juniper.tungsten.api.types.LogicalRouter;
import net.juniper.tungsten.api.types.NetworkPolicy;
import net.juniper.tungsten.api.types.Project;
import net.juniper.tungsten.api.types.VirtualMachine;
import net.juniper.tungsten.api.types.VirtualMachineInterface;
import net.juniper.tungsten.api.types.VirtualNetwork;
import org.apache.cloudstack.network.tungsten.agent.api.ApplyTungstenNetworkPolicyCommand;
import org.apache.cloudstack.network.tungsten.agent.api.AssignTungstenFloatingIpCommand;
import org.apache.cloudstack.network.tungsten.agent.api.ClearTungstenNetworkGatewayCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenDomainCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenFloatingIpCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenFloatingIpPoolCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenLogicalRouterCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenNetworkCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenNetworkLoadbalancerCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenNetworkPolicyCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenProjectCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenVirtualMachineCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenDomainCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenFloatingIpCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenFloatingIpPoolCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenLoadBalancerCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenLoadBalancerListenerCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenNetworkCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenNetworkPolicyCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenProjectCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenVRouterPortCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenVmCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenVmInterfaceCommand;
import org.apache.cloudstack.network.tungsten.agent.api.GetTungstenFabricNetworkCommand;
import org.apache.cloudstack.network.tungsten.agent.api.GetTungstenFloatingIpsCommand;
import org.apache.cloudstack.network.tungsten.agent.api.GetTungstenNatIpCommand;
import org.apache.cloudstack.network.tungsten.agent.api.GetTungstenNetworkDnsCommand;
import org.apache.cloudstack.network.tungsten.agent.api.GetTungstenPolicyCommand;
import org.apache.cloudstack.network.tungsten.agent.api.ReleaseTungstenFloatingIpCommand;
import org.apache.cloudstack.network.tungsten.agent.api.SetTungstenNetworkGatewayCommand;
import org.apache.cloudstack.network.tungsten.agent.api.TungstenAnswer;
import org.apache.cloudstack.network.tungsten.agent.api.TungstenCommand;
import org.apache.cloudstack.network.tungsten.agent.api.UpdateLoadBalancerServiceInstanceCommand;
import org.apache.cloudstack.network.tungsten.model.TungstenLoadBalancerMember;
import org.apache.cloudstack.network.tungsten.model.TungstenNetworkPolicy;
import org.apache.cloudstack.network.tungsten.model.TungstenRule;
import org.apache.cloudstack.network.tungsten.service.TungstenApi;
import org.apache.cloudstack.network.tungsten.service.TungstenVRouterApi;
import org.apache.cloudstack.network.tungsten.vrouter.Port;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.naming.ConfigurationException;

@RunWith(PowerMockRunner.class)
@PrepareForTest({TungstenVRouterApi.class, TungstenResource.class, TungstenNetworkPolicy.class})
public class TungstenResourceTest {
    @Mock
    TungstenApi tungstenApi;

    TungstenResource tungstenResource;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        tungstenResource = new TungstenResource();
        tungstenResource.tungstenApi = tungstenApi;
        Whitebox.setInternalState(tungstenResource, "vrouterPort", "9091");
        mockStatic(TungstenVRouterApi.class);

        Project project = mock(Project.class);
        when(project.getUuid()).thenReturn("065eab99-b819-4f3f-8e97-99c2ab22e6ed");
        when(tungstenApi.getTungstenProjectByFqn(any())).thenReturn(project);
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

    @Test(expected = ConfigurationException.class)
    public void configureFailTest() throws ConfigurationException {
        Map<String, Object> map = new HashMap<>();
        assertTrue(tungstenResource.configure("tungsten", map));
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
            "guest", "10.1.1.100");
        VirtualMachineInterface virtualMachineInterface = mock(VirtualMachineInterface.class);
        VirtualMachine virtualMachine = mock(VirtualMachine.class);
        VirtualNetwork virtualNetwork = mock(VirtualNetwork.class);
        InstanceIp instanceIp = mock(InstanceIp.class);
        when(virtualMachineInterface.getUuid()).thenReturn("b604c7f7-1dbc-42d8-bceb-2c0898034a7a");
        when(tungstenApi.getTungstenObject(eq(VirtualNetwork.class), anyString())).thenReturn(virtualNetwork);
        when(tungstenApi.createTungstenVirtualMachine(anyString(), anyString())).thenReturn(virtualMachine);
        when(tungstenApi.createTungstenVmInterface(anyString(), anyString(), anyString(), anyString(), anyString(),
            anyString())).thenReturn(virtualMachineInterface);
        when(tungstenApi.createTungstenInstanceIp(anyString(), anyString(), anyString(), anyString())).thenReturn(
            instanceIp);
        when(TungstenVRouterApi.addTungstenVrouterPort(anyString(), anyString(), any(Port.class))).thenReturn(true);
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
        ;
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
        when(TungstenVRouterApi.deleteTungstenVrouterPort(anyString(), anyString(), anyString())).thenReturn(true);
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
        List<TungstenRule> tungstenRuleList = Arrays.asList(tungstenRule);
        TungstenCommand command = new CreateTungstenNetworkPolicyCommand("policy", "projectFqn", tungstenRuleList);
        NetworkPolicy networkPolicy = mock(NetworkPolicy.class);
        when(
            tungstenApi.createOrUpdateTungstenNetworkPolicy(anyString(), anyString(), eq(tungstenRuleList))).thenReturn(
            networkPolicy);
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
        TungstenNetworkPolicy tungstenNetworkPolicy = mock(TungstenNetworkPolicy.class);
        whenNew(TungstenNetworkPolicy.class).withArguments(networkPolicy, virtualNetworkList)
            .thenReturn(tungstenNetworkPolicy);
        when(networkPolicy.getUuid()).thenReturn("ac617be6-bf80-4086-9d6a-c05ff78e2264");
        when(tungstenApi.getTungstenObjectByName(eq(NetworkPolicy.class), any(), anyString())).thenReturn(
            networkPolicy);
        when(tungstenApi.applyTungstenNetworkPolicy(anyString(), anyString(), anyInt(), anyInt())).thenReturn(
            networkPolicy);
        doReturn(virtualNetworkList).when(tungstenApi).getNetworksFromNetworkPolicy(any(NetworkPolicy.class));
        TungstenAnswer answer = (TungstenAnswer) tungstenResource.executeRequest(command);
        assertTrue(answer.getResult());
        assertEquals(tungstenNetworkPolicy, answer.getTungstenModel());
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
        when(tungstenApi.getTungstenObjectByName(eq(FloatingIpPool.class), any(), anyString())).thenReturn(
            floatingIpPool);
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
        when(loadbalancer.getLoadbalancerListenerBackRefs()).thenReturn(Arrays.asList(listerner));
        when(loadbalancerListener.getLoadbalancerPoolBackRefs()).thenReturn(Arrays.asList(pool));
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
}
