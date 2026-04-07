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
import org.apache.cloudstack.storage.command.ClvmLockTransferAnswer;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.utils.script.Script;
import com.cloud.utils.script.OutputInterpreter;

@ResourceWrapper(handles = ClvmLockTransferCommand.class)
public class LibvirtClvmLockTransferCommandWrapper
        extends CommandWrapper<ClvmLockTransferCommand, Answer, LibvirtComputingResource> {

    protected Logger logger = LogManager.getLogger(getClass());

    @Override
    public Answer execute(ClvmLockTransferCommand cmd, LibvirtComputingResource serverResource) {
        String lvPath = cmd.getLvPath();
        ClvmLockTransferCommand.Operation operation = cmd.getOperation();
        String volumeUuid = cmd.getVolumeUuid();

        logger.info("Executing CLVM lock transfer: operation={}, lv={}, volume={}",
                operation, lvPath, volumeUuid);

        try {

            if (operation == ClvmLockTransferCommand.Operation.QUERY_LOCK_STATE) {
                return handleQueryLockState(cmd, lvPath, volumeUuid);
            }

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
                    return new ClvmLockTransferAnswer(cmd, false, "Unknown operation: " + operation);
            }

            Script script = new Script("/usr/sbin/lvchange", 30000, logger);
            script.add(lvchangeOpt);
            script.add(lvPath);

            String result = script.execute();

            if (result != null) {
                logger.error("CLVM lock transfer failed for volume {}: {}",
                        volumeUuid, result);
                return new ClvmLockTransferAnswer(cmd, false,
                    String.format("lvchange %s %s failed: %s", lvchangeOpt, lvPath, result));
            }

            logger.info("Successfully executed CLVM lock transfer: {} {} for volume {}",
                    lvchangeOpt, lvPath, volumeUuid);

            return new ClvmLockTransferAnswer(cmd, true,
                String.format("Successfully %s CLVM volume %s", operationDesc, volumeUuid));

        } catch (Exception e) {
            logger.error("Exception during CLVM lock transfer for volume {}: {}",
                    volumeUuid, e.getMessage(), e);
            return new ClvmLockTransferAnswer(cmd, false, "Exception: " + e.getMessage());
        }
    }

    /**
     * Query which host currently holds the CLVM lock for a volume.
     * Executes: lvs -o lv_attr,lv_host --noheadings <lvPath>
     *
     * This queries the actual CLVM lock state (source of truth).
     * The lv_host attribute shows which host currently has the volume activated.
     *
     * @return ClvmLockTransferAnswer with lock holder hostname
     */
    private Answer handleQueryLockState(ClvmLockTransferCommand cmd, String lvPath, String volumeUuid) {
        try {
            Script script = new Script("/usr/sbin/lvs", 10000, logger);
            script.add("-o");
            script.add("lv_attr,lv_host");
            script.add("--noheadings");
            script.add(lvPath);

            OutputInterpreter.OneLineParser parser = new OutputInterpreter.OneLineParser();
            String result = script.execute(parser);

            if (result != null) {
                logger.error("Failed to query lock state for volume {}: {}", volumeUuid, result);
                return new ClvmLockTransferAnswer(cmd, false,
                    String.format("lvs command failed: %s", result));
            }

            // Parse output: "  -wi-a-e--- host5.example.com"
            String output = parser.getLine();
            if (output == null || output.trim().isEmpty()) {
                return new ClvmLockTransferAnswer(cmd, false, "No output from lvs command");
            }

            String[] parts = output.trim().split("\\s+");
            if (parts.length < 1) {
                return new ClvmLockTransferAnswer(cmd, false, "Invalid lvs output format");
            }

            String lvAttr = parts[0];
            String hostname = parts.length > 1 ? parts[1] : null;

            boolean isActive = lvAttr.length() > 4 && lvAttr.charAt(4) == 'a';
            boolean isExclusive = lvAttr.length() > 5 && lvAttr.charAt(5) == 'e';

            logger.info("Queried lock state for volume {}: attr={}, hostname={}, active={}, exclusive={}",
                    volumeUuid, lvAttr, hostname, isActive, isExclusive);

            return new ClvmLockTransferAnswer(cmd, true,
                    String.format("Lock state: active=%s, exclusive=%s, host=%s",
                            isActive, isExclusive, hostname != null ? hostname : "none"),
                    hostname, isActive, isExclusive, lvAttr);

        } catch (Exception e) {
            logger.error("Exception during lock state query for volume {}: {}",
                    volumeUuid, e.getMessage(), e);
            return new ClvmLockTransferAnswer(cmd, false, "Exception: " + e.getMessage());
        }
    }
}
