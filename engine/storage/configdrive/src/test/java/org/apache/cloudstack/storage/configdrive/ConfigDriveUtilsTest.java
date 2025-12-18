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

package org.apache.cloudstack.storage.configdrive;

import static org.apache.cloudstack.storage.configdrive.ConfigDriveUtils.mergeJsonArraysAndUpdateObject;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import com.google.gson.JsonObject;

@RunWith(MockitoJUnitRunner.class)
public class ConfigDriveUtilsTest {

    @Test
    public void testMergeJsonArraysAndUpdateObjectWithEmptyObjects() {
        JsonObject finalObject = new JsonObject();
        JsonObject newObj = new JsonObject();
        mergeJsonArraysAndUpdateObject(finalObject, newObj, "links", "id", "type");
        Assert.assertEquals("{}", finalObject.toString());
    }

    @Test
    public void testMergeJsonArraysAndUpdateObjectWithNewMembersAdded() {
        JsonObject finalObject = new JsonObject();

        JsonObject newObj = new JsonObject();
        JsonArray newMembers = new JsonArray();
        JsonObject newMember = new JsonObject();
        newMember.addProperty("id", "eth0");
        newMember.addProperty("type", "phy");
        newMembers.add(newMember);
        newObj.add("links", newMembers);

        mergeJsonArraysAndUpdateObject(finalObject, newObj, "links", "id", "type");
        Assert.assertEquals(1, finalObject.getAsJsonArray("links").size());
        JsonObject expectedObj = new JsonParser().parse("{'links': [{'id': 'eth0', 'type': 'phy'}]}").getAsJsonObject();
        Assert.assertEquals(expectedObj, finalObject);
    }

    @Test
    public void testMergeJsonArraysAndUpdateObjectWithDuplicateMembersIgnored() {
        JsonObject finalObject = new JsonObject();
        JsonArray existingMembers = new JsonArray();
        JsonObject existingMember = new JsonObject();
        existingMember.addProperty("id", "eth0");
        existingMember.addProperty("type", "phy");
        existingMembers.add(existingMember);
        finalObject.add("links", existingMembers);

        JsonObject newObj = new JsonObject();
        newObj.add("links", existingMembers); // same as existingMembers for duplication

        mergeJsonArraysAndUpdateObject(finalObject, newObj, "links", "id", "type");
        Assert.assertEquals(1, finalObject.getAsJsonArray("links").size());
        JsonObject expectedObj = new JsonParser().parse("{'links': [{'id': 'eth0', 'type': 'phy'}]}").getAsJsonObject();
        Assert.assertEquals(expectedObj, finalObject);
    }

    @Test
    public void testMergeJsonArraysAndUpdateObjectWithDifferentMembers() {
        JsonObject finalObject = new JsonObject();

        JsonArray newMembers = new JsonArray();
        JsonObject newMember = new JsonObject();
        newMember.addProperty("id", "eth0");
        newMember.addProperty("type", "phy");
        newMembers.add(newMember);
        finalObject.add("links", newMembers);

        JsonObject newObj = new JsonObject();
        newMembers = new JsonArray();
        newMember = new JsonObject();
        newMember.addProperty("id", "eth1");
        newMember.addProperty("type", "phy");
        newMembers.add(newMember);
        newObj.add("links", newMembers);

        mergeJsonArraysAndUpdateObject(finalObject, newObj, "links", "id", "type");
        Assert.assertEquals(2, finalObject.getAsJsonArray("links").size());
        JsonObject expectedObj = new JsonParser().parse("{'links': [{'id': 'eth0', 'type': 'phy'}, {'id': 'eth1', 'type': 'phy'}]}").getAsJsonObject();
        Assert.assertEquals(expectedObj, finalObject);
    }

    @Test(expected = NullPointerException.class)
    public void testMergeJsonArraysAndUpdateObjectWithNullObjects() {
        mergeJsonArraysAndUpdateObject(null, null, "services", "id", "type");
    }
}
