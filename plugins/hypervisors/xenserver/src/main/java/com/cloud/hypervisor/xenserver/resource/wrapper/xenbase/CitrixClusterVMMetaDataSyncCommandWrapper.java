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
import com.cloud.agent.api.ClusterVMMetaDataSyncAnswer;
import com.cloud.agent.api.ClusterVMMetaDataSyncCommand;
import com.cloud.hypervisor.xenserver.resource.CitrixResourceBase;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.xensource.xenapi.Connection;
import com.xensource.xenapi.Host;
import com.xensource.xenapi.Pool;

@ResourceWrapper(handles =  ClusterVMMetaDataSyncCommand.class)
public final class CitrixClusterVMMetaDataSyncCommandWrapper extends CommandWrapper<ClusterVMMetaDataSyncCommand, Answer, CitrixResourceBase> {

    private static final Logger s_logger = Logger.getLogger(CitrixClusterVMMetaDataSyncCommandWrapper.class);

    @Override
    public Answer execute(final ClusterVMMetaDataSyncCommand command, final CitrixResourceBase citrixResourceBase) {
        final Connection conn = citrixResourceBase.getConnection();
        //check if this is master
        try {
            final Pool pool = Pool.getByUuid(conn, citrixResourceBase.getHost().getPool());
            final Pool.Record poolr = pool.getRecord(conn);
            final Host.Record hostr = poolr.master.getRecord(conn);
            if (!citrixResourceBase.getHost().getUuid().equals(hostr.uuid)) {
                return new ClusterVMMetaDataSyncAnswer(command.getClusterId(), null);
            }
        } catch (final Throwable e) {
            s_logger.warn("Check for master failed, failing the Cluster sync VMMetaData command");
            return new ClusterVMMetaDataSyncAnswer(command.getClusterId(), null);
        }
        final HashMap<String, String> vmMetadatum = citrixResourceBase.clusterVMMetaDataSync(conn);
        return new ClusterVMMetaDataSyncAnswer(command.getClusterId(), vmMetadatum);
    }
}
