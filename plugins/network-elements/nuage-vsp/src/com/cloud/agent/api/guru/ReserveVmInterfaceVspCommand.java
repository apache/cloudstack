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

package com.cloud.agent.api.guru;

import com.cloud.agent.api.Command;

public class ReserveVmInterfaceVspCommand extends Command {

    String _nicUuid;
    String _nicMacAddress;
    String _networkUuid;
    boolean _isL3Network;
    String _vpcUuid;
    String _networkDomainUuid;
    String _networksAccountUuid;
    boolean _isDomainRouter;
    String _domainRouterIp;
    String _vmInstanceName;
    String _vmUuid;
    String _vmUserName;
    String _vmUserDomainName;

    public ReserveVmInterfaceVspCommand(String nicUuid, String nicMacAddress, String networkUuid, boolean isL3Network, String vpcUuid, String networkDomainUuid,
            String networksAccountUuid, boolean isDomainRouter, String domainRouterIp, String vmInstanceName, String vmUuid, String vmUserName, String vmUserDomainName) {
        super();
        this._nicUuid = nicUuid;
        this._nicMacAddress = nicMacAddress;
        this._networkUuid = networkUuid;
        this._isL3Network = isL3Network;
        this._vpcUuid = vpcUuid;
        this._networkDomainUuid = networkDomainUuid;
        this._networksAccountUuid = networksAccountUuid;
        this._isDomainRouter = isDomainRouter;
        this._domainRouterIp = domainRouterIp;
        this._vmInstanceName = vmInstanceName;
        this._vmUuid = vmUuid;
        this._vmUserName = vmUserName;
        this._vmUserDomainName = vmUserDomainName;
    }

    public String getNicUuid() {
        return _nicUuid;
    }

    public String getNicMacAddress() {
        return _nicMacAddress;
    }

    public String getNetworkUuid() {
        return _networkUuid;
    }

    public boolean isL3Network() {
        return _isL3Network;
    }

    public String getVpcUuid() {
        return _vpcUuid;
    }

    public String getNetworkDomainUuid() {
        return _networkDomainUuid;
    }

    public String getNetworksAccountUuid() {
        return _networksAccountUuid;
    }

    public boolean isDomainRouter() {
        return _isDomainRouter;
    }

    public String _getDomainRouterIp() {
        return _domainRouterIp;
    }

    public String _getVmInstanceName() {
        return _vmInstanceName;
    }

    public String _getVmUuid() {
        return _vmUuid;
    }

    public String _getVmUserName() {
        return _vmUserName;
    }

    public String _getVmUserDomainName() {
        return _vmUserDomainName;
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }

}
