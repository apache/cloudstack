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

import junit.framework.Assert;

import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import com.cloud.bridge.util.JsonAccessor;
import com.cloud.gate.testcase.BaseTestCase;
import com.cloud.stack.models.CloudStackSnapshot;

public class JsonAccessorTestCase extends BaseTestCase {
    protected final static Logger logger = Logger.getLogger(UtilTestCase.class);

    public void testJsonAccessor() {
        JsonParser parser = new JsonParser();
        JsonElement json = parser.parse("{firstName: 'Kelven', lastName: 'Yang', arrayObj: [{name: 'elem1'}, {name: 'elem2'}]}");
        JsonAccessor jsonAccessor = new JsonAccessor(json);

        Assert.assertTrue("Kelven".equals(jsonAccessor.getAsString("firstName")));
        Assert.assertTrue("Kelven".equals(jsonAccessor.getAsString("this.firstName")));
        Assert.assertTrue("Yang".equals(jsonAccessor.getAsString("lastName")));
        Assert.assertTrue("Yang".equals(jsonAccessor.getAsString("this.lastName")));

        Assert.assertTrue("elem1".equals(jsonAccessor.getAsString("arrayObj[0].name")));
        Assert.assertTrue("elem2".equals(jsonAccessor.getAsString("arrayObj[1].name")));

        Assert.assertTrue("elem1".equals(jsonAccessor.getAsString("this.arrayObj.this[0].name")));
        Assert.assertTrue("elem2".equals(jsonAccessor.getAsString("this.arrayObj.this[1].name")));

        Assert.assertTrue(jsonAccessor.getMatchCount("firstName") == 1);
        Assert.assertTrue(jsonAccessor.getMatchCount("middleName") == -1);
        Assert.assertTrue(jsonAccessor.getMatchCount("arrayObj") == 2);
        Assert.assertTrue(jsonAccessor.getMatchCount("arrayObj[0]") == 1);
    }

    public void testGson() {
        String response =
            "{ \"queryasyncjobresultresponse\" : {\"jobid\":5868,\"jobstatus\":1,\"jobprocstatus\":0,\"jobresultcode\":0,\"jobresulttype\":\"object\",\"jobresult\":{\"snapshot\":{\"id\":3161,\"account\":\"admin\",\"domainid\":1,\"domain\":\"ROOT\",\"snapshottype\":\"MANUAL\",\"volumeid\":186928,\"volumename\":\"KY-DATA-VOL\",\"volumetype\":\"DATADISK\",\"created\":\"2011-06-02T05:05:41-0700\",\"name\":\"i-2-246446-VM_KY-DATA-VOL_20110602120541\",\"intervaltype\":\"MANUAL\",\"state\":\"BackedUp\"}}}}";

        JsonParser parser = new JsonParser();
        JsonElement json = parser.parse(response);
        JsonAccessor jsonAccessor = new JsonAccessor(json);

        Gson gson = new Gson();
        CloudStackSnapshot snapshot = gson.fromJson(jsonAccessor.eval("queryasyncjobresultresponse.jobresult.snapshot"), CloudStackSnapshot.class);

        Assert.assertTrue("BackedUp".equals(snapshot.getState()));
    }
}
