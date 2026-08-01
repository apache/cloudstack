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
package org.apache.cloudstack.service;

import com.cloud.network.Network;
import com.cloud.network.SDNProviderNetworkRule;
import com.cloud.utils.exception.CloudRuntimeException;
import com.vmware.nsx.cluster.Status;
import com.vmware.nsx.model.ClusterStatus;
import com.vmware.nsx.model.ControllerClusterStatus;
import com.vmware.nsx_policy.infra.LbAppProfiles;
import com.vmware.nsx_policy.infra.LbMonitorProfiles;
import com.vmware.nsx_policy.infra.LbPools;
import com.vmware.nsx_policy.infra.LbServices;
import com.vmware.nsx_policy.infra.LbVirtualServers;
import com.vmware.nsx_policy.infra.IpsecVpnDpdProfiles;
import com.vmware.nsx_policy.infra.IpsecVpnIkeProfiles;
import com.vmware.nsx_policy.infra.IpsecVpnTunnelProfiles;
import com.vmware.nsx_policy.infra.Tier1s;
import com.vmware.nsx_policy.infra.tier_1s.IpsecVpnServices;
import com.vmware.nsx_policy.infra.tier_1s.LocaleServices;
import com.vmware.nsx_policy.infra.tier_1s.StaticRoutes;
import com.vmware.nsx_policy.infra.tier_1s.nat.NatRules;
import com.vmware.nsx_policy.infra.domains.Groups;
import com.vmware.nsx_policy.infra.tier_1s.ipsec_vpn_services.Sessions;
import com.vmware.nsx_policy.model.ApiError;
import com.vmware.nsx_policy.model.Group;
import com.vmware.nsx_policy.model.IPSecVpnDpdProfile;
import com.vmware.nsx_policy.model.IPSecVpnIkeProfile;
import com.vmware.nsx_policy.model.IPSecVpnServiceListResult;
import com.vmware.nsx_policy.model.IPSecVpnTunnelProfile;
import com.vmware.nsx_policy.model.LBAppProfileListResult;
import com.vmware.nsx_policy.model.LBIcmpMonitorProfile;
import com.vmware.nsx_policy.model.LBService;
import com.vmware.nsx_policy.model.LBTcpMonitorProfile;
import com.vmware.nsx_policy.model.LBPool;
import com.vmware.nsx_policy.model.LBPoolMember;
import com.vmware.nsx_policy.model.LBVirtualServer;
import com.vmware.nsx_policy.model.PathExpression;
import com.vmware.nsx_policy.model.PolicyNatRule;
import com.vmware.nsx_policy.model.PolicyNatRuleListResult;
import com.vmware.nsx_policy.model.RouteBasedIPSecVpnSession;
import com.vmware.nsx_policy.model.StaticRoutesListResult;
import com.vmware.nsx_policy.model.Tag;
import com.vmware.nsx_policy.model.Tier1;
import com.vmware.vapi.bindings.Service;
import com.vmware.vapi.bindings.Structure;
import com.vmware.vapi.std.errors.Error;
import com.vmware.vapi.std.errors.NotFound;
import org.apache.cloudstack.resource.NsxLoadBalancerMember;
import org.apache.cloudstack.utils.NsxControllerUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.util.List;
import java.util.function.Function;

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class NsxApiClientTest {

    private static final String TIER_1_GATEWAY_NAME = "t1";

    @Mock
    private Function<Class<? extends Service>, Service> nsxService;
    @Mock
    private Groups groupService;

    private NsxApiClient client = new NsxApiClient();

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        client.nsxService = nsxService;
        Mockito.when(nsxService.apply(Groups.class)).thenReturn(groupService);
    }

    @Test
    public void testCreateGroupForSegment() {
        final Group[] groups = new Group[1];
        final PathExpression[] pathExpressions = new PathExpression[1];
        try (MockedConstruction<Group> ignored = Mockito.mockConstruction(Group.class, (mock, context) -> {
            groups[0] = mock;
        }); MockedConstruction<PathExpression> ignoredExp = Mockito.mockConstruction(PathExpression.class, (mock, context) -> {
            pathExpressions[0] = mock;
        })
        ) {
            String segmentName = "segment1";
            client.createGroupForSegment(segmentName);
            Mockito.verify(groupService).patch(NsxApiClient.DEFAULT_DOMAIN, segmentName, groups[0]);
            String segmentPath = String.format("%s/%s", NsxApiClient.SEGMENTS_PATH, segmentName);
            Mockito.verify(groups[0]).setExpression(List.of(pathExpressions[0]));
            Mockito.verify(pathExpressions[0]).setPaths(List.of(segmentPath));
        }
    }

    @Test
    public void testGetGroupsForTrafficIngress() {
        SDNProviderNetworkRule rule = Mockito.mock(SDNProviderNetworkRule.class);
        Mockito.when(rule.getSourceCidrList()).thenReturn(List.of("ANY"));
        Mockito.when(rule.getTrafficType()).thenReturn("Ingress");
        Mockito.when(rule.getService()).thenReturn(Network.Service.NetworkACL);
        String segmentName = "segment";
        List<String> sourceGroups = client.getGroupsForTraffic(rule, segmentName, true);
        List<String> destinationGroups = client.getGroupsForTraffic(rule, segmentName, false);
        Assert.assertEquals(List.of("ANY"), sourceGroups);
        Assert.assertEquals(List.of(String.format("%s/%s", NsxApiClient.GROUPS_PATH_PREFIX, segmentName)), destinationGroups);
    }

    @Test
    public void testGetGroupsForTrafficEgress() {
        SDNProviderNetworkRule rule = Mockito.mock(SDNProviderNetworkRule.class);
        Mockito.when(rule.getSourceCidrList()).thenReturn(List.of("ANY"));
        Mockito.when(rule.getTrafficType()).thenReturn("Egress");
        Mockito.when(rule.getService()).thenReturn(Network.Service.NetworkACL);
        String segmentName = "segment";
        List<String> sourceGroups = client.getGroupsForTraffic(rule, segmentName, true);
        List<String> destinationGroups = client.getGroupsForTraffic(rule, segmentName, false);
        Assert.assertEquals(List.of(String.format("%s/%s", NsxApiClient.GROUPS_PATH_PREFIX, segmentName)), sourceGroups);
        Assert.assertEquals(List.of("ANY"), destinationGroups);
    }

    @Test
    public void testIsNsxControllerActive() {
        Status statusService = Mockito.mock(Status.class);
        Mockito.when(nsxService.apply(Status.class)).thenReturn(statusService);
        ClusterStatus clusterStatus = Mockito.mock(ClusterStatus.class);
        ControllerClusterStatus status = Mockito.mock(ControllerClusterStatus.class);
        Mockito.when(status.getStatus()).thenReturn("stable");
        Mockito.when(statusService.get()).thenReturn(clusterStatus);
        Mockito.when(clusterStatus.getControlClusterStatus()).thenReturn(status);
        Assert.assertTrue(client.isNsxControllerActive());
    }

    @Test
    public void testCreateNsxLbServerPoolExistingMonitorProfileSkipsMonitorPatch() {
        String lbServerPoolName = NsxControllerUtils.getServerPoolName(TIER_1_GATEWAY_NAME, 1L);
        List<NsxLoadBalancerMember> memberList = List.of(new NsxLoadBalancerMember(1L, "10.0.0.1", 80));

        LbPools lbPools = Mockito.mock(LbPools.class);
        LbMonitorProfiles lbMonitorProfiles = mockLbMonitorProfiles();

        Mockito.when(nsxService.apply(LbPools.class)).thenReturn(lbPools);
        Mockito.when(lbPools.get(lbServerPoolName)).thenThrow(new NotFound(null, null));

        client.createNsxLbServerPool(memberList, TIER_1_GATEWAY_NAME, lbServerPoolName, "roundrobin", "80", "TCP");

        verify(lbMonitorProfiles, never()).patch(anyString(), any(LBTcpMonitorProfile.class));
        verify(lbPools).patch(eq(lbServerPoolName), any(LBPool.class));
    }

    @Test
    public void testCreateNsxLbServerPoolMissingMonitorTCPProfilePerformsPatch() {
        String lbServerPoolName = NsxControllerUtils.getServerPoolName(TIER_1_GATEWAY_NAME, 1L);
        List<NsxLoadBalancerMember> memberList = List.of(new NsxLoadBalancerMember(1L, "10.0.0.1", 80));

        LbPools lbPools = Mockito.mock(LbPools.class);
        LbMonitorProfiles lbMonitorProfiles = Mockito.mock(LbMonitorProfiles.class);
        Structure monitorStructure = Mockito.mock(Structure.class, Mockito.RETURNS_DEEP_STUBS);

        Mockito.when(nsxService.apply(LbPools.class)).thenReturn(lbPools);
        Mockito.when(nsxService.apply(LbMonitorProfiles.class)).thenReturn(lbMonitorProfiles);
        Mockito.when(lbMonitorProfiles.get(anyString())).thenThrow(new NotFound(null, null)).thenReturn(monitorStructure);
        Mockito.when(monitorStructure._getDataValue().getField("path").toString()).thenReturn("/infra/lb-monitor-profiles/test");
        Mockito.when(lbPools.get(lbServerPoolName)).thenThrow(new NotFound(null, null));

        client.createNsxLbServerPool(memberList, TIER_1_GATEWAY_NAME, lbServerPoolName, "roundrobin", "80", "TCP");

        verify(lbMonitorProfiles).patch(anyString(), any(LBTcpMonitorProfile.class));
        verify(lbPools).patch(eq(lbServerPoolName), any(LBPool.class));
    }

    @Test
    public void testCreateNsxLbServerPoolMissingMonitorUDPProfilePerformsPatch() {
        String lbServerPoolName = NsxControllerUtils.getServerPoolName(TIER_1_GATEWAY_NAME, 1L);
        List<NsxLoadBalancerMember> memberList = List.of(new NsxLoadBalancerMember(1L, "10.0.0.1", 80));

        LbPools lbPools = Mockito.mock(LbPools.class);
        LbMonitorProfiles lbMonitorProfiles = Mockito.mock(LbMonitorProfiles.class);
        Structure monitorStructure = Mockito.mock(Structure.class, Mockito.RETURNS_DEEP_STUBS);

        Mockito.when(nsxService.apply(LbPools.class)).thenReturn(lbPools);
        Mockito.when(nsxService.apply(LbMonitorProfiles.class)).thenReturn(lbMonitorProfiles);
        Mockito.when(lbMonitorProfiles.get(anyString())).thenThrow(new NotFound(null, null)).thenReturn(monitorStructure);
        Mockito.when(monitorStructure._getDataValue().getField("path").toString()).thenReturn("/infra/lb-monitor-profiles/test");
        Mockito.when(lbPools.get(lbServerPoolName)).thenThrow(new NotFound(null, null));

        client.createNsxLbServerPool(memberList, TIER_1_GATEWAY_NAME, lbServerPoolName, "roundrobin", "80", "UDP");

        verify(lbMonitorProfiles).patch(anyString(), any(LBIcmpMonitorProfile.class));
        verify(lbPools).patch(eq(lbServerPoolName), any(LBPool.class));
    }

    @Test
    public void testCreateNsxLbServerPoolPoolExistsWithSameMembersSkipsPatch() {
        long lbId = 1L;
        String lbServerPoolName = NsxControllerUtils.getServerPoolName(TIER_1_GATEWAY_NAME, lbId);
        List<NsxLoadBalancerMember> memberList = List.of(
                new NsxLoadBalancerMember(1L, "10.0.0.1", 80),
                new NsxLoadBalancerMember(2L, "10.0.0.2", 80)
        );
        List<LBPoolMember> sameMembers = List.of(
                createPoolMember(2L, "10.0.0.2", 80),
                createPoolMember(1L, "10.0.0.1", 80)
        );

        LbPools lbPools = Mockito.mock(LbPools.class);
        LBPool existingPool = Mockito.mock(LBPool.class);

        Mockito.when(nsxService.apply(LbPools.class)).thenReturn(lbPools);
        Mockito.when(lbPools.get(lbServerPoolName)).thenReturn(existingPool);
        Mockito.when(existingPool.getMembers()).thenReturn(sameMembers);

        client.createNsxLbServerPool(memberList, TIER_1_GATEWAY_NAME, lbServerPoolName, "roundrobin", "80", "TCP");

        verify(nsxService, never()).apply(LbMonitorProfiles.class);
        verify(lbPools, never()).patch(anyString(), any(LBPool.class));
    }

    @Test
    public void testCreateNsxLbServerPoolPoolExistsWithoutMembersAndEmptyUpdateSkipsPatch() {
        String lbServerPoolName = NsxControllerUtils.getServerPoolName(TIER_1_GATEWAY_NAME, 1L);

        LbPools lbPools = Mockito.mock(LbPools.class);
        LBPool existingPool = Mockito.mock(LBPool.class);

        Mockito.when(nsxService.apply(LbPools.class)).thenReturn(lbPools);
        Mockito.when(lbPools.get(lbServerPoolName)).thenReturn(existingPool);
        Mockito.when(existingPool.getMembers()).thenReturn(null);

        client.createNsxLbServerPool(List.of(), TIER_1_GATEWAY_NAME, lbServerPoolName, "roundrobin", "80", "TCP");

        verify(nsxService, never()).apply(LbMonitorProfiles.class);
        verify(lbPools, never()).patch(anyString(), any(LBPool.class));
    }

    @Test
    public void testCreateNsxLbServerPoolPoolExistsWithDuplicateMembersSkipsPatch() {
        long lbId = 1L;
        String lbServerPoolName = NsxControllerUtils.getServerPoolName(TIER_1_GATEWAY_NAME, lbId);
        List<NsxLoadBalancerMember> memberList = List.of(
                new NsxLoadBalancerMember(1L, "10.0.0.1", 80),
                new NsxLoadBalancerMember(2L, "10.0.0.2", 80)
        );

        LbPools lbPools = Mockito.mock(LbPools.class);
        LBPool existingPool = Mockito.mock(LBPool.class);

        Mockito.when(nsxService.apply(LbPools.class)).thenReturn(lbPools);
        Mockito.when(lbPools.get(lbServerPoolName)).thenReturn(existingPool);
        Mockito.when(existingPool.getMembers()).thenReturn(List.of(
                createPoolMember(1L, "10.0.0.1", 80),
                createPoolMember(1L, "10.0.0.1", 80),
                createPoolMember(2L, "10.0.0.2", 80)
        ));

        client.createNsxLbServerPool(memberList, TIER_1_GATEWAY_NAME, lbServerPoolName, "roundrobin", "80", "TCP");

        verify(nsxService, never()).apply(LbMonitorProfiles.class);
        verify(lbPools, never()).patch(anyString(), any(LBPool.class));
    }

    @Test
    public void testCreateNsxLbServerPoolPoolExistsWithDifferentMembersPerformsPatch() {
        long lbId = 1L;
        String lbServerPoolName = NsxControllerUtils.getServerPoolName(TIER_1_GATEWAY_NAME, lbId);
        List<NsxLoadBalancerMember> memberList = List.of(
                new NsxLoadBalancerMember(1L, "10.0.0.1", 80),
                new NsxLoadBalancerMember(2L, "10.0.0.2", 80)
        );

        LbPools lbPools = Mockito.mock(LbPools.class);
        LBPool existingPool = Mockito.mock(LBPool.class);

        mockLbMonitorProfiles();
        Mockito.when(nsxService.apply(LbPools.class)).thenReturn(lbPools);
        Mockito.when(lbPools.get(lbServerPoolName)).thenReturn(existingPool);
        Mockito.when(existingPool.getMembers()).thenReturn(List.of(
                createPoolMember(1L, "10.0.0.10", 80)
        ));

        client.createNsxLbServerPool(memberList, TIER_1_GATEWAY_NAME, lbServerPoolName, "roundrobin", "80", "TCP");

        verify(lbPools).patch(eq(lbServerPoolName), any(LBPool.class));
    }

    @Test
    public void testCreateNsxLbServerPoolPoolDoesNotExistPerformsPatch() {
        String lbServerPoolName = NsxControllerUtils.getServerPoolName(TIER_1_GATEWAY_NAME, 1L);
        List<NsxLoadBalancerMember> memberList = List.of(new NsxLoadBalancerMember(1L, "10.0.0.1", 80));

        LbPools lbPools = Mockito.mock(LbPools.class);

        mockLbMonitorProfiles();
        Mockito.when(nsxService.apply(LbPools.class)).thenReturn(lbPools);
        Mockito.when(lbPools.get(lbServerPoolName)).thenThrow(new NotFound(null, null));

        client.createNsxLbServerPool(memberList, TIER_1_GATEWAY_NAME, lbServerPoolName, "roundrobin", "80", "TCP");

        verify(lbPools).patch(eq(lbServerPoolName), any(LBPool.class));
    }

    @Test
    public void testCreateAndAddNsxLbVirtualServerVirtualServerAlreadyExistsSkipsPatch() {
        long lbId = 1L;
        String lbVirtualServerName = NsxControllerUtils.getVirtualServerName(TIER_1_GATEWAY_NAME, lbId);
        String lbServiceName = NsxControllerUtils.getLoadBalancerName(TIER_1_GATEWAY_NAME);
        List<NsxLoadBalancerMember> memberList = List.of(new NsxLoadBalancerMember(1L, "10.0.0.1", 80));

        LbPools lbPools = Mockito.mock(LbPools.class);
        LbServices lbServices = Mockito.mock(LbServices.class);
        LbVirtualServers lbVirtualServers = Mockito.mock(LbVirtualServers.class);
        LBVirtualServer existingVs = Mockito.mock(LBVirtualServer.class);

        mockLbMonitorProfiles();
        Mockito.when(nsxService.apply(LbPools.class)).thenReturn(lbPools);
        Mockito.when(nsxService.apply(LbServices.class)).thenReturn(lbServices);
        Mockito.when(nsxService.apply(LbVirtualServers.class)).thenReturn(lbVirtualServers);
        Mockito.when(lbPools.get(anyString())).thenThrow(new NotFound(null, null));
        Mockito.when(lbServices.get(anyString())).thenReturn(null);
        Mockito.when(lbVirtualServers.get(lbVirtualServerName)).thenReturn(existingVs);

        client.createAndAddNsxLbVirtualServer(TIER_1_GATEWAY_NAME, lbId, "192.168.1.1", "443",
                memberList, "roundrobin", "TCP", "80");

        verify(lbVirtualServers).get(lbVirtualServerName);
        verify(lbVirtualServers, never()).get(lbServiceName);
        verify(lbVirtualServers, never()).patch(anyString(), any(LBVirtualServer.class));
    }

    @Test
    public void testCreateAndAddNsxLbVirtualServerVirtualServerNotFoundPerformsPatch() {
        long lbId = 1L;
        String lbServerPoolName = NsxControllerUtils.getServerPoolName(TIER_1_GATEWAY_NAME, lbId);
        String lbVirtualServerName = NsxControllerUtils.getVirtualServerName(TIER_1_GATEWAY_NAME, lbId);
        String lbServiceName = NsxControllerUtils.getLoadBalancerName(TIER_1_GATEWAY_NAME);
        List<NsxLoadBalancerMember> memberList = List.of(new NsxLoadBalancerMember(1L, "10.0.0.1", 80));

        LbPools lbPools = Mockito.mock(LbPools.class);
        LBPool lbPool = Mockito.mock(LBPool.class);
        LbServices lbServices = Mockito.mock(LbServices.class);
        LBService lbService = Mockito.mock(LBService.class);
        LbVirtualServers lbVirtualServers = Mockito.mock(LbVirtualServers.class);

        mockLbMonitorProfiles();
        mockLbAppProfiles();
        Mockito.when(nsxService.apply(LbPools.class)).thenReturn(lbPools);
        Mockito.when(nsxService.apply(LbServices.class)).thenReturn(lbServices);
        Mockito.when(nsxService.apply(LbVirtualServers.class)).thenReturn(lbVirtualServers);
        Mockito.when(lbPools.get(lbServerPoolName)).thenThrow(new NotFound(null, null)).thenReturn(lbPool);
        Mockito.when(lbPool.getPath()).thenReturn("/infra/lb-pools/" + lbServerPoolName);
        Mockito.when(lbServices.get(lbServiceName)).thenReturn(lbService);
        Mockito.when(lbService.getPath()).thenReturn("/infra/lb-services/" + lbServiceName);
        Mockito.when(lbVirtualServers.get(lbVirtualServerName)).thenThrow(new NotFound(null, null));

        client.createAndAddNsxLbVirtualServer(TIER_1_GATEWAY_NAME, lbId, "192.168.1.1", "443",
                memberList, "roundrobin", "TCP", "80");

        verify(lbVirtualServers).get(lbVirtualServerName);
        verify(lbVirtualServers, never()).get(lbServiceName);
        verify(lbVirtualServers).patch(eq(lbVirtualServerName), any(LBVirtualServer.class));
    }

    @Test
    public void testCreateNsxLbServerPoolThrowsExceptionOnPatchError() {
        String lbServerPoolName = NsxControllerUtils.getServerPoolName(TIER_1_GATEWAY_NAME, 1L);
        List<NsxLoadBalancerMember> memberList = List.of(new NsxLoadBalancerMember(1L, "10.0.0.1", 80));

        LbPools lbPools = Mockito.mock(LbPools.class);
        Structure errorData = Mockito.mock(Structure.class);
        ApiError apiError = new ApiError();
        apiError.setErrorData(errorData);

        mockLbMonitorProfiles();
        Mockito.when(nsxService.apply(LbPools.class)).thenReturn(lbPools);
        Mockito.when(lbPools.get(lbServerPoolName)).thenThrow(new NotFound(null, null));
        when(errorData._convertTo(ApiError.class)).thenReturn(apiError);
        doThrow(new Error(List.of(), errorData)).when(lbPools).patch(eq(lbServerPoolName), any(LBPool.class));

        CloudRuntimeException thrownException = assertThrows(CloudRuntimeException.class, () -> {
            client.createNsxLbServerPool(memberList, TIER_1_GATEWAY_NAME, lbServerPoolName, "roundrobin", "80", "TCP");
        });
        assertTrue(thrownException.getMessage().startsWith("Failed to create NSX LB server pool, due to"));
    }

    @Test
    public void testVpnNatOrderingRestoresOriginalSourceNatSequenceAndTags() {
        NatRules natRules = Mockito.mock(NatRules.class);
        PolicyNatRule sourceNatRule = new PolicyNatRule.Builder()
                .setId("t1-NAT")
                .setDisplayName("CloudStack source NAT")
                .setAction("SNAT")
                .setTranslatedNetwork("203.0.113.10")
                .setSourceNetwork("0.0.0.0/0")
                .setDestinationNetwork("ANY")
                .setSequenceNumber(42L)
                .setTags(List.of(new Tag.Builder().setScope("owner").setTag("cloudstack").build()))
                .setEnabled(true)
                .build();
        Mockito.when(nsxService.apply(NatRules.class)).thenReturn(natRules);
        Mockito.when(natRules.get(TIER_1_GATEWAY_NAME, "USER", "t1-NAT")).thenReturn(sourceNatRule);

        client.ensureVpnNatExemptions(TIER_1_GATEWAY_NAME, "203.0.113.20");

        ArgumentCaptor<PolicyNatRule> demotedCaptor = ArgumentCaptor.forClass(PolicyNatRule.class);
        verify(natRules).patch(eq(TIER_1_GATEWAY_NAME), eq("USER"), eq("t1-NAT"), demotedCaptor.capture());
        PolicyNatRule demotedRule = demotedCaptor.getValue();
        assertEquals(Long.valueOf(1000L), demotedRule.getSequenceNumber());
        assertEquals("203.0.113.10", demotedRule.getTranslatedNetwork());
        assertTrue(demotedRule.getTags().stream().anyMatch(tag -> "owner".equals(tag.getScope())
                && "cloudstack".equals(tag.getTag())));
        assertTrue(demotedRule.getTags().stream().anyMatch(tag -> "cloudstack-vpn-original-snat-sequence".equals(tag.getScope())
                && "42".equals(tag.getTag())));

        clearInvocations(natRules);
        Mockito.when(natRules.get(TIER_1_GATEWAY_NAME, "USER", "t1-NAT")).thenReturn(demotedRule);
        client.restoreSourceNatRuleSequence(TIER_1_GATEWAY_NAME);

        ArgumentCaptor<PolicyNatRule> restoredCaptor = ArgumentCaptor.forClass(PolicyNatRule.class);
        verify(natRules).patch(eq(TIER_1_GATEWAY_NAME), eq("USER"), eq("t1-NAT"), restoredCaptor.capture());
        PolicyNatRule restoredRule = restoredCaptor.getValue();
        assertEquals(Long.valueOf(42L), restoredRule.getSequenceNumber());
        assertEquals("203.0.113.10", restoredRule.getTranslatedNetwork());
        assertEquals(1, restoredRule.getTags().size());
        assertEquals("owner", restoredRule.getTags().get(0).getScope());
        assertEquals("cloudstack", restoredRule.getTags().get(0).getTag());
    }

    @Test
    public void testDeleteTier1GatewayRemovesVpnResourcesBeforeLocaleServices() {
        Tier1s tier1s = Mockito.mock(Tier1s.class);
        StaticRoutes staticRoutes = Mockito.mock(StaticRoutes.class);
        NatRules natRules = Mockito.mock(NatRules.class);
        IpsecVpnServices vpnServices = Mockito.mock(IpsecVpnServices.class);
        LocaleServices localeServices = Mockito.mock(LocaleServices.class);
        StaticRoutesListResult staticRoutesResult = Mockito.mock(StaticRoutesListResult.class);
        PolicyNatRuleListResult natRulesResult = Mockito.mock(PolicyNatRuleListResult.class);
        IPSecVpnServiceListResult vpnServicesResult = Mockito.mock(IPSecVpnServiceListResult.class);

        when(nsxService.apply(Tier1s.class)).thenReturn(tier1s);
        when(nsxService.apply(StaticRoutes.class)).thenReturn(staticRoutes);
        when(nsxService.apply(NatRules.class)).thenReturn(natRules);
        when(nsxService.apply(IpsecVpnServices.class)).thenReturn(vpnServices);
        when(nsxService.apply(LocaleServices.class)).thenReturn(localeServices);
        when(tier1s.get(TIER_1_GATEWAY_NAME)).thenReturn(Mockito.mock(Tier1.class));
        when(staticRoutes.list(eq(TIER_1_GATEWAY_NAME), nullable(String.class), eq(false),
                nullable(String.class), nullable(Long.class), nullable(Boolean.class), nullable(String.class)))
                .thenReturn(staticRoutesResult);
        when(staticRoutesResult.getResults()).thenReturn(List.of());
        when(natRules.list(eq(TIER_1_GATEWAY_NAME), anyString(), nullable(String.class), eq(false),
                nullable(String.class), nullable(Long.class), nullable(Boolean.class), nullable(String.class)))
                .thenReturn(natRulesResult);
        when(natRulesResult.getResults()).thenReturn(List.of());
        when(vpnServices.list(eq(TIER_1_GATEWAY_NAME), nullable(String.class), eq(false),
                nullable(String.class), nullable(Long.class), eq(false), nullable(String.class)))
                .thenReturn(vpnServicesResult);
        when(vpnServicesResult.getResults()).thenReturn(List.of());

        client.deleteTier1Gateway(TIER_1_GATEWAY_NAME);

        InOrder inOrder = Mockito.inOrder(staticRoutes, natRules, vpnServices, localeServices, tier1s);
        inOrder.verify(staticRoutes).list(eq(TIER_1_GATEWAY_NAME), nullable(String.class), eq(false),
                nullable(String.class), nullable(Long.class), nullable(Boolean.class), nullable(String.class));
        inOrder.verify(natRules).list(eq(TIER_1_GATEWAY_NAME), anyString(), nullable(String.class), eq(false),
                nullable(String.class), nullable(Long.class), nullable(Boolean.class), nullable(String.class));
        inOrder.verify(natRules).delete(eq(TIER_1_GATEWAY_NAME), anyString(), anyString());
        inOrder.verify(vpnServices).list(eq(TIER_1_GATEWAY_NAME), nullable(String.class), eq(false),
                nullable(String.class), nullable(Long.class), eq(false), nullable(String.class));
        inOrder.verify(natRules).list(eq(TIER_1_GATEWAY_NAME), anyString(), nullable(String.class), eq(false),
                nullable(String.class), nullable(Long.class), nullable(Boolean.class), nullable(String.class));
        inOrder.verify(localeServices).delete(TIER_1_GATEWAY_NAME, "default");
        inOrder.verify(tier1s).delete(TIER_1_GATEWAY_NAME);
    }

    @Test
    public void testCreateRouteBasedVpnSessionRemovesSessionWhenPatchFails() {
        IpsecVpnIkeProfiles ikeProfiles = Mockito.mock(IpsecVpnIkeProfiles.class);
        IpsecVpnTunnelProfiles tunnelProfiles = Mockito.mock(IpsecVpnTunnelProfiles.class);
        IpsecVpnDpdProfiles dpdProfiles = Mockito.mock(IpsecVpnDpdProfiles.class);
        Sessions sessions = Mockito.mock(Sessions.class);
        Structure errorData = Mockito.mock(Structure.class);
        ApiError apiError = new ApiError();
        apiError.setErrorData(errorData);

        Mockito.when(nsxService.apply(IpsecVpnIkeProfiles.class)).thenReturn(ikeProfiles);
        Mockito.when(nsxService.apply(IpsecVpnTunnelProfiles.class)).thenReturn(tunnelProfiles);
        Mockito.when(nsxService.apply(IpsecVpnDpdProfiles.class)).thenReturn(dpdProfiles);
        Mockito.when(nsxService.apply(Sessions.class)).thenReturn(sessions);
        Mockito.when(errorData._convertTo(ApiError.class)).thenReturn(apiError);
        doThrow(new Error(List.of(), errorData)).when(sessions).patch(anyString(), anyString(), anyString(), any(RouteBasedIPSecVpnSession.class));

        assertThrows(CloudRuntimeException.class, () -> client.createRouteBasedVpnSession(
                TIER_1_GATEWAY_NAME, "connection-uuid", "203.0.113.10", "psk",
                "aes256-sha256;modp2048", "aes256-sha256;modp2048", 86400L, 3600L,
                true, "ikev2", false, "169.254.64.21", 30));

        verify(sessions).delete(eq(TIER_1_GATEWAY_NAME), eq("t1-vpn"), eq("cs-conn-connection-uuid"));
        verify(ikeProfiles).delete("cs-conn-connection-uuid-ike");
        verify(tunnelProfiles).delete("cs-conn-connection-uuid-esp");
        verify(dpdProfiles).delete("cs-conn-connection-uuid-dpd");
    }

    @Test
    public void testCreateRouteBasedVpnSessionDoesNotDeleteExistingSessionWhenPatchFails() {
        IpsecVpnIkeProfiles ikeProfiles = Mockito.mock(IpsecVpnIkeProfiles.class);
        IpsecVpnTunnelProfiles tunnelProfiles = Mockito.mock(IpsecVpnTunnelProfiles.class);
        IpsecVpnDpdProfiles dpdProfiles = Mockito.mock(IpsecVpnDpdProfiles.class);
        Sessions sessions = Mockito.mock(Sessions.class);
        Structure existingSession = Mockito.mock(Structure.class);
        Structure errorData = Mockito.mock(Structure.class);
        ApiError apiError = new ApiError();
        apiError.setErrorData(errorData);

        Mockito.when(nsxService.apply(IpsecVpnIkeProfiles.class)).thenReturn(ikeProfiles);
        Mockito.when(nsxService.apply(IpsecVpnTunnelProfiles.class)).thenReturn(tunnelProfiles);
        Mockito.when(nsxService.apply(IpsecVpnDpdProfiles.class)).thenReturn(dpdProfiles);
        Mockito.when(nsxService.apply(Sessions.class)).thenReturn(sessions);
        Mockito.when(sessions.get(TIER_1_GATEWAY_NAME, "t1-vpn", "cs-conn-connection-uuid"))
                .thenReturn(existingSession);
        Mockito.when(errorData._convertTo(ApiError.class)).thenReturn(apiError);
        doThrow(new Error(List.of(), errorData)).when(sessions).patch(anyString(), anyString(), anyString(), any(RouteBasedIPSecVpnSession.class));

        assertThrows(CloudRuntimeException.class, () -> client.createRouteBasedVpnSession(
                TIER_1_GATEWAY_NAME, "connection-uuid", "203.0.113.10", "psk",
                "aes256-sha256;modp2048", "aes256-sha256;modp2048", 86400L, 3600L,
                true, "ikev2", false, "169.254.64.21", 30));

        verify(sessions, never()).delete(anyString(), anyString(), anyString());
        verify(ikeProfiles, never()).delete(anyString());
        verify(tunnelProfiles, never()).delete(anyString());
        verify(dpdProfiles, never()).delete(anyString());
    }

    @Test
    public void testCreateRouteBasedVpnSessionCleansProfilesWhenProfilePatchFails() {
        IpsecVpnIkeProfiles ikeProfiles = Mockito.mock(IpsecVpnIkeProfiles.class);
        IpsecVpnTunnelProfiles tunnelProfiles = Mockito.mock(IpsecVpnTunnelProfiles.class);
        IpsecVpnDpdProfiles dpdProfiles = Mockito.mock(IpsecVpnDpdProfiles.class);
        Sessions sessions = Mockito.mock(Sessions.class);
        Structure errorData = Mockito.mock(Structure.class);
        ApiError apiError = new ApiError();
        apiError.setErrorData(errorData);

        Mockito.when(nsxService.apply(IpsecVpnIkeProfiles.class)).thenReturn(ikeProfiles);
        Mockito.when(nsxService.apply(IpsecVpnTunnelProfiles.class)).thenReturn(tunnelProfiles);
        Mockito.when(nsxService.apply(IpsecVpnDpdProfiles.class)).thenReturn(dpdProfiles);
        Mockito.when(nsxService.apply(Sessions.class)).thenReturn(sessions);
        Mockito.when(errorData._convertTo(ApiError.class)).thenReturn(apiError);
        doThrow(new Error(List.of(), errorData)).when(tunnelProfiles)
                .patch(anyString(), any(IPSecVpnTunnelProfile.class));

        assertThrows(CloudRuntimeException.class, () -> client.createRouteBasedVpnSession(
                TIER_1_GATEWAY_NAME, "connection-uuid", "203.0.113.10", "psk",
                "aes256-sha256;modp2048", "aes256-sha256;modp2048", 86400L, 3600L,
                true, "ikev2", false, "169.254.64.21", 30));

        verify(sessions).delete(TIER_1_GATEWAY_NAME, "t1-vpn", "cs-conn-connection-uuid");
        verify(ikeProfiles).delete("cs-conn-connection-uuid-ike");
        verify(tunnelProfiles).delete("cs-conn-connection-uuid-esp");
        verify(dpdProfiles).delete("cs-conn-connection-uuid-dpd");
    }

    @Test
    public void testCreateRouteBasedVpnSessionPreservesPreExistingProfilesAfterFailure() {
        IpsecVpnIkeProfiles ikeProfiles = Mockito.mock(IpsecVpnIkeProfiles.class);
        IpsecVpnTunnelProfiles tunnelProfiles = Mockito.mock(IpsecVpnTunnelProfiles.class);
        IpsecVpnDpdProfiles dpdProfiles = Mockito.mock(IpsecVpnDpdProfiles.class);
        Sessions sessions = Mockito.mock(Sessions.class);
        Structure errorData = Mockito.mock(Structure.class);
        ApiError apiError = new ApiError();
        apiError.setErrorData(errorData);

        Mockito.when(nsxService.apply(IpsecVpnIkeProfiles.class)).thenReturn(ikeProfiles);
        Mockito.when(nsxService.apply(IpsecVpnTunnelProfiles.class)).thenReturn(tunnelProfiles);
        Mockito.when(nsxService.apply(IpsecVpnDpdProfiles.class)).thenReturn(dpdProfiles);
        Mockito.when(nsxService.apply(Sessions.class)).thenReturn(sessions);
        Mockito.when(ikeProfiles.get("cs-conn-connection-uuid-ike")).thenReturn(Mockito.mock(IPSecVpnIkeProfile.class));
        Mockito.when(tunnelProfiles.get("cs-conn-connection-uuid-esp")).thenReturn(Mockito.mock(IPSecVpnTunnelProfile.class));
        Mockito.when(dpdProfiles.get("cs-conn-connection-uuid-dpd")).thenReturn(Mockito.mock(IPSecVpnDpdProfile.class));
        Mockito.when(errorData._convertTo(ApiError.class)).thenReturn(apiError);
        doThrow(new Error(List.of(), errorData)).when(sessions)
                .patch(anyString(), anyString(), anyString(), any(RouteBasedIPSecVpnSession.class));

        assertThrows(CloudRuntimeException.class, () -> client.createRouteBasedVpnSession(
                TIER_1_GATEWAY_NAME, "connection-uuid", "203.0.113.10", "psk",
                "aes256-sha256;modp2048", "aes256-sha256;modp2048", 86400L, 3600L,
                true, "ikev2", false, "169.254.64.21", 30));

        verify(ikeProfiles, never()).delete(anyString());
        verify(tunnelProfiles, never()).delete(anyString());
        verify(dpdProfiles, never()).delete(anyString());
    }

    @Test
    public void testDeleteVpnConnectionContinuesCleanupAfterRouteAndNatFailures() {
        IpsecVpnIkeProfiles ikeProfiles = Mockito.mock(IpsecVpnIkeProfiles.class);
        IpsecVpnTunnelProfiles tunnelProfiles = Mockito.mock(IpsecVpnTunnelProfiles.class);
        IpsecVpnDpdProfiles dpdProfiles = Mockito.mock(IpsecVpnDpdProfiles.class);
        Sessions sessions = Mockito.mock(Sessions.class);
        Mockito.when(nsxService.apply(com.vmware.nsx_policy.infra.tier_1s.StaticRoutes.class))
                .thenThrow(new CloudRuntimeException("route cleanup failed"));
        Mockito.when(nsxService.apply(NatRules.class)).thenThrow(new CloudRuntimeException("NAT cleanup failed"));
        Mockito.when(nsxService.apply(Sessions.class)).thenReturn(sessions);
        Mockito.when(nsxService.apply(IpsecVpnIkeProfiles.class)).thenReturn(ikeProfiles);
        Mockito.when(nsxService.apply(IpsecVpnTunnelProfiles.class)).thenReturn(tunnelProfiles);
        Mockito.when(nsxService.apply(IpsecVpnDpdProfiles.class)).thenReturn(dpdProfiles);

        assertThrows(CloudRuntimeException.class,
                () -> client.deleteVpnConnection(TIER_1_GATEWAY_NAME, "connection-uuid"));

        verify(sessions).delete(TIER_1_GATEWAY_NAME, "t1-vpn", "cs-conn-connection-uuid");
        verify(ikeProfiles).delete("cs-conn-connection-uuid-ike");
        verify(tunnelProfiles).delete("cs-conn-connection-uuid-esp");
        verify(dpdProfiles).delete("cs-conn-connection-uuid-dpd");
    }

    private LbMonitorProfiles mockLbMonitorProfiles() {
        LbMonitorProfiles lbMonitorProfiles = Mockito.mock(LbMonitorProfiles.class);
        Structure monitorStructure = Mockito.mock(Structure.class, Mockito.RETURNS_DEEP_STUBS);

        Mockito.when(nsxService.apply(LbMonitorProfiles.class)).thenReturn(lbMonitorProfiles);
        Mockito.when(lbMonitorProfiles.get(anyString())).thenReturn(monitorStructure);
        Mockito.when(monitorStructure._getDataValue().getField("path").toString()).thenReturn("/infra/lb-monitor-profiles/test");
        return lbMonitorProfiles;
    }

    private void mockLbAppProfiles() {
        LbAppProfiles lbAppProfiles = Mockito.mock(LbAppProfiles.class);
        LBAppProfileListResult appProfileListResult = Mockito.mock(LBAppProfileListResult.class);
        Structure appProfile = Mockito.mock(Structure.class, Mockito.RETURNS_DEEP_STUBS);

        Mockito.when(nsxService.apply(LbAppProfiles.class)).thenReturn(lbAppProfiles);
        Mockito.when(lbAppProfiles.list(null, null, null, null, null, null)).thenReturn(appProfileListResult);
        Mockito.when(appProfileListResult.getResults()).thenReturn(List.of(appProfile));
        Mockito.when(appProfile._getDataValue().getField("path").toString()).thenReturn("/infra/lb-app-profiles/default-tcp-profile");
    }

    private LBPoolMember createPoolMember(long vmId, String ipAddress, int port) {
        return new LBPoolMember.Builder()
                .setDisplayName(NsxControllerUtils.getServerPoolMemberName(TIER_1_GATEWAY_NAME, vmId))
                .setIpAddress(ipAddress)
                .setPort(String.valueOf(port))
                .build();
    }
}
