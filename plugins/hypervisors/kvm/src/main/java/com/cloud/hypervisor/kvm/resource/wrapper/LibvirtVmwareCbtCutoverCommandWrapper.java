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
package com.cloud.hypervisor.kvm.resource.wrapper;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.VmwareCbtCutoverCommand;
import com.cloud.agent.api.VmwareCbtMigrationAnswer;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;

@ResourceWrapper(handles = VmwareCbtCutoverCommand.class)
public class LibvirtVmwareCbtCutoverCommandWrapper extends CommandWrapper<VmwareCbtCutoverCommand, Answer, LibvirtComputingResource> {

    @Override
    public Answer execute(VmwareCbtCutoverCommand cmd, LibvirtComputingResource serverResource) {
        if (!serverResource.hostSupportsVmwareCbtMigration()) {
            String msg = String.format("Cannot cut over VMware CBT migration %s on host %s. VDDK, qemu-img and qemu-nbd are required.",
                    cmd.getMigrationUuid(), serverResource.getPrivateIp());
            logger.info(msg);
            return new VmwareCbtMigrationAnswer(cmd, false, msg, cmd.getMigrationUuid());
        }

        String msg = String.format("VMware CBT cutover for migration %s reached the KVM agent, but final cutover execution is not implemented yet.",
                cmd.getMigrationUuid());
        logger.info(msg);
        return new VmwareCbtMigrationAnswer(cmd, false, msg, cmd.getMigrationUuid(), cmd.getFinalCycleNumber(),
                0, 0, 0, false, null);
    }
}
