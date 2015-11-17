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

import java.util.ArrayList;
import java.util.List;

import org.apache.cloudstack.api.InternalIdentity;

import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.FirewallRule.State;
import com.cloud.utils.net.NetUtils;

/**
 * FirewallRuleTO transfers a port range for an ip to be opened.
 *
 * There are essentially three states transferred with each state.
 *      sent multiple times to the destination.  If the rule is not on
 *   2. alreadyAdded - the rule has been successfully added before.  Rules
 *      in this state are sent for completeness and optimization.
 *      If the rule already exists on the destination, the destination should
 *      reply the rule is successfully applied.
 *
 *   - srcPortRange: port range to open.
 *   - protocol: protocol to open for.  Usually tcp and udp.
 *
 */
public class FirewallRuleTO implements InternalIdentity {
    Long id;
    String srcVlanTag;
    String srcIp;
    String protocol;
    Integer[] srcPortRange;
    Boolean revoked;
    Boolean alreadyAdded;
    private List<String> sourceCidrList;
    FirewallRule.Purpose purpose;
    private Integer icmpType;
    private Integer icmpCode;
    private FirewallRule.TrafficType trafficType;
    private String guestCidr;
    private Boolean defaultEgressPolicy = false;
    private FirewallRule.FirewallRuleType type;

    protected FirewallRuleTO() {
    }

    public FirewallRuleTO(Long id, String srcIp, String protocol, Integer srcPortStart, Integer srcPortEnd, Boolean revoked, Boolean alreadyAdded,
            FirewallRule.Purpose purpose, List<String> sourceCidr, Integer icmpType, Integer icmpCode) {
        this(id, null, srcIp, protocol, srcPortStart, srcPortEnd, revoked, alreadyAdded, purpose, sourceCidr, icmpType, icmpCode);
    }

    public FirewallRuleTO(Long id, String srcVlanTag, String srcIp, String protocol, Integer srcPortStart, Integer srcPortEnd, Boolean revoked, Boolean alreadyAdded,
            FirewallRule.Purpose purpose, List<String> sourceCidr, Integer icmpType, Integer icmpCode) {
        this.id = id;
        this.srcVlanTag = srcVlanTag;
        this.srcIp = srcIp;
        this.protocol = protocol;

        if (srcPortStart != null) {
            List<Integer> portRange = new ArrayList<Integer>();
            portRange.add(srcPortStart);
            if (srcPortEnd != null) {
                portRange.add(srcPortEnd);
            }

            srcPortRange = new Integer[portRange.size()];
            int i = 0;
            for (Integer port : portRange) {
                srcPortRange[i] = port;
                i++;
            }
        }

        this.revoked = revoked;
        this.alreadyAdded = alreadyAdded;
        this.purpose = purpose;
        this.sourceCidrList = sourceCidr;
        this.icmpType = icmpType;
        this.icmpCode = icmpCode;
        this.trafficType = null;
    }

    public FirewallRuleTO(FirewallRule rule, String srcVlanTag, String srcIp) {
        this(rule.getId(),
            srcVlanTag,
            srcIp,
            rule.getProtocol(),
            rule.getSourcePortStart(),
            rule.getSourcePortEnd(),
            rule.getState() == State.Revoke,
            rule.getState() == State.Active,
            rule.getPurpose(),
            rule.getSourceCidrList(),
            rule.getIcmpType(),
            rule.getIcmpCode());
    }

    public FirewallRuleTO(FirewallRule rule, String srcIp) {
        this(rule.getId(),
            null,
            srcIp,
            rule.getProtocol(),
            rule.getSourcePortStart(),
            rule.getSourcePortEnd(),
            rule.getState() == State.Revoke,
            rule.getState() == State.Active,
            rule.getPurpose(),
            rule.getSourceCidrList(),
            rule.getIcmpType(),
            rule.getIcmpCode());
    }

    public FirewallRuleTO(FirewallRule rule, String srcVlanTag, String srcIp, FirewallRule.Purpose purpose) {
        this(rule.getId(),
            srcVlanTag,
            srcIp,
            rule.getProtocol(),
            rule.getSourcePortStart(),
            rule.getSourcePortEnd(),
            rule.getState() == State.Revoke,
            rule.getState() == State.Active,
            purpose,
            rule.getSourceCidrList(),
            rule.getIcmpType(),
            rule.getIcmpCode());
    }

    public FirewallRuleTO(FirewallRule rule, String srcVlanTag, String srcIp, FirewallRule.Purpose purpose, FirewallRule.TrafficType trafficType) {
        this(rule.getId(),
            srcVlanTag,
            srcIp,
            rule.getProtocol(),
            rule.getSourcePortStart(),
            rule.getSourcePortEnd(),
            rule.getState() == State.Revoke,
            rule.getState() == State.Active,
            purpose,
            rule.getSourceCidrList(),
            rule.getIcmpType(),
            rule.getIcmpCode());
        this.trafficType = trafficType;
    }

    public FirewallRuleTO(FirewallRule rule, String srcVlanTag, String srcIp, FirewallRule.Purpose purpose, FirewallRule.TrafficType trafficType,
            Boolean defaultEgressPolicy) {
        this(rule.getId(),
            srcVlanTag,
            srcIp,
            rule.getProtocol(),
            rule.getSourcePortStart(),
            rule.getSourcePortEnd(),
            rule.getState() == State.Revoke,
            rule.getState() == State.Active,
            purpose,
            rule.getSourceCidrList(),
            rule.getIcmpType(),
            rule.getIcmpCode());
        this.trafficType = trafficType;
        this.defaultEgressPolicy = defaultEgressPolicy;
    }

    public FirewallRuleTO(FirewallRule rule, String srcVlanTag, String srcIp, FirewallRule.Purpose purpose, Boolean revokeState, Boolean alreadyAdded) {
        this(rule.getId(),
            srcVlanTag,
            srcIp,
            rule.getProtocol(),
            rule.getSourcePortStart(),
            rule.getSourcePortEnd(),
            revokeState,
            alreadyAdded,
            purpose,
            rule.getSourceCidrList(),
            rule.getIcmpType(),
            rule.getIcmpCode());
    }

    public FirewallRuleTO(FirewallRule rule, String guestVlanTag, FirewallRule.TrafficType trafficType, String guestCidr, Boolean defaultEgressPolicy,
            FirewallRule.FirewallRuleType type) {
        this(rule.getId(),
            guestVlanTag,
            null,
            rule.getProtocol(),
            rule.getSourcePortStart(),
            rule.getSourcePortEnd(),
            rule.getState() == State.Revoke,
            rule.getState() == State.Active,
            rule.getPurpose(),
            rule.getSourceCidrList(),
            rule.getIcmpType(),
            rule.getIcmpCode());
        this.trafficType = trafficType;
        this.defaultEgressPolicy = defaultEgressPolicy;
        this.guestCidr = guestCidr;
        this.type = type;
    }

    public FirewallRule.TrafficType getTrafficType() {
        return trafficType;
    }

    @Override
    public Long getId() {
        return id;
    }

    public String getSrcVlanTag() {
        return srcVlanTag;
    }

    public String getSrcIp() {
        return srcIp;
    }

    public String getProtocol() {
        return protocol;
    }

    public Integer[] getSrcPortRange() {
        return srcPortRange;
    }

    public Integer getIcmpType() {
        return icmpType;
    }

    public Integer getIcmpCode() {
        return icmpCode;
    }

    public String getStringSrcPortRange() {
        if (srcPortRange == null || srcPortRange.length < 2)
            return "0:0";
        else
            return NetUtils.portRangeToString(srcPortRange);
    }

    public Boolean revoked() {
        return revoked;
    }

    public List<String> getSourceCidrList() {
        return sourceCidrList;
    }

    public Boolean isAlreadyAdded() {
        return alreadyAdded;
    }

    public FirewallRule.Purpose getPurpose() {
        return purpose;
    }

    public Boolean isDefaultEgressPolicy() {
        return defaultEgressPolicy;
    }

    public String getGuestCidr() {
        return guestCidr;
    }

    public FirewallRule.FirewallRuleType getType() {
        return type;
    }
}
