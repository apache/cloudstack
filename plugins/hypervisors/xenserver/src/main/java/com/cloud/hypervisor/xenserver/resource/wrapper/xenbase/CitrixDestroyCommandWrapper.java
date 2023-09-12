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
import com.cloud.agent.api.storage.DestroyCommand;
import com.cloud.agent.api.to.VolumeTO;
import com.cloud.hypervisor.xenserver.resource.CitrixResourceBase;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.xensource.xenapi.Connection;
import com.xensource.xenapi.VBD;
import com.xensource.xenapi.VDI;

@ResourceWrapper(handles =  DestroyCommand.class)
public final class CitrixDestroyCommandWrapper extends CommandWrapper<DestroyCommand, Answer, CitrixResourceBase> {

    private static final Logger s_logger = Logger.getLogger(CitrixDestroyCommandWrapper.class);

    @Override
    public Answer execute(final DestroyCommand command, final CitrixResourceBase citrixResourceBase) {
        final Connection conn = citrixResourceBase.getConnection();
        final VolumeTO vol = command.getVolume();
        // Look up the VDI
        final String volumeUUID = vol.getPath();
        VDI vdi = null;
        try {
            vdi = citrixResourceBase.getVDIbyUuid(conn, volumeUUID);
        } catch (final Exception e) {
            return new Answer(command, true, "Success");
        }
        Set<VBD> vbds = null;
        try {
            vbds = vdi.getVBDs(conn);
        } catch (final Exception e) {
            final String msg = "VDI getVBDS for " + volumeUUID + " failed due to " + e.toString();
            s_logger.warn(msg, e);
            return new Answer(command, false, msg);
        }
        for (final VBD vbd : vbds) {
            try {
                vbd.unplug(conn);
                vbd.destroy(conn);
            } catch (final Exception e) {
                final String msg = "VM destroy for " + volumeUUID + "  failed due to " + e.toString();
                s_logger.warn(msg, e);
                return new Answer(command, false, msg);
            }
        }
        try {
            final Set<VDI> snapshots = vdi.getSnapshots(conn);
            for (final VDI snapshot : snapshots) {
                snapshot.destroy(conn);
            }
            vdi.destroy(conn);
        } catch (final Exception e) {
            final String msg = "VDI destroy for " + volumeUUID + " failed due to " + e.toString();
            s_logger.warn(msg, e);
            return new Answer(command, false, msg);
        }

        return new Answer(command, true, "Success");
    }
}
