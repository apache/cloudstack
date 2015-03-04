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
import java.util.Map;

import com.cloud.agent.api.Command;

public class ApplyAclRuleVspCommand extends Command {

    String _networkUuid;
    String _networkDomainUuid;
    String _vpcOrSubnetUuid;
    boolean _isL3Network;
    List<Map<String, Object>> _aclRules;
    boolean _isVpc;
    long _networkId;

    public ApplyAclRuleVspCommand(String networkUuid, String networkDomainUuid, String vpcOrSubnetUuid, boolean isL3Network, List<Map<String, Object>> aclRules, boolean isVpc,
            long networkId) {
        super();
        this._networkUuid = networkUuid;
        this._networkDomainUuid = networkDomainUuid;
        this._vpcOrSubnetUuid = vpcOrSubnetUuid;
        this._isL3Network = isL3Network;
        this._aclRules = aclRules;
        this._isVpc = isVpc;
        this._networkId = networkId;
    }

    public String getNetworkUuid() {
        return _networkUuid;
    }

    public String getNetworkDomainUuid() {
        return _networkDomainUuid;
    }

    public String getVpcOrSubnetUuid() {
        return _vpcOrSubnetUuid;
    }

    public boolean isL3Network() {
        return _isL3Network;
    }

    public List<Map<String, Object>> getAclRules() {
        return _aclRules;
    }

    public boolean isVpc() {
        return _isVpc;
    }

    public long getNetworkId() {
        return this._networkId;
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }

}
