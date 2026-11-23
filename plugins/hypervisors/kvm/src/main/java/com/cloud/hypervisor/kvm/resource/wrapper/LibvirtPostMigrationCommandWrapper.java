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
import org.libvirt.Domain;
import org.libvirt.LibvirtException;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.PostMigrationAnswer;
import com.cloud.agent.api.PostMigrationCommand;
import com.cloud.agent.api.to.NicTO;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.exception.InternalErrorException;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.resource.LibvirtConnection;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef.DiskDef;
import com.cloud.network.Networks;
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
                vm,
                LibvirtComputingResource.ClvmVolumeState.EXCLUSIVE
            );

            applyManualVlanTrunkMembership(conn, vmName, vm.getNics(), libvirtComputingResource);

            logger.debug("Successfully completed post-migration tasks for VM {}", vmName);
            return new PostMigrationAnswer(command);

        } catch (final LibvirtException e) {
            logger.error("Libvirt error during post-migration for VM {}: {}", vmName, e.getMessage(), e);
            return new PostMigrationAnswer(command, e);
        } catch (final Exception e) {
            logger.error("Error during post-migration for VM {}: {}", vmName, e.getMessage(), e);
            return new PostMigrationAnswer(command, e);
        }
    }

    private void applyManualVlanTrunkMembership(Connect conn, String vmName, NicTO[] nics, LibvirtComputingResource libvirtComputingResource)
            throws InternalErrorException {
        if (nics == null) {
            return;
        }
        Exception firstFailure = null;
        for (NicTO nic : nics) {
            if (nic.getBroadcastType() != Networks.BroadcastDomainType.Vlan) {
                continue;
            }
            try {
                applyVlanMembershipOrFailClosed(conn, vmName, nic, libvirtComputingResource);
            } catch (Exception e) {
                // keep going so every other nic of the VM is also applied or cut off, not left on VLAN 1
                logger.error("Failed to set up the VLAN membership of nic {} of VM {}: {}", nic.getMac(), vmName, e.getMessage(), e);
                firstFailure = firstFailure == null ? e : firstFailure;
            }
        }
        if (firstFailure != null) {
            throw new InternalErrorException(String.format("VLAN membership could not be set up for every nic of VM %s: %s", vmName, firstFailure.getMessage()));
        }
    }

    private void applyVlanMembershipOrFailClosed(Connect conn, String vmName, NicTO nic, LibvirtComputingResource libvirtComputingResource) throws Exception {
        LibvirtVMDef.InterfaceDef liveInterface;
        try {
            liveInterface = libvirtComputingResource.getInterface(conn, vmName, nic.getMac());
            libvirtComputingResource.getVifDriver(nic.getType(), nic.getName()).ensureVlanTrunkMembership(liveInterface, nic);
        } catch (Exception e) {
            failClosed(conn, vmName, nic, libvirtComputingResource);
            throw e;
        }
        libvirtComputingResource.warnIfNicOnVlanAwareBridgeDespiteOptOut(liveInterface, nic, vmName);
        restoreLinkIfCutOff(conn, vmName, nic, liveInterface, libvirtComputingResource);
    }

    // Without its VLANs the tap sits on the bridge's default VLAN 1, so cut the nic off rather than leave it there
    private void failClosed(Connect conn, String vmName, NicTO nic, LibvirtComputingResource libvirtComputingResource) {
        if (!nic.isEnabled()) {
            return;
        }
        try {
            setNicLinkState(conn, vmName, nic, false, libvirtComputingResource);
            logger.error("Could not apply the VLAN membership of nic {} of VM {}; its link was set down so it does not sit on the bridge's default VLAN. "
                    + "The link is restored once the membership is applied; otherwise correct it or restart the VM.", nic.getMac(), vmName);
        } catch (Exception e) {
            logger.error("Could not apply the VLAN membership of nic {} of VM {} and could not set its link down either, so it may still be on the bridge's default VLAN: {}",
                    nic.getMac(), vmName, e.getMessage());
        }
    }

    // The link follows the nic's enabled state, so an enabled nic found down was cut off by an earlier failed attempt, possibly on another host
    private void restoreLinkIfCutOff(Connect conn, String vmName, NicTO nic, LibvirtVMDef.InterfaceDef liveInterface,
            LibvirtComputingResource libvirtComputingResource) throws LibvirtException {
        if (nic.isEnabled() && !liveInterface.isLinkStateUp()) {
            setNicLinkState(conn, vmName, nic, true, libvirtComputingResource);
            logger.info("VLAN membership of nic {} of VM {} is applied now; its link was set up again", nic.getMac(), vmName);
        }
    }

    private void setNicLinkState(Connect conn, String vmName, NicTO nic, boolean up, LibvirtComputingResource libvirtComputingResource) throws LibvirtException {
        Domain domain = libvirtComputingResource.getDomain(conn, vmName);
        try {
            LibvirtVMDef.InterfaceDef liveInterface = libvirtComputingResource.getInterface(conn, vmName, nic.getMac());
            liveInterface.setLinkStateUp(up);
            domain.updateDeviceFlags(liveInterface.toString(), Domain.DeviceModifyFlags.LIVE);
        } finally {
            domain.free();
        }
    }
}
