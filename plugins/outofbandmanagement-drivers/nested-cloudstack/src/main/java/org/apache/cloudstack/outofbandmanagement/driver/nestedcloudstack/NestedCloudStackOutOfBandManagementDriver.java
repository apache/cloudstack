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
package org.apache.cloudstack.outofbandmanagement.driver.nestedcloudstack;

import br.com.autonomiccs.apacheCloudStack.client.ApacheCloudStackClient;
import br.com.autonomiccs.apacheCloudStack.client.ApacheCloudStackRequest;
import br.com.autonomiccs.apacheCloudStack.client.beans.ApacheCloudStackUser;
import br.com.autonomiccs.apacheCloudStack.exceptions.ApacheCloudStackClientRequestRuntimeException;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.exception.CloudRuntimeException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import com.google.common.collect.ImmutableMap;
import org.apache.cloudstack.outofbandmanagement.OutOfBandManagement;
import org.apache.cloudstack.outofbandmanagement.OutOfBandManagementDriver;
import org.apache.cloudstack.outofbandmanagement.driver.OutOfBandManagementDriverChangePasswordCommand;
import org.apache.cloudstack.outofbandmanagement.driver.OutOfBandManagementDriverCommand;
import org.apache.cloudstack.outofbandmanagement.driver.OutOfBandManagementDriverPowerCommand;
import org.apache.cloudstack.outofbandmanagement.driver.OutOfBandManagementDriverResponse;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public final class NestedCloudStackOutOfBandManagementDriver extends AdapterBase implements OutOfBandManagementDriver {
    private static final Logger LOG = Logger.getLogger(NestedCloudStackOutOfBandManagementDriver.class);

    public OutOfBandManagementDriverResponse execute(final OutOfBandManagementDriverCommand cmd) {
        OutOfBandManagementDriverResponse response = new OutOfBandManagementDriverResponse(null, "Unsupported Command", false);

        if (cmd instanceof OutOfBandManagementDriverPowerCommand) {
            response = execute((OutOfBandManagementDriverPowerCommand) cmd);
        } else if (cmd instanceof OutOfBandManagementDriverChangePasswordCommand) {
            throw new CloudRuntimeException("Change password operation is not supported by the nested-cloudstack out-of-band management driver");
        }

        return response;
    }

    protected void ensureOptionExists(final ImmutableMap<OutOfBandManagement.Option, String> options, final OutOfBandManagement.Option option) {
        if (options != null && option != null && options.containsKey(option) && StringUtils.isNotEmpty(options.get(option))) {
            return;
        }
        throw new CloudRuntimeException("Invalid out-of-band management configuration detected for the nested-cloudstack driver");
    }

    protected OutOfBandManagement.PowerState getNestedVMPowerState(final String jsonResponse) {
        if (StringUtils.isEmpty(jsonResponse)) {
            return OutOfBandManagement.PowerState.Unknown;
        }

        final ObjectMapper mapper = new ObjectMapper();
        try {
            Map<String, Object> listResponse = mapper.readValue(jsonResponse, Map.class);
            if (listResponse != null && listResponse.containsKey("listvirtualmachinesresponse")
                    && ((Map<String, Object>) listResponse.get("listvirtualmachinesresponse")).containsKey("virtualmachine")) {
                Map<String, String> vmResponse = ((Map<String, List<Map<String, String>>>) listResponse.get("listvirtualmachinesresponse")).get("virtualmachine").get(0);
                if (vmResponse != null && vmResponse.containsKey("state")) {
                    if("Running".equals(vmResponse.get("state"))) {
                        return OutOfBandManagement.PowerState.On;
                    } else if("Stopped".equals(vmResponse.get("state"))) {
                        return OutOfBandManagement.PowerState.Off;
                    }
                }
            }
        } catch (IOException e) {
            LOG.warn("Exception caught while de-serializing and reading state of the nested-cloudstack VM from the response: " + jsonResponse + ", with exception:", e);
        }
        return OutOfBandManagement.PowerState.Unknown;
    }

    private OutOfBandManagementDriverResponse execute(final OutOfBandManagementDriverPowerCommand cmd) {
        if (cmd == null || cmd.getPowerOperation() == null) {
            throw new CloudRuntimeException("Invalid out-of-band management power command provided to the nested-cloudstack driver");
        }

        final ImmutableMap<OutOfBandManagement.Option, String> options = cmd.getOptions();
        ensureOptionExists(options, OutOfBandManagement.Option.ADDRESS);
        ensureOptionExists(options, OutOfBandManagement.Option.PORT);
        ensureOptionExists(options, OutOfBandManagement.Option.USERNAME);
        ensureOptionExists(options, OutOfBandManagement.Option.PASSWORD);

        final String url = options.get(OutOfBandManagement.Option.ADDRESS);
        final String vmUuid = options.get(OutOfBandManagement.Option.PORT);
        final String apiKey = options.get(OutOfBandManagement.Option.USERNAME);
        final String secretKey = options.get(OutOfBandManagement.Option.PASSWORD);

        final ApacheCloudStackUser apacheCloudStackUser = new ApacheCloudStackUser(secretKey, apiKey);
        final ApacheCloudStackClient client = new ApacheCloudStackClient(url, apacheCloudStackUser);
        client.setValidateServerHttpsCertificate(false);
        client.setShouldRequestsExpire(false);
        client.setConnectionTimeout((int) cmd.getTimeout().getStandardSeconds());

        String apiName = "listVirtualMachines";
        switch (cmd.getPowerOperation()) {
            case ON:
                apiName = "startVirtualMachine";
                break;
            case OFF:
            case SOFT:
                apiName = "stopVirtualMachine";
                break;
            case CYCLE:
            case RESET:
                apiName = "rebootVirtualMachine";
                break;
        }

        final ApacheCloudStackRequest apacheCloudStackRequest = new ApacheCloudStackRequest(apiName);
        apacheCloudStackRequest.addParameter("response", "json");
        apacheCloudStackRequest.addParameter("forced", "true");
        apacheCloudStackRequest.addParameter("id", vmUuid);

        final String apiResponse;
        try {
            apiResponse = client.executeRequest(apacheCloudStackRequest);
        } catch (final ApacheCloudStackClientRequestRuntimeException e) {
            LOG.error("Nested CloudStack oobm plugin failed due to API error: ", e);
            final OutOfBandManagementDriverResponse failedResponse = new OutOfBandManagementDriverResponse(e.getResponse(), "HTTP error code: " + e.getStatusCode(), false);
            if (e.getStatusCode() == 401) {
                failedResponse.setAuthFailure(true);
            }
            return failedResponse;
        }

        final OutOfBandManagementDriverResponse response = new OutOfBandManagementDriverResponse(apiResponse, null, true);
        if (OutOfBandManagement.PowerOperation.STATUS.equals(cmd.getPowerOperation())) {
            response.setPowerState(getNestedVMPowerState(apiResponse));
        }
        return response;
    }
}
