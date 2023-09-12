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
import com.cloud.agent.api.PlugNicAnswer;
import com.cloud.agent.api.PlugNicCommand;
import com.cloud.agent.api.to.NicTO;
import com.cloud.hypervisor.xenserver.resource.CitrixResourceBase;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.xensource.xenapi.Connection;
import com.xensource.xenapi.VIF;
import com.xensource.xenapi.VM;

@ResourceWrapper(handles =  PlugNicCommand.class)
public final class CitrixPlugNicCommandWrapper extends CommandWrapper<PlugNicCommand, Answer, CitrixResourceBase> {

    private static final Logger s_logger = Logger.getLogger(CitrixPlugNicCommandWrapper.class);

    @Override
    public Answer execute(final PlugNicCommand command, final CitrixResourceBase citrixResourceBase) {
        final Connection conn = citrixResourceBase.getConnection();
        final String vmName = command.getVmName();
        try {
            final Set<VM> vms = VM.getByNameLabel(conn, vmName);
            if (vms == null || vms.isEmpty()) {
                return new PlugNicAnswer(command, false, "Can not find VM " + vmName);
            }
            final VM vm = vms.iterator().next();
            final NicTO nic = command.getNic();

            String mac = nic.getMac();
            final Set<VIF> routerVIFs = vm.getVIFs(conn);
            mac = mac.trim();

            int counter = 0;
            for (final VIF vif : routerVIFs) {
                final String lmac = vif.getMAC(conn);
                if (lmac.trim().equals(mac)) {
                    counter++;
                }
            }
            // We allow 2 routers with the same mac. It's needed for the redundant vpc routers.
            // [FIXME] Find a way to identify the type of the router or if it's
            // redundant.
            if (counter > 2) {
                final String msg = " Plug Nic failed due to a VIF with the same mac " + nic.getMac() + " exists in more than 2 routers.";
                s_logger.error(msg);
                return new PlugNicAnswer(command, false, msg);
            }

            // Wilder Rodrigues - replaced this code with the code above.
            // VIF vif = getVifByMac(conn, vm, nic.getMac());
            // if (vif != null) {
            // final String msg = " Plug Nic failed due to a VIF with the same mac " + nic.getMac() + " exists";
            // s_logger.warn(msg);
            // return new PlugNicAnswer(cmd, false, msg);
            // }

            final String deviceId = citrixResourceBase.getLowestAvailableVIFDeviceNum(conn, vm);
            nic.setDeviceId(Integer.parseInt(deviceId));
            final VIF vif = citrixResourceBase.createVif(conn, vmName, vm, null, nic);
            // vif = createVif(conn, vmName, vm, null, nic);
            vif.plug(conn);
            return new PlugNicAnswer(command, true, "success");
        } catch (final Exception e) {
            final String msg = " Plug Nic failed due to " + e.toString();
            s_logger.error(msg, e);
            return new PlugNicAnswer(command, false, msg);
        }
    }
}
