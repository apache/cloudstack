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
package org.apache.cloudstack.api.command.admin.config;

import java.util.ArrayList;
import java.util.List;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.ConfigurationGroupResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.config.ConfigurationGroup;

import com.cloud.utils.Pair;

@APICommand(name = ListCfgGroupsByCmd.APINAME, description = "Lists all configuration groups (primarily used for UI).", responseObject = ConfigurationGroupResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false, since = "4.18.0")
public class ListCfgGroupsByCmd extends BaseListCmd {

    public static final String APINAME = "listConfigurationGroups";

    // ///////////////////////////////////////////////////
    // ////////////// API parameters /////////////////////
    // ///////////////////////////////////////////////////

    @Parameter(name = ApiConstants.GROUP, type = CommandType.STRING, description = "lists configuration group by group name")
    private String groupName;

    // ///////////////////////////////////////////////////
    // ///////////////// Accessors ///////////////////////
    // ///////////////////////////////////////////////////

    public String getGroupName() {
        return groupName;
    }

    // ///////////////////////////////////////////////////
    // ///////////// API Implementation///////////////////
    // ///////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return APINAME.toLowerCase() + BaseCmd.RESPONSE_SUFFIX;
    }

    @Override
    public void execute() {
        Pair<List<? extends ConfigurationGroup>, Integer> result = _mgr.listConfigurationGroups(this);
        ListResponse<ConfigurationGroupResponse> response = new ListResponse<>();
        List<ConfigurationGroupResponse> configGroupResponses = new ArrayList<>();
        for (ConfigurationGroup cfgGroup : result.first()) {
            ConfigurationGroupResponse cfgGroupResponse = _responseGenerator.createConfigurationGroupResponse(cfgGroup);
            configGroupResponses.add(cfgGroupResponse);
        }

        response.setResponses(configGroupResponses, result.second());
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }
}
