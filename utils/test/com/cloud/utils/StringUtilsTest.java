// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// the License.  You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class StringUtilsTest {
    @Test
    public void testCleanPasswordFromJsonObjectAtEnd() {
        String input = "{\"foo\":\"bar\",\"password\":\"test\"}";
        //TODO: It would be nice to clean up the regex in question to not
        //have to return the trailing comma in the expected string below
        String expected = "{\"foo\":\"bar\",}";
        String result = StringUtils.cleanString(input);
        assertEquals(result, expected);
    }

    @Test
    public void testCleanPasswordFromJsonObjectInMiddle() {
        String input = "{\"foo\":\"bar\",\"password\":\"test\",\"test\":\"blah\"}";
        String expected = "{\"foo\":\"bar\",\"test\":\"blah\"}";
        String result = StringUtils.cleanString(input);
        assertEquals(result, expected);
    }

    @Test
    public void testCleanPasswordFromJsonObjectAlone() {
        String input = "{\"password\":\"test\"}";
        String expected = "{}";
        String result = StringUtils.cleanString(input);
        assertEquals(result, expected);
    }

    @Test
    public void testCleanPasswordFromJsonObjectAtStart() {
        String input = "{\"password\":\"test\",\"test\":\"blah\"}";
        String expected = "{\"test\":\"blah\"}";
        String result = StringUtils.cleanString(input);
        assertEquals(result, expected);
    }

    @Test
    public void testCleanPasswordFromJsonObjectWithMultiplePasswords() {
        String input = "{\"description\":\"foo\"}],\"password\":\"bar\",\"nic\":[{\"password\":\"bar2\",\"id\":\"1\"}]}";
        String expected = "{\"description\":\"foo\"}],\"nic\":[{\"id\":\"1\"}]}";
        String result = StringUtils.cleanString(input);
        assertEquals(result, expected);
    }

    @Test
    public void testCleanPasswordFromRequestString() {
        String input = "username=foo&password=bar&url=foobar";
        String expected = "username=foo&url=foobar";
        String result = StringUtils.cleanString(input);
        assertEquals(result, expected);
    }

    @Test
    public void testCleanPasswordFromEncodedRequestString() {
        String input = "name=SS1&provider=SMB&zoneid=5a60af2b-3025-4f2a-9ecc-8e33bf2b94e3&url=cifs%3A%2F%2F10.102.192.150%2FSMB-Share%2Fsowmya%2Fsecondary%3Fuser%3Dsowmya%26password%3DXXXXX%40123%26domain%3DBLR";
        String expected = "name=SS1&provider=SMB&zoneid=5a60af2b-3025-4f2a-9ecc-8e33bf2b94e3&url=cifs%3A%2F%2F10.102.192.150%2FSMB-Share%2Fsowmya%2Fsecondary%3Fuser%3Dsowmya%26domain%3DBLR";
        String result = StringUtils.cleanString(input);
        assertEquals(result, expected);
    }

    @Test
    public void testCleanPasswordFromRequestStringWithMultiplePasswords() {
        String input = "username=foo&password=bar&url=foobar&password=bar2&test=4";
        String expected = "username=foo&url=foobar&test=4";
        String result = StringUtils.cleanString(input);
        assertEquals(result, expected);
    }

    @Test
    public void testCleanPasswordFromRequestStringMatchedAtEndSingleQuote() {
        String input = "'username=foo&password=bar'";
        String expected = "'username=foo'";
        String result = StringUtils.cleanString(input);
        assertEquals(result, expected);
    }

    @Test
    public void testCleanPasswordFromRequestStringMatchedAtEndDoubleQuote() {
        String input = "\"username=foo&password=bar\"";
        String expected = "\"username=foo\"";
        String result = StringUtils.cleanString(input);
        assertEquals(result, expected);
    }

    @Test
    public void testCleanPasswordFromRequestStringMatchedAtMiddleDoubleQuote() {
        String input = "\"username=foo&password=bar&goo=sdf\"";
        String expected = "\"username=foo&goo=sdf\"";
        String result = StringUtils.cleanString(input);
        assertEquals(result, expected);
    }

    @Test
    public void testJoin() {
        assertEquals("a-b-c", StringUtils.join("-", "a", "b", "c"));
        assertEquals("", StringUtils.join("-"));
    }

    @Test
    public void testCleanSecretkeyFromJsonObjectAtEnd() {
        String input = "{\"foo\":\"bar\",\"secretkey\":\"test\"}";
        // TODO: It would be nice to clean up the regex in question to not
        // have to return the trailing comma in the expected string below
        String expected = "{\"foo\":\"bar\",}";
        String result = StringUtils.cleanString(input);
        assertEquals(result, expected);
    }

    @Test
    public void testCleanSecretkeyFromJsonObjectInMiddle() {
        String input = "{\"foo\":\"bar\",\"secretkey\":\"test\",\"test\":\"blah\"}";
        String expected = "{\"foo\":\"bar\",\"test\":\"blah\"}";
        String result = StringUtils.cleanString(input);
        assertEquals(result, expected);
    }

    @Test
    public void testCleanSecretkeyFromJsonObjectAlone() {
        String input = "{\"secretkey\":\"test\"}";
        String expected = "{}";
        String result = StringUtils.cleanString(input);
        assertEquals(result, expected);
    }

    @Test
    public void testCleanSecretkeyFromJsonObjectAtStart() {
        String input = "{\"secretkey\":\"test\",\"test\":\"blah\"}";
        String expected = "{\"test\":\"blah\"}";
        String result = StringUtils.cleanString(input);
        assertEquals(result, expected);
    }

    @Test
    public void testCleanSecretkeyFromJsonObjectWithMultiplePasswords() {
        String input = "{\"description\":\"foo\"}],\"secretkey\":\"bar\",\"nic\":[{\"secretkey\":\"bar2\",\"id\":\"1\"}]}";
        String expected = "{\"description\":\"foo\"}],\"nic\":[{\"id\":\"1\"}]}";
        String result = StringUtils.cleanString(input);
        assertEquals(result, expected);
    }

    @Test
    public void testCleanAccesskeyFromJsonObjectAtEnd() {
        String input = "{\"foo\":\"bar\",\"accesskey\":\"test\"}";
        // TODO: It would be nice to clean up the regex in question to not
        // have to return the trailing comma in the expected string below
        String expected = "{\"foo\":\"bar\",}";
        String result = StringUtils.cleanString(input);
        assertEquals(result, expected);
    }

    @Test
    public void testCleanAccesskeyFromJsonObjectInMiddle() {
        String input = "{\"foo\":\"bar\",\"accesskey\":\"test\",\"test\":\"blah\"}";
        String expected = "{\"foo\":\"bar\",\"test\":\"blah\"}";
        String result = StringUtils.cleanString(input);
        assertEquals(result, expected);
    }

    @Test
    public void testCleanAccesskeyFromJsonObjectAlone() {
        String input = "{\"accesskey\":\"test\"}";
        String expected = "{}";
        String result = StringUtils.cleanString(input);
        assertEquals(result, expected);
    }

    @Test
    public void testCleanAccesskeyFromJsonObjectAtStart() {
        String input = "{\"accesskey\":\"test\",\"test\":\"blah\"}";
        String expected = "{\"test\":\"blah\"}";
        String result = StringUtils.cleanString(input);
        assertEquals(result, expected);
    }

    @Test
    public void testCleanAccesskeyFromJsonObjectWithMultiplePasswords() {
        String input = "{\"description\":\"foo\"}],\"accesskey\":\"bar\",\"nic\":[{\"accesskey\":\"bar2\",\"id\":\"1\"}]}";
        String expected = "{\"description\":\"foo\"}],\"nic\":[{\"id\":\"1\"}]}";
        String result = StringUtils.cleanString(input);
        assertEquals(result, expected);
    }

    @Test
    public void testCleanAccesskeyFromRequestString() {
        String input = "username=foo&accesskey=bar&url=foobar";
        String expected = "username=foo&url=foobar";
        String result = StringUtils.cleanString(input);
        assertEquals(result, expected);
    }

    @Test
    public void testCleanSecretkeyFromRequestString() {
        String input = "username=foo&secretkey=bar&url=foobar";
        String expected = "username=foo&url=foobar";
        String result = StringUtils.cleanString(input);
        assertEquals(result, expected);
    }
}
