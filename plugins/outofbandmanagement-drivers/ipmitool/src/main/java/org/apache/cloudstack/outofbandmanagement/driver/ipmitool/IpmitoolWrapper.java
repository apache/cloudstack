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

package org.apache.cloudstack.outofbandmanagement.driver.ipmitool;

import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.commons.lang3.StringUtils;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.apache.cloudstack.outofbandmanagement.OutOfBandManagement;
import org.apache.cloudstack.outofbandmanagement.driver.OutOfBandManagementDriverResponse;
import org.apache.cloudstack.utils.process.ProcessResult;
import org.apache.cloudstack.utils.process.ProcessRunner;
import org.apache.log4j.Logger;
import org.joda.time.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

public final class IpmitoolWrapper {
    public static final Logger LOG = Logger.getLogger(IpmitoolWrapper.class);

    private final ProcessRunner RUNNER;

    public IpmitoolWrapper(ExecutorService executor) {
        this.RUNNER = new ProcessRunner(executor);
    }

    public String parsePowerCommand(OutOfBandManagement.PowerOperation operation) {
        if (operation == null) {
            throw new IllegalStateException("Invalid power operation requested");
        }
        switch (operation) {
            case ON:
            case OFF:
            case CYCLE:
            case RESET:
            case SOFT:
            case STATUS:
                break;
            default:
                throw new IllegalStateException("Invalid power operation requested");
        }
        return operation.toString().toLowerCase();
    }

    public OutOfBandManagement.PowerState parsePowerState(final String standardOutput) {
        if (StringUtils.isEmpty(standardOutput)) {
            return OutOfBandManagement.PowerState.Unknown;
        }
        if (standardOutput.equals("Chassis Power is on")) {
            return OutOfBandManagement.PowerState.On;
        } else if (standardOutput.equals("Chassis Power is off")) {
            return OutOfBandManagement.PowerState.Off;
        }
        return OutOfBandManagement.PowerState.Unknown;
    }

    public List<String> getIpmiToolCommandArgs(final String ipmiToolPath, final String ipmiInterface, final String retries,
                                                      final ImmutableMap<OutOfBandManagement.Option, String> options, String... commands) {

        final ImmutableList.Builder<String> ipmiToolCommands = ImmutableList.<String>builder()
                                                            .add(ipmiToolPath)
                                                            .add("-I")
                                                            .add(ipmiInterface)
                                                            .add("-R")
                                                            .add(retries)
                                                            .add("-v");

        if (options != null) {
            for (ImmutableMap.Entry<OutOfBandManagement.Option, String> option : options.entrySet()) {
                switch (option.getKey()) {
                    case ADDRESS:
                        ipmiToolCommands.add("-H");
                        break;
                    case PORT:
                        ipmiToolCommands.add("-p");
                        break;
                    case USERNAME:
                        ipmiToolCommands.add("-U");
                        break;
                    case PASSWORD:
                        ipmiToolCommands.add("-P");
                        break;
                    default:
                        continue;
                }
                ipmiToolCommands.add(option.getValue());
            }
        }
        for (String command : commands) {
            ipmiToolCommands.add(command);
        }
        return ipmiToolCommands.build();
    }

    public String findIpmiUser(final String usersList, final String username) {
        /**
         * Expected usersList string contains legends on first line and users on rest
         * ID Name  Callin Link Auth IPMI Msg Channel Priv Limit
         * 1  admin true   true true ADMINISTRATOR
         */

        // Assuming user 'ID' index on 1st position
        int idIndex = 0;

        // Assuming  user 'Name' index on 2nd position
        int usernameIndex = 1;

        final String[] lines = usersList.split("\\r?\\n");
        if (lines.length < 2) {
            throw new CloudRuntimeException("Error parsing user ID from ipmi user listing");
        }
        // Find user and name indexes from the 1st line if not on default position
        final String[] legends = lines[0].split(" +");
        for (int idx = 0; idx < legends.length; idx++) {
            if (legends[idx].equals("ID")) {
                idIndex = idx;
            }
            if (legends[idx].equals("Name")) {
                usernameIndex = idx;
            }
        }
        // Find user 'ID' based on provided username and ID/Name positions
        String userId = null;
        for (int idx = 1; idx < lines.length; idx++) {
            final String[] words = lines[idx].split(" +");
            if (usernameIndex < words.length && idIndex < words.length) {
                if (words[usernameIndex].equals(username)) {
                    userId = words[idIndex];
                }
            }
        }
        return userId;
    }

    public OutOfBandManagementDriverResponse executeCommands(final List<String> commands) {
        return executeCommands(commands, ProcessRunner.DEFAULT_MAX_TIMEOUT);
    }

    public OutOfBandManagementDriverResponse executeCommands(final List<String> commands, final Duration timeOut) {
        final ProcessResult result = RUNNER.executeCommands(commands, timeOut);
        if (LOG.isTraceEnabled()) {
            List<String> cleanedCommands = new ArrayList<String>();
            int maskNextCommand = 0;
            for (String command : commands) {
                if (maskNextCommand > 0) {
                    cleanedCommands.add("**** ");
                    maskNextCommand--;
                    continue;
                }
                if (command.equalsIgnoreCase("-P")) {
                    maskNextCommand = 1;
                } else if (command.toLowerCase().endsWith("password")) {
                    maskNextCommand = 2;
                }
                cleanedCommands.add(command);
            }
            LOG.trace("Executed ipmitool process with commands: " + StringUtils.join(cleanedCommands, ", ") +
                      "\nIpmitool execution standard output: " + result.getStdOutput() +
                      "\nIpmitool execution error output: " + result.getStdError());
        }
        return new OutOfBandManagementDriverResponse(result.getStdOutput(), result.getStdError(), result.isSuccess());
    }
}
