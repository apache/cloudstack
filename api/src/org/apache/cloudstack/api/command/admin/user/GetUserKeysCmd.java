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

package org.apache.cloudstack.api.command.admin.user;


import com.cloud.user.Account;
import com.cloud.user.User;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.RegisterResponse;
import org.apache.cloudstack.api.response.UserResponse;

import java.util.List;
import java.util.logging.Logger;

@APICommand(name = "getUserKeys",
            description = "This command allows the user to query the seceret and API keys for the account",
            responseObject = RegisterResponse.class,
            requestHasSensitiveInfo = false,
            responseHasSensitiveInfo = true)

public class GetUserKeysCmd extends BaseCmd{

    @Parameter(name= ApiConstants.ID, type = CommandType.UUID, entityType = UserResponse.class, required = true, description = "ID of the user whose keys are required")
    private Long id;

    public static final Logger s_logger = Logger.getLogger(RegisterCmd.class.getName());
    public static final String s_name = "getuserkeysresponse";

    public Long getID(){
        return id;
    }

    public String getCommandName(){
        return s_name;
    }

    public long getEntityOwnerId(){
        User user = _entityMgr.findById(User.class, getID());
        if(user != null){
            return user.getAccountId();
        }
        else return Account.ACCOUNT_ID_SYSTEM;
    }
    public void execute(){
        List<String> keys = _accountService.getKeys(this);
        RegisterResponse response = new RegisterResponse();
        if(keys != null){
            response.setApiKey(keys.get(0));
            response.setSecretKey(keys.get(1));
        }

        response.setObjectName("listkeys");
        response.setResponseName(getCommandName());
        this.setResponseObject(response);
    }
}
