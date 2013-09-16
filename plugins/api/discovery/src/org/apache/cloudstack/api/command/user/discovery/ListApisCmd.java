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
package org.apache.cloudstack.api.command.user.discovery;

import javax.inject.Inject;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ApiDiscoveryResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.discovery.ApiDiscoveryService;

import org.apache.log4j.Logger;

import com.cloud.user.User;

@APICommand(name = "listApis", responseObject = ApiDiscoveryResponse.class, description = "lists all available apis on the server, provided by the Api Discovery plugin", since = "4.1.0")
public class ListApisCmd extends BaseCmd {

    public static final Logger s_logger = Logger.getLogger(ListApisCmd.class.getName());
    private static final String s_name = "listapisresponse";

    @Inject
    ApiDiscoveryService _apiDiscoveryService;

    @Parameter(name=ApiConstants.NAME, type=CommandType.STRING, description="API name")
    private String name;

    @Override
    public void execute() throws ServerApiException {
        if (_apiDiscoveryService != null) {
            User user = CallContext.current().getCallingUser();
            ListResponse<ApiDiscoveryResponse> response = (ListResponse<ApiDiscoveryResponse>) _apiDiscoveryService.listApis(user, name);
            if (response == null) {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Api Discovery plugin was unable to find an api by that name or process any apis");
            }
            response.setResponseName(getCommandName());
            this.setResponseObject(response);
        }
    }

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public long getEntityOwnerId() {
        // no owner is needed for list command
        return 0;
    }
}
