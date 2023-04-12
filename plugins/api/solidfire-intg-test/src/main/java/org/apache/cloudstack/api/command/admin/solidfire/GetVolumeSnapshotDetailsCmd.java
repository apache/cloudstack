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
package org.apache.cloudstack.api.command.admin.solidfire;

import java.util.List;

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.solidfire.ApiVolumeSnapshotDetailsResponse;
import org.apache.cloudstack.api.response.solidfire.ApiVolumeiScsiNameResponse;
import org.apache.cloudstack.util.solidfire.SolidFireIntegrationTestUtil;

@APICommand(name = "getVolumeSnapshotDetails", responseObject = ApiVolumeiScsiNameResponse.class, description = "Get Volume Snapshot Details",
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)

public class GetVolumeSnapshotDetailsCmd extends BaseCmd {
    private static final Logger LOGGER = Logger.getLogger(GetVolumeSnapshotDetailsCmd.class.getName());

    @Parameter(name = ApiConstants.SNAPSHOT_ID, type = CommandType.STRING, description = "CloudStack Snapshot UUID", required = true)
    private String snapshotUuid;

    @Inject private SolidFireIntegrationTestUtil util;

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public long getEntityOwnerId() {
        return util.getAccountIdForSnapshotUuid(snapshotUuid);
    }

    @Override
    public void execute() {
        LOGGER.info("'" + GetVolumeSnapshotDetailsCmd.class.getSimpleName() + ".execute' method invoked");

        List<ApiVolumeSnapshotDetailsResponse> responses = util.getSnapshotDetails(snapshotUuid);

        ListResponse<ApiVolumeSnapshotDetailsResponse> listReponse = new ListResponse<>();

        listReponse.setResponses(responses);
        listReponse.setResponseName(getCommandName());
        listReponse.setObjectName("apivolumesnapshotdetails");

        this.setResponseObject(listReponse);
    }
}
