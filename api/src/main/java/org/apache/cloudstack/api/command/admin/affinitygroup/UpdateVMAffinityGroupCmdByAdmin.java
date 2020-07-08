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
package org.apache.cloudstack.api.command.admin.affinitygroup;

import org.apache.log4j.Logger;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ResponseObject.ResponseView;
import org.apache.cloudstack.api.command.admin.AdminCmd;
import org.apache.cloudstack.api.command.user.affinitygroup.UpdateVMAffinityGroupCmd;
import org.apache.cloudstack.api.response.UserVmResponse;

import com.cloud.vm.VirtualMachine;


@APICommand(name = "updateVMAffinityGroup", description = "Updates the affinity/anti-affinity group associations of a virtual machine. The VM has to be stopped and restarted for the "
        + "new properties to take effect.", responseObject = UserVmResponse.class, responseView = ResponseView.Full,
        entityType = {VirtualMachine.class},
        requestHasSensitiveInfo = false,
        responseHasSensitiveInfo = true)
public class UpdateVMAffinityGroupCmdByAdmin extends UpdateVMAffinityGroupCmd implements AdminCmd {
    public static final Logger s_logger = Logger.getLogger(UpdateVMAffinityGroupCmdByAdmin.class.getName());
}
