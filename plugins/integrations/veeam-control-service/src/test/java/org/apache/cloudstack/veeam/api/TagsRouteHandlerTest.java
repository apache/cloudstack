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

package org.apache.cloudstack.veeam.api;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import com.cloud.exception.InvalidParameterValueException;
import org.apache.cloudstack.veeam.api.dto.Tag;
import org.apache.cloudstack.veeam.utils.Negotiation;
import org.junit.Test;

public class TagsRouteHandlerTest extends RouteHandlerTestSupport {

    @Test
    public void testHandleGetListAndById() throws Exception {
        final TagsRouteHandler handler = new TagsRouteHandler();
        handler.serverAdapter = mock(org.apache.cloudstack.veeam.adapter.ServerAdapter.class);
        when(handler.serverAdapter.listAllTags(null, null)).thenReturn(List.of(withId(new Tag(), "tag-1")));
        when(handler.serverAdapter.getTag("tag-1")).thenReturn(withId(new Tag(), "tag-1"));

        final ResponseCapture list = newResponse();
        handler.handle(newRequest("GET"), list.response, "/api/tags", Negotiation.OutFormat.JSON, newServlet());
        verify(list.response).setStatus(200);
        assertContains(list.body(), "\"tag\":[");

        final ResponseCapture item = newResponse();
        handler.handle(newRequest("GET"), item.response, "/api/tags/tag-1", Negotiation.OutFormat.JSON, newServlet());
        verify(item.response).setStatus(200);
        assertContains(item.body(), "\"id\":\"tag-1\"");
    }

    @Test
    public void testHandleMissingTagReturnsNotFound() throws Exception {
        final TagsRouteHandler handler = new TagsRouteHandler();
        handler.serverAdapter = mock(org.apache.cloudstack.veeam.adapter.ServerAdapter.class);
        when(handler.serverAdapter.getTag("missing")).thenThrow(new InvalidParameterValueException("missing tag"));

        final ResponseCapture response = newResponse();
        handler.handle(newRequest("GET"), response.response, "/api/tags/missing", Negotiation.OutFormat.JSON, newServlet());

        verify(response.response).setStatus(404);
        assertContains(response.body(), "missing tag");
    }
}
