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
import org.apache.cloudstack.api.response.DataCenterIpv4SubnetResponse;
import org.apache.cloudstack.datacenter.DataCenterIpv4GuestSubnet;
import org.apache.cloudstack.network.RoutedIpv4Manager;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class DedicateIpv4SubnetForZoneCmdTest {

    RoutedIpv4Manager routedIpv4Manager = Mockito.spy(RoutedIpv4Manager.class);

    @Test
    public void testDedicateIpv4SubnetForZoneCmd() {
        Long id = 1L;
        String accountName = "user";
        Long projectId = 10L;
        Long domainId = 11L;

        DedicateIpv4SubnetForZoneCmd cmd = new DedicateIpv4SubnetForZoneCmd();
        ReflectionTestUtils.setField(cmd, "id", id);
        ReflectionTestUtils.setField(cmd, "accountName", accountName);
        ReflectionTestUtils.setField(cmd,"projectId", projectId);
        ReflectionTestUtils.setField(cmd,"domainId", domainId);
        ReflectionTestUtils.setField(cmd,"routedIpv4Manager", routedIpv4Manager);

        Assert.assertEquals(id, cmd.getId());
        Assert.assertEquals(accountName, cmd.getAccountName());
        Assert.assertEquals(projectId, cmd.getProjectId());
        Assert.assertEquals(domainId, cmd.getDomainId());

        Assert.assertEquals(1L, cmd.getEntityOwnerId());
        Assert.assertEquals(EventTypes.EVENT_ZONE_IP4_SUBNET_DEDICATE, cmd.getEventType());
        Assert.assertEquals(String.format("Dedicating zone IPv4 subnet %s", id), cmd.getEventDescription());

        DataCenterIpv4GuestSubnet zoneSubnet = Mockito.mock(DataCenterIpv4GuestSubnet.class);
        Mockito.when(routedIpv4Manager.dedicateDataCenterIpv4GuestSubnet(cmd)).thenReturn(zoneSubnet);

        DataCenterIpv4SubnetResponse response = Mockito.mock(DataCenterIpv4SubnetResponse.class);
        Mockito.when(routedIpv4Manager.createDataCenterIpv4SubnetResponse(zoneSubnet)).thenReturn(response);

        try {
            cmd.execute();
        } catch (Exception ignored) {
        }

        Assert.assertEquals(response, cmd.getResponseObject());
    }
}
