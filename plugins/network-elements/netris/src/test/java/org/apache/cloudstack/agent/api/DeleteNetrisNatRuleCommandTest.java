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

import org.junit.Test;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertEquals;

public class DeleteNetrisNatRuleCommandTest {

    private static final long ZONE_ID = 1L;
    private static final Long ACCOUNT_ID = 2L;
    private static final Long DOMAIN_ID = 3L;
    private static final String VNET_NAME = "test-vnet";
    private static final Long NETWORK_ID = 4L;
    private static final String VPC_NAME = "test-vpc";
    private static final Long VPC_ID = 5L;
    private static final boolean IS_VPC = true;
    private static final String NAT_RULE_TYPE = "DNAT";
    private static final String NAT_RULE_NAME = "test-nat-rule";
    private static final String NAT_IP = "10.0.0.1";

    @Test
    public void testConstructorAndGetters() {
        // Act
        DeleteNetrisNatRuleCommand command = new DeleteNetrisNatRuleCommand(
                ZONE_ID, ACCOUNT_ID, DOMAIN_ID, VPC_NAME, VPC_ID, VNET_NAME, NETWORK_ID, IS_VPC);

        // Assert
        assertEquals(ZONE_ID, command.getZoneId());
        assertEquals(ACCOUNT_ID, command.getAccountId());
        assertEquals(DOMAIN_ID, command.getDomainId());
        assertEquals(VPC_NAME, command.getVpcName());
        assertEquals(VPC_ID, command.getVpcId());
        assertEquals(VNET_NAME, command.getName());
        assertEquals(NETWORK_ID, command.getId());
        assertEquals(IS_VPC, command.isVpc());
        assertNull(command.getNatRuleType());
        assertNull(command.getNatRuleName());
        assertNull(command.getNatIp());
    }

    @Test
    public void testSettersAndGetters() {
        // Arrange
        DeleteNetrisNatRuleCommand command = new DeleteNetrisNatRuleCommand(
                ZONE_ID, ACCOUNT_ID, DOMAIN_ID, VPC_NAME, VPC_ID, VNET_NAME, NETWORK_ID, IS_VPC);

        // Act
        command.setNatRuleType(NAT_RULE_TYPE);
        command.setNatRuleName(NAT_RULE_NAME);
        command.setNatIp(NAT_IP);
        command.setVpcName(VPC_NAME);
        command.setVpcId(VPC_ID);

        // Assert
        assertEquals(NAT_RULE_TYPE, command.getNatRuleType());
        assertEquals(NAT_RULE_NAME, command.getNatRuleName());
        assertEquals(NAT_IP, command.getNatIp());
        assertEquals(VPC_NAME, command.getVpcName());
        assertEquals(VPC_ID, command.getVpcId());
    }

    @Test
    public void testConstructorWithEmptyStrings() {
        // Act
        DeleteNetrisNatRuleCommand command = new DeleteNetrisNatRuleCommand(
                ZONE_ID, ACCOUNT_ID, DOMAIN_ID, "", VPC_ID, "", NETWORK_ID, IS_VPC);

        // Assert
        assertEquals(ZONE_ID, command.getZoneId());
        assertEquals(ACCOUNT_ID, command.getAccountId());
        assertEquals(DOMAIN_ID, command.getDomainId());
        assertEquals("", command.getVpcName());
        assertEquals(VPC_ID, command.getVpcId());
        assertEquals("", command.getName());
        assertEquals(NETWORK_ID, command.getId());
        assertEquals(IS_VPC, command.isVpc());
    }
}
