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
package org.apache.cloudstack.api.command.admin.config;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ConfigurationResponse;
import org.apache.log4j.Logger;

import com.cloud.configuration.Configuration;
import com.cloud.user.Account;

@APICommand(name = "updateConfiguration", description="Updates a configuration.", responseObject=ConfigurationResponse.class)
public class UpdateCfgCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(UpdateCfgCmd.class.getName());
    private static final String s_name = "updateconfigurationresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.NAME, type=CommandType.STRING, required=true, description="the name of the configuration")
    private String cfgName;

    @Parameter(name=ApiConstants.VALUE, type=CommandType.STRING, description="the value of the configuration", length=4095)
    private String value;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getCfgName() {
        return cfgName;
    }

    public String getValue() {
        return value;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public void execute(){
        Configuration cfg = _configService.updateConfiguration(this);
        if (cfg != null) {
            ConfigurationResponse response = _responseGenerator.createConfigurationResponse(cfg);
            response.setResponseName(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to update config");
        }
    }
}
