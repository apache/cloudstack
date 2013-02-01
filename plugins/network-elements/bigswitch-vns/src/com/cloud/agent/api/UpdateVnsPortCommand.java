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

public class UpdateVnsPortCommand extends Command {
    private String _networkUuid;
    private String _portUuid;
    private String _tenantUuid;
    private String _portName;

    public UpdateVnsPortCommand(String networkUuid, String portUuid, String tenantUuid, String portName) {
        this._networkUuid = networkUuid;
        this._portUuid = portUuid;
        this._tenantUuid = tenantUuid;
        this._portName = portName;
    }


    public String getNetworkUuid() {
        return _networkUuid;
    }


    public String getPortUuid() {
        return _portUuid;
    }


    public String getTenantUuid() {
        return _tenantUuid;
    }


    public String getPortName() {
        return _portName;
    }


    @Override
    public boolean executeInSequence() {
        return false;
    }

}
