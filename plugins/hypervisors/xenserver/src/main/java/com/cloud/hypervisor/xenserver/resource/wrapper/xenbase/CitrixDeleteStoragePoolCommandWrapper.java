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
import com.cloud.agent.api.DeleteStoragePoolCommand;
import com.cloud.agent.api.to.StorageFilerTO;
import com.cloud.hypervisor.xenserver.resource.CitrixHelper;
import com.cloud.hypervisor.xenserver.resource.CitrixResourceBase;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.xensource.xenapi.Connection;
import com.xensource.xenapi.SR;

@ResourceWrapper(handles =  DeleteStoragePoolCommand.class)
public final class CitrixDeleteStoragePoolCommandWrapper extends CommandWrapper<DeleteStoragePoolCommand, Answer, CitrixResourceBase> {
    private static final Logger s_logger = Logger.getLogger(CitrixDeleteStoragePoolCommandWrapper.class);

    @Override
    public Answer execute(final DeleteStoragePoolCommand command, final CitrixResourceBase citrixResourceBase) {
        final Connection conn = citrixResourceBase.getConnection();
        final StorageFilerTO poolTO = command.getPool();

        try {
            final SR sr;

            // getRemoveDatastore being true indicates we are using managed storage and need to pull the SR name out of a Map
            // instead of pulling it out using getUuid of the StorageFilerTO instance.

            String srNameLabel;
            if (command.getRemoveDatastore()) {
                Map<String, String> details = command.getDetails();
                srNameLabel = details.get(DeleteStoragePoolCommand.DATASTORE_NAME);
            }
            else {
                srNameLabel = CitrixHelper.getSRNameLabel(poolTO.getUuid(), poolTO.getType(), poolTO.getPath());
            }
            sr = citrixResourceBase.getStorageRepository(conn, srNameLabel);

            citrixResourceBase.removeSR(conn, sr);

            final Answer answer = new Answer(command, true, "success");

            return answer;
        } catch (final Exception e) {
            final String msg = "DeleteStoragePoolCommand XenAPIException:" + e.getMessage() + " host:" + citrixResourceBase.getHost().getUuid() +
                    " pool: " + poolTO.getHost() + poolTO.getPath();

            s_logger.error(msg, e);

            return new Answer(command, false, msg);
        }
    }
}
