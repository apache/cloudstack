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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.storage.to.VolumeObjectTO;
import org.apache.log4j.Logger;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.DeleteVMSnapshotAnswer;
import com.cloud.agent.api.DeleteVMSnapshotCommand;
import com.cloud.hypervisor.xenserver.resource.CitrixResourceBase;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.vm.snapshot.VMSnapshot;
import com.xensource.xenapi.Connection;
import com.xensource.xenapi.Types;
import com.xensource.xenapi.VBD;
import com.xensource.xenapi.VDI;
import com.xensource.xenapi.VM;

@ResourceWrapper(handles =  DeleteVMSnapshotCommand.class)
public final class CitrixDeleteVMSnapshotCommandWrapper extends CommandWrapper<DeleteVMSnapshotCommand, Answer, CitrixResourceBase> {

    private static final Logger s_logger = Logger.getLogger(CitrixDeleteVMSnapshotCommandWrapper.class);

    @Override
    public Answer execute(final DeleteVMSnapshotCommand command, final CitrixResourceBase citrixResourceBase) {
        final String snapshotName = command.getTarget().getSnapshotName();
        final Connection conn = citrixResourceBase.getConnection();

        try {
            final List<VDI> vdiList = new ArrayList<VDI>();
            final Set<VM> snapshots = VM.getByNameLabel(conn, snapshotName);
            if (snapshots == null || snapshots.size() == 0) {
                s_logger.warn("VM snapshot with name " + snapshotName + " does not exist, assume it is already deleted");
                return new DeleteVMSnapshotAnswer(command, command.getVolumeTOs());
            }
            final VM snapshot = snapshots.iterator().next();
            final Set<VBD> vbds = snapshot.getVBDs(conn);
            for (final VBD vbd : vbds) {
                if (vbd.getType(conn) == Types.VbdType.DISK) {
                    final VDI vdi = vbd.getVDI(conn);
                    vdiList.add(vdi);
                }
            }
            if (command.getTarget().getType() == VMSnapshot.Type.DiskAndMemory) {
                vdiList.add(snapshot.getSuspendVDI(conn));
            }
            snapshot.destroy(conn);
            for (final VDI vdi : vdiList) {
                vdi.destroy(conn);
            }

            try {
                Thread.sleep(5000);
            } catch (final InterruptedException ex) {

            }
            // re-calculate used capacify for this VM snapshot
            for (final VolumeObjectTO volumeTo : command.getVolumeTOs()) {
                try {
                    final long size = citrixResourceBase.getVMSnapshotChainSize(conn, volumeTo, command.getVmName(), snapshotName);
                    volumeTo.setSize(size);
                } catch (final CloudRuntimeException cre) {

                }
            }

            return new DeleteVMSnapshotAnswer(command, command.getVolumeTOs());
        } catch (final Exception e) {
            s_logger.warn("Catch Exception: " + e.getClass().toString() + " due to " + e.toString(), e);
            return new DeleteVMSnapshotAnswer(command, false, e.getMessage());
        }
    }
}
