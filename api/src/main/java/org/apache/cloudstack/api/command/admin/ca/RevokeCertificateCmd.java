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

package org.apache.cloudstack.api.command.admin.ca;

import java.math.BigInteger;

import javax.inject.Inject;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.ca.CAManager;
import org.apache.cloudstack.context.CallContext;
import org.apache.commons.lang3.StringUtils;

import com.cloud.event.EventTypes;

@APICommand(name = "revokeCertificate",
        description = "Revokes certificate using configured CA plugin",
        responseObject = SuccessResponse.class,
        requestHasSensitiveInfo = true,
        responseHasSensitiveInfo = false,
        since = "4.11.0",
        authorized = {RoleType.Admin})
public class RevokeCertificateCmd extends BaseAsyncCmd {


    @Inject
    private CAManager caManager;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.SERIAL, type = BaseCmd.CommandType.STRING, required = true, description = "The certificate serial number, as a hex value")
    private String serial;

    @Parameter(name = ApiConstants.CN, type = BaseCmd.CommandType.STRING, description = "The certificate CN")
    private String cn;

    @Parameter(name = ApiConstants.PROVIDER, type = BaseCmd.CommandType.STRING, description = "Name of the CA service provider, otherwise the default configured provider plugin will be used")
    private String provider;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public BigInteger getSerialBigInteger() {
        if (StringUtils.isEmpty(serial)) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, "Certificate serial cannot be empty");
        }
        return new BigInteger(serial, 16);
    }

    public String getCn() {
        return cn;
    }

    public String getProvider() {
        return provider;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() {
        boolean result = caManager.revokeCertificate(getSerialBigInteger(), getCn(), getProvider());
        SuccessResponse response = new SuccessResponse(getCommandName());
        response.setSuccess(result);
        setResponseObject(response);
    }

    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccount().getId();
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_CA_CERTIFICATE_REVOKE;
    }

    @Override
    public String getEventDescription() {
        return "revoking certificate with serial id=" + serial + ", cn=" + cn;
    }
}
