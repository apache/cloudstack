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
package org.apache.cloudstack.api.command.user.userdata;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.network.NetworkModel;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.ProjectResponse;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class BaseRegisterUserDataCmd extends BaseCmd {

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, required = true, description = "Name of the user data")
    private String name;

    //Owner information
    @Parameter(name = ApiConstants.ACCOUNT, type = CommandType.STRING, description = "an optional account for the user data. Must be used with domainId.")
    private String accountName;

    @Parameter(name = ApiConstants.DOMAIN_ID,
            type = CommandType.UUID,
            entityType = DomainResponse.class,
            description = "an optional domainId for the user data. If the account parameter is used, domainId must also be used.")
    private Long domainId;

    @Parameter(name = ApiConstants.PROJECT_ID, type = CommandType.UUID, entityType = ProjectResponse.class, description = "an optional project for the user data")
    private Long projectId;

    @Parameter(name = ApiConstants.PARAMS, type = CommandType.STRING, description = "comma separated list of variables declared in user data content")
    private String params;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getName() {
        return name;
    }

    public String getAccountName() {
        return accountName;
    }

    public Long getDomainId() {
        return domainId;
    }

    public Long getProjectId() {
        return projectId;
    }

    public String getParams() {
        checkForVRMetadataFileNames(params);
        return params;
    }

    public void checkForVRMetadataFileNames(String params) {
        if (StringUtils.isNotEmpty(params)) {
            List<String> keyValuePairs = new ArrayList<>(Arrays.asList(params.split(",")));
            keyValuePairs.retainAll(NetworkModel.metadataFileNames);
            if (!keyValuePairs.isEmpty()) {
                throw new InvalidParameterValueException(String.format("Params passed here have a few virtual router metadata file names %s", keyValuePairs));
            }
        }
    }
}
