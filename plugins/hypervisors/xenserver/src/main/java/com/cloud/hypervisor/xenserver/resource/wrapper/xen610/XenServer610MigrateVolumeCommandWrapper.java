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

package com.cloud.hypervisor.xenserver.resource.wrapper.xen610;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.storage.MigrateVolumeAnswer;
import com.cloud.agent.api.storage.MigrateVolumeCommand;
import com.cloud.agent.api.to.DiskTO;
import com.cloud.agent.api.to.StorageFilerTO;
import com.cloud.hypervisor.xenserver.resource.CitrixHelper;
import com.cloud.hypervisor.xenserver.resource.XenServer610Resource;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.xensource.xenapi.Connection;
import com.xensource.xenapi.SR;
import com.xensource.xenapi.Task;
import com.xensource.xenapi.Types;
import com.xensource.xenapi.VDI;

@ResourceWrapper(handles =  MigrateVolumeCommand.class)
public final class XenServer610MigrateVolumeCommandWrapper extends CommandWrapper<MigrateVolumeCommand, Answer, XenServer610Resource> {
    private static final Logger LOGGER = Logger.getLogger(XenServer610MigrateVolumeCommandWrapper.class);

    @Override
    public Answer execute(final MigrateVolumeCommand command, final XenServer610Resource xenServer610Resource) {
        Connection connection = xenServer610Resource.getConnection();
        String srcVolumeUuid = command.getVolumePath();
        SR destPool = null;
        Map<String, String> destDetails = command.getDestDetails();

        try {
            VDI srcVolume = xenServer610Resource.getVDIbyUuid(connection, srcVolumeUuid);

            if (destDetails != null && Boolean.parseBoolean(destDetails.get(DiskTO.MANAGED))) {
                String iScsiName = destDetails.get(DiskTO.IQN);
                String storageHost = destDetails.get(DiskTO.STORAGE_HOST);
                String chapInitiatorUsername = destDetails.get(DiskTO.CHAP_INITIATOR_USERNAME);
                String chapInitiatorSecret = destDetails.get(DiskTO.CHAP_INITIATOR_SECRET);

                destPool = xenServer610Resource.getIscsiSR(connection, iScsiName, storageHost, iScsiName,
                        chapInitiatorUsername, chapInitiatorSecret, false);
            }
            else {
                final StorageFilerTO pool = command.getPool();
                destPool = xenServer610Resource.getStorageRepository(connection, CitrixHelper.getSRNameLabel(pool.getUuid(), pool.getType(), pool.getPath()));
            }

            Map<String, String> other = new HashMap<>();

            other.put("live", "true");

            // Live migrate the VDI.
            Task task = srcVolume.poolMigrateAsync(connection, destPool, other);

            long timeout = xenServer610Resource.getMigrateWait() * 1000L;

            xenServer610Resource.waitForTask(connection, task, 1000, timeout);
            xenServer610Resource.checkForSuccess(connection, task);

            VDI destVdi = Types.toVDI(task, connection);

            return new MigrateVolumeAnswer(command, true, null, destVdi.getUuid(connection));
        } catch (Exception ex) {
            if (destDetails != null && Boolean.parseBoolean(destDetails.get(DiskTO.MANAGED)) && destPool != null) {
                xenServer610Resource.removeSR(connection, destPool);
            }

            String msg = "Caught exception " + ex.getClass().getName() + " due to the following: " + ex.toString();

            LOGGER.error(msg, ex);

            return new MigrateVolumeAnswer(command, false, msg, null);
        }
    }
}
