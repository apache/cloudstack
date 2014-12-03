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
package org.apache.cloudstack.api.command.admin.loadbalancer;

import java.util.ArrayList;
import java.util.List;

import com.cloud.vm.VirtualMachine;
import org.apache.cloudstack.api.response.LoadBalancerRuleVmMapResponse;
import org.apache.log4j.Logger;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ResponseObject.ResponseView;
import org.apache.cloudstack.api.command.user.loadbalancer.ListLoadBalancerRuleInstancesCmd;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.UserVmResponse;

import com.cloud.uservm.UserVm;
import com.cloud.utils.Pair;

@APICommand(name = "listLoadBalancerRuleInstances", description = "List all virtual machine instances that are assigned to a load balancer rule.", responseObject = LoadBalancerRuleVmMapResponse.class, responseView = ResponseView.Full,
        requestHasSensitiveInfo = false,
        responseHasSensitiveInfo = true)
public class ListLoadBalancerRuleInstancesCmdByAdmin extends ListLoadBalancerRuleInstancesCmd {
    public static final Logger s_logger = Logger.getLogger (ListLoadBalancerRuleInstancesCmdByAdmin.class.getName());



    @Override
    public void execute(){
        Pair<List<? extends UserVm>, List<String>> vmServiceMap =  _lbService.listLoadBalancerInstances(this);
        List<? extends UserVm> result = vmServiceMap.first();
        List<String> serviceStates  = vmServiceMap.second();


        if (!isListLbVmip()) {
            // list lb instances
            ListResponse<UserVmResponse> response = new ListResponse<UserVmResponse>();
            List<UserVmResponse> vmResponses = new ArrayList<UserVmResponse>();
            if (result != null) {
                vmResponses = _responseGenerator.createUserVmResponse(ResponseView.Restricted, "loadbalancerruleinstance", result.toArray(new UserVm[result.size()]));


                for (int i = 0; i < result.size(); i++) {
                    vmResponses.get(i).setServiceState(serviceStates.get(i));
                }
            }
            response.setResponses(vmResponses);
            response.setResponseName(getCommandName());
            setResponseObject(response);

        } else {
            ListResponse<LoadBalancerRuleVmMapResponse> lbRes = new ListResponse<LoadBalancerRuleVmMapResponse>();

            List<UserVmResponse> vmResponses = new ArrayList<UserVmResponse>();
            List<LoadBalancerRuleVmMapResponse> listlbVmRes = new ArrayList<LoadBalancerRuleVmMapResponse>();

            if (result != null) {
                vmResponses = _responseGenerator.createUserVmResponse(ResponseView.Full, "loadbalancerruleinstance", result.toArray(new UserVm[result.size()]));

                List<String> ipaddr = null;

                for (int i=0;i<result.size(); i++) {
                    LoadBalancerRuleVmMapResponse lbRuleVmIpResponse = new LoadBalancerRuleVmMapResponse();
                    vmResponses.get(i).setServiceState(serviceStates.get(i));
                    lbRuleVmIpResponse.setUserVmResponse(vmResponses.get(i));
                    //get vm id from the uuid
                    VirtualMachine lbvm = _entityMgr.findByUuid(VirtualMachine.class, vmResponses.get(i).getId());
                    lbRuleVmIpResponse.setIpAddr(_lbService.listLbVmIpAddress(getId(), lbvm.getId()));
                    lbRuleVmIpResponse.setObjectName("lbrulevmidip");
                    listlbVmRes.add(lbRuleVmIpResponse);
                }
            }

            lbRes.setResponseName(getCommandName());
            lbRes.setResponses(listlbVmRes);
            setResponseObject(lbRes);
        }
    }
}
