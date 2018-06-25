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

import java.util.ArrayList;
import java.util.List;

import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.log4j.Logger;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.AccountResponse;
import org.apache.cloudstack.api.response.ClusterResponse;
import org.apache.cloudstack.api.response.ConfigurationResponse;
import org.apache.cloudstack.api.response.ImageStoreResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.StoragePoolResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.cloudstack.config.Configuration;

import com.cloud.utils.Pair;

@APICommand(name = "listConfigurations", description = "Lists all configurations.", responseObject = ConfigurationResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class ListCfgsByCmd extends BaseListCmd {
    public static final Logger s_logger = Logger.getLogger(ListCfgsByCmd.class.getName());

    private static final String s_name = "listconfigurationsresponse";

    // ///////////////////////////////////////////////////
    // ////////////// API parameters /////////////////////
    // ///////////////////////////////////////////////////

    @Parameter(name = ApiConstants.CATEGORY, type = CommandType.STRING, description = "lists configurations by category")
    private String category;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, description = "lists configuration by name")
    private String configName;

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

    @Parameter(name = ApiConstants.DOMAIN_ID,
               type = CommandType.UUID,
               entityType = DomainResponse.class,
               description = "the ID of the Domain to update the parameter value for corresponding domain")
    private Long domainId;

    @Parameter(name = ApiConstants.IMAGE_STORE_UUID,
            type = CommandType.UUID,
            entityType = ImageStoreResponse.class,
            description = "the ID of the Image Store to update the parameter value for corresponding image store")
    private Long imageStoreId;

    // ///////////////////////////////////////////////////
    // ///////////////// Accessors ///////////////////////
    // ///////////////////////////////////////////////////

    public String getCategory() {
        return category;
    }

    public String getConfigName() {
        return configName;
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

    public Long getDomainId() {
        return domainId;
    }

    public Long getImageStoreId() {
        return imageStoreId;
    }

    @Override
    public Long getPageSizeVal() {
        Long defaultPageSize = 500L;
        Integer pageSizeInt = getPageSize();
        if (pageSizeInt != null) {
            if (pageSizeInt.longValue() == s_pageSizeUnlimited) {
                defaultPageSize = null;
            } else {
                defaultPageSize = pageSizeInt.longValue();
            }
        }
        return defaultPageSize;
    }

    // ///////////////////////////////////////////////////
    // ///////////// API Implementation///////////////////
    // ///////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public void execute() {
        Pair<List<? extends Configuration>, Integer> result = _mgr.searchForConfigurations(this);
        ListResponse<ConfigurationResponse> response = new ListResponse<ConfigurationResponse>();
        List<ConfigurationResponse> configResponses = new ArrayList<ConfigurationResponse>();
        for (Configuration cfg : result.first()) {
            ConfigurationResponse cfgResponse = _responseGenerator.createConfigurationResponse(cfg);
            cfgResponse.setObjectName("configuration");
            if (getZoneId() != null) {
                cfgResponse.setScope("zone");
            }
            if (getClusterId() != null) {
                cfgResponse.setScope("cluster");
            }
            if (getStoragepoolId() != null) {
                cfgResponse.setScope("storagepool");
            }
            if (getAccountId() != null) {
                cfgResponse.setScope("account");
            }
            if (getDomainId() != null) {
                cfgResponse.setScope("domain");
            }
            if (getImageStoreId() != null){
                cfgResponse.setScope("imagestore");
            }
            configResponses.add(cfgResponse);
        }

        response.setResponses(configResponses, result.second());
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }
}
