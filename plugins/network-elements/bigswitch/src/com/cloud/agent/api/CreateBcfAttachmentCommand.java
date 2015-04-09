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

public class CreateBcfAttachmentCommand extends BcfCommand {
    private String _tenantId;
    private String _tenantName;
    private String _networkId;
    private String _portId;
    private String _nicId;
    private Integer _vlan;
    private String _ipv4;
    private String _mac;

    public CreateBcfAttachmentCommand(String tenantId, String tenantName,
            String networkId, String portId, String nicId,
            Integer vlan, String ipv4, String mac) {
        this._tenantId = tenantId;
        this._tenantName = tenantName;
        this._networkId = networkId;
        this._portId = portId;
        this._nicId = nicId;
        this._vlan = vlan;
        this._ipv4 = ipv4;
        this._mac = mac;
    }

    public String getTenantId() {
        return _tenantId;
    }

    public String getTenantName() {
        return _tenantName;
    }
    public String getNetworkId() {
        return _networkId;
    }

    public String getPortId() {
        return _portId;
    }

    public String getNicId() {
        return _nicId;
    }

    public Integer getVlan() {
        return _vlan;
    }

    public String getIpv4() {
        return _ipv4;
    }

    public String getMac() {
        return _mac;
    }
}
