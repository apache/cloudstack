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

import java.util.Collection;

import com.cloud.agent.api.Command;

public class ImplementNetworkVspCommand extends Command {

    String _networkDomainName;
    String _networkDomainPath;
    String _networkDomainUuid;
    String _networkAccountName;
    String _networkAccountUuid;
    String _networkName;
    String _networkCidr;
    String _networkGateway;
    String _networkUuid;
    boolean _isL3Network;
    String _vpcName;
    String _vpcUuid;
    boolean _defaultEgressPolicy;
    Collection<String> _ipAddressRange;

    public ImplementNetworkVspCommand(String networkDomainName, String networkDomainPath, String networkDomainUuid, String networkAccountName, String networkAccountUuid,
            String networkName, String networkCidr, String networkGateway, String networkUuid, boolean isL3Network, String vpcName, String vpcUuid, boolean defaultEgressPolicy,
            Collection<String> ipAddressRange) {
        super();
        this._networkDomainName = networkDomainName;
        this._networkDomainPath = networkDomainPath;
        this._networkDomainUuid = networkDomainUuid;
        this._networkAccountName = networkAccountName;
        this._networkAccountUuid = networkAccountUuid;
        this._networkName = networkName;
        this._networkCidr = networkCidr;
        this._networkGateway = networkGateway;
        this._networkUuid = networkUuid;
        this._isL3Network = isL3Network;
        this._vpcName = vpcName;
        this._vpcUuid = vpcUuid;
        this._defaultEgressPolicy = defaultEgressPolicy;
        this._ipAddressRange = ipAddressRange;
    }

    public String getNetworkDomainName() {
        return _networkDomainName;
    }

    public String getNetworkDomainPath() {
        return _networkDomainPath;
    }

    public String getNetworkDomainUuid() {
        return _networkDomainUuid;
    }

    public String getNetworkAccountName() {
        return _networkAccountName;
    }

    public String getNetworkAccountUuid() {
        return _networkAccountUuid;
    }

    public String getNetworkName() {
        return _networkName;
    }

    public String getNetworkCidr() {
        return _networkCidr;
    }

    public String getNetworkGateway() {
        return _networkGateway;
    }

    public String getNetworkUuid() {
        return _networkUuid;
    }

    public boolean isL3Network() {
        return _isL3Network;
    }

    public String getVpcName() {
        return _vpcName;
    }

    public String getVpcUuid() {
        return _vpcUuid;
    }

    public boolean isDefaultEgressPolicy() {
        return _defaultEgressPolicy;
    }

    public Collection<String> getIpAddressRange() {
        return _ipAddressRange;
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }

}
