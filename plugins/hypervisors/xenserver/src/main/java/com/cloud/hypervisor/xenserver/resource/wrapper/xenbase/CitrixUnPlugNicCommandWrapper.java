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
import java.util.Set;

import org.apache.commons.collections.MapUtils;
import org.apache.log4j.Logger;
import org.apache.xmlrpc.XmlRpcException;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.UnPlugNicAnswer;
import com.cloud.agent.api.UnPlugNicCommand;
import com.cloud.agent.api.to.NicTO;
import com.cloud.hypervisor.xenserver.resource.CitrixResourceBase;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.xensource.xenapi.Connection;
import com.xensource.xenapi.Network;
import com.xensource.xenapi.Types;
import com.xensource.xenapi.VIF;
import com.xensource.xenapi.VM;

@ResourceWrapper(handles =  UnPlugNicCommand.class)
public final class CitrixUnPlugNicCommandWrapper extends CommandWrapper<UnPlugNicCommand, Answer, CitrixResourceBase> {

    private static final Logger s_logger = Logger.getLogger(CitrixUnPlugNicCommandWrapper.class);

    @Override
    public Answer execute(final UnPlugNicCommand command, final CitrixResourceBase citrixResourceBase) {
        final Connection conn = citrixResourceBase.getConnection();
        final String vmName = command.getVmName();
        final Map<String, Boolean> vlanToPersistenceMap = command.getVlanToPersistenceMap();
        try {
            final Set<VM> vms = VM.getByNameLabel(conn, vmName);
            if (vms == null || vms.isEmpty()) {
                return new UnPlugNicAnswer(command, false, "Can not find VM " + vmName);
            }
            final VM vm = vms.iterator().next();
            final NicTO nic = command.getNic();
            final String mac = nic.getMac();
            final VIF vif = citrixResourceBase.getVifByMac(conn, vm, mac);
            if (vif != null) {
                vif.unplug(conn);
                final Network network = vif.getNetwork(conn);
                vif.destroy(conn);
                try {
                    if (network.getNameLabel(conn).startsWith("VLAN")) {
                        String networkLabel = network.getNameLabel(conn);
                        citrixResourceBase.disableVlanNetwork(conn, network, shouldDeleteVlan(networkLabel, vlanToPersistenceMap));
                    }
                } catch (final Exception e) {
                }
            }
            return new UnPlugNicAnswer(command, true, "success");
        } catch (final Exception e) {
            final String msg = " UnPlug Nic failed due to " + e.toString();
            s_logger.warn(msg, e);
            return new UnPlugNicAnswer(command, false, msg);
        }
    }

    private boolean shouldDeleteVlan(String networkLabel, Map<String, Boolean> vlanToPersistenceMap) throws XmlRpcException, Types.XenAPIException {
        String[] networkNameParts = networkLabel.split("-");
        String networkVlan = networkNameParts[networkNameParts.length -1];
        if (MapUtils.isNotEmpty(vlanToPersistenceMap) && networkVlan != null && vlanToPersistenceMap.containsKey(networkVlan)) {
            return vlanToPersistenceMap.get(networkVlan);
        }
        return true;
    }
}
