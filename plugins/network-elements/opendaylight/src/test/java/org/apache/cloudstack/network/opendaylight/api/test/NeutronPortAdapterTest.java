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

import org.apache.cloudstack.network.opendaylight.api.NeutronRestApiException;
import org.apache.cloudstack.network.opendaylight.api.model.NeutronPort;
import org.apache.cloudstack.network.opendaylight.api.model.NeutronPortWrapper;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.junit.Test;

import com.google.gson.JsonParser;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

public class NeutronPortAdapterTest {

    private final Gson gsonNeutronPort = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();

    @Test
    public void gsonNeutronPortMarshalingTest() throws NeutronRestApiException {
        NeutronPort port = new NeutronPort();

        port.setId(UUID.fromString("ca31aa7f-84c7-416d-bc00-1f84927367e0"));
        port.setName("test_gre");
        port.setAdminStateUp(true);
        port.setDeviceId(UUID.fromString("ca31aa7f-84c7-416d-bc00-1f84927367e0"));
        port.setMacAddress("ca31aa7f-84c7-416d-bc00-1f84927367e0");
        port.setNetworkId(UUID.fromString("ca31aa7f-84c7-416d-bc00-1f84927367e0"));
        port.setStatus("ACTIVE");
        port.setTenantId("wilder");

        NeutronPortWrapper portWrapper = new NeutronPortWrapper();
        portWrapper.setPort(port);

        StringRequestEntity entity;
        try {
            entity = new StringRequestEntity(gsonNeutronPort.toJson(portWrapper), "application/json", null);

            String actual = entity.getContent();

            JsonParser parser = new JsonParser();
            Assert.assertEquals(parser.parse(jsonString), parser.parse(actual));
        } catch (UnsupportedEncodingException e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public <T> void gsonNeutronPortUnmarshalingTest() throws NeutronRestApiException {
        NeutronPort port = new NeutronPort();

        port.setId(UUID.fromString("ca31aa7f-84c7-416d-bc00-1f84927367e0"));
        port.setName("test_gre");
        port.setAdminStateUp(true);
        port.setDeviceId(UUID.fromString("ca31aa7f-84c7-416d-bc00-1f84927367e0"));
        port.setMacAddress("ca31aa7f-84c7-416d-bc00-1f84927367e0");
        port.setNetworkId(UUID.fromString("ca31aa7f-84c7-416d-bc00-1f84927367e0"));
        port.setStatus("ACTIVE");
        port.setTenantId("wilder");

        NeutronPortWrapper portWrapper = new NeutronPortWrapper();
        portWrapper.setPort(port);

        NeutronPortWrapper returnValue = (NeutronPortWrapper) gsonNeutronPort.fromJson(jsonString, TypeToken.get(portWrapper.getClass()).getType());

        Assert.assertNotNull(returnValue);
        Assert.assertEquals("ca31aa7f-84c7-416d-bc00-1f84927367e0", returnValue.getPort().getMacAddress());
    }

    static String jsonString = "{\"port\":{\"id\":\"ca31aa7f-84c7-416d-bc00-1f84927367e0\",\"name\":\"test_gre\",\"tenant_id\":\"wilder\",\"network_id\":"
            + "\"ca31aa7f-84c7-416d-bc00-1f84927367e0\",\"mac_address\":\"ca31aa7f-84c7-416d-bc00-1f84927367e0\",\"device_id\":\"ca31aa7f-84c7-416d-bc00-1f84927367e0\","
            + "\"admin_state_up\":true,\"status\":\"ACTIVE\"}}";
}
