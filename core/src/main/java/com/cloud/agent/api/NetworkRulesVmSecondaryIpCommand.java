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

import com.cloud.vm.VirtualMachine;

public class NetworkRulesVmSecondaryIpCommand extends Command {

    private String vmName;
    private VirtualMachine.Type type;
    private String vmSecIp;
    private String vmMac;
    private String action;

    public NetworkRulesVmSecondaryIpCommand(String vmName, VirtualMachine.Type type) {
        this.vmName = vmName;
        this.type = type;
    }

    public NetworkRulesVmSecondaryIpCommand(String vmName, String vmMac, String secondaryIp, boolean action) {
        this.vmName = vmName;
        this.vmMac = vmMac;
        this.vmSecIp = secondaryIp;
        if (action) {
            this.action = "-A";
        } else {
            this.action = "-D";
        }
    }

    public String getVmName() {
        return vmName;
    }

    public VirtualMachine.Type getType() {
        return type;
    }

    public String getVmSecIp() {
        return vmSecIp;
    }

    public String getVmMac() {
        return vmMac;
    }

    public String getAction() {
        return action;
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }
}
