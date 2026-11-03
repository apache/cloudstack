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

package org.apache.cloudstack.veeam.filter;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import javax.servlet.FilterChain;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.cloudstack.veeam.VeeamControlService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@RunWith(MockitoJUnitRunner.class)
public class AllowedClientCidrsFilterTest {

    @Mock
    private VeeamControlService veeamControlService;

    @Mock
    private FilterChain chain;

    @Test
    public void testDoFilterAllowsNonHttpRequestsToPassThrough() throws Exception {
        final AllowedClientCidrsFilter filter = new AllowedClientCidrsFilter(veeamControlService);
        final ServletRequest request = mock(ServletRequest.class);
        final ServletResponse response = mock(ServletResponse.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verifyNoInteractions(veeamControlService);
    }

    @Test
    public void testDoFilterRejectsWhenServiceIsUnavailable() throws Exception {
        final AllowedClientCidrsFilter filter = new AllowedClientCidrsFilter(null);
        final MockHttpServletRequest request = new MockHttpServletRequest();
        final MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        assertEquals(503, response.getStatus());
        assertEquals("Service Unavailable", response.getErrorMessage());
        verifyNoInteractions(chain);
    }

    @Test
    public void testDoFilterAllowsRequestWhenNoCidrsConfigured() throws Exception {
        final AllowedClientCidrsFilter filter = new AllowedClientCidrsFilter(veeamControlService);
        final MockHttpServletRequest request = new MockHttpServletRequest();
        final MockHttpServletResponse response = new MockHttpServletResponse();
        when(veeamControlService.getAllowedClientCidrs()).thenReturn(Collections.emptyList());

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    public void testDoFilterAllowsRequestFromConfiguredCidr() throws Exception {
        final AllowedClientCidrsFilter filter = new AllowedClientCidrsFilter(veeamControlService);
        final MockHttpServletRequest request = new MockHttpServletRequest();
        final MockHttpServletResponse response = new MockHttpServletResponse();
        request.setRemoteAddr("192.168.10.25");
        when(veeamControlService.getAllowedClientCidrs()).thenReturn(List.of("192.168.10.0/24"));

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testDoFilterRejectsRequestOutsideConfiguredCidrs() throws Exception {
        final AllowedClientCidrsFilter filter = new AllowedClientCidrsFilter(veeamControlService);
        final MockHttpServletRequest request = new MockHttpServletRequest();
        final MockHttpServletResponse response = new MockHttpServletResponse();
        request.setRemoteAddr("10.10.10.10");
        when(veeamControlService.getAllowedClientCidrs()).thenReturn(List.of("192.168.10.0/24"));

        filter.doFilter(request, response, chain);

        assertEquals(403, response.getStatus());
        assertEquals("Forbidden", response.getErrorMessage());
        verifyNoInteractions(chain);
    }

    @Test
    public void testDoFilterRejectsMalformedRemoteAddress() throws Exception {
        final AllowedClientCidrsFilter filter = new AllowedClientCidrsFilter(veeamControlService);
        final MockHttpServletRequest request = new MockHttpServletRequest();
        final MockHttpServletResponse response = new MockHttpServletResponse();
        request.setRemoteAddr("not-a-valid-ip-address");
        when(veeamControlService.getAllowedClientCidrs()).thenReturn(List.of("192.168.10.0/24"));

        filter.doFilter(request, response, chain);

        assertEquals(403, response.getStatus());
        assertEquals("Forbidden", response.getErrorMessage());
        verifyNoInteractions(chain);
    }
}
