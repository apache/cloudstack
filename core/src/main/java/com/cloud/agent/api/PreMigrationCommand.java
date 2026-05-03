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

import com.cloud.agent.api.to.VirtualMachineTO;

/**
 * PreMigrationCommand is sent to the source host before VM migration starts.
 * It performs pre-migration tasks such as:
 * - Converting CLVM volume exclusive locks to shared mode so destination host can access them
 * - Other pre-migration preparation operations on the source host
 *
 * This command runs on the SOURCE host before PrepareForMigrationCommand runs on the DESTINATION host.
 */
public class PreMigrationCommand extends Command {
    private VirtualMachineTO vm;
    private String vmName;

    protected PreMigrationCommand() {
    }

    public PreMigrationCommand(VirtualMachineTO vm, String vmName) {
        this.vm = vm;
        this.vmName = vmName;
    }

    public VirtualMachineTO getVirtualMachine() {
        return vm;
    }

    public String getVmName() {
        return vmName;
    }

    @Override
    public boolean executeInSequence() {
        return true;
    }
}
