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
package com.cloud.hypervisor.kvm.resource.rolling.maintenance;

import com.cloud.utils.Pair;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.OutputInterpreter;
import com.cloud.utils.script.Script;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.Duration;

import java.io.File;

public class RollingMaintenanceAgentExecutor extends RollingMaintenanceExecutorBase implements RollingMaintenanceExecutor {


    private String output;
    private boolean success;

    public RollingMaintenanceAgentExecutor(String hooksDir) {
        super(hooksDir);
    }

    @Override
    public Pair<Boolean, String> startStageExecution(String stage, File scriptFile, int timeout, String payload) {
        checkHooksDirectory();
        Duration duration = Duration.standardSeconds(timeout);
        final Script script = new Script(scriptFile.getAbsolutePath(), duration, logger);
        final OutputInterpreter.AllLinesParser parser = new OutputInterpreter.AllLinesParser();
        if (StringUtils.isNotEmpty(payload)) {
            script.add(payload);
        }
        logger.info("Executing stage: " + stage + " script: " + script);
        output = script.execute(parser) + " " + parser.getLines();

        if (script.isTimeout()) {
            String msg = "Script " + scriptFile + " timed out";
            logger.error(msg);
            success = false;
            return new Pair<>(false, msg);
        }

        int exitValue = script.getExitValue();
        if (exitValue == exitValueTerminatedSignal) {
            throw new CloudRuntimeException("Script " + scriptFile + " terminated");
        }
        success = exitValue == 0 || exitValue == exitValueAvoidMaintenance;
        setAvoidMaintenance(exitValue == exitValueAvoidMaintenance);
        logger.info("Execution finished for stage: " + stage + " script: " + script + ": " + exitValue);
        if (logger.isDebugEnabled()) {
            logger.debug(output);
            logger.debug("Stage " + stage + " execution finished: " + exitValue);
        }
        return new Pair<>(true, "Stage " + stage + " finished");
    }

    @Override
    public String getStageExecutionOutput(String stage, File scriptFile) {
        return output;
    }

    @Override
    public boolean isStageRunning(String stage, File scriptFile, String payload) {
        // In case of reconnection, it is assumed that the stage is finished
        return false;
    }

    @Override
    public boolean getStageExecutionSuccess(String stage, File scriptFile) {
        return success;
    }
}
