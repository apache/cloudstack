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
package org.apache.cloudstack.api.command.admin.internallb;

import org.apache.commons.lang.BooleanUtils;
import org.apache.log4j.Logger;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListProjectAndAccountResourcesCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.DomainRouterResponse;
import org.apache.cloudstack.api.response.HostResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.NetworkResponse;
import org.apache.cloudstack.api.response.PodResponse;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.cloudstack.api.response.VpcResponse;
import org.apache.cloudstack.api.response.ZoneResponse;

import com.cloud.network.router.VirtualRouter.Role;
import com.cloud.vm.VirtualMachine;

@APICommand(name = "listInternalLoadBalancerVMs", description = "List internal LB VMs.", responseObject = DomainRouterResponse.class, entityType = {VirtualMachine.class},
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class ListInternalLBVMsCmd extends BaseListProjectAndAccountResourcesCmd {
    public static final Logger s_logger = Logger.getLogger(ListInternalLBVMsCmd.class.getName());

    private static final String s_name = "listinternallbvmsresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.HOST_ID, type = CommandType.UUID, entityType = HostResponse.class, description = "the host ID of the Internal LB VM")
    private Long hostId;

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = UserVmResponse.class, description = "the ID of the Internal LB VM")
    private Long id;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, description = "the name of the Internal LB VM")
    private String routerName;

    @Parameter(name = ApiConstants.POD_ID, type = CommandType.UUID, entityType = PodResponse.class, description = "the Pod ID of the Internal LB VM")
    private Long podId;

    @Parameter(name = ApiConstants.STATE, type = CommandType.STRING, description = "the state of the Internal LB VM")
    private String state;

    @Parameter(name = ApiConstants.ZONE_ID, type = CommandType.UUID, entityType = ZoneResponse.class, description = "the Zone ID of the Internal LB VM")
    private Long zoneId;

    @Parameter(name = ApiConstants.NETWORK_ID, type = CommandType.UUID, entityType = NetworkResponse.class, description = "list by network id")
    private Long networkId;

    @Parameter(name = ApiConstants.VPC_ID, type = CommandType.UUID, entityType = VpcResponse.class, description = "List Internal LB VMs by VPC")
    private Long vpcId;

    @Parameter(name = ApiConstants.FOR_VPC, type = CommandType.BOOLEAN, description = "if true is passed for this parameter, list only VPC Internal LB VMs")
    private Boolean forVpc;


    @Parameter(name = ApiConstants.FETCH_ROUTER_HEALTH_CHECK_RESULTS, type = CommandType.BOOLEAN, since = "4.14",
            description = "if true is passed for this parameter, also fetch last executed health check results for the VM. Default is false")
    private Boolean fetchHealthCheckResults;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getHostId() {
        return hostId;
    }

    public Long getId() {
        return id;
    }

    public String getRouterName() {
        return routerName;
    }

    public Long getPodId() {
        return podId;
    }

    public String getState() {
        return state;
    }

    public Long getZoneId() {
        return zoneId;
    }

    public Long getNetworkId() {
        return networkId;
    }

    public Long getVpcId() {
        return vpcId;
    }

    public Boolean getForVpc() {
        return forVpc;
    }

    public String getRole() {
        return Role.INTERNAL_LB_VM.toString();
    }

    public boolean shouldFetchHealthCheckResults() {
        return BooleanUtils.isTrue(fetchHealthCheckResults);
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.DomainRouter;
    }

    @Override
    public void execute() {
        ListResponse<DomainRouterResponse> response = _queryService.searchForInternalLbVms(this);
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }
}
