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

public class TrashNetworkVspCommand extends Command {

    private final String _domainUuid;
    private final String _networkUuid;
    private final boolean _isL3Network;
    private final boolean _isSharedNetwork;
    private final String _vpcUuid;
    private final String _domainTemplateName;

    private TrashNetworkVspCommand(String domainUuid, String networkUuid, boolean isL3Network, boolean isSharedNetwork, String vpcUuid, String domainTemplateName) {
        super();
        this._domainUuid = domainUuid;
        this._networkUuid = networkUuid;
        this._isL3Network = isL3Network;
        this._isSharedNetwork = isSharedNetwork;
        this._vpcUuid = vpcUuid;
        this._domainTemplateName = domainTemplateName;
    }

    public String getDomainUuid() {
        return _domainUuid;
    }

    public String getNetworkUuid() {
        return _networkUuid;
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

    public String getDomainTemplateName() {
        return _domainTemplateName;
    }

    public static class Builder implements CmdBuilder<TrashNetworkVspCommand> {
        private String _domainUuid;
        private String _networkUuid;
        private boolean _isL3Network;
        private boolean _isSharedNetwork;
        private String _vpcUuid;
        private String _domainTemplateName;

        public Builder domainUuid(String domainUuid) {
            this._domainUuid = domainUuid;
            return this;
        }

        public Builder networkUuid(String networkUuid) {
            this._networkUuid = networkUuid;
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

        public Builder domainTemplateName(String domainTemplateName) {
            this._domainTemplateName = domainTemplateName;
            return this;
        }

        @Override
        public TrashNetworkVspCommand build() {
            return new TrashNetworkVspCommand(_domainUuid, _networkUuid, _isL3Network, _isSharedNetwork, _vpcUuid, _domainTemplateName);
        }
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TrashNetworkVspCommand)) return false;
        if (!super.equals(o)) return false;

        TrashNetworkVspCommand that = (TrashNetworkVspCommand) o;

        if (_isL3Network != that._isL3Network) return false;
        if (_isSharedNetwork != that._isSharedNetwork) return false;
        if (_domainTemplateName != null ? !_domainTemplateName.equals(that._domainTemplateName) : that._domainTemplateName != null)
            return false;
        if (_domainUuid != null ? !_domainUuid.equals(that._domainUuid) : that._domainUuid != null) return false;
        if (_networkUuid != null ? !_networkUuid.equals(that._networkUuid) : that._networkUuid != null) return false;
        if (_vpcUuid != null ? !_vpcUuid.equals(that._vpcUuid) : that._vpcUuid != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (_domainUuid != null ? _domainUuid.hashCode() : 0);
        result = 31 * result + (_networkUuid != null ? _networkUuid.hashCode() : 0);
        result = 31 * result + (_isL3Network ? 1 : 0);
        result = 31 * result + (_isSharedNetwork ? 1 : 0);
        result = 31 * result + (_vpcUuid != null ? _vpcUuid.hashCode() : 0);
        result = 31 * result + (_domainTemplateName != null ? _domainTemplateName.hashCode() : 0);
        return result;
    }
}
