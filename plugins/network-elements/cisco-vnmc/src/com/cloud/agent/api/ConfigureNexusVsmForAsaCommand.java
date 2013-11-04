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

import com.cloud.agent.api.LogLevel.Log4jLevel;

/**
 * Command for configuring n1kv VSM for asa1kv device. It does the following in VSM:
 * a. creating vservice node for asa1kv
 * b. updating vlan of inside port profile associated with asa1kv
 */
public class ConfigureNexusVsmForAsaCommand extends Command {
    private long _vlanId;
    private String _ipAddress;
    private String _vsmUsername;
    @LogLevel(Log4jLevel.Off)
    private String _vsmPassword;
    private String _vsmIp;
    private String _asaInPortProfile;

    public ConfigureNexusVsmForAsaCommand(long vlanId, String ipAddress,
            String vsmUsername, String vsmPassword, String vsmIp, String asaInPortProfile) {
        super();
        this._vlanId = vlanId;
        this._ipAddress = ipAddress;
        this._vsmUsername = vsmUsername;
        this._vsmPassword = vsmPassword;
        this._vsmIp = vsmIp;
        this._asaInPortProfile = asaInPortProfile;
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }

    public long getVlanId() {
        return _vlanId;
    }

    public void setVlanId(long _vlanId) {
        this._vlanId = _vlanId;
    }

    public String getIpAddress() {
        return _ipAddress;
    }

    public void setIpAddress(String _ipAddress) {
        this._ipAddress = _ipAddress;
    }

    public String getVsmUsername() {
        return _vsmUsername;
    }

    public void setVsmUsername(String _vsmUsername) {
        this._vsmUsername = _vsmUsername;
    }

    public String getVsmPassword() {
        return _vsmPassword;
    }

    public void setVsmPassword(String _vsmPassword) {
        this._vsmPassword = _vsmPassword;
    }

    public String getVsmIp() {
        return _vsmIp;
    }

    public void setVsmIp(String _vsmIp) {
        this._vsmIp = _vsmIp;
    }

    public String getAsaInPortProfile() {
        return _asaInPortProfile;
    }

    public void setAsaInPortProfile(String _asaInPortProfile) {
        this._asaInPortProfile = _asaInPortProfile;
    }
}
