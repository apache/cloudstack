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

package com.cloud.hypervisor.xenserver.resource.wrapper.xen56p1;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.xmlrpc.XmlRpcException;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.FenceAnswer;
import com.cloud.agent.api.FenceCommand;
import com.cloud.hypervisor.xenserver.resource.XenServer56Resource;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.xensource.xenapi.Connection;
import com.xensource.xenapi.Types.XenAPIException;
import com.xensource.xenapi.VBD;
import com.xensource.xenapi.VDI;
import com.xensource.xenapi.VM;

@ResourceWrapper(handles =  FenceCommand.class)
public final class XenServer56FP1FenceCommandWrapper extends CommandWrapper<FenceCommand, Answer, XenServer56Resource> {

    private static final Logger s_logger = Logger.getLogger(XenServer56FP1FenceCommandWrapper.class);

    @Override
    public Answer execute(final FenceCommand command, final XenServer56Resource xenServer56) {
        final Connection conn = xenServer56.getConnection();
        try {
            final Boolean alive = xenServer56.checkHeartbeat(command.getHostGuid());
            if ( alive == null ) {
                s_logger.debug("Failed to check heartbeat,  so unable to fence");
                return new FenceAnswer(command, false, "Failed to check heartbeat, so unable to fence");
            }
            if ( alive ) {
                s_logger.debug("Heart beat is still going so unable to fence");
                return new FenceAnswer(command, false, "Heartbeat is still going on unable to fence");
            }
            final Set<VM> vms = VM.getByNameLabel(conn, command.getVmName());
            for (final VM vm : vms) {
                final Set<VDI> vdis = new HashSet<VDI>();
                final Set<VBD> vbds = vm.getVBDs(conn);
                for (final VBD vbd : vbds) {
                    final VDI vdi = vbd.getVDI(conn);
                    if (!xenServer56.isRefNull(vdi)) {
                        vdis.add(vdi);
                    }
                }
                s_logger.info("Fence command for VM " + command.getVmName());
                vm.powerStateReset(conn);
                vm.destroy(conn);
                for (final VDI vdi : vdis) {
                    final Map<String, String> smConfig = vdi.getSmConfig(conn);
                    for (final String key : smConfig.keySet()) {
                        if (key.startsWith("host_")) {
                            vdi.removeFromSmConfig(conn, key);
                            break;
                        }
                    }
                }
            }
            return new FenceAnswer(command);
        } catch (final XmlRpcException e) {
            s_logger.warn("Unable to fence", e);
            return new FenceAnswer(command, false, e.getMessage());
        } catch (final XenAPIException e) {
            s_logger.warn("Unable to fence", e);
            return new FenceAnswer(command, false, e.getMessage());
        } catch (final Exception e) {
            s_logger.warn("Unable to fence", e);
            return new FenceAnswer(command, false, e.getMessage());
        }
    }
}
