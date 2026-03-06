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
 * PostMigrationCommand is sent to the destination host after a successful VM migration.
 * It performs post-migration tasks such as:
 * - Claiming exclusive locks on CLVM volumes (converting from shared to exclusive mode)
 * - Other post-migration cleanup operations
 */
public class PostMigrationCommand extends Command {
    private VirtualMachineTO vm;
    private String vmName;

    protected PostMigrationCommand() {
    }

    public PostMigrationCommand(VirtualMachineTO vm, String vmName) {
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
