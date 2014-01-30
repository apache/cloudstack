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
package org.apache.cloudstack.network.element;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;

import java.io.ByteArrayInputStream;
import java.util.UUID;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.junit.Test;

public class SspClientTest {
    String uuid = UUID.randomUUID().toString();

    String apiUrl = "http://a.example.jp/";
    String username = "foo";
    String password = "bar";

    @Test
    public void loginTest() throws Exception {
        SspClient sspClient = spy(new SspClient(apiUrl, username, password));

        HttpClient client = mock(HttpClient.class);
        HttpResponse res = mock(HttpResponse.class, RETURNS_DEEP_STUBS);
        doReturn(client).when(sspClient).getHttpClient();
        when(client.execute(any(HttpUriRequest.class))).thenReturn(res);
        when(res.getStatusLine().getStatusCode()).thenReturn(HttpStatus.SC_OK);

        assertTrue(sspClient.login());
        assertTrue(sspClient.login());
        assertTrue(sspClient.login());
    }

    @Test
    public void createNetworkTest() throws Exception {
        String networkName = "example network 1";
        String tenant_net_uuid = UUID.randomUUID().toString();
        SspClient sspClient = spy(new SspClient(apiUrl, username, password));

        HttpClient client = mock(HttpClient.class);
        HttpResponse res = mock(HttpResponse.class, RETURNS_DEEP_STUBS);
        doReturn(client).when(sspClient).getHttpClient();
        when(client.execute(any(HttpUriRequest.class))).thenReturn(res);
        when(res.getStatusLine().getStatusCode()).thenReturn(HttpStatus.SC_CREATED);
        String body = "{\"uuid\":\"" + tenant_net_uuid + "\",\"name\":\"" + networkName
                + "\",\"tenant_uuid\":\"" + uuid + "\"}";
        when(res.getEntity().getContent()).thenReturn(
                new ByteArrayInputStream(body.getBytes("UTF-8")));

        SspClient.TenantNetwork tnet = sspClient.createTenantNetwork(uuid, networkName);
        assertEquals(tnet.name, networkName);
        assertEquals(tnet.uuid, tenant_net_uuid);
        assertEquals(tnet.tenantUuid, uuid);
    }

    @Test
    public void deleteNetworkTest() throws Exception {
        String tenant_net_uuid = UUID.randomUUID().toString();
        SspClient sspClient = spy(new SspClient(apiUrl, username, password));

        HttpClient client = mock(HttpClient.class);
        HttpResponse res = mock(HttpResponse.class, RETURNS_DEEP_STUBS);
        doReturn(client).when(sspClient).getHttpClient();
        when(client.execute(any(HttpUriRequest.class))).thenReturn(res);
        when(res.getStatusLine().getStatusCode()).thenReturn(HttpStatus.SC_NO_CONTENT);

        sspClient.deleteTenantNetwork(tenant_net_uuid);
    }
}
