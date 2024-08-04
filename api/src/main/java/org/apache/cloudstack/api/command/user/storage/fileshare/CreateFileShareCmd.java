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
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.user.Account;
import com.cloud.user.AccountService;
import com.cloud.utils.exception.CloudRuntimeException;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCreateCmd;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.user.UserCmd;
import org.apache.cloudstack.api.response.DiskOfferingResponse;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.FileShareResponse;
import org.apache.cloudstack.api.response.NetworkResponse;
import org.apache.cloudstack.api.response.ProjectResponse;
import org.apache.cloudstack.api.response.ServiceOfferingResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.storage.fileshare.FileShare;
import org.apache.cloudstack.storage.fileshare.FileShareService;

@APICommand(name = "createFileShare",
        responseObject= FileShareResponse.class,
        description = "Create a new File Share of specified size and disk offering, attached to the given network",
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

    @Parameter(name = ApiConstants.ACCOUNT,
            type = BaseCmd.CommandType.STRING,
            description = "the account associated with the file share. Must be used with the domainId parameter.")
    private String accountName;

    @Parameter(name = ApiConstants.DOMAIN_ID,
            type = CommandType.UUID,
            entityType = DomainResponse.class,
            description = "the domain ID associated with the file share. If used with the account parameter"
                    + " returns the file share associated with the account for the specified domain." +
                    "If account is NOT provided then the file share will be assigned to the caller account and domain.")
    private Long domainId;

    @Parameter(name = ApiConstants.PROJECT_ID,
            type = CommandType.UUID,
            entityType = ProjectResponse.class,
            description = "the project associated with the file share. Mutually exclusive with account parameter")
    private Long projectId;

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
            required = true,
            entityType = DiskOfferingResponse.class,
            description = "the disk offering to use for the underlying storage.")
    private Long diskOfferingId;

    @Parameter(name = ApiConstants.MIN_IOPS,
            type = CommandType.LONG,
            description = "min iops")
    private Long minIops;

    @Parameter(name = ApiConstants.MAX_IOPS,
            type = CommandType.LONG,
            description = "max iops")
    private Long maxIops;

    @Parameter(name = ApiConstants.SERVICE_OFFERING_ID,
            type = CommandType.UUID,
            required = true,
            entityType = ServiceOfferingResponse.class,
            description = "the offering to use for the file share vm.")
    private Long serviceOfferingId;

    @Parameter(name = ApiConstants.FORMAT,
            type = CommandType.STRING,
            description = "the filesystem format (XFS / EXT4) which will be installed on the file share.")
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
            description = "network to attach the file share to")
    private Long networkId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getName() {
        return name;
    }


    public Long getProjectId() {
        return projectId;
    }

    public Long getDomainId() {
        return domainId;
    }

    public String getAccountName() {
        return accountName;
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

    public Long getMaxIops() {
        return maxIops;
    }

    public Long getMinIops() {
        return minIops;
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
        Long accountId = _accountService.finalyzeAccountId(accountName, domainId, projectId, true);
        if (accountId == null) {
            return CallContext.current().getCallingAccount().getId();
        }
        return accountId;
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_FILESHARE_CREATE;
    }

    @Override
    public String getEventDescription() {
        return "Creating fileshare " + name;
    }

    private String getCreateExceptionMsg(Exception ex) {
        return "File share create failed with exception" + ex.getMessage();
    }

    private String getStartExceptionMsg(Exception ex) {
        return "File share start failed with exception: " + ex.getMessage();
    }

    public void create() throws ResourceAllocationException {
        FileShare fileShare;
        try {
            fileShare = fileShareService.createFileShare(this);
            if (fileShare != null) {
                setEntityId(fileShare.getId());
                setEntityUuid(fileShare.getUuid());
            } else {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to create Fileshare");
            }
        } catch (ResourceUnavailableException ex) {
            logger.warn("File share create exception: ", ex);
            throw new ServerApiException(ApiErrorCode.RESOURCE_UNAVAILABLE_ERROR, getCreateExceptionMsg(ex));
        } catch (ResourceAllocationException ex) {
            logger.warn("File share create exception: ", ex);
            throw new ServerApiException(ApiErrorCode.RESOURCE_ALLOCATION_ERROR, getCreateExceptionMsg(ex));
        } catch (ConcurrentOperationException ex) {
            logger.warn("File share create exception: ", ex);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, getCreateExceptionMsg(ex));
        } catch (InsufficientCapacityException ex) {
            logger.warn("File share create exception: ", ex);
            throw new ServerApiException(ApiErrorCode.INSUFFICIENT_CAPACITY_ERROR, getCreateExceptionMsg(ex));
        }
    }

    @Override
    public void execute() {
        FileShare fileShare;
        try {
            fileShare = fileShareService.startFileShare(this.getEntityId());
        } catch (ResourceUnavailableException ex) {
            logger.warn("File share start exception: ", ex);
            throw new ServerApiException(ApiErrorCode.RESOURCE_UNAVAILABLE_ERROR, getStartExceptionMsg(ex));
        } catch (ConcurrentOperationException ex) {
            logger.warn("File share start exception: ", ex);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, getStartExceptionMsg(ex));
        } catch (InsufficientCapacityException ex) {
            logger.warn("File share start exception: ", ex);
            throw new ServerApiException(ApiErrorCode.INSUFFICIENT_CAPACITY_ERROR, getStartExceptionMsg(ex));
        } catch (ResourceAllocationException ex) {
            logger.warn("File share start exception: ", ex);
            throw new ServerApiException(ApiErrorCode.RESOURCE_UNAVAILABLE_ERROR, ex.getMessage());
        } catch (OperationTimedoutException ex) {
            throw new CloudRuntimeException("File share start timed out due to " + ex.getMessage());
        }

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
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to start file share");
        }
    }
}
