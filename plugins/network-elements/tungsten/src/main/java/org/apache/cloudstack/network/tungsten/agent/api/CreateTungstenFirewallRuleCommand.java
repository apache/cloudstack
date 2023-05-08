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
package org.apache.cloudstack.network.tungsten.agent.api;

import java.util.Objects;
import java.util.UUID;

public class CreateTungstenFirewallRuleCommand extends TungstenCommand {
    private final String uuid;
    private final String firewallPolicyUuid;
    private final String name;
    private final String action;
    private final String serviceGroupUuid;
    private final String srcTagUuid;
    private final String srcAddressGroupUuid;
    private final String srcNetworkUuid;
    private final String direction;
    private final String destTagUuid;
    private final String destAddressGroupUuid;
    private final String destNetworkUuid;
    private final String tagTypeUuid;
    private final int sequence;

    public CreateTungstenFirewallRuleCommand(final String firewallPolicyUuid, final String name, final String action, final String serviceGroupUuid,
        final String srcTagUuid, final String srcAddressGroupUuid, final String srcNetworkUuid, final String direction, final String destTagUuid,
        final String destAddressGroupUuid, final String destNetworkUuid, final String tagTypeUuid, final int sequence) {
        this.uuid = UUID.randomUUID().toString();
        this.firewallPolicyUuid = firewallPolicyUuid;
        this.name = name;
        this.action = action;
        this.serviceGroupUuid = serviceGroupUuid;
        this.srcTagUuid = srcTagUuid;
        this.srcAddressGroupUuid = srcAddressGroupUuid;
        this.srcNetworkUuid = srcNetworkUuid;
        this.direction = direction;
        this.destTagUuid = destTagUuid;
        this.destAddressGroupUuid = destAddressGroupUuid;
        this.destNetworkUuid = destNetworkUuid;
        this.tagTypeUuid = tagTypeUuid;
        this.sequence = sequence;
    }

    public String getUuid() {
        return uuid;
    }

    public String getFirewallPolicyUuid() {
        return firewallPolicyUuid;
    }

    public String getName() {
        return name;
    }

    public String getAction() {
        return action;
    }

    public String getServiceGroupUuid() {
        return serviceGroupUuid;
    }

    public String getSrcAddressGroupUuid() {
        return srcAddressGroupUuid;
    }

    public String getSrcNetworkUuid() {
        return srcNetworkUuid;
    }

    public String getDirection() {
        return direction;
    }

    public String getDestAddressGroupUuid() {
        return destAddressGroupUuid;
    }

    public String getDestNetworkUuid() {
        return destNetworkUuid;
    }

    public String getTagTypeUuid() {
        return tagTypeUuid;
    }

    public String getSrcTagUuid() {
        return srcTagUuid;
    }

    public String getDestTagUuid() {
        return destTagUuid;
    }

    public int getSequence() {
        return sequence;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        CreateTungstenFirewallRuleCommand that = (CreateTungstenFirewallRuleCommand) o;
        return sequence == that.sequence && Objects.equals(uuid, that.uuid) && Objects.equals(firewallPolicyUuid, that.firewallPolicyUuid) && Objects.equals(name, that.name) && Objects.equals(action, that.action) && Objects.equals(serviceGroupUuid, that.serviceGroupUuid) && Objects.equals(srcTagUuid, that.srcTagUuid) && Objects.equals(srcAddressGroupUuid, that.srcAddressGroupUuid) && Objects.equals(srcNetworkUuid, that.srcNetworkUuid) && Objects.equals(direction, that.direction) && Objects.equals(destTagUuid, that.destTagUuid) && Objects.equals(destAddressGroupUuid, that.destAddressGroupUuid) && Objects.equals(destNetworkUuid, that.destNetworkUuid) && Objects.equals(tagTypeUuid, that.tagTypeUuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), uuid, firewallPolicyUuid, name, action, serviceGroupUuid, srcTagUuid, srcAddressGroupUuid, srcNetworkUuid, direction, destTagUuid, destAddressGroupUuid, destNetworkUuid, tagTypeUuid, sequence);
    }
}
