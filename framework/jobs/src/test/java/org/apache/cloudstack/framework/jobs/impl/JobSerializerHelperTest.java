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
package org.apache.cloudstack.framework.jobs.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.junit.Test;

import com.cloud.utils.exception.CloudRuntimeException;

public class JobSerializerHelperTest {

    /**
     * A stand-in for a VO/entity object: not Serializable, the way real metadata values
     * (VMInstanceVO, Account, StoragePoolVO, ...) commonly are not.
     */
    private static class NonSerializableEntity {
        private final String name;

        NonSerializableEntity(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    @Test
    public void testSerializingCloudRuntimeExceptionNormalizesNonSerializableMetadata() {
        CloudRuntimeException ex = new CloudRuntimeException("Unable to find network with ID abc");
        ex.setMessageKey("vm.deploy.network.not.found");
        ex.setMetadata(Map.of("network", new NonSerializableEntity("my-network")));

        // this would throw NotSerializableException if the metadata weren't normalized to
        // string values first, since NonSerializableEntity does not implement Serializable
        String serialized = JobSerializerHelper.toObjectSerializedString(ex);

        assertNotNull(serialized);
        // the exception instance passed in is mutated in place by normalizeMetadata
        Object networkValue = ex.getMetadata().get("network");
        assertTrue("metadata value should have been converted to a String", networkValue instanceof String);
        assertEquals("my-network", networkValue);
    }

    @Test
    public void testSerializingCloudRuntimeExceptionRoundTripsMessageAndKey() {
        CloudRuntimeException ex = new CloudRuntimeException("Unable to find network with ID abc");
        ex.setMessageKey("vm.deploy.network.not.found");
        ex.setMetadata(Map.of("id", "abc"));

        String serialized = JobSerializerHelper.toObjectSerializedString(ex);
        Object result = JobSerializerHelper.fromObjectSerializedString(serialized);

        assertTrue(result instanceof CloudRuntimeException);
        CloudRuntimeException deserialized = (CloudRuntimeException) result;
        assertEquals("Unable to find network with ID abc", deserialized.getMessage());
        assertEquals("vm.deploy.network.not.found", deserialized.getMessageKey());
        assertEquals("abc", deserialized.getMetadata().get("id"));
    }

    @Test
    public void testSerializingCloudRuntimeExceptionWithNoMetadataDoesNotFail() {
        CloudRuntimeException ex = new CloudRuntimeException("plain failure");

        String serialized = JobSerializerHelper.toObjectSerializedString(ex);

        assertNotNull(serialized);
        assertNull(ex.getMetadata());
    }

    @Test
    public void testSerializingNonCloudRuntimeExceptionObjectIsUnaffected() {
        String plainValue = "just a plain serializable object";

        String serialized = JobSerializerHelper.toObjectSerializedString(plainValue);
        Object result = JobSerializerHelper.fromObjectSerializedString(serialized);

        assertEquals(plainValue, result);
    }
}
