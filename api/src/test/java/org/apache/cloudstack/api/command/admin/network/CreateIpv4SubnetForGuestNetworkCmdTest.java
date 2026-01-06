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

import com.cloud.event.EventTypes;

import org.apache.cloudstack.api.response.Ipv4SubnetForGuestNetworkResponse;
import org.apache.cloudstack.network.Ipv4GuestSubnetNetworkMap;
import org.apache.cloudstack.network.RoutedIpv4Manager;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class CreateIpv4SubnetForGuestNetworkCmdTest {

    RoutedIpv4Manager routedIpv4Manager = Mockito.spy(RoutedIpv4Manager.class);

    @Test
    public void testCreateIpv4SubnetForGuestNetworkCmd() {
        Long parentId = 1L;
        String subnet = "192.168.1.0/24";
        Integer cidrSize = 26;

        CreateIpv4SubnetForGuestNetworkCmd cmd = new CreateIpv4SubnetForGuestNetworkCmd();
        ReflectionTestUtils.setField(cmd, "parentId", parentId);
        ReflectionTestUtils.setField(cmd, "subnet", subnet);
        ReflectionTestUtils.setField(cmd, "cidrSize", cidrSize);
        ReflectionTestUtils.setField(cmd,"routedIpv4Manager", routedIpv4Manager);

        Assert.assertEquals(parentId, cmd.getParentId());
        Assert.assertEquals(subnet, cmd.getSubnet());
        Assert.assertEquals(cidrSize, cmd.getCidrSize());
        Assert.assertEquals(1L, cmd.getEntityOwnerId());
        Assert.assertEquals(EventTypes.EVENT_IP4_GUEST_SUBNET_CREATE, cmd.getEventType());
        Assert.assertEquals(String.format("Creating guest IPv4 subnet %s in zone subnet=%s", subnet, parentId), cmd.getEventDescription());

        Ipv4GuestSubnetNetworkMap ipv4GuestSubnetNetworkMap = Mockito.mock(Ipv4GuestSubnetNetworkMap.class);
        Mockito.when(routedIpv4Manager.createIpv4SubnetForGuestNetwork(cmd)).thenReturn(ipv4GuestSubnetNetworkMap);

        Ipv4SubnetForGuestNetworkResponse response = Mockito.mock(Ipv4SubnetForGuestNetworkResponse.class);
        Mockito.when(routedIpv4Manager.createIpv4SubnetForGuestNetworkResponse(ipv4GuestSubnetNetworkMap)).thenReturn(response);

        try {
            cmd.execute();
        } catch (Exception ignored) {
        }

        Assert.assertEquals(response, cmd.getResponseObject());
    }
}
