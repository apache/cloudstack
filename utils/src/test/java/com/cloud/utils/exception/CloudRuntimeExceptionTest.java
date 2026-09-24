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

package com.cloud.utils.exception;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.util.Map;

import org.junit.Test;

public class CloudRuntimeExceptionTest {

    @Test
    public void testWrappingCloudRuntimeExceptionCopiesMessageKeyAndMetadata() {
        CloudRuntimeException cause = new CloudRuntimeException("original message");
        cause.setMessageKey("some.error.key");
        Map<String, Object> metadata = Map.of("id", "abc-123");
        cause.setMetadata(metadata);

        CloudRuntimeException wrapper = new CloudRuntimeException(cause);

        assertEquals("some.error.key", wrapper.getMessageKey());
        assertSame(metadata, wrapper.getMetadata());
        assertEquals("original message", wrapper.getMessage());
    }

    @Test
    public void testWrappingCloudRuntimeExceptionWithNoKeyOrMetadataLeavesThemNull() {
        CloudRuntimeException cause = new CloudRuntimeException("plain message");

        CloudRuntimeException wrapper = new CloudRuntimeException(cause);

        assertNull(wrapper.getMessageKey());
        assertNull(wrapper.getMetadata());
    }

    @Test
    public void testWrappingNonCloudRuntimeThrowableLeavesKeyAndMetadataNull() {
        Throwable cause = new IllegalStateException("boom");

        CloudRuntimeException wrapper = new CloudRuntimeException(cause);

        assertNull(wrapper.getMessageKey());
        assertNull(wrapper.getMetadata());
        assertEquals("boom", wrapper.getMessage());
    }

    @Test
    public void testWrappingCloudRuntimeExceptionSubclassAlsoCopiesMetadata() {
        // sanity check that the instanceof check in the constructor also matches
        // subclasses of CloudRuntimeException (e.g. ConcurrentOperationException-style types)
        CloudRuntimeException cause = new CloudRuntimeException("sub message") {
        };
        cause.setMessageKey("sub.key");
        cause.setMetadata(Map.of("k", "v"));

        CloudRuntimeException wrapper = new CloudRuntimeException((Throwable) cause);

        assertEquals("sub.key", wrapper.getMessageKey());
        assertEquals(Map.of("k", "v"), wrapper.getMetadata());
    }

    @Test
    public void testGetSetMessageKeyAndMetadata() {
        CloudRuntimeException ex = new CloudRuntimeException("msg");
        assertNull(ex.getMessageKey());
        assertNull(ex.getMetadata());

        ex.setMessageKey("key.name");
        Map<String, Object> metadata = Map.of("a", 1);
        ex.setMetadata(metadata);

        assertEquals("key.name", ex.getMessageKey());
        assertSame(metadata, ex.getMetadata());
    }
}
