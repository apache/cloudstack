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
import org.apache.cloudstack.api.response.FileShareResponse;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.storage.fileshare.FileShare;
import org.apache.cloudstack.storage.fileshare.FileShareService;

import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.user.Account;
import com.cloud.utils.exception.CloudRuntimeException;

@APICommand(name = "restartFileShare",
        responseObject= SuccessResponse.class,
        description = "Restart a File Share",
        responseView = ResponseObject.ResponseView.Restricted,
        entityType = FileShare.class,
        requestHasSensitiveInfo = false,
        since = "4.20.0",
        authorized = {RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User})
public class RestartFileShareCmd extends BaseAsyncCmd implements UserCmd {

    @Inject
    FileShareService fileShareService;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID,
            type = CommandType.UUID,
            required = true,
            entityType = FileShareResponse.class,
            description = "the ID of the file share")
    private Long id;

    @Parameter(name = ApiConstants.CLEANUP,
            type = CommandType.BOOLEAN,
            description = "is cleanup required")
    private boolean cleanup;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public Boolean getCleanup() {
        return cleanup;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getEventType() {
        return EventTypes.EVENT_FILESHARE_RESTART;
    }

    @Override
    public String getEventDescription() {
        return "Restarting fileshare " + id;
    }

    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccount().getId();
    }

    private String getRestartExceptionMsg(Exception ex) {
        return "File share restart failed with exception" + ex.getMessage();
    }

    @Override
    public void execute() {
        FileShare fileShare;
        try {
            fileShare = fileShareService.restartFileShare(this.getId(), this.getCleanup());
        } catch (ResourceUnavailableException ex) {
            logger.warn("File share restart exception: ", ex);
            throw new ServerApiException(ApiErrorCode.RESOURCE_UNAVAILABLE_ERROR, getRestartExceptionMsg(ex));
        } catch (ConcurrentOperationException ex) {
            logger.warn("File share restart exception: ", ex);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, getRestartExceptionMsg(ex));
        } catch (InsufficientCapacityException ex) {
            logger.warn("File share restart exception: ", ex);
            throw new ServerApiException(ApiErrorCode.INSUFFICIENT_CAPACITY_ERROR, getRestartExceptionMsg(ex));
        } catch (ResourceAllocationException ex) {
            logger.warn("File share restart exception: ", ex);
            throw new ServerApiException(ApiErrorCode.RESOURCE_UNAVAILABLE_ERROR, ex.getMessage());
        } catch (OperationTimedoutException ex) {
            logger.warn("File share restart exception: ", ex);
            throw new CloudRuntimeException("File share start timed out due to " + ex.getMessage());
        }

        if (fileShare != null) {
            ResponseObject.ResponseView respView = getResponseView();
            Account caller = CallContext.current().getCallingAccount();
            if (_accountService.isRootAdmin(caller.getId())) {
                respView = ResponseObject.ResponseView.Full;
            }
            FileShareResponse response = _responseGenerator.createFileShareResponse(respView, fileShare);
            response.setObjectName(FileShare.class.getSimpleName().toLowerCase());
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to restart file share");
        }
    }
}
