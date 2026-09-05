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

public class DeleteNetrisVnetCommandTest {

    private static final long ZONE_ID = 1L;
    private static final Long ACCOUNT_ID = 2L;
    private static final Long DOMAIN_ID = 3L;
    private static final String NAME = "test-vnet";
    private static final Long ID = 4L;
    private static final String VPC_NAME = "test-vpc";
    private static final Long VPC_ID = 5L;
    private static final String VNET_CIDR = "10.0.0.0/24";
    private static final boolean IS_VPC = true;
    private static final String VNET_V6_CIDR = "2001:db8::/32";

    @Test
    public void testConstructorAndGetters() {
        // Act
        DeleteNetrisVnetCommand command = new DeleteNetrisVnetCommand(
                ZONE_ID, ACCOUNT_ID, DOMAIN_ID, NAME, ID, VPC_NAME, VPC_ID, VNET_CIDR, IS_VPC);

        // Assert
        Assert.assertEquals(ZONE_ID, command.getZoneId());
        Assert.assertEquals(ACCOUNT_ID, command.getAccountId());
        Assert.assertEquals(DOMAIN_ID, command.getDomainId());
        Assert.assertEquals(NAME, command.getName());
        Assert.assertEquals(ID, command.getId());
        Assert.assertEquals(VPC_NAME, command.getVpcName());
        Assert.assertEquals(VPC_ID, command.getVpcId());
        Assert.assertEquals(VNET_CIDR, command.getVNetCidr());
        Assert.assertEquals(IS_VPC, command.isVpc());
    }

    @Test
    public void testConstructorWithEmptyStrings() {
        // Act
        DeleteNetrisVnetCommand command = new DeleteNetrisVnetCommand(
                ZONE_ID, ACCOUNT_ID, DOMAIN_ID, "", ID, "", VPC_ID, VNET_CIDR, IS_VPC);

        // Assert
        Assert.assertEquals(ZONE_ID, command.getZoneId());
        Assert.assertEquals(ACCOUNT_ID, command.getAccountId());
        Assert.assertEquals(DOMAIN_ID, command.getDomainId());
        Assert.assertEquals("", command.getName());
        Assert.assertEquals(ID, command.getId());
        Assert.assertEquals("", command.getVpcName());
        Assert.assertEquals(VPC_ID, command.getVpcId());
        Assert.assertEquals(VNET_CIDR, command.getVNetCidr());
        Assert.assertEquals(IS_VPC, command.isVpc());
    }

    @Test
    public void testVnetV6CidrSetterAndGetter() {
        // Arrange
        DeleteNetrisVnetCommand command = new DeleteNetrisVnetCommand(
                ZONE_ID, ACCOUNT_ID, DOMAIN_ID, NAME, ID, VPC_NAME, VPC_ID, VNET_CIDR, IS_VPC);

        // Act
        command.setvNetV6Cidr(VNET_V6_CIDR);

        // Assert
        Assert.assertEquals(VNET_V6_CIDR, command.getvNetV6Cidr());
    }
}
