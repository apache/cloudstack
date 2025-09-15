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
import com.google.gson.JsonSyntaxException;
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
    public void testGsonDateFormatDeserializationWithSameDateFormat() throws Exception {
        String json = "{\"str\":\"TestString\",\"num\":999,\"date\":\"2025-08-22T15:39:43+0000\"}";
        TestClass testObj = gson.fromJson(json, TestClass.class);

        assertEquals("TestString", testObj.getStr());
        assertEquals(999, testObj.getNum());
        Date expectedDate = new SimpleDateFormat(DateUtil.ZONED_DATETIME_FORMAT).parse("2025-08-22T15:39:43+0000");
        assertEquals(expectedDate, testObj.getDate());
    }

    @Test (expected = JsonSyntaxException.class)
    public void testGsonDateFormatDeserializationWithDifferentDateFormat() throws Exception {
        String json = "{\"str\":\"TestString\",\"num\":999,\"date\":\"22/08/2025T15:39:43+0000\"}";
        gson.fromJson(json, TestClass.class);
        /* Deserialization throws the below exception:
        com.google.gson.JsonSyntaxException: 22/08/2025T15:39:43+0000

            at com.google.gson.DefaultTypeAdapters$DefaultDateTypeAdapter.deserializeToDate(DefaultTypeAdapters.java:376)
            at com.google.gson.DefaultTypeAdapters$DefaultDateTypeAdapter.deserialize(DefaultTypeAdapters.java:351)
            at com.google.gson.DefaultTypeAdapters$DefaultDateTypeAdapter.deserialize(DefaultTypeAdapters.java:307)
            at com.google.gson.JsonDeserializationVisitor.invokeCustomDeserializer(JsonDeserializationVisitor.java:92)
            at com.google.gson.JsonObjectDeserializationVisitor.visitFieldUsingCustomHandler(JsonObjectDeserializationVisitor.java:117)
            at com.google.gson.ReflectingFieldNavigator.visitFieldsReflectively(ReflectingFieldNavigator.java:63)
            at com.google.gson.ObjectNavigator.accept(ObjectNavigator.java:120)
            at com.google.gson.JsonDeserializationContextDefault.fromJsonObject(JsonDeserializationContextDefault.java:76)
            at com.google.gson.JsonDeserializationContextDefault.deserialize(JsonDeserializationContextDefault.java:54)
            at com.google.gson.Gson.fromJson(Gson.java:551)
            at com.google.gson.Gson.fromJson(Gson.java:498)
            at com.google.gson.Gson.fromJson(Gson.java:467)
            at com.google.gson.Gson.fromJson(Gson.java:417)
            at com.google.gson.Gson.fromJson(Gson.java:389)
            at com.cloud.serializer.GsonHelperTest.testGsonDateFormatDeserializationWithDifferentDateFormat(GsonHelperTest.java:113)
            at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
            at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
            at java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
            at java.base/java.lang.reflect.Method.invoke(Method.java:566)
            at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:59)
            at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)
            at org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:56)
            at org.junit.internal.runners.statements.InvokeMethod.evaluate(InvokeMethod.java:17)
            at org.junit.internal.runners.statements.RunBefores.evaluate(RunBefores.java:26)
            at org.junit.runners.ParentRunner$3.evaluate(ParentRunner.java:306)
            at org.junit.runners.BlockJUnit4ClassRunner$1.evaluate(BlockJUnit4ClassRunner.java:100)
            at org.junit.runners.ParentRunner.runLeaf(ParentRunner.java:366)
            at org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:103)
            at org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:63)
            at org.junit.runners.ParentRunner$4.run(ParentRunner.java:331)
            at org.junit.runners.ParentRunner$1.schedule(ParentRunner.java:79)
            at org.junit.runners.ParentRunner.runChildren(ParentRunner.java:329)
            at org.junit.runners.ParentRunner.access$100(ParentRunner.java:66)
            at org.junit.runners.ParentRunner$2.evaluate(ParentRunner.java:293)
            at org.junit.runners.ParentRunner$3.evaluate(ParentRunner.java:306)
            at org.junit.runners.ParentRunner.run(ParentRunner.java:413)
            at org.junit.runner.JUnitCore.run(JUnitCore.java:137)
            at com.intellij.junit4.JUnit4IdeaTestRunner.startRunnerWithArgs(JUnit4IdeaTestRunner.java:69)
            at com.intellij.rt.junit.IdeaTestRunner$Repeater$1.execute(IdeaTestRunner.java:38)
            at com.intellij.rt.execution.junit.TestsRepeater.repeat(TestsRepeater.java:11)
            at com.intellij.rt.junit.IdeaTestRunner$Repeater.startRunnerWithArgs(IdeaTestRunner.java:35)
            at com.intellij.rt.junit.JUnitStarter.prepareStreamsAndStart(JUnitStarter.java:231)
            at com.intellij.rt.junit.JUnitStarter.main(JUnitStarter.java:55)
        Caused by: java.text.ParseException: Unparseable date: "22/08/2025T15:39:43+0000"
            at java.base/java.text.DateFormat.parse(DateFormat.java:395)
            at com.google.gson.DefaultTypeAdapters$DefaultDateTypeAdapter.deserializeToDate(DefaultTypeAdapters.java:374)
            ... 42 more
         */
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
