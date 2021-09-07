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

import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.exception.CloudRuntimeException;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.outofbandmanagement.OutOfBandManagement;
import org.apache.cloudstack.outofbandmanagement.OutOfBandManagementDriver;
import org.apache.cloudstack.outofbandmanagement.OutOfBandManagementService;
import org.apache.cloudstack.outofbandmanagement.driver.OutOfBandManagementDriverChangePasswordCommand;
import org.apache.cloudstack.outofbandmanagement.driver.OutOfBandManagementDriverCommand;
import org.apache.cloudstack.outofbandmanagement.driver.OutOfBandManagementDriverPowerCommand;
import org.apache.cloudstack.outofbandmanagement.driver.OutOfBandManagementDriverResponse;
import org.apache.log4j.Logger;
import org.joda.time.Duration;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.commons.lang3.StringUtils;

public final class IpmitoolOutOfBandManagementDriver extends AdapterBase implements OutOfBandManagementDriver, Configurable {
    public static final Logger LOG = Logger.getLogger(IpmitoolOutOfBandManagementDriver.class);

    private static volatile boolean isDriverEnabled = false;
    private static boolean isIpmiToolBinAvailable = false;

    private final ExecutorService ipmitoolExecutor = Executors.newFixedThreadPool(OutOfBandManagementService.SyncThreadPoolSize.value(), new NamedThreadFactory("IpmiToolDriver"));
    private final IpmitoolWrapper IPMITOOL = new IpmitoolWrapper(ipmitoolExecutor);

    public final ConfigKey<String> IpmiToolPath = new ConfigKey<String>("Advanced", String.class, "outofbandmanagement.ipmitool.path", "/usr/bin/ipmitool",
            "The out of band management ipmitool path used by the IpmiTool driver. Default: /usr/bin/ipmitool.", true, ConfigKey.Scope.Global);

    public final ConfigKey<String> IpmiToolInterface = new ConfigKey<String>("Advanced", String.class, "outofbandmanagement.ipmitool.interface", "lanplus",
            "The out of band management IpmiTool driver interface to use. Default: lanplus. Valid values are: lan, lanplus, open etc.", true, ConfigKey.Scope.Global);

    public final ConfigKey<String> IpmiToolRetries = new ConfigKey<String>("Advanced", String.class, "outofbandmanagement.ipmitool.retries", "1",
            "The out of band management IpmiTool driver retries option -R. Default 1.", true, ConfigKey.Scope.Global);

    private String getIpmiUserId(ImmutableMap<OutOfBandManagement.Option, String> options, final Duration timeOut) {
        final String username = options.get(OutOfBandManagement.Option.USERNAME);
        if (Strings.isNullOrEmpty(username)) {
            throw new CloudRuntimeException("Empty IPMI user configured, cannot proceed to find user's ID.");
        }

        final List<String> ipmiToolCommands = IPMITOOL.getIpmiToolCommandArgs(IpmiToolPath.value(),
                IpmiToolInterface.value(),
                IpmiToolRetries.value(),
                options, "user", "list");
        final OutOfBandManagementDriverResponse output = IPMITOOL.executeCommands(ipmiToolCommands, timeOut);
        if (!output.isSuccess()) {
            String oneLineCommand = StringUtils.join(ipmiToolCommands, " ");
            String message = String.format("Failed to find IPMI user [%s] to change password. Command [%s], error [%s].", username, oneLineCommand, output.getError());
            LOG.debug(message);
            throw new CloudRuntimeException(message);
        }

        final String userId = IPMITOOL.findIpmiUser(output.getResult(), username);
        if (Strings.isNullOrEmpty(userId)) {
            String message = String.format("No IPMI user ID found for the username [%s].", username);
            LOG.debug(message);
            throw new CloudRuntimeException(message);
        }
        return userId;
    }

    public OutOfBandManagementDriverResponse execute(final OutOfBandManagementDriverCommand cmd) {
        if (!isIpmiToolBinAvailable) {
            initDriver();
            if (!isIpmiToolBinAvailable) {
                String message = "Aborting operation due to ipmitool binary not available for execution.";
                LOG.debug(message);
                return new OutOfBandManagementDriverResponse(null, message, false);
            }
        }

        OutOfBandManagementDriverResponse response = new OutOfBandManagementDriverResponse(null, "Unsupported Command", false);
        if (!isDriverEnabled) {
            String message = "Driver not enabled or shutdown.";
            LOG.debug(message);
            response.setError(message);
            return response;
        }
        if (cmd instanceof OutOfBandManagementDriverPowerCommand) {
            response = execute((OutOfBandManagementDriverPowerCommand) cmd);
        } else if (cmd instanceof OutOfBandManagementDriverChangePasswordCommand) {
            response = execute((OutOfBandManagementDriverChangePasswordCommand) cmd);
        }

        if (response != null && !response.isSuccess() && response.getError().contains("RAKP 2 HMAC is invalid")) {
            String message = String.format("Setting authFailure as 'true' due to [%s].", response.getError());
            LOG.debug(message);
            response.setAuthFailure(true);
        }
        return response;
    }

    private OutOfBandManagementDriverResponse execute(final OutOfBandManagementDriverPowerCommand cmd) {
        List<String> ipmiToolCommands = IPMITOOL.getIpmiToolCommandArgs(IpmiToolPath.value(),
                IpmiToolInterface.value(),
                IpmiToolRetries.value(),
                cmd.getOptions(), "chassis", "power", IPMITOOL.parsePowerCommand(cmd.getPowerOperation()));

        final OutOfBandManagementDriverResponse response = IPMITOOL.executeCommands(ipmiToolCommands, cmd.getTimeout());

        String oneLineCommand = StringUtils.join(ipmiToolCommands, " ");
        String result = response.getResult().trim();

        if (response.isSuccess()) {
            LOG.debug(String.format("The command [%s] was successful and got the result [%s].", oneLineCommand, result));
            if (cmd.getPowerOperation().equals(OutOfBandManagement.PowerOperation.STATUS)) {
                response.setPowerState(IPMITOOL.parsePowerState(result));
            }
        } else {
            LOG.debug(String.format("The command [%s] failed and got the result [%s]. Error: [%s].", oneLineCommand, result, response.getError()));
        }
        return response;
    }

    private OutOfBandManagementDriverResponse execute(final OutOfBandManagementDriverChangePasswordCommand cmd) {
        final String outOfBandManagementUserId = getIpmiUserId(cmd.getOptions(), cmd.getTimeout());

        final List<String> ipmiToolCommands = IPMITOOL.getIpmiToolCommandArgs(IpmiToolPath.value(),
                IpmiToolInterface.value(), IpmiToolRetries.value(),
                cmd.getOptions(), "user", "set", "password", outOfBandManagementUserId, cmd.getNewPassword());
        return IPMITOOL.executeCommands(ipmiToolCommands, cmd.getTimeout());
    }

    private void initDriver() {
        isDriverEnabled = true;
        final OutOfBandManagementDriverResponse output = IPMITOOL.executeCommands(Arrays.asList(IpmiToolPath.value(), "-V"));
        if (output.isSuccess() && output.getResult().startsWith("ipmitool version")) {
            isIpmiToolBinAvailable = true;
            LOG.debug(String.format("OutOfBandManagementDriver ipmitool initialized [%s].", output.getResult()));
        } else {
            isIpmiToolBinAvailable = false;
            LOG.error(String.format("OutOfBandManagementDriver ipmitool failed initialization with error [%s]; standard output [%s].", output.getError(), output.getResult()));
        }
    }

    private void stopDriver() {
        isDriverEnabled = false;
    }

    @Override
    public boolean start() {
        initDriver();
        return true;
    }

    @Override
    public boolean stop() {
        ipmitoolExecutor.shutdown();
        stopDriver();
        return true;
    }

    @Override
    public String getConfigComponentName() {
        return IpmitoolOutOfBandManagementDriver.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[] {IpmiToolPath, IpmiToolInterface, IpmiToolRetries};
    }
}
