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

import java.util.Collection;
import java.util.List;

public class ImplementNetworkVspCommand extends Command {

    private final String _networkDomainName;
    private final String _networkDomainPath;
    private final String _networkDomainUuid;
    private final String _networkAccountName;
    private final String _networkAccountUuid;
    private final String _networkName;
    private final String _networkCidr;
    private final String _networkGateway;
    private final Long _networkAclId;
    private final List<String> _dnsServers;
    private final List<String> _gatewaySystemIds;
    private final String _networkUuid;
    private final boolean _isL3Network;
    private final boolean _isVpc;
    private final boolean _isSharedNetwork;
    private final String _vpcName;
    private final String _vpcUuid;
    private final boolean _defaultEgressPolicy;
    private final List<String[]> _ipAddressRange;
    private final String _domainTemplateName;

    private ImplementNetworkVspCommand(String networkDomainName, String networkDomainPath, String networkDomainUuid, String networkAccountName, String networkAccountUuid,
            String networkName, String networkCidr, String networkGateway, Long networkAclId, List<String> dnsServers, List<String> gatewaySystemIds, String networkUuid,
            boolean isL3Network, boolean isVpc, boolean isSharedNetwork, String vpcName, String vpcUuid, boolean defaultEgressPolicy, List<String[]> ipAddressRange,
            String domainTemplateName) {
        super();
        this._networkDomainName = networkDomainName;
        this._networkDomainPath = networkDomainPath;
        this._networkDomainUuid = networkDomainUuid;
        this._networkAccountName = networkAccountName;
        this._networkAccountUuid = networkAccountUuid;
        this._networkName = networkName;
        this._networkCidr = networkCidr;
        this._networkGateway = networkGateway;
        this._networkAclId = networkAclId;
        this._dnsServers = dnsServers;
        this._gatewaySystemIds = gatewaySystemIds;
        this._networkUuid = networkUuid;
        this._isL3Network = isL3Network;
        this._isVpc = isVpc;
        this._isSharedNetwork = isSharedNetwork;
        this._vpcName = vpcName;
        this._vpcUuid = vpcUuid;
        this._defaultEgressPolicy = defaultEgressPolicy;
        this._ipAddressRange = ipAddressRange;
        this._domainTemplateName = domainTemplateName;
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

    public Long getNetworkAclId() {
        return _networkAclId;
    }

    public List<String> getDnsServers() {
        return _dnsServers;
    }

    public List<String> getGatewaySystemIds() {
        return _gatewaySystemIds;
    }

    public String getNetworkUuid() {
        return _networkUuid;
    }

    public boolean isL3Network() {
        return _isL3Network;
    }

    public boolean isVpc() {
        return _isVpc;
    }

    public boolean isSharedNetwork() {
        return _isSharedNetwork;
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

    public Collection<String[]> getIpAddressRange() {
        return _ipAddressRange;
    }

    public String getDomainTemplateName() {
        return _domainTemplateName;
    }

    public static class Builder implements CmdBuilder<ImplementNetworkVspCommand> {
        private String _networkDomainName;
        private String _networkDomainPath;
        private String _networkDomainUuid;
        private String _networkAccountName;
        private String _networkAccountUuid;
        private String _networkName;
        private String _networkCidr;
        private String _networkGateway;
        private Long _networkAclId;
        private List<String> _dnsServers;
        private List<String> _gatewaySystemIds;
        private String _networkUuid;
        private boolean _isL3Network;
        private boolean _isVpc;
        private boolean _isSharedNetwork;
        private String _vpcName;
        private String _vpcUuid;
        private boolean _defaultEgressPolicy;
        private List<String[]> _ipAddressRange;
        private String _domainTemplateName;

        public Builder networkDomainName(String networkDomainName) {
            this._networkDomainName = networkDomainName;
            return this;
        }

        public Builder networkDomainPath(String networkDomainPath) {
            this._networkDomainPath = networkDomainPath;
            return this;
        }

        public Builder networkDomainUuid(String networkDomainUuid) {
            this._networkDomainUuid = networkDomainUuid;
            return this;
        }

        public Builder networkAccountName(String networkAccountName) {
            this._networkAccountName = networkAccountName;
            return this;
        }

        public Builder networkAccountUuid(String networkAccountUuid) {
            this._networkAccountUuid = networkAccountUuid;
            return this;
        }

        public Builder networkName(String networkName) {
            this._networkName = networkName;
            return this;
        }

        public Builder networkCidr(String networkCidr) {
            this._networkCidr = networkCidr;
            return this;
        }

        public Builder networkGateway(String networkGateway) {
            this._networkGateway = networkGateway;
            return this;
        }

        public Builder networkAclId(Long networkAclId) {
            this._networkAclId = networkAclId;
            return this;
        }

        public Builder dnsServers(List<String> dnsServers) {
            this._dnsServers = dnsServers;
            return this;
        }

        public Builder gatewaySystemIds(List<String> gatewaySystemIds) {
            this._gatewaySystemIds = gatewaySystemIds;
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

        public Builder isVpc(boolean isVpc) {
            this._isVpc = isVpc;
            return this;
        }

        public Builder isSharedNetwork(boolean isSharedNetwork) {
            this._isSharedNetwork = isSharedNetwork;
            return this;
        }

        public Builder vpcName(String vpcName) {
            this._vpcName = vpcName;
            return this;
        }

        public Builder vpcUuid(String vpcUuid) {
            this._vpcUuid = vpcUuid;
            return this;
        }

        public Builder defaultEgressPolicy(boolean defaultEgressPolicy) {
            this._defaultEgressPolicy = defaultEgressPolicy;
            return this;
        }

        public Builder ipAddressRange(List<String[]> ipAddressRange) {
            this._ipAddressRange = ipAddressRange;
            return this;
        }

        public Builder domainTemplateName(String domainTemplateName) {
            this._domainTemplateName = domainTemplateName;
            return this;
        }

        @Override
        public ImplementNetworkVspCommand build() {
            return new ImplementNetworkVspCommand(_networkDomainName, _networkDomainPath, _networkDomainUuid, _networkAccountName, _networkAccountUuid, _networkName,
                    _networkCidr, _networkGateway, _networkAclId, _dnsServers, _gatewaySystemIds, _networkUuid, _isL3Network, _isVpc, _isSharedNetwork, _vpcName, _vpcUuid,
                    _defaultEgressPolicy, _ipAddressRange, _domainTemplateName);
        }
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ImplementNetworkVspCommand)) return false;
        if (!super.equals(o)) return false;

        ImplementNetworkVspCommand that = (ImplementNetworkVspCommand) o;

        if (_defaultEgressPolicy != that._defaultEgressPolicy) return false;
        if (_isL3Network != that._isL3Network) return false;
        if (_isSharedNetwork != that._isSharedNetwork) return false;
        if (_isVpc != that._isVpc) return false;
        if (_dnsServers != null ? !_dnsServers.equals(that._dnsServers) : that._dnsServers != null) return false;
        if (_domainTemplateName != null ? !_domainTemplateName.equals(that._domainTemplateName) : that._domainTemplateName != null)
            return false;
        if (_gatewaySystemIds != null ? !_gatewaySystemIds.equals(that._gatewaySystemIds) : that._gatewaySystemIds != null)
            return false;
        if (_ipAddressRange != null ? !_ipAddressRange.equals(that._ipAddressRange) : that._ipAddressRange != null)
            return false;
        if (_networkAccountName != null ? !_networkAccountName.equals(that._networkAccountName) : that._networkAccountName != null)
            return false;
        if (_networkAccountUuid != null ? !_networkAccountUuid.equals(that._networkAccountUuid) : that._networkAccountUuid != null)
            return false;
        if (_networkAclId != null ? !_networkAclId.equals(that._networkAclId) : that._networkAclId != null)
            return false;
        if (_networkCidr != null ? !_networkCidr.equals(that._networkCidr) : that._networkCidr != null) return false;
        if (_networkDomainName != null ? !_networkDomainName.equals(that._networkDomainName) : that._networkDomainName != null)
            return false;
        if (_networkDomainPath != null ? !_networkDomainPath.equals(that._networkDomainPath) : that._networkDomainPath != null)
            return false;
        if (_networkDomainUuid != null ? !_networkDomainUuid.equals(that._networkDomainUuid) : that._networkDomainUuid != null)
            return false;
        if (_networkGateway != null ? !_networkGateway.equals(that._networkGateway) : that._networkGateway != null)
            return false;
        if (_networkName != null ? !_networkName.equals(that._networkName) : that._networkName != null) return false;
        if (_networkUuid != null ? !_networkUuid.equals(that._networkUuid) : that._networkUuid != null) return false;
        if (_vpcName != null ? !_vpcName.equals(that._vpcName) : that._vpcName != null) return false;
        if (_vpcUuid != null ? !_vpcUuid.equals(that._vpcUuid) : that._vpcUuid != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (_networkDomainName != null ? _networkDomainName.hashCode() : 0);
        result = 31 * result + (_networkDomainPath != null ? _networkDomainPath.hashCode() : 0);
        result = 31 * result + (_networkDomainUuid != null ? _networkDomainUuid.hashCode() : 0);
        result = 31 * result + (_networkAccountName != null ? _networkAccountName.hashCode() : 0);
        result = 31 * result + (_networkAccountUuid != null ? _networkAccountUuid.hashCode() : 0);
        result = 31 * result + (_networkName != null ? _networkName.hashCode() : 0);
        result = 31 * result + (_networkCidr != null ? _networkCidr.hashCode() : 0);
        result = 31 * result + (_networkGateway != null ? _networkGateway.hashCode() : 0);
        result = 31 * result + (_networkAclId != null ? _networkAclId.hashCode() : 0);
        result = 31 * result + (_dnsServers != null ? _dnsServers.hashCode() : 0);
        result = 31 * result + (_gatewaySystemIds != null ? _gatewaySystemIds.hashCode() : 0);
        result = 31 * result + (_networkUuid != null ? _networkUuid.hashCode() : 0);
        result = 31 * result + (_isL3Network ? 1 : 0);
        result = 31 * result + (_isVpc ? 1 : 0);
        result = 31 * result + (_isSharedNetwork ? 1 : 0);
        result = 31 * result + (_vpcName != null ? _vpcName.hashCode() : 0);
        result = 31 * result + (_vpcUuid != null ? _vpcUuid.hashCode() : 0);
        result = 31 * result + (_defaultEgressPolicy ? 1 : 0);
        result = 31 * result + (_ipAddressRange != null ? _ipAddressRange.hashCode() : 0);
        result = 31 * result + (_domainTemplateName != null ? _domainTemplateName.hashCode() : 0);
        return result;
    }
}
