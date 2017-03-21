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

import javax.inject.Inject;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.network.lb.VpcInlineLoadBalancerVmManager;
import org.apache.log4j.Logger;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.command.admin.internallb.ListInternalLBVMsCmd;
import org.apache.cloudstack.api.response.DomainRouterResponse;
import org.apache.cloudstack.api.response.ListResponse;
import com.cloud.network.router.VirtualRouter.Role;

@APICommand(name = "listVpcInlineLoadBalancerVMs", description="List vpc inline LB VMs.", responseObject=DomainRouterResponse.class,
        since = "4.10.0", authorized = {RoleType.Admin})
public class ListVpcInlineLoadBalancerVmsCmd extends ListInternalLBVMsCmd {
    public static final Logger s_logger = Logger.getLogger(ListVpcInlineLoadBalancerVmsCmd.class.getName());

    private static final String s_name = "listvpcinlinelbvmsresponse";

    @Inject
    private VpcInlineLoadBalancerVmManager _vpcInlineLbVmMgr;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getRole() {
        return Role.LB.toString();
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public void execute(){
        ListResponse<DomainRouterResponse> response = _queryService.searchForInternalLbVms(this);
        _vpcInlineLbVmMgr.fillPublicIps(response);

        response.setResponseName(getCommandName());
        response.setObjectName("vpcinlineloadbalancervm");
        this.setResponseObject(response);
    }
}
