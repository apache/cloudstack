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
package org.apache.cloudstack.api.response;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Date;

@RunWith(MockitoJUnitRunner.class)
public final class ASNumberResponseTest {

    private static String uuid = "uuid";
    private static String accountId = "account-id";
    private static String accountName = "account-name";
    private static String domainId = "domain-uuid";
    private static String domainName = "domain-name";
    private static Long asNumber = 15000L;
    private static String asNumberRangeId = "as-number-range-uuid";
    private static String asNumberRange = "10000-20000";
    private static String zoneId = "zone-id";
    private static String zoneName = "zone-name";
    private static Date allocated = new Date();
    private static String allocationState = "allocated";

    private static String associatedNetworkId = "network-id";

    private static String associatedNetworkName = "network-name";

    private static String vpcId = "vpc-uuid";
    private static String vpcName = "vpc-name";
    private static Date created = new Date();



    @Test
    public void testASNumberResponse() {
        final ASNumberResponse response = new ASNumberResponse();

        response.setId(uuid);
        response.setAccountId(accountId);
        response.setAccountName(accountName);
        response.setDomainId(domainId);
        response.setDomainName(domainName);
        response.setAsNumber(asNumber);
        response.setAsNumberRangeId(asNumberRangeId);
        response.setAsNumberRange(asNumberRange);
        response.setZoneId(zoneId);
        response.setZoneName(zoneName);
        response.setAllocated(allocated);
        response.setAllocationState(allocationState);
        response.setAssociatedNetworkId(associatedNetworkId);
        response.setAssociatedNetworkName(associatedNetworkName);
        response.setVpcId(vpcId);
        response.setVpcName(vpcName);
        response.setCreated(created);

        Assert.assertEquals(uuid, response.getId());
        Assert.assertEquals(accountId, response.getAccountId());
        Assert.assertEquals(accountName, response.getAccountName());
        Assert.assertEquals(domainId, response.getDomainId());
        Assert.assertEquals(domainName, response.getDomainName());
        Assert.assertEquals(asNumber, response.getAsNumber());
        Assert.assertEquals(asNumberRangeId, response.getAsNumberRangeId());
        Assert.assertEquals(asNumberRange, response.getAsNumberRange());
        Assert.assertEquals(zoneId, response.getZoneId());
        Assert.assertEquals(zoneName, response.getZoneName());
        Assert.assertEquals(allocated, response.getAllocated());
        Assert.assertEquals(allocationState, response.getAllocationState());
        Assert.assertEquals(associatedNetworkId, response.getAssociatedNetworkId());
        Assert.assertEquals(associatedNetworkName, response.getAssociatedNetworkName());
        Assert.assertEquals(vpcId, response.getVpcId());
        Assert.assertEquals(vpcName, response.getVpcName());
        Assert.assertEquals(created, response.getCreated());
    }
}
