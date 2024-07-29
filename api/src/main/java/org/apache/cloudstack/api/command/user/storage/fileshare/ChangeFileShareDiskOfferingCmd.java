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
package org.apache.cloudstack.api.command.user.storage.fileshare;

import javax.inject.Inject;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.user.UserCmd;
import org.apache.cloudstack.api.response.DiskOfferingResponse;
import org.apache.cloudstack.api.response.FileShareResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.storage.fileshare.FileShare;
import org.apache.cloudstack.storage.fileshare.FileShareService;

import com.cloud.event.EventTypes;
import com.cloud.user.Account;
import com.cloud.user.AccountService;

@APICommand(name = "changeFileShareDiskOffering",
        responseObject= FileShareResponse.class,
        description = "Change Disk offering of a File Share.. ",
        responseView = ResponseObject.ResponseView.Restricted,
        entityType = FileShare.class,
        requestHasSensitiveInfo = false,
        since = "4.20.0",
        authorized = {RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User})
public class ChangeFileShareDiskOfferingCmd extends BaseAsyncCmd implements UserCmd {

    @Inject
    FileShareService fileShareService;

    @Inject
    protected AccountService accountService;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID,
            type = CommandType.UUID,
            required = true,
            entityType = FileShareResponse.class,
            description = "the ID of the file share")
    private Long id;

    @Parameter(name = ApiConstants.DISK_OFFERING_ID,
            type = CommandType.UUID,
            entityType = DiskOfferingResponse.class,
            description = "the disk offering to use for the underlying storage.")
    private Long diskOfferingId;

    @Parameter(name = ApiConstants.SIZE,
            type = CommandType.LONG,
            required = true,
            description = "the size of the file share in GiB")
    private Long size;

    @Parameter(name = ApiConstants.MIN_IOPS,
            type = CommandType.LONG,
            description = "min iops")
    private Long minIops;

    @Parameter(name = ApiConstants.MAX_IOPS,
            type = CommandType.LONG,
            description = "max iops")
    private Long maxIops;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public Long getSize() {
       return size;
    }

    public Long getDiskOfferingId() {
        return diskOfferingId;
    }

    public Long getMinIops() {
        return minIops;
    }

    public Long getMaxIops() {
        return maxIops;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getEventType() {
        return EventTypes.EVENT_FILESHARE_UPDATE;
    }

    @Override
    public String getEventDescription() {
        return "Changing disk offering for the fileshare " + id;
    }

    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccount().getId();
    }

    @Override
    public void execute() {
        FileShare fileShare = fileShareService.changeFileShareDiskOffering(this);
        if (fileShare != null) {
            ResponseObject.ResponseView respView = getResponseView();
            Account caller = CallContext.current().getCallingAccount();
            if (accountService.isRootAdmin(caller.getId())) {
                respView = ResponseObject.ResponseView.Full;
            }
            FileShareResponse response = _responseGenerator.createFileShareResponse(respView, fileShare);
            response.setObjectName(FileShare.class.getSimpleName().toLowerCase());
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to change disk offering for the file share");
        }
    }
}