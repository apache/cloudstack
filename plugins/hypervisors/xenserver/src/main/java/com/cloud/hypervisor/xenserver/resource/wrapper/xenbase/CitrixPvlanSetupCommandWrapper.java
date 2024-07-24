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

import org.apache.log4j.Logger;
import org.apache.xmlrpc.XmlRpcException;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.PvlanSetupCommand;
import com.cloud.hypervisor.xenserver.resource.CitrixResourceBase;
import com.cloud.hypervisor.xenserver.resource.XsLocalNetwork;
import com.cloud.network.Networks.TrafficType;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.utils.exception.CloudRuntimeException;
import com.xensource.xenapi.Connection;
import com.xensource.xenapi.Types.XenAPIException;

@ResourceWrapper(handles =  PvlanSetupCommand.class)
public final class CitrixPvlanSetupCommandWrapper extends CommandWrapper<PvlanSetupCommand, Answer, CitrixResourceBase> {

    private static final Logger s_logger = Logger.getLogger(CitrixPvlanSetupCommandWrapper.class);

    @Override
    public Answer execute(final PvlanSetupCommand command, final CitrixResourceBase citrixResourceBase) {
        final Connection conn = citrixResourceBase.getConnection();

        final String primaryPvlan = command.getPrimary();
        final String isolatedPvlan = command.getIsolated();
        final String op = command.getOp();
        final String dhcpName = command.getDhcpName();
        final String dhcpMac = command.getDhcpMac();
        final String dhcpIp = command.getDhcpIp();
        final String vmMac = command.getVmMac();
        final String networkTag = command.getNetworkTag();

        String nwNameLabel = null;
        try {
            final XsLocalNetwork nw = citrixResourceBase.getNativeNetworkForTraffic(conn, TrafficType.Guest, networkTag);
            if (nw == null) {
                s_logger.error("Network is not configured on the backend for pvlan " + primaryPvlan);
                throw new CloudRuntimeException("Network for the backend is not configured correctly for pvlan primary: " + primaryPvlan);
            }
            nwNameLabel = nw.getNetwork().getNameLabel(conn);
        } catch (final XenAPIException e) {
            s_logger.warn("Fail to get network", e);
            return new Answer(command, false, e.toString());
        } catch (final XmlRpcException e) {
            s_logger.warn("Fail to get network", e);
            return new Answer(command, false, e.toString());
        }

        String result = null;
        if (command.getType() == PvlanSetupCommand.Type.DHCP) {
            result = citrixResourceBase.callHostPlugin(conn, "ovs-pvlan", "setup-pvlan-dhcp", "op", op, "nw-label", nwNameLabel, "primary-pvlan", primaryPvlan, "isolated-pvlan",
                    isolatedPvlan, "dhcp-name", dhcpName, "dhcp-ip", dhcpIp, "dhcp-mac", dhcpMac);

            if (result == null || result.isEmpty() || !Boolean.parseBoolean(result)) {
                s_logger.warn("Failed to program pvlan for dhcp server with mac " + dhcpMac);
                return new Answer(command, false, result);
            } else {
                s_logger.info("Programmed pvlan for dhcp server with mac " + dhcpMac);
            }
        } else if (command.getType() == PvlanSetupCommand.Type.VM) {
            result = citrixResourceBase.callHostPlugin(conn, "ovs-pvlan", "setup-pvlan-vm", "op", op, "nw-label", nwNameLabel, "primary-pvlan", primaryPvlan, "isolated-pvlan",
                    isolatedPvlan, "vm-mac", vmMac);

            if (result == null || result.isEmpty() || !Boolean.parseBoolean(result)) {
                s_logger.warn("Failed to program pvlan for vm with mac " + vmMac);
                return new Answer(command, false, result);
            } else {
                s_logger.info("Programmed pvlan for vm with mac " + vmMac);
            }
        }
        return new Answer(command, true, result);
    }
}
