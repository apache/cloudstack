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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

public class SetupNetrisPublicRangeCommandTest {

    private static final Long ZONE_ID = 1L;
    private static final String SUPER_CIDR = "10.0.0.0/16";
    private static final String EXACT_CIDR = "10.0.1.0/24";

    @Test
    public void testConstructorAndGetters() {
        // Act
        SetupNetrisPublicRangeCommand command = new SetupNetrisPublicRangeCommand(
                ZONE_ID, SUPER_CIDR, EXACT_CIDR);

        // Assert
        assertEquals(ZONE_ID.longValue(), command.getZoneId());
        assertEquals(SUPER_CIDR, command.getSuperCidr());
        assertEquals(EXACT_CIDR, command.getExactCidr());
        assertNull(command.getAccountId());
        assertNull(command.getDomainId());
        assertNull(command.getName());
        assertEquals(ZONE_ID, command.getId());
        assertFalse(command.isVpc());
    }

    @Test
    public void testConstructorWithEmptyStrings() {
        // Act
        SetupNetrisPublicRangeCommand command = new SetupNetrisPublicRangeCommand(
                ZONE_ID, "", "");

        // Assert
        assertEquals(ZONE_ID.longValue(), command.getZoneId());
        assertEquals("", command.getSuperCidr());
        assertEquals("", command.getExactCidr());
        assertNull(command.getAccountId());
        assertNull(command.getDomainId());
        assertNull(command.getName());
        assertEquals(ZONE_ID, command.getId());
        assertFalse(command.isVpc());
    }
}
