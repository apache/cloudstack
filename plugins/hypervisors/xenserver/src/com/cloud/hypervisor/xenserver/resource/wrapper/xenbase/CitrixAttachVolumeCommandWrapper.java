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

import java.util.Set;

import org.apache.log4j.Logger;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.AttachVolumeAnswer;
import com.cloud.agent.api.AttachVolumeCommand;
import com.cloud.hypervisor.xenserver.resource.CitrixResourceBase;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.xensource.xenapi.Connection;
import com.xensource.xenapi.SR;
import com.xensource.xenapi.Types;
import com.xensource.xenapi.Types.XenAPIException;
import com.xensource.xenapi.VBD;
import com.xensource.xenapi.VDI;
import com.xensource.xenapi.VM;

@ResourceWrapper(handles =  AttachVolumeCommand.class)
public final class CitrixAttachVolumeCommandWrapper extends CommandWrapper<AttachVolumeCommand, Answer, CitrixResourceBase> {

    private static final Logger s_logger = Logger.getLogger(CitrixAttachVolumeCommandWrapper.class);

    @Override
    public Answer execute(final AttachVolumeCommand command, final CitrixResourceBase citrixResourceBase) {
        final Connection conn = citrixResourceBase.getConnection();
        final boolean attach = command.getAttach();
        final String vmName = command.getVmName();
        final String vdiNameLabel = vmName + "-DATA";
        final Long deviceId = command.getDeviceId();

        String errorMsg;
        if (attach) {
            errorMsg = "Failed to attach volume";
        } else {
            errorMsg = "Failed to detach volume";
        }

        try {
            VDI vdi = null;

            if (command.getAttach() && command.isManaged()) {
                final SR sr = citrixResourceBase.getIscsiSR(conn, command.get_iScsiName(), command.getStorageHost(), command.get_iScsiName(), command.getChapInitiatorUsername(),
                        command.getChapInitiatorPassword(), true);

                vdi = citrixResourceBase.getVDIbyUuid(conn, command.getVolumePath(), false);

                if (vdi == null) {
                    vdi = citrixResourceBase.createVdi(sr, vdiNameLabel, command.getVolumeSize());
                }
            } else {
                vdi = citrixResourceBase.getVDIbyUuid(conn, command.getVolumePath());
            }

            // Look up the VM
            final VM vm = citrixResourceBase.getVM(conn, vmName);
            if (attach) {
                // Figure out the disk number to attach the VM to
                String diskNumber = null;
                if (deviceId != null) {
                    if (deviceId.longValue() == 3) {
                        final String msg = "Device 3 is reserved for CD-ROM, choose other device";
                        return new AttachVolumeAnswer(command, msg);
                    }
                    if (citrixResourceBase.isDeviceUsed(conn, vm, deviceId)) {
                        final String msg = "Device " + deviceId + " is used in VM " + vmName;
                        return new AttachVolumeAnswer(command, msg);
                    }
                    diskNumber = deviceId.toString();
                } else {
                    diskNumber = citrixResourceBase.getUnusedDeviceNum(conn, vm);
                }
                // Create a new VBD
                final VBD.Record vbdr = new VBD.Record();
                vbdr.VM = vm;
                vbdr.VDI = vdi;
                vbdr.bootable = false;
                vbdr.userdevice = diskNumber;
                vbdr.mode = Types.VbdMode.RW;
                vbdr.type = Types.VbdType.DISK;
                vbdr.unpluggable = true;
                final VBD vbd = VBD.create(conn, vbdr);

                // Attach the VBD to the VM
                vbd.plug(conn);

                // Update the VDI's label to include the VM name
                vdi.setNameLabel(conn, vdiNameLabel);

                return new AttachVolumeAnswer(command, Long.parseLong(diskNumber), vdi.getUuid(conn));
            } else {
                // Look up all VBDs for this VDI
                final Set<VBD> vbds = vdi.getVBDs(conn);

                // Detach each VBD from its VM, and then destroy it
                for (final VBD vbd : vbds) {
                    final VBD.Record vbdr = vbd.getRecord(conn);

                    if (vbdr.currentlyAttached) {
                        vbd.unplug(conn);
                    }

                    vbd.destroy(conn);
                }

                // Update the VDI's label to be "detached"
                vdi.setNameLabel(conn, "detached");

                if (command.isManaged()) {
                    citrixResourceBase.handleSrAndVdiDetach(command.get_iScsiName(), conn);
                }

                return new AttachVolumeAnswer(command);
            }
        } catch (final XenAPIException e) {
            final String msg = errorMsg + " for uuid: " + command.getVolumePath() + "  due to " + e.toString();
            s_logger.warn(msg, e);
            return new AttachVolumeAnswer(command, msg);
        } catch (final Exception e) {
            final String msg = errorMsg + " for uuid: " + command.getVolumePath() + "  due to " + e.getMessage();
            s_logger.warn(msg, e);
            return new AttachVolumeAnswer(command, msg);
        }
    }
}