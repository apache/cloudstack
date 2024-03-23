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
package org.apache.cloudstack.api.command.admin.sioc;

import javax.inject.Inject;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.StoragePoolResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.cloudstack.api.response.sioc.ApiUpdateSiocInfoResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.sioc.SiocManager;

import com.cloud.user.Account;

@APICommand(name = "updateSiocInfo", description = "Update SIOC info", responseObject = ApiUpdateSiocInfoResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false,
        since = "4.11.0",
        authorized = {RoleType.Admin})
public class UpdateSiocInfoCmd extends BaseCmd {


    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ZONE_ID, type = CommandType.UUID, entityType = ZoneResponse.class, description = "Zone ID", required = true)
    private long zoneId;

    @Parameter(name = ApiConstants.STORAGE_ID, type = CommandType.UUID, entityType = StoragePoolResponse.class, description = "Storage Pool ID", required = true)
    private long storagePoolId;

    @Parameter(name = "sharespergb", type = CommandType.INTEGER, description = "Shares per GB", required = true)
    private int sharesPerGB;

    @Parameter(name = "limitiopspergb", type = CommandType.INTEGER, description = "Limit IOPS per GB", required = true)
    private int limitIopsPerGB;

    @Parameter(name = "iopsnotifythreshold", type = CommandType.INTEGER, description = "Notify if IOPS above this value", required = true)
    private int iopsNotifyThreshold;

    @Inject private SiocManager manager;

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

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
        logger.info("'UpdateSiocInfoCmd.execute' method invoked");

        String msg = "Success";

        try {
            manager.updateSiocInfo(zoneId, storagePoolId, sharesPerGB, limitIopsPerGB, iopsNotifyThreshold);
        }
        catch (Exception ex) {
            msg = ex.getMessage();
        }

        ApiUpdateSiocInfoResponse response = new ApiUpdateSiocInfoResponse(msg);

        response.setResponseName(getCommandName());
        response.setObjectName("apiupdatesiocinfo");

        setResponseObject(response);
    }
}
