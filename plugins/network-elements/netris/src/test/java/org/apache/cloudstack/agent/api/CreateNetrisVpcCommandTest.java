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

public class CreateNetrisVpcCommandTest {

    private static final long ZONE_ID = 1L;
    private static final Long ACCOUNT_ID = 2L;
    private static final Long DOMAIN_ID = 3L;
    private static final String NAME = "test-vpc";
    private static final String CIDR = "10.0.0.0/24";
    private static final Long VPC_ID = 4L;
    private static final boolean IS_VPC = true;

    @Test
    public void testConstructorAndGetters() {
        // Act
        CreateNetrisVpcCommand command = new CreateNetrisVpcCommand(
                ZONE_ID, ACCOUNT_ID, DOMAIN_ID, NAME, CIDR, VPC_ID, IS_VPC);

        // Assert
        Assert.assertEquals(ZONE_ID, command.getZoneId());
        Assert.assertEquals(ACCOUNT_ID, command.getAccountId());
        Assert.assertEquals(DOMAIN_ID, command.getDomainId());
        Assert.assertEquals(NAME, command.getName());
        Assert.assertEquals(VPC_ID, command.getId());
        Assert.assertEquals(IS_VPC, command.isVpc());
        Assert.assertEquals(CIDR, command.getCidr());
    }

    @Test
    public void testConstructorWithEmptyStrings() {
        // Act
        CreateNetrisVpcCommand command = new CreateNetrisVpcCommand(
                ZONE_ID, ACCOUNT_ID, DOMAIN_ID, "", "", VPC_ID, IS_VPC);

        // Assert
        Assert.assertEquals(ZONE_ID, command.getZoneId());
        Assert.assertEquals(ACCOUNT_ID, command.getAccountId());
        Assert.assertEquals(DOMAIN_ID, command.getDomainId());
        Assert.assertEquals("", command.getName());
        Assert.assertEquals(VPC_ID, command.getId());
        Assert.assertEquals(IS_VPC, command.isVpc());
        Assert.assertEquals("", command.getCidr());
    }
}
