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
import java.util.Map;

import org.apache.log4j.Logger;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.ModifyStoragePoolAnswer;
import com.cloud.agent.api.ModifyStoragePoolCommand;
import com.cloud.agent.api.to.StorageFilerTO;
import com.cloud.hypervisor.xenserver.resource.CitrixHelper;
import com.cloud.hypervisor.xenserver.resource.CitrixResourceBase;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.storage.template.TemplateProp;
import com.cloud.utils.exception.CloudRuntimeException;
import com.xensource.xenapi.Connection;
import com.xensource.xenapi.SR;
import com.xensource.xenapi.Types.XenAPIException;

@ResourceWrapper(handles =  ModifyStoragePoolCommand.class)
public final class CitrixModifyStoragePoolCommandWrapper extends CommandWrapper<ModifyStoragePoolCommand, Answer, CitrixResourceBase> {

    private static final Logger s_logger = Logger.getLogger(CitrixModifyStoragePoolCommandWrapper.class);

    @Override
    public Answer execute(final ModifyStoragePoolCommand command, final CitrixResourceBase citrixResourceBase) {
        final Connection conn = citrixResourceBase.getConnection();
        final StorageFilerTO pool = command.getPool();
        final boolean add = command.getAdd();
        if (add) {
            try {
                String srName = command.getStoragePath();
                if (srName == null) {
                    srName = CitrixHelper.getSRNameLabel(pool.getUuid(), pool.getType(), pool.getPath());
                }
                final SR sr = citrixResourceBase.getStorageRepository(conn, srName);
                citrixResourceBase.setupHeartbeatSr(conn, sr, false);
                final long capacity = sr.getPhysicalSize(conn);
                final long available = capacity - sr.getPhysicalUtilisation(conn);
                if (capacity == -1) {
                    final String msg = "Pool capacity is -1! pool: " + pool.getHost() + pool.getPath();
                    s_logger.warn(msg);
                    return new Answer(command, false, msg);
                }
                final Map<String, TemplateProp> tInfo = new HashMap<String, TemplateProp>();
                final ModifyStoragePoolAnswer answer = new ModifyStoragePoolAnswer(command, capacity, available, tInfo);
                return answer;
            } catch (final XenAPIException e) {
                final String msg = "ModifyStoragePoolCommand add XenAPIException:" + e.toString() + " host:" + citrixResourceBase.getHost().getUuid() + " pool: " + pool.getHost()
                        + pool.getPath();
                s_logger.warn(msg, e);
                return new Answer(command, false, msg);
            } catch (final Exception e) {
                final String msg = "ModifyStoragePoolCommand add XenAPIException:" + e.getMessage() + " host:" + citrixResourceBase.getHost().getUuid() + " pool: "
                        + pool.getHost() + pool.getPath();
                s_logger.warn(msg, e);
                return new Answer(command, false, msg);
            }
        } else {
            try {
                final SR sr = citrixResourceBase.getStorageRepository(conn,
                        CitrixHelper.getSRNameLabel(pool.getUuid(), pool.getType(), pool.getPath()));
                final String srUuid = sr.getUuid(conn);
                final String result = citrixResourceBase.callHostPluginPremium(conn, "setup_heartbeat_file", "host", citrixResourceBase.getHost().getUuid(), "sr", srUuid, "add",
                        "false");
                if (result == null || !result.split("#")[1].equals("0")) {
                    throw new CloudRuntimeException("Unable to remove heartbeat file entry for SR " + srUuid + " due to " + result);
                }
                return new Answer(command, true, "success");
            } catch (final XenAPIException e) {
                final String msg = "ModifyStoragePoolCommand remove XenAPIException:" + e.toString() + " host:" + citrixResourceBase.getHost().getUuid() + " pool: "
                        + pool.getHost() + pool.getPath();
                s_logger.warn(msg, e);
                return new Answer(command, false, msg);
            } catch (final Exception e) {
                final String msg = "ModifyStoragePoolCommand remove XenAPIException:" + e.getMessage() + " host:" + citrixResourceBase.getHost().getUuid() + " pool: "
                        + pool.getHost() + pool.getPath();
                s_logger.warn(msg, e);
                return new Answer(command, false, msg);
            }
        }
    }
}
