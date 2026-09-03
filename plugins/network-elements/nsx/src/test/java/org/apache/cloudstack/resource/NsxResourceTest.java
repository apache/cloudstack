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
package org.apache.cloudstack.resource;

import com.cloud.agent.api.Command;
import com.cloud.serializer.GsonHelper;
import com.cloud.network.Network;
import com.cloud.network.dao.NetworkVO;
import com.cloud.utils.exception.CloudRuntimeException;
import com.vmware.nsx.model.TransportZone;
import com.vmware.nsx.model.TransportZoneListResult;
import com.vmware.nsx_policy.model.EnforcementPoint;
import com.vmware.nsx_policy.model.Site;
import junit.framework.Assert;
import org.apache.cloudstack.NsxAnswer;
import org.apache.cloudstack.agent.api.CreateNsxDistributedFirewallRulesCommand;
import org.apache.cloudstack.agent.api.CreateNsxLoadBalancerRuleCommand;
import org.apache.cloudstack.agent.api.CreateNsxPortForwardRuleCommand;
import org.apache.cloudstack.agent.api.CreateNsxSegmentCommand;
import org.apache.cloudstack.agent.api.CreateNsxStaticNatCommand;
import org.apache.cloudstack.agent.api.CreateNsxTier1GatewayCommand;
import org.apache.cloudstack.agent.api.CreateOrUpdateNsxTier1NatRuleCommand;
import org.apache.cloudstack.agent.api.CreateNsxVpnConnectionCommand;
import org.apache.cloudstack.agent.api.CreateNsxVpnGatewayCommand;
import org.apache.cloudstack.agent.api.DeleteNsxDistributedFirewallRulesCommand;
import org.apache.cloudstack.agent.api.DeleteNsxNatRuleCommand;
import org.apache.cloudstack.agent.api.DeleteNsxSegmentCommand;
import org.apache.cloudstack.agent.api.DeleteNsxTier1GatewayCommand;
import org.apache.cloudstack.agent.api.DeleteNsxVpnConnectionCommand;
import org.apache.cloudstack.agent.api.DeleteNsxVpnGatewayCommand;
import org.apache.cloudstack.agent.api.GetNsxVpnSessionStatusCommand;
import org.apache.cloudstack.agent.api.NsxCommand;
import org.apache.cloudstack.agent.api.UpdateNsxVpnConnectionStateCommand;
import org.apache.cloudstack.service.NsxApiClient;
import org.apache.cloudstack.utils.NsxControllerUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import javax.naming.ConfigurationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class NsxResourceTest {

    @Mock
    NsxApiClient nsxApi;

    NsxResource nsxResource;
    AutoCloseable closeable;
    @Mock
    TransportZoneListResult transportZoneListResult;

    private static final String transportZone = "Overlay";
    private static final String tier0Gateway = "Tier0-GW01";
    private static final String edgeCluster = "EdgeCluster";

    private static final long domainId = 1L;
    private static final long accountId = 2L;
    private static final long zoneId = 1L;

    @Before
    public void setup() {
        closeable = MockitoAnnotations.openMocks(this);
        nsxResource = new NsxResource();
        nsxResource.nsxApiClient = nsxApi;
        nsxResource.transportZone = transportZone;
        nsxResource.tier0Gateway = tier0Gateway;
        nsxResource.edgeCluster = edgeCluster;
    }

    @After
    public void tearDown() throws Exception {
        closeable.close();
    }

    @Test
    public void testConfigure() throws ConfigurationException {
        Map<String, Object> params = new HashMap<>();
        params.put("name", "nsxController");
        params.put("guid", "5944b356-644f-11ee-b8c2-f37bc1b564ff");
        params.put("zoneId", "1");
        params.put("hostname", "host1");
        params.put("username", "admin");
        params.put("password", "password");
        params.put("tier0Gateway", tier0Gateway);
        params.put("edgeCluster", edgeCluster);
        params.put("transportZone", transportZone);
        params.put("port", "443");

        Assert.assertTrue(nsxResource.configure("nsx", params));
    }

    @Test
    public void testConfigure_MissingParameter() throws ConfigurationException {
        Map<String, Object> params = new HashMap<>();

        assertThrows(ConfigurationException.class, () -> nsxResource.configure("nsx", params));
    }

    @Test
    public void testCreateNsxTier1Gateway() {
        NsxCommand command = new CreateNsxTier1GatewayCommand(domainId, accountId, zoneId,
                3L, "VPC01", true, false);
        NsxAnswer answer = (NsxAnswer) nsxResource.executeRequest(command);
        assertTrue(answer.getResult());
    }

    @Test
    public void testCreateNsxTier1GatewayError() {
        NsxCommand command = new CreateNsxTier1GatewayCommand(domainId, accountId, zoneId,
                3L, "VPC01", true, false);
        Mockito.doThrow(new CloudRuntimeException("ERROR"))
                .when(nsxApi).createTier1Gateway(anyString(), anyString(), anyString(), anyBoolean());
        NsxAnswer answer = (NsxAnswer) nsxResource.executeRequest(command);
        assertFalse(answer.getResult());
    }

    @Test
    public void testDeleteTier1Gateway() {
        NsxCommand command = new DeleteNsxTier1GatewayCommand(domainId, accountId, zoneId,
                2L, "VPC01", true);
        NsxAnswer answer = (NsxAnswer) nsxResource.executeRequest(command);
        assertTrue(answer.getResult());
    }

    @Test
    public void testDeleteTier1GatewayError() {
        NsxCommand command = new DeleteNsxTier1GatewayCommand(domainId, accountId, zoneId,
                2L, "VPC01", true);
        Mockito.doThrow(new CloudRuntimeException("ERROR")).when(nsxApi).deleteTier1Gateway(anyString());
        NsxAnswer answer = (NsxAnswer) nsxResource.executeRequest(command);
        assertFalse(answer.getResult());
    }

    @Test
    public void testCreateNsxSegment() {
        NetworkVO tierNetwork = new NetworkVO();
        tierNetwork.setName("tier1");
        tierNetwork.setCidr("10.0.0.0/8");
        tierNetwork.setGateway("10.0.0.1");
        Site site = mock(Site.class);
        List<Site> siteList = List.of(site);
        EnforcementPoint enforcementPoint = mock(EnforcementPoint.class);
        List<EnforcementPoint> enforcementPointList = List.of(enforcementPoint);
        List<TransportZone> transportZoneList = List.of(new TransportZone.Builder().setDisplayName(transportZone).build());

        NsxCommand command = new CreateNsxSegmentCommand(domainId, accountId, zoneId,
                2L, "VPC01", 3L, "Web", "10.10.10.1", "10.10.10.0/24");

        when(nsxApi.getDefaultSiteId()).thenReturn("site1");

        when(nsxApi.getDefaultEnforcementPointPath(anyString())).thenReturn("enforcementPointPath");

        when(nsxApi.getTransportZones()).thenReturn(transportZoneListResult);
        when(transportZoneListResult.getResults()).thenReturn(transportZoneList);

        NsxAnswer answer = (NsxAnswer) nsxResource.executeRequest(command);
        assertTrue(answer.getResult());
    }

    @Test
    public void testCreateNsxSegmentEmptySites() {
        when(nsxApi.getDefaultSiteId()).thenReturn(null);
        CreateNsxSegmentCommand command = Mockito.mock(CreateNsxSegmentCommand.class);
        NsxAnswer answer = (NsxAnswer) nsxResource.executeRequest(command);
        assertFalse(answer.getResult());
    }

    @Test
    public void testCreateNsxSegmentEmptyEnforcementPoints() {
        Site site = mock(Site.class);
        when(nsxApi.getDefaultSiteId()).thenReturn("site1");
        when(nsxApi.getDefaultEnforcementPointPath(anyString())).thenReturn(null);
        CreateNsxSegmentCommand command = Mockito.mock(CreateNsxSegmentCommand.class);
        NsxAnswer answer = (NsxAnswer) nsxResource.executeRequest(command);
        assertFalse(answer.getResult());
    }

    @Test
    public void testCreateNsxSegmentEmptyTransportZones() {
        Site site = mock(Site.class);
        when(nsxApi.getDefaultSiteId()).thenReturn("site1");
        CreateNsxSegmentCommand command = Mockito.mock(CreateNsxSegmentCommand.class);
        NsxAnswer answer = (NsxAnswer) nsxResource.executeRequest(command);
        assertFalse(answer.getResult());
    }

    @Test
    public void testDeleteNsxSegment() {
        NetworkVO tierNetwork = new NetworkVO();
        tierNetwork.setName("tier1");
        DeleteNsxSegmentCommand command = new DeleteNsxSegmentCommand(domainId, accountId, zoneId,
                3L, "VPC01", 2L, "Web");
        NsxAnswer answer = (NsxAnswer) nsxResource.executeRequest(command);
        assertTrue(answer.getResult());
    }

    @Test
    public void testDeleteNsxSegmentError() {
        NetworkVO tierNetwork = new NetworkVO();
        tierNetwork.setName("tier1");
        DeleteNsxSegmentCommand command = new DeleteNsxSegmentCommand(domainId, accountId, zoneId,
                3L, "VPC01", 2L, "Web");
        doThrow(new CloudRuntimeException("ERROR")).when(nsxApi).deleteSegment(anyLong(), anyLong(), anyLong(), anyLong(), anyLong(), anyString());
        NsxAnswer answer = (NsxAnswer) nsxResource.executeRequest(command);
        assertFalse(answer.getResult());
    }

    @Test
    public void testCreateStaticNat() {
        CreateNsxStaticNatCommand cmd = new CreateNsxStaticNatCommand(domainId, accountId, zoneId, 3L, "VPC01", true, 2L, "10.1.12.10", "172.30.20.12");
        NsxAnswer answer = (NsxAnswer) nsxResource.executeRequest(cmd);
        assertTrue(answer.getResult());
    }

    @Test
    public void testCreatePortForwardRule() {
        CreateNsxPortForwardRuleCommand cmd = new CreateNsxPortForwardRuleCommand(domainId, accountId, zoneId, 3L, "VPC01", true, 2L, 5L, "10.1.12.10", "172.30.20.12", "2222", "22", "tcp");
        NsxAnswer answer = (NsxAnswer) nsxResource.executeRequest(cmd);
        assertTrue(answer.getResult());
    }

    @Test
    public void testDeleteNsxNatRule() {
        DeleteNsxNatRuleCommand cmd = new DeleteNsxNatRuleCommand(domainId, accountId, zoneId, 3L, "VPC01", true, 2L, 5L, "22", "tcp");
        Network.Service service = mock(Network.Service.class);
        when(service.getName()).thenReturn("PortForwarding");
        cmd.setService(service);
        NsxAnswer answer = (NsxAnswer) nsxResource.executeRequest(cmd);
        assertTrue(answer.getResult());
        verify(nsxApi).deleteNatRule(service, "22", "tcp", "VPC01", "D1-A2-Z1-V3", "D1-A2-Z1-V3-PF5");
    }

    @Test
    public void testCreateNsxLoadBalancerRule() {
        List<NsxLoadBalancerMember> loadBalancerMembers = List.of(new NsxLoadBalancerMember(
                1L, "172.30.20.12", 6443
        ));
        CreateNsxLoadBalancerRuleCommand cmd = new CreateNsxLoadBalancerRuleCommand(domainId, accountId, zoneId,
                3L, "VPC01", true, loadBalancerMembers, 1L, "6443", "6443", "RoundRobin", "TCP");
        NsxAnswer answer = (NsxAnswer) nsxResource.executeRequest(cmd);
        assertTrue(answer.getResult());
    }


    @Test
    public void testCreateNsxDistributedFirewallRule() {
        List<NsxNetworkRule> networkRules = List.of(new NsxNetworkRule());
        CreateNsxDistributedFirewallRulesCommand cmd = new CreateNsxDistributedFirewallRulesCommand(domainId, accountId, zoneId,
                3L, 1L, networkRules);
        NsxAnswer answer = (NsxAnswer) nsxResource.executeRequest(cmd);
        assertTrue(answer.getResult());
    }

    @Test
    public void testDeleteNsxDistributedFirewallRule() {
        List<NsxNetworkRule> networkRules = List.of(new NsxNetworkRule());
        DeleteNsxDistributedFirewallRulesCommand cmd = new DeleteNsxDistributedFirewallRulesCommand(domainId, accountId, zoneId,
                3L, 1L, networkRules);
        NsxAnswer answer = (NsxAnswer) nsxResource.executeRequest(cmd);
        assertTrue(answer.getResult());
    }

    @Test
    public void testCreateTier1NatRule() {
        long vpcId = 5L;
        String tier1GatewayName = NsxControllerUtils.getTier1GatewayName(domainId, accountId, zoneId, vpcId, true);
        CreateOrUpdateNsxTier1NatRuleCommand command = new CreateOrUpdateNsxTier1NatRuleCommand(domainId, accountId, zoneId,
                tier1GatewayName, "SNAT", "10.1.10.10", "natRuleId");
        NsxAnswer answer = (NsxAnswer) nsxResource.executeRequest(command);
        assertTrue(answer.getResult());
    }

    @Test
    public void testCreateNsxVpnGatewayRollsBackAfterFailure() {
        CreateNsxVpnGatewayCommand command = new CreateNsxVpnGatewayCommand(domainId, accountId, zoneId,
                3L, "VPC01", "203.0.113.20");
        doThrow(new CloudRuntimeException("ERROR")).when(nsxApi).createVpnService(anyString(), anyString());

        NsxAnswer answer = (NsxAnswer) nsxResource.executeRequest(command);

        assertFalse(answer.getResult());
        assertFalse(answer.isObjectExistent());
        assertFalse(answer.isEndpointMayBeInUse());
        verify(nsxApi).deleteVpnService("D1-A2-Z1-V3");
    }

    @Test
    public void testCreateNsxVpnGatewayRefusesToAdoptExistingService() {
        CreateNsxVpnGatewayCommand command = new CreateNsxVpnGatewayCommand(domainId, accountId, zoneId,
                3L, "VPC01", "203.0.113.20");
        when(nsxApi.isVpnServicePresent("D1-A2-Z1-V3")).thenReturn(true);

        NsxAnswer answer = (NsxAnswer) nsxResource.executeRequest(command);

        assertFalse(answer.getResult());
        assertTrue(answer.isObjectExistent());
        assertFalse(answer.isEndpointMayBeInUse());
        verify(nsxApi, never()).createVpnService(anyString(), anyString());
        verify(nsxApi, never()).deleteVpnService("D1-A2-Z1-V3");
    }

    @Test
    public void testCreateNsxVpnGatewayReconcilesPreviouslyOwnedExistingService() {
        CreateNsxVpnGatewayCommand command = new CreateNsxVpnGatewayCommand(domainId, accountId, zoneId,
                3L, "VPC01", "203.0.113.20", true);
        when(nsxApi.isVpnServicePresent("D1-A2-Z1-V3")).thenReturn(true);

        NsxAnswer answer = (NsxAnswer) nsxResource.executeRequest(command);

        assertTrue(answer.getResult());
        verify(nsxApi).createVpnService("D1-A2-Z1-V3", "203.0.113.20");
        verify(nsxApi, never()).deleteVpnService("D1-A2-Z1-V3");
    }

    @Test
    public void testCreateNsxVpnGatewayReportsPossibleResourceWhenRollbackFails() {
        CreateNsxVpnGatewayCommand command = new CreateNsxVpnGatewayCommand(domainId, accountId, zoneId,
                3L, "VPC01", "203.0.113.20");
        doThrow(new CloudRuntimeException("create failed")).when(nsxApi).createVpnService(anyString(), anyString());
        doThrow(new CloudRuntimeException("rollback failed")).when(nsxApi).deleteVpnService("D1-A2-Z1-V3");

        NsxAnswer answer = (NsxAnswer) nsxResource.executeRequest(command);

        assertFalse(answer.getResult());
        assertTrue(answer.isEndpointMayBeInUse());
    }

    @Test
    public void testCreateNsxVpnConnectionRollsBackNewSessionAfterRouteFailure() {
        CreateNsxVpnConnectionCommand command = new CreateNsxVpnConnectionCommand(domainId, accountId, zoneId,
                3L, "VPC01", 5L, "203.0.113.10", "psk", "aes256-sha256;modp2048",
                "aes256-sha256;modp2048", 86400L, 3600L, true, "ikev2", false,
                List.of("192.168.100.0/24"), "169.254.64.21", "169.254.64.22", 30,
                "10.1.0.0/16", "203.0.113.20");
        when(nsxApi.getRouteBasedVpnSessionLocalVtiIps(anyString(), anyLong())).thenReturn(Set.of());
        when(nsxApi.createRouteBasedVpnSession(anyString(), anyLong(), anyString(), anyString(), anyString(),
                anyString(), anyLong(), anyLong(), anyBoolean(), anyString(), anyBoolean(), anyString(), anyInt()))
                .thenReturn(NsxApiClient.VpnSessionProvisioningResult.CREATED);
        doThrow(new CloudRuntimeException("ERROR")).when(nsxApi).addVpnConnectionRoutes(anyString(), anyLong(), anyList(), anyString(), anyString());

        NsxAnswer answer = (NsxAnswer) nsxResource.executeRequest(command);

        assertFalse(answer.getResult());
        verify(nsxApi).rollbackVpnConnection("D1-A2-Z1-V3", 5L);
    }

    @Test
    public void testCreateNsxVpnConnectionDisablesPreexistingSessionAfterRouteFailure() {
        CreateNsxVpnConnectionCommand command = new CreateNsxVpnConnectionCommand(domainId, accountId, zoneId,
                3L, "VPC01", 5L, "203.0.113.10", "psk", "aes256-sha256;modp2048",
                "aes256-sha256;modp2048", 86400L, 3600L, true, "ikev2", false,
                List.of("192.168.100.0/24"), "169.254.64.21", "169.254.64.22", 30,
                "10.1.0.0/16", "203.0.113.20");
        when(nsxApi.getRouteBasedVpnSessionLocalVtiIps(anyString(), anyLong())).thenReturn(Set.of());
        when(nsxApi.createRouteBasedVpnSession(anyString(), anyLong(), anyString(), anyString(), anyString(),
                anyString(), anyLong(), anyLong(), anyBoolean(), anyString(), anyBoolean(), anyString(), anyInt()))
                .thenReturn(NsxApiClient.VpnSessionProvisioningResult.PREEXISTING);
        doThrow(new CloudRuntimeException("ERROR")).when(nsxApi).addVpnConnectionRoutes(anyString(), anyLong(),
                anyList(), anyString(), anyString());

        NsxAnswer answer = (NsxAnswer) nsxResource.executeRequest(command);

        assertFalse(answer.getResult());
        verify(nsxApi, never()).rollbackVpnConnection(anyString(), anyLong());
        verify(nsxApi).updateVpnConnectionState("D1-A2-Z1-V3", 5L, false);
    }

    @Test
    public void testCreateNsxVpnConnectionEnablesSessionAfterRoutesAndNatExemptions() {
        CreateNsxVpnConnectionCommand command = new CreateNsxVpnConnectionCommand(domainId, accountId, zoneId,
                3L, "VPC01", 5L, "203.0.113.10", "psk", "aes256-sha256;modp2048",
                "aes256-sha256;modp2048", 86400L, 3600L, true, "ikev2", false,
                List.of("192.168.100.0/24"), "169.254.64.21", "169.254.64.22", 30,
                "10.1.0.0/16", "203.0.113.20");
        when(nsxApi.getRouteBasedVpnSessionLocalVtiIps(anyString(), anyLong())).thenReturn(Set.of());
        when(nsxApi.createRouteBasedVpnSession(anyString(), anyLong(), anyString(), anyString(), anyString(),
                anyString(), anyLong(), anyLong(), anyBoolean(), anyString(), anyBoolean(), anyString(), anyInt()))
                .thenReturn(NsxApiClient.VpnSessionProvisioningResult.CREATED);

        NsxAnswer answer = (NsxAnswer) nsxResource.executeRequest(command);

        assertTrue(answer.getResult());
        InOrder inOrder = Mockito.inOrder(nsxApi);
        inOrder.verify(nsxApi).createRouteBasedVpnSession(anyString(), anyLong(), anyString(), anyString(), anyString(),
                anyString(), anyLong(), anyLong(), anyBoolean(), anyString(), anyBoolean(), anyString(), anyInt());
        inOrder.verify(nsxApi).addVpnConnectionRoutes("D1-A2-Z1-V3", 5L,
                List.of("192.168.100.0/24"), "169.254.64.22", "10.1.0.0/16");
        inOrder.verify(nsxApi).ensureVpnNatExemptions("D1-A2-Z1-V3", "203.0.113.20");
        inOrder.verify(nsxApi).updateVpnConnectionState("D1-A2-Z1-V3", 5L, true);
    }

    @Test
    public void testCreateNsxVpnConnectionRejectsVtiCollisionBeforeCreate() {
        CreateNsxVpnConnectionCommand command = new CreateNsxVpnConnectionCommand(domainId, accountId, zoneId,
                3L, "VPC01", 5L, "203.0.113.10", "psk", "aes256-sha256;modp2048",
                "aes256-sha256;modp2048", 86400L, 3600L, true, "ikev2", false,
                List.of("192.168.100.0/24"), "169.254.64.21", "169.254.64.22", 30,
                "10.1.0.0/16", "203.0.113.20");
        when(nsxApi.getRouteBasedVpnSessionLocalVtiIps(anyString(), anyLong())).thenReturn(Set.of("169.254.64.21"));

        NsxAnswer answer = (NsxAnswer) nsxResource.executeRequest(command);

        assertFalse(answer.getResult());
        verify(nsxApi, Mockito.never()).createRouteBasedVpnSession(anyString(), anyLong(), anyString(), anyString(),
                anyString(), anyString(), anyLong(), anyLong(), anyBoolean(), anyString(), anyBoolean(), anyString(), anyInt());
    }

    @Test
    public void testNsxVpnLifecycleCommandsDispatch() {
        DeleteNsxVpnGatewayCommand deleteGateway = new DeleteNsxVpnGatewayCommand(domainId, accountId, zoneId, 3L, "VPC01");
        DeleteNsxVpnConnectionCommand deleteConnection = new DeleteNsxVpnConnectionCommand(domainId, accountId, zoneId,
                3L, "VPC01", 5L);
        UpdateNsxVpnConnectionStateCommand update = new UpdateNsxVpnConnectionStateCommand(domainId, accountId, zoneId,
                3L, "VPC01", 5L, false);
        GetNsxVpnSessionStatusCommand status = new GetNsxVpnSessionStatusCommand(domainId, accountId, zoneId,
                3L, "VPC01", 5L);
        when(nsxApi.getVpnSessionStatus("D1-A2-Z1-V3", 5L)).thenReturn("UP");

        assertTrue(((NsxAnswer) nsxResource.executeRequest(deleteGateway)).getResult());
        assertTrue(((NsxAnswer) nsxResource.executeRequest(deleteConnection)).getResult());
        assertTrue(((NsxAnswer) nsxResource.executeRequest(update)).getResult());
        NsxAnswer statusAnswer = (NsxAnswer) nsxResource.executeRequest(status);
        assertTrue(statusAnswer.getResult());
        assertTrue(statusAnswer.getDetails().contains("UP"));
        verify(nsxApi).deleteVpnService("D1-A2-Z1-V3");
        verify(nsxApi).deleteVpnConnection("D1-A2-Z1-V3", 5L);
        verify(nsxApi).updateVpnConnectionState("D1-A2-Z1-V3", 5L, false);
    }

    @Test
    public void testNsxVpnConnectionCommandRoundTripsThroughCloudStackWireAdaptor() {
        CreateNsxVpnConnectionCommand command = new CreateNsxVpnConnectionCommand(domainId, accountId, zoneId,
                3L, "VPC01", 5L, "203.0.113.10", "secret-psk", "aes256-sha256;modp2048",
                "aes256-sha256;modp2048", 86400L, 3600L, true, "ikev2", true,
                List.of("192.168.100.0/24", "192.168.101.0/24"), "169.254.64.21", "169.254.64.22", 30,
                "10.1.0.0/16", "203.0.113.20");

        String wire = GsonHelper.getGson().toJson(new Command[]{command}, Command[].class);
        String logPayload = GsonHelper.getGsonLogger().toJson(new Command[]{command}, Command[].class);
        Command[] decoded = GsonHelper.getGson().fromJson(wire, Command[].class);

        assertTrue(wire.contains("secret-psk"));
        assertFalse(logPayload.contains("secret-psk"));
        assertFalse(logPayload.contains("\"psk\""));
        assertTrue(decoded[0] instanceof CreateNsxVpnConnectionCommand);
        CreateNsxVpnConnectionCommand decodedCommand = (CreateNsxVpnConnectionCommand) decoded[0];
        assertEquals("secret-psk", decodedCommand.getPsk());
        assertEquals(command.getPeerCidrs(), decodedCommand.getPeerCidrs());
        assertEquals(command.getVtiLocalIp(), decodedCommand.getVtiLocalIp());
        assertEquals(command.getVtiPeerIp(), decodedCommand.getVtiPeerIp());
        assertEquals(command.getIkeLifetime(), decodedCommand.getIkeLifetime());
        assertEquals(command.getEspLifetime(), decodedCommand.getEspLifetime());
    }
}
