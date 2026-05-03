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

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.PreMigrationCommand;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef.DiskDef;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.libvirt.Connect;
import org.libvirt.Domain;
import org.libvirt.LibvirtException;

import java.util.List;

/**
 * Handles PreMigrationCommand on the source host before live migration.
 * Converts CLVM volume locks from exclusive to shared mode so the destination host can access them.
 */
@ResourceWrapper(handles = PreMigrationCommand.class)
public class LibvirtPreMigrationCommandWrapper extends CommandWrapper<PreMigrationCommand, Answer, LibvirtComputingResource> {
    protected Logger logger = LogManager.getLogger(getClass());

    @Override
    public Answer execute(PreMigrationCommand command, LibvirtComputingResource libvirtComputingResource) {
        String vmName = command.getVmName();
        VirtualMachineTO vmSpec = command.getVirtualMachine();

        logger.info("Preparing source host for migration of VM: {}", vmName);

        Connect conn = null;
        Domain dm = null;

        try {
            LibvirtUtilitiesHelper libvirtUtilitiesHelper = libvirtComputingResource.getLibvirtUtilitiesHelper();
            conn = libvirtUtilitiesHelper.getConnectionByVmName(vmName);
            dm = conn.domainLookupByName(vmName);

            List<DiskDef> disks = libvirtComputingResource.getDisks(conn, vmName);
            logger.info("Converting CLVM volumes to shared mode for VM: {}", vmName);
            LibvirtComputingResource.modifyClvmVolumesStateForMigration(
                disks,
                libvirtComputingResource,
                vmSpec,
                LibvirtComputingResource.ClvmVolumeState.SHARED
            );

            logger.info("Successfully prepared source host for migration of VM: {}", vmName);
            return new Answer(command, true, "Source host prepared for migration");

        } catch (LibvirtException e) {
            logger.error("Failed to prepare source host for migration of VM: {}", vmName, e);
            return new Answer(command, false, "Failed to prepare source host: " + e.getMessage());
        } finally {
            if (dm != null) {
                try {
                    dm.free();
                } catch (LibvirtException e) {
                    logger.warn("Failed to free domain {}: {}", vmName, e.getMessage());
                }
            }
        }
    }
}
