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

package com.cloud.servlet;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class StaticResourceServletTest {

    File rootDirectory;

    @Before
    public void setupFiles() throws IOException {
        rootDirectory = new File("target/tmp");
        rootDirectory.mkdirs();
        final File webInf = new File(rootDirectory, "WEB-INF");
        webInf.mkdirs();
        final File dir = new File(rootDirectory, "dir");
        dir.mkdirs();
        final File indexHtml = new File(rootDirectory, "index.html");
        indexHtml.createNewFile();
        FileUtils.writeStringToFile(indexHtml, "index.html");
        final File defaultCss = new File(rootDirectory, "default.css");
        defaultCss.createNewFile();
        final File defaultCssGziped = new File(rootDirectory, "default.css.gz");
        defaultCssGziped.createNewFile();
    }

    @After
    public void cleanupFiles() {
        FileUtils.deleteQuietly(rootDirectory);
    }

    // negative tests

    @Test
    public void testNoSuchFile() throws ServletException, IOException {
        final StaticResourceServlet servlet = Mockito
                .mock(StaticResourceServlet.class);
        Mockito.doCallRealMethod()
                .when(servlet)
                .doGet(Matchers.any(HttpServletRequest.class),
                        Matchers.any(HttpServletResponse.class));
        final ServletContext servletContext = Mockito
                .mock(ServletContext.class);
        Mockito.when(servletContext.getRealPath("notexisting.css")).thenReturn(
                new File(rootDirectory, "notexisting.css").getAbsolutePath());
        Mockito.when(servlet.getServletContext()).thenReturn(servletContext);

        final HttpServletRequest request = Mockito
                .mock(HttpServletRequest.class);
        Mockito.when(request.getServletPath()).thenReturn("notexisting.css");
        final HttpServletResponse response = Mockito
                .mock(HttpServletResponse.class);
        servlet.doGet(request, response);
        Mockito.verify(response).setStatus(HttpServletResponse.SC_NOT_FOUND);
    }

    @Test
    public void testDirectory() throws ServletException, IOException {
        final HttpServletResponse response = doGetTest("dir");
        Mockito.verify(response).setStatus(HttpServletResponse.SC_NOT_FOUND);
    }

    @Test
    public void testWebInf() throws ServletException, IOException {
        final HttpServletResponse response = doGetTest("WEB-INF/web.xml");
        Mockito.verify(response).setStatus(HttpServletResponse.SC_NOT_FOUND);
    }

    // positive tests

    @Test
    public void testNotCompressedFile() throws ServletException, IOException {
        final HttpServletResponse response = doGetTest("index.html");
        Mockito.verify(response).setStatus(HttpServletResponse.SC_OK);
        Mockito.verify(response).setContentType("text/html");
        Mockito.verify(response, Mockito.times(0)).setHeader(
                "Content-Encoding", "gzip");
    }

    @Test
    public void testCompressedFile() throws ServletException, IOException {
        final HashMap<String, String> headers = new HashMap<String, String>();
        headers.put("Accept-Encoding", "gzip");
        final HttpServletResponse response = doGetTest("default.css", headers);
        Mockito.verify(response).setStatus(HttpServletResponse.SC_OK);
        Mockito.verify(response).setContentType("text/css");
        Mockito.verify(response, Mockito.times(1)).setHeader(
                "Content-Encoding", "gzip");
    }

    @Test
    public void testCompressedFileWithoutBrowserSupport()
            throws ServletException, IOException {
        final HashMap<String, String> headers = new HashMap<String, String>();
        headers.put("Accept-Encoding", "");
        final HttpServletResponse response = doGetTest("default.css", headers);
        Mockito.verify(response).setStatus(HttpServletResponse.SC_OK);
        Mockito.verify(response).setContentType("text/css");
        Mockito.verify(response, Mockito.times(0)).setHeader(
                "Content-Encoding", "gzip");
    }

    @Test
    public void testWithEtag() throws ServletException, IOException {
        final HashMap<String, String> headers = new HashMap<String, String>();
        headers.put("If-None-Match", StaticResourceServlet.getEtag(new File(
                rootDirectory, "default.css")));
        final HttpServletResponse response = doGetTest("default.css", headers);
        Mockito.verify(response).setStatus(HttpServletResponse.SC_NOT_MODIFIED);
    }

    @Test
    public void testWithEtagOutdated() throws ServletException, IOException {
        final HashMap<String, String> headers = new HashMap<String, String>();
        headers.put("If-None-Match", "NO-GOOD-ETAG");
        final HttpServletResponse response = doGetTest("default.css", headers);
        Mockito.verify(response).setStatus(HttpServletResponse.SC_OK);
    }

    // utility methods

    @Test
    public void getEtag() {
        Assert.assertNotNull(StaticResourceServlet.getEtag(new File(
                rootDirectory, "index.html")));
    }

    @Test
    public void getContentType() {
        Assert.assertEquals("text/plain",
                StaticResourceServlet.getContentType("foo.txt"));
        Assert.assertEquals("text/html",
                StaticResourceServlet.getContentType("index.html"));
        Assert.assertEquals("text/plain",
                StaticResourceServlet.getContentType("README.TXT"));
    }

    @Test
    public void isClientCompressionSupported() {
        final HttpServletRequest request = Mockito
                .mock(HttpServletRequest.class);
        Mockito.when(request.getHeader("Accept-Encoding")).thenReturn(
                "gzip, deflate");
        Assert.assertTrue(StaticResourceServlet
                .isClientCompressionSupported(request));
    }

    @Test
    public void isClientCompressionSupportedWithoutHeader() {
        final HttpServletRequest request = Mockito
                .mock(HttpServletRequest.class);
        Mockito.when(request.getHeader("Accept-Encoding")).thenReturn(null);
        Assert.assertFalse(StaticResourceServlet
                .isClientCompressionSupported(request));
    }

    // test utilities
    private HttpServletResponse doGetTest(final String uri)
            throws ServletException, IOException {
        return doGetTest(uri, Collections.<String, String> emptyMap());
    }

    private HttpServletResponse doGetTest(final String uri,
            final Map<String, String> headers) throws ServletException,
            IOException {
        final StaticResourceServlet servlet = Mockito
                .mock(StaticResourceServlet.class);
        Mockito.doCallRealMethod()
                .when(servlet)
                .doGet(Matchers.any(HttpServletRequest.class),
                        Matchers.any(HttpServletResponse.class));
        final ServletContext servletContext = Mockito
                .mock(ServletContext.class);
        Mockito.when(servletContext.getRealPath(uri)).thenReturn(
                new File(rootDirectory, uri).getAbsolutePath());
        Mockito.when(servlet.getServletContext()).thenReturn(servletContext);

        final HttpServletRequest request = Mockito
                .mock(HttpServletRequest.class);
        Mockito.when(request.getServletPath()).thenReturn(uri);
        Mockito.when(request.getHeader(Matchers.anyString())).thenAnswer(
                new Answer<String>() {

                    @Override
                    public String answer(final InvocationOnMock invocation)
                            throws Throwable {
                        return headers.get(invocation.getArguments()[0]);
                    }
                });
        final HttpServletResponse response = Mockito
                .mock(HttpServletResponse.class);
        final ServletOutputStream responseBody = Mockito
                .mock(ServletOutputStream.class);
        Mockito.when(response.getOutputStream()).thenReturn(responseBody);
        servlet.doGet(request, response);
        return response;
    }

}
