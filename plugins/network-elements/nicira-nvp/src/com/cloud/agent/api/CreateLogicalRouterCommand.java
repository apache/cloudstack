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
package com.cloud.agent.api;

/**
 *
 */
public class CreateLogicalRouterCommand extends Command {
    private String _gatewayServiceUuid;
    private String _logicalSwitchUuid;
    private long _vlanId;
    private String _name;
    private String _ownerName;
    private String _publicIpCidr;
    private String _publicNextHop;
    private String _internalIpCidr;

    public CreateLogicalRouterCommand(String gatewayServiceUuid, long vlanId,
            String logicalSwitchUuid, String name,
            String publicIpCidr, String publicNextHop,
            String internalIpCidr, String ownerName) {
        super();
        _gatewayServiceUuid = gatewayServiceUuid;
        _logicalSwitchUuid = logicalSwitchUuid;
        _vlanId = vlanId;
        _name = name;
        _ownerName = ownerName;
        _publicIpCidr = publicIpCidr;
        _publicNextHop = publicNextHop;
        _internalIpCidr = internalIpCidr;
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }

    public String getGatewayServiceUuid() {
        return _gatewayServiceUuid;
    }

    public void setGatewayServiceUuid(String gatewayServiceUuid) {
        _gatewayServiceUuid = gatewayServiceUuid;
    }

    public String getLogicalSwitchUuid() {
        return _logicalSwitchUuid;
    }

    public void setLogicalSwitchUuid(String logicalSwitchUuid) {
        _logicalSwitchUuid = logicalSwitchUuid;
    }

    public long getVlanId() {
        return _vlanId;
    }

    public void setVlanId(long vlanId) {
        _vlanId = vlanId;
    }

    public String getName() {
        return _name;
    }

    public void setName(String name) {
        _name = name;
    }

    public String getOwnerName() {
        return _ownerName;
    }

    public void setOwnerName(String ownerName) {
        _ownerName = ownerName;
    }

    public String getPublicIpCidr() {
        return _publicIpCidr;
    }

    public void setPublicIpCidr(String publicIpCidr) {
        _publicIpCidr = publicIpCidr;
    }

    public String getInternalIpCidr() {
        return _internalIpCidr;
    }

    public void setInternalIpCidr(String internalIpCidr) {
        _internalIpCidr = internalIpCidr;
    }

    public String getPublicNextHop() {
        return _publicNextHop;
    }

    public void setPublicNextHop(String publicNextHop) {
        _publicNextHop = publicNextHop;
    }
}
