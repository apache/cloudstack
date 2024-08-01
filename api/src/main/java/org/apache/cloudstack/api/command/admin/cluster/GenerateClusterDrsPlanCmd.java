/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cloudstack.api.command.admin.cluster;

import com.cloud.user.Account;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.ClusterDrsPlanResponse;
import org.apache.cloudstack.api.response.ClusterResponse;
import org.apache.cloudstack.cluster.ClusterDrsService;

import javax.inject.Inject;

import static org.apache.cloudstack.cluster.ClusterDrsService.ClusterDrsMaxMigrations;

@APICommand(name = "generateClusterDrsPlan", description = "Generate DRS plan for a cluster",
            responseObject = ClusterDrsPlanResponse.class, since = "4.19.0", requestHasSensitiveInfo = false,
            responseHasSensitiveInfo = false)
public class GenerateClusterDrsPlanCmd extends BaseCmd {

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = ClusterResponse.class, required = true,
               description = "the ID of the Cluster")
    private Long id;

    @Parameter(name = ApiConstants.MIGRATIONS, type = CommandType.INTEGER,
               description = "Maximum number of VMs to migrate for a DRS execution. Defaults to value of cluster's drs.vm.migrations setting")
    private Integer migrations;

    @Inject
    private ClusterDrsService clusterDrsService;

    public Integer getMaxMigrations() {
        if (migrations == null) {
            return ClusterDrsMaxMigrations.valueIn(getId());
        }
        return migrations;
    }

    public Long getId() {
        return id;
    }

    @Override
    public void execute() {
        final ClusterDrsPlanResponse response = clusterDrsService.generateDrsPlan(this);
        response.setResponseName(getCommandName());
        response.setObjectName(getCommandName());
        this.setResponseObject(response);
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public Long getApiResourceId() {
        return getId();
    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.Cluster;
    }
}
