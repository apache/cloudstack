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
import com.vmware.nsx.model.TransportZone;
import com.vmware.nsx.model.TransportZoneListResult;
import com.vmware.nsx_policy.model.EnforcementPoint;
import com.vmware.nsx_policy.model.EnforcementPointListResult;
import com.vmware.nsx_policy.model.Site;
import com.vmware.nsx_policy.model.SiteListResult;
import junit.framework.Assert;
import org.apache.cloudstack.NsxAnswer;
import org.apache.cloudstack.agent.api.CreateNsxDistributedFirewallRulesCommand;
import org.apache.cloudstack.agent.api.CreateNsxLoadBalancerRuleCommand;
import org.apache.cloudstack.agent.api.CreateNsxPortForwardRuleCommand;
import org.apache.cloudstack.agent.api.CreateNsxSegmentCommand;
import org.apache.cloudstack.agent.api.CreateNsxStaticNatCommand;
import org.apache.cloudstack.agent.api.CreateNsxTier1GatewayCommand;
import org.apache.cloudstack.agent.api.DeleteNsxDistributedFirewallRulesCommand;
import org.apache.cloudstack.agent.api.DeleteNsxNatRuleCommand;
import org.apache.cloudstack.agent.api.DeleteNsxSegmentCommand;
import org.apache.cloudstack.agent.api.DeleteNsxTier1GatewayCommand;
import org.apache.cloudstack.agent.api.NsxCommand;
import org.apache.cloudstack.service.NsxApiClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import javax.naming.ConfigurationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class NsxResourceTest {

    @Mock
    NsxApiClient nsxApi;

    NsxResource nsxResource;
    AutoCloseable closeable;
    @Mock
    EnforcementPointListResult enforcementPointListResult;
    @Mock
    SiteListResult siteListResult;
    @Mock
    TransportZoneListResult transportZoneListResult;

    @Before
    public void setup() {
        closeable = MockitoAnnotations.openMocks(this);
        nsxResource = new NsxResource();
        nsxResource.nsxApiClient = nsxApi;
        nsxResource.transportZone = "Overlay";
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
        params.put("tier0Gateway", "Tier0-GW01");
        params.put("edgeCluster", "EdgeCluster");
        params.put("transportZone", "Overlay");
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
        NsxCommand command = new CreateNsxTier1GatewayCommand(1L, 2L,
                1L, 3L, "VPC01", true, false);

        NsxAnswer answer = (NsxAnswer) nsxResource.executeRequest(command);
        assertTrue(answer.getResult());
    }

    @Test
    public void testDeleteTier1Gateway() {
        NsxCommand command = new DeleteNsxTier1GatewayCommand(1L, 1L,
                1L, 2L, "VPC01", true);

        NsxAnswer answer = (NsxAnswer) nsxResource.executeRequest(command);
        assertTrue(answer.getResult());
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
        List<TransportZone> transportZoneList = List.of(new TransportZone.Builder().setDisplayName("Overlay").build());

        NsxCommand command = new CreateNsxSegmentCommand(1L, 1L,
                1L, 2L, "VPC01", 3L, "Web", "10.10.10.1", "10.10.10.0/24");

        when(nsxApi.getSites()).thenReturn(siteListResult);
        when(siteListResult.getResults()).thenReturn(siteList);
        when(siteList.get(0).getId()).thenReturn("site1");

        when(nsxApi.getEnforcementPoints(anyString())).thenReturn(enforcementPointListResult);
        when(enforcementPointListResult.getResults()).thenReturn(enforcementPointList);
        when(enforcementPointList.get(0).getPath()).thenReturn("enforcementPointPath");

        when(nsxApi.getTransportZones()).thenReturn(transportZoneListResult);
        when(transportZoneListResult.getResults()).thenReturn(transportZoneList);

        NsxAnswer answer = (NsxAnswer) nsxResource.executeRequest(command);
        assertTrue(answer.getResult());
    }

    @Test
    public void testDeleteNsxSegment() {
        NetworkVO tierNetwork = new NetworkVO();
        tierNetwork.setName("tier1");
        DeleteNsxSegmentCommand command = new DeleteNsxSegmentCommand(1L, 1L, 1L, 3L, "VPC01", 2L, "Web");

        NsxAnswer answer = (NsxAnswer) nsxResource.executeRequest(command);
        assertTrue(answer.getResult());
    }

    @Test
    public void testCreateStaticNat() {
        CreateNsxStaticNatCommand cmd = new CreateNsxStaticNatCommand(1L, 1L, 1L, 3L, "VPC01", true, 2L, "10.1.12.10", "172.30.20.12");
        NsxAnswer answer = (NsxAnswer) nsxResource.executeRequest(cmd);
        assertTrue(answer.getResult());
    }

    @Test
    public void testCreatePortForwardRule() {
        CreateNsxPortForwardRuleCommand cmd = new CreateNsxPortForwardRuleCommand(1L, 1L, 1L, 3L, "VPC01", true, 2L, 5L, "10.1.12.10", "172.30.20.12", "2222", "22", "tcp");
        NsxAnswer answer = (NsxAnswer) nsxResource.executeRequest(cmd);
        assertTrue(answer.getResult());
    }

    @Test
    public void testDeleteNsxNatRule() {
        DeleteNsxNatRuleCommand cmd = new DeleteNsxNatRuleCommand(1L, 1L, 1L, 3L, "VPC01", true, 2L, 5L, "22", "tcp");
        NsxAnswer answer = (NsxAnswer) nsxResource.executeRequest(cmd);
        assertTrue(answer.getResult());
    }

    @Test
    public void testCreateNsxLoadBalancerRule() {
        List<NsxLoadBalancerMember> loadBalancerMembers = List.of(new NsxLoadBalancerMember(
                1L, "172.30.20.12", 6443
        ));
        CreateNsxLoadBalancerRuleCommand cmd = new CreateNsxLoadBalancerRuleCommand(1L, 1L, 1L,
                3L, "VPC01", true, loadBalancerMembers, 1L, "6443", "6443", "RoundRobin", "TCP");
        NsxAnswer answer = (NsxAnswer) nsxResource.executeRequest(cmd);
        assertTrue(answer.getResult());
    }


    @Test
    public void testCreateNsxDistributedFirewallRule() {
        List<NsxNetworkRule> networkRules = List.of(new NsxNetworkRule());
        CreateNsxDistributedFirewallRulesCommand cmd = new CreateNsxDistributedFirewallRulesCommand(1L, 1L, 1L,
                3L, 1L, networkRules);
        NsxAnswer answer = (NsxAnswer) nsxResource.executeRequest(cmd);
        assertTrue(answer.getResult());
    }

    @Test
    public void testDeleteNsxDistributedFirewallRule() {
        List<NsxNetworkRule> networkRules = List.of(new NsxNetworkRule());
        DeleteNsxDistributedFirewallRulesCommand cmd = new DeleteNsxDistributedFirewallRulesCommand(1L, 1L, 1L,
                3L, 1L, networkRules);
        NsxAnswer answer = (NsxAnswer) nsxResource.executeRequest(cmd);
        assertTrue(answer.getResult());
    }


}
