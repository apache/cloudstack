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

import java.io.UnsupportedEncodingException;
import java.util.UUID;

import junit.framework.Assert;

import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.junit.Test;

import com.google.gson.JsonParser;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import org.apache.cloudstack.network.opendaylight.api.NeutronRestApiException;
import org.apache.cloudstack.network.opendaylight.api.model.NeutronNetwork;
import org.apache.cloudstack.network.opendaylight.api.model.NeutronNetworkWrapper;

public class NeutronNetworkAdapterTest {

    private final Gson gsonNeutronNetwork = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();

    @Test
    public void gsonNeutronNetworkMarshalingTest() throws NeutronRestApiException {
        NeutronNetwork network = new NeutronNetwork();
        network.setId(UUID.fromString("ca31aa7f-84c7-416d-bc00-1f84927367e0"));
        network.setName("test_gre");
        network.setNetworkType("test");
        network.setSegmentationId(1001);
        network.setShared(true);
        network.setTenantId("wilder");

        NeutronNetworkWrapper networkWrapper = new NeutronNetworkWrapper();
        networkWrapper.setNetwork(network);

        StringRequestEntity entity;
        try {
            entity = new StringRequestEntity(gsonNeutronNetwork.toJson(networkWrapper), "application/json", null);

            String actual = entity.getContent();
            JsonParser parser = new JsonParser();
            Assert.assertEquals(parser.parse(jsonString), parser.parse(actual));
        } catch (UnsupportedEncodingException e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public <T> void gsonNeutronNetworkUnmarshalingTest() throws NeutronRestApiException {
        NeutronNetwork network = new NeutronNetwork();
        network.setId(UUID.fromString("ca31aa7f-84c7-416d-bc00-1f84927367e0"));
        network.setName("test_gre");
        network.setNetworkType("test");
        network.setSegmentationId(1001);
        network.setShared(true);
        network.setTenantId("wilder");

        NeutronNetworkWrapper networkWrapper = new NeutronNetworkWrapper();
        networkWrapper.setNetwork(network);

        NeutronNetworkWrapper returnValue = (NeutronNetworkWrapper) gsonNeutronNetwork.fromJson(jsonString, TypeToken.get(networkWrapper.getClass()).getType());

        Assert.assertNotNull(returnValue);
        Assert.assertEquals("test_gre", returnValue.getNetwork().getName());
    }

    static String jsonString = "{\"network\":{\"id\":\"ca31aa7f-84c7-416d-bc00-1f84927367e0\",\"name\":"
            + "\"test_gre\",\"shared\":true,\"tenant_id\":\"wilder\",\"provider:network_type\":\"test\",\"provider:segmentation_id\":1001}}";
}
