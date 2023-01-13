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
package org.apache.cloudstack.api.command.user.snapshot;

import java.util.ArrayList;
import java.util.List;

import org.apache.cloudstack.acl.RoleType;
import org.apache.log4j.Logger;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.SnapshotPolicyResponse;
import org.apache.cloudstack.api.response.VolumeResponse;

import com.cloud.storage.snapshot.SnapshotPolicy;
import com.cloud.utils.Pair;

@APICommand(name = "listSnapshotPolicies", description = "Lists snapshot policies.", responseObject = SnapshotPolicyResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class ListSnapshotPoliciesCmd extends BaseListCmd {
    public static final Logger s_logger = Logger.getLogger(ListSnapshotPoliciesCmd.class.getName());


    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.VOLUME_ID, type = CommandType.UUID, entityType = VolumeResponse.class, description = "the ID of the disk volume")
    private Long volumeId;

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = SnapshotPolicyResponse.class, description = "the ID of the snapshot policy")
    private Long id;

    @Parameter(name = ApiConstants.FOR_DISPLAY, type = CommandType.BOOLEAN, description = "list resources by display flag; only ROOT admin is eligible to pass this parameter", since = "4.4", authorized = {RoleType.Admin})
    private Boolean display;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getVolumeId() {
        return volumeId;
    }

    @Override
    public boolean isDisplay() {
        if (display != null) {
            return display;
        }
        return true;
    }

    public Long getId() {
        return id;
    }
    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() {
        Pair<List<? extends SnapshotPolicy>, Integer> result = _snapshotService.listPoliciesforVolume(this);
        ListResponse<SnapshotPolicyResponse> response = new ListResponse<SnapshotPolicyResponse>();
        List<SnapshotPolicyResponse> policyResponses = new ArrayList<SnapshotPolicyResponse>();
        for (SnapshotPolicy policy : result.first()) {
            SnapshotPolicyResponse policyResponse = _responseGenerator.createSnapshotPolicyResponse(policy);
            policyResponse.setObjectName("snapshotpolicy");
            policyResponses.add(policyResponse);
        }
        response.setResponses(policyResponses, result.second());
        response.setResponseName(getCommandName());
        this.setResponseObject(response);
    }
}
