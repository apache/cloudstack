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

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CleanForMigrationStorageCommand;
import com.cloud.agent.api.CleanForMigrationStorageCommandAnswer;
import com.cloud.hypervisor.xenserver.resource.CitrixResourceBase;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.xensource.xenapi.Connection;
import com.xensource.xenapi.Types;
import com.xensource.xenapi.VM;
import org.apache.log4j.Logger;
import org.apache.xmlrpc.XmlRpcException;

@ResourceWrapper(handles = CleanForMigrationStorageCommand.class)
public final class CitrixCleanForMigrationStorageCommandWrapper extends CommandWrapper<CleanForMigrationStorageCommand, Answer, CitrixResourceBase> {

    private static final Logger s_logger = Logger.getLogger(CitrixCleanForMigrationStorageCommandWrapper.class);

    @Override
    public Answer execute(final CleanForMigrationStorageCommand command, final CitrixResourceBase citrixResourceBase) {
        final Connection conn = citrixResourceBase.getConnection();
        try {
            VM vm = citrixResourceBase.getVM(conn, command.getVmSpec().getName());
            vm.destroy(conn);
        } catch (Types.XenAPIException e) {
            s_logger.warn("Catch Exception " + e.getClass().getName() + " clean for migration storage failed due to " + e.toString(), e);
            return new CleanForMigrationStorageCommandAnswer(command, e);
        } catch (XmlRpcException e) {
            s_logger.warn("Catch Exception " + e.getClass().getName() + " clean for migration storage failed due to " + e.toString(), e);
            return new CleanForMigrationStorageCommandAnswer(command, e);
        }

        return new CleanForMigrationStorageCommandAnswer(command);
    }
}