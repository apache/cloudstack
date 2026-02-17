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

package com.cloud.hypervisor.kvm.resource.wrapper;

import java.util.List;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.libvirt.Connect;
import org.libvirt.LibvirtException;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.PostMigrationAnswer;
import com.cloud.agent.api.PostMigrationCommand;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.resource.LibvirtConnection;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef.DiskDef;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;

/**
 * Wrapper for PostMigrationCommand on KVM hypervisor.
 * Handles post-migration tasks on the destination host after a VM has been successfully migrated.
 * Primary responsibility: Convert CLVM volumes from shared mode to exclusive mode on destination.
 */
@ResourceWrapper(handles = PostMigrationCommand.class)
public final class LibvirtPostMigrationCommandWrapper extends CommandWrapper<PostMigrationCommand, Answer, LibvirtComputingResource> {

    protected Logger logger = LogManager.getLogger(getClass());

    @Override
    public Answer execute(final PostMigrationCommand command, final LibvirtComputingResource libvirtComputingResource) {
        final VirtualMachineTO vm = command.getVirtualMachine();
        final String vmName = command.getVmName();

        if (vm == null || vmName == null) {
            return new PostMigrationAnswer(command, "VM or VM name is null");
        }

        logger.debug("Executing post-migration tasks for VM {} on destination host", vmName);

        try {
            final Connect conn = LibvirtConnection.getConnectionByVmName(vmName);

            List<DiskDef> disks = libvirtComputingResource.getDisks(conn, vmName);
            logger.debug("[CLVM Post-Migration] Processing volumes for VM {} to claim exclusive locks on any CLVM volumes", vmName);
            LibvirtComputingResource.modifyClvmVolumesStateForMigration(
                disks,
                libvirtComputingResource,
                vm,
                LibvirtComputingResource.ClvmVolumeState.EXCLUSIVE
            );

            logger.debug("Successfully completed post-migration tasks for VM {}", vmName);
            return new PostMigrationAnswer(command);

        } catch (final LibvirtException e) {
            logger.error("LibVirt error during post-migration for VM {}: {}", vmName, e.getMessage(), e);
            return new PostMigrationAnswer(command, e);
        } catch (final Exception e) {
            logger.error("Error during post-migration for VM {}: {}", vmName, e.getMessage(), e);
            return new PostMigrationAnswer(command, e);
        }
    }
}
