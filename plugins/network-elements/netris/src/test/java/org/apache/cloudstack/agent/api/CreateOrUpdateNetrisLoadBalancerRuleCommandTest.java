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

import com.cloud.network.netris.NetrisLbBackend;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class CreateOrUpdateNetrisLoadBalancerRuleCommandTest {

    private static final long ZONE_ID = 1L;
    private static final Long ACCOUNT_ID = 2L;
    private static final Long DOMAIN_ID = 3L;
    private static final String NAME = "test-lb";
    private static final Long ID = 4L;
    private static final boolean IS_VPC = true;
    private static final Long LB_ID = 5L;
    private static final String PUBLIC_IP = "10.0.0.1";
    private static final String PUBLIC_PORT = "80";
    private static final String PRIVATE_PORT = "8080";
    private static final String ALGORITHM = "roundrobin";
    private static final String PROTOCOL = "tcp";
    private static final String CIDR_LIST = "0.0.0.0/0";
    private static final String RULE_NAME = "test-rule";
    private static final Long INSTANCE_ID = 6L;
    private static final String BACKEND_IP = "10.0.0.2";
    private static final Integer BACKEND_PORT = 8080;

    @Test
    public void testConstructorAndGetters() {
        // Arrange
        List<NetrisLbBackend> lbBackends = Arrays.asList(
                new NetrisLbBackend(INSTANCE_ID, BACKEND_IP, BACKEND_PORT)
        );

        // Act
        CreateOrUpdateNetrisLoadBalancerRuleCommand command = new CreateOrUpdateNetrisLoadBalancerRuleCommand(
                ZONE_ID, ACCOUNT_ID, DOMAIN_ID, NAME, ID, IS_VPC,
                lbBackends, LB_ID, PUBLIC_IP, PUBLIC_PORT,
                PRIVATE_PORT, ALGORITHM, PROTOCOL);

        // Assert
        Assert.assertEquals(ZONE_ID, command.getZoneId());
        Assert.assertEquals(ACCOUNT_ID, command.getAccountId());
        Assert.assertEquals(DOMAIN_ID, command.getDomainId());
        Assert.assertEquals(NAME, command.getName());
        Assert.assertEquals(ID, command.getId());
        Assert.assertEquals(IS_VPC, command.isVpc());
        Assert.assertEquals(LB_ID, command.getLbId());
        Assert.assertEquals(PUBLIC_IP, command.getPublicIp());
        Assert.assertEquals(PUBLIC_PORT, command.getPublicPort());
        Assert.assertEquals(PRIVATE_PORT, command.getPrivatePort());
        Assert.assertEquals(ALGORITHM, command.getAlgorithm());
        Assert.assertEquals(PROTOCOL, command.getProtocol());
        Assert.assertEquals(lbBackends, command.getLbBackends());
    }

    @Test
    public void testConstructorWithNullValues() {
        // Act
        CreateOrUpdateNetrisLoadBalancerRuleCommand command = new CreateOrUpdateNetrisLoadBalancerRuleCommand(
                ZONE_ID, null, null, NAME, null, IS_VPC,
                null, LB_ID, PUBLIC_IP, PUBLIC_PORT,
                PRIVATE_PORT, ALGORITHM, PROTOCOL);

        // Assert
        Assert.assertEquals(ZONE_ID, command.getZoneId());
        Assert.assertNull(command.getAccountId());
        Assert.assertNull(command.getDomainId());
        Assert.assertEquals(NAME, command.getName());
        Assert.assertNull(command.getId());
        Assert.assertEquals(IS_VPC, command.isVpc());
        Assert.assertEquals(LB_ID, command.getLbId());
        Assert.assertEquals(PUBLIC_IP, command.getPublicIp());
        Assert.assertEquals(PUBLIC_PORT, command.getPublicPort());
        Assert.assertEquals(PRIVATE_PORT, command.getPrivatePort());
        Assert.assertEquals(ALGORITHM, command.getAlgorithm());
        Assert.assertEquals(PROTOCOL, command.getProtocol());
        Assert.assertNull(command.getLbBackends());
    }

    @Test
    public void testSetters() {
        // Arrange
        CreateOrUpdateNetrisLoadBalancerRuleCommand command = new CreateOrUpdateNetrisLoadBalancerRuleCommand(
                ZONE_ID, ACCOUNT_ID, DOMAIN_ID, NAME, ID, IS_VPC,
                null, LB_ID, PUBLIC_IP, PUBLIC_PORT,
                PRIVATE_PORT, ALGORITHM, PROTOCOL);

        // Act
        command.setCidrList(CIDR_LIST);
        command.setRuleName(RULE_NAME);

        // Assert
        Assert.assertEquals(CIDR_LIST, command.getCidrList());
        Assert.assertEquals(RULE_NAME, command.getRuleName());
    }

    @Test
    public void testConstructorWithEmptyStrings() {
        // Act
        CreateOrUpdateNetrisLoadBalancerRuleCommand command = new CreateOrUpdateNetrisLoadBalancerRuleCommand(
                ZONE_ID, ACCOUNT_ID, DOMAIN_ID, "", ID, IS_VPC,
                null, LB_ID, "", "",
                "", "", "");

        // Assert
        Assert.assertEquals(ZONE_ID, command.getZoneId());
        Assert.assertEquals(ACCOUNT_ID, command.getAccountId());
        Assert.assertEquals(DOMAIN_ID, command.getDomainId());
        Assert.assertEquals("", command.getName());
        Assert.assertEquals(ID, command.getId());
        Assert.assertEquals(IS_VPC, command.isVpc());
        Assert.assertEquals(LB_ID, command.getLbId());
        Assert.assertEquals("", command.getPublicIp());
        Assert.assertEquals("", command.getPublicPort());
        Assert.assertEquals("", command.getPrivatePort());
        Assert.assertEquals("", command.getAlgorithm());
        Assert.assertEquals("", command.getProtocol());
        Assert.assertNull(command.getLbBackends());
    }
}
