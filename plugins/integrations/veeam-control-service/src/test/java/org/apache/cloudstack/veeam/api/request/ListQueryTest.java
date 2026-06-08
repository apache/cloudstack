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

package org.apache.cloudstack.veeam.api.request;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.junit.Test;

public class ListQueryTest {

    @Test
    public void testFromRequest_WithNoParametersReturnsDefaults() {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameterMap()).thenReturn(Collections.emptyMap());

        final ListQuery query = ListQuery.fromRequest(request);

        assertFalse(query.isAllContent());
        assertNull(query.getLimit());
        assertNull(query.getOffset());
        assertFalse(query.followContains("tags"));
    }

    @Test
    public void testFromRequest_ParsesAllContentMaxAndFollow() {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameterMap()).thenReturn(Map.of(
                "all_content", new String[]{"true"},
                "max", new String[]{"25"},
                "follow", new String[]{"tags, disk_attachments.disk, ,nics.reporteddevices"}
        ));
        when(request.getParameter("all_content")).thenReturn("true");
        when(request.getParameter("max")).thenReturn("25");
        when(request.getParameter("follow")).thenReturn("tags, disk_attachments.disk, ,nics.reporteddevices");
        when(request.getParameter("search")).thenReturn(null);

        final ListQuery query = ListQuery.fromRequest(request);

        assertTrue(query.isAllContent());
        assertTrue(query.followContains("tags"));
        assertTrue(query.followContains("disk_attachments.disk"));
        assertTrue(query.followContains("nics.reporteddevices"));
    }

    @Test
    public void testFromRequest_SearchParserIgnoresNonEqualsAndUsesPageValueAsMaxCurrentBehavior() {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameterMap()).thenReturn(Map.of("search", new String[]{"name=vm and page=3 and status!=down and x>=1"}));
        when(request.getParameter("all_content")).thenReturn(null);
        when(request.getParameter("max")).thenReturn(null);
        when(request.getParameter("follow")).thenReturn(null);
        when(request.getParameter("search")).thenReturn("name=vm and page=3 and status!=down and x>=1");

        final ListQuery query = ListQuery.fromRequest(request);

        // Document existing behavior: when search contains page=..., max is set from it.
        org.junit.Assert.assertEquals(Long.valueOf(3L), query.getLimit());
    }

    @Test
    public void testGetOffset_UsesPageAndMax() {
        final ListQuery query = new ListQuery();
        query.page = 3L;
        query.max = 10L;

        org.junit.Assert.assertEquals(Long.valueOf(20L), query.getOffset());
    }
}
