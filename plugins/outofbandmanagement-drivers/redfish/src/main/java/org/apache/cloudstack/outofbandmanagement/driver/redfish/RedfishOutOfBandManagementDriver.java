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
package org.apache.cloudstack.outofbandmanagement.driver.redfish;

import javax.inject.Inject;

import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.outofbandmanagement.OutOfBandManagement;
import org.apache.cloudstack.outofbandmanagement.OutOfBandManagementDriver;
import org.apache.cloudstack.outofbandmanagement.OutOfBandManagementVO;
import org.apache.cloudstack.outofbandmanagement.dao.OutOfBandManagementDao;
import org.apache.cloudstack.outofbandmanagement.driver.OutOfBandManagementDriverChangePasswordCommand;
import org.apache.cloudstack.outofbandmanagement.driver.OutOfBandManagementDriverCommand;
import org.apache.cloudstack.outofbandmanagement.driver.OutOfBandManagementDriverPowerCommand;
import org.apache.cloudstack.outofbandmanagement.driver.OutOfBandManagementDriverResponse;
import org.apache.cloudstack.utils.redfish.RedfishClient;
import org.apache.cloudstack.utils.redfish.RedfishClient.RedfishResetCmd;
import org.apache.commons.httpclient.HttpStatus;

import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.exception.CloudRuntimeException;
import com.google.common.collect.ImmutableMap;

public class RedfishOutOfBandManagementDriver extends AdapterBase implements OutOfBandManagementDriver, Configurable {

    @Inject
    private OutOfBandManagementDao outOfBandManagementDao;
    private RedfishWrapper redfishWrapper = new RedfishWrapper();

    public static final ConfigKey<Boolean> IGNORE_SSL_CERTIFICATE = new ConfigKey<Boolean>("Advanced", Boolean.class, "redfish.ignore.ssl", "false",
            "Default value is false, ensuring that the client requests validate the certificate when using SSL. If set to true the redfish client will ignore SSL certificate validation when sending requests to a Redfish server.",
            true, ConfigKey.Scope.Global);

    public static final ConfigKey<Boolean> USE_HTTPS = new ConfigKey<Boolean>("Advanced", Boolean.class, "redfish.use.https", "true",
            "Use HTTPS/SSL for all connections.", true, ConfigKey.Scope.Global);

    public static final ConfigKey<Integer> REDFISHT_REQUEST_MAX_RETRIES = new ConfigKey<Integer>("Advanced", Integer.class, "redfish.retries", "2",
            "Number of retries allowed if a Redfish REST request experiment connection issues. If set to 0 (zero) there will be no retries.", true, ConfigKey.Scope.Global);


    private static final String HTTP_STATUS_OK = String.valueOf(HttpStatus.SC_OK);

    @Override
    public OutOfBandManagementDriverResponse execute(OutOfBandManagementDriverCommand cmd) {
        OutOfBandManagementDriverResponse response = new OutOfBandManagementDriverResponse(null, "Unsupported Command", false);
        if (cmd instanceof OutOfBandManagementDriverPowerCommand) {
            response = execute((OutOfBandManagementDriverPowerCommand)cmd);
        } else if (cmd instanceof OutOfBandManagementDriverChangePasswordCommand) {
            response = execute((OutOfBandManagementDriverChangePasswordCommand)cmd);
        } else {
            throw new CloudRuntimeException(String.format("Operation [%s] not supported by the Redfish out-of-band management driver", cmd.getClass().getSimpleName()));
        }
        return response;
    }

    /**
     *  Sends a HTTPS request to execute the given power command ({@link OutOfBandManagementDriverPowerCommand})
     */
    private OutOfBandManagementDriverResponse execute(final OutOfBandManagementDriverPowerCommand cmd) {
        ImmutableMap<OutOfBandManagement.Option, String> outOfBandOptions = cmd.getOptions();
        String username = outOfBandOptions.get(OutOfBandManagement.Option.USERNAME);
        String password = outOfBandOptions.get(OutOfBandManagement.Option.PASSWORD);
        String hostAddress = outOfBandOptions.get(OutOfBandManagement.Option.ADDRESS);
        RedfishClient redfishClient = new RedfishClient(username, password, USE_HTTPS.value(), IGNORE_SSL_CERTIFICATE.value(), REDFISHT_REQUEST_MAX_RETRIES.value());

        RedfishClient.RedfishPowerState powerState = null;
        if (cmd.getPowerOperation() == OutOfBandManagement.PowerOperation.STATUS) {
            powerState = redfishClient.getSystemPowerState(hostAddress);
        } else {
            RedfishResetCmd redfishCmd = redfishWrapper.parsePowerCommand(cmd.getPowerOperation());
            redfishClient.executeComputerSystemReset(hostAddress, redfishCmd);
        }

        OutOfBandManagementDriverResponse response = new OutOfBandManagementDriverResponse(HTTP_STATUS_OK, null, true);
        if (powerState != null) {
            OutOfBandManagement.PowerState oobPowerState = redfishWrapper.parseRedfishPowerStateToOutOfBand(powerState);
            response.setPowerState(oobPowerState);
        }
        return response;
    }

    /**
     * Executes the password change command (OutOfBandManagementDriverChangePasswordCommand)
     */
    private OutOfBandManagementDriverResponse execute(final OutOfBandManagementDriverChangePasswordCommand cmd) {
        cmd.getNewPassword();
        String hostAddress = cmd.getOptions().get(OutOfBandManagement.Option.ADDRESS);
        OutOfBandManagementVO outOfBandManagement = outOfBandManagementDao.findByHostAddress(hostAddress);
        outOfBandManagement.setPassword(cmd.getNewPassword());
        outOfBandManagementDao.update(outOfBandManagement.getId(), outOfBandManagement);

        OutOfBandManagementDriverResponse response = new OutOfBandManagementDriverResponse(HTTP_STATUS_OK, null, true);

        return response;
    }

    @Override
    public String getConfigComponentName() {
        return RedfishOutOfBandManagementDriver.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[] {IGNORE_SSL_CERTIFICATE, USE_HTTPS, REDFISHT_REQUEST_MAX_RETRIES};
    }

}
