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

import com.google.common.base.Strings;
import org.apache.cloudstack.acl.RoleService;
import org.apache.log4j.Logger;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiArgValidator;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.AccountResponse;
import org.apache.cloudstack.api.response.ClusterResponse;
import org.apache.cloudstack.api.response.ConfigurationResponse;
import org.apache.cloudstack.api.response.ImageStoreResponse;
import org.apache.cloudstack.api.response.StoragePoolResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.cloudstack.config.Configuration;

import com.cloud.user.Account;

@APICommand(name = "updateConfiguration", description = "Updates a configuration.", responseObject = ConfigurationResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class UpdateCfgCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(UpdateCfgCmd.class.getName());
    private static final String s_name = "updateconfigurationresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, required = true, description = "the name of the configuration")
    private String cfgName;

    @Parameter(name = ApiConstants.VALUE, type = CommandType.STRING, description = "the value of the configuration", length = 4095)
    private String value;

    @Parameter(name = ApiConstants.ZONE_ID,
               type = CommandType.UUID,
               entityType = ZoneResponse.class,
               description = "the ID of the Zone to update the parameter value for corresponding zone")
    private Long zoneId;

    @Parameter(name = ApiConstants.CLUSTER_ID,
               type = CommandType.UUID,
               entityType = ClusterResponse.class,
               description = "the ID of the Cluster to update the parameter value for corresponding cluster")
    private Long clusterId;

    @Parameter(name = ApiConstants.STORAGE_ID,
               type = CommandType.UUID,
               entityType = StoragePoolResponse.class,
               description = "the ID of the Storage pool to update the parameter value for corresponding storage pool")
    private Long storagePoolId;

    @Parameter(name = ApiConstants.ACCOUNT_ID,
               type = CommandType.UUID,
               entityType = AccountResponse.class,
               description = "the ID of the Account to update the parameter value for corresponding account")
    private Long accountId;

    @Parameter(name = ApiConstants.IMAGE_STORE_UUID,
            type = CommandType.UUID,
            entityType = ImageStoreResponse.class,
            description = "the ID of the Image Store to update the parameter value for corresponding image store",
            validations = ApiArgValidator.PositiveNumber)
    private Long imageStoreId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getCfgName() {
        return cfgName;
    }

    public void setCfgName(final String cfgName) {
        this.cfgName = cfgName;
    }

    public String getValue() {
        return value;
    }

    public Long getZoneId() {
        return zoneId;
    }

    public Long getClusterId() {
        return clusterId;
    }

    public Long getStoragepoolId() {
        return storagePoolId;
    }

    public Long getAccountId() {
        return accountId;
    }

    public Long getImageStoreId() {
        return imageStoreId;
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
    public void execute() {
        if (Strings.isNullOrEmpty(getCfgName())) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, "Empty configuration name provided");
        }
        if (getCfgName().equalsIgnoreCase(RoleService.EnableDynamicApiChecker.key())) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, "Restricted configuration update not allowed");
        }
        Configuration cfg = _configService.updateConfiguration(this);
        if (cfg != null) {
            ConfigurationResponse response = _responseGenerator.createConfigurationResponse(cfg);
            response.setResponseName(getCommandName());
            if (getZoneId() != null) {
                response.setScope("zone");
            }
            if (getClusterId() != null) {
                response.setScope("cluster");
            }
            if (getStoragepoolId() != null) {
                response.setScope("storagepool");
            }
            if (getAccountId() != null) {
                response.setScope("account");
            }
            response.setValue(value);
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to update config");
        }
    }
}
