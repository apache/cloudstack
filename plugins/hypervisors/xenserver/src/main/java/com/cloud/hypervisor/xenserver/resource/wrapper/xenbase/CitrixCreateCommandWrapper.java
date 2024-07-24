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

import java.util.HashMap;

import org.apache.log4j.Logger;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.storage.CreateAnswer;
import com.cloud.agent.api.storage.CreateCommand;
import com.cloud.agent.api.to.StorageFilerTO;
import com.cloud.agent.api.to.VolumeTO;
import com.cloud.hypervisor.xenserver.resource.CitrixHelper;
import com.cloud.hypervisor.xenserver.resource.CitrixResourceBase;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.vm.DiskProfile;
import com.xensource.xenapi.Connection;
import com.xensource.xenapi.SR;
import com.xensource.xenapi.Types;
import com.xensource.xenapi.VDI;

@ResourceWrapper(handles =  CreateCommand.class)
public final class CitrixCreateCommandWrapper extends CommandWrapper<CreateCommand, Answer, CitrixResourceBase> {

    private static final Logger s_logger = Logger.getLogger(CitrixCreateCommandWrapper.class);

    @Override
    public Answer execute(final CreateCommand command, final CitrixResourceBase citrixResourceBase) {
        final Connection conn = citrixResourceBase.getConnection();
        final StorageFilerTO pool = command.getPool();
        final DiskProfile dskch = command.getDiskCharacteristics();

        VDI vdi = null;
        try {
            final SR poolSr = citrixResourceBase.getStorageRepository(conn,
                    CitrixHelper.getSRNameLabel(pool.getUuid(), pool.getType(), pool.getPath()));
            if (command.getTemplateUrl() != null) {
                VDI tmpltvdi = null;

                tmpltvdi = citrixResourceBase.getVDIbyUuid(conn, command.getTemplateUrl());
                vdi = tmpltvdi.createClone(conn, new HashMap<String, String>());
                vdi.setNameLabel(conn, dskch.getName());
            } else {
                final VDI.Record vdir = new VDI.Record();
                vdir.nameLabel = dskch.getName();
                vdir.SR = poolSr;
                vdir.type = Types.VdiType.USER;

                vdir.virtualSize = dskch.getSize();
                vdi = VDI.create(conn, vdir);
            }

            VDI.Record vdir;
            vdir = vdi.getRecord(conn);

            s_logger.debug("Successfully created VDI for " + command + ".  Uuid = " + vdir.uuid);

            final VolumeTO vol =
                    new VolumeTO(command.getVolumeId(), dskch.getType(), pool.getType(), pool.getUuid(), vdir.nameLabel, pool.getPath(), vdir.uuid, vdir.virtualSize, null);

            return new CreateAnswer(command, vol);
        } catch (final Exception e) {
            s_logger.warn("Unable to create volume; Pool=" + pool + "; Disk: " + dskch, e);
            return new CreateAnswer(command, e);
        }
    }
}
