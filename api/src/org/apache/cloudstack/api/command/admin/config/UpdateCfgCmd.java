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
import org.apache.cloudstack.api.response.*;
import org.apache.cloudstack.config.Configuration;

import org.apache.log4j.Logger;

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

    @Parameter(name=ApiConstants.ZONE_ID, type=CommandType.UUID, entityType=ZoneResponse.class, description="the ID of the Zone to update the parameter value for corresponding zone")
    private Long zone_id;

    @Parameter(name=ApiConstants.CLUSTER_ID, type=CommandType.UUID, entityType=ClusterResponse.class, description="the ID of the Cluster to update the parameter value for corresponding cluster")
    private Long cluster_id;

    @Parameter(name=ApiConstants.STORAGE_ID, type=CommandType.UUID, entityType=StoragePoolResponse.class, description="the ID of the Storage pool to update the parameter value for corresponding storage pool")
    private Long storagepool_id;

    @Parameter(name=ApiConstants.ACCOUNT_ID, type=CommandType.UUID, entityType=AccountResponse.class, description="the ID of the Account to update the parameter value for corresponding account")
    private Long account_id;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getCfgName() {
        return cfgName;
    }

    public String getValue() {
        return value;
    }

    public Long getZoneId() {
        return zone_id;
    }

    public Long getClusterId() {
        return cluster_id;
    }

    public Long getStoragepoolId() {
        return storagepool_id;
    }

    public Long getAccountId() {
        return account_id;
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
            if(getZoneId() != null) {
                response.setScope("zone");
            }
            if(getClusterId() != null) {
                response.setScope("cluster");
            }
            if(getStoragepoolId() != null) {
                response.setScope("storagepool");
            }
            if(getAccountId() != null) {
                response.setScope("account");
            }
            response.setValue(value);
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to update config");
        }
    }
}
