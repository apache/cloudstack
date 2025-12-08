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

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.solidfire.ApiPathForVolumeResponse;
import org.apache.cloudstack.util.solidfire.SolidFireIntegrationTestUtil;

@APICommand(name = "getPathForVolume", responseObject = ApiPathForVolumeResponse.class, description = "Get the path associated with the provided volume UUID",
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class GetPathForVolumeCmd extends BaseCmd {

    @Parameter(name = ApiConstants.VOLUME_ID, type = CommandType.STRING, description = "CloudStack Volume UUID", required = true)
    private String _volumeUuid;

    @Inject private SolidFireIntegrationTestUtil _util;

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public long getEntityOwnerId() {
        return _util.getAccountIdForVolumeUuid(_volumeUuid);
    }

    @Override
    public void execute() {
        logger.info("'GetPathForVolumeIdCmd.execute' method invoked");

        String pathForVolume = _util.getPathForVolumeUuid(_volumeUuid);

        ApiPathForVolumeResponse response = new ApiPathForVolumeResponse(pathForVolume);

        response.setResponseName(getCommandName());
        response.setObjectName("apipathforvolume");

        setResponseObject(response);
    }
}
