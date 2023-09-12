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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.cloudstack.storage.to.VolumeObjectTO;
import org.apache.log4j.Logger;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.RevertToVMSnapshotAnswer;
import com.cloud.agent.api.RevertToVMSnapshotCommand;
import com.cloud.hypervisor.xenserver.resource.CitrixResourceBase;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.vm.VirtualMachine.PowerState;
import com.cloud.vm.snapshot.VMSnapshot;
import com.xensource.xenapi.Connection;
import com.xensource.xenapi.Types;
import com.xensource.xenapi.VBD;
import com.xensource.xenapi.VDI;
import com.xensource.xenapi.VM;

@ResourceWrapper(handles =  RevertToVMSnapshotCommand.class)
public final class CitrixRevertToVMSnapshotCommandWrapper extends CommandWrapper<RevertToVMSnapshotCommand, Answer, CitrixResourceBase> {

    private static final Logger s_logger = Logger.getLogger(CitrixRevertToVMSnapshotCommandWrapper.class);

    @Override
    public Answer execute(final RevertToVMSnapshotCommand command, final CitrixResourceBase citrixResourceBase) {
        final String vmName = command.getVmName();
        final List<VolumeObjectTO> listVolumeTo = command.getVolumeTOs();
        final VMSnapshot.Type vmSnapshotType = command.getTarget().getType();
        final Boolean snapshotMemory = vmSnapshotType == VMSnapshot.Type.DiskAndMemory;
        final Connection conn = citrixResourceBase.getConnection();
        PowerState vmState = null;
        VM vm = null;
        try {

            final Set<VM> vmSnapshots = VM.getByNameLabel(conn, command.getTarget().getSnapshotName());
            if (vmSnapshots == null || vmSnapshots.size() == 0) {
                return new RevertToVMSnapshotAnswer(command, false, "Cannot find vmSnapshot with name: " + command.getTarget().getSnapshotName());
            }

            final VM vmSnapshot = vmSnapshots.iterator().next();

            // find target VM or creating a work VM
            try {
                vm = citrixResourceBase.getVM(conn, vmName);
            } catch (final Exception e) {
                vm = citrixResourceBase.createWorkingVM(conn, vmName, command.getGuestOSType(), command.getPlatformEmulator(), listVolumeTo);
            }

            if (vm == null) {
                return new RevertToVMSnapshotAnswer(command, false, "Revert to VM Snapshot Failed due to can not find vm: " + vmName);
            }

            // call plugin to execute revert
            citrixResourceBase.revertToSnapshot(conn, vmSnapshot, vmName, vm.getUuid(conn), snapshotMemory, citrixResourceBase.getHost().getUuid());
            vm = citrixResourceBase.getVM(conn, vmName);
            final Set<VBD> vbds = vm.getVBDs(conn);
            final Map<String, VDI> vdiMap = new HashMap<String, VDI>();
            // get vdi:vbdr to a map
            for (final VBD vbd : vbds) {
                final VBD.Record vbdr = vbd.getRecord(conn);
                if (vbdr.type == Types.VbdType.DISK) {
                    final VDI vdi = vbdr.VDI;
                    vdiMap.put(vbdr.userdevice, vdi);
                }
            }

            if (!snapshotMemory) {
                vm.destroy(conn);
                vmState = PowerState.PowerOff;
            } else {
                vmState = PowerState.PowerOn;
            }

            // after revert, VM's volumes path have been changed, need to report to manager
            for (final VolumeObjectTO volumeTo : listVolumeTo) {
                final Long deviceId = volumeTo.getDeviceId();
                final VDI vdi = vdiMap.get(deviceId.toString());
                volumeTo.setPath(vdi.getUuid(conn));
            }

            return new RevertToVMSnapshotAnswer(command, listVolumeTo, vmState);
        } catch (final Exception e) {
            s_logger.error("revert vm " + vmName + " to snapshot " + command.getTarget().getSnapshotName() + " failed due to " + e.getMessage());
            return new RevertToVMSnapshotAnswer(command, false, e.getMessage());
        }
    }
}
