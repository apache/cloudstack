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
package org.apache.cloudstack.api.command.admin.vmsnapshot;

import java.util.logging.Logger;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.ResponseObject.ResponseView;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.user.vmsnapshot.RevertToVMSnapshotCmd;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.cloudstack.context.CallContext;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.uservm.UserVm;

@APICommand(name = "revertToVMSnapshot", description = "Revert VM from a vmsnapshot.", responseObject = UserVmResponse.class, since = "4.2.0", responseView = ResponseView.Full,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = true)
public class RevertToVMSnapshotCmdByAdmin extends RevertToVMSnapshotCmd {
    public static final Logger s_logger = Logger
            .getLogger(RevertToVMSnapshotCmdByAdmin.class.getName());


    @Override
    public void execute() throws  ResourceUnavailableException, InsufficientCapacityException, ResourceAllocationException, ConcurrentOperationException {
        CallContext.current().setEventDetails(
                "vmsnapshot id: " + getVmSnapShotId());
        UserVm result = _vmSnapshotService.revertToSnapshot(getVmSnapShotId());
        if (result != null) {
            UserVmResponse response = _responseGenerator.createUserVmResponse(ResponseView.Full,
                    "virtualmachine", result).get(0);
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR,"Failed to revert VM snapshot");
        }
    }


}
