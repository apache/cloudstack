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

package org.apache.cloudstack.network.opendaylight.api.resources;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Collections;

import org.apache.cloudstack.network.opendaylight.api.NeutronRestApiException;
import org.apache.cloudstack.network.opendaylight.api.enums.NeutronNorthboundEnum;
import org.apache.cloudstack.network.opendaylight.api.model.NeutronPortWrapper;
import org.apache.cloudstack.network.opendaylight.api.model.NeutronPortsList;
import org.apache.commons.httpclient.methods.StringRequestEntity;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

public class NeutronPortsNorthboundAction extends Action {

    private final Gson gsonNeutronPort;

    public NeutronPortsNorthboundAction(final URL url, final String username, final String password) {
        super(url, username, password);
        gsonNeutronPort = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
    }

    @SuppressWarnings("unchecked")
    public <T> T listAllPorts() throws NeutronRestApiException {
        String uri = NeutronNorthboundEnum.PORTS_URI.getUri();
        String bodystring = executeGet(uri, Collections.<String, String> emptyMap());

        Type returnType = new TypeToken<NeutronPortsList<NeutronPortWrapper>>() {
        }.getType();

        T returnValue = (T) gsonNeutronPort.fromJson(bodystring, returnType);

        return returnValue;
    }

    @SuppressWarnings("unchecked")
    public <T> T findPortById(final String portId) throws NeutronRestApiException {
        String uri = NeutronNorthboundEnum.PORTS_PARAM_URI.getUri();
        uri = MessageFormat.format(uri, portId);

        String bodystring = executeGet(uri, Collections.<String, String> emptyMap());

        Type returnType = new TypeToken<NeutronPortWrapper>() {
        }.getType();

        T returnValue = (T) gsonNeutronPort.fromJson(bodystring, returnType);

        return returnValue;
    }

    @SuppressWarnings("unchecked")
    public <T> T createNeutronPort(final NeutronPortWrapper newPortWrapper) throws NeutronRestApiException {
        try {
            String uri = NeutronNorthboundEnum.PORTS_URI.getUri();
            StringRequestEntity entity = new StringRequestEntity(gsonNeutronPort.toJson(newPortWrapper), JSON_CONTENT_TYPE, null);

            String bodystring = executePost(uri, entity);

            T result = (T) gsonNeutronPort.fromJson(bodystring, TypeToken.get(NeutronPortWrapper.class).getType());

            return result;
        } catch (UnsupportedEncodingException e) {
            throw new NeutronRestApiException("Failed to encode json request body", e);
        }
    }

    public <T> void updateNeutronPort(final String portId, final NeutronPortWrapper newPortWrapper) throws NeutronRestApiException {
        try {
            String uri = NeutronNorthboundEnum.PORTS_PARAM_URI.getUri();
            uri = MessageFormat.format(uri, portId);

            StringRequestEntity entity = new StringRequestEntity(gsonNeutronPort.toJson(newPortWrapper), JSON_CONTENT_TYPE, null);

            executePut(uri, entity);
        } catch (UnsupportedEncodingException e) {
            throw new NeutronRestApiException("Failed to encode json request body", e);
        }
    }

    public <T> void deleteNeutronPort(final String portId) throws NeutronRestApiException {
        String uri = NeutronNorthboundEnum.PORTS_PARAM_URI.getUri();
        uri = MessageFormat.format(uri, portId);

        executeDelete(uri);
    }
}
