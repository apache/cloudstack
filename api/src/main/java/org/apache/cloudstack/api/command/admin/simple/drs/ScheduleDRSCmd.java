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
package org.apache.cloudstack.api.command.admin.simple.drs;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiArgValidator;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ClusterResponse;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.api.response.simple.drs.ScheduleDRSResponse;
import org.apache.cloudstack.simple.drs.SimpleDRSJobInfo;
import org.apache.cloudstack.simple.drs.SimpleDRSManager;
import org.apache.log4j.Logger;

import javax.inject.Inject;

@APICommand(name = ScheduleDRSCmd.APINAME, description = "Schedule a DRS job using configured DRS provider and rebalancing algorithm", responseObject = ScheduleDRSResponse.class, since = "4.14.0", authorized = {
        RoleType.Admin })
public class ScheduleDRSCmd extends BaseCmd {

    public static final String APINAME = "scheduleDRS";

    @Parameter(name = ApiConstants.CLUSTER_ID, type = CommandType.UUID, entityType = ClusterResponse.class, required = true, description = "The cluster on which to perform DRS", validations = {
            ApiArgValidator.NotNullOrEmpty })
    private Long clusterId;

    @Inject
    private SimpleDRSManager drsManager;

    private static final Logger LOG = Logger.getLogger(ScheduleDRSCmd.class);

    public ScheduleDRSCmd() {
    }

    public ScheduleDRSCmd(Long clusterId) {
        this.clusterId = clusterId;
    }

    public Long getClusterId() {
        return this.clusterId;
    }

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException,
            ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        SimpleDRSJobInfo info = new SimpleDRSJobInfo(clusterId);
        drsManager.schedule(info);
        SuccessResponse response = new SuccessResponse("scheduledrsresponse");
        this.setResponseObject(response);
    }

    @Override
    public String getCommandName() {
        return APINAME.toLowerCase() + BaseCmd.RESPONSE_SUFFIX;
    }

    @Override
    public long getEntityOwnerId() {
        return 0;
    }
}
