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
package org.apache.cloudstack.api.command.admin.network;

import java.util.ArrayList;
import java.util.List;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.response.IsolationMethodResponse;
import org.apache.cloudstack.api.response.ListResponse;

import com.cloud.network.Networks;

@APICommand(name = "listNetworkIsolationMethods",
            description = "Lists supported methods of network isolation",
            responseObject = IsolationMethodResponse.class,
            since = "4.2.0",
            requestHasSensitiveInfo = false,
            responseHasSensitiveInfo = false)
public class ListNetworkIsolationMethodsCmd extends BaseListCmd {


    @Override
    public void execute() {
        Networks.IsolationType[] methods = _ntwkModel.listNetworkIsolationMethods();

        ListResponse<IsolationMethodResponse> response = new ListResponse<IsolationMethodResponse>();
        List<IsolationMethodResponse> isolationResponses = new ArrayList<IsolationMethodResponse>();
        if (methods != null) {
            for (Networks.IsolationType method : methods) {
                IsolationMethodResponse isolationMethod = _responseGenerator.createIsolationMethodResponse(method);
                isolationResponses.add(isolationMethod);
            }
        }
        response.setResponses(isolationResponses, isolationResponses.size());
        response.setResponseName(getCommandName());
        this.setResponseObject(response);

    }

    }
