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

import com.cloud.agent.api.RollingMaintenanceAnswer;
import com.cloud.agent.api.RollingMaintenanceCommand;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.resource.rolling.maintenance.RollingMaintenanceAgentExecutor;
import com.cloud.hypervisor.kvm.resource.rolling.maintenance.RollingMaintenanceExecutor;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.resource.RollingMaintenanceManager;
import com.cloud.utils.exception.CloudRuntimeException;

import java.io.File;

@ResourceWrapper(handles =  RollingMaintenanceCommand.class)
public class LibvirtRollingMaintenanceCommandWrapper extends CommandWrapper<RollingMaintenanceCommand, RollingMaintenanceAnswer, LibvirtComputingResource> {


    @Override
    public RollingMaintenanceAnswer execute(RollingMaintenanceCommand command, LibvirtComputingResource resource) {
        RollingMaintenanceExecutor executor = resource.getRollingMaintenanceExecutor();
        String stage = command.isCheckMaintenanceScript() ? RollingMaintenanceManager.Stage.Maintenance.toString() : command.getStage();
        int timeout = command.getWait();
        String payload = command.getPayload();

        try {
            File scriptFile = executor.getStageScriptFile(stage);
            if (command.isCheckMaintenanceScript()) {
                return new RollingMaintenanceAnswer(command, scriptFile != null);
            } else if (scriptFile == null) {
                logger.info("No script file defined for stage " + stage + ". Skipping stage...");
                return new RollingMaintenanceAnswer(command, true, "Skipped stage " + stage, true);
            }

            if (command.isStarted() && executor instanceof RollingMaintenanceAgentExecutor) {
                String msg = "Stage has been started previously and the agent restarted, setting stage as finished";
                logger.info(msg);
                return new RollingMaintenanceAnswer(command, true, msg, true);
            }
            logger.info("Processing stage " + stage);
            if (!command.isStarted()) {
                executor.startStageExecution(stage, scriptFile, timeout, payload);
            }
            if (executor.isStageRunning(stage, scriptFile, payload)) {
                return new RollingMaintenanceAnswer(command, true, "Stage " + stage + " still running", false);
            }
            boolean success = executor.getStageExecutionSuccess(stage, scriptFile);
            String output = executor.getStageExecutionOutput(stage, scriptFile);
            RollingMaintenanceAnswer answer = new RollingMaintenanceAnswer(command, success, output, true);
            if (executor.getStageAvoidMaintenance(stage, scriptFile)) {
                logger.info("Avoid maintenance flag added to the answer for the stage " + stage);
                answer.setAvoidMaintenance(true);
            }
            logger.info("Finished processing stage " + stage);
            return answer;
        } catch (CloudRuntimeException e) {
            return new RollingMaintenanceAnswer(command, false, e.getMessage(), false);
        }
    }
}
