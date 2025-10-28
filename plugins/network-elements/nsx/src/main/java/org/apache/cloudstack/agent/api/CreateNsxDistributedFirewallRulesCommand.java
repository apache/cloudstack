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
package org.apache.cloudstack.agent.api;

import org.apache.cloudstack.resource.NsxNetworkRule;

import java.util.List;
import java.util.Objects;

public class CreateNsxDistributedFirewallRulesCommand extends NsxCommand {

    private Long vpcId;
    private long networkId;
    private List<NsxNetworkRule> rules;

    public CreateNsxDistributedFirewallRulesCommand(long domainId, long accountId, long zoneId,
                                                    Long vpcId, long networkId,
                                                    List<NsxNetworkRule> rules) {
        super(domainId, accountId, zoneId);
        this.vpcId = vpcId;
        this.networkId = networkId;
        this.rules = rules;
    }

    public Long getVpcId() {
        return vpcId;
    }

    public long getNetworkId() {
        return networkId;
    }

    public List<NsxNetworkRule> getRules() {
        return rules;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass() || !super.equals(o)) {
            return false;
        }
        CreateNsxDistributedFirewallRulesCommand that = (CreateNsxDistributedFirewallRulesCommand) o;
        return networkId == that.networkId && Objects.equals(vpcId, that.vpcId) && Objects.equals(rules, that.rules);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), vpcId, networkId, rules);
    }
}
