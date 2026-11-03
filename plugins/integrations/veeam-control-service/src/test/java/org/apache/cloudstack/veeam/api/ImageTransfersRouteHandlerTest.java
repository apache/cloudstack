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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import com.cloud.exception.InvalidParameterValueException;
import org.apache.cloudstack.veeam.api.dto.ImageTransfer;
import org.apache.cloudstack.veeam.utils.Negotiation;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class ImageTransfersRouteHandlerTest extends RouteHandlerTestSupport {

    @Test
    public void testHandleGetListAndById() throws Exception {
        final ImageTransfersRouteHandler handler = new ImageTransfersRouteHandler();
        handler.serverAdapter = mock(org.apache.cloudstack.veeam.adapter.ServerAdapter.class);
        when(handler.serverAdapter.listAllImageTransfers(null, 5L)).thenReturn(List.of(withId(new ImageTransfer(), "transfer-1")));
        when(handler.serverAdapter.getImageTransfer("transfer-1")).thenReturn(withId(new ImageTransfer(), "transfer-1"));

        final ResponseCapture list = newResponse();
        handler.handle(newRequest("GET", Map.of("max", "5"), null, null), list.response,
                "/api/imagetransfers", Negotiation.OutFormat.JSON, newServlet());
        verify(list.response).setStatus(200);
        assertContains(list.body(), "\"image_transfer\":[");

        final ResponseCapture item = newResponse();
        handler.handle(newRequest("GET"), item.response, "/api/imagetransfers/transfer-1", Negotiation.OutFormat.JSON, newServlet());
        verify(item.response).setStatus(200);
        assertContains(item.body(), "\"id\":\"transfer-1\"");
    }

    @Test
    public void testHandlePostParsesRequestAndCancelFinalizeActions() throws Exception {
        final ImageTransfersRouteHandler handler = new ImageTransfersRouteHandler();
        handler.serverAdapter = mock(org.apache.cloudstack.veeam.adapter.ServerAdapter.class);

        final ArgumentCaptor<ImageTransfer> captor = ArgumentCaptor.forClass(ImageTransfer.class);
        final ImageTransfer created = withId(new ImageTransfer(), "transfer-created");
        created.setPhase("transferring");
        when(handler.serverAdapter.createImageTransfer(captor.capture())).thenReturn(created);

        final ResponseCapture post = newResponse();
        handler.handle(newRequest("POST", Map.of(), "application/json", "{\"phase\":\"transferring\"}"), post.response,
                "/api/imagetransfers", Negotiation.OutFormat.JSON, newServlet());
        verify(post.response).setStatus(201);
        assertEquals("transferring", captor.getValue().getPhase());
        assertContains(post.body(), "transfer-created");

        final ResponseCapture cancel = newResponse();
        handler.handle(newRequest("POST"), cancel.response, "/api/imagetransfers/transfer-1/cancel", Negotiation.OutFormat.JSON, newServlet());
        verify(handler.serverAdapter).cancelImageTransfer("transfer-1");
        verify(cancel.response).setStatus(200);
        assertContains(cancel.body(), "cancelled successfully");

        final ResponseCapture finalize = newResponse();
        handler.handle(newRequest("POST"), finalize.response, "/api/imagetransfers/transfer-1/finalize", Negotiation.OutFormat.JSON, newServlet());
        verify(handler.serverAdapter).finalizeImageTransfer("transfer-1");
        verify(finalize.response).setStatus(200);
        assertContains(finalize.body(), "finalized successfully");
    }

    @Test
    public void testHandleMissingTransferAndUnsupportedActionMethod() throws Exception {
        final ImageTransfersRouteHandler handler = new ImageTransfersRouteHandler();
        handler.serverAdapter = mock(org.apache.cloudstack.veeam.adapter.ServerAdapter.class);
        when(handler.serverAdapter.getImageTransfer("missing")).thenThrow(new InvalidParameterValueException("missing transfer"));

        final ResponseCapture missing = newResponse();
        handler.handle(newRequest("GET"), missing.response, "/api/imagetransfers/missing", Negotiation.OutFormat.JSON, newServlet());
        verify(missing.response).setStatus(404);
        assertContains(missing.body(), "missing transfer");

        final ResponseCapture wrongMethod = newResponse();
        handler.handle(newRequest("GET"), wrongMethod.response, "/api/imagetransfers/transfer-1/cancel", Negotiation.OutFormat.JSON, newServlet());
        verify(wrongMethod.response).setHeader("Allow", "POST");
        verify(wrongMethod.response).setStatus(405);
        assertContains(wrongMethod.body(), "Method Not Allowed");
    }
}
