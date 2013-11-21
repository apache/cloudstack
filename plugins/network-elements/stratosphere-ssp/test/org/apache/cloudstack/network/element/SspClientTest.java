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

import java.util.UUID;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.junit.Test;

public class SspClientTest {
    HttpClient _client = mock(HttpClient.class);
    PostMethod _postMethod = mock(PostMethod.class);
    PutMethod _putMethod = mock(PutMethod.class);
    DeleteMethod _deleteMethod = mock(DeleteMethod.class);

    String uuid = UUID.randomUUID().toString();

    String apiUrl = "http://a.example.jp/";
    String username = "foo";
    String password = "bar";
    SspClient sspClient = new SspClient(apiUrl, username, password) {
        {
            client = _client;
            postMethod = _postMethod;
            putMethod = _putMethod;
            deleteMethod = _deleteMethod;
        }
    };

    @SuppressWarnings("deprecation")
    private URI getUri() throws Exception {
        return new URI(apiUrl);
    }

    @Test
    public void loginTest() throws Exception {
        when(_postMethod.getURI()).thenReturn(getUri());
        when(_postMethod.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        assertTrue(sspClient.login());
        assertTrue(sspClient.login());
        assertTrue(sspClient.login());
    }

    @Test
    public void createNetworkTest() throws Exception {
        String networkName = "example network 1";
        String tenant_net_uuid = UUID.randomUUID().toString();

        when(_postMethod.getURI()).thenReturn(getUri());
        when(_postMethod.getStatusCode()).thenReturn(HttpStatus.SC_CREATED);
        when(_postMethod.getResponseBodyAsString()).thenReturn("{\"uuid\":\"" + tenant_net_uuid + "\",\"name\":\"" + networkName + "\",\"tenant_uuid\":\"" + uuid + "\"}");
        SspClient.TenantNetwork tnet = sspClient.createTenantNetwork(uuid, networkName);
        assertEquals(tnet.name, networkName);
        assertEquals(tnet.uuid, tenant_net_uuid);
        assertEquals(tnet.tenantUuid, uuid);
    }

    @Test
    public void deleteNetworkTest() throws Exception {
        String tenant_net_uuid = UUID.randomUUID().toString();

        when(_deleteMethod.getURI()).thenReturn(getUri());
        when(_deleteMethod.getStatusCode()).thenReturn(HttpStatus.SC_NO_CONTENT);

        sspClient.deleteTenantNetwork(tenant_net_uuid);
    }
}
