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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class CreateOrUpdateNetrisNatCommandTest {

    private static final long ZONE_ID = 1L;
    private static final Long ACCOUNT_ID = 2L;
    private static final Long DOMAIN_ID = 3L;
    private static final String VPC_NAME = "test-vpc";
    private static final Long VPC_ID = 4L;
    private static final String VNET_NAME = "test-vnet";
    private static final Long NETWORK_ID = 5L;
    private static final boolean IS_VPC = true;
    private static final String VPC_CIDR = "10.0.0.0/24";
    private static final String NAT_RULE_NAME = "test-nat-rule";
    private static final String NAT_IP = "10.0.0.1";
    private static final String NAT_RULE_TYPE = "DNAT";
    private static final String PROTOCOL = "TCP";
    private static final String SOURCE_ADDRESS = "192.168.1.0/24";
    private static final String SOURCE_PORT = "80";
    private static final String DESTINATION_ADDRESS = "10.0.0.10";
    private static final String DESTINATION_PORT = "8080";
    private static final String STATE = "enabled";
    private static final String VM_IP = "10.0.0.100";

    @Test
    public void testConstructorAndGetters() {
        // Act
        CreateOrUpdateNetrisNatCommand command = new CreateOrUpdateNetrisNatCommand(
                ZONE_ID, ACCOUNT_ID, DOMAIN_ID, VPC_NAME, VPC_ID, VNET_NAME, NETWORK_ID, IS_VPC, VPC_CIDR);

        // Assert
        assertEquals(ZONE_ID, command.getZoneId());
        assertEquals(ACCOUNT_ID, command.getAccountId());
        assertEquals(DOMAIN_ID, command.getDomainId());
        assertEquals(VPC_NAME, command.getVpcName());
        assertEquals(VPC_ID, command.getVpcId());
        assertEquals(VNET_NAME, command.getName());
        assertEquals(NETWORK_ID, command.getId());
        assertEquals(IS_VPC, command.isVpc());
        assertEquals(VPC_CIDR, command.getVpcCidr());
        assertNull(command.getNatRuleName());
        assertNull(command.getNatIp());
        assertNull(command.getNatRuleType());
        assertNull(command.getProtocol());
        assertNull(command.getSourceAddress());
        assertNull(command.getSourcePort());
        assertNull(command.getDestinationAddress());
        assertNull(command.getDestinationPort());
        assertNull(command.getState());
        assertNull(command.getVmIp());
    }

    @Test
    public void testSettersAndGetters() {
        // Arrange
        CreateOrUpdateNetrisNatCommand command = new CreateOrUpdateNetrisNatCommand(
                ZONE_ID, ACCOUNT_ID, DOMAIN_ID, VPC_NAME, VPC_ID, VNET_NAME, NETWORK_ID, IS_VPC, VPC_CIDR);

        // Act
        command.setNatRuleName(NAT_RULE_NAME);
        command.setNatIp(NAT_IP);
        command.setNatRuleType(NAT_RULE_TYPE);
        command.setProtocol(PROTOCOL);
        command.setSourceAddress(SOURCE_ADDRESS);
        command.setSourcePort(SOURCE_PORT);
        command.setDestinationAddress(DESTINATION_ADDRESS);
        command.setDestinationPort(DESTINATION_PORT);
        command.setState(STATE);
        command.setVmIp(VM_IP);
        command.setVpcName(VPC_NAME);
        command.setVpcId(VPC_ID);
        command.setVpcCidr(VPC_CIDR);

        // Assert
        assertEquals(NAT_RULE_NAME, command.getNatRuleName());
        assertEquals(NAT_IP, command.getNatIp());
        assertEquals(NAT_RULE_TYPE, command.getNatRuleType());
        assertEquals(PROTOCOL, command.getProtocol());
        assertEquals(SOURCE_ADDRESS, command.getSourceAddress());
        assertEquals(SOURCE_PORT, command.getSourcePort());
        assertEquals(DESTINATION_ADDRESS, command.getDestinationAddress());
        assertEquals(DESTINATION_PORT, command.getDestinationPort());
        assertEquals(STATE, command.getState());
        assertEquals(VM_IP, command.getVmIp());
        assertEquals(VPC_NAME, command.getVpcName());
        assertEquals(VPC_ID, command.getVpcId());
        assertEquals(VPC_CIDR, command.getVpcCidr());
    }

    @Test
    public void testConstructorWithEmptyStrings() {
        // Act
        CreateOrUpdateNetrisNatCommand command = new CreateOrUpdateNetrisNatCommand(
                ZONE_ID, ACCOUNT_ID, DOMAIN_ID, "", VPC_ID, "", NETWORK_ID, IS_VPC, "");

        // Assert
        assertEquals(ZONE_ID, command.getZoneId());
        assertEquals(ACCOUNT_ID, command.getAccountId());
        assertEquals(DOMAIN_ID, command.getDomainId());
        assertEquals("", command.getVpcName());
        assertEquals(VPC_ID, command.getVpcId());
        assertEquals("", command.getName());
        assertEquals(NETWORK_ID, command.getId());
        assertEquals(IS_VPC, command.isVpc());
        assertEquals("", command.getVpcCidr());
    }

    @Test
    public void testConstructorWithNullValues() {
        // Act
        CreateOrUpdateNetrisNatCommand command = new CreateOrUpdateNetrisNatCommand(
                ZONE_ID, null, null, VPC_NAME, null, VNET_NAME, null, IS_VPC, VPC_CIDR);

        // Assert
        assertEquals(ZONE_ID, command.getZoneId());
        assertNull(command.getAccountId());
        assertNull(command.getDomainId());
        assertEquals(VPC_NAME, command.getVpcName());
        assertNull(command.getVpcId());
        assertEquals(VNET_NAME, command.getName());
        assertNull(command.getId());
        assertEquals(IS_VPC, command.isVpc());
        assertEquals(VPC_CIDR, command.getVpcCidr());
    }
}
