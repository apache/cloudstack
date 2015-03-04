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

package com.cloud.agent.api;

public class VspResourceCommand extends Command {

    String _method;
    String _resource;
    String _resourceId;
    String _childResource;
    Object _entityDetails;
    String _resourceFilter;
    String _proxyUserUuid;
    String _proxyUserDomainuuid;

    public VspResourceCommand(String method, String resource, String resourceId, String childResource, Object entityDetails, String resourceFilter, String proxyUserUuid,
            String proxyUserDomainuuid) {
        super();
        this._method = method;
        this._resource = resource;
        this._resourceId = resourceId;
        this._childResource = childResource;
        this._entityDetails = entityDetails;
        this._resourceFilter = resourceFilter;
        this._proxyUserUuid = proxyUserUuid;
        this._proxyUserDomainuuid = proxyUserDomainuuid;
    }

    public String getRequestType() {
        return _method;
    }

    public String getResource() {
        return _resource;
    }

    public String getResourceId() {
        return _resourceId;
    }

    public String getChildResource() {
        return _childResource;
    }

    public Object getEntityDetails() {
        return _entityDetails;
    }

    public String getResourceFilter() {
        return _resourceFilter;
    }

    public String getProxyUserUuid() {
        return _proxyUserUuid;
    }

    public String getProxyUserDomainuuid() {
        return _proxyUserDomainuuid;
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }
}
