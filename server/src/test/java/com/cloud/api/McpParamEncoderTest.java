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
package com.cloud.api;

import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.google.gson.JsonParser;

public class McpParamEncoderTest {

    private Map<String, String> encode(String json) {
        return McpParamEncoder.encode(JsonParser.parseString(json).getAsJsonObject());
    }

    @Test
    public void encodesPlainValuesAsStrings() {
        Map<String, String> encoded = encode("{\"name\": \"vm-1\", \"pagesize\": 20, \"listall\": true, \"cpuspeed\": 1.5}");

        Assert.assertEquals(Map.of("name", "vm-1", "pagesize", "20", "listall", "true", "cpuspeed", "1.5"), encoded);
    }

    @Test
    public void ignoresNullValues() {
        Assert.assertEquals(Map.of("name", "vm-1"), encode("{\"name\": \"vm-1\", \"keyword\": null, \"details\": {\"a\": null}}"));
    }

    @Test
    public void encodesListsAsCommaSeparatedValues() {
        Assert.assertEquals(Map.of("ids", "a,b,c", "ports", "80,443"), encode("{\"ids\": [\"a\", \"b\", \"c\"], \"ports\": [80, 443], \"empty\": []}"));
    }

    @Test
    public void encodesAnObjectAsTheFirstEntryOfAMap() {
        Assert.assertEquals(Map.of("details[0].cpuNumber", "2", "details[0].memory", "1024"),
                encode("{\"details\": {\"cpuNumber\": 2, \"memory\": 1024}}"));
    }

    @Test
    public void encodesAListOfObjectsAsIndexedMapEntries() {
        Assert.assertEquals(Map.of("rules[0].rule", "list*", "rules[0].permission", "allow", "rules[1].rule", "*", "rules[1].permission", "deny"),
                encode("{\"rules\": [{\"rule\": \"list*\", \"permission\": \"allow\"}, {\"rule\": \"*\", \"permission\": \"deny\"}]}"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsObjectsNestedInsideAMap() {
        encode("{\"details\": {\"a\": {\"b\": 1}}}");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsListsNestedInsideAList() {
        encode("{\"ids\": [[\"a\"]]}");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsListsMixingObjectsAndPlainValues() {
        encode("{\"rules\": [{\"rule\": \"*\"}, \"allow\"]}");
    }

    @Test
    public void rejectsReservedParametersInAnyCase() {
        for (String reserved : new String[] {"command", "response", "apikey", "signature", "signatureversion", "expires", "sessionkey", "httpmethod", "ApiKey"}) {
            Assert.assertThrows(reserved, IllegalArgumentException.class, () -> encode("{\"" + reserved + "\": \"x\"}"));
        }
    }
}
