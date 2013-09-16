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
 * Command for cleaning up logical edge firewall in VNMC
 */
public class CleanupLogicalEdgeFirewallCommand extends Command {
    private long _vlanId;

    public CleanupLogicalEdgeFirewallCommand(long vlanId) {
        super();
        this._vlanId = vlanId;
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

}
