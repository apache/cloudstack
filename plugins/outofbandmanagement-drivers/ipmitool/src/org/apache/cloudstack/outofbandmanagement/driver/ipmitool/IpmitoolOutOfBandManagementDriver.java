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
import com.cloud.utils.exception.CloudRuntimeException;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.outofbandmanagement.OutOfBandManagement;
import org.apache.cloudstack.outofbandmanagement.OutOfBandManagementDriver;
import org.apache.cloudstack.outofbandmanagement.driver.OutOfBandManagementDriverChangePasswordCommand;
import org.apache.cloudstack.outofbandmanagement.driver.OutOfBandManagementDriverCommand;
import org.apache.cloudstack.outofbandmanagement.driver.OutOfBandManagementDriverPowerCommand;
import org.apache.cloudstack.outofbandmanagement.driver.OutOfBandManagementDriverResponse;
import org.apache.log4j.Logger;
import org.joda.time.Duration;

import javax.ejb.Local;
import java.util.Arrays;
import java.util.List;

@Local(value = {OutOfBandManagementDriver.class})
public class IpmitoolOutOfBandManagementDriver extends AdapterBase implements OutOfBandManagementDriver, Configurable {
    public static final Logger LOG = Logger.getLogger(IpmitoolOutOfBandManagementDriver.class);

    private static volatile boolean isDriverEnabled = false;
    private static boolean isIpmiToolBinAvailable = false;

    ConfigKey<String> IpmiToolPath = new ConfigKey<String>("Advanced", String.class, "outofbandmanagement.ipmitool.path", "/usr/bin/ipmitool",
            "The out of band management ipmitool path used by the IpmiTool driver. Default: /usr/bin/ipmitool.", true, ConfigKey.Scope.Global);

    ConfigKey<String> IpmiToolInterface = new ConfigKey<String>("Advanced", String.class, "outofbandmanagement.ipmitool.interface", "lanplus",
            "The out of band management IpmiTool driver interface to use. Default: lanplus. Valid values are: lan, lanplus, open etc.", true, ConfigKey.Scope.Global);

    ConfigKey<String> IpmiToolRetries = new ConfigKey<String>("Advanced", String.class, "outofbandmanagement.ipmitool.retries", "1",
            "The out of band management IpmiTool driver retries option -R. Default 1.", true, ConfigKey.Scope.Global);

    private String getIpmiUserId(ImmutableMap<OutOfBandManagement.Option, String> options, final Duration timeOut) {
        final String username = options.get(OutOfBandManagement.Option.USERNAME);
        if (Strings.isNullOrEmpty(username)) {
            throw new CloudRuntimeException("Empty IPMI user configured, cannot proceed to find user's ID");
        }

        final List<String> ipmiToolCommands = IpmitoolWrapper.getIpmiToolCommandArgs(IpmiToolPath.value(),
                IpmiToolInterface.value(),
                IpmiToolRetries.value(),
                options, "user", "list");
        OutOfBandManagementDriverResponse output = IpmitoolWrapper.executeCommands(ipmiToolCommands, timeOut);
        if (!output.isSuccess()) {
            throw new CloudRuntimeException("Failed to find IPMI user to change password, error: " + output.getError());
        }

        final String userId = IpmitoolWrapper.findIpmiUser(output.getResult(), username);
        if (Strings.isNullOrEmpty(userId)) {
            throw new CloudRuntimeException("No IPMI user ID found for the username: " + username);
        }
        return userId;
    }

    public OutOfBandManagementDriverResponse execute(final OutOfBandManagementDriverCommand cmd) {
        if (!isIpmiToolBinAvailable) {
            initDriver();
            if (!isIpmiToolBinAvailable) {
                return new OutOfBandManagementDriverResponse(null, "Aborting operation due to ipmitool binary not available for execution", false);
            }
        }

        OutOfBandManagementDriverResponse response = new OutOfBandManagementDriverResponse(null, "Unsupported Command", false);
        if (!isDriverEnabled) {
            response.setError("Driver not enabled or shutdown");
            return response;
        }
        if (cmd instanceof OutOfBandManagementDriverPowerCommand) {
            response = execute((OutOfBandManagementDriverPowerCommand) cmd);
        } else if (cmd instanceof OutOfBandManagementDriverChangePasswordCommand) {
            response = execute((OutOfBandManagementDriverChangePasswordCommand) cmd);
        }

        if (response != null && !response.isSuccess() && response.getError().contains("RAKP 2 HMAC is invalid")) {
            response.setAuthFailure(true);
        }
        return response;
    }

    private OutOfBandManagementDriverResponse execute(final OutOfBandManagementDriverPowerCommand cmd) {
        List<String> ipmiToolCommands = IpmitoolWrapper.getIpmiToolCommandArgs(IpmiToolPath.value(),
                IpmiToolInterface.value(),
                IpmiToolRetries.value(),
                cmd.getOptions(), "chassis", "power", IpmitoolWrapper.parsePowerCommand(cmd.getPowerOperation()));
        OutOfBandManagementDriverResponse response = IpmitoolWrapper.executeCommands(ipmiToolCommands, cmd.getTimeout());

        if (response.isSuccess() && cmd.getPowerOperation().equals(OutOfBandManagement.PowerOperation.STATUS)) {
            response.setPowerState(IpmitoolWrapper.parsePowerState(response.getResult().trim()));
        }
        return response;
    }

    private OutOfBandManagementDriverResponse execute(final OutOfBandManagementDriverChangePasswordCommand cmd) {
        String outOfBandManagementUserId = getIpmiUserId(cmd.getOptions(), cmd.getTimeout());

        List<String> ipmiToolCommands = IpmitoolWrapper.getIpmiToolCommandArgs(IpmiToolPath.value(),
                IpmiToolInterface.value(), IpmiToolRetries.value(),
                cmd.getOptions(), "user", "set", "password", outOfBandManagementUserId, cmd.getNewPassword());
        return IpmitoolWrapper.executeCommands(ipmiToolCommands, cmd.getTimeout());
    }

    private void initDriver() {
        isDriverEnabled = true;
        OutOfBandManagementDriverResponse output = IpmitoolWrapper.executeCommands(Arrays.asList(IpmiToolPath.value(), "-V"), Duration.ZERO);
        if (output.isSuccess() && output.getResult().startsWith("ipmitool version")) {
            isIpmiToolBinAvailable = true;
            LOG.debug("OutOfBandManagementDriver ipmitool initialized: " + output.getResult());
        } else {
            isIpmiToolBinAvailable = false;
            LOG.error("OutOfBandManagementDriver ipmitool failed initialization with error: " + output.getError() + "; standard output: " + output.getResult());
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
