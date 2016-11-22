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

import com.cloud.agent.api.Command;
import net.nuage.vsp.acs.client.api.model.VspAclRule;
import net.nuage.vsp.acs.client.api.model.VspNetwork;

import java.util.List;

public class ApplyAclRuleVspCommand extends Command {

    private final VspAclRule.ACLType _aclType;
    private final VspNetwork _network;
    private final List<VspAclRule> _aclRules;
    private final boolean _networkReset;

    public ApplyAclRuleVspCommand(VspAclRule.ACLType aclType, VspNetwork network, List<VspAclRule> aclRules, boolean networkReset) {
        super();
        this._aclType = aclType;
        this._network = network;
        this._aclRules = aclRules;
        this._networkReset = networkReset;
    }

    public VspAclRule.ACLType getAclType() {
        return _aclType;
    }

    public VspNetwork getNetwork() {
        return _network;
    }

    public List<VspAclRule> getAclRules() {
        return _aclRules;
    }

    public boolean isNetworkReset() {
        return _networkReset;
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

        if (_networkReset != that._networkReset) return false;
        if (_aclRules != null ? !_aclRules.equals(that._aclRules) : that._aclRules != null) return false;
        if (_aclType != that._aclType) return false;
        if (_network != null ? !_network.equals(that._network) : that._network != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (_aclType != null ? _aclType.hashCode() : 0);
        result = 31 * result + (_network != null ? _network.hashCode() : 0);
        result = 31 * result + (_aclRules != null ? _aclRules.hashCode() : 0);
        result = 31 * result + (_networkReset ? 1 : 0);
        return result;
    }
}
