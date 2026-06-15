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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.servlet.http.HttpServletRequest;

import org.junit.Test;

public class NegotiationTest {

    @Test
    public void testResponseFormat_DefaultsToXmlForNullBlankWildcardAndUnknown() {
        final HttpServletRequest request = mock(HttpServletRequest.class);

        when(request.getHeader("Accept")).thenReturn(null);
        assertEquals(Negotiation.OutFormat.XML, Negotiation.responseFormat(request));

        when(request.getHeader("Accept")).thenReturn("   ");
        assertEquals(Negotiation.OutFormat.XML, Negotiation.responseFormat(request));

        when(request.getHeader("Accept")).thenReturn("*/*");
        assertEquals(Negotiation.OutFormat.XML, Negotiation.responseFormat(request));

        when(request.getHeader("Accept")).thenReturn("application/octet-stream");
        assertEquals(Negotiation.OutFormat.XML, Negotiation.responseFormat(request));
    }

    @Test
    public void testResponseFormat_ResolvesJsonAndXmlMediaTypesCaseInsensitively() {
        final HttpServletRequest request = mock(HttpServletRequest.class);

        when(request.getHeader("Accept")).thenReturn("Application/JSON");
        assertEquals(Negotiation.OutFormat.JSON, Negotiation.responseFormat(request));

        when(request.getHeader("Accept")).thenReturn("application/xml");
        assertEquals(Negotiation.OutFormat.XML, Negotiation.responseFormat(request));

        when(request.getHeader("Accept")).thenReturn("text/xml");
        assertEquals(Negotiation.OutFormat.XML, Negotiation.responseFormat(request));
    }

    @Test
    public void testContentType_ReturnsMimeTypeForEachFormat() {
        assertEquals("application/json", Negotiation.contentType(Negotiation.OutFormat.JSON));
        assertEquals("application/xml", Negotiation.contentType(Negotiation.OutFormat.XML));
    }
}
