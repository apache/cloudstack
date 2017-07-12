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

import java.util.List;
import java.util.Objects;

import net.nuage.vsp.acs.client.api.model.VspAclRule;
import net.nuage.vsp.acs.client.api.model.VspDhcpDomainOption;
import net.nuage.vsp.acs.client.api.model.VspNetwork;

import org.apache.commons.lang.builder.HashCodeBuilder;

import com.cloud.agent.api.Command;

public class ImplementVspCommand extends Command {

    private final VspNetwork _network;
    private final List<VspAclRule> _ingressFirewallRules;
    private final List<VspAclRule> _egressFirewallRules;
    private final List<String> _floatingIpUuids;
    private final VspDhcpDomainOption _dhcpOption;

    public ImplementVspCommand(VspNetwork network, List<VspAclRule> ingressFirewallRules,
                               List<VspAclRule> egressFirewallRules, List<String> floatingIpUuids, VspDhcpDomainOption dhcpOption) {
        super();
        this._network = network;
        this._ingressFirewallRules = ingressFirewallRules;
        this._egressFirewallRules = egressFirewallRules;
        this._floatingIpUuids = floatingIpUuids;
        this._dhcpOption = dhcpOption;
    }

    public VspNetwork getNetwork() {
        return _network;
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

    public VspDhcpDomainOption getDhcpOption() {
        return _dhcpOption;
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof ImplementVspCommand)) {
            return false;
        }

        ImplementVspCommand that = (ImplementVspCommand) o;

        return super.equals(that)
            && Objects.equals(_network, that._network)
            && Objects.equals(_dhcpOption, that._dhcpOption)
            && Objects.equals(_floatingIpUuids, that._floatingIpUuids)
            && Objects.equals(_ingressFirewallRules, that._ingressFirewallRules)
            && Objects.equals(_egressFirewallRules, that._egressFirewallRules);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .appendSuper(super.hashCode())
                .append(_network)
                .append(_dhcpOption)
                .toHashCode();
    }
}
