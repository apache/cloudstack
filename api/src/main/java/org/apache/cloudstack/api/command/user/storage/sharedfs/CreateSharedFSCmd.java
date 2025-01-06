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
package org.apache.cloudstack.api.command.user.storage.sharedfs;

import javax.inject.Inject;

import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.user.Account;
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
import org.apache.cloudstack.api.response.SharedFSResponse;
import org.apache.cloudstack.api.response.NetworkResponse;
import org.apache.cloudstack.api.response.ProjectResponse;
import org.apache.cloudstack.api.response.ServiceOfferingResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.storage.sharedfs.SharedFS;
import org.apache.cloudstack.storage.sharedfs.SharedFSProvider;
import org.apache.cloudstack.storage.sharedfs.SharedFSService;

@APICommand(name = "createSharedFileSystem",
        responseObject= SharedFSResponse.class,
        description = "Create a new Shared File System of specified size and disk offering, attached to the given network",
        responseView = ResponseObject.ResponseView.Restricted,
        entityType = SharedFS.class,
        requestHasSensitiveInfo = false,
        since = "4.20.0",
        authorized = {RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User})
public class CreateSharedFSCmd extends BaseAsyncCreateCmd implements UserCmd {

    @Inject
    SharedFSService sharedFSService;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.NAME,
            type = CommandType.STRING,
            required = true,
            description = "the name of the shared filesystem.")
    private String name;

    @Parameter(name = ApiConstants.ACCOUNT,
            type = BaseCmd.CommandType.STRING,
            description = "the account associated with the shared filesystem. Must be used with the domainId parameter.")
    private String accountName;

    @Parameter(name = ApiConstants.DOMAIN_ID,
            type = CommandType.UUID,
            entityType = DomainResponse.class,
            description = "the domain ID associated with the shared filesystem. If used with the account parameter"
                    + " returns the shared filesystem associated with the account for the specified domain." +
                    "If account is NOT provided then the shared filesystem will be assigned to the caller account and domain.")
    private Long domainId;

    @Parameter(name = ApiConstants.PROJECT_ID,
            type = CommandType.UUID,
            entityType = ProjectResponse.class,
            description = "the project associated with the shared filesystem. Mutually exclusive with account parameter")
    private Long projectId;

    @Parameter(name = ApiConstants.DESCRIPTION,
            type = CommandType.STRING,
            description = "the description for the shared filesystem.")
    private String description;

    @Parameter(name = ApiConstants.SIZE,
            type = CommandType.LONG,
            description = "the size of the shared filesystem in GiB")
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
            description = "the disk offering to use for the underlying storage. This will define the size and other specifications like encryption and qos for the shared filesystem.")
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
            description = "the service offering to use for the shared filesystem instance hosting the data. The offering should be HA enabled and the cpu count and memory size should be greater than equal to sharedfsvm.min.cpu.count and sharedfsvm.min.ram.size respectively")
    private Long serviceOfferingId;

    @Parameter(name = ApiConstants.FILESYSTEM,
            type = CommandType.STRING,
            required = true,
            description = "the filesystem format (XFS / EXT4) which will be installed on the shared filesystem.")
    private String fsFormat;

    @Parameter(name = ApiConstants.PROVIDER,
            type = CommandType.STRING,
            description = "the provider to be used for the shared filesystem. The list of providers can be fetched by using the listSharedFileSystemProviders API.")
    private String sharedFSProviderName;

    @Parameter(name = ApiConstants.NETWORK_ID,
            type = CommandType.UUID,
            required = true,
            entityType = NetworkResponse.class,
            description = "network to attach the shared filesystem to")
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

    public String getSharedFSProviderName() {
        if (sharedFSProviderName != null) {
            return sharedFSProviderName;
        } else {
            return SharedFSProvider.SharedFSProviderType.SHAREDFSVM.toString();
        }
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.SharedFS;
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
        return EventTypes.EVENT_SHAREDFS_CREATE;
    }

    @Override
    public String getEventDescription() {
        return "Creating shared filesystem " + name;
    }

    private String getCreateExceptionMsg(Exception ex) {
        return "Shared FileSystem create failed with exception" + ex.getMessage();
    }

    private String getStartExceptionMsg(Exception ex) {
        return "Shared FileSystem start failed with exception: " + ex.getMessage();
    }

    public void create() {
        SharedFS sharedFS;
        sharedFS = sharedFSService.allocSharedFS(this);
        if (sharedFS != null) {
            setEntityId(sharedFS.getId());
            setEntityUuid(sharedFS.getUuid());
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to create Shared FileSystem");
        }
    }

    @Override
    public void execute() {
        SharedFS sharedFS;
        try {
            sharedFS = sharedFSService.deploySharedFS(this);
        } catch (ResourceUnavailableException ex) {
            logger.warn("Shared FileSystem start exception: ", ex);
            throw new ServerApiException(ApiErrorCode.RESOURCE_UNAVAILABLE_ERROR, getStartExceptionMsg(ex));
        } catch (ConcurrentOperationException ex) {
            logger.warn("Shared FileSystem start exception: ", ex);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, getStartExceptionMsg(ex));
        } catch (InsufficientCapacityException ex) {
            logger.warn("Shared FileSystem start exception: ", ex);
            throw new ServerApiException(ApiErrorCode.INSUFFICIENT_CAPACITY_ERROR, getStartExceptionMsg(ex));
        } catch (ResourceAllocationException ex) {
            logger.warn("Shared FileSystem start exception: ", ex);
            throw new ServerApiException(ApiErrorCode.RESOURCE_UNAVAILABLE_ERROR, ex.getMessage());
        } catch (OperationTimedoutException ex) {
            throw new CloudRuntimeException("Shared FileSystem start timed out due to " + ex.getMessage());
        }

        if (sharedFS != null) {
            ResponseObject.ResponseView respView = getResponseView();
            Account caller = CallContext.current().getCallingAccount();
            if (_accountService.isRootAdmin(caller.getId())) {
                respView = ResponseObject.ResponseView.Full;
            }
            SharedFSResponse response = _responseGenerator.createSharedFSResponse(respView, sharedFS);
            response.setObjectName(SharedFS.class.getSimpleName().toLowerCase());
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to start Shared FileSystem");
        }
    }
}
