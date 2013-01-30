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

package org.apache.cloudstack.api.command.user.vmsnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListTaggedResourcesCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.command.admin.host.ListHostsCmd;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.cloudstack.api.response.VMSnapshotResponse;

import com.cloud.vm.snapshot.VMSnapshot;

@APICommand(name="listVMSnapshot", description = "List virtual machine snapshot by conditions", responseObject = VMSnapshotResponse.class, since = "4.1.0")
public class ListVMSnapshotCmd extends BaseListTaggedResourcesCmd {
    public static final Logger s_logger = Logger.getLogger(ListHostsCmd.class
            .getName());

    private static final String s_name = "listvmsnapshotresponse";

    // ///////////////////////////////////////////////////
    // ////////////// API parameters /////////////////////
    // ///////////////////////////////////////////////////

    @Parameter(name=ApiConstants.VM_SNAPSHOT_ID, type=CommandType.UUID, entityType=VMSnapshotResponse.class,
             description="The ID of the VM snapshot")
    private Long id;

    @Parameter(name=ApiConstants.STATE, type=CommandType.STRING, description="state of the virtual machine snapshot")
    private String state;

    @Parameter(name = ApiConstants.VIRTUAL_MACHINE_ID, type = CommandType.UUID, entityType=UserVmResponse.class, description = "the ID of the vm")
    private Long vmId;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, description = "lists snapshot by snapshot name or display name")
    private String vmSnapshotName;

    // ///////////////////////////////////////////////////
    // ///////////////// Accessors ///////////////////////
    // ///////////////////////////////////////////////////

    public String getState() {
        return state;
    }

    public String getVmSnapshotName() {
        return vmSnapshotName;
    }

    public Long getVmId() {
        return vmId;
    }

    public Long getId() {
        return id;
    }

    // ///////////////////////////////////////////////////
    // ///////////// API Implementation///////////////////
    // ///////////////////////////////////////////////////

    @Override
    public void execute() {
        List<? extends VMSnapshot> result = _vmSnapshotService
                .listVMSnapshots(this);
        ListResponse<VMSnapshotResponse> response = new ListResponse<VMSnapshotResponse>();
        List<VMSnapshotResponse> snapshotResponses = new ArrayList<VMSnapshotResponse>();
        for (VMSnapshot r : result) {
            VMSnapshotResponse vmSnapshotResponse = _responseGenerator
                    .createVMSnapshotResponse(r);
            vmSnapshotResponse.setObjectName("vmSnapshot");
            snapshotResponses.add(vmSnapshotResponse);
        }
        response.setResponses(snapshotResponses);
        response.setResponseName(getCommandName());
        this.setResponseObject(response);
    }

    @Override
    public String getCommandName() {
        return s_name;
    }

}
