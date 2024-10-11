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

import org.apache.commons.httpclient.methods.StringRequestEntity;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import org.apache.cloudstack.network.opendaylight.api.NeutronRestApiException;
import org.apache.cloudstack.network.opendaylight.api.enums.NeutronNorthboundEnum;
import org.apache.cloudstack.network.opendaylight.api.model.NeutronNetwork;
import org.apache.cloudstack.network.opendaylight.api.model.NeutronNetworkWrapper;
import org.apache.cloudstack.network.opendaylight.api.model.NeutronNetworksList;

public class NeutronNetworksNorthboundAction extends Action {

    private final Gson gsonNeutronNetwork;

    public NeutronNetworksNorthboundAction(final URL url, final String username, final String password) {
        super(url, username, password);
        gsonNeutronNetwork = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
    }

    @SuppressWarnings("unchecked")
    public <T> T listAllNetworks() throws NeutronRestApiException {
        String uri = NeutronNorthboundEnum.NETWORKS_URI.getUri();
        String bodystring = executeGet(uri, Collections.<String, String> emptyMap());

        Type returnType = new TypeToken<NeutronNetworksList<NeutronNetwork>>() {
        }.getType();

        T returnValue = (T) gsonNeutronNetwork.fromJson(bodystring, returnType);

        return returnValue;
    }

    @SuppressWarnings("unchecked")
    public <T> T findNetworkById(final String networkId) throws NeutronRestApiException {
        String uri = NeutronNorthboundEnum.NETWORK_PARAM_URI.getUri();
        uri = MessageFormat.format(uri, networkId);

        String bodystring = executeGet(uri, Collections.<String, String> emptyMap());

        Type returnType = new TypeToken<NeutronNetworkWrapper>() {
        }.getType();

        T returnValue = (T) gsonNeutronNetwork.fromJson(bodystring, returnType);

        return returnValue;
    }

    @SuppressWarnings("unchecked")
    public <T> T createNeutronNetwork(final NeutronNetworkWrapper newNetworkWrapper) throws NeutronRestApiException {
        try {
            String uri = NeutronNorthboundEnum.NETWORKS_URI.getUri();
            StringRequestEntity entity = new StringRequestEntity(gsonNeutronNetwork.toJson(newNetworkWrapper), JSON_CONTENT_TYPE, null);

            String bodystring = executePost(uri, entity);

            T result = (T) gsonNeutronNetwork.fromJson(bodystring, TypeToken.get(NeutronNetworkWrapper.class).getType());

            return result;
        } catch (UnsupportedEncodingException e) {
            throw new NeutronRestApiException("Failed to encode json request body", e);
        }
    }

    public <T> void updateNeutronNetwork(final String networkId, final NeutronNetworkWrapper newNetworkWrapper) throws NeutronRestApiException {
        try {
            String uri = NeutronNorthboundEnum.NETWORK_PARAM_URI.getUri();
            uri = MessageFormat.format(uri, networkId);

            StringRequestEntity entity = new StringRequestEntity(gsonNeutronNetwork.toJson(newNetworkWrapper), JSON_CONTENT_TYPE, null);

            executePut(uri, entity);
        } catch (UnsupportedEncodingException e) {
            throw new NeutronRestApiException("Failed to encode json request body", e);
        }
    }

    public <T> void deleteNeutronNetwork(final String networkId) throws NeutronRestApiException {
        String uri = NeutronNorthboundEnum.NETWORK_PARAM_URI.getUri();
        uri = MessageFormat.format(uri, networkId);

        executeDelete(uri);
    }
}
