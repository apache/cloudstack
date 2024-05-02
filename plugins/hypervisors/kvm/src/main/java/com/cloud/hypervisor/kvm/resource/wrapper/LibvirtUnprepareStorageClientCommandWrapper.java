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

package com.cloud.hypervisor.kvm.resource.wrapper;

import org.apache.cloudstack.storage.datastore.util.ScaleIOUtil;
import org.apache.log4j.Logger;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.UnprepareStorageClientAnswer;
import com.cloud.agent.api.UnprepareStorageClientCommand;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;

@ResourceWrapper(handles = UnprepareStorageClientCommand.class)
public class LibvirtUnprepareStorageClientCommandWrapper extends CommandWrapper<UnprepareStorageClientCommand, Answer, LibvirtComputingResource> {

    private static final Logger s_logger = Logger.getLogger(LibvirtUnprepareStorageClientCommandWrapper.class);

    @Override
    public Answer execute(UnprepareStorageClientCommand cmd, LibvirtComputingResource serverResource) {
        if (!ScaleIOUtil.isSDCServiceInstalled()) {
            s_logger.debug("SDC service not installed on host, no need to unprepare the SDC client");
            return new UnprepareStorageClientAnswer(cmd, true);
        }

        if (!ScaleIOUtil.isSDCServiceEnabled()) {
            s_logger.debug("SDC service not enabled on host, no need to unprepare the SDC client");
            return new UnprepareStorageClientAnswer(cmd, true);
        }

        if (!ScaleIOUtil.stopSDCService()) {
            return new UnprepareStorageClientAnswer(cmd, false, "Couldn't stop SDC service on host");
        }

        return new UnprepareStorageClientAnswer(cmd, true);
    }
}
