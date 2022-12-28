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

import javax.inject.Inject;

import org.apache.log4j.Logger;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.solidfire.ApiSolidFireVolumeSizeResponse;
import org.apache.cloudstack.solidfire.SolidFireIntegrationTestManager;
import org.apache.cloudstack.util.solidfire.SolidFireIntegrationTestUtil;

@APICommand(name = "getSolidFireVolumeSize", responseObject = ApiSolidFireVolumeSizeResponse.class, description = "Get the SF volume size including Hypervisor Snapshot Reserve",
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class GetSolidFireVolumeSizeCmd extends BaseCmd {
    private static final Logger LOGGER = Logger.getLogger(GetSolidFireVolumeSizeCmd.class.getName());

    @Parameter(name = ApiConstants.VOLUME_ID, type = CommandType.STRING, description = "Volume UUID", required = true)
    private String volumeUuid;

    @Inject private SolidFireIntegrationTestManager manager;
    @Inject private SolidFireIntegrationTestUtil util;

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public long getEntityOwnerId() {
        return util.getAccountIdForVolumeUuid(volumeUuid);
    }

    @Override
    public void execute() {
        LOGGER.info("'GetSolidFireVolumeSizeCmd.execute' method invoked");

        long sfVolumeSize = manager.getSolidFireVolumeSize(volumeUuid);

        ApiSolidFireVolumeSizeResponse response = new ApiSolidFireVolumeSizeResponse(sfVolumeSize);

        response.setResponseName(getCommandName());
        response.setObjectName("apisolidfirevolumesize");

        this.setResponseObject(response);
    }
}
