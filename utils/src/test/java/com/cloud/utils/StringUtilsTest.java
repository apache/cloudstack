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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Test;

public class StringUtilsTest {

    @Test
    public void testGetPreferredCharset() {
        final boolean ifUtf8Supported = StringUtils.isUtf8Supported();
        if (ifUtf8Supported) {
            assertEquals(StringUtils.getPreferredCharset(), Charset.forName("UTF-8"));
        } else {
            assertNotEquals(StringUtils.getPreferredCharset(), Charset.forName("UTF-8"));
        }
    }

    @Test
    public void testGetDefaultCharset() {
        // Is this test irrelevant? Is wrapping the Charset.defaultCharset() too much?
        // This test was added in order to cover the new StringUtils.getDefaultCharset().
        // One cannot be sure that StringUtils.getPreferredCharset() will always be
        // equals to Charset.defaultCharset()
        assertEquals(StringUtils.getDefaultCharset(), Charset.defaultCharset());
    }

    @Test
    public void testCleanPasswordFromJsonObjectAtEnd() {
        final String input = "{\"foo\":\"bar\",\"password\":\"test\"}";
        //TODO: It would be nice to clean up the regex in question to not
        //have to return the trailing comma in the expected string below
        final String expected = "{\"foo\":\"bar\",}";
        final String result = StringUtils.cleanString(input);
        assertEquals(result, expected);
    }

    @Test
    public void testCleanPasswordFromJsonObjectInMiddle() {
        final String input = "{\"foo\":\"bar\",\"password\":\"test\",\"test\":\"blah\"}";
        final String expected = "{\"foo\":\"bar\",\"test\":\"blah\"}";
        final String result = StringUtils.cleanString(input);
        assertEquals(result, expected);
    }

    @Test
    public void testCleanPasswordFromJsonObjectAlone() {
        final String input = "{\"password\":\"test\"}";
        final String expected = "{}";
        final String result = StringUtils.cleanString(input);
        assertEquals(result, expected);
    }

    @Test
    public void testCleanPasswordFromJsonObjectAtStart() {
        final String input = "{\"password\":\"test\",\"test\":\"blah\"}";
        final String expected = "{\"test\":\"blah\"}";
        final String result = StringUtils.cleanString(input);
        assertEquals(result, expected);
    }

    @Test
    public void testCleanPasswordFromJsonObjectWithMultiplePasswords() {
        final String input = "{\"description\":\"foo\"}],\"password\":\"bar\",\"nic\":[{\"password\":\"bar2\",\"id\":\"1\"}]}";
        final String expected = "{\"description\":\"foo\"}],\"nic\":[{\"id\":\"1\"}]}";
        final String result = StringUtils.cleanString(input);
        assertEquals(result, expected);
    }

    @Test
    public void testCleanPasswordFromRequestString() {
        final String input = "username=foo&password=bar&url=foobar";
        final String expected = "username=foo&url=foobar";
        final String result = StringUtils.cleanString(input);
        assertEquals(result, expected);
    }

    @Test
    public void testCleanPasswordFromEncodedRequestString() {
        final String input = "name=SS1&provider=SMB&zoneid=5a60af2b-3025-4f2a-9ecc-8e33bf2b94e3&url=cifs%3A%2F%2F10.102.192.150%2FSMB-Share%2Fsowmya%2Fsecondary%3Fuser%3Dsowmya%26password%3DXXXXX%40123%26domain%3DBLR";
        final String expected = "name=SS1&provider=SMB&zoneid=5a60af2b-3025-4f2a-9ecc-8e33bf2b94e3&url=cifs%3A%2F%2F10.102.192.150%2FSMB-Share%2Fsowmya%2Fsecondary%3Fuser%3Dsowmya%26domain%3DBLR";
        final String result = StringUtils.cleanString(input);
        assertEquals(result, expected);
    }

    @Test
    public void testCleanPasswordFromRequestStringWithMultiplePasswords() {
        final String input = "username=foo&password=bar&url=foobar&password=bar2&test=4";
        final String expected = "username=foo&url=foobar&test=4";
        final String result = StringUtils.cleanString(input);
        assertEquals(result, expected);
    }

    @Test
    public void testCleanPasswordFromRequestStringMatchedAtEndSingleQuote() {
        final String input = "'username=foo&password=bar'";
        final String expected = "'username=foo'";
        final String result = StringUtils.cleanString(input);
        assertEquals(result, expected);
    }

    @Test
    public void testCleanPasswordFromRequestStringMatchedAtEndDoubleQuote() {
        final String input = "\"username=foo&password=bar\"";
        final String expected = "\"username=foo\"";
        final String result = StringUtils.cleanString(input);
        assertEquals(result, expected);
    }

    @Test
    public void testCleanPasswordFromRequestStringMatchedAtMiddleDoubleQuote() {
        final String input = "\"username=foo&password=bar&goo=sdf\"";
        final String expected = "\"username=foo&goo=sdf\"";
        final String result = StringUtils.cleanString(input);
        assertEquals(result, expected);
    }

    @Test
    public void testCleanSecretkeyFromJsonObjectAtEnd() {
        final String input = "{\"foo\":\"bar\",\"secretkey\":\"test\"}";
        // TODO: It would be nice to clean up the regex in question to not
        // have to return the trailing comma in the expected string below
        final String expected = "{\"foo\":\"bar\",}";
        final String result = StringUtils.cleanString(input);
        assertEquals(result, expected);
    }

    @Test
    public void testCleanSecretkeyFromJsonObjectInMiddle() {
        final String input = "{\"foo\":\"bar\",\"secretkey\":\"test\",\"test\":\"blah\"}";
        final String expected = "{\"foo\":\"bar\",\"test\":\"blah\"}";
        final String result = StringUtils.cleanString(input);
        assertEquals(result, expected);
    }

    @Test
    public void testCleanSecretkeyFromJsonObjectAlone() {
        final String input = "{\"secretkey\":\"test\"}";
        final String expected = "{}";
        final String result = StringUtils.cleanString(input);
        assertEquals(result, expected);
    }

    @Test
    public void testCleanSecretkeyFromJsonObjectAtStart() {
        final String input = "{\"secretkey\":\"test\",\"test\":\"blah\"}";
        final String expected = "{\"test\":\"blah\"}";
        final String result = StringUtils.cleanString(input);
        assertEquals(result, expected);
    }

    @Test
    public void testCleanSecretkeyFromJsonObjectWithMultiplePasswords() {
        final String input = "{\"description\":\"foo\"}],\"secretkey\":\"bar\",\"nic\":[{\"secretkey\":\"bar2\",\"id\":\"1\"}]}";
        final String expected = "{\"description\":\"foo\"}],\"nic\":[{\"id\":\"1\"}]}";
        final String result = StringUtils.cleanString(input);
        assertEquals(result, expected);
    }

    @Test
    public void testCleanAccesskeyFromJsonObjectAtEnd() {
        final String input = "{\"foo\":\"bar\",\"accesskey\":\"test\"}";
        // TODO: It would be nice to clean up the regex in question to not
        // have to return the trailing comma in the expected string below
        final String expected = "{\"foo\":\"bar\",}";
        final String result = StringUtils.cleanString(input);
        assertEquals(result, expected);
    }

    @Test
    public void testCleanAccesskeyFromJsonObjectInMiddle() {
        final String input = "{\"foo\":\"bar\",\"accesskey\":\"test\",\"test\":\"blah\"}";
        final String expected = "{\"foo\":\"bar\",\"test\":\"blah\"}";
        final String result = StringUtils.cleanString(input);
        assertEquals(result, expected);
    }

    @Test
    public void testCleanAccesskeyFromJsonObjectAlone() {
        final String input = "{\"accesskey\":\"test\"}";
        final String expected = "{}";
        final String result = StringUtils.cleanString(input);
        assertEquals(result, expected);
    }

    @Test
    public void testCleanAccesskeyFromJsonObjectAtStart() {
        final String input = "{\"accesskey\":\"test\",\"test\":\"blah\"}";
        final String expected = "{\"test\":\"blah\"}";
        final String result = StringUtils.cleanString(input);
        assertEquals(result, expected);
    }

    @Test
    public void testCleanAccesskeyFromJsonObjectWithMultiplePasswords() {
        final String input = "{\"description\":\"foo\"}],\"accesskey\":\"bar\",\"nic\":[{\"accesskey\":\"bar2\",\"id\":\"1\"}]}";
        final String expected = "{\"description\":\"foo\"}],\"nic\":[{\"id\":\"1\"}]}";
        final String result = StringUtils.cleanString(input);
        assertEquals(result, expected);
    }

    @Test
    public void testCleanAccesskeyFromRequestString() {
        final String input = "username=foo&accesskey=bar&url=foobar";
        final String expected = "username=foo&url=foobar";
        final String result = StringUtils.cleanString(input);
        assertEquals(result, expected);
    }

    @Test
    public void testCleanSecretkeyFromRequestString() {
        final String input = "username=foo&secretkey=bar&url=foobar";
        final String expected = "username=foo&url=foobar";
        final String result = StringUtils.cleanString(input);
        assertEquals(result, expected);
    }

    @Test
    public void listToCsvTags() {
        assertEquals("a,b,c", StringUtils.listToCsvTags(Arrays.asList("a","b", "c")));
        assertEquals("", StringUtils.listToCsvTags(new ArrayList<String>()));
    }

    @Test
    public void testToCSVList() {
        String input = "one,two,three,four,five,six,seven,eight,nine,ten";
        String output = StringUtils.toCSVList(Arrays.asList(input.split(",")));
        assertTrue(input.equals(output));
    }

    @Test
    public void testGetKeyValuePairWithSeparator() {
        String key = "ssh";
        String value = "ABCD==";
        String kp = String.format("%s=%s", key, value);
        Pair<String, String> output = StringUtils.getKeyValuePairWithSeparator(kp, "=");
        assertEquals(key, output.first());
        assertEquals(value, output.second());
    }
}
