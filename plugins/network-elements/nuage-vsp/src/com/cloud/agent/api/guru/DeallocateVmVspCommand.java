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

public class DeallocateVmVspCommand extends Command {

    String _networkUuid;
    String _nicFrmDdUuid;
    String _nicMacAddress;
    String _nicIp4Address;
    boolean _isL3Network;
    String _vpcUuid;
    String _networksDomainUuid;
    String _vmInstanceName;
    String _vmUuid;

    public DeallocateVmVspCommand(String networkUuid, String nicFrmDdUuid, String nicMacAddress, String nicIp4Address, boolean isL3Network, String vpcUuid,
            String networksDomainUuid, String vmInstanceName, String vmUuid) {
        super();
        this._networkUuid = networkUuid;
        this._nicFrmDdUuid = nicFrmDdUuid;
        this._nicMacAddress = nicMacAddress;
        this._nicIp4Address = nicIp4Address;
        this._isL3Network = isL3Network;
        this._vpcUuid = vpcUuid;
        this._networksDomainUuid = networksDomainUuid;
        this._vmInstanceName = vmInstanceName;
        this._vmUuid = vmUuid;
    }

    public String getNetworkUuid() {
        return _networkUuid;
    }

    public String getNicFrmDdUuid() {
        return _nicFrmDdUuid;
    }

    public String getNicMacAddress() {
        return _nicMacAddress;
    }

    public String getNicIp4Address() {
        return _nicIp4Address;
    }

    public boolean isL3Network() {
        return _isL3Network;
    }

    public String getVpcUuid() {
        return _vpcUuid;
    }

    public String getNetworksDomainUuid() {
        return _networksDomainUuid;
    }

    public String getVmInstanceName() {
        return _vmInstanceName;
    }

    public String getVmUuid() {
        return _vmUuid;
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }

}
