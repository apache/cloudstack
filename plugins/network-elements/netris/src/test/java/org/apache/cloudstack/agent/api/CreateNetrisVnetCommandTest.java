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
package org.apache.cloudstack.agent.api;

import org.junit.Assert;
import org.junit.Test;

public class CreateNetrisVnetCommandTest {

    private static final Long ZONE_ID = 1L;
    private static final Long ACCOUNT_ID = 2L;
    private static final Long DOMAIN_ID = 3L;
    private static final String VPC_NAME = "test-vpc";
    private static final Long VPC_ID = 4L;
    private static final String VNET_NAME = "test-vnet";
    private static final Long NETWORK_ID = 5L;
    private static final String CIDR = "10.0.0.0/24";
    private static final String GATEWAY = "10.0.0.1";
    private static final boolean IS_VPC = true;
    private static final Integer VXLAN_ID = 100;
    private static final String NETRIS_TAG = "test-tag";
    private static final String IPV6_CIDR = "2001:db8::/64";
    private static final Boolean GLOBAL_ROUTING = true;

    @Test
    public void testConstructorAndGetters() {
        // Act
        CreateNetrisVnetCommand command = new CreateNetrisVnetCommand(
                ZONE_ID, ACCOUNT_ID, DOMAIN_ID, VPC_NAME, VPC_ID, VNET_NAME, NETWORK_ID, CIDR, GATEWAY, IS_VPC);

        // Assert
        Assert.assertEquals(ZONE_ID.longValue(), command.getZoneId());
        Assert.assertEquals(ACCOUNT_ID, command.getAccountId());
        Assert.assertEquals(DOMAIN_ID, command.getDomainId());
        Assert.assertEquals(VNET_NAME, command.getName());
        Assert.assertEquals(NETWORK_ID, command.getId());
        Assert.assertEquals(IS_VPC, command.isVpc());
        Assert.assertEquals(VPC_NAME, command.getVpcName());
        Assert.assertEquals(VPC_ID, command.getVpcId());
        Assert.assertEquals(CIDR, command.getCidr());
        Assert.assertEquals(GATEWAY, command.getGateway());
    }

    @Test
    public void testSetters() {
        // Arrange
        CreateNetrisVnetCommand command = new CreateNetrisVnetCommand(
                ZONE_ID, ACCOUNT_ID, DOMAIN_ID, VPC_NAME, VPC_ID, VNET_NAME, NETWORK_ID, CIDR, GATEWAY, IS_VPC);

        // Act
        command.setVxlanId(VXLAN_ID);
        command.setNetrisTag(NETRIS_TAG);
        command.setIpv6Cidr(IPV6_CIDR);
        command.setGlobalRouting(GLOBAL_ROUTING);

        // Assert
        Assert.assertEquals(VXLAN_ID, command.getVxlanId());
        Assert.assertEquals(NETRIS_TAG, command.getNetrisTag());
        Assert.assertEquals(IPV6_CIDR, command.getIpv6Cidr());
        Assert.assertEquals(GLOBAL_ROUTING, command.isGlobalRouting());
    }

    @Test
    public void testConstructorWithEmptyStrings() {
        // Act
        CreateNetrisVnetCommand command = new CreateNetrisVnetCommand(
                ZONE_ID, ACCOUNT_ID, DOMAIN_ID, "", VPC_ID, "", NETWORK_ID, "", "", IS_VPC);

        // Assert
        Assert.assertEquals(ZONE_ID.longValue(), command.getZoneId());
        Assert.assertEquals(ACCOUNT_ID, command.getAccountId());
        Assert.assertEquals(DOMAIN_ID, command.getDomainId());
        Assert.assertEquals("", command.getName());
        Assert.assertEquals(NETWORK_ID, command.getId());
        Assert.assertEquals(IS_VPC, command.isVpc());
        Assert.assertEquals("", command.getVpcName());
        Assert.assertEquals(VPC_ID, command.getVpcId());
        Assert.assertEquals("", command.getCidr());
        Assert.assertEquals("", command.getGateway());
    }
}
