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

package com.cloud.hypervisor.kvm.resource.wrapper;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.cloud.agent.api.Answer;
import org.apache.cloudstack.storage.command.ClvmLockTransferCommand;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.utils.script.Script;

@ResourceWrapper(handles = ClvmLockTransferCommand.class)
public class LibvirtClvmLockTransferCommandWrapper
        extends CommandWrapper<ClvmLockTransferCommand, Answer, LibvirtComputingResource> {

    protected Logger logger = LogManager.getLogger(getClass());

    @Override
    public Answer execute(ClvmLockTransferCommand cmd, LibvirtComputingResource serverResource) {
        String lvPath = cmd.getLvPath();
        ClvmLockTransferCommand.Operation operation = cmd.getOperation();
        String volumeUuid = cmd.getVolumeUuid();

        logger.info(String.format("Executing CLVM lock transfer: operation=%s, lv=%s, volume=%s",
                operation, lvPath, volumeUuid));

        try {
            String lvchangeOpt;
            String operationDesc;
            switch (operation) {
                case DEACTIVATE:
                    lvchangeOpt = "-an";
                    operationDesc = "deactivated";
                    break;
                case ACTIVATE_EXCLUSIVE:
                    lvchangeOpt = "-aey";
                    operationDesc = "activated exclusively";
                    break;
                case ACTIVATE_SHARED:
                    lvchangeOpt = "-asy";
                    operationDesc = "activated in shared mode";
                    break;
                default:
                    return new Answer(cmd, false, "Unknown operation: " + operation);
            }

            Script script = new Script("/usr/sbin/lvchange", 30000, logger);
            script.add(lvchangeOpt);
            script.add(lvPath);

            String result = script.execute();

            if (result != null) {
                logger.error("CLVM lock transfer failed for volume {}: {}}",
                        volumeUuid, result);
                return new Answer(cmd, false,
                    String.format("lvchange %s %s failed: %s", lvchangeOpt, lvPath, result));
            }

            logger.info("Successfully executed CLVM lock transfer: {} {}} for volume {}}",
                    lvchangeOpt, lvPath, volumeUuid);

            return new Answer(cmd, true,
                String.format("Successfully %s CLVM volume %s", operationDesc, volumeUuid));

        } catch (Exception e) {
            logger.error("Exception during CLVM lock transfer for volume {}: {}}",
                    volumeUuid, e.getMessage(), e);
            return new Answer(cmd, false, "Exception: " + e.getMessage());
        }
    }
}
