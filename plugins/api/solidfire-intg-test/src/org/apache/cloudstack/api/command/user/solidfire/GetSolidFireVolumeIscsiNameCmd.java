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
package org.apache.cloudstack.api.command.user.solidfire;

import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.Volume;
import com.cloud.user.Account;

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.ApiSolidFireVolumeIscsiNameResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.solidfire.ApiSolidFireService;

@APICommand(name = "getSolidFireVolumeIscsiName", responseObject = ApiSolidFireVolumeIscsiNameResponse.class, description = "Get SolidFire Volume's Iscsi Name",
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)

public class GetSolidFireVolumeIscsiNameCmd extends BaseCmd {
    private static final Logger s_logger = Logger.getLogger(GetSolidFireVolumeIscsiNameCmd.class.getName());
    private static final String s_name = "getsolidfirevolumeiscsinameresponse";

    @Parameter(name = ApiConstants.VOLUME_ID, type = CommandType.STRING, description = "CloudStack Volume UUID", required = true)
    private String volumeUuid;

    @Inject private ApiSolidFireService _apiSolidFireService;
    @Inject private VolumeDao _volumeDao;

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public long getEntityOwnerId() {
        Account account = CallContext.current().getCallingAccount();

        if (account != null) {
            return account.getId();
        }

        return Account.ACCOUNT_ID_SYSTEM; // no account info given, parent this command to SYSTEM so ERROR events are tracked
    }

    @Override
    public void execute() {
        Volume volume = _volumeDao.findByUuid(volumeUuid);

        ApiSolidFireVolumeIscsiNameResponse response = _apiSolidFireService.getSolidFireVolumeIscsiName(volume);

        response.setResponseName(getCommandName());
        response.setObjectName("apisolidfirevolumeiscsiname");

        this.setResponseObject(response);
    }
}