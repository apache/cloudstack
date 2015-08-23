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
import org.springframework.mock.web.MockHttpSession;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class HttpUtilsTest {

    @Test
    public void findCookieTest() {
        Cookie[] cookies = null;
        String cookieName = null;

        // null test
        assertNull(HttpUtils.findCookie(cookies, cookieName));
        cookieName = "";
        assertNull(HttpUtils.findCookie(cookies, cookieName));

        // value test
        cookieName = "daakuBandar";
        cookies = new Cookie[]{new Cookie(cookieName, "someValue")};
        assertNull(HttpUtils.findCookie(cookies, "aalasiLangur"));
        assertNotNull(HttpUtils.findCookie(cookies, cookieName));
    }

    @Test
    public void validateSessionKeyTest() {
        HttpSession session = null;
        Map<String, Object[]> params = null;
        String sessionKeyString = null;
        Cookie[] cookies = null;
        final String sessionKeyValue = "randomUniqueSessionID";

        // session and sessionKeyString null test
        assertFalse(HttpUtils.validateSessionKey(session, params, cookies, sessionKeyString));
        sessionKeyString =  "sessionkey";
        assertFalse(HttpUtils.validateSessionKey(session, params, cookies, sessionKeyString));

        // param and cookie null test
        session = new MockHttpSession();
        session.setAttribute(sessionKeyString, sessionKeyValue);
        assertFalse(HttpUtils.validateSessionKey(session, params, cookies, sessionKeyString));

        // param null, cookies not null test
        params = null;
        cookies = new Cookie[]{new Cookie(sessionKeyString, sessionKeyValue)};
        assertFalse(HttpUtils.validateSessionKey(session, params, cookies, "randomString"));
        assertTrue(HttpUtils.validateSessionKey(session, params, cookies, sessionKeyString));

        // param not null, cookies null test
        params = new HashMap<String, Object[]>();
        params.put(sessionKeyString, new String[]{"randomString"});
        cookies = null;
        assertFalse(HttpUtils.validateSessionKey(session, params, cookies, sessionKeyString));
        params.put(sessionKeyString, new String[]{sessionKeyValue});
        assertTrue(HttpUtils.validateSessionKey(session, params, cookies, sessionKeyString));

        // both param and cookies not null test
        params = new HashMap<String, Object[]>();
        cookies = new Cookie[]{new Cookie(sessionKeyString, sessionKeyValue)};
        params.put(sessionKeyString, new String[]{"incorrectValue"});
        assertFalse(HttpUtils.validateSessionKey(session, params, cookies, sessionKeyString));
        params.put(sessionKeyString, new String[]{sessionKeyValue});
        assertTrue(HttpUtils.validateSessionKey(session, params, cookies, sessionKeyString));
    }
}
