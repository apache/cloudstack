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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.MessageFormat;

import org.apache.log4j.Logger;
import org.libvirt.Connect;
import org.libvirt.Domain;
import org.libvirt.DomainInfo.DomainState;
import org.libvirt.DomainSnapshot;
import org.libvirt.LibvirtException;

import com.ceph.rados.IoCTX;
import com.ceph.rados.Rados;
import com.ceph.rados.RadosException;
import com.ceph.rbd.Rbd;
import com.ceph.rbd.RbdException;
import com.ceph.rbd.RbdImage;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.BackupSnapshotAnswer;
import com.cloud.agent.api.BackupSnapshotCommand;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.storage.KVMPhysicalDisk;
import com.cloud.hypervisor.kvm.storage.KVMStoragePool;
import com.cloud.hypervisor.kvm.storage.KVMStoragePoolManager;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;

@ResourceWrapper(handles =  BackupSnapshotCommand.class)
public final class LibvirtBackupSnapshotCommandWrapper extends CommandWrapper<BackupSnapshotCommand, Answer, LibvirtComputingResource> {

    private static final Logger s_logger = Logger.getLogger(LibvirtBackupSnapshotCommandWrapper.class);

    @Override
    public Answer execute(final BackupSnapshotCommand command, final LibvirtComputingResource libvirtComputingResource) {
        final Long dcId = command.getDataCenterId();
        final Long accountId = command.getAccountId();
        final Long volumeId = command.getVolumeId();
        final String secondaryStoragePoolUrl = command.getSecondaryStorageUrl();
        final String snapshotName = command.getSnapshotName();
        String snapshotDestPath = null;
        String snapshotRelPath = null;
        final String vmName = command.getVmName();
        KVMStoragePool secondaryStoragePool = null;
        final KVMStoragePoolManager storagePoolMgr = libvirtComputingResource.getStoragePoolMgr();

        try {
            final LibvirtUtilitiesHelper libvirtUtilitiesHelper = libvirtComputingResource.getLibvirtUtilitiesHelper();
            final Connect conn = libvirtUtilitiesHelper.getConnectionByVmName(vmName);

            secondaryStoragePool = storagePoolMgr.getStoragePoolByURI(secondaryStoragePoolUrl);

            final String ssPmountPath = secondaryStoragePool.getLocalPath();
            snapshotRelPath = File.separator + "snapshots" + File.separator + dcId + File.separator + accountId + File.separator + volumeId;

            snapshotDestPath = ssPmountPath + File.separator + "snapshots" + File.separator + dcId + File.separator + accountId + File.separator + volumeId;
            final KVMStoragePool primaryPool = storagePoolMgr.getStoragePool(command.getPool().getType(), command.getPrimaryStoragePoolNameLabel());
            final KVMPhysicalDisk snapshotDisk = primaryPool.getPhysicalDisk(command.getVolumePath());

            final String manageSnapshotPath = libvirtComputingResource.manageSnapshotPath();
            final int cmdsTimeout = libvirtComputingResource.getCmdsTimeout();

            /**
             * RBD snapshots can't be copied using qemu-img, so we have to use
             * the Java bindings for librbd here.
             *
             * These bindings will read the snapshot and write the contents to
             * the secondary storage directly
             *
             * It will stop doing so if the amount of time spend is longer then
             * cmds.timeout
             */
            if (primaryPool.getType() == StoragePoolType.RBD) {
                try {
                    final Rados r = new Rados(primaryPool.getAuthUserName());
                    r.confSet("mon_host", primaryPool.getSourceHost() + ":" + primaryPool.getSourcePort());
                    r.confSet("key", primaryPool.getAuthSecret());
                    r.confSet("client_mount_timeout", "30");
                    r.connect();
                    s_logger.debug("Succesfully connected to Ceph cluster at " + r.confGet("mon_host"));

                    final IoCTX io = r.ioCtxCreate(primaryPool.getSourceDir());
                    final Rbd rbd = new Rbd(io);
                    final RbdImage image = rbd.open(snapshotDisk.getName(), snapshotName);
                    final File fh = new File(snapshotDestPath);
                    try(BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(fh));) {
                        final int chunkSize = 4194304;
                        long offset = 0;
                        s_logger.debug("Backuping up RBD snapshot " + snapshotName + " to  " + snapshotDestPath);
                        while (true) {
                            final byte[] buf = new byte[chunkSize];
                            final int bytes = image.read(offset, buf, chunkSize);
                            if (bytes <= 0) {
                                break;
                            }
                            bos.write(buf, 0, bytes);
                            offset += bytes;
                        }
                        s_logger.debug("Completed backing up RBD snapshot " + snapshotName + " to  " + snapshotDestPath + ". Bytes written: " + offset);
                    }catch(final IOException ex)
                    {
                        s_logger.error("BackupSnapshotAnswer:Exception:"+ ex.getMessage());
                    }
                    r.ioCtxDestroy(io);
                } catch (final RadosException e) {
                    s_logger.error("A RADOS operation failed. The error was: " + e.getMessage());
                    return new BackupSnapshotAnswer(command, false, e.toString(), null, true);
                } catch (final RbdException e) {
                    s_logger.error("A RBD operation on " + snapshotDisk.getName() + " failed. The error was: " + e.getMessage());
                    return new BackupSnapshotAnswer(command, false, e.toString(), null, true);
                }
            } else {
                final Script scriptCommand = new Script(manageSnapshotPath, cmdsTimeout, s_logger);
                scriptCommand.add("-b", snapshotDisk.getPath());
                scriptCommand.add("-n", snapshotName);
                scriptCommand.add("-p", snapshotDestPath);
                scriptCommand.add("-t", snapshotName);
                final String result = scriptCommand.execute();

                if (result != null) {
                    s_logger.debug("Failed to backup snaptshot: " + result);
                    return new BackupSnapshotAnswer(command, false, result, null, true);
                }
            }
            /* Delete the snapshot on primary */

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

            final KVMStoragePool primaryStorage = storagePoolMgr.getStoragePool(command.getPool().getType(), command.getPool().getUuid());

            if (state == DomainState.VIR_DOMAIN_RUNNING && !primaryStorage.isExternalSnapshot()) {
                final MessageFormat snapshotXML = new MessageFormat("   <domainsnapshot>" + "       <name>{0}</name>" + "          <domain>"
                        + "            <uuid>{1}</uuid>" + "        </domain>" + "    </domainsnapshot>");

                final String vmUuid = vm.getUUIDString();
                final Object[] args = new Object[] {snapshotName, vmUuid};
                final String snapshot = snapshotXML.format(args);
                s_logger.debug(snapshot);
                final DomainSnapshot snap = vm.snapshotLookupByName(snapshotName);
                if (snap != null) {
                    snap.delete(0);
                } else {
                    throw new CloudRuntimeException("Unable to find vm snapshot with name -" + snapshotName);
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
                final Script scriptCommand = new Script(manageSnapshotPath, cmdsTimeout, s_logger);
                scriptCommand.add("-d", snapshotDisk.getPath());
                scriptCommand.add("-n", snapshotName);
                final String result = scriptCommand.execute();
                if (result != null) {
                    s_logger.debug("Failed to backup snapshot: " + result);
                    return new BackupSnapshotAnswer(command, false, "Failed to backup snapshot: " + result, null, true);
                }
            }
        } catch (final LibvirtException e) {
            return new BackupSnapshotAnswer(command, false, e.toString(), null, true);
        } catch (final CloudRuntimeException e) {
            return new BackupSnapshotAnswer(command, false, e.toString(), null, true);
        } finally {
            if (secondaryStoragePool != null) {
                storagePoolMgr.deleteStoragePool(secondaryStoragePool.getType(), secondaryStoragePool.getUuid());
            }
        }
        return new BackupSnapshotAnswer(command, true, null, snapshotRelPath + File.separator + snapshotName, true);
    }
}
