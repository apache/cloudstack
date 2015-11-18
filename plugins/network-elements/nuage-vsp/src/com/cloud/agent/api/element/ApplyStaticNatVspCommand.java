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

package com.cloud.agent.api.element;

import com.cloud.agent.api.CmdBuilder;
import com.cloud.agent.api.Command;

import java.util.List;
import java.util.Map;

public class ApplyStaticNatVspCommand extends Command {

    private final String _networkDomainUuid;
    private final String _networkUuid;
    private final String _vpcOrSubnetUuid;
    private final boolean _isL3Network;
    private final boolean _isVpc;
    private final List<Map<String, Object>> _staticNatDetails;

    private ApplyStaticNatVspCommand(String networkDomainUuid, String networkUuid, String vpcOrSubnetUuid, boolean isL3Network, boolean isVpc,
            List<Map<String, Object>> staticNatDetails) {
        super();
        this._networkDomainUuid = networkDomainUuid;
        this._networkUuid = networkUuid;
        this._vpcOrSubnetUuid = vpcOrSubnetUuid;
        this._isL3Network = isL3Network;
        this._isVpc = isVpc;
        this._staticNatDetails = staticNatDetails;
    }

    public String getNetworkDomainUuid() {
        return _networkDomainUuid;
    }

    public String getNetworkUuid() {
        return _networkUuid;
    }

    public String getVpcOrSubnetUuid() {
        return _vpcOrSubnetUuid;
    }

    public boolean isL3Network() {
        return _isL3Network;
    }

    public boolean isVpc() {
        return _isVpc;
    }

    public List<Map<String, Object>> getStaticNatDetails() {
        return _staticNatDetails;
    }

    public static class Builder implements CmdBuilder<ApplyStaticNatVspCommand> {
        private String _networkDomainUuid;
        private String _networkUuid;
        private String _vpcOrSubnetUuid;
        private boolean _isL3Network;
        private boolean _isVpc;
        private List<Map<String, Object>> _staticNatDetails;

        public Builder networkDomainUuid(String networkDomainUuid) {
            this._networkDomainUuid = networkDomainUuid;
            return this;
        }

        public Builder networkUuid(String networkUuid) {
            this._networkUuid = networkUuid;
            return this;
        }

        public Builder vpcOrSubnetUuid(String vpcOrSubnetUuid) {
            this._vpcOrSubnetUuid = vpcOrSubnetUuid;
            return this;
        }

        public Builder isL3Network(boolean isL3Network) {
            this._isL3Network = isL3Network;
            return this;
        }

        public Builder isVpc(boolean isVpc) {
            this._isVpc = isVpc;
            return this;
        }

        public Builder staticNatDetails(List<Map<String, Object>> staticNatDetails) {
            this._staticNatDetails = staticNatDetails;
            return this;
        }

        @Override
        public ApplyStaticNatVspCommand build() {
            return new ApplyStaticNatVspCommand(_networkDomainUuid, _networkUuid, _vpcOrSubnetUuid, _isL3Network, _isVpc, _staticNatDetails);
        }
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ApplyStaticNatVspCommand)) return false;
        if (!super.equals(o)) return false;

        ApplyStaticNatVspCommand that = (ApplyStaticNatVspCommand) o;

        if (_isL3Network != that._isL3Network) return false;
        if (_isVpc != that._isVpc) return false;
        if (_networkDomainUuid != null ? !_networkDomainUuid.equals(that._networkDomainUuid) : that._networkDomainUuid != null)
            return false;
        if (_networkUuid != null ? !_networkUuid.equals(that._networkUuid) : that._networkUuid != null) return false;
        if (_staticNatDetails != null ? !_staticNatDetails.equals(that._staticNatDetails) : that._staticNatDetails != null)
            return false;
        if (_vpcOrSubnetUuid != null ? !_vpcOrSubnetUuid.equals(that._vpcOrSubnetUuid) : that._vpcOrSubnetUuid != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (_networkDomainUuid != null ? _networkDomainUuid.hashCode() : 0);
        result = 31 * result + (_networkUuid != null ? _networkUuid.hashCode() : 0);
        result = 31 * result + (_vpcOrSubnetUuid != null ? _vpcOrSubnetUuid.hashCode() : 0);
        result = 31 * result + (_isL3Network ? 1 : 0);
        result = 31 * result + (_isVpc ? 1 : 0);
        result = 31 * result + (_staticNatDetails != null ? _staticNatDetails.hashCode() : 0);
        return result;
    }
}
