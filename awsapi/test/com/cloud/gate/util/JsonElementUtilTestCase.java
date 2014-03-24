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
package com.cloud.gate.util;

import com.cloud.bridge.util.JsonElementUtil;
import com.google.gson.JsonArray;
import junit.framework.Assert;

import com.cloud.gate.testcase.BaseTestCase;
import com.cloud.stack.models.CloudStackSnapshot;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

public class JsonElementUtilTestCase extends BaseTestCase {
    private final static JsonParser JSON_PARSER = new JsonParser();

    public void testJsonElementUtils() {
    	JsonElement json = JSON_PARSER.parse("{firstName: 'Kelven', lastName: 'Yang', arrayObj: [{name: 'elem1'}, {name: 'elem2'}], level1: {level2: 'some'}}");

        assertEquals("Kelven", JsonElementUtil.getAsString(json, "firstName"));
        assertEquals("Yang", JsonElementUtil.getAsString(json, "lastName"));
        assertEquals("some", JsonElementUtil.getAsString(json, "level1", "level2"));
        assertTrue(JsonElementUtil.getAsJsonElement(json, "arrayObj") instanceof JsonArray);
    }
    
    public void testGson() {
    	String response = "{ \"queryasyncjobresultresponse\" : {\"jobid\":5868,\"jobstatus\":1,\"jobprocstatus\":0,\"jobresultcode\":0,\"jobresulttype\":\"object\",\"jobresult\":{\"snapshot\":{\"id\":3161,\"account\":\"admin\",\"domainid\":1,\"domain\":\"ROOT\",\"snapshottype\":\"MANUAL\",\"volumeid\":186928,\"volumename\":\"KY-DATA-VOL\",\"volumetype\":\"DATADISK\",\"created\":\"2011-06-02T05:05:41-0700\",\"name\":\"i-2-246446-VM_KY-DATA-VOL_20110602120541\",\"intervaltype\":\"MANUAL\",\"state\":\"BackedUp\"}}}}";
    	JsonElement json = JSON_PARSER.parse(response);
    	Gson gson = new Gson();
    	CloudStackSnapshot snapshot = gson.fromJson(JsonElementUtil.getAsJsonElement(json, "queryasyncjobresultresponse", "jobresult", "snapshot"), CloudStackSnapshot.class);
    	Assert.assertTrue("BackedUp".equals(snapshot.getState()));
    }
}
