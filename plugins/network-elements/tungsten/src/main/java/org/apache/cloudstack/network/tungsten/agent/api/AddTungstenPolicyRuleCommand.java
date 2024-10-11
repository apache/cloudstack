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

public class AddTungstenPolicyRuleCommand extends TungstenCommand {
    private final String uuid;
    private final String policyUuid;
    private final String action;
    private final String direction;
    private final String protocol;
    private final String srcNetwork;
    private final String srcIpPrefix;
    private final int srcIpPrefixLen;
    private final int srcStartPort;
    private final int srcEndPort;
    private final String destNetwork;
    private final String destIpPrefix;
    private final int destIpPrefixLen;
    private final int destStartPort;
    private final int destEndPort;

    public AddTungstenPolicyRuleCommand(final String policyUuid, final String action, final String direction,
        final String protocol, final String srcNetwork, final String srcIpPrefix, final int srcIpPrefixLen,
        final int srcStartPort, final int srcEndPort, final String destNetwork, final String destIpPrefix,
        final int destIpPrefixLen, final int destStartPort, final int destEndPort) {
        this.uuid = UUID.randomUUID().toString();
        this.policyUuid = policyUuid;
        this.action = action;
        this.direction = direction;
        this.protocol = protocol;
        this.srcNetwork = srcNetwork;
        this.srcIpPrefix = srcIpPrefix;
        this.srcIpPrefixLen = srcIpPrefixLen;
        this.srcStartPort = srcStartPort;
        this.srcEndPort = srcEndPort;
        this.destNetwork = destNetwork;
        this.destIpPrefix = destIpPrefix;
        this.destIpPrefixLen = destIpPrefixLen;
        this.destStartPort = destStartPort;
        this.destEndPort = destEndPort;
    }

    public String getUuid() {
        return uuid;
    }

    public String getPolicyUuid() {
        return policyUuid;
    }

    public String getAction() {
        return action;
    }

    public String getDirection() {
        return direction;
    }

    public String getProtocol() {
        return protocol;
    }

    public String getSrcNetwork() {
        return srcNetwork;
    }

    public String getSrcIpPrefix() {
        return srcIpPrefix;
    }

    public int getSrcIpPrefixLen() {
        return srcIpPrefixLen;
    }

    public int getSrcStartPort() {
        return srcStartPort;
    }

    public int getSrcEndPort() {
        return srcEndPort;
    }

    public String getDestNetwork() {
        return destNetwork;
    }

    public String getDestIpPrefix() {
        return destIpPrefix;
    }

    public int getDestIpPrefixLen() {
        return destIpPrefixLen;
    }

    public int getDestStartPort() {
        return destStartPort;
    }

    public int getDestEndPort() {
        return destEndPort;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        AddTungstenPolicyRuleCommand that = (AddTungstenPolicyRuleCommand) o;
        return srcIpPrefixLen == that.srcIpPrefixLen && srcStartPort == that.srcStartPort && srcEndPort == that.srcEndPort && destIpPrefixLen == that.destIpPrefixLen && destStartPort == that.destStartPort && destEndPort == that.destEndPort && Objects.equals(uuid, that.uuid) && Objects.equals(policyUuid, that.policyUuid) && Objects.equals(action, that.action) && Objects.equals(direction, that.direction) && Objects.equals(protocol, that.protocol) && Objects.equals(srcNetwork, that.srcNetwork) && Objects.equals(srcIpPrefix, that.srcIpPrefix) && Objects.equals(destNetwork, that.destNetwork) && Objects.equals(destIpPrefix, that.destIpPrefix);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), uuid, policyUuid, action, direction, protocol, srcNetwork, srcIpPrefix, srcIpPrefixLen, srcStartPort, srcEndPort, destNetwork, destIpPrefix, destIpPrefixLen, destStartPort, destEndPort);
    }
}
