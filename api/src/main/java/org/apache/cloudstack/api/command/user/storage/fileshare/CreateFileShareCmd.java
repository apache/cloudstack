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

import com.cloud.event.EventTypes;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.user.Account;
import com.cloud.user.AccountService;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCreateCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.user.UserCmd;
import org.apache.cloudstack.api.response.DiskOfferingResponse;
import org.apache.cloudstack.api.response.FileShareResponse;
import org.apache.cloudstack.api.response.NetworkResponse;
import org.apache.cloudstack.api.response.ServiceOfferingResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.storage.fileshare.FileShare;
import org.apache.cloudstack.storage.fileshare.FileShareService;

@APICommand(name = "createFileShare",
        responseObject= FileShareResponse.class,
        description = "Creates a new file share of specified size and disk offering and attached to the given guest network",
        responseView = ResponseObject.ResponseView.Restricted,
        entityType = FileShare.class,
        requestHasSensitiveInfo = false,
        since = "4.20.0",
        authorized = {RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User})
public class CreateFileShareCmd extends BaseAsyncCreateCmd implements UserCmd {

    @Inject
    FileShareService fileShareService;

    @Inject
    protected AccountService accountService;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.NAME,
            type = CommandType.STRING,
            required = true,
            description = "the name of the file share.")
    private String name;

    @Parameter(name = ApiConstants.DESCRIPTION,
            type = CommandType.STRING,
            description = "the description for the file share.")
    private String description;

    @Parameter(name = ApiConstants.SIZE,
            type = CommandType.LONG,
            description = "the size of the file share in GiB")
    private Long size;

    @Parameter(name = ApiConstants.ZONE_ID,
            type = CommandType.UUID,
            required = true,
            entityType = ZoneResponse.class,
            description = "the zone id.")
    private Long zoneId;

    @Parameter(name = ApiConstants.DISK_OFFERING_ID,
            type = CommandType.UUID,
            entityType = DiskOfferingResponse.class,
            description = "the disk offering to use for the underlying storage.")
    private Long diskOfferingId;

    @Parameter(name = ApiConstants.SERVICE_OFFERING_ID,
            type = CommandType.UUID,
            required = true,
            entityType = ServiceOfferingResponse.class,
            description = "the disk offering to use for the underlying storage.")
    private Long serviceOfferingId;

    @Parameter(name = ApiConstants.MOUNT_OPTIONS,
            type = CommandType.STRING,
            description = "the comma separated list of mount options to use for mounting this file share.")
    private String mountOptions;

    @Parameter(name = ApiConstants.FORMAT,
            type = CommandType.STRING,
            description = "the filesystem format which will be installed on the file share.")
    private String fsFormat;

    @Parameter(name = ApiConstants.PROVIDER,
            type = CommandType.STRING,
            required = true,
            description = "the provider to be used for the file share.")
    private String fileShareProviderName;

    @Parameter(name = ApiConstants.NETWORK_ID,
            type = CommandType.UUID,
            required = true,
            entityType = NetworkResponse.class,
            description = "list of network id to attach file share to")
    private Long networkId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Long getSize() {
        return size;
    }

    public Long getZoneId() {
        return zoneId;
    }

    public Long getDiskOfferingId() {
        return diskOfferingId;
    }

    public Long getServiceOfferingId() {
        return serviceOfferingId;
    }

    public String getMountOptions() {
        return mountOptions;
    }

    public String getFsFormat() {
        return fsFormat;
    }

    public Long getNetworkId() {
        return networkId;
    }

    public String getFileShareProviderName() {
        return fileShareProviderName;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.FileShare;
    }

    @Override
    public Long getApiResourceId() {
        return this.getEntityId();
    }
    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccount().getId();
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_FILESHARE_CREATE;
    }

    @Override
    public String getEventDescription() {
        return "Creating fileshare " + name;
    }

    public void create() throws ResourceAllocationException {
        FileShare fileShare = fileShareService.allocFileShare(this);
        if (fileShare != null) {
            setEntityId(fileShare.getId());
            setEntityUuid(fileShare.getUuid());
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to allocate Fileshare");
        }
    }

    @Override
    public void execute() {
        FileShare fileShare = fileShareService.deployFileShare(this.getEntityId(), this.getNetworkId(), this.getDiskOfferingId(), this.getSize());
        if (fileShare == null) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to deploy file share");
        }

        fileShare = fileShareService.startFileShare(this.getEntityId());
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
            //revert changes?
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to initialize file share");
        }
    }
}