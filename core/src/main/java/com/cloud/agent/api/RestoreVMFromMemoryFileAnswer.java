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

/**
 * Answer for {@link RestoreVMFromMemoryFileCommand}.
 *
 * <p>Contains the result of restoring a VM from a memory file,
 * including the VM's power state after restoration.</p>
 */
public class RestoreVMFromMemoryFileAnswer extends Answer {

    private VirtualMachine.PowerState vmPowerState;

    public RestoreVMFromMemoryFileAnswer() {
        // Default constructor for serialization
    }

    public RestoreVMFromMemoryFileAnswer(RestoreVMFromMemoryFileCommand cmd, boolean success, String details) {
        super(cmd, success, details);
    }

    public RestoreVMFromMemoryFileAnswer(RestoreVMFromMemoryFileCommand cmd, boolean success, String details,
                                          VirtualMachine.PowerState vmPowerState) {
        super(cmd, success, details);
        this.vmPowerState = vmPowerState;
    }

    public VirtualMachine.PowerState getVmPowerState() {
        return vmPowerState;
    }

    public void setVmPowerState(VirtualMachine.PowerState vmPowerState) {
        this.vmPowerState = vmPowerState;
    }
}
