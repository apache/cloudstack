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
package org.apache.cloudstack.api.commands;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ClusterResponse;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.DedicateClusterResponse;
import org.apache.cloudstack.dedicated.DedicatedService;
import org.apache.log4j.Logger;

import com.cloud.dc.DedicatedResources;
import com.cloud.event.EventTypes;
import com.cloud.user.Account;

@APICommand(name = "dedicateCluster", description= "Dedicate an existing cluster", responseObject = DedicateClusterResponse.class )
public class DedicateClusterCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(DedicateClusterCmd.class.getName());

    private static final String s_name = "dedicateclusterresponse";
    @Inject DedicatedService dedicatedService;

    @Parameter(name=ApiConstants.CLUSTER_ID, type=CommandType.UUID, entityType=ClusterResponse.class,
            required=true, description="the ID of the Cluster")
    private Long clusterId;

    @Parameter(name=ApiConstants.DOMAIN_ID, type=CommandType.UUID, entityType=DomainResponse.class, required=true, description="the ID of the containing domain")
    private Long domainId;

    @Parameter(name = ApiConstants.ACCOUNT, type = CommandType.STRING, description = "the name of the account which needs dedication. Must be used with domainId.")
    private String accountName;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getClusterId() {
        return clusterId;
    }

    public Long getDomainId() {
        return domainId;
    }

    public String getAccountName() {
        return accountName;
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_DEDICATE_RESOURCE;
    }

    @Override
    public String getEventDescription() {
        return "dedicating a cluster";
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
        List<? extends DedicatedResources> result = dedicatedService.dedicateCluster(getClusterId(), getDomainId(), getAccountName());
        ListResponse<DedicateClusterResponse> response = new ListResponse<DedicateClusterResponse>();
        List<DedicateClusterResponse> clusterResponseList = new ArrayList<DedicateClusterResponse>();
        if (result != null) {
            for (DedicatedResources resource : result) {
                DedicateClusterResponse clusterResponse = dedicatedService.createDedicateClusterResponse(resource);
                clusterResponseList.add(clusterResponse);
            }
            response.setResponses(clusterResponseList);
            response.setResponseName(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to dedicate cluster");
        }
    }

}
