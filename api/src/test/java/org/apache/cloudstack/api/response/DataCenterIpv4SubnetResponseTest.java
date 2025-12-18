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
public final class DataCenterIpv4SubnetResponseTest {

    private static String uuid = "uuid";
    private static String subnet = "10.10.10.0/26";
    private static String accountName = "account-name";
    private static String domainId = "domain-uuid";
    private static String domainName = "domain-name";
    private static String projectId = "project-uuid";
    private static String projectName = "project-name";
    private static String zoneId = "zone-id";
    private static String zoneName = "zone-name";
    private static Date created = new Date();

    @Test
    public void testDataCenterIpv4SubnetResponse() {
        final DataCenterIpv4SubnetResponse response = new DataCenterIpv4SubnetResponse();

        response.setId(uuid);
        response.setSubnet(subnet);
        response.setAccountName(accountName);
        response.setDomainId(domainId);
        response.setDomainName(domainName);
        response.setProjectId(projectId);
        response.setProjectName(projectName);
        response.setZoneId(zoneId);
        response.setZoneName(zoneName);
        response.setCreated(created);

        Assert.assertEquals(uuid, response.getId());
        Assert.assertEquals(subnet, response.getSubnet());
        Assert.assertEquals(accountName, response.getAccountName());
        Assert.assertEquals(domainId, response.getDomainId());
        Assert.assertEquals(domainName, response.getDomainName());
        Assert.assertEquals(projectId, response.getProjectId());
        Assert.assertEquals(projectName, response.getProjectName());
        Assert.assertEquals(zoneId, response.getZoneId());
        Assert.assertEquals(zoneName, response.getZoneName());
        Assert.assertEquals(created, response.getCreated());
    }
}
