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
package org.apache.cloudstack.report;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import org.junit.Assert;
import org.junit.Test;

import com.cloud.storage.Storage;
import com.google.common.util.concurrent.AtomicLongMap;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

/**
 * The adapter that turns every counter in the usage report into a JSON object.
 * Its handling of keys decides most of the payload's wire format.
 */
public class AtomicGsonAdapterTest {

    private final AtomicGsonAdapter adapter = new AtomicGsonAdapter();

    private String write(AtomicLongMap<Object> value) throws IOException {
        StringWriter out = new StringWriter();
        try (JsonWriter writer = new JsonWriter(out)) {
            writer.setSerializeNulls(true);
            adapter.write(writer, value);
        }
        return out.toString();
    }

    @Test
    public void testNullMapIsWrittenAsJsonNull() throws IOException {
        Assert.assertEquals("null", write(null));
    }

    @Test
    public void testEmptyMapIsWrittenAsEmptyObject() throws IOException {
        Assert.assertEquals("{}", write(AtomicLongMap.create()));
    }

    /**
     * AtomicLongMap.asMap() is backed by a ConcurrentHashMap, so the order of keys
     * within a counter object is not deterministic. The receiving service must treat
     * these as unordered objects; only the key/value pairs are part of the contract.
     */
    @Test
    public void testCountsAreWrittenAsNumbers() throws IOException {
        AtomicLongMap<Object> counter = AtomicLongMap.create();
        counter.getAndIncrement("KVM");
        counter.getAndIncrement("KVM");
        counter.getAndIncrement("VMware");

        JsonObject json = JsonParser.parseString(write(counter)).getAsJsonObject();
        Assert.assertEquals(2, json.size());
        Assert.assertEquals(2, json.get("KVM").getAsLong());
        Assert.assertEquals(1, json.get("VMware").getAsLong());
    }

    /**
     * Boolean keys reach the wire as the strings "true" and "false"; the report uses
     * these for ha_enabled, dynamically_scalable, compute_only and use_local_storage.
     */
    @Test
    public void testBooleanKeysBecomeStringKeys() throws IOException {
        AtomicLongMap<Object> counter = AtomicLongMap.create();
        counter.getAndIncrement(Boolean.TRUE);
        counter.getAndIncrement(Boolean.FALSE);
        counter.getAndIncrement(Boolean.FALSE);

        String json = write(counter);
        Assert.assertTrue(json, json.contains("\"true\":1"));
        Assert.assertTrue(json, json.contains("\"false\":2"));
    }

    /**
     * Keys go through String.valueOf(), i.e. toString(), so an enum that overrides
     * toString() is serialized by that override and not by its constant name.
     */
    @Test
    public void testEnumKeysUseToStringNotConstantName() throws IOException {
        AtomicLongMap<Object> counter = AtomicLongMap.create();
        counter.getAndIncrement(Storage.ProvisioningType.THIN);

        Assert.assertEquals("{\"thin\":1}", write(counter));
    }

    @Test
    public void testNullKeysCannotReachThePayload() throws IOException {
        AtomicLongMap<Object> counter = AtomicLongMap.create();
        counter.getAndIncrement("KVM");

        // AtomicLongMap rejects null keys outright, so a "null" key can only ever
        // appear if a caller stringifies before counting. Guard the assumption.
        try {
            counter.getAndIncrement(null);
            Assert.fail("AtomicLongMap unexpectedly accepted a null key");
        } catch (NullPointerException expected) {
            // expected
        }

        Assert.assertEquals("{\"KVM\":1}", write(counter));
    }

    @Test
    public void testReadConsumesNullAndReturnsNull() throws IOException {
        try (JsonReader reader = new JsonReader(new StringReader("null"))) {
            Assert.assertNull(adapter.read(reader));
        }
    }
}
