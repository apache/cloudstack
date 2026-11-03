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

public class UpdateNetrisVnetCommandTest {

    private static final long ZONE_ID = 1L;
    private static final Long ACCOUNT_ID = 2L;
    private static final Long DOMAIN_ID = 3L;
    private static final String NAME = "test-network";
    private static final Long ID = 4L;
    private static final boolean IS_VPC = true;
    private static final String PREV_NETWORK_NAME = "prev-network";
    private static final String VPC_NAME = "test-vpc";
    private static final Long VPC_ID = 5L;

    @Test
    public void testConstructorAndGetters() {
        // Act
        UpdateNetrisVnetCommand command = new UpdateNetrisVnetCommand(
                ZONE_ID, ACCOUNT_ID, DOMAIN_ID, NAME, ID, IS_VPC);

        // Assert
        assertEquals(ZONE_ID, command.getZoneId());
        assertEquals(ACCOUNT_ID, command.getAccountId());
        assertEquals(DOMAIN_ID, command.getDomainId());
        assertEquals(NAME, command.getName());
        assertEquals(ID, command.getId());
        assertEquals(IS_VPC, command.isVpc());
        assertNull(command.getPrevNetworkName());
        assertNull(command.getVpcName());
        assertNull(command.getVpcId());
    }

    @Test
    public void testSettersAndGetters() {
        // Arrange
        UpdateNetrisVnetCommand command = new UpdateNetrisVnetCommand(
                ZONE_ID, ACCOUNT_ID, DOMAIN_ID, NAME, ID, IS_VPC);

        // Act
        command.setPrevNetworkName(PREV_NETWORK_NAME);
        command.setVpcName(VPC_NAME);
        command.setVpcId(VPC_ID);

        // Assert
        assertEquals(PREV_NETWORK_NAME, command.getPrevNetworkName());
        assertEquals(VPC_NAME, command.getVpcName());
        assertEquals(VPC_ID, command.getVpcId());
    }

    @Test
    public void testConstructorWithEmptyStrings() {
        // Act
        UpdateNetrisVnetCommand command = new UpdateNetrisVnetCommand(
                ZONE_ID, ACCOUNT_ID, DOMAIN_ID, "", ID, IS_VPC);

        // Assert
        assertEquals(ZONE_ID, command.getZoneId());
        assertEquals(ACCOUNT_ID, command.getAccountId());
        assertEquals(DOMAIN_ID, command.getDomainId());
        assertEquals("", command.getName());
        assertEquals(ID, command.getId());
        assertEquals(IS_VPC, command.isVpc());
        assertNull(command.getPrevNetworkName());
        assertNull(command.getVpcName());
        assertNull(command.getVpcId());
    }

    @Test
    public void testConstructorWithNullValues() {
        // Act
        UpdateNetrisVnetCommand command = new UpdateNetrisVnetCommand(
                ZONE_ID, null, null, NAME, null, IS_VPC);

        // Assert
        assertEquals(ZONE_ID, command.getZoneId());
        assertNull(command.getAccountId());
        assertNull(command.getDomainId());
        assertEquals(NAME, command.getName());
        assertNull(command.getId());
        assertEquals(IS_VPC, command.isVpc());
        assertNull(command.getPrevNetworkName());
        assertNull(command.getVpcName());
        assertNull(command.getVpcId());
    }
}
