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

package org.apache.cloudstack;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.cloudstack.utils.security.HMACSignUtil;
import org.junit.Test;

public class ShareSignedUrlFilterTest {

    @Test
    public void deniesRequestWhenExpParameterIsWithinDeltaButInvalid() throws Exception {
        ShareSignedUrlFilter filter = new ShareSignedUrlFilter("secret");
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        HttpServletResponse mockResponse = mock(HttpServletResponse.class);
        FilterChain mockChain = mock(FilterChain.class);

        when(mockRequest.getParameter("exp")).thenReturn("invalid");
        when(mockRequest.getParameter("sig")).thenReturn("signature");

        filter.doFilter(mockRequest, mockResponse, mockChain);

        verify(mockResponse).sendError(HttpServletResponse.SC_FORBIDDEN, "Bad exp");
        verifyNoInteractions(mockChain);
    }

    @Test
    public void allowsRequestWhenExpParameterIsValidAndWithinDelta() throws Exception {
        String secret = "secret";
        ShareSignedUrlFilter filter = new ShareSignedUrlFilter(secret);
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        HttpServletResponse mockResponse = mock(HttpServletResponse.class);
        FilterChain mockChain = mock(FilterChain.class);

        String exp = String.valueOf(Instant.now().getEpochSecond() + 50); // Within delta
        String data = "/share/resource|" + exp;
        String validSignature = HMACSignUtil.generateSignature(data, secret);

        when(mockRequest.getParameter("exp")).thenReturn(exp);
        when(mockRequest.getParameter("sig")).thenReturn(validSignature);
        when(mockRequest.getRequestURI()).thenReturn("/share/resource");

        filter.doFilter(mockRequest, mockResponse, mockChain);

        verify(mockChain).doFilter(mockRequest, mockResponse);
    }

    @Test
    public void deniesRequestWhenExpParameterIsValidButOutsideDelta() throws Exception {
        ShareSignedUrlFilter filter = new ShareSignedUrlFilter("secret");
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        HttpServletResponse mockResponse = mock(HttpServletResponse.class);
        FilterChain mockChain = mock(FilterChain.class);

        String exp = String.valueOf(Instant.now().getEpochSecond() - 200); // Outside delta
        when(mockRequest.getParameter("exp")).thenReturn(exp);
        when(mockRequest.getParameter("sig")).thenReturn("signature");

        filter.doFilter(mockRequest, mockResponse, mockChain);

        verify(mockResponse).sendError(HttpServletResponse.SC_FORBIDDEN, "Token expired");
        verifyNoInteractions(mockChain);
    }

    @Test
    public void deniesRequestWhenSignatureIsValidButExpParameterIsMissing() throws Exception {
        ShareSignedUrlFilter filter = new ShareSignedUrlFilter("secret");
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        HttpServletResponse mockResponse = mock(HttpServletResponse.class);
        FilterChain mockChain = mock(FilterChain.class);

        when(mockRequest.getParameter("exp")).thenReturn(null);
        when(mockRequest.getParameter("sig")).thenReturn("validSignature");

        filter.doFilter(mockRequest, mockResponse, mockChain);

        verify(mockResponse).sendError(HttpServletResponse.SC_FORBIDDEN, "Missing token");
        verifyNoInteractions(mockChain);
    }
}
