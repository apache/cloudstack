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

package org.apache.cloudstack.servlet;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class ShareServletTest {

    @Test
    public void doGetReturnsServiceUnavailableWhenShareFeatureIsDisabled() throws IOException {
        ShareServlet servlet = new ShareServlet();
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        HttpServletResponse mockResponse = mock(HttpServletResponse.class);

        servlet.doGet(mockRequest, mockResponse);

        verify(mockResponse).sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Share feature disabled");
    }

    @Test
    public void doGetReturnsBadRequestWhenPathInfoIsInvalid() throws IOException {
        ShareServlet servlet = new ShareServlet();
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        HttpServletResponse mockResponse = mock(HttpServletResponse.class);

        ReflectionTestUtils.setField(servlet, "baseDir", Paths.get("/base/dir"));

        when(mockRequest.getPathInfo()).thenReturn(null);

        servlet.doGet(mockRequest, mockResponse);

        verify(mockResponse).sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid path");
    }

    @Test
    public void doGetReturnsForbiddenWhenPathIsOutsideBaseDir() throws IOException {
        ShareServlet servlet = new ShareServlet();
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        HttpServletResponse mockResponse = mock(HttpServletResponse.class);

        ReflectionTestUtils.setField(servlet, "baseDir", Paths.get("/base/dir"));

        when(mockRequest.getPathInfo()).thenReturn("/../../outside");

        servlet.doGet(mockRequest, mockResponse);

        verify(mockResponse).sendError(HttpServletResponse.SC_FORBIDDEN);
    }

    @Test
    public void doGetReturnsNotFoundWhenFileDoesNotExist() throws IOException {
        ShareServlet servlet = new ShareServlet();
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        HttpServletResponse mockResponse = mock(HttpServletResponse.class);

        ReflectionTestUtils.setField(servlet, "baseDir", Paths.get("/base/dir"));

        when(mockRequest.getPathInfo()).thenReturn("/nonexistent/file");

        servlet.doGet(mockRequest, mockResponse);

        verify(mockResponse).sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    @Test
    public void doGetServesFileSuccessfully() throws IOException {
        ShareServlet servlet = new ShareServlet();
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        HttpServletResponse mockResponse = mock(HttpServletResponse.class);
        ServletOutputStream mockOutputStream = mock(ServletOutputStream.class);

        Path baseDir = Files.createTempDirectory("shareServletTest");
        ReflectionTestUtils.setField(servlet, "baseDir", baseDir);
        Path file = Files.createTempFile(baseDir, "testFile", ".txt");
        Files.writeString(file, "test content");

        when(mockRequest.getPathInfo()).thenReturn(file.getFileName().toString());
        when(mockResponse.getOutputStream()).thenReturn(mockOutputStream);

        servlet.doGet(mockRequest, mockResponse);

        verify(mockResponse).setHeader("Accept-Ranges", "bytes");
        verify(mockResponse).setContentType(Files.probeContentType(file));
        final BasicFileAttributes attrs = Files.readAttributes(file, BasicFileAttributes.class);
        verify(mockResponse).setContentLengthLong(attrs.size());
    }
}
