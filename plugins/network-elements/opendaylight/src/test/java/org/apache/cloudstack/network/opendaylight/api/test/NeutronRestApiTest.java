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

package org.apache.cloudstack.network.opendaylight.api.test;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.UUID;

import junit.framework.Assert;

import org.apache.cloudstack.network.opendaylight.api.NeutronRestApi;
import org.apache.cloudstack.network.opendaylight.api.NeutronRestApiException;
import org.apache.cloudstack.network.opendaylight.api.NeutronRestFactory;
import org.apache.cloudstack.network.opendaylight.api.model.NeutronNetwork;
import org.apache.cloudstack.network.opendaylight.api.model.NeutronNetworkWrapper;
import org.apache.cloudstack.network.opendaylight.api.resources.NeutronNetworksNorthboundAction;
import org.apache.cloudstack.network.opendaylight.api.resources.NeutronNodesNorthboundAction;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.junit.Test;

public class NeutronRestApiTest {

    NeutronRestFactory factory = NeutronRestFactory.getInstance();

    NeutronRestApi httpGet = factory.getNeutronApi(GetMethod.class);
    NeutronRestApi httpPost = factory.getNeutronApi(PostMethod.class);
    NeutronRestApi httpPut = factory.getNeutronApi(PutMethod.class);
    NeutronRestApi httpDelete = factory.getNeutronApi(DeleteMethod.class);

    @Test
    public void resourceHttpGetInstances() throws NeutronRestApiException {
        NeutronRestApi newHttpGet = factory.getNeutronApi(GetMethod.class);
        assertTrue(httpGet == newHttpGet);
    }

    @Test
    public void resourceHttpPostInstances() throws NeutronRestApiException {
        NeutronRestApi newHttpPost = factory.getNeutronApi(PostMethod.class);
        assertTrue(httpPost == newHttpPost);
    }

    @Test
    public void resourceHttpPutInstances() throws NeutronRestApiException {
        NeutronRestApi newHttpPut = factory.getNeutronApi(PutMethod.class);
        assertTrue(httpPut == newHttpPut);
    }

    @Test
    public void resourceHttpDeleteInstances() throws NeutronRestApiException {
        NeutronRestApi newHttpDelete = factory.getNeutronApi(DeleteMethod.class);
        assertTrue(httpDelete == newHttpDelete);
    }

    @Test(expected = NeutronRestApiException.class)
    public void neutronNetworksFail() throws NeutronRestApiException {
        URL url;
        try {
            url = new URL("http://localhost:8080");

            NeutronNetworksNorthboundAction neutron = new NeutronNetworksNorthboundAction(url, "admin", "admin");
            neutron.listAllNetworks();
        } catch (MalformedURLException e) {
            Assert.fail("Should not fail here.");
        }
    }

    @Test(expected = NeutronRestApiException.class)
    public void neutronFindNetworkByIdFail() throws NeutronRestApiException {
        URL url;
        try {
            url = new URL("http://localhost:8080");

            NeutronNetworksNorthboundAction neutron = new NeutronNetworksNorthboundAction(url, "admin", "admin");
            neutron.findNetworkById("0AACEED5-A688-429A-92FC-E1C9E4EEEE98");
        } catch (MalformedURLException e) {
            Assert.fail("Should not fail here.");
        }
    }

    @Test(expected = NeutronRestApiException.class)
    public void neutronNodesFail() throws NeutronRestApiException {
        URL url;
        try {
            url = new URL("http://localhost:8080");

            NeutronNodesNorthboundAction neutron = new NeutronNodesNorthboundAction(url, "admin", "admin");
            neutron.listAllNodes();
        } catch (MalformedURLException e) {
            Assert.fail("Should not fail here.");
        }
    }

    /*
     * Test fails because there is no controller. It's used only to test that
     * the HTTP methods are correct.
     */
    @Test(expected = NeutronRestApiException.class)
    public void neutronHTTPDeleteMethod() throws NeutronRestApiException {
        URL url;
        try {
            url = new URL("http://127.0.0.1:8080");

            NeutronNetworksNorthboundAction neutron = new NeutronNetworksNorthboundAction(url, "admin", "admin");
            neutron.deleteNeutronNetwork("0AACEED5-A688-429A-92FC-E1C9E4EEEE98");
        } catch (MalformedURLException e) {
            Assert.fail("Should not fail here.");
        }
    }

    /*
     * Test fails because there is no controller. It's used only to test that
     * the HTTP methods are correct.
     */
    @Test(expected = NeutronRestApiException.class)
    public void neutronHTTPGetMethod() throws NeutronRestApiException {
        URL url;
        try {
            url = new URL("http://localhost:8080");

            NeutronNetworksNorthboundAction neutron = new NeutronNetworksNorthboundAction(url, "admin", "admin");
            neutron.listAllNetworks();
        } catch (MalformedURLException e) {
            Assert.fail("Should not fail here.");
        }
    }

    /*
     * Test fails because there is no controller. It's used only to test that
     * the HTTP methods are correct.
     */
    @Test(expected = NeutronRestApiException.class)
    public void neutronHTTPPostMethod() throws NeutronRestApiException {
        URL url;
        try {
            url = new URL("http://localhost:8080");

            NeutronNetwork network = new NeutronNetwork();
            network.setId(UUID.fromString("ca31aa7f-84c7-416d-bc00-1f84927367e0"));
            network.setName("test_gre");
            network.setNetworkType("test");
            network.setSegmentationId(1001);
            network.setShared(true);
            network.setTenantId("wilder");

            NeutronNetworkWrapper networkWrapper = new NeutronNetworkWrapper();
            networkWrapper.setNetwork(network);

            NeutronNetworksNorthboundAction neutron = new NeutronNetworksNorthboundAction(url, "admin", "admin");
            neutron.createNeutronNetwork(networkWrapper);

        } catch (MalformedURLException e) {
            Assert.fail("Should not fail here.");
        }
    }

    /*
     * Test fails because there is no controller. It's used only to test that
     * the HTTP methods are correct.
     */
    @Test(expected = NeutronRestApiException.class)
    public void neutronHTTPPutMethod() throws NeutronRestApiException {
        URL url;
        try {
            url = new URL("http://localhost:8080");

            NeutronNetwork network = new NeutronNetwork();
            network.setId(UUID.fromString("ca31aa7f-84c7-416d-bc00-1f84927367e0"));
            network.setName("test_gre");
            network.setNetworkType("test");
            network.setSegmentationId(1001);
            network.setShared(true);
            network.setTenantId("wilder");

            NeutronNetworkWrapper networkWrapper = new NeutronNetworkWrapper();
            networkWrapper.setNetwork(network);

            NeutronNetworksNorthboundAction neutron = new NeutronNetworksNorthboundAction(url, "admin", "admin");
            neutron.updateNeutronNetwork("ca31aa7f-84c7-416d-bc00-1f84927367e0", networkWrapper);

        } catch (MalformedURLException e) {
            Assert.fail("Should not fail here.");
        }
    }

    /*
     * Test fails because there is no controller. It's used only to test that
     * the HTTP methods are correct.
     */
    @Test(expected = NeutronRestApiException.class)
    public void neutronHTTPPutUriMethod() throws NeutronRestApiException {
        URL url;
        try {
            url = new URL("http://localhost:8080");

            NeutronNodesNorthboundAction neutron = new NeutronNodesNorthboundAction(url, "admin", "admin");
            neutron.updateNeutronNodeV1("ca31aa7f-84c7-416d-bc00-1f84927367e0", "1.1.1.1.", 6400);

        } catch (MalformedURLException e) {
            Assert.fail("Should not fail here.");
        }
    }

    static String networkJSON = "{" + "\"networks\": [" + "{" + "\"network\": {" + "\"segmentation_id\": 100," + "\"shared\": false," + "\"name\": \"net_test\","
            + "\"network_type\": \"test\"," + "\"tenant_id\": \"t\"," + "\"id\": \"0AACEED5-A688-429A-92FC-E1C9E4EEEE98\"," + "\"status\": \"ACTIVE\"" + "}" + "}" + "]" + "}";
}

class NeutronRestApiMock extends NeutronRestApi {

    HttpClient client = mock(HttpClient.class);

    NeutronRestApiMock(final Class<? extends HttpMethodBase> httpClazz) {
        super(httpClazz);
    }

    @Override
    public void executeMethod(final HttpMethodBase method) throws NeutronRestApiException {
        try {
            client.executeMethod(method);
        } catch (HttpException e) {
            method.releaseConnection();
            throw new NeutronRestApiException("API call to Neutron NVP Controller Failed", e);
        } catch (IOException e) {
            method.releaseConnection();
            throw new NeutronRestApiException("API call to Neutron NVP Controller Failed", e);
        }
    }
}
