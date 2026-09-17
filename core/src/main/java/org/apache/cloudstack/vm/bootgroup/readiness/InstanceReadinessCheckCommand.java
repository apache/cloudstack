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

package org.apache.cloudstack.vm.bootgroup.readiness;

import com.cloud.agent.api.routing.NetworkElementCommand;

/**
 * Dispatched to a VR to run a readiness check (ping or TCP port connect) against a user VM's IP,
 * on behalf of the instance boot group readiness feature. Deliberately separate from
 * {@code org.apache.cloudstack.diagnostics.DiagnosticsCommand}, which is a general-purpose admin
 * tool scoped to system VMs (SSVM/CPVM/VR) — this command runs its own dedicated VR script instead
 * of sharing that one.
 */
public class InstanceReadinessCheckCommand extends NetworkElementCommand {

    public static final String CHECK_TYPE_PING = "ping";
    public static final String CHECK_TYPE_PORT_CHECK = "portcheck";

    private final String checkType;
    private final String ipAddress;
    private Integer port;
    private final boolean executeInSequence;

    public InstanceReadinessCheckCommand(String ipAddress, boolean executeInSequence) {
        this.checkType = CHECK_TYPE_PING;
        this.ipAddress = ipAddress;
        this.executeInSequence = executeInSequence;
    }

    public InstanceReadinessCheckCommand(String ipAddress, Integer port, boolean executeInSequence) {
        this.checkType = CHECK_TYPE_PORT_CHECK;
        this.ipAddress = ipAddress;
        this.port = port;
        this.executeInSequence = executeInSequence;
    }

    public String getCheckType() {
        return checkType;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public Integer getPort() {
        return port;
    }

    @Override
    public boolean isQuery() {
        return true;
    }

    @Override
    public boolean executeInSequence() {
        return executeInSequence;
    }
}
