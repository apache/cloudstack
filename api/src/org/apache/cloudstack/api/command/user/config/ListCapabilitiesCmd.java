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
package org.apache.cloudstack.api.command.user.config;

import java.util.Map;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.response.CapabilitiesResponse;
import org.apache.log4j.Logger;

import com.cloud.user.Account;

@APICommand(name = "listCapabilities", description="Lists capabilities", responseObject=CapabilitiesResponse.class)
public class ListCapabilitiesCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(ListCapabilitiesCmd.class.getName());

    private static final String s_name = "listcapabilitiesresponse";

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public void execute(){
        Map<String, Object> capabilities = _mgr.listCapabilities(this);
        CapabilitiesResponse response = new CapabilitiesResponse();
        response.setSecurityGroupsEnabled((Boolean)capabilities.get("securityGroupsEnabled"));
        response.setCloudStackVersion((String)capabilities.get("cloudStackVersion"));
        response.setUserPublicTemplateEnabled((Boolean)capabilities.get("userPublicTemplateEnabled"));
        response.setSupportELB((String)capabilities.get("supportELB"));
        response.setProjectInviteRequired((Boolean)capabilities.get("projectInviteRequired"));
        response.setAllowUsersCreateProjects((Boolean)capabilities.get("allowusercreateprojects"));
        response.setDiskOffMaxSize((Long)capabilities.get("customDiskOffMaxSize"));
        if (capabilities.containsKey("apiLimitInterval")) {
            response.setApiLimitInterval((Integer) capabilities.get("apiLimitInterval"));
        }
        if (capabilities.containsKey("apiLimitMax")) {
            response.setApiLimitMax((Integer) capabilities.get("apiLimitMax"));
        }
        response.setObjectName("capability");
        response.setResponseName(getCommandName());
        this.setResponseObject(response);
    }
}
