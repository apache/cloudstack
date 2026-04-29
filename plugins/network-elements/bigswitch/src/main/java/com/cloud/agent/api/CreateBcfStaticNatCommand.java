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

public class CreateBcfStaticNatCommand extends BcfCommand {
    private final String _tenantId;
    private final String _networkId;
    private final String _privateIp;
    private final String _publicIp;
    private final String _mac;

    public CreateBcfStaticNatCommand(final String tenantId, final String networkId,
            final String privateIp, final String publicIp, final String mac){
        this._tenantId = tenantId;
        this._networkId = networkId;
        this._privateIp = privateIp;
        this._publicIp = publicIp;
        this._mac = mac;
    }

    public String getTenantId() {
        return _tenantId;
    }

    public String getNetworkId() {
        return _networkId;
    }

    public String getPrivateIp() {
        return _privateIp;
    }

    public String getPublicIp() {
        return _publicIp;
    }

    public String getMac() {
        return _mac;
    }
}
