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

import com.cloud.agent.api.Answer;
import org.apache.cloudstack.storage.clvm.command.ClvmLockTransferCommand;
import org.apache.cloudstack.storage.clvm.command.ClvmLockTransferAnswer;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.utils.script.Script;
import com.cloud.utils.script.OutputInterpreter;

@ResourceWrapper(handles = ClvmLockTransferCommand.class)
public class LibvirtClvmLockTransferCommandWrapper
        extends CommandWrapper<ClvmLockTransferCommand, Answer, LibvirtComputingResource> {

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

            Script script = new Script("/usr/sbin/lvchange", 60000, logger);
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
     * Query whether this host currently has the CLVM LV activated locally.
     * Executes: lvs -o lv_attr,lv_host,lv_active --noheadings <lvPath>
     *
     * lv_attr[4]=='a' (isActive) is LOCAL and is the authoritative signal — true only on
     * the host where the LV is currently activated. The management server fans out this
     * query to all cluster hosts; the one returning isActive=true is the lock holder.
     * lv_attr[5]=='o' (isOpen) means a VM has the device open on this host (doing I/O).
     * lv_host is retained for diagnostic logging only — do NOT use it to identify the
     * lock holder.
     */
    private Answer handleQueryLockState(ClvmLockTransferCommand cmd, String lvPath, String volumeUuid) {
        try {
            Script script = new Script("/usr/sbin/lvs", 30000, logger);
            script.add("-o");
            script.add("lv_attr,lv_host");
            script.add("--noheadings");
            script.add(lvPath);

            OutputInterpreter.AllLinesParser parser = new OutputInterpreter.AllLinesParser();
            String result = script.execute(parser);

            if (result != null) {
                logger.error("Failed to query lock state for volume {}: {}", volumeUuid, result);
                return new ClvmLockTransferAnswer(cmd, false,
                    String.format("lvs command failed: %s", result));
            }

            String[] lines = parser.getLines().split("\n");
            String dataLine = null;

            for (String line : lines) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty() &&
                    trimmed.length() >= 10 &&
                    "-wrsvmpco".indexOf(trimmed.charAt(0)) >= 0) {
                    dataLine = trimmed;
                    break;
                }
            }

            if (dataLine == null) {
                String allOutput = parser.getLines();
                logger.warn("Could not find lv_attr data line in lvs output for volume {}: {}",
                          volumeUuid, allOutput);
                return new ClvmLockTransferAnswer(cmd, false,
                    String.format("Could not parse lvs output. Full output: %s", allOutput));
            }

            logger.debug("Parsed lv_attr data line for volume {}: {}", volumeUuid, dataLine);

            String[] parts = dataLine.split("\\s+");
            if (parts.length < 1) {
                return new ClvmLockTransferAnswer(cmd, false, "Invalid lvs output format");
            }

            String lvAttr = parts[0];
            // lv_host: for diagnostics only, unreliable for lock-holder identification
            String hostname = parts.length > 1 ? parts[1] : null;

            // lv_attr[4]=='a' → LV is active on THIS host (local activation state)
            boolean isActive = lvAttr.length() > 4 && lvAttr.charAt(4) == 'a';
            // lv_attr[5]=='o' → a process has the device file open on this host (VM doing I/O)
            boolean isOpen   = lvAttr.length() > 5 && lvAttr.charAt(5) == 'o';

            logger.info("Queried lock state for volume {}: attr={}, hostname={}, active={}, open={}",
                    volumeUuid, lvAttr, hostname, isActive, isOpen);

            return new ClvmLockTransferAnswer(cmd, true,
                    String.format("Lock state: active=%s, open=%s, host=%s",
                            isActive, isOpen, hostname != null ? hostname : "none"),
                    hostname, isActive, isOpen, lvAttr);

        } catch (Exception e) {
            logger.error("Exception during lock state query for volume {}: {}",
                    volumeUuid, e.getMessage(), e);
            return new ClvmLockTransferAnswer(cmd, false, "Exception: " + e.getMessage());
        }
    }

}
