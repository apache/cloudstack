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

package org.apache.cloudstack.api;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.command.admin.management.ListMgmtsCmd;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.ManagementServerResponse;
import org.apache.cloudstack.metrics.MetricsService;
import org.apache.cloudstack.response.ManagementServerMetricsResponse;

import javax.inject.Inject;
import java.util.List;

@APICommand(name = ListMgmtsMetricsCmd.APINAME, description = "Lists Management Server metrics", responseObject = ManagementServerMetricsResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false,  responseView = ResponseObject.ResponseView.Full,
        since = "4.17.0", authorized = {RoleType.Admin})
public class ListMgmtsMetricsCmd  extends ListMgmtsCmd {
    public static final String APINAME = "listManagementServersMetrics";

    @Parameter(name = MetricConstants.SYSTEM, type = CommandType.BOOLEAN, entityType = ManagementServerMetricsResponse.class, description = "include system level stats")
    private boolean system;

    @Inject
    private MetricsService metricsService;

    @Override
    public String getCommandName() {
        return APINAME.toLowerCase() + BaseCmd.RESPONSE_SUFFIX;
    }

    @Override
    public void execute() {
        ListResponse<ManagementServerResponse> managementServers = _queryService.listManagementServers(this);
        final List<ManagementServerMetricsResponse> metricsResponses = metricsService.listManagementServerMetrics(managementServers.getResponses());
        ListResponse<ManagementServerMetricsResponse> response = new ListResponse<>();
        response.setResponses(metricsResponses, managementServers.getCount());
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }
}
