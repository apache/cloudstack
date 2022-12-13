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
import org.apache.cloudstack.api.ApiArgValidator;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ImageStoreResponse;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.log4j.Logger;

import org.apache.cloudstack.api.response.AccountResponse;
import org.apache.cloudstack.api.response.ClusterResponse;
import org.apache.cloudstack.api.response.ConfigurationResponse;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.NetworkResponse;
import org.apache.cloudstack.api.response.StoragePoolResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.cloudstack.config.Configuration;

import com.cloud.user.Account;
import com.cloud.utils.Pair;

@APICommand(name = "resetConfiguration", description = "Resets a configuration. The configuration will be set to default value for global setting, and removed from account_details or domain_details for Account/Domain settings", responseObject = ConfigurationResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false, since = "4.16.0")
public class ResetCfgCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(ResetCfgCmd.class.getName());

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, required = true, description = "the name of the configuration", validations = {ApiArgValidator.NotNullOrEmpty})
    private String cfgName;

    @Parameter(name = ApiConstants.ZONE_ID,
               type = CommandType.UUID,
               entityType = ZoneResponse.class,
               description = "the ID of the Zone to reset the parameter value for corresponding zone")
    private Long zoneId;

    @Parameter(name = ApiConstants.CLUSTER_ID,
               type = CommandType.UUID,
               entityType = ClusterResponse.class,
               description = "the ID of the Cluster to reset the parameter value for corresponding cluster")
    private Long clusterId;

    @Parameter(name = ApiConstants.STORAGE_ID,
               type = CommandType.UUID,
               entityType = StoragePoolResponse.class,
               description = "the ID of the Storage pool to reset the parameter value for corresponding storage pool")
    private Long storagePoolId;

    @Parameter(name = ApiConstants.DOMAIN_ID,
            type = CommandType.UUID,
            entityType = DomainResponse.class,
            description = "the ID of the Domain to reset the parameter value for corresponding domain")
    private Long domainId;

    @Parameter(name = ApiConstants.ACCOUNT_ID,
               type = CommandType.UUID,
               entityType = AccountResponse.class,
               description = "the ID of the Account to reset the parameter value for corresponding account")
    private Long accountId;

    @Parameter(name = ApiConstants.IMAGE_STORE_ID,
            type = CommandType.UUID,
            entityType = ImageStoreResponse.class,
            description = "the ID of the Image Store to reset the parameter value for corresponding image store")
    private Long imageStoreId;

    @Parameter(name = ApiConstants.NETWORK_ID,
               type = CommandType.UUID,
               entityType = NetworkResponse.class,
               description = "the ID of the Network to update the parameter value for corresponding network")
    private Long networkId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getCfgName() {
        return cfgName;
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

    public Long getDomainId() {
        return domainId;
    }

    public Long getAccountId() {
        return accountId;
    }

    public Long getImageStoreId() {
        return imageStoreId;
    }

    public Long getNetworkId() {
        return networkId;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public void execute() {
        Pair<Configuration, String> cfg = _configService.resetConfiguration(this);
        if (cfg != null) {
            ConfigurationResponse response = _responseGenerator.createConfigurationResponse(cfg.first());
            response.setResponseName(getCommandName());
            if (getZoneId() != null) {
                response.setScope(ConfigKey.Scope.Zone.name());
            }
            if (getClusterId() != null) {
                response.setScope(ConfigKey.Scope.Cluster.name());
            }
            if (getStoragepoolId() != null) {
                response.setScope(ConfigKey.Scope.StoragePool.name());
            }
            if (getDomainId() != null) {
                response.setScope(ConfigKey.Scope.Domain.name());
            }
            if (getAccountId() != null) {
                response.setScope(ConfigKey.Scope.Account.name());
            }
            if (getImageStoreId() != null) {
                response.setScope(ConfigKey.Scope.ImageStore.name());
            }
            if (getNetworkId() != null) {
                response.setScope(ConfigKey.Scope.Network.name());
            }
            response.setValue(cfg.second());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to reset config");
        }
    }
}
