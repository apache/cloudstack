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

package com.cloud.agent.api;

/**
 *
 */
public class CreateLogicalRouterCommand extends Command {
    private String gatewayServiceUuid;
    private String logicalSwitchUuid;
    private long vlanId;
    private String name;
    private String ownerName;
    private String publicIpCidr;
    private String publicNextHop;
    private String internalIpCidr;

    public CreateLogicalRouterCommand(final String gatewayServiceUuid, final long vlanId, final String logicalSwitchUuid, final String name, final String publicIpCidr,
            final String publicNextHop, final String internalIpCidr, final String ownerName) {
        super();
        this.gatewayServiceUuid = gatewayServiceUuid;
        this.logicalSwitchUuid = logicalSwitchUuid;
        this.vlanId = vlanId;
        this.name = name;
        this.ownerName = ownerName;
        this.publicIpCidr = publicIpCidr;
        this.publicNextHop = publicNextHop;
        this.internalIpCidr = internalIpCidr;
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }

    public String getGatewayServiceUuid() {
        return gatewayServiceUuid;
    }

    public void setGatewayServiceUuid(final String gatewayServiceUuid) {
        this.gatewayServiceUuid = gatewayServiceUuid;
    }

    public String getLogicalSwitchUuid() {
        return logicalSwitchUuid;
    }

    public void setLogicalSwitchUuid(final String logicalSwitchUuid) {
        this.logicalSwitchUuid = logicalSwitchUuid;
    }

    public long getVlanId() {
        return vlanId;
    }

    public void setVlanId(final long vlanId) {
        this.vlanId = vlanId;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(final String ownerName) {
        this.ownerName = ownerName;
    }

    public String getPublicIpCidr() {
        return publicIpCidr;
    }

    public void setPublicIpCidr(final String publicIpCidr) {
        this.publicIpCidr = publicIpCidr;
    }

    public String getInternalIpCidr() {
        return internalIpCidr;
    }

    public void setInternalIpCidr(final String internalIpCidr) {
        this.internalIpCidr = internalIpCidr;
    }

    public String getPublicNextHop() {
        return publicNextHop;
    }

    public void setPublicNextHop(final String publicNextHop) {
        this.publicNextHop = publicNextHop;
    }
}
