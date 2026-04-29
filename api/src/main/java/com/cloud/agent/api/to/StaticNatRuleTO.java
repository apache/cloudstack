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
package com.cloud.agent.api.to;

import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.FirewallRule.State;
import com.cloud.network.rules.StaticNatRule;

/**
 * StaticNatRuleTO specifies one static nat rule.
 *
 *
 */

public class StaticNatRuleTO extends FirewallRuleTO {
    String dstIp;

    protected StaticNatRuleTO() {
    }

    public StaticNatRuleTO(StaticNatRule rule, String srcVlanTag, String srcIp, String dstIp) {
        super(rule.getId(),
            srcVlanTag,
            srcIp,
            rule.getProtocol(),
            rule.getSourcePortStart(),
            rule.getSourcePortEnd(),
            rule.getState() == State.Revoke,
            rule.getState() == State.Active,
            rule.getPurpose(),
            null,
            0,
            0);
        this.dstIp = dstIp;
    }

    public StaticNatRuleTO(StaticNatRule rule, String scrIp, String dstIp) {
        super(rule.getId(),
            scrIp,
            rule.getProtocol(),
            rule.getSourcePortStart(),
            rule.getSourcePortEnd(),
            rule.getState() == State.Revoke,
            rule.getState() == State.Active,
            rule.getPurpose(),
            null,
            0,
            0);
        this.dstIp = dstIp;
    }

    public StaticNatRuleTO(long id, String srcIp, Integer srcPortStart, Integer srcPortEnd, String dstIp, Integer dstPortStart, Integer dstPortEnd, String protocol,
            boolean revoked, boolean alreadyAdded) {
        super(id, srcIp, protocol, srcPortStart, srcPortEnd, revoked, alreadyAdded, FirewallRule.Purpose.StaticNat, null, 0, 0);
        this.dstIp = dstIp;
    }

    public StaticNatRuleTO(long id, String srcVlanTag, String srcIp, Integer srcPortStart, Integer srcPortEnd, String dstIp, Integer dstPortStart, Integer dstPortEnd,
            String protocol, boolean revoked, boolean alreadyAdded) {
        super(id, srcVlanTag, srcIp, protocol, srcPortStart, srcPortEnd, revoked, alreadyAdded, FirewallRule.Purpose.StaticNat, null, 0, 0);
        this.dstIp = dstIp;
    }

    public String getDstIp() {
        return dstIp;
    }

}
