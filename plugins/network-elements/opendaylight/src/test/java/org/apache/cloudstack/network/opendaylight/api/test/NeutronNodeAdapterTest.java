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

import junit.framework.Assert;

import org.apache.cloudstack.network.opendaylight.api.NeutronRestApiException;
import org.apache.cloudstack.network.opendaylight.api.model.NeutronNode;
import org.apache.cloudstack.network.opendaylight.api.model.NeutronNodeWrapper;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.junit.Test;

import com.google.gson.JsonParser;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

public class NeutronNodeAdapterTest {

    private final Gson gsonNeutronNode = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();

    @Test
    public void gsonNeutronPortMarshalingTest() throws NeutronRestApiException {
        NeutronNode node = new NeutronNode("node-test", "test");
        NeutronNodeWrapper nodeWrapper = new NeutronNodeWrapper(node);

        StringRequestEntity entity;
        try {
            entity = new StringRequestEntity(gsonNeutronNode.toJson(nodeWrapper), "application/json", null);

            String actual = entity.getContent();
            JsonParser parser = new JsonParser();
            Assert.assertEquals(parser.parse(jsonString), parser.parse(actual));
        } catch (UnsupportedEncodingException e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public <T> void gsonNeutronPortUnmarshalingTest() throws NeutronRestApiException {
        NeutronNodeWrapper returnValue = (NeutronNodeWrapper) gsonNeutronNode.fromJson(jsonString, TypeToken.get(NeutronNodeWrapper.class).getType());

        Assert.assertNotNull(returnValue);
        Assert.assertEquals("node-test", returnValue.getNode().getId().toString());
    }

    @Test
    public <T> void gsonNeutronPortUnmarshalingNullTest() throws NeutronRestApiException {
        String json = null;
        NeutronNodeWrapper returnValue = (NeutronNodeWrapper) gsonNeutronNode.fromJson(json, TypeToken.get(NeutronNodeWrapper.class).getType());

        Assert.assertNull(returnValue);
    }

    static String jsonString = "{\"node\":{\"id\":\"node-test\",\"type\":\"test\"}}";
}
