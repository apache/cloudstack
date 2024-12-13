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
import java.util.HashMap;
import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public final class BgpPeerResponseTest {

    private static String uuid = "uuid";
    private static String ip4Address = "ip4-address";
    private static String ip6Address = "ip6-address";
    private static Long asNumber = 15000L;
    private static String password = "password";
    private static String accountName = "account-name";
    private static String domainId = "domain-uuid";
    private static String domainName = "domain-name";
    private static String projectId = "project-uuid";
    private static String projectName = "project-name";
    private static String zoneId = "zone-id";
    private static String zoneName = "zone-name";
    private static Date created = new Date();

    @Test
    public void testBgpPeerResponse() {
        final BgpPeerResponse response = new BgpPeerResponse();

        response.setId(uuid);
        response.setIp4Address(ip4Address);
        response.setIp6Address(ip6Address);
        response.setAsNumber(asNumber);
        response.setPassword(password);
        response.setAccountName(accountName);
        response.setDomainId(domainId);
        response.setDomainName(domainName);
        response.setProjectId(projectId);
        response.setProjectName(projectName);
        response.setZoneId(zoneId);
        response.setZoneName(zoneName);
        response.setCreated(created);
        Map<String, String> details = new HashMap<>();
        details.put("key", "value");
        response.setDetails(details);

        Assert.assertEquals(uuid, response.getId());
        Assert.assertEquals(ip4Address, response.getIp4Address());
        Assert.assertEquals(ip6Address, response.getIp6Address());
        Assert.assertEquals(asNumber, response.getAsNumber());
        Assert.assertEquals(password, response.getPassword());
        Assert.assertEquals(accountName, response.getAccountName());
        Assert.assertEquals(domainId, response.getDomainId());
        Assert.assertEquals(domainName, response.getDomainName());
        Assert.assertEquals(projectId, response.getProjectId());
        Assert.assertEquals(projectName, response.getProjectName());
        Assert.assertEquals(zoneId, response.getZoneId());
        Assert.assertEquals(zoneName, response.getZoneName());
        Assert.assertEquals(created, response.getCreated());
        Assert.assertEquals(details, response.getDetails());
    }
}
