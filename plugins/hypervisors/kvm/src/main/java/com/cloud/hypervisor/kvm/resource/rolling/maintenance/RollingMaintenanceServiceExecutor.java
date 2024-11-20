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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Stream;

public class RollingMaintenanceServiceExecutor extends RollingMaintenanceExecutorBase implements RollingMaintenanceExecutor {

    private static final String servicePrefix = "cloudstack-rolling-maintenance";
    private static final String resultsFileSuffix = "rolling-maintenance-results";
    private static final String outputFileSuffix = "rolling-maintenance-output";


    public RollingMaintenanceServiceExecutor(String hooksDir) {
        super(hooksDir);
    }

    /**
     * Generate and return escaped instance name to use on systemd service invokation
     */
    private String generateInstanceName(String stage, String file, String payload) {
        String instanceName = String.format("%s,%s,%s,%s,%s", stage, file, getTimeout(),
                getResultsFilePath(), getOutputFilePath());
        if (StringUtils.isNotBlank(payload)) {
            instanceName += "," + payload;
        }
        return Script.runSimpleBashScript(String.format("systemd-escape '%s'", instanceName));
    }

    private String invokeService(String action, String stage, String file, String payload) {
        logger.debug("Invoking rolling maintenance service for stage: " + stage + " and file " + file + " with action: " + action);
        final OutputInterpreter.AllLinesParser parser = new OutputInterpreter.AllLinesParser();
        Script command = new Script("/bin/systemctl", logger);
        command.add(action);
        String service = servicePrefix + "@" + generateInstanceName(stage, file, payload);
        command.add(service);
        String result = command.execute(parser);
        int exitValue = command.getExitValue();
        logger.trace("Execution: " + command.toString() + " - exit code: " + exitValue +
                ": " + result + (StringUtils.isNotBlank(parser.getLines()) ? parser.getLines() : ""));
        return StringUtils.isBlank(result) ? parser.getLines().replace("\n", " ") : result;
    }

    @Override
    public Pair<Boolean, String> startStageExecution(String stage, File scriptFile, int timeout, String payload) {
        checkHooksDirectory();
        setTimeout(timeout);
        String result = invokeService("start", stage, scriptFile.getAbsolutePath(), payload);
        if (StringUtils.isNotBlank(result)) {
            throw new CloudRuntimeException("Error starting stage: " + stage + " execution: " + result);
        }
        logger.trace("Stage " + stage + "execution started");
        return new Pair<>(true, "OK");
    }

    private String getResultsFilePath() {
        return getHooksDir() + resultsFileSuffix;
    }

    private String getOutputFilePath() {
        return getHooksDir() + outputFileSuffix;
    }

    private String readFromFile(String filePath) {
        StringBuilder contentBuilder = new StringBuilder();

        try (Stream<String> stream = Files.lines( Paths.get(filePath), StandardCharsets.UTF_8)) {
            stream.forEach(s -> contentBuilder.append(s).append("\n"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return contentBuilder.toString();
    }

    @Override
    public String getStageExecutionOutput(String stage, File scriptFile) {
        return readFromFile(getOutputFilePath());
    }

    @Override
    public boolean isStageRunning(String stage, File scriptFile, String payload) {
        String result = invokeService("is-active", stage, scriptFile.getAbsolutePath(), payload);
        if (StringUtils.isNotBlank(result) && result.equals("failed")) {
            String status = invokeService("status", stage, scriptFile.getAbsolutePath(), payload);
            String errorMsg = "Stage " + stage + " execution failed, status: " + status;
            logger.error(errorMsg);
            throw new CloudRuntimeException(errorMsg);
        }
        return StringUtils.isNotBlank(result) && result.equals("active");
    }

    @Override
    public boolean getStageExecutionSuccess(String stage, File scriptFile) {
        String fileContent = readFromFile(getResultsFilePath());
        if (StringUtils.isBlank(fileContent)) {
            throw new CloudRuntimeException("Empty content in file " + getResultsFilePath());
        }
        fileContent = fileContent.replace("\n", "");
        String[] parts = fileContent.split(",");
        if (parts.length < 3) {
            throw new CloudRuntimeException("Results file " + getResultsFilePath() + " unexpected content: " + fileContent);
        }
        if (!parts[0].equalsIgnoreCase(stage)) {
            throw new CloudRuntimeException("Expected stage " + stage + " results but got stage " + parts[0]);
        }
        setAvoidMaintenance(Boolean.parseBoolean(parts[2]));
        return Boolean.parseBoolean(parts[1]);
    }
}
