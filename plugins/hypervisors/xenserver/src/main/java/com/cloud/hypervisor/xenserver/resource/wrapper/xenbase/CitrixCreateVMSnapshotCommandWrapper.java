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

package com.cloud.hypervisor.xenserver.resource.wrapper.xenbase;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.storage.to.VolumeObjectTO;
import org.apache.log4j.Logger;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CreateVMSnapshotAnswer;
import com.cloud.agent.api.CreateVMSnapshotCommand;
import com.cloud.hypervisor.xenserver.resource.CitrixResourceBase;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.vm.snapshot.VMSnapshot;
import com.xensource.xenapi.Connection;
import com.xensource.xenapi.Pool;
import com.xensource.xenapi.SR;
import com.xensource.xenapi.Task;
import com.xensource.xenapi.Types;
import com.xensource.xenapi.Types.VmPowerState;
import com.xensource.xenapi.VBD;
import com.xensource.xenapi.VDI;
import com.xensource.xenapi.VM;

@ResourceWrapper(handles =  CreateVMSnapshotCommand.class)
public final class CitrixCreateVMSnapshotCommandWrapper extends CommandWrapper<CreateVMSnapshotCommand, Answer, CitrixResourceBase> {

    private static final Logger s_logger = Logger.getLogger(CitrixCreateVMSnapshotCommandWrapper.class);

    @Override
    public Answer execute(final CreateVMSnapshotCommand command, final CitrixResourceBase citrixResourceBase) {
        final String vmName = command.getVmName();
        final String vmSnapshotName = command.getTarget().getSnapshotName();
        final List<VolumeObjectTO> listVolumeTo = command.getVolumeTOs();

        VmPowerState vmState = VmPowerState.HALTED;

        final String guestOSType = command.getGuestOSType();
        final String platformEmulator = command.getPlatformEmulator();

        final boolean snapshotMemory = command.getTarget().getType() == VMSnapshot.Type.DiskAndMemory;
        final long timeout = command.getWait();

        final Connection conn = citrixResourceBase.getConnection();
        VM vm = null;
        VM vmSnapshot = null;
        boolean success = false;

        try {
            // check if VM snapshot already exists
            final Set<VM> vmSnapshots = VM.getByNameLabel(conn, command.getTarget().getSnapshotName());
            if (vmSnapshots == null || vmSnapshots.size() > 0) {
                return new CreateVMSnapshotAnswer(command, command.getTarget(), command.getVolumeTOs());
            }

            // check if there is already a task for this VM snapshot
            Task task = null;
            Set<Task> tasks = Task.getByNameLabel(conn, "Async.VM.snapshot");
            if(tasks == null) {
                tasks = new LinkedHashSet<>();
            }
            final Set<Task> tasksByName = Task.getByNameLabel(conn, "Async.VM.checkpoint");
            if(tasksByName != null) {
                tasks.addAll(tasksByName);
            }
            for (final Task taskItem : tasks) {
                if (taskItem.getOtherConfig(conn).containsKey("CS_VM_SNAPSHOT_KEY")) {
                    final String vmSnapshotTaskName = taskItem.getOtherConfig(conn).get("CS_VM_SNAPSHOT_KEY");
                    if (vmSnapshotTaskName != null && vmSnapshotTaskName.equals(command.getTarget().getSnapshotName())) {
                        task = taskItem;
                    }
                }
            }

            // create a new task if there is no existing task for this VM snapshot
            if (task == null) {
                try {
                    vm = citrixResourceBase.getVM(conn, vmName);
                    vmState = vm.getPowerState(conn);
                } catch (final Exception e) {
                    if (!snapshotMemory) {
                        vm = citrixResourceBase.createWorkingVM(conn, vmName, guestOSType, platformEmulator, listVolumeTo);
                    }
                }

                if (vm == null) {
                    return new CreateVMSnapshotAnswer(command, false, "Creating VM Snapshot Failed due to can not find vm: " + vmName);
                }

                // call Xenserver API
                if (!snapshotMemory) {
                    task = vm.snapshotAsync(conn, vmSnapshotName);
                } else {
                    final Set<VBD> vbds = vm.getVBDs(conn);
                    final Pool pool = Pool.getByUuid(conn, citrixResourceBase.getHost().getPool());
                    for (final VBD vbd : vbds) {
                        final VBD.Record vbdr = vbd.getRecord(conn);
                        if (vbdr.userdevice.equals("0")) {
                            final VDI vdi = vbdr.VDI;
                            final SR sr = vdi.getSR(conn);
                            // store memory image on the same SR with ROOT volume
                            pool.setSuspendImageSR(conn, sr);
                        }
                    }
                    task = vm.checkpointAsync(conn, vmSnapshotName);
                }
                task.addToOtherConfig(conn, "CS_VM_SNAPSHOT_KEY", vmSnapshotName);
            }

            citrixResourceBase.waitForTask(conn, task, 1000, timeout * 1000);
            citrixResourceBase.checkForSuccess(conn, task);
            final String result = task.getResult(conn);

            // extract VM snapshot ref from result
            final String ref = result.substring("<value>".length(), result.length() - "</value>".length());
            vmSnapshot = Types.toVM(ref);
            try {
                Thread.sleep(5000);
            } catch (final InterruptedException ex) {

            }
            // calculate used capacity for this VM snapshot
            for (final VolumeObjectTO volumeTo : command.getVolumeTOs()) {
                try {
                    final long size = citrixResourceBase.getVMSnapshotChainSize(conn, volumeTo, command.getVmName(), vmSnapshotName);
                    volumeTo.setSize(size);
                } catch (final CloudRuntimeException cre) {
                }
            }

            success = true;
            return new CreateVMSnapshotAnswer(command, command.getTarget(), command.getVolumeTOs());
        } catch (final Exception e) {
            String msg = "";
            if (e instanceof Types.BadAsyncResult) {
                final String licenseKeyWord = "LICENCE_RESTRICTION";
                final Types.BadAsyncResult errorResult = (Types.BadAsyncResult)e;
                if (errorResult.shortDescription != null && errorResult.shortDescription.contains(licenseKeyWord)) {
                    msg = licenseKeyWord;
                }
            } else {
                msg = e.toString();
            }
            s_logger.warn("Creating VM Snapshot " + command.getTarget().getSnapshotName() + " failed due to: " + msg, e);
            return new CreateVMSnapshotAnswer(command, false, msg);
        } finally {
            try {
                if (!success) {
                    if (vmSnapshot != null) {
                        s_logger.debug("Delete existing VM Snapshot " + vmSnapshotName + " after making VolumeTO failed");
                        final Set<VBD> vbds = vmSnapshot.getVBDs(conn);
                        for (final VBD vbd : vbds) {
                            final VBD.Record vbdr = vbd.getRecord(conn);
                            if (vbdr.type == Types.VbdType.DISK) {
                                final VDI vdi = vbdr.VDI;
                                vdi.destroy(conn);
                            }
                        }
                        vmSnapshot.destroy(conn);
                    }
                }
                if (vmState == VmPowerState.HALTED) {
                    if (vm != null) {
                        vm.destroy(conn);
                    }
                }
            } catch (final Exception e2) {
                s_logger.error("delete snapshot error due to " + e2.getMessage());
            }
        }
    }
}
