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

import org.apache.cloudstack.diagnostics.CopyToSecondaryStorageCommand;
import org.apache.log4j.Logger;

import com.cloud.agent.api.Answer;
import com.cloud.hypervisor.xenserver.resource.CitrixResourceBase;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.xensource.xenapi.Connection;


@ResourceWrapper(handles = CopyToSecondaryStorageCommand.class)
public class CitrixCoppyToSecondaryStorageCommandWrapper extends CommandWrapper<CopyToSecondaryStorageCommand, Answer, CitrixResourceBase> {
    public static final Logger LOGGER = Logger.getLogger(CitrixCoppyToSecondaryStorageCommandWrapper.class);

    @Override
    public Answer execute(CopyToSecondaryStorageCommand cmd, CitrixResourceBase citrixResourceBase) {
        final Connection conn = citrixResourceBase.getConnection();
        String msg = String.format("Copying diagnostics zip file %s from system vm %s to secondary storage %s", cmd.getFileName(), cmd.getSystemVmIp(), cmd.getSecondaryStorageUrl());
        LOGGER.debug(msg);
        // Allow the hypervisor host to copy file from system VM to mounted secondary storage
        return citrixResourceBase.copyDiagnosticsFileToSecondaryStorage(conn, cmd);
    }
}
