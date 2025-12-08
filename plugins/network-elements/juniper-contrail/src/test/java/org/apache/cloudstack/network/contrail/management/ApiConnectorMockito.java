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

package org.apache.cloudstack.network.contrail.management;

import static org.mockito.Mockito.spy;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import net.juniper.contrail.api.ApiConnector;
import net.juniper.contrail.api.ApiConnectorMock;
import net.juniper.contrail.api.ApiObjectBase;
import net.juniper.contrail.api.ApiPropertyBase;
import net.juniper.contrail.api.ObjectReference;
import net.juniper.contrail.api.types.NetworkIpam;


import com.google.common.collect.ImmutableMap;

public class ApiConnectorMockito implements ApiConnector {

    static final Map<String, ApiObjectBase> object_map = new ImmutableMap.Builder<String, ApiObjectBase>().put("network-ipam:default-network-ipam", new NetworkIpam())
        .build();
    private ApiConnectorMock _mock;
    private ApiConnector _spy;

    public ApiConnectorMockito(String hostname, int port) {
        _mock = new ApiConnectorMock(hostname, port);
        _spy = spy(_mock);
    }

    public ApiConnector getSpy() {
        return _spy;
    }

    @Override
    public boolean create(ApiObjectBase arg0) throws IOException {
        return _spy.create(arg0);
    }

    @Override
    public void delete(ApiObjectBase arg0) throws IOException {
        _spy.delete(arg0);
    }

    @Override
    public void delete(Class<? extends ApiObjectBase> arg0, String arg1) throws IOException {
        _spy.delete(arg0, arg1);
    }

    @Override
    public ApiObjectBase find(Class<? extends ApiObjectBase> arg0, ApiObjectBase arg1, String arg2) throws IOException {
        StringBuilder msg = new StringBuilder();
        msg.append("find " + arg0.getName());
        if (arg1 != null) {
            msg.append(" parent: " + arg1.getName());
        }
        msg.append(" name: " + arg2);

        return _mock.find(arg0, arg1, arg2);
    }

    @Override
    public ApiObjectBase findByFQN(Class<? extends ApiObjectBase> arg0, String arg1) throws IOException {
        return _mock.findByFQN(arg0, arg1);
    }

    @Override
    public ApiObjectBase findById(Class<? extends ApiObjectBase> arg0, String arg1) throws IOException {
        return _mock.findById(arg0, arg1);
    }

    @Override
    public String findByName(Class<? extends ApiObjectBase> arg0, List<String> arg1) throws IOException {
        return _mock.findByName(arg0, arg1);
    }

    @Override
    public String findByName(Class<? extends ApiObjectBase> arg0, ApiObjectBase arg1, String arg2) throws IOException {
        StringBuilder msg = new StringBuilder();
        msg.append("findByName " + arg0.getName());
        if (arg1 != null) {
            msg.append(" parent: " + arg1.getName());
        }
        msg.append(" name: " + arg2);
        return _mock.findByName(arg0, arg1, arg2);
    }

    @Override
    public <T extends ApiPropertyBase> List<? extends ApiObjectBase> getObjects(Class<? extends ApiObjectBase> arg0, List<ObjectReference<T>> arg1) throws IOException {
        return _mock.getObjects(arg0, arg1);
    }

    @Override
    public List<? extends ApiObjectBase> list(Class<? extends ApiObjectBase> arg0, List<String> arg1) throws IOException {
        return _mock.list(arg0, arg1);
    }

    @Override
    public boolean read(ApiObjectBase arg0) throws IOException {
        return _mock.read(arg0);
    }

    @Override
    public boolean update(ApiObjectBase arg0) throws IOException {
        return _spy.update(arg0);
    }

}
