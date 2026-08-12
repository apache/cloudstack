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
import com.vmware.nsx_policy.infra.domains.Groups;
import com.vmware.nsx_policy.model.ApiError;
import com.vmware.nsx_policy.model.Group;
import com.vmware.nsx_policy.model.LBAppProfileListResult;
import com.vmware.nsx_policy.model.LBIcmpMonitorProfile;
import com.vmware.nsx_policy.model.LBService;
import com.vmware.nsx_policy.model.LBTcpMonitorProfile;
import com.vmware.nsx_policy.model.LBPool;
import com.vmware.nsx_policy.model.LBPoolMember;
import com.vmware.nsx_policy.model.LBVirtualServer;
import com.vmware.nsx_policy.model.PathExpression;
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

import java.util.List;
import java.util.function.Function;

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
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
