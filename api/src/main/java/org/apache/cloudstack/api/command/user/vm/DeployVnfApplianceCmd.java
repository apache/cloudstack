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
package org.apache.cloudstack.api.command.user.vm;

import com.cloud.exception.ResourceAllocationException;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.VirtualMachine;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.api.command.user.UserCmd;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.cloudstack.storage.template.VnfTemplateUtils;
import org.apache.commons.collections.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

@APICommand(name = "deployVnfAppliance",
        description = "Creates and automatically starts a VNF appliance based on a service offering, disk offering, and template.",
        responseObject = UserVmResponse.class,
        responseView = ResponseObject.ResponseView.Restricted,
        entityType = {VirtualMachine.class},
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = true,
        authorized = {RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User},
        since = "4.19.0")
public class DeployVnfApplianceCmd extends DeployVMCmd implements UserCmd {

    @Parameter(name = ApiConstants.VNF_CONFIGURE_MANAGEMENT, type = CommandType.BOOLEAN, required = false,
            description = "True by default, security group or network rules (source nat and firewall rules) will be configured for VNF management interfaces. False otherwise. " +
                    "Network rules are configured if management network is an isolated network or shared network with security groups.")
    private Boolean vnfConfigureManagement;

    @Parameter(name = ApiConstants.VNF_CIDR_LIST, type = CommandType.LIST, collectionType = CommandType.STRING,
            description = "the CIDR list to forward traffic from to the VNF management interface. Multiple entries must be separated by a single comma character (,). The default value is 0.0.0.0/0.")
    private List<String> vnfCidrlist;

    public Boolean getVnfConfigureManagement() {
        return vnfConfigureManagement != null && vnfConfigureManagement;
    }

    public List<String> getVnfCidrlist() {
        if (CollectionUtils.isNotEmpty(vnfCidrlist)) {
            return vnfCidrlist;
        } else {
            List<String> defaultCidrList = new ArrayList<String>();
            defaultCidrList.add(NetUtils.ALL_IP4_CIDRS);
            return defaultCidrList;
        }
    }

    @Override
    public void create() throws ResourceAllocationException {
        VnfTemplateUtils.validateVnfCidrList(this.getVnfCidrlist());

        super.create();
    }
}
