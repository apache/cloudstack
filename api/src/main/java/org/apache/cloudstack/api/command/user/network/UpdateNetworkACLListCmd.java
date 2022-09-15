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
import org.apache.cloudstack.api.BaseAsyncCustomIdCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.NetworkACLResponse;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.log4j.Logger;

import com.cloud.event.EventTypes;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.vpc.NetworkACL;
import com.cloud.user.Account;

@APICommand(name = "updateNetworkACLList", description = "Updates network ACL list", responseObject = SuccessResponse.class, since = "4.4", requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class UpdateNetworkACLListCmd extends BaseAsyncCustomIdCmd {
    public static final Logger s_logger = Logger.getLogger(UpdateNetworkACLListCmd.class.getName());
    private static final String s_name = "updatenetworkacllistresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = NetworkACLResponse.class, required = true, description = "the ID of the network ACL")
    private Long id;

    @Parameter(name = ApiConstants.FOR_DISPLAY, type = CommandType.BOOLEAN, description = "an optional field, whether to the display the list to the end user or not", since = "4.4", authorized = {
            RoleType.Admin})
    private Boolean display;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, description = "Name of the network ACL list")
    private String name;

    @Parameter(name = ApiConstants.DESCRIPTION, type = CommandType.STRING, description = "Description of the network ACL list")
    private String description;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public Boolean getDisplay() {
        return display;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////
    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_NETWORK_ACL_UPDATE;
    }

    @Override
    public String getEventDescription() {
        return ("Updating network ACL list ID=" + id);
    }

    @Override
    public long getEntityOwnerId() {
        Account caller = CallContext.current().getCallingAccount();
        return caller.getAccountId();
    }

    @Override
    public void execute() throws ResourceUnavailableException {
        NetworkACL acl = _networkACLService.updateNetworkACL(this);
        NetworkACLResponse aclResponse = _responseGenerator.createNetworkACLResponse(acl);
        setResponseObject(aclResponse);
        aclResponse.setResponseName(getCommandName());
    }

    @Override
    public void checkUuid() {
        if (this.getCustomId() != null) {
            _uuidMgr.checkUuid(this.getCustomId(), NetworkACL.class);
        }
    }

    public String getDescription() {
        return description;
    }

    public String getName() {
        return name;
    }

    @Override
    public Long getApiResourceId() {
        return id;
    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.NetworkAcl;
    }
}
