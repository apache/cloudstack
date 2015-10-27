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

public class ApplyAclRuleVspCommand extends Command {

    private final boolean _networkAcl;
    private final String _networkUuid;
    private final String _networkDomainUuid;
    private final String _vpcOrSubnetUuid;
    private final String _networkName;
    private final boolean _isL2Network;
    private final List<Map<String, Object>> _aclRules;
    private final long _networkId;
    private final boolean _egressDefaultPolicy;
    private final Boolean _acsIngressAcl;
    private final boolean _networkReset;
    private final String _domainTemplateName;

    private ApplyAclRuleVspCommand(boolean networkAcl, String networkUuid, String networkDomainUuid, String vpcOrSubnetUuid, String networkName, boolean isL2Network,
            List<Map<String, Object>> aclRules, long networkId, boolean egressDefaultPolicy, Boolean acsIngressAcl, boolean networkReset, String domainTemplateName) {
        super();
        this._networkAcl = networkAcl;
        this._networkUuid = networkUuid;
        this._networkDomainUuid = networkDomainUuid;
        this._vpcOrSubnetUuid = vpcOrSubnetUuid;
        this._networkName = networkName;
        this._isL2Network = isL2Network;
        this._aclRules = aclRules;
        this._networkId = networkId;
        this._egressDefaultPolicy = egressDefaultPolicy;
        this._acsIngressAcl = acsIngressAcl;
        this._networkReset = networkReset;
        this._domainTemplateName = domainTemplateName;
    }

    public boolean isNetworkAcl() {
        return _networkAcl;
    }

    public String getNetworkUuid() {
        return _networkUuid;
    }

    public String getNetworkDomainUuid() {
        return _networkDomainUuid;
    }

    public String getVpcOrSubnetUuid() {
        return _vpcOrSubnetUuid;
    }

    public String getNetworkName() {
        return _networkName;
    }

    public boolean isL2Network() {
        return _isL2Network;
    }

    public List<Map<String, Object>> getAclRules() {
        return _aclRules;
    }

    public long getNetworkId() {
        return _networkId;
    }

    public boolean isEgressDefaultPolicy() {
        return _egressDefaultPolicy;
    }

    public Boolean getAcsIngressAcl() {
        return _acsIngressAcl;
    }

    public boolean isNetworkReset() {
        return _networkReset;
    }

    public String getDomainTemplateName() {
        return _domainTemplateName;
    }

    public static class Builder implements CmdBuilder<ApplyAclRuleVspCommand> {
        private boolean _networkAcl;
        private String _networkUuid;
        private String _networkDomainUuid;
        private String _vpcOrSubnetUuid;
        private String _networkName;
        private boolean _isL2Network;
        private List<Map<String, Object>> _aclRules;
        private long _networkId;
        private boolean _egressDefaultPolicy;
        private Boolean _acsIngressAcl;
        private boolean _networkReset;
        private String _domainTemplateName;

        public Builder networkAcl(boolean networkAcl) {
            this._networkAcl = networkAcl;
            return this;
        }

        public Builder networkUuid(String networkUuid) {
            this._networkUuid = networkUuid;
            return this;
        }

        public Builder networkDomainUuid(String networkDomainUuid) {
            this._networkDomainUuid = networkDomainUuid;
            return this;
        }

        public Builder vpcOrSubnetUuid(String vpcOrSubnetUuid) {
            this._vpcOrSubnetUuid = vpcOrSubnetUuid;
            return this;
        }

        public Builder networkName(String networkName) {
            this._networkName = networkName;
            return this;
        }

        public Builder isL2Network(boolean isL2Network) {
            this._isL2Network = isL2Network;
            return this;
        }

        public Builder aclRules(List<Map<String, Object>> aclRules) {
            this._aclRules = aclRules;
            return this;
        }

        public Builder networkId(long networkId) {
            this._networkId = networkId;
            return this;
        }

        public Builder egressDefaultPolicy(boolean egressDefaultPolicy) {
            this._egressDefaultPolicy = egressDefaultPolicy;
            return this;
        }

        public Builder acsIngressAcl(Boolean acsIngressAcl) {
            this._acsIngressAcl = acsIngressAcl;
            return this;
        }

        public Builder networkReset(boolean networkReset) {
            this._networkReset = networkReset;
            return this;
        }

        public Builder domainTemplateName(String domainTemplateName) {
            this._domainTemplateName = domainTemplateName;
            return this;
        }

        @Override
        public ApplyAclRuleVspCommand build() {
            return new ApplyAclRuleVspCommand(_networkAcl, _networkUuid, _networkDomainUuid, _vpcOrSubnetUuid, _networkName, _isL2Network, _aclRules,
                    _networkId, _egressDefaultPolicy, _acsIngressAcl, _networkReset, _domainTemplateName);
        }
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ApplyAclRuleVspCommand)) return false;
        if (!super.equals(o)) return false;

        ApplyAclRuleVspCommand that = (ApplyAclRuleVspCommand) o;

        if (_egressDefaultPolicy != that._egressDefaultPolicy) return false;
        if (_isL2Network != that._isL2Network) return false;
        if (_networkAcl != that._networkAcl) return false;
        if (_networkId != that._networkId) return false;
        if (_networkReset != that._networkReset) return false;
        if (_aclRules != null ? !_aclRules.equals(that._aclRules) : that._aclRules != null) return false;
        if (_acsIngressAcl != null ? !_acsIngressAcl.equals(that._acsIngressAcl) : that._acsIngressAcl != null)
            return false;
        if (_domainTemplateName != null ? !_domainTemplateName.equals(that._domainTemplateName) : that._domainTemplateName != null)
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
        result = 31 * result + (_networkAcl ? 1 : 0);
        result = 31 * result + (_networkUuid != null ? _networkUuid.hashCode() : 0);
        result = 31 * result + (_networkDomainUuid != null ? _networkDomainUuid.hashCode() : 0);
        result = 31 * result + (_vpcOrSubnetUuid != null ? _vpcOrSubnetUuid.hashCode() : 0);
        result = 31 * result + (_networkName != null ? _networkName.hashCode() : 0);
        result = 31 * result + (_isL2Network ? 1 : 0);
        result = 31 * result + (_aclRules != null ? _aclRules.hashCode() : 0);
        result = 31 * result + (int) (_networkId ^ (_networkId >>> 32));
        result = 31 * result + (_egressDefaultPolicy ? 1 : 0);
        result = 31 * result + (_acsIngressAcl != null ? _acsIngressAcl.hashCode() : 0);
        result = 31 * result + (_networkReset ? 1 : 0);
        result = 31 * result + (_domainTemplateName != null ? _domainTemplateName.hashCode() : 0);
        return result;
    }
}
