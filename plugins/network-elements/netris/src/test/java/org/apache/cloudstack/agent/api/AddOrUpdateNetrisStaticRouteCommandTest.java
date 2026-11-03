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

public class AddOrUpdateNetrisStaticRouteCommandTest {

    private static final long ZONE_ID = 1L;
    private static final Long ACCOUNT_ID = 2L;
    private static final Long DOMAIN_ID = 3L;
    private static final String NAME = "test-vpc";
    private static final Long ID = 4L;
    private static final boolean IS_VPC = true;
    private static final String PREFIX = "10.0.0.0/24";
    private static final String NEXT_HOP = "10.0.0.1";
    private static final Long ROUTE_ID = 5L;
    private static final boolean UPDATE_ROUTE = true;

    @Test
    public void testConstructorAndGetters() {
        // Act
        AddOrUpdateNetrisStaticRouteCommand command = new AddOrUpdateNetrisStaticRouteCommand(
                ZONE_ID, ACCOUNT_ID, DOMAIN_ID, NAME, ID, IS_VPC, PREFIX, NEXT_HOP, ROUTE_ID, UPDATE_ROUTE);

        // Assert
        Assert.assertEquals(ZONE_ID, command.getZoneId());
        Assert.assertEquals(ACCOUNT_ID, command.getAccountId());
        Assert.assertEquals(DOMAIN_ID, command.getDomainId());
        Assert.assertEquals(NAME, command.getName());
        Assert.assertEquals(ID, command.getId());
        Assert.assertEquals(IS_VPC, command.isVpc());
        Assert.assertEquals(PREFIX, command.getPrefix());
        Assert.assertEquals(NEXT_HOP, command.getNextHop());
        Assert.assertEquals(ROUTE_ID, command.getRouteId());
        Assert.assertEquals(UPDATE_ROUTE, command.isUpdateRoute());
    }

    @Test
    public void testConstructorWithNullValues() {
        // Act
        AddOrUpdateNetrisStaticRouteCommand command = new AddOrUpdateNetrisStaticRouteCommand(
                ZONE_ID, null, null, NAME, null, IS_VPC, PREFIX, NEXT_HOP, null, UPDATE_ROUTE);

        // Assert
        Assert.assertEquals(ZONE_ID, command.getZoneId());
        Assert.assertNull(command.getAccountId());
        Assert.assertNull(command.getDomainId());
        Assert.assertEquals(NAME, command.getName());
        Assert.assertNull(command.getId());
        Assert.assertEquals(IS_VPC, command.isVpc());
        Assert.assertEquals(PREFIX, command.getPrefix());
        Assert.assertEquals(NEXT_HOP, command.getNextHop());
        Assert.assertNull(command.getRouteId());
        Assert.assertEquals(UPDATE_ROUTE, command.isUpdateRoute());
    }

    @Test
    public void testConstructorWithEmptyStrings() {
        // Act
        AddOrUpdateNetrisStaticRouteCommand command = new AddOrUpdateNetrisStaticRouteCommand(
                ZONE_ID, ACCOUNT_ID, DOMAIN_ID, "", ID, IS_VPC, "", "", ROUTE_ID, UPDATE_ROUTE);

        // Assert
        Assert.assertEquals(ZONE_ID, command.getZoneId());
        Assert.assertEquals(ACCOUNT_ID, command.getAccountId());
        Assert.assertEquals(DOMAIN_ID, command.getDomainId());
        Assert.assertEquals("", command.getName());
        Assert.assertEquals(ID, command.getId());
        Assert.assertEquals(IS_VPC, command.isVpc());
        Assert.assertEquals("", command.getPrefix());
        Assert.assertEquals("", command.getNextHop());
        Assert.assertEquals(ROUTE_ID, command.getRouteId());
        Assert.assertEquals(UPDATE_ROUTE, command.isUpdateRoute());
    }
}
