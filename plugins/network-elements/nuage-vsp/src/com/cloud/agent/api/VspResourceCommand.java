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

    private final String _method;
    private final String _resource;
    private final String _resourceId;
    private final String _childResource;
    private final Object _entityDetails;
    private final String _resourceFilter;
    private final String _proxyUserUuid;
    private final String _proxyUserDomainuuid;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        VspResourceCommand that = (VspResourceCommand) o;

        if (_childResource != null ? !_childResource.equals(that._childResource) : that._childResource != null)
            return false;
        if (_entityDetails != null ? !_entityDetails.equals(that._entityDetails) : that._entityDetails != null)
            return false;
        if (_method != null ? !_method.equals(that._method) : that._method != null) return false;
        if (_proxyUserDomainuuid != null ? !_proxyUserDomainuuid.equals(that._proxyUserDomainuuid) : that._proxyUserDomainuuid != null)
            return false;
        if (_proxyUserUuid != null ? !_proxyUserUuid.equals(that._proxyUserUuid) : that._proxyUserUuid != null)
            return false;
        if (_resource != null ? !_resource.equals(that._resource) : that._resource != null) return false;
        if (_resourceFilter != null ? !_resourceFilter.equals(that._resourceFilter) : that._resourceFilter != null)
            return false;
        if (_resourceId != null ? !_resourceId.equals(that._resourceId) : that._resourceId != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = _method != null ? _method.hashCode() : 0;
        result = 31 * result + (_resource != null ? _resource.hashCode() : 0);
        result = 31 * result + (_resourceId != null ? _resourceId.hashCode() : 0);
        result = 31 * result + (_childResource != null ? _childResource.hashCode() : 0);
        result = 31 * result + (_entityDetails != null ? _entityDetails.hashCode() : 0);
        result = 31 * result + (_resourceFilter != null ? _resourceFilter.hashCode() : 0);
        result = 31 * result + (_proxyUserUuid != null ? _proxyUserUuid.hashCode() : 0);
        result = 31 * result + (_proxyUserDomainuuid != null ? _proxyUserDomainuuid.hashCode() : 0);
        return result;
    }
}
