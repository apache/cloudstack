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

import java.util.Map;

import com.cloud.agent.api.to.NicTO;
import com.cloud.vm.VirtualMachine;

public class ReplugNicCommand extends Command {

    NicTO nic;
    String instanceName;
    VirtualMachine.Type vmType;
    Map<String, String> details;

    public NicTO getNic() {
        return nic;
    }

    @Override
    public boolean executeInSequence() {
        return true;
    }

    protected ReplugNicCommand() {
    }

    public ReplugNicCommand(NicTO nic, String instanceName, VirtualMachine.Type vmtype) {
        this.nic = nic;
        this.instanceName = instanceName;
        this.vmType = vmtype;
    }

    public ReplugNicCommand(NicTO nic, String instanceName, VirtualMachine.Type vmtype, Map<String, String> details) {
        this.nic = nic;
        this.instanceName = instanceName;
        this.vmType = vmtype;
        this.details = details;
    }

    public String getVmName() {
        return instanceName;
    }

    public VirtualMachine.Type getVMType() {
        return vmType;
    }

    public Map<String, String> getDetails() {
        return this.details;
    }
}
