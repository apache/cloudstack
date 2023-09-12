//
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
//

package com.cloud.network.brocade;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;

import com.cloud.network.schema.showvcs.Output;

import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.Before;
import org.junit.Test;

public class BrocadeVcsApiTest {
    protected static final long NETWORK_ID = 42L;
    protected static final int VLAN_ID = 14;
    private static final String MAC_ADDRESS_32 = "0050.56bf.0002";
    protected static final String OUTPUT_XML_RESPONSE = "<output xmlns='urn:brocade.com:mgmt:brocade-vcs'> <vcs-nodes> <vcs-node-info> <node-state>Online</node-state> </vcs-node-info> </vcs-nodes> </output>";
    BrocadeVcsApi api;
    DefaultHttpClient client = mock(DefaultHttpClient.class);
    HttpRequestBase method;
    HttpResponse response;
    HttpResponse postResponse;
    String type;
    String uri;

    @Before
    public void setUp() {
        api = new BrocadeVcsApi("localhost", "admin", "password") {

            @Override
            public HttpRequestBase createMethod(final String newType, final String newUri) {
                type = newType;
                uri = newUri;
                return method;
            }

            @Override
            public HttpResponse executeMethod(HttpRequestBase method) throws BrocadeVcsApiException {
                return response;
            }

            @Override
            protected <T> boolean executeCreateObject(T newObject, String uri) throws BrocadeVcsApiException {

                return true;
            }
        };

        api._client = client;

    }

    @Test
    public void testGetSwitchStatus() throws BrocadeVcsApiException, IOException {
        // Prepare

        method = mock(HttpPost.class);

        response = mock(HttpResponse.class);
        final StatusLine statusLine = mock(StatusLine.class);
        when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        when(response.getStatusLine()).thenReturn(statusLine);
        when(response.getEntity()).thenReturn(new StringEntity(OUTPUT_XML_RESPONSE));

        // Execute
        final Output result = api.getSwitchStatus();

        // Assert
        verify(method, times(1)).releaseConnection();
        assertEquals("Wrong URI for get SwitchStatus REST service", Constants.STATUS_URI, uri);
        assertEquals("Wrong HTTP method for get SwitchStatus REST service", "post", type);
        assertEquals("Wrong state for get SwitchStatus REST service", "Online", result.getVcsNodes().getVcsNodeInfo().get(0).getNodeState());
    }

    @Test
    public void testCreateNetwork() throws BrocadeVcsApiException, IOException {
        // Prepare

        method = mock(HttpPatch.class);

        response = mock(HttpResponse.class);
        final StatusLine statusLine = mock(StatusLine.class);
        when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_NO_CONTENT);
        when(response.getStatusLine()).thenReturn(statusLine);

        // Execute
        api.createNetwork(VLAN_ID, NETWORK_ID);

        // Assert
        verify(method, times(6)).releaseConnection();
        assertEquals("Wrong URI for Network creation REST service", Constants.URI, uri);
        assertEquals("Wrong HTTP method for Network creation REST service", "patch", type);
    }

    @Test
    public void testDeleteNetwork() throws BrocadeVcsApiException, IOException {
        // Prepare

        method = mock(HttpPatch.class);

        response = mock(HttpResponse.class);
        final StatusLine statusLine = mock(StatusLine.class);
        when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_NO_CONTENT);
        when(response.getStatusLine()).thenReturn(statusLine);

        // Execute
        api.deleteNetwork(VLAN_ID, NETWORK_ID);

        // Assert
        verify(method, times(3)).releaseConnection();
        assertEquals("Wrong URI for Network creation REST service", Constants.URI, uri);
        assertEquals("Wrong HTTP method for Network creation REST service", "patch", type);
    }

    @Test
    public void testAssociateMacToNetwork() throws BrocadeVcsApiException, IOException {
        // Prepare

        method = mock(HttpPatch.class);

        response = mock(HttpResponse.class);
        final StatusLine statusLine = mock(StatusLine.class);
        when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_NO_CONTENT);
        when(response.getStatusLine()).thenReturn(statusLine);

        // Execute
        api.associateMacToNetwork(NETWORK_ID, MAC_ADDRESS_32);

        // Assert
        verify(method, times(1)).releaseConnection();
        assertEquals("Wrong URI for Network creation REST service", Constants.URI, uri);
        assertEquals("Wrong HTTP method for Network creation REST service", "patch", type);
    }

    @Test
    public void testDisassociateMacFromNetwork() throws BrocadeVcsApiException, IOException {
        // Prepare

        method = mock(HttpPatch.class);

        response = mock(HttpResponse.class);
        final StatusLine statusLine = mock(StatusLine.class);
        when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_NO_CONTENT);
        when(response.getStatusLine()).thenReturn(statusLine);

        // Execute
        api.disassociateMacFromNetwork(NETWORK_ID, MAC_ADDRESS_32);

        // Assert
        verify(method, times(1)).releaseConnection();
        assertEquals("Wrong URI for Network creation REST service", Constants.URI, uri);
        assertEquals("Wrong HTTP method for Network creation REST service", "patch", type);
    }

}
