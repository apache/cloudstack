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
import com.cloud.agent.api.OvsFetchInterfaceAnswer;
import com.cloud.agent.api.OvsFetchInterfaceCommand;
import com.cloud.hypervisor.xenserver.resource.CitrixResourceBase;
import com.cloud.hypervisor.xenserver.resource.XsLocalNetwork;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.utils.exception.CloudRuntimeException;
import com.xensource.xenapi.Connection;
import com.xensource.xenapi.PIF;
import com.xensource.xenapi.Types.BadServerResponse;
import com.xensource.xenapi.Types.XenAPIException;

@ResourceWrapper(handles =  OvsFetchInterfaceCommand.class)
public final class CitrixOvsFetchInterfaceCommandWrapper extends CommandWrapper<OvsFetchInterfaceCommand, Answer, CitrixResourceBase> {

    private static final Logger s_logger = Logger.getLogger(CitrixOvsFetchInterfaceCommandWrapper.class);

    @Override
    public Answer execute(final OvsFetchInterfaceCommand command, final CitrixResourceBase citrixResourceBase) {
        String label = command.getLabel();
        //FIXME: this is a tricky to pass the network checking in XCP. I temporary get default label from Host.
        if (citrixResourceBase.isXcp()) {
            label = citrixResourceBase.getLabel();
        }
        s_logger.debug("Will look for network with name-label:" + label + " on host " + citrixResourceBase.getHost().getIp());
        final Connection conn = citrixResourceBase.getConnection();
        try {
            final XsLocalNetwork nw = citrixResourceBase.getNetworkByName(conn, label);
            if(nw == null) {
                throw new CloudRuntimeException("Unable to locate the network with name-label: " + label + " on host: " + citrixResourceBase.getHost().getIp());
            }
            s_logger.debug("Network object:" + nw.getNetwork().getUuid(conn));
            final PIF pif = nw.getPif(conn);
            final PIF.Record pifRec = pif.getRecord(conn);
            s_logger.debug("PIF object:" + pifRec.uuid + "(" + pifRec.device + ")");
            return new OvsFetchInterfaceAnswer(command, true, "Interface " + pifRec.device + " retrieved successfully", pifRec.IP, pifRec.netmask, pifRec.MAC);
        } catch (final BadServerResponse e) {
            s_logger.error("An error occurred while fetching the interface for " + label + " on host " + citrixResourceBase.getHost().getIp(), e);
            return new OvsFetchInterfaceAnswer(command, false, "EXCEPTION:" + e.getMessage());
        } catch (final XenAPIException e) {
            s_logger.error("An error occurred while fetching the interface for " + label + " on host " + citrixResourceBase.getHost().getIp(), e);
            return new OvsFetchInterfaceAnswer(command, false, "EXCEPTION:" + e.getMessage());
        } catch (final XmlRpcException e) {
            s_logger.error("An error occurred while fetching the interface for " + label + " on host " + citrixResourceBase.getHost().getIp(), e);
            return new OvsFetchInterfaceAnswer(command, false, "EXCEPTION:" + e.getMessage());
        }
    }
}
