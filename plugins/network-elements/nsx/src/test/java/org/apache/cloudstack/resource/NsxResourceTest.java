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
import org.apache.cloudstack.agent.api.DeleteNsxDistributedFirewallRulesCommand;
import org.apache.cloudstack.agent.api.DeleteNsxNatRuleCommand;
import org.apache.cloudstack.agent.api.DeleteNsxSegmentCommand;
import org.apache.cloudstack.agent.api.DeleteNsxTier1GatewayCommand;
import org.apache.cloudstack.agent.api.NsxCommand;
import org.apache.cloudstack.service.NsxApiClient;
import org.apache.cloudstack.utils.NsxControllerUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import javax.naming.ConfigurationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
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
        NsxAnswer answer = (NsxAnswer) nsxResource.executeRequest(cmd);
        assertTrue(answer.getResult());
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
}
