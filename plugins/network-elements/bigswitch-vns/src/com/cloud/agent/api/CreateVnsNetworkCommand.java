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

public class CreateVnsNetworkCommand extends Command {
    private String _networkUuid;
    private String _name;
    private String _tenantUuid;
    private int _vlan;

    public CreateVnsNetworkCommand(String networkUuid, String name, String tenantUuid, int vlan) {
	this._networkUuid = networkUuid;
        this._name = name;
        this._tenantUuid = tenantUuid;
        this._vlan = vlan;
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }

    public String getNetworkUuid() {
	return _networkUuid;
    }

    public String getName() {
        return _name;
    }

    public String getTenantUuid() {
        return _tenantUuid;
    }

    public int getVlan() {
        return _vlan;
    }
}
