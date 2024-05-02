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

import java.util.HashMap;
import java.util.Map;

import org.apache.cloudstack.storage.datastore.client.ScaleIOGatewayClient;
import org.apache.cloudstack.storage.datastore.util.ScaleIOUtil;
import org.apache.log4j.Logger;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.PrepareStorageClientAnswer;
import com.cloud.agent.api.PrepareStorageClientCommand;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;

@ResourceWrapper(handles = PrepareStorageClientCommand.class)
public class LibvirtPrepareStorageClientCommandWrapper extends CommandWrapper<PrepareStorageClientCommand, Answer, LibvirtComputingResource> {

    private static final Logger s_logger = Logger.getLogger(LibvirtPrepareStorageClientCommandWrapper.class);

    @Override
    public Answer execute(PrepareStorageClientCommand cmd, LibvirtComputingResource serverResource) {
        if (!ScaleIOUtil.isSDCServiceInstalled()) {
            s_logger.debug("SDC service not installed on host, preparing the SDC client not possible");
            return new PrepareStorageClientAnswer(cmd, false, "SDC service not installed on host");
        }

        if (!ScaleIOUtil.isSDCServiceEnabled()) {
            s_logger.debug("SDC service not enabled on host, enabling it");
            if (!ScaleIOUtil.enableSDCService()) {
                return new PrepareStorageClientAnswer(cmd, false, "SDC service not enabled on host");
            }
        }

        if (!ScaleIOUtil.isSDCServiceActive()) {
            if (!ScaleIOUtil.startSDCService()) {
                return new PrepareStorageClientAnswer(cmd, false, "Couldn't start SDC service on host");
            }
        } else if (!ScaleIOUtil.restartSDCService()) {
            return new PrepareStorageClientAnswer(cmd, false, "Couldn't restart SDC service on host");
        }

        return new PrepareStorageClientAnswer(cmd, true, getSDCDetails(cmd.getDetails()));
    }

    private Map<String, String> getSDCDetails(Map<String, String> details) {
        Map<String, String> sdcDetails = new HashMap<String, String>();
        if (details == null || !details.containsKey(ScaleIOGatewayClient.STORAGE_POOL_SYSTEM_ID))  {
            return sdcDetails;
        }

        String storageSystemId = details.get(ScaleIOGatewayClient.STORAGE_POOL_SYSTEM_ID);
        String sdcId = ScaleIOUtil.getSdcId(storageSystemId);
        if (sdcId != null) {
            sdcDetails.put(ScaleIOGatewayClient.SDC_ID, sdcId);
        } else {
            String sdcGuId = ScaleIOUtil.getSdcGuid();
            if (sdcGuId != null) {
                sdcDetails.put(ScaleIOGatewayClient.SDC_GUID, sdcGuId);
            }
        }
        return sdcDetails;
    }
}
