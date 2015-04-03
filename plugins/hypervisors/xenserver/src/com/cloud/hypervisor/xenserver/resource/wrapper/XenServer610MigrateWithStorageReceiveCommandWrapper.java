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

package com.cloud.hypervisor.xenserver.resource.wrapper;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.MigrateWithStorageReceiveAnswer;
import com.cloud.agent.api.MigrateWithStorageReceiveCommand;
import com.cloud.agent.api.to.NicTO;
import com.cloud.agent.api.to.StorageFilerTO;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.agent.api.to.VolumeTO;
import com.cloud.hypervisor.xenserver.resource.XenServer610Resource;
import com.cloud.hypervisor.xenserver.resource.XsHost;
import com.cloud.hypervisor.xenserver.resource.XsLocalNetwork;
import com.cloud.network.Networks.TrafficType;
import com.cloud.resource.CommandWrapper;
import com.cloud.utils.exception.CloudRuntimeException;
import com.xensource.xenapi.Connection;
import com.xensource.xenapi.Host;
import com.xensource.xenapi.Network;
import com.xensource.xenapi.SR;

public final class XenServer610MigrateWithStorageReceiveCommandWrapper extends CommandWrapper<MigrateWithStorageReceiveCommand, Answer, XenServer610Resource> {

    private static final Logger s_logger = Logger.getLogger(XenServer610MigrateWithStorageReceiveCommandWrapper.class);

    @Override
    public Answer execute(final MigrateWithStorageReceiveCommand command, final XenServer610Resource xenServer610Resource) {
        final Connection connection = xenServer610Resource.getConnection();
        final VirtualMachineTO vmSpec = command.getVirtualMachine();
        final Map<VolumeTO, StorageFilerTO> volumeToFiler = command.getVolumeToFiler();

        try {
            // Get a map of all the SRs to which the vdis will be migrated.
            final Map<VolumeTO, Object> volumeToSr = new HashMap<VolumeTO, Object>();
            for (final Map.Entry<VolumeTO, StorageFilerTO> entry : volumeToFiler.entrySet()) {
                final StorageFilerTO storageFiler = entry.getValue();
                final SR sr = xenServer610Resource.getStorageRepository(connection, storageFiler.getUuid());
                volumeToSr.put(entry.getKey(), sr);
            }

            // Get the list of networks to which the vifs will attach.
            final Map<NicTO, Object> nicToNetwork = new HashMap<NicTO, Object>();
            for (final NicTO nicTo : vmSpec.getNics()) {
                final Network network = xenServer610Resource.getNetwork(connection, nicTo);
                nicToNetwork.put(nicTo, network);
            }

            final XsLocalNetwork nativeNetworkForTraffic = xenServer610Resource.getNativeNetworkForTraffic(connection, TrafficType.Storage, null);
            final Network network = nativeNetworkForTraffic.getNetwork();
            final XsHost xsHost = xenServer610Resource.getHost();
            final String uuid = xsHost.getUuid();

            final Map<String, String> other = new HashMap<String, String>();
            other.put("live", "true");

            final Host host = Host.getByUuid(connection, uuid);
            final Map<String, String> token = host.migrateReceive(connection, network, other);

            return new MigrateWithStorageReceiveAnswer(command, volumeToSr, nicToNetwork, token);
        } catch (final CloudRuntimeException e) {
            s_logger.error("Migration of vm " + vmSpec.getName() + " with storage failed due to " + e.toString(), e);
            return new MigrateWithStorageReceiveAnswer(command, e);
        } catch (final Exception e) {
            s_logger.error("Migration of vm " + vmSpec.getName() + " with storage failed due to " + e.toString(), e);
            return new MigrateWithStorageReceiveAnswer(command, e);
        }
    }
}