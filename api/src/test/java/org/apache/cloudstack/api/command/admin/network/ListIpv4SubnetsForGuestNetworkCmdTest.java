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

package org.apache.cloudstack.api.command.admin.network;

import org.apache.cloudstack.api.response.Ipv4SubnetForGuestNetworkResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.network.Ipv4GuestSubnetNetworkMap;
import org.apache.cloudstack.network.RoutedIpv4Manager;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class ListIpv4SubnetsForGuestNetworkCmdTest {

    RoutedIpv4Manager routedIpv4Manager = Mockito.spy(RoutedIpv4Manager.class);

    @Test
    public void testListIpv4SubnetsForGuestNetworkCmd() {
        Long id = 1L;
        Long zoneId = 2L;
        Long parentId = 2L;
        String subnet = "192.168.1.0/24";
        Long networkId = 10L;
        Long vpcId = 11L;

        ListIpv4SubnetsForGuestNetworkCmd cmd = new ListIpv4SubnetsForGuestNetworkCmd();
        ReflectionTestUtils.setField(cmd, "id", id);
        ReflectionTestUtils.setField(cmd, "zoneId", zoneId);
        ReflectionTestUtils.setField(cmd, "subnet", subnet);
        ReflectionTestUtils.setField(cmd, "parentId", parentId);
        ReflectionTestUtils.setField(cmd,"networkId", networkId);
        ReflectionTestUtils.setField(cmd,"vpcId", vpcId);
        ReflectionTestUtils.setField(cmd,"routedIpv4Manager", routedIpv4Manager);

        Assert.assertEquals(id, cmd.getId());
        Assert.assertEquals(zoneId, cmd.getZoneId());
        Assert.assertEquals(subnet, cmd.getSubnet());
        Assert.assertEquals(networkId, cmd.getNetworkId());
        Assert.assertEquals(vpcId, cmd.getVpcId());
        Assert.assertEquals(parentId, cmd.getParentId());

        Assert.assertEquals(0L, cmd.getEntityOwnerId());

        Ipv4GuestSubnetNetworkMap ipv4GuestSubnetNetworkMap = Mockito.mock(Ipv4GuestSubnetNetworkMap.class);
        List<Ipv4GuestSubnetNetworkMap> ipv4GuestSubnetNetworkMaps = Arrays.asList(ipv4GuestSubnetNetworkMap);
        Mockito.when(routedIpv4Manager.listIpv4GuestSubnetsForGuestNetwork(cmd)).thenReturn(ipv4GuestSubnetNetworkMaps);

        Ipv4SubnetForGuestNetworkResponse response = Mockito.mock(Ipv4SubnetForGuestNetworkResponse.class);
        Mockito.when(routedIpv4Manager.createIpv4SubnetForGuestNetworkResponse(ipv4GuestSubnetNetworkMap)).thenReturn(response);

        try {
            cmd.execute();
        } catch (Exception ignored) {
        }

        Assert.assertTrue(cmd.getResponseObject() instanceof ListResponse);
        ListResponse listResponse = (ListResponse) cmd.getResponseObject();
        Assert.assertEquals(1, (int) listResponse.getCount());
        Assert.assertEquals(response, listResponse.getResponses().get(0));
    }
}
