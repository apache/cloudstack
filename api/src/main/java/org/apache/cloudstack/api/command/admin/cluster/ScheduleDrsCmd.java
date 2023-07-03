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

import com.cloud.org.Cluster;
import com.cloud.user.Account;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.ClusterResponse;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.cluster.ClusterDrsService;
import org.apache.log4j.Logger;

import javax.inject.Inject;

import static org.apache.cloudstack.cluster.ClusterDrsService.ClusterDrsIterations;

@APICommand(name = "scheduleDRS", description = "Schedule DRS for a cluster", responseObject = SuccessResponse.class, since = "4.19.0", authorized = {RoleType.Admin})
public class ScheduleDrsCmd extends BaseAsyncCmd {
    static final Logger LOG = Logger.getLogger(ScheduleDrsCmd.class);

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = ClusterResponse.class, required = true, description = "the ID of the Cluster")
    private Long id;

    @Parameter(name = "iterations", type = CommandType.DOUBLE,
            description = "The maximum number of iterations in a DRS job defined as a percentage (as a value between 0 and 1) of total number of workloads. Defaults to value of cluster's drs.iterations setting")
    private Double iterations;

    @Inject
    private ClusterDrsService clusterDrsService;

    public Long getId() {
        return id;
    }

    public Double getIterations() {
        if (iterations == null) {
            return ClusterDrsIterations.valueIn(getId());
        }
        return iterations;
    }

    @Override
    public String getEventType() {
        return "CLUSTER.DRS"; // EventTypes.EVENT_CLUSTER_DRS;
    }

    @Override
    public String getEventDescription() {
        Cluster cluster = _resourceService.getCluster(getId());
        return "Scheduling DRS for cluster: " + cluster.getUuid();
    }

    @Override
    public void execute() {
        final SuccessResponse response = clusterDrsService.executeDrs(this);
        response.setResponseName(getCommandName());
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
