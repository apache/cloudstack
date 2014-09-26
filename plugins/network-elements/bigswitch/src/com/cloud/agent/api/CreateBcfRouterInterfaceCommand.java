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

public class CreateBcfRouterInterfaceCommand extends BcfCommand{
    private final String _tenantId;
    private final String _networkId;
    private final String _cidr;
    private final String _gateway;
    private final String _networkName;

    public CreateBcfRouterInterfaceCommand(final String tenantId, final String networkId,
            final String cidr, final String gateway, final String networkName){
        this._tenantId = tenantId;
        this._networkId = networkId;
        this._networkName = networkName;
        this._cidr = cidr;
        this._gateway = gateway;
    }

    public String getTenantId() {
        return _tenantId;
    }

    public String getNetworkId() {
        return _networkId;
    }

    public String getNetworkName() {
        return _networkName;
    }

    public String getCidr() {
        return _cidr;
    }

    public String getGateway() {
        return _gateway;
    }
}
