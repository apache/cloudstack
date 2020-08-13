//
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
//
package com.cloud.utils;

import org.junit.Test;

import java.util.Locale;
import static org.junit.Assert.assertEquals;
import static com.cloud.utils.HumanReadableJson.getHumanReadableBytesJson;

public class HumanReadableJsonTest {

    @Test
    public void parseJsonObjectTest() {
        assertEquals("{}", getHumanReadableBytesJson("{}"));
    }
    @Test
    public void parseJsonArrayTest() {
        assertEquals("[]", getHumanReadableBytesJson("[]"));
        assertEquals("[[],[]]", getHumanReadableBytesJson("[[],[]]"));
        assertEquals("[{},{}]", getHumanReadableBytesJson("[{},{}]"));
    }
    @Test
    public void parseSimpleJsonTest() {
        assertEquals("[{\"object\":{}}]", getHumanReadableBytesJson("[{\"object\":{}}]"));
    }
    @Test
    public void parseComplexJsonTest() {
        assertEquals("[{\"object\":[],\"object2\":[]},{}]", getHumanReadableBytesJson("[{\"object\":[],\"object2\":[]},{}]"));
        assertEquals("[{\"object\":{},\"object2\":{}}]", getHumanReadableBytesJson("[{\"object\":{},\"object2\":{}}]"));
        assertEquals("[{\"object\":[{},{}]}]", getHumanReadableBytesJson("[{\"object\":[{},{}]}]"));
        assertEquals("[{\"object\":[]},{\"object\":[]}]", getHumanReadableBytesJson("[{\"object\":[]},{\"object\":[]}]"));
        assertEquals("[{\"object\":[{\"object\":[]}]},{\"object\":[]}]", getHumanReadableBytesJson("[{\"object\":[{\"object\":[]}]},{\"object\":[]}]"));
    }
    @Test
    public void parseMatchJsonTest() {
        assertEquals("[{\"size\":\"(0 bytes) 0\"}]", getHumanReadableBytesJson("[{\"size\": \"0\"}]"));
        assertEquals("[{\"size\":\"(0 bytes) 0\",\"bytesSent\":\"(0 bytes) 0\"}]", getHumanReadableBytesJson("[{\"size\": \"0\", \"bytesSent\": \"0\"}]"));
    }

    @Test
    public void localeTest() {
        Locale.setDefault(Locale.UK); // UK test
        assertEquals("[{\"size\":\"(100.05 KB) 102456\"}]", getHumanReadableBytesJson("[{\"size\": \"102456\"}]"));
        Locale.setDefault(Locale.US); // US test
        assertEquals("[{\"size\":\"(100.05 KB) 102456\"}]", getHumanReadableBytesJson("[{\"size\": \"102456\"}]"));
        Locale.setDefault(Locale.forLanguageTag("en-ZA")); // Other region test
        assertEquals("[{\"size\":\"(100,05 KB) 102456\"}]", getHumanReadableBytesJson("[{\"size\": \"102456\"}]"));
    }
}
