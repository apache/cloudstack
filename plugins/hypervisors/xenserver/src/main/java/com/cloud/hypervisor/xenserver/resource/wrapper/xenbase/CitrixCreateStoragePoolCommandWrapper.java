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

import org.apache.log4j.Logger;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CreateStoragePoolCommand;
import com.cloud.agent.api.to.StorageFilerTO;
import com.cloud.hypervisor.xenserver.resource.CitrixResourceBase;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.storage.Storage.StoragePoolType;
import com.xensource.xenapi.Connection;

@ResourceWrapper(handles =  CreateStoragePoolCommand.class)
public final class CitrixCreateStoragePoolCommandWrapper extends CommandWrapper<CreateStoragePoolCommand, Answer, CitrixResourceBase> {

    private static final Logger s_logger = Logger.getLogger(CitrixCreateStoragePoolCommandWrapper.class);

    @Override
    public Answer execute(final CreateStoragePoolCommand command, final CitrixResourceBase citrixResourceBase) {
        final Connection conn = citrixResourceBase.getConnection();
        final StorageFilerTO pool = command.getPool();

        try {
            if (command.getCreateDatastore()) {
                Map<String, String> details = command.getDetails();

                String srNameLabel = details.get(CreateStoragePoolCommand.DATASTORE_NAME);
                String storageHost = details.get(CreateStoragePoolCommand.STORAGE_HOST);
                String iqn = details.get(CreateStoragePoolCommand.IQN);

                citrixResourceBase.getIscsiSR(conn, srNameLabel, storageHost, iqn, null, null, false);
            }
            else {
                if (pool.getType() == StoragePoolType.NetworkFilesystem) {
                    citrixResourceBase.getNfsSR(conn, Long.toString(pool.getId()), pool.getUuid(), pool.getHost(), pool.getPath(), pool.toString());
                } else if (pool.getType() == StoragePoolType.IscsiLUN) {
                    citrixResourceBase.getIscsiSR(conn, pool.getUuid(), pool.getHost(), pool.getPath(), null, null, false);
                } else if (pool.getType() == StoragePoolType.PreSetup) {
                } else {
                    return new Answer(command, false, "The pool type: " + pool.getType().name() + " is not supported.");
                }
            }

            return new Answer(command, true, "success");
        } catch (final Exception e) {
            final String msg = "Catch Exception " + e.getClass().getName() + ", create StoragePool failed due to " + e.toString() + " on host:"
                    + citrixResourceBase.getHost().getUuid() + " pool: " + pool.getHost() + pool.getPath();

            s_logger.warn(msg, e);

            return new Answer(command, false, msg);
        }
    }
}
