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

import java.util.Map;
import com.cloud.agent.api.GetVmIpAddressCommand;
import com.cloud.resource.ResourceWrapper;
import com.cloud.utils.net.NetUtils;
import com.xensource.xenapi.Connection;
import com.xensource.xenapi.VM;
import com.xensource.xenapi.VMGuestMetrics;
import com.xensource.xenapi.Types;
import org.apache.log4j.Logger;

import com.cloud.agent.api.Answer;
import com.cloud.hypervisor.xenserver.resource.CitrixResourceBase;
import com.cloud.resource.CommandWrapper;
import org.apache.xmlrpc.XmlRpcException;

@ResourceWrapper(handles =  GetVmIpAddressCommand.class)
public final class CitrixGetVmIpAddressCommandWrapper extends CommandWrapper<GetVmIpAddressCommand, Answer, CitrixResourceBase> {

    private static final Logger s_logger = Logger.getLogger(CitrixGetVmIpAddressCommandWrapper.class);

    @Override
    public Answer execute(final GetVmIpAddressCommand command, final CitrixResourceBase citrixResourceBase) {
        final Connection conn = citrixResourceBase.getConnection();

        String vmName = command.getVmName();
        String networkCidr = command.getVmNetworkCidr();
        boolean result = false;
        String errorMsg = null;
        String vmIp = null;

        try {
            VM vm = citrixResourceBase.getVM(conn, vmName);
            VMGuestMetrics mtr = vm.getGuestMetrics(conn);
            VMGuestMetrics.Record rec = mtr.getRecord(conn);
            Map<String, String> vmIpsMap = rec.networks;

            for (String ipAddr: vmIpsMap.values()) {
                if (NetUtils.isIpWithInCidrRange(ipAddr, networkCidr)) {
                    vmIp = ipAddr;
                    break;
                }
            }

            if (vmIp != null) {
                s_logger.debug("VM " +vmName + " ip address got retrieved "+vmIp);
                result = true;
                return new Answer(command, result, vmIp);
            }

        }catch (Types.XenAPIException e) {
            s_logger.debug("Got exception in GetVmIpAddressCommand "+ e.getMessage());
            errorMsg = "Failed to retrived vm ip addr, exception: "+e.getMessage();
        }catch (XmlRpcException e) {
            s_logger.debug("Got exception in GetVmIpAddressCommand "+ e.getMessage());
            errorMsg = "Failed to retrived vm ip addr, exception: "+e.getMessage();
        }

        return new Answer(command, result, errorMsg);

    }
}
