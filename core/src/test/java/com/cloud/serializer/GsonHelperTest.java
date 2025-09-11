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

package com.cloud.serializer;

import com.cloud.agent.api.to.NfsTO;
import com.cloud.storage.DataStoreRole;
import com.cloud.utils.DateUtil;
import com.google.gson.Gson;
import org.junit.Before;
import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Test cases to verify working order of GsonHelper.java
 * with regards to a concrete implementation of the DataStoreTO
 * interface
 */
public class GsonHelperTest {

    private Gson gson;
    private Gson gsonLogger;
    private NfsTO nfsTO;

    @Before
    public void setUp() {
        gson = GsonHelper.getGson();
        gsonLogger = GsonHelper.getGsonLogger();
        nfsTO = new NfsTO("http://example.com", DataStoreRole.Primary);
    }

    @Test
    public void testGsonSerialization() {
        String json = gson.toJson(nfsTO);
        assertNotNull(json);
        assertTrue(json.contains("\"_url\":\"http://example.com\""));
        assertTrue(json.contains("\"_role\":\"Primary\""));
    }

    @Test
    public void testGsonDeserialization() {
        String json = "{\"_url\":\"http://example.com\",\"_role\":\"Primary\"}";
        NfsTO deserializedNfsTO = gson.fromJson(json, NfsTO.class);
        assertNotNull(deserializedNfsTO);
        assertEquals("http://example.com", deserializedNfsTO.getUrl());
        assertEquals(DataStoreRole.Primary, deserializedNfsTO.getRole());
    }

    @Test
    public void testGsonLoggerSerialization() {
        String json = gsonLogger.toJson(nfsTO);
        assertNotNull(json);
        assertTrue(json.contains("\"_url\":\"http://example.com\""));
        assertTrue(json.contains("\"_role\":\"Primary\""));
    }

    @Test
    public void testGsonLoggerDeserialization() {
        String json ="{\"_url\":\"http://example.com\",\"_role\":\"Primary\"}";
        NfsTO deserializedNfsTO = gsonLogger.fromJson(json, NfsTO.class);
        assertNotNull(deserializedNfsTO);
        assertEquals("http://example.com", deserializedNfsTO.getUrl());
        assertEquals(DataStoreRole.Primary, deserializedNfsTO.getRole());
    }

    @Test
    public void testGsonDateFormatSerialization() {
        Date now = new Date();
        TestClass testObj = new TestClass("TestString", 999, now);
        String json = gson.toJson(testObj);

        assertTrue(json.contains("TestString"));
        assertTrue(json.contains("999"));
        String expectedDate = new SimpleDateFormat(DateUtil.ZONED_DATETIME_FORMAT).format(now);
        assertTrue(json.contains(expectedDate));
    }

    @Test
    public void testGsonDateFormatDeserialization() throws Exception {
        String json = "{\"str\":\"TestString\",\"num\":999,\"date\":\"2025-08-22T15:39:43+0000\"}";
        TestClass testObj = gson.fromJson(json, TestClass.class);

        assertEquals("TestString", testObj.getStr());
        assertEquals(999, testObj.getNum());
        Date expectedDate = new SimpleDateFormat(DateUtil.ZONED_DATETIME_FORMAT).parse("2025-08-22T15:39:43+0000");
        assertEquals(expectedDate, testObj.getDate());
    }

    private static class TestClass {
        private String str;
        private int num;
        private Date date;

        public TestClass(String str, int num, Date date) {
            this.str = str;
            this.num = num;
            this.date = date;
        }

        public String getStr() {
            return str;
        }

        public int getNum() {
            return num;
        }

        public Date getDate() {
            return date;
        }
    }
}
