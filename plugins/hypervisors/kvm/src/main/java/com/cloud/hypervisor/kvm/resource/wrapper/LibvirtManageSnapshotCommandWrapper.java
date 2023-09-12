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

import java.io.File;
import java.text.MessageFormat;

import org.apache.log4j.Logger;
import org.libvirt.Connect;
import org.libvirt.Domain;
import org.libvirt.DomainInfo.DomainState;
import org.libvirt.DomainSnapshot;
import org.libvirt.LibvirtException;

import com.ceph.rados.IoCTX;
import com.ceph.rados.Rados;
import com.ceph.rbd.Rbd;
import com.ceph.rbd.RbdImage;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.ManageSnapshotAnswer;
import com.cloud.agent.api.ManageSnapshotCommand;
import com.cloud.agent.api.to.StorageFilerTO;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.storage.KVMPhysicalDisk;
import com.cloud.hypervisor.kvm.storage.KVMStoragePool;
import com.cloud.hypervisor.kvm.storage.KVMStoragePoolManager;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.utils.script.Script;

@ResourceWrapper(handles =  ManageSnapshotCommand.class)
public final class LibvirtManageSnapshotCommandWrapper extends CommandWrapper<ManageSnapshotCommand, Answer, LibvirtComputingResource> {

    private static final Logger s_logger = Logger.getLogger(LibvirtManageSnapshotCommandWrapper.class);

    @Override
    public Answer execute(final ManageSnapshotCommand command, final LibvirtComputingResource libvirtComputingResource) {
        final String snapshotName = command.getSnapshotName();
        final String snapshotPath = command.getSnapshotPath();
        final String vmName = command.getVmName();
        try {
            final LibvirtUtilitiesHelper libvirtUtilitiesHelper = libvirtComputingResource.getLibvirtUtilitiesHelper();
            final Connect conn = libvirtUtilitiesHelper.getConnectionByVmName(vmName);
            DomainState state = null;
            Domain vm = null;
            if (vmName != null) {
                try {
                    vm = libvirtComputingResource.getDomain(conn, command.getVmName());
                    state = vm.getInfo().state;
                } catch (final LibvirtException e) {
                    s_logger.trace("Ignoring libvirt error.", e);
                }
            }

            final KVMStoragePoolManager storagePoolMgr = libvirtComputingResource.getStoragePoolMgr();
            final StorageFilerTO pool = command.getPool();
            final KVMStoragePool primaryPool = storagePoolMgr.getStoragePool(pool.getType(), pool.getUuid());

            final KVMPhysicalDisk disk = primaryPool.getPhysicalDisk(command.getVolumePath());
            if (state == DomainState.VIR_DOMAIN_RUNNING && !primaryPool.isExternalSnapshot()) {

                final MessageFormat snapshotXML = new MessageFormat("   <domainsnapshot>" + "       <name>{0}</name>" + "          <domain>"
                        + "            <uuid>{1}</uuid>" + "        </domain>" + "    </domainsnapshot>");

                final String vmUuid = vm.getUUIDString();
                final Object[] args = new Object[] {snapshotName, vmUuid};
                final String snapshot = snapshotXML.format(args);
                s_logger.debug(snapshot);
                if (command.getCommandSwitch().equalsIgnoreCase(ManageSnapshotCommand.CREATE_SNAPSHOT)) {
                    vm.snapshotCreateXML(snapshot);
                } else {
                    final DomainSnapshot snap = vm.snapshotLookupByName(snapshotName);
                    snap.delete(0);
                }

                /*
                 * libvirt on RHEL6 doesn't handle resume event emitted from
                 * qemu
                 */
                vm = libvirtComputingResource.getDomain(conn, command.getVmName());
                state = vm.getInfo().state;
                if (state == DomainState.VIR_DOMAIN_PAUSED) {
                    vm.resume();
                }
            } else {
                /**
                 * For RBD we can't use libvirt to do our snapshotting or any Bash scripts.
                 * libvirt also wants to store the memory contents of the Virtual Machine,
                 * but that's not possible with RBD since there is no way to store the memory
                 * contents in RBD.
                 *
                 * So we rely on the Java bindings for RBD to create our snapshot
                 *
                 * This snapshot might not be 100% consistent due to writes still being in the
                 * memory of the Virtual Machine, but if the VM runs a kernel which supports
                 * barriers properly (>2.6.32) this won't be any different then pulling the power
                 * cord out of a running machine.
                 */
                if (primaryPool.getType() == StoragePoolType.RBD) {
                    try {
                        final Rados r = new Rados(primaryPool.getAuthUserName());
                        r.confSet("mon_host", primaryPool.getSourceHost() + ":" + primaryPool.getSourcePort());
                        r.confSet("key", primaryPool.getAuthSecret());
                        r.confSet("client_mount_timeout", "30");
                        r.connect();
                        s_logger.debug("Successfully connected to Ceph cluster at " + r.confGet("mon_host"));

                        final IoCTX io = r.ioCtxCreate(primaryPool.getSourceDir());
                        final Rbd rbd = new Rbd(io);
                        final RbdImage image = rbd.open(disk.getName());

                        if (command.getCommandSwitch().equalsIgnoreCase(ManageSnapshotCommand.CREATE_SNAPSHOT)) {
                            s_logger.debug("Attempting to create RBD snapshot " + disk.getName() + "@" + snapshotName);
                            image.snapCreate(snapshotName);
                        } else {
                            s_logger.debug("Attempting to remove RBD snapshot " + disk.getName() + "@" + snapshotName);
                            image.snapRemove(snapshotName);
                        }

                        rbd.close(image);
                        r.ioCtxDestroy(io);
                    } catch (final Exception e) {
                        s_logger.error("A RBD snapshot operation on " + disk.getName() + " failed. The error was: " + e.getMessage());
                    }
                } else {
                    /* VM is not running, create a snapshot by ourself */
                    final int cmdsTimeout = libvirtComputingResource.getCmdsTimeout();
                    final String manageSnapshotPath = libvirtComputingResource.manageSnapshotPath();

                    final Script scriptCommand = new Script(manageSnapshotPath, cmdsTimeout, s_logger);
                    if (command.getCommandSwitch().equalsIgnoreCase(ManageSnapshotCommand.CREATE_SNAPSHOT)) {
                        scriptCommand.add("-c", disk.getPath());
                    } else {
                        scriptCommand.add("-d", snapshotPath);
                    }

                    scriptCommand.add("-n", snapshotName);
                    final String result = scriptCommand.execute();
                    if (result != null) {
                        s_logger.debug("Failed to manage snapshot: " + result);
                        return new ManageSnapshotAnswer(command, false, "Failed to manage snapshot: " + result);
                    }
                }
            }
            return new ManageSnapshotAnswer(command, command.getSnapshotId(), disk.getPath() + File.separator + snapshotName, true, null);
        } catch (final LibvirtException e) {
            s_logger.debug("Failed to manage snapshot: " + e.toString());
            return new ManageSnapshotAnswer(command, false, "Failed to manage snapshot: " + e.toString());
        }
    }
}
