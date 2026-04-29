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

package org.apache.cloudstack.veeam.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;

import org.junit.Test;

public class DataUtilTest {

    @Test
    public void testB64Url_UsesUrlSafeAlphabetAndNoPadding() {
        final String encoded = DataUtil.b64Url(new byte[]{(byte)0xfb, (byte)0xff});
        assertEquals("-_8", encoded);
    }

    @Test
    public void testJsonEscape_NullAndEscapedCharacters() {
        assertEquals("", DataUtil.jsonEscape(null));
        assertEquals("a\\\\b\\\"c", DataUtil.jsonEscape("a\\b\"c"));
    }

    @Test
    public void testConstantTimeEquals_StringOverload() {
        assertTrue(DataUtil.constantTimeEquals("abc", "abc"));
        assertFalse(DataUtil.constantTimeEquals("abc", "abd"));
        assertFalse(DataUtil.constantTimeEquals(null, "abc"));
        assertFalse(DataUtil.constantTimeEquals("abc", null));
    }

    @Test
    public void testConstantTimeEquals_ByteArrayOverload() {
        final byte[] left = "sample".getBytes(StandardCharsets.UTF_8);
        assertTrue(DataUtil.constantTimeEquals(left, "sample".getBytes(StandardCharsets.UTF_8)));
        assertFalse(DataUtil.constantTimeEquals(left, "samples".getBytes(StandardCharsets.UTF_8)));
        assertFalse(DataUtil.constantTimeEquals(left, "samplE".getBytes(StandardCharsets.UTF_8)));
    }
}
