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
package com.cloud.api.commands;

import org.apache.log4j.Logger;

import com.cloud.api.ApiConstants;
import com.cloud.api.BaseAsyncCmd;
import com.cloud.api.BaseCmd;
import com.cloud.api.IdentityMapper;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.VpcResponse;
import com.cloud.event.EventTypes;
import com.cloud.network.vpc.Vpc;
import com.cloud.user.Account;

@Implementation(description="Updates a VPC", responseObject=VpcResponse.class)
public class UpdateVPCCmd extends BaseAsyncCmd{
    public static final Logger s_logger = Logger.getLogger(UpdateVPCCmd.class.getName());
    private static final String _name = "updatevpcresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    
    @IdentityMapper(entityTableName="vpc")
    @Parameter(name=ApiConstants.ID, type=CommandType.LONG, description="the id of the VPC")
    private Long id;

    @Parameter(name=ApiConstants.NAME, type=CommandType.STRING, description="the name of the VPC")
    private String vpcName;
    
    @Parameter(name=ApiConstants.DISPLAY_TEXT, type=CommandType.STRING, description="the display text of the VPC")
    private String displayText;


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
    

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////
    @Override
    public String getCommandName() {
        return _name;
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
    public void execute(){
        Vpc result = _vpcService.updateVpc(getId(), getVpcName(), getDisplayText());
        if (result != null) {
            VpcResponse response = _responseGenerator.createVpcResponse(result);
            response.setResponseName(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to update VPC");
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
}
