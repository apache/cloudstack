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

public class ImplementVspCommand extends Command {

    private final VspNetwork _network;
    private final List<String> _dnsServers;
    private final List<VspAclRule> _ingressFirewallRules;
    private final List<VspAclRule> _egressFirewallRules;
    private final List<String> _floatingIpUuids;

    public ImplementVspCommand(VspNetwork network, List<String> dnsServers, List<VspAclRule> ingressFirewallRules,
            List<VspAclRule> egressFirewallRules, List<String> floatingIpUuids) {
        super();
        this._network = network;
        this._dnsServers = dnsServers;
        this._ingressFirewallRules = ingressFirewallRules;
        this._egressFirewallRules = egressFirewallRules;
        this._floatingIpUuids = floatingIpUuids;
    }

    public VspNetwork getNetwork() {
        return _network;
    }

    public List<String> getDnsServers() {
        return _dnsServers;
    }

    public List<VspAclRule> getIngressFirewallRules() {
        return _ingressFirewallRules;
    }

    public List<VspAclRule> getEgressFirewallRules() {
        return _egressFirewallRules;
    }

    public List<String> getFloatingIpUuids() {
        return _floatingIpUuids;
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

        if (_dnsServers != null ? !_dnsServers.equals(that._dnsServers) : that._dnsServers != null) return false;
        if (_egressFirewallRules != null ? !_egressFirewallRules.equals(that._egressFirewallRules) : that._egressFirewallRules != null)
            return false;
        if (_floatingIpUuids != null ? !_floatingIpUuids.equals(that._floatingIpUuids) : that._floatingIpUuids != null)
            return false;
        if (_ingressFirewallRules != null ? !_ingressFirewallRules.equals(that._ingressFirewallRules) : that._ingressFirewallRules != null)
            return false;
        if (_network != null ? !_network.equals(that._network) : that._network != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (_network != null ? _network.hashCode() : 0);
        result = 31 * result + (_dnsServers != null ? _dnsServers.hashCode() : 0);
        result = 31 * result + (_ingressFirewallRules != null ? _ingressFirewallRules.hashCode() : 0);
        result = 31 * result + (_egressFirewallRules != null ? _egressFirewallRules.hashCode() : 0);
        result = 31 * result + (_floatingIpUuids != null ? _floatingIpUuids.hashCode() : 0);
        return result;
    }
}
