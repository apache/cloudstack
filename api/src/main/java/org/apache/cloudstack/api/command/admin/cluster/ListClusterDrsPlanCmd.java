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

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.ClusterDrsPlanResponse;
import org.apache.cloudstack.api.response.ClusterResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.cluster.ClusterDrsService;

import javax.inject.Inject;

@APICommand(name = "listClusterDrsPlan", description = "List DRS plans for a clusters",
            responseObject = ClusterDrsPlanResponse.class, since = "4.19.0", requestHasSensitiveInfo = false)
public class ListClusterDrsPlanCmd extends BaseListCmd {
    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = ClusterDrsPlanResponse.class,
               description = "ID of the drs plan")
    private Long id;

    @Parameter(name = ApiConstants.CLUSTER_ID, type = CommandType.UUID, entityType = ClusterResponse.class,
               description = "ID of the cluster")
    private Long clusterId;

    @Inject
    private ClusterDrsService clusterDrsService;

    public Long getId() {
        return id;
    }

    public Long getClusterId() {
        return clusterId;
    }

    @Override
    public void execute() {
        ListResponse<ClusterDrsPlanResponse> response = clusterDrsService.listDrsPlan(this);
        response.setResponseName(getCommandName());
        response.setObjectName(getCommandName());
        setResponseObject(response);
    }
}
