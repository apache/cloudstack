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

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.user.UserCmd;
import org.apache.cloudstack.api.response.SharedFSResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.storage.sharedfs.SharedFS;
import org.apache.cloudstack.storage.sharedfs.SharedFSService;

import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.user.Account;
import com.cloud.utils.exception.CloudRuntimeException;

@APICommand(name = "startSharedFileSystem",
        responseObject= SharedFSResponse.class,
        description = "Start a Shared FileSystem",
        responseView = ResponseObject.ResponseView.Restricted,
        entityType = SharedFS.class,
        requestHasSensitiveInfo = false,
        since = "4.20.0",
        authorized = {RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User})
public class StartSharedFSCmd extends BaseAsyncCmd implements UserCmd {

    @Inject
    SharedFSService sharedFSService;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID,
            type = CommandType.UUID,
            required = true,
            entityType = SharedFSResponse.class,
            description = "the ID of the shared filesystem")
    private Long id;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccount().getId();
    }

    @Override
    public String getEventDescription() {
        return "Starting Shared FileSystem " + id;
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_SHAREDFS_START;
    }

    private String getStartExceptionMsg(Exception ex) {
        return "Shared FileSystem start failed with exception: " + ex.getMessage();
    }

    @Override
    public void execute() {
        SharedFS sharedFS;
        try {
            sharedFS = sharedFSService.startSharedFS(this.getId());
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
            logger.warn("Shared FileSystem start exception: ", ex);
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
