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

public class DeleteNetrisLoadBalancerRuleCommandTest {

    private static final long ZONE_ID = 1L;
    private static final Long ACCOUNT_ID = 2L;
    private static final Long DOMAIN_ID = 3L;
    private static final String NAME = "test-lb";
    private static final Long ID = 4L;
    private static final boolean IS_VPC = true;
    private static final Long LB_ID = 5L;
    private static final String RULE_NAME = "test-rule";
    private static final String CIDR_LIST = "0.0.0.0/0";

    @Test
    public void testConstructorAndGetters() {
        // Act
        DeleteNetrisLoadBalancerRuleCommand command = new DeleteNetrisLoadBalancerRuleCommand(
                ZONE_ID, ACCOUNT_ID, DOMAIN_ID, NAME, ID, IS_VPC, LB_ID);

        // Assert
        Assert.assertEquals(ZONE_ID, command.getZoneId());
        Assert.assertEquals(ACCOUNT_ID, command.getAccountId());
        Assert.assertEquals(DOMAIN_ID, command.getDomainId());
        Assert.assertEquals(NAME, command.getName());
        Assert.assertEquals(ID, command.getId());
        Assert.assertEquals(IS_VPC, command.isVpc());
        Assert.assertEquals(LB_ID, command.getLbId());
    }

    @Test
    public void testConstructorWithNullValues() {
        // Act
        DeleteNetrisLoadBalancerRuleCommand command = new DeleteNetrisLoadBalancerRuleCommand(
                ZONE_ID, null, null, NAME, null, IS_VPC, LB_ID);

        // Assert
        Assert.assertEquals(ZONE_ID, command.getZoneId());
        Assert.assertNull(command.getAccountId());
        Assert.assertNull(command.getDomainId());
        Assert.assertEquals(NAME, command.getName());
        Assert.assertNull(command.getId());
        Assert.assertEquals(IS_VPC, command.isVpc());
        Assert.assertEquals(LB_ID, command.getLbId());
    }

    @Test
    public void testSetters() {
        // Arrange
        DeleteNetrisLoadBalancerRuleCommand command = new DeleteNetrisLoadBalancerRuleCommand(
                ZONE_ID, ACCOUNT_ID, DOMAIN_ID, NAME, ID, IS_VPC, LB_ID);

        // Act
        command.setRuleName(RULE_NAME);
        command.setCidrList(CIDR_LIST);
        command.setLbId(LB_ID);

        // Assert
        Assert.assertEquals(RULE_NAME, command.getRuleName());
        Assert.assertEquals(CIDR_LIST, command.getCidrList());
        Assert.assertEquals(LB_ID, command.getLbId());
    }

    @Test
    public void testConstructorWithEmptyStrings() {
        // Act
        DeleteNetrisLoadBalancerRuleCommand command = new DeleteNetrisLoadBalancerRuleCommand(
                ZONE_ID, ACCOUNT_ID, DOMAIN_ID, "", ID, IS_VPC, LB_ID);

        // Assert
        Assert.assertEquals(ZONE_ID, command.getZoneId());
        Assert.assertEquals(ACCOUNT_ID, command.getAccountId());
        Assert.assertEquals(DOMAIN_ID, command.getDomainId());
        Assert.assertEquals("", command.getName());
        Assert.assertEquals(ID, command.getId());
        Assert.assertEquals(IS_VPC, command.isVpc());
        Assert.assertEquals(LB_ID, command.getLbId());
    }
}
