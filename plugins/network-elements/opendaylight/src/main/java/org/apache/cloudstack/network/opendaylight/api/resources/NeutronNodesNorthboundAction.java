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

import java.lang.reflect.Type;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Collections;

import org.apache.cloudstack.network.opendaylight.api.NeutronRestApiException;
import org.apache.cloudstack.network.opendaylight.api.enums.NeutronNorthboundEnum;
import org.apache.cloudstack.network.opendaylight.api.model.NeutronNodeWrapper;
import org.apache.cloudstack.network.opendaylight.api.model.NeutronNodesList;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

public class NeutronNodesNorthboundAction extends Action {

    private final Gson gsonNeutronNode;

    public NeutronNodesNorthboundAction(final URL url, final String username, final String password) {
        super(url, username, password);
        gsonNeutronNode = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
    }

    @SuppressWarnings("unchecked")
    public <T> T listAllNodes() throws NeutronRestApiException {
        String uri = NeutronNorthboundEnum.PORTS_URI.getUri();
        String bodystring = executeGet(uri, Collections.<String, String> emptyMap());

        Type returnType = new TypeToken<NeutronNodesList<NeutronNodeWrapper>>() {
        }.getType();

        T returnValue = (T) gsonNeutronNode.fromJson(bodystring, returnType);

        return returnValue;
    }

    public <T> void deleteNode(final String nodeType, final String nodeId) throws NeutronRestApiException {
        String uri = NeutronNorthboundEnum.NETWORK_PARAM_URI.getUri();
        uri = MessageFormat.format(uri, nodeType, nodeId);

        executeDelete(uri);
    }

    @SuppressWarnings("unchecked")
    public <T> T updateNeutronNodeV1(final String nodeId, final String ipAddress, final int port) throws NeutronRestApiException {
        String uri = NeutronNorthboundEnum.NODE_PORT_PER_NODE_URI.getUri();
        uri = MessageFormat.format(uri, nodeId, ipAddress, String.valueOf(port));

        String bodystring = executePut(uri);

        T result = (T) gsonNeutronNode.fromJson(bodystring, TypeToken.get(NeutronNodeWrapper.class).getType());

        return result;
    }

    @SuppressWarnings("unchecked")
    public <T> T updateNeutronNodeV2(final String nodeType, final String nodeId, final String ipAddress, final int port) throws NeutronRestApiException {
        String uri = NeutronNorthboundEnum.NODE_PORT_PER_TYPE_URI.getUri();
        uri = MessageFormat.format(uri, nodeType, nodeId, ipAddress, String.valueOf(port));

        String bodystring = executePut(uri);

        T result = (T) gsonNeutronNode.fromJson(bodystring, TypeToken.get(NeutronNodeWrapper.class).getType());

        return result;
    }
}
