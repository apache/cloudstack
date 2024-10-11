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
import com.vmware.nsx.cluster.Status;
import com.vmware.nsx.model.ClusterStatus;
import com.vmware.nsx.model.ControllerClusterStatus;
import com.vmware.nsx_policy.infra.domains.Groups;
import com.vmware.nsx_policy.model.Group;
import com.vmware.nsx_policy.model.PathExpression;
import com.vmware.vapi.bindings.Service;
import org.apache.cloudstack.resource.NsxNetworkRule;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.function.Function;

public class NsxApiClientTest {

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
        NsxNetworkRule rule = Mockito.mock(NsxNetworkRule.class);
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
        NsxNetworkRule rule = Mockito.mock(NsxNetworkRule.class);
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
}
