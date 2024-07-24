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
package org.apache.cloudstack.api.command.admin.vpc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import org.apache.cloudstack.api.ApiCmdTestUtil;
import org.apache.cloudstack.api.ApiConstants;
import org.springframework.test.util.ReflectionTestUtils;


public class CreateVPCOfferingCmdTest {

    @Test
    public void testServiceProviders() throws IllegalArgumentException,
            IllegalAccessException {
        CreateVPCOfferingCmd cmd = new CreateVPCOfferingCmd();
        HashMap<String, Map<String, String>> providers = new HashMap<String, Map<String, String>>();
        HashMap<String, String> kv = new HashMap<String, String>();
        kv.put("service", "TEST-SERVICE");
        kv.put("provider", "TEST-PROVIDER");
        providers.put("does not matter", kv);
        ApiCmdTestUtil.set(cmd, ApiConstants.SERVICE_PROVIDER_LIST, providers);
        Map<String, List<String>> providerMap = cmd.getServiceProviders();
        Assert.assertNotNull(providerMap);
        Assert.assertEquals(1, providerMap.size());
        Assert.assertTrue(providerMap.containsKey("TEST-SERVICE"));
        Assert.assertTrue(providerMap.get("TEST-SERVICE").contains("TEST-PROVIDER"));
    }

    @Test
    public void testServiceProvidersEmpty() throws IllegalArgumentException,
            IllegalAccessException {
        CreateVPCOfferingCmd cmd = new CreateVPCOfferingCmd();
        ApiCmdTestUtil.set(cmd, ApiConstants.SERVICE_PROVIDER_LIST, new HashMap<String, Map<String, String>>());
        Assert.assertNull(cmd.getServiceProviders());
    }

    @Test
    public void getDetailsNull() throws IllegalArgumentException,
            IllegalAccessException {
        CreateVPCOfferingCmd cmd = new CreateVPCOfferingCmd();
        ApiCmdTestUtil.set(cmd, ApiConstants.SERVICE_PROVIDER_LIST, null);
        Assert.assertNull(cmd.getServiceProviders());
    }

    @Test
    public void testCreateVPCOfferingWithEmptyDisplayText() {
        CreateVPCOfferingCmd cmd = new CreateVPCOfferingCmd();
        String netName = "net-vpc";
        ReflectionTestUtils.setField(cmd,"vpcOfferingName", netName);
        Assert.assertEquals(cmd.getDisplayText(), netName);
    }

}
