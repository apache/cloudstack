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

import com.cloud.agent.api.CmdBuilder;
import com.cloud.agent.api.Command;

public class DeallocateVmVspCommand extends Command {

    private final String _networkUuid;
    private final String _nicFromDdUuid;
    private final String _nicMacAddress;
    private final String _nicIp4Address;
    private final boolean _isL3Network;
    private final boolean _isSharedNetwork;
    private final String _vpcUuid;
    private final String _networksDomainUuid;
    private final String _vmInstanceName;
    private final String _vmUuid;
    private final boolean _isExpungingState;

    private DeallocateVmVspCommand(String networkUuid, String nicFromDdUuid, String nicMacAddress, String nicIp4Address, boolean isL3Network, boolean isSharedNetwork, String vpcUuid,
            String networksDomainUuid, String vmInstanceName, String vmUuid, boolean isExpungingState) {
        super();
        this._networkUuid = networkUuid;
        this._nicFromDdUuid = nicFromDdUuid;
        this._nicMacAddress = nicMacAddress;
        this._nicIp4Address = nicIp4Address;
        this._isL3Network = isL3Network;
        this._isSharedNetwork = isSharedNetwork;
        this._vpcUuid = vpcUuid;
        this._networksDomainUuid = networksDomainUuid;
        this._vmInstanceName = vmInstanceName;
        this._vmUuid = vmUuid;
        this._isExpungingState = isExpungingState;
    }

    public String getNetworkUuid() {
        return _networkUuid;
    }

    public String getNicFromDdUuid() {
        return _nicFromDdUuid;
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

    public boolean isSharedNetwork() {
        return _isSharedNetwork;
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

    public boolean isExpungingState() {
        return _isExpungingState;
    }

    public static class Builder implements CmdBuilder<DeallocateVmVspCommand> {
        private String _networkUuid;
        private String _nicFromDdUuid;
        private String _nicMacAddress;
        private String _nicIp4Address;
        private boolean _isL3Network;
        private boolean _isSharedNetwork;
        private String _vpcUuid;
        private String _networksDomainUuid;
        private String _vmInstanceName;
        private String _vmUuid;
        private boolean _isExpungingState;

        public Builder networkUuid(String networkUuid) {
            this._networkUuid = networkUuid;
            return this;
        }

        public Builder nicFromDbUuid(String nicFromDbUuid) {
            this._nicFromDdUuid = nicFromDbUuid;
            return this;
        }

        public Builder nicMacAddress(String nicMacAddress) {
            this._nicMacAddress = nicMacAddress;
            return this;
        }

        public Builder nicIp4Address(String nicIp4Address) {
            this._nicIp4Address = nicIp4Address;
            return this;
        }

        public Builder isL3Network(boolean isL3Network) {
            this._isL3Network = isL3Network;
            return this;
        }

        public Builder isSharedNetwork(boolean isSharedNetwork) {
            this._isSharedNetwork = isSharedNetwork;
            return this;
        }

        public Builder vpcUuid(String vpcUuid) {
            this._vpcUuid = vpcUuid;
            return this;
        }

        public Builder networksDomainUuid(String networksDomainUuid) {
            this._networksDomainUuid = networksDomainUuid;
            return this;
        }

        public Builder vmInstanceName(String vmInstanceName) {
            this._vmInstanceName = vmInstanceName;
            return this;
        }

        public Builder vmUuid(String vmUuid) {
            this._vmUuid = vmUuid;
            return this;
        }

        public Builder isExpungingState(boolean isExpungingState) {
            this._isExpungingState = isExpungingState;
            return this;
        }

        @Override
        public DeallocateVmVspCommand build() {
            return new DeallocateVmVspCommand(_networkUuid,_nicFromDdUuid, _nicMacAddress, _nicIp4Address, _isL3Network, _isSharedNetwork, _vpcUuid,
                    _networksDomainUuid, _vmInstanceName, _vmUuid, _isExpungingState);
        }
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DeallocateVmVspCommand)) return false;
        if (!super.equals(o)) return false;

        DeallocateVmVspCommand that = (DeallocateVmVspCommand) o;

        if (_isExpungingState != that._isExpungingState) return false;
        if (_isL3Network != that._isL3Network) return false;
        if (_isSharedNetwork != that._isSharedNetwork) return false;
        if (_networkUuid != null ? !_networkUuid.equals(that._networkUuid) : that._networkUuid != null) return false;
        if (_networksDomainUuid != null ? !_networksDomainUuid.equals(that._networksDomainUuid) : that._networksDomainUuid != null)
            return false;
        if (_nicFromDdUuid != null ? !_nicFromDdUuid.equals(that._nicFromDdUuid) : that._nicFromDdUuid != null)
            return false;
        if (_nicIp4Address != null ? !_nicIp4Address.equals(that._nicIp4Address) : that._nicIp4Address != null)
            return false;
        if (_nicMacAddress != null ? !_nicMacAddress.equals(that._nicMacAddress) : that._nicMacAddress != null)
            return false;
        if (_vmInstanceName != null ? !_vmInstanceName.equals(that._vmInstanceName) : that._vmInstanceName != null)
            return false;
        if (_vmUuid != null ? !_vmUuid.equals(that._vmUuid) : that._vmUuid != null) return false;
        if (_vpcUuid != null ? !_vpcUuid.equals(that._vpcUuid) : that._vpcUuid != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (_networkUuid != null ? _networkUuid.hashCode() : 0);
        result = 31 * result + (_nicFromDdUuid != null ? _nicFromDdUuid.hashCode() : 0);
        result = 31 * result + (_nicMacAddress != null ? _nicMacAddress.hashCode() : 0);
        result = 31 * result + (_nicIp4Address != null ? _nicIp4Address.hashCode() : 0);
        result = 31 * result + (_isL3Network ? 1 : 0);
        result = 31 * result + (_isSharedNetwork ? 1 : 0);
        result = 31 * result + (_vpcUuid != null ? _vpcUuid.hashCode() : 0);
        result = 31 * result + (_networksDomainUuid != null ? _networksDomainUuid.hashCode() : 0);
        result = 31 * result + (_vmInstanceName != null ? _vmInstanceName.hashCode() : 0);
        result = 31 * result + (_vmUuid != null ? _vmUuid.hashCode() : 0);
        result = 31 * result + (_isExpungingState ? 1 : 0);
        return result;
    }
}
