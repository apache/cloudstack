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
package org.apache.cloudstack.network.tungsten.vrouter;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;
import java.util.Arrays;

@RunWith(PowerMockRunner.class)
@PrepareForTest({HttpClients.class, EntityUtils.class})
public class VRouterApiConnectorImplTest {
    VRouterApiConnector vRouterApiConnector;

    @Before
    public void setup() {
        VRouter vRouter = mock(VRouter.class);
        vRouterApiConnector = new VRouterApiConnectorImpl(vRouter);
        PowerMockito.mockStatic(HttpClients.class);
        PowerMockito.mockStatic(EntityUtils.class);
    }

    @Test
    public void addPortTest() throws Exception {
        Port port = mock(Port.class);
        CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
        CloseableHttpResponse closeableHttpResponse = mock(CloseableHttpResponse.class);
        HttpEntity httpEntity = mock(HttpEntity.class);

        when(HttpClients.createDefault()).thenReturn(httpClient);
        when(httpClient.execute(any(HttpUriRequest.class))).thenReturn(closeableHttpResponse);
        when(closeableHttpResponse.getEntity()).thenReturn(httpEntity);
        when(EntityUtils.toString(any(HttpEntity.class))).thenReturn("{}");

        assertTrue(vRouterApiConnector.addPort(port));
    }

    @Test
    public void addPortWithExceptionTest() throws Exception {
        Port port = mock(Port.class);
        CloseableHttpClient httpClient = mock(CloseableHttpClient.class);

        when(HttpClients.createDefault()).thenReturn(httpClient);
        when(httpClient.execute(any(HttpUriRequest.class))).thenThrow(IOException.class);

        assertFalse(vRouterApiConnector.addPort(port));
    }

    @Test
    public void addPortWithFailTest() throws Exception {
        Port port = mock(Port.class);
        CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
        CloseableHttpResponse closeableHttpResponse = mock(CloseableHttpResponse.class);
        HttpEntity httpEntity = mock(HttpEntity.class);

        when(HttpClients.createDefault()).thenReturn(httpClient);
        when(httpClient.execute(any(HttpUriRequest.class))).thenReturn(closeableHttpResponse);
        when(closeableHttpResponse.getEntity()).thenReturn(httpEntity);
        when(EntityUtils.toString(any(HttpEntity.class))).thenReturn("{error:404}");

        assertFalse(vRouterApiConnector.addPort(port));
    }

    @Test
    public void deletePortTest() throws Exception {
        CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
        CloseableHttpResponse closeableHttpResponse = mock(CloseableHttpResponse.class);
        HttpEntity httpEntity = mock(HttpEntity.class);

        when(HttpClients.createDefault()).thenReturn(httpClient);
        when(httpClient.execute(any(HttpUriRequest.class))).thenReturn(closeableHttpResponse);
        when(closeableHttpResponse.getEntity()).thenReturn(httpEntity);
        when(EntityUtils.toString(any(HttpEntity.class))).thenReturn("{}");

        assertTrue(vRouterApiConnector.deletePort("948f421c-edde-4518-a391-09299cc25dc2"));
    }

    @Test
    public void deletePortWithExceptionTest() throws Exception {
        CloseableHttpClient httpClient = mock(CloseableHttpClient.class);

        when(HttpClients.createDefault()).thenReturn(httpClient);
        when(httpClient.execute(any(HttpUriRequest.class))).thenThrow(IOException.class);

        assertFalse(vRouterApiConnector.deletePort("948f421c-edde-4518-a391-09299cc25dc2"));
    }

    @Test
    public void enablePortTest() throws Exception {
        CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
        CloseableHttpResponse closeableHttpResponse = mock(CloseableHttpResponse.class);
        HttpEntity httpEntity = mock(HttpEntity.class);

        when(HttpClients.createDefault()).thenReturn(httpClient);
        when(httpClient.execute(any(HttpUriRequest.class))).thenReturn(closeableHttpResponse);
        when(closeableHttpResponse.getEntity()).thenReturn(httpEntity);
        when(EntityUtils.toString(any(HttpEntity.class))).thenReturn("{}");

        assertTrue(vRouterApiConnector.enablePort("948f421c-edde-4518-a391-09299cc25dc2"));
    }

    @Test
    public void enablePortWithExceptionTest() throws Exception {
        CloseableHttpClient httpClient = mock(CloseableHttpClient.class);

        when(HttpClients.createDefault()).thenReturn(httpClient);
        when(httpClient.execute(any(HttpUriRequest.class))).thenThrow(IOException.class);

        assertFalse(vRouterApiConnector.enablePort("948f421c-edde-4518-a391-09299cc25dc2"));
    }

    @Test
    public void disablePortTest() throws Exception {
        CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
        CloseableHttpResponse closeableHttpResponse = mock(CloseableHttpResponse.class);
        HttpEntity httpEntity = mock(HttpEntity.class);

        when(HttpClients.createDefault()).thenReturn(httpClient);
        when(httpClient.execute(any(HttpUriRequest.class))).thenReturn(closeableHttpResponse);
        when(closeableHttpResponse.getEntity()).thenReturn(httpEntity);
        when(EntityUtils.toString(any(HttpEntity.class))).thenReturn("{}");

        assertTrue(vRouterApiConnector.disablePort("948f421c-edde-4518-a391-09299cc25dc2"));
    }

    @Test
    public void disablePortWithExceptionTest() throws Exception {
        CloseableHttpClient httpClient = mock(CloseableHttpClient.class);

        when(HttpClients.createDefault()).thenReturn(httpClient);
        when(httpClient.execute(any(HttpUriRequest.class))).thenThrow(IOException.class);

        assertFalse(vRouterApiConnector.disablePort("948f421c-edde-4518-a391-09299cc25dc2"));
    }

    @Test
    public void addGatewayTest() throws Exception {
        Gateway gateway1 = mock(Gateway.class);
        Gateway gateway2 = mock(Gateway.class);
        CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
        CloseableHttpResponse closeableHttpResponse = mock(CloseableHttpResponse.class);
        HttpEntity httpEntity = mock(HttpEntity.class);

        when(HttpClients.createDefault()).thenReturn(httpClient);
        when(httpClient.execute(any(HttpUriRequest.class))).thenReturn(closeableHttpResponse);
        when(closeableHttpResponse.getEntity()).thenReturn(httpEntity);
        when(EntityUtils.toString(any(HttpEntity.class))).thenReturn("{}");

        assertTrue(vRouterApiConnector.addGateway(Arrays.asList(gateway1, gateway2)));
    }

    @Test
    public void addGatewayWithExceptionTest() throws Exception {
        Gateway gateway1 = mock(Gateway.class);
        Gateway gateway2 = mock(Gateway.class);
        CloseableHttpClient httpClient = mock(CloseableHttpClient.class);

        when(HttpClients.createDefault()).thenReturn(httpClient);
        when(httpClient.execute(any(HttpUriRequest.class))).thenThrow(IOException.class);

        assertFalse(vRouterApiConnector.addGateway(Arrays.asList(gateway1, gateway2)));
    }

    @Test
    public void deleteGatewayTest() throws Exception {
        Gateway gateway1 = mock(Gateway.class);
        Gateway gateway2 = mock(Gateway.class);
        CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
        CloseableHttpResponse closeableHttpResponse = mock(CloseableHttpResponse.class);
        HttpEntity httpEntity = mock(HttpEntity.class);

        when(HttpClients.createDefault()).thenReturn(httpClient);
        when(httpClient.execute(any(HttpUriRequest.class))).thenReturn(closeableHttpResponse);
        when(closeableHttpResponse.getEntity()).thenReturn(httpEntity);
        when(EntityUtils.toString(any(HttpEntity.class))).thenReturn("{}");

        assertTrue(vRouterApiConnector.deleteGateway(Arrays.asList(gateway1, gateway2)));
    }

    @Test
    public void deleteGatewayWithExceptionTest() throws Exception {
        Gateway gateway1 = mock(Gateway.class);
        Gateway gateway2 = mock(Gateway.class);
        CloseableHttpClient httpClient = mock(CloseableHttpClient.class);

        when(HttpClients.createDefault()).thenReturn(httpClient);
        when(httpClient.execute(any(HttpUriRequest.class))).thenThrow(IOException.class);

        assertFalse(vRouterApiConnector.deleteGateway(Arrays.asList(gateway1, gateway2)));
    }
}
