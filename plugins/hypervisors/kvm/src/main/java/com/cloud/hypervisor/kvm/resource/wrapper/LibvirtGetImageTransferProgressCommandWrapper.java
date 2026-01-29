//Licensed to the Apache Software Foundation (ASF) under one
//or more contributor license agreements.  See the NOTICE file
//distributed with this work for additional information
//regarding copyright ownership.  The ASF licenses this file
//to you under the Apache License, Version 2.0 (the
//"License"); you may not use this file except in compliance
//the License.  You may obtain a copy of the License at
//
//http://www.apache.org/licenses/LICENSE-2.0
//
//Unless required by applicable law or agreed to in writing,
//software distributed under the License is distributed on an
//"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
//KIND, either express or implied.  See the License for the
//specific language governing permissions and limitations
//under the License.

package com.cloud.hypervisor.kvm.resource.wrapper;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.backup.GetImageTransferProgressAnswer;
import org.apache.cloudstack.backup.GetImageTransferProgressCommand;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cloud.agent.api.Answer;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.storage.KVMPhysicalDisk;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;

@ResourceWrapper(handles = GetImageTransferProgressCommand.class)
public class LibvirtGetImageTransferProgressCommandWrapper extends CommandWrapper<GetImageTransferProgressCommand, Answer, LibvirtComputingResource> {
    protected Logger logger = LogManager.getLogger(getClass());

    @Override
    public Answer execute(GetImageTransferProgressCommand cmd, LibvirtComputingResource resource) {
        try {
            List<String> transferIds = cmd.getTransferIds();
            Map<String, String> volumePaths = cmd.getVolumePaths();
            Map<String, Long> volumeSizes = cmd.getVolumeSizes();
            Map<String, Long> progressMap = new HashMap<>();

            if (transferIds == null || transferIds.isEmpty()) {
                return new GetImageTransferProgressAnswer(cmd, true, "No transfers to check", progressMap);
            }

            for (String transferId : transferIds) {
                String volumePath = volumePaths.get(transferId);
                Long volumeSize = volumeSizes.get(transferId);

                if (volumePath == null || volumeSize == null || volumeSize == 0) {
                    logger.warn("Missing volume path or size for transferId: {}", transferId);
                    progressMap.put(transferId, null);
                    continue;
                }

                try {
                    File file = new File(volumePath);
                    if (!file.exists()) {
                        logger.warn("Volume file does not exist: {}", volumePath);
                        progressMap.put(transferId, null);
                        continue;
                    }

                    long currentSize = file.length();

                    if (volumePath.endsWith(".qcow2") || volumePath.endsWith(".qcow")) {
                        try {
                            currentSize = KVMPhysicalDisk.getVirtualSizeFromFile(volumePath);
                        } catch (Exception e) {
                            logger.warn("Failed to get virtual size for qcow2 file: {}, using physical size", volumePath, e);
                        }
                    }
                    progressMap.put(transferId, currentSize);
                    logger.debug("Transfer {} progress, current: {})", transferId, currentSize, volumeSize);

                } catch (Exception e) {
                    logger.error("Error getting progress for transferId: {}, path: {}", transferId, volumePath, e);
                    progressMap.put(transferId, null);
                }
            }

            return new GetImageTransferProgressAnswer(cmd, true, "Progress retrieved successfully", progressMap);

        } catch (Exception e) {
            logger.error("Error executing GetImageTransferProgressCommand", e);
            return new GetImageTransferProgressAnswer(cmd, false, "Error getting transfer progress: " + e.getMessage());
        }
    }
}
