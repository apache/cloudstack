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
package org.apache.cloudstack.api.command.admin.storage;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import org.apache.cloudstack.api.ApiCmdTestUtil;

public class CreateSecondaryStagingStoreCmdTest {

    @Test
    public void getDetails() throws IllegalArgumentException,
            IllegalAccessException {
        CreateSecondaryStagingStoreCmd cmd = new CreateSecondaryStagingStoreCmd();
        HashMap<String, Map<String, String>> details = new HashMap<String, Map<String, String>>();
        HashMap<String, String> kv = new HashMap<String, String>();
        kv.put("key", "TEST-KEY");
        kv.put("value", "TEST-VALUE");
        details.put("does not matter", kv);
        ApiCmdTestUtil.set(cmd, "details", details);
        Map<String, String> detailsMap = cmd.getDetails();
        Assert.assertNotNull(detailsMap);
        Assert.assertEquals(1, detailsMap.size());
        Assert.assertTrue(detailsMap.containsKey("TEST-KEY"));
        Assert.assertEquals("TEST-VALUE", detailsMap.get("TEST-KEY"));
    }

    @Test
    public void getDetailsEmpty() throws IllegalArgumentException,
            IllegalAccessException {
        CreateSecondaryStagingStoreCmd cmd = new CreateSecondaryStagingStoreCmd();
        ApiCmdTestUtil.set(cmd, "details", new HashMap<String, Map<String, String>>());
        Assert.assertNull(cmd.getDetails());
    }

    @Test
    public void getDetailsNull() throws IllegalArgumentException,
            IllegalAccessException {
        CreateSecondaryStagingStoreCmd cmd = new CreateSecondaryStagingStoreCmd();
        ApiCmdTestUtil.set(cmd, "details", null);
        Assert.assertNull(cmd.getDetails());
    }

}
