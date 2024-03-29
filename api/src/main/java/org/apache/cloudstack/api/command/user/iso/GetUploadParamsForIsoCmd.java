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
package org.apache.cloudstack.api.command.user.iso;

import java.net.MalformedURLException;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.AbstractGetUploadParamsCmd;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.GetUploadParamsResponse;
import org.apache.cloudstack.api.response.GuestOSResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.commons.lang3.StringUtils;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;

@APICommand(name = "getUploadParamsForIso",
        description = "upload an existing ISO into the CloudStack cloud.",
        responseObject = GetUploadParamsResponse.class, since = "4.13",
        authorized = {RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User},
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class GetUploadParamsForIsoCmd extends AbstractGetUploadParamsCmd {

    private static final String s_name = "postuploadisoresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.BOOTABLE, type = BaseCmd.CommandType.BOOLEAN, description = "true if this ISO is bootable. If not passed explicitly its assumed to be true")
    private Boolean bootable;

    @Parameter(name = ApiConstants.DISPLAY_TEXT,
            type = BaseCmd.CommandType.STRING,
            description = "the display text of the ISO. This is usually used for display purposes.",
            length = 4096)
    private String displayText;

    @Parameter(name = ApiConstants.IS_FEATURED, type = BaseCmd.CommandType.BOOLEAN, description = "true if you want this ISO to be featured")
    private Boolean featured;

    @Parameter(name = ApiConstants.IS_PUBLIC,
            type = BaseCmd.CommandType.BOOLEAN,
            description = "true if you want to register the ISO to be publicly available to all users, false otherwise.")
    private Boolean publicIso;

    @Parameter(name = ApiConstants.IS_EXTRACTABLE, type = BaseCmd.CommandType.BOOLEAN, description = "true if the ISO or its derivatives are extractable; default is false")
    private Boolean extractable;

    @Parameter(name = ApiConstants.OS_TYPE_ID,
            type = BaseCmd.CommandType.UUID,
            entityType = GuestOSResponse.class,
            description = "the ID of the OS type that best represents the OS of this ISO. If the ISO is bootable this parameter needs to be passed")
    private Long osTypeId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Boolean isBootable() {
        return bootable;
    }

    public String getDisplayText() {
        return StringUtils.isBlank(displayText) ? getName() : displayText;
    }

    public Boolean isFeatured() {
        return featured;
    }

    public Boolean isPublic() {
        return publicIso;
    }

    public Boolean isExtractable() {
        return extractable;
    }

    public Long getOsTypeId() {
        return osTypeId;
    }


    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        validateRequest();
        try {
            GetUploadParamsResponse response = _templateService.registerIsoForPostUpload(this);
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } catch (ResourceAllocationException | MalformedURLException e) {
            logger.error("Exception while registering ISO", e);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Exception while registering ISO: " + e.getMessage());
        }
    }

    private void validateRequest() {
        if (getZoneId() <= 0) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, "Invalid zoneid");
        }
    }

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public long getEntityOwnerId() {
        Long accountId = _accountService.finalyzeAccountId(getAccountName(), getDomainId(), getProjectId(), true);
        if (accountId == null) {
            return CallContext.current().getCallingAccount().getId();
        }
        return accountId;
    }
}
