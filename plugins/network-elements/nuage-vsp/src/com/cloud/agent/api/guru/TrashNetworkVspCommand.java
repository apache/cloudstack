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

package com.cloud.agent.api.guru;

import com.cloud.agent.api.Command;

public class TrashNetworkVspCommand extends Command {

    String _domainUuid;
    String _networkUuid;
    boolean _isL3Network;
    String _vpcUuid;

    public TrashNetworkVspCommand(String domainUuid, String networkUuid, boolean isL3Network, String vpcUuid) {
        super();
        this._domainUuid = domainUuid;
        this._networkUuid = networkUuid;
        this._isL3Network = isL3Network;
        this._vpcUuid = vpcUuid;
    }

    public String getDomainUuid() {
        return _domainUuid;
    }

    public String getNetworkUuid() {
        return _networkUuid;
    }

    public boolean isL3Network() {
        return _isL3Network;
    }

    public String getVpcUuid() {
        return _vpcUuid;
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }

}
