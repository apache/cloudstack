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
import java.util.HashMap;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.xmlrpc.XmlRpcException;

import static com.cloud.hypervisor.xenserver.discoverer.XcpServerDiscoverer.isUefiSupported;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.ReadyAnswer;
import com.cloud.agent.api.ReadyCommand;
import com.cloud.hypervisor.xenserver.resource.CitrixHelper;
import com.cloud.hypervisor.xenserver.resource.CitrixResourceBase;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.xensource.xenapi.Connection;
import com.xensource.xenapi.Host;
import com.xensource.xenapi.Types.XenAPIException;
import com.xensource.xenapi.VM;

@ResourceWrapper(handles =  ReadyCommand.class)
public final class CitrixReadyCommandWrapper extends CommandWrapper<ReadyCommand, Answer, CitrixResourceBase> {

    private static final Logger s_logger = Logger.getLogger(CitrixReadyCommandWrapper.class);

    @Override
    public Answer execute(final ReadyCommand command, final CitrixResourceBase citrixResourceBase) {
        final Connection conn = citrixResourceBase.getConnection();
        final Long dcId = command.getDataCenterId();
        Map<String, String> hostDetails = new HashMap<String, String>();
        // Ignore the result of the callHostPlugin. Even if unmounting the
        // snapshots dir fails, let Ready command
        // succeed.
        citrixResourceBase.umountSnapshotDir(conn, dcId);

        citrixResourceBase.setupLinkLocalNetwork(conn);
        // try to destroy CD-ROM device for all system VMs on this host
        try {
            final Host host = Host.getByUuid(conn, citrixResourceBase.getHost().getUuid());
            final Set<VM> vms = host.getResidentVMs(conn);
            citrixResourceBase.destroyPatchVbd(conn, vms);

            final Host.Record hr = host.getRecord(conn);
            if (isUefiSupported(CitrixHelper.getProductVersion(hr))) {
                hostDetails.put(com.cloud.host.Host.HOST_UEFI_ENABLE, Boolean.TRUE.toString());
            }
        } catch (final Exception e) {
        }
        try {
            final boolean result = citrixResourceBase.cleanupHaltedVms(conn);
            if (!result) {
                return new ReadyAnswer(command, "Unable to cleanup halted vms");
            }
        } catch (final XenAPIException e) {
            s_logger.warn("Unable to cleanup halted vms", e);
            return new ReadyAnswer(command, "Unable to cleanup halted vms");
        } catch (final XmlRpcException e) {
            s_logger.warn("Unable to cleanup halted vms", e);
            return new ReadyAnswer(command, "Unable to cleanup halted vms");
        }

        return new ReadyAnswer(command, hostDetails);
    }
}
