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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;

import org.apache.logging.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.mock.web.MockHttpSession;

@RunWith(MockitoJUnitRunner.class)
public class HttpUtilsTest {

    // Use a custom protocol (e.g., "mockhttp") for testing.
    private static FakeURLStreamHandler fakeHandler;

    @Mock
    private Logger logger;

    @Mock
    private HttpURLConnection httpConn;

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
        assertFalse(HttpUtils.validateSessionKey(session, params, cookies, sessionKeyString, HttpUtils.ApiSessionKeyCheckOption.CookieOrParameter));
        sessionKeyString =  "sessionkey";
        assertFalse(HttpUtils.validateSessionKey(session, params, cookies, sessionKeyString, HttpUtils.ApiSessionKeyCheckOption.CookieOrParameter));

        // param and cookie null test
        session = new MockHttpSession();
        final String sessionId = session.getId();
        session.setAttribute(sessionKeyString, sessionKeyValue);
        assertFalse(HttpUtils.validateSessionKey(session, params, cookies, sessionKeyString, HttpUtils.ApiSessionKeyCheckOption.CookieOrParameter));

        // param null, cookies not null test (JSESSIONID is null)
        params = null;
        cookies = new Cookie[]{new Cookie(sessionKeyString, sessionKeyValue)};
        assertFalse(HttpUtils.validateSessionKey(session, params, cookies, "randomString", HttpUtils.ApiSessionKeyCheckOption.CookieOrParameter));
        assertTrue(HttpUtils.validateSessionKey(session, params, cookies, sessionKeyString, HttpUtils.ApiSessionKeyCheckOption.CookieOrParameter));

        // param null, cookies not null test (JSESSIONID is not null and matches)
        cookies = new Cookie[2];
        cookies[0] = new Cookie(sessionKeyString, sessionKeyValue);
        cookies[1] = new Cookie("JSESSIONID", sessionId + ".node0");
        assertFalse(HttpUtils.validateSessionKey(session, params, cookies, "randomString", HttpUtils.ApiSessionKeyCheckOption.CookieOrParameter));

        // param null, cookies not null test (JSESSIONID is not null but mismatches)
        cookies = new Cookie[2];
        cookies[0] = new Cookie(sessionKeyString, sessionKeyValue);
        cookies[1] = new Cookie("JSESSIONID", "node0xxxxxxxxxxxxx.node0");
        assertFalse(HttpUtils.validateSessionKey(session, params, cookies, "randomString", HttpUtils.ApiSessionKeyCheckOption.CookieOrParameter));
        assertFalse(HttpUtils.validateSessionKey(session, params, cookies, sessionKeyString, HttpUtils.ApiSessionKeyCheckOption.CookieOrParameter));

        // param not null, cookies null test
        params = new HashMap<String, Object[]>();
        params.put(sessionKeyString, new String[]{"randomString"});
        cookies = null;
        assertFalse(HttpUtils.validateSessionKey(session, params, cookies, sessionKeyString, HttpUtils.ApiSessionKeyCheckOption.CookieOrParameter));
        params.put(sessionKeyString, new String[]{sessionKeyValue});
        assertTrue(HttpUtils.validateSessionKey(session, params, cookies, sessionKeyString, HttpUtils.ApiSessionKeyCheckOption.CookieOrParameter));

        // both param and cookies not null test (JSESSIONID is null)
        params = new HashMap<String, Object[]>();
        cookies = new Cookie[2];
        cookies[0] = new Cookie(sessionKeyString, sessionKeyValue);
        params.put(sessionKeyString, new String[]{"incorrectValue"});
        assertFalse(HttpUtils.validateSessionKey(session, params, cookies, sessionKeyString, HttpUtils.ApiSessionKeyCheckOption.CookieOrParameter));
        params.put(sessionKeyString, new String[]{sessionKeyValue});
        assertTrue(HttpUtils.validateSessionKey(session, params, cookies, sessionKeyString, HttpUtils.ApiSessionKeyCheckOption.CookieOrParameter));

        // both param and cookies not null test (JSESSIONID is not null but mismatches)
        params = new HashMap<String, Object[]>();
        cookies = new Cookie[2];
        cookies[0] = new Cookie(sessionKeyString, sessionKeyValue);
        cookies[1] = new Cookie("JSESSIONID", "node0xxxxxxxxxxxxx.node0");
        params.put(sessionKeyString, new String[]{"incorrectValue"});
        assertFalse(HttpUtils.validateSessionKey(session, params, cookies, sessionKeyString, HttpUtils.ApiSessionKeyCheckOption.CookieOrParameter));
        params.put(sessionKeyString, new String[]{sessionKeyValue});
        assertFalse(HttpUtils.validateSessionKey(session, params, cookies, sessionKeyString, HttpUtils.ApiSessionKeyCheckOption.CookieOrParameter));

        // both param and cookies not null test (JSESSIONID is not null amd matches)
        params = new HashMap<String, Object[]>();
        cookies = new Cookie[2];
        cookies[0] = new Cookie(sessionKeyString, sessionKeyValue);
        cookies[1] = new Cookie("JSESSIONID", sessionId + ".node0");
        params.put(sessionKeyString, new String[]{"incorrectValue"});
        assertFalse(HttpUtils.validateSessionKey(session, params, cookies, sessionKeyString, HttpUtils.ApiSessionKeyCheckOption.CookieOrParameter));
        params.put(sessionKeyString, new String[]{sessionKeyValue});
        assertTrue(HttpUtils.validateSessionKey(session, params, cookies, sessionKeyString, HttpUtils.ApiSessionKeyCheckOption.CookieOrParameter));

        // param not null, cookies null test (JSESSIONID is not null amd matches)
        params = new HashMap<String, Object[]>();
        cookies = new Cookie[1];
        cookies[0] = new Cookie("JSESSIONID", sessionId + ".node0");
        params.put(sessionKeyString, new String[]{"incorrectValue"});
        assertFalse(HttpUtils.validateSessionKey(session, params, cookies, sessionKeyString, HttpUtils.ApiSessionKeyCheckOption.ParameterOnly));
        assertFalse(HttpUtils.validateSessionKey(session, params, cookies, sessionKeyString, HttpUtils.ApiSessionKeyCheckOption.CookieAndParameter));
        params.put(sessionKeyString, new String[]{sessionKeyValue});
        assertTrue(HttpUtils.validateSessionKey(session, params, cookies, sessionKeyString, HttpUtils.ApiSessionKeyCheckOption.ParameterOnly));
        assertFalse(HttpUtils.validateSessionKey(session, params, cookies, sessionKeyString, HttpUtils.ApiSessionKeyCheckOption.CookieAndParameter));

        // param not null (correct), cookies not null test (correct)
        cookies = new Cookie[2];
        cookies[0] = new Cookie(sessionKeyString, sessionKeyValue);
        cookies[1] = new Cookie("JSESSIONID", sessionId + ".node0");
        assertTrue(HttpUtils.validateSessionKey(session, params, cookies, sessionKeyString, HttpUtils.ApiSessionKeyCheckOption.CookieOrParameter));
        assertTrue(HttpUtils.validateSessionKey(session, params, cookies, sessionKeyString, HttpUtils.ApiSessionKeyCheckOption.ParameterOnly));
        assertTrue(HttpUtils.validateSessionKey(session, params, cookies, sessionKeyString, HttpUtils.ApiSessionKeyCheckOption.CookieAndParameter));

        // param not null (correct), cookies not null test (wrong)
        cookies = new Cookie[2];
        cookies[0] = new Cookie(sessionKeyString, "incorrectValue");
        cookies[1] = new Cookie("JSESSIONID", sessionId + ".node0");
        assertFalse(HttpUtils.validateSessionKey(session, params, cookies, sessionKeyString, HttpUtils.ApiSessionKeyCheckOption.CookieOrParameter));
        assertFalse(HttpUtils.validateSessionKey(session, params, cookies, sessionKeyString, HttpUtils.ApiSessionKeyCheckOption.ParameterOnly));
        assertFalse(HttpUtils.validateSessionKey(session, params, cookies, sessionKeyString, HttpUtils.ApiSessionKeyCheckOption.CookieAndParameter));
    }

    private static class FakeURLStreamHandler extends URLStreamHandler {
        private HttpURLConnection connection;

        public void setHttpURLConnection(HttpURLConnection connection) {
            this.connection = connection;
        }

        @Override
        protected URLConnection openConnection(URL u) {
            return connection;
        }
    }

    // Register our custom URLStreamHandlerFactory once for the tests.
    @BeforeClass
    public static void setUpOnce() {
        fakeHandler = new FakeURLStreamHandler();
        try {
            URL.setURLStreamHandlerFactory(protocol -> {
                if ("mockhttp".equals(protocol)) {
                    return fakeHandler;
                }
                return null;
            });
        } catch (Error e) {
            // The factory can only be set once. In case it is already set, ignore.
        }
    }

    @Test
    public void testSuccessfulDownload_withContentLength() throws Exception {
        String fileURL = "mockhttp://example.com/file.txt";
        File tempFile = File.createTempFile("downloadTest", ".tmp");
        tempFile.deleteOnExit();
        byte[] fileData = "Hello World".getBytes();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(fileData);
        when(httpConn.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(httpConn.getContentLength()).thenReturn(fileData.length);
        when(httpConn.getInputStream()).thenReturn(inputStream);
        fakeHandler.setHttpURLConnection(httpConn);
        boolean result = HttpUtils.downloadFileWithProgress(fileURL, tempFile.getAbsolutePath(), logger);
        assertTrue(result);
        verify(logger, atLeastOnce()).debug(anyString(), anyInt(), eq(fileURL));
        verify(logger).info("File {} downloaded successfully using {}.", fileURL, tempFile.getAbsolutePath());
        byte[] actualData = Files.readAllBytes(tempFile.toPath());
        assertArrayEquals(fileData, actualData);
    }

    @Test
    public void testSuccessfulDownload_negativeContentLength() throws Exception {
        String fileURL = "mockhttp://example.com/file.txt";
        File tempFile = File.createTempFile("downloadTest", ".tmp");
        tempFile.deleteOnExit();
        byte[] fileData = "Hello World".getBytes();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(fileData);
        when(httpConn.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
        // Simulate missing content length
        when(httpConn.getContentLength()).thenReturn(-1);
        when(httpConn.getInputStream()).thenReturn(inputStream);
        fakeHandler.setHttpURLConnection(httpConn);
        boolean result = HttpUtils.downloadFileWithProgress(fileURL, tempFile.getAbsolutePath(), logger);
        assertTrue(result);
        verify(logger).warn("Content length not provided for {}, progress updates may not be accurate", fileURL);
        verify(logger).info("File {} downloaded successfully using {}.", fileURL, tempFile.getAbsolutePath());
        byte[] actualData = Files.readAllBytes(tempFile.toPath());
        assertArrayEquals(fileData, actualData);
    }

    @Test
    public void testDownloadFile_nonOKResponse() throws Exception {
        String fileURL = "mockhttp://example.com/file.txt";
        String savePath = "dummyPath";
        when(httpConn.getResponseCode()).thenReturn(HttpURLConnection.HTTP_NOT_FOUND);
        fakeHandler.setHttpURLConnection(httpConn);
        boolean result = HttpUtils.downloadFileWithProgress(fileURL, savePath, logger);
        assertFalse(result);
        verify(logger).error("No file to download {}. Server replied with code: {}", fileURL, HttpURLConnection.HTTP_NOT_FOUND);
    }

    @Test
    public void testDownloadFile_exceptionDuringDownload() throws Exception {
        String fileURL = "mockhttp://example.com/file.txt";
        String savePath = "dummyPath";
        when(httpConn.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(httpConn.getContentLength()).thenReturn(100);
        // Simulate an IOException when trying to get the InputStream
        when(httpConn.getInputStream()).thenThrow(new IOException("Connection error"));
        fakeHandler.setHttpURLConnection(httpConn);
        boolean result = HttpUtils.downloadFileWithProgress(fileURL, savePath, logger);
        assertFalse(result);
        verify(logger).error(contains("Failed to download {} due to: {}"), eq(fileURL), eq("Connection error"), any(IOException.class));
    }
}
