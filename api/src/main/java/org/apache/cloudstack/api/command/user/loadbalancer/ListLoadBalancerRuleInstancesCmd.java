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
package org.apache.cloudstack.api.command.user.loadbalancer;

import java.util.ArrayList;
import java.util.List;

import com.cloud.vm.VirtualMachine;

import org.apache.cloudstack.api.command.user.UserCmd;
import org.apache.cloudstack.api.response.LoadBalancerRuleVmMapResponse;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject.ResponseView;
import org.apache.cloudstack.api.response.FirewallRuleResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.UserVmResponse;

import com.cloud.uservm.UserVm;
import com.cloud.utils.Pair;

@APICommand(name = "listLoadBalancerRuleInstances", description = "List all virtual machine instances that are assigned to a load balancer rule.", responseObject = LoadBalancerRuleVmMapResponse.class, responseView = ResponseView.Restricted,
            requestHasSensitiveInfo = false,
            responseHasSensitiveInfo = true)
public class ListLoadBalancerRuleInstancesCmd extends BaseListCmd implements UserCmd {

    private static final String s_name = "listloadbalancerruleinstancesresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.APPLIED,
               type = CommandType.BOOLEAN,
               description = "true if listing all virtual machines currently applied to the load balancer rule; default is true")
    private Boolean applied;

    @Parameter(name = ApiConstants.ID,
               type = CommandType.UUID,
               entityType = FirewallRuleResponse.class,
               required = true,
               description = "the ID of the load balancer rule")
    private Long id;


    @Parameter(name = ApiConstants.LIST_LB_VMIPS,
            type = CommandType.BOOLEAN,
            description = "true if load balancer rule VM IP information to be included; default is false")
    private boolean isListLbVmip;


    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Boolean isApplied() {
        return applied;
    }

    public Long getId() {
        return id;
    }

    public boolean isListLbVmip() {
        return isListLbVmip;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public void execute() {
        Pair<List<? extends UserVm>, List<String>> vmServiceMap =  _lbService.listLoadBalancerInstances(this);
        List<? extends UserVm> result = vmServiceMap.first();
        logger.debug(String.format("A total of [%s] user VMs were obtained when listing the load balancer instances: [%s].", result.size(), result));

        List<String> serviceStates  = vmServiceMap.second();
        logger.debug(String.format("A total of [%s] service states were obtained when listing the load balancer instances: [%s].", serviceStates.size(), serviceStates));

        if (!isListLbVmip()) {
            ListResponse<UserVmResponse> response = new ListResponse<>();
            List<UserVmResponse> vmResponses = _responseGenerator.createUserVmResponse(ResponseView.Restricted, "loadbalancerruleinstance", result.toArray(new UserVm[0]));

            for (int i = 0; i < result.size(); i++) {
                vmResponses.get(i).setServiceState(serviceStates.get(i));
            }

            response.setResponses(vmResponses);
            response.setResponseName(getCommandName());
            setResponseObject(response);
            return;
        }

        ListResponse<LoadBalancerRuleVmMapResponse> lbRes = new ListResponse<>();

        List<UserVmResponse> vmResponses = _responseGenerator.createUserVmResponse(getResponseView(), "loadbalancerruleinstance", result.toArray(new UserVm[0]));
        List<LoadBalancerRuleVmMapResponse> lbRuleVmMapList = new ArrayList<>();

        for (int i=0; i<result.size(); i++) {
            LoadBalancerRuleVmMapResponse lbRuleVmIpResponse = new LoadBalancerRuleVmMapResponse();
            UserVmResponse userVmResponse = vmResponses.get(i);
            userVmResponse.setServiceState(serviceStates.get(i));
            lbRuleVmIpResponse.setUserVmResponse(userVmResponse);

            VirtualMachine lbVm = _entityMgr.findByUuid(VirtualMachine.class, userVmResponse.getId());
            lbRuleVmIpResponse.setIpAddr(_lbService.listLbVmIpAddress(getId(), lbVm.getId()));
            lbRuleVmIpResponse.setObjectName("lbrulevmidip");
            lbRuleVmMapList.add(lbRuleVmIpResponse);
        }

        lbRes.setResponseName(getCommandName());
        lbRes.setResponses(lbRuleVmMapList);
        setResponseObject(lbRes);
    }
}
