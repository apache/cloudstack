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
package org.apache.cloudstack.api.command.user.vpc;

import org.apache.cloudstack.api.ApiCommandResourceType;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.acl.SecurityChecker.AccessType;
import org.apache.cloudstack.api.ACL;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.BaseAsyncCustomIdCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject.ResponseView;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.user.UserCmd;
import org.apache.cloudstack.api.response.VpcResponse;

import com.cloud.event.EventTypes;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.vpc.Vpc;
import com.cloud.user.Account;

@APICommand(name = "updateVPC", description = "Updates a VPC", responseObject = VpcResponse.class, responseView = ResponseView.Restricted, entityType = {Vpc.class},
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class UpdateVPCCmd extends BaseAsyncCustomIdCmd implements UserCmd {
    private static final String s_name = "updatevpcresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @ACL(accessType = AccessType.OperateEntry)
    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = VpcResponse.class, required = true, description = "the id of the VPC")
    private Long id;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, description = "the name of the VPC")
    private String vpcName;

    @Parameter(name = ApiConstants.DISPLAY_TEXT, type = CommandType.STRING, description = "the display text of the VPC")
    private String displayText;

    @Parameter(name = ApiConstants.FOR_DISPLAY, type = CommandType.BOOLEAN, description = "an optional field, whether to the display the vpc to the end user or not", since = "4.4", authorized = {RoleType.Admin})
    private Boolean display;

    @Parameter(name = ApiConstants.PUBLIC_MTU, type = CommandType.INTEGER,
            description = "MTU to be configured on the network VR's public facing interfaces", since = "4.18.0")
    private Integer publicMtu;

    @Parameter(name = ApiConstants.SOURCE_NAT_IP,
            type = CommandType.STRING,
            description = "IPV4 address to be assigned to the public interface of the network router. This address must already be acquired for this VPC",
            since = "4.19")
    private String sourceNatIP;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getVpcName() {
        return vpcName;
    }

    public String getDisplayText() {
        return displayText;
    }

    public Long getId() {
        return id;
    }

    public Boolean isDisplayVpc() {
        return display;
    }

    public Integer getPublicMtu() {
        return publicMtu;
    }

    public String getSourceNatIP() {
        return sourceNatIP;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////
    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public long getEntityOwnerId() {
        Vpc vpc = _entityMgr.findById(Vpc.class, getId());
        if (vpc != null) {
            return vpc.getAccountId();
        }

        return Account.ACCOUNT_ID_SYSTEM; // no account info given, parent this command to SYSTEM so ERROR events are tracked
    }

    @Override
    public void execute() {
        try {
            Vpc result = _vpcService.updateVpc(this);
            if (result != null) {
                VpcResponse response = _responseGenerator.createVpcResponse(getResponseView(), result);
                response.setResponseName(getCommandName());
                setResponseObject(response);
            } else {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to update VPC");
            }
        } catch (final ResourceUnavailableException ex) {
            logger.warn("Exception: ", ex);
            throw new ServerApiException(ApiErrorCode.RESOURCE_UNAVAILABLE_ERROR, ex.getMessage());
        } catch (final InsufficientCapacityException ex) {
            logger.info(ex);
            logger.trace(ex);
            throw new ServerApiException(ApiErrorCode.INSUFFICIENT_CAPACITY_ERROR, ex.getMessage());
        }
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_VPC_UPDATE;
    }

    @Override
    public String getEventDescription() {
        return "updating VPC id=" + getId();
    }

    @Override
    public String getSyncObjType() {
        return BaseAsyncCmd.vpcSyncObject;
    }

    @Override
    public Long getSyncObjId() {
        return getId();
    }

    @Override
    public void checkUuid() {
        if (getCustomId() != null) {
            _uuidMgr.checkUuid(getCustomId(), Vpc.class);
        }
    }

    @Override
    public Long getApiResourceId() {
        return getId();
    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.Vpc;
    }
}
