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

import com.cloud.host.Host;
import com.cloud.vm.VirtualMachine;

public class FenceCommand extends Command {

    public FenceCommand() {
        super();
    }

    String vmName;
    String hostGuid;
    String hostIp;
    boolean inSeq;

    public FenceCommand(VirtualMachine vm, Host host) {
        super();
        vmName = vm.getInstanceName();
        hostGuid = host.getGuid();
        hostIp = host.getPrivateIpAddress();
        inSeq = false;
    }

    public void setSeq(boolean inseq) {
        inSeq = inseq;
    }

    public String getVmName() {
        return vmName;
    }

    public String getHostGuid() {
        return hostGuid;
    }

    public String getHostIp() {
        return hostIp;
    }

    @Override
    public boolean executeInSequence() {
        return inSeq;
    }
}
