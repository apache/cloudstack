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

public class ImplementVspCommand extends Command {

    private final long _networkId;
    private final String _networkDomainUuid;
    private final String _networkUuid;
    private final String _networkName;
    private final String _vpcOrSubnetUuid;
    private final boolean _isL2Network;
    private final boolean _isL3Network;
    private final boolean _isVpc;
    private final boolean _isShared;
    private final String _domainTemplateName;
    private final boolean _isFirewallServiceSupported;
    private final List<String> _dnsServers;
    private final List<Map<String, Object>> _ingressFirewallRules;
    private final List<Map<String, Object>> _egressFirewallRules;
    private final List<String> _acsFipUuid;
    private final boolean _egressDefaultPolicy;

    private ImplementVspCommand(long networkId, String networkDomainUuid, String networkUuid, String networkName, String vpcOrSubnetUuid, boolean isL2Network, boolean isL3Network,
            boolean isVpc, boolean isShared, String domainTemplateName, boolean isFirewallServiceSupported, List<String> dnsServers, List<Map<String, Object>> ingressFirewallRules,
            List<Map<String, Object>> egressFirewallRules, List<String> acsFipUuid, boolean egressDefaultPolicy) {
        super();
        this._networkId = networkId;
        this._networkDomainUuid = networkDomainUuid;
        this._networkUuid = networkUuid;
        this._networkName = networkName;
        this._vpcOrSubnetUuid = vpcOrSubnetUuid;
        this._isL2Network = isL2Network;
        this._isL3Network = isL3Network;
        this._isVpc = isVpc;
        this._isShared = isShared;
        this._domainTemplateName = domainTemplateName;
        this._isFirewallServiceSupported = isFirewallServiceSupported;
        this._dnsServers = dnsServers;
        this._ingressFirewallRules = ingressFirewallRules;
        this._egressFirewallRules = egressFirewallRules;
        this._acsFipUuid = acsFipUuid;
        this._egressDefaultPolicy = egressDefaultPolicy;
    }

    public long getNetworkId() {
        return _networkId;
    }

    public String getNetworkDomainUuid() {
        return _networkDomainUuid;
    }

    public String getNetworkUuid() {
        return _networkUuid;
    }

    public String getNetworkName() {
        return _networkName;
    }

    public String getVpcOrSubnetUuid() {
        return _vpcOrSubnetUuid;
    }

    public boolean isL2Network() {
        return _isL2Network;
    }

    public boolean isL3Network() {
        return _isL3Network;
    }

    public boolean isVpc() {
        return _isVpc;
    }

    public boolean isShared() {
        return _isShared;
    }

    public String getDomainTemplateName() {
        return _domainTemplateName;
    }

    public boolean isFirewallServiceSupported() {
        return _isFirewallServiceSupported;
    }

    public List<String> getDnsServers() {
        return _dnsServers;
    }

    public List<Map<String, Object>> getIngressFirewallRules() {
        return _ingressFirewallRules;
    }

    public List<Map<String, Object>> getEgressFirewallRules() {
        return _egressFirewallRules;
    }

    public List<String> getAcsFipUuid() {
        return _acsFipUuid;
    }

    public boolean isEgressDefaultPolicy() {
        return _egressDefaultPolicy;
    }

    public static class Builder implements CmdBuilder<ImplementVspCommand> {
        private long _networkId;
        private String _networkDomainUuid;
        private String _networkUuid;
        private String _networkName;
        private String _vpcOrSubnetUuid;
        private boolean _isL2Network;
        private boolean _isL3Network;
        private boolean _isVpc;
        private boolean _isShared;
        private String _domainTemplateName;
        private boolean _isFirewallServiceSupported;
        private List<String> _dnsServers;
        private List<Map<String, Object>> _ingressFirewallRules;
        private List<Map<String, Object>> _egressFirewallRules;
        private List<String> _acsFipUuid;
        private boolean _egressDefaultPolicy;

        public Builder networkId(long networkId) {
            this._networkId = networkId;
            return this;
        }

        public Builder networkDomainUuid(String networkDomainUuid) {
            this._networkDomainUuid = networkDomainUuid;
            return this;
        }

        public Builder networkUuid(String networkUuid) {
            this._networkUuid = networkUuid;
            return this;
        }

        public Builder networkName(String networkName) {
            this._networkName = networkName;
            return this;
        }

        public Builder vpcOrSubnetUuid(String vpcOrSubnetUuid) {
            this._vpcOrSubnetUuid = vpcOrSubnetUuid;
            return this;
        }

        public Builder isL2Network(boolean isL2Network) {
            this._isL2Network = isL2Network;
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

        public Builder isShared(boolean isShared) {
            this._isShared = isShared;
            return this;
        }

        public Builder domainTemplateName(String domainTemplateName) {
            this._domainTemplateName = domainTemplateName;
            return this;
        }

        public Builder isFirewallServiceSupported(boolean isFirewallServiceSupported) {
            this._isFirewallServiceSupported = isFirewallServiceSupported;
            return this;
        }

        public Builder dnsServers(List<String> dnsServers) {
            this._dnsServers = dnsServers;
            return this;
        }

        public Builder ingressFirewallRules(List<Map<String, Object>> ingressFirewallRules) {
            this._ingressFirewallRules = ingressFirewallRules;
            return this;
        }

        public Builder egressFirewallRules(List<Map<String, Object>> egressFirewallRules) {
            this._egressFirewallRules = egressFirewallRules;
            return this;
        }

        public Builder acsFipUuid(List<String> acsFipUuid) {
            this._acsFipUuid = acsFipUuid;
            return this;
        }

        public Builder egressDefaultPolicy(boolean egressDefaultPolicy) {
            this._egressDefaultPolicy = egressDefaultPolicy;
            return this;
        }

        @Override
        public ImplementVspCommand build() {
            return new ImplementVspCommand(_networkId, _networkDomainUuid, _networkUuid, _networkName, _vpcOrSubnetUuid, _isL2Network, _isL3Network, _isVpc, _isShared,
                    _domainTemplateName, _isFirewallServiceSupported, _dnsServers, _ingressFirewallRules, _egressFirewallRules, _acsFipUuid, _egressDefaultPolicy);
        }
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ImplementVspCommand)) return false;
        if (!super.equals(o)) return false;

        ImplementVspCommand that = (ImplementVspCommand) o;

        if (_egressDefaultPolicy != that._egressDefaultPolicy) return false;
        if (_isFirewallServiceSupported != that._isFirewallServiceSupported) return false;
        if (_isL2Network != that._isL2Network) return false;
        if (_isL3Network != that._isL3Network) return false;
        if (_isShared != that._isShared) return false;
        if (_isVpc != that._isVpc) return false;
        if (_networkId != that._networkId) return false;
        if (_acsFipUuid != null ? !_acsFipUuid.equals(that._acsFipUuid) : that._acsFipUuid != null) return false;
        if (_dnsServers != null ? !_dnsServers.equals(that._dnsServers) : that._dnsServers != null) return false;
        if (_domainTemplateName != null ? !_domainTemplateName.equals(that._domainTemplateName) : that._domainTemplateName != null)
            return false;
        if (_egressFirewallRules != null ? !_egressFirewallRules.equals(that._egressFirewallRules) : that._egressFirewallRules != null)
            return false;
        if (_ingressFirewallRules != null ? !_ingressFirewallRules.equals(that._ingressFirewallRules) : that._ingressFirewallRules != null)
            return false;
        if (_networkDomainUuid != null ? !_networkDomainUuid.equals(that._networkDomainUuid) : that._networkDomainUuid != null)
            return false;
        if (_networkName != null ? !_networkName.equals(that._networkName) : that._networkName != null) return false;
        if (_networkUuid != null ? !_networkUuid.equals(that._networkUuid) : that._networkUuid != null) return false;
        if (_vpcOrSubnetUuid != null ? !_vpcOrSubnetUuid.equals(that._vpcOrSubnetUuid) : that._vpcOrSubnetUuid != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (int) (_networkId ^ (_networkId >>> 32));
        result = 31 * result + (_networkDomainUuid != null ? _networkDomainUuid.hashCode() : 0);
        result = 31 * result + (_networkUuid != null ? _networkUuid.hashCode() : 0);
        result = 31 * result + (_networkName != null ? _networkName.hashCode() : 0);
        result = 31 * result + (_vpcOrSubnetUuid != null ? _vpcOrSubnetUuid.hashCode() : 0);
        result = 31 * result + (_isL2Network ? 1 : 0);
        result = 31 * result + (_isL3Network ? 1 : 0);
        result = 31 * result + (_isVpc ? 1 : 0);
        result = 31 * result + (_isShared ? 1 : 0);
        result = 31 * result + (_domainTemplateName != null ? _domainTemplateName.hashCode() : 0);
        result = 31 * result + (_isFirewallServiceSupported ? 1 : 0);
        result = 31 * result + (_dnsServers != null ? _dnsServers.hashCode() : 0);
        result = 31 * result + (_ingressFirewallRules != null ? _ingressFirewallRules.hashCode() : 0);
        result = 31 * result + (_egressFirewallRules != null ? _egressFirewallRules.hashCode() : 0);
        result = 31 * result + (_acsFipUuid != null ? _acsFipUuid.hashCode() : 0);
        result = 31 * result + (_egressDefaultPolicy ? 1 : 0);
        return result;
    }
}
