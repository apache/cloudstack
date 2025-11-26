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
package org.apache.cloudstack.api.command.admin.offering;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ServiceOfferingCategoryResponse;

import com.cloud.offering.ServiceOfferingCategory;
import com.cloud.user.Account;

@APICommand(name = "createServiceOfferingCategory",
        description = "Creates a service offering category.",
        responseObject = ServiceOfferingCategoryResponse.class,
        since = "4.23.0",
        requestHasSensitiveInfo = false,
        responseHasSensitiveInfo = false)
public class CreateServiceOfferingCategoryCmd extends BaseCmd {

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.NAME,
            type = CommandType.STRING,
            required = true,
            description = "the name of the service offering category")
    private String name;

    @Parameter(name = ApiConstants.SORT_KEY,
            type = CommandType.INTEGER,
            description = "sort key of the service offering category, default is 0")
    private Integer sortKey;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getName() {
        return name;
    }

    public Integer getSortKey() {
        return sortKey;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() {
        ServiceOfferingCategory result = _configService.createServiceOfferingCategory(this);
        if (result != null) {
            ServiceOfferingCategoryResponse response = _responseGenerator.createServiceOfferingCategoryResponse(result);
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to create service offering category");
        }
    }

    @Override
    public long getEntityOwnerId() {
        return Account.Type.ADMIN.ordinal();
    }
}
