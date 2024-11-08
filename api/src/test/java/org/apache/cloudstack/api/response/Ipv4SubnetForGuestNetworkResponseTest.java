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
package org.apache.cloudstack.api.response;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Date;

@RunWith(MockitoJUnitRunner.class)
public final class Ipv4SubnetForGuestNetworkResponseTest {

    private static String uuid = "uuid";
    private static String parentId = "parent-id";
    private static String parentSubnet = "10.10.0.0/20";
    private static String subnet = "10.10.0.0/24";
    private static String state = "Allocating";

    private static String zoneId = "zone-id";
    private static String zoneName = "zone-name";
    private static Date allocated = new Date();
    private static String networkId = "network-id";
    private static String networkName = "network-name";
    private static String vpcId = "vpc-uuid";
    private static String vpcName = "vpc-name";
    private static Date created = new Date();
    private static Date removed = new Date();



    @Test
    public void testIpv4SubnetForGuestNetworkResponse() {
        final Ipv4SubnetForGuestNetworkResponse response = new Ipv4SubnetForGuestNetworkResponse();

        response.setId(uuid);
        response.setSubnet(subnet);
        response.setParentId(parentId);
        response.setParentSubnet(parentSubnet);
        response.setState(state);
        response.setZoneId(zoneId);
        response.setZoneName(zoneName);
        response.setAllocatedTime(allocated);
        response.setNetworkId(networkId);
        response.setNetworkName(networkName);
        response.setVpcId(vpcId);
        response.setVpcName(vpcName);
        response.setCreated(created);
        response.setRemoved(removed);

        Assert.assertEquals(uuid, response.getId());
        Assert.assertEquals(subnet, response.getSubnet());
        Assert.assertEquals(parentId, response.getParentId());
        Assert.assertEquals(parentSubnet, response.getParentSubnet());
        Assert.assertEquals(state, response.getState());
        Assert.assertEquals(zoneId, response.getZoneId());
        Assert.assertEquals(zoneName, response.getZoneName());
        Assert.assertEquals(allocated, response.getAllocatedTime());
        Assert.assertEquals(networkId, response.getNetworkId());
        Assert.assertEquals(networkName, response.getNetworkName());
        Assert.assertEquals(vpcId, response.getVpcId());
        Assert.assertEquals(vpcName, response.getVpcName());
        Assert.assertEquals(created, response.getCreated());
        Assert.assertEquals(removed, response.getRemoved());
    }
}
