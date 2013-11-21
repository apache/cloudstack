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

import java.util.ArrayList;
import java.util.List;

/**
 * Command for creating a logical edge firewall in VNMC
 */
public class CreateLogicalEdgeFirewallCommand extends Command {
    private long _vlanId;
    private String _publicIp;
    private String _internalIp;
    private String _publicSubnet;
    private String _internalSubnet;
    private List<String> _publicGateways;

    public CreateLogicalEdgeFirewallCommand(long vlanId, String publicIp, String internalIp, String publicSubnet, String internalSubnet) {
        super();
        this._vlanId = vlanId;
        this._publicIp = publicIp;
        this._internalIp = internalIp;
        this._publicSubnet = publicSubnet;
        this.setInternalSubnet(internalSubnet);
        _publicGateways = new ArrayList<String>();
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }

    public long getVlanId() {
        return _vlanId;
    }

    public void setVlanId(long vlanId) {
        this._vlanId = vlanId;
    }

    public String getPublicIp() {
        return _publicIp;
    }

    public void setPublicIp(String publicIp) {
        this._publicIp = publicIp;
    }

    public String getInternalIp() {
        return _internalIp;
    }

    public void setInternalIp(String internalIp) {
        this._internalIp = internalIp;
    }

    public String getPublicSubnet() {
        return _publicSubnet;
    }

    public void setPublicSubnet(String publicSubnet) {
        this._publicSubnet = publicSubnet;
    }

    public String getInternalSubnet() {
        return _internalSubnet;
    }

    public void setInternalSubnet(String _internalSubnet) {
        this._internalSubnet = _internalSubnet;
    }

    public List<String> getPublicGateways() {
        return _publicGateways;
    }

}
