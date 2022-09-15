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
package org.apache.cloudstack.api.command.user.network;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCreateCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.NetworkACLResponse;
import org.apache.cloudstack.api.response.VpcResponse;
import org.apache.log4j.Logger;

import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.vpc.NetworkACL;
import com.cloud.network.vpc.Vpc;
import com.cloud.user.Account;

@APICommand(name = "createNetworkACLList", description = "Creates a network ACL for the given VPC", responseObject = NetworkACLResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class CreateNetworkACLListCmd extends BaseAsyncCreateCmd {
    public static final Logger s_logger = Logger.getLogger(CreateNetworkACLListCmd.class.getName());

    private static final String s_name = "createnetworkacllistresponse";

    // ///////////////////////////////////////////////////
    // ////////////// API parameters /////////////////////
    // ///////////////////////////////////////////////////

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, required = true, description = "Name of the network ACL list")
    private String name;

    @Parameter(name = ApiConstants.DESCRIPTION, type = CommandType.STRING, description = "Description of the network ACL list")
    private String description;

    @Parameter(name = ApiConstants.VPC_ID,
               type = CommandType.UUID,
               required = true,
               entityType = VpcResponse.class,
               description = "ID of the VPC associated with this network ACL list")
    private Long vpcId;

    @Parameter(name = ApiConstants.FOR_DISPLAY, type = CommandType.BOOLEAN, description = "an optional field, whether to the display the list to the end user or not", since = "4.4", authorized = {RoleType.Admin})
    private Boolean display;

    // ///////////////////////////////////////////////////
    // ///////////////// Accessors ///////////////////////
    // ///////////////////////////////////////////////////

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Long getVpcId() {
        return vpcId;
    }

    @Override
    public boolean isDisplay() {
        if (display != null) {
            return display;
        } else {
            return true;
        }
    }

    // ///////////////////////////////////////////////////
    // ///////////// API Implementation///////////////////
    // ///////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public void create() {
        NetworkACL result = _networkACLService.createNetworkACL(getName(), getDescription(), getVpcId(), isDisplay());
        setEntityId(result.getId());
        setEntityUuid(result.getUuid());
    }

    @Override
    public void execute() throws ResourceUnavailableException {
        NetworkACL acl = _networkACLService.getNetworkACL(getEntityId());
        if (acl != null) {
            NetworkACLResponse aclResponse = _responseGenerator.createNetworkACLResponse(acl);
            setResponseObject(aclResponse);
            aclResponse.setResponseName(getCommandName());
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to create network ACL");
        }
    }

    @Override
    public long getEntityOwnerId() {
        Vpc vpc = _entityMgr.findById(Vpc.class, getVpcId());
        if (vpc == null) {
            throw new InvalidParameterValueException("Invalid vpcId is given");
        }

        Account account = _accountService.getAccount(vpc.getAccountId());
        return account.getId();
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_NETWORK_ACL_CREATE;
    }

    @Override
    public String getEventDescription() {
        return "Creating Network ACL with ID: " + getEntityUuid();
    }

    @Override
    public Long getApiResourceId() {
        return getEntityId();
    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.NetworkAcl;
    }
}
