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

import java.util.List;

import javax.inject.Inject;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.command.user.UserCmd;
import org.apache.cloudstack.api.command.user.vm.ListVMsCmd;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.cloudstack.metrics.MetricsService;
import org.apache.cloudstack.response.VmMetricsResponse;

/**
 * API supported for backward compatibility. Use the {@link ListVMsUsageHistoryCmd} API instead. <br>
 * The reasons for this are: <br>
 * <ul>
 *     <li>While API {@link ListVMsMetricsCmd} allows ACS users to get only the most recent stats data
 *     from VMs or their cumulative data, the {@link ListVMsUsageHistoryCmd} API allows getting historical
 *     data by filtering by specific VMs and periods.</li>
 *     <li>{@link ListVMsMetricsCmd} just extends the {@link ListVMsCmd} API, so it inherits all of
 *     its parameters, even if some of them are not suitable/useful for the API purpose.</li>
 *     <li>{@link ListVMsMetricsCmd} returns all VM information just like the {@link ListVMsCmd} API,
 *     although most of it is not suitable/useful for the API purpose.</li>
 * </ul>
 */
@APICommand(name = ListVMsMetricsCmd.APINAME, description = "Lists VM metrics", responseObject = VmMetricsResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false,  responseView = ResponseObject.ResponseView.Restricted,
        since = "4.9.3", authorized = {RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User})
public class ListVMsMetricsCmd extends ListVMsCmd implements UserCmd {
    public static final String APINAME = "listVirtualMachinesMetrics";

    @Inject
    private MetricsService metricsService;

    @Override
    public String getCommandName() {
        return APINAME.toLowerCase() + BaseCmd.RESPONSE_SUFFIX;
    }

    @Override
    public void execute() {
        ListResponse<UserVmResponse> userVms = _queryService.searchForUserVMs(this);
        updateVMResponse(userVms.getResponses());
        final List<VmMetricsResponse> metricsResponses = metricsService.listVmMetrics(userVms.getResponses());
        ListResponse<VmMetricsResponse> response = new ListResponse<>();
        response.setResponses(metricsResponses, userVms.getCount());
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }
}
