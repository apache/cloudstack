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

package org.apache.cloudstack.api.command.user.autoscale;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.CounterResponse;
import org.apache.cloudstack.api.response.ListResponse;

import com.cloud.network.as.Counter;
import com.cloud.user.Account;

@APICommand(name = "listCounters", description = "List the counters for VM auto scaling", responseObject = CounterResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class ListCountersCmd extends BaseListCmd {
    public static final Logger s_logger = Logger.getLogger(ListCountersCmd.class.getName());
    private static final String s_name = "counterresponse";

    // ///////////////////////////////////////////////////
    // ////////////// API parameters /////////////////////
    // ///////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = CounterResponse.class, description = "ID of the Counter.")
    private Long id;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, description = "Name of the counter.")
    private String name;

    @Parameter(name = ApiConstants.SOURCE, type = CommandType.STRING, description = "Source of the counter.")
    private String source;

    @Parameter(name = ApiConstants.PROVIDER, type = CommandType.STRING, description = "Network provider of the counter.", since = "4.18.0")
    private String provider;

    // ///////////////////////////////////////////////////
    // ///////////// API Implementation///////////////////
    // ///////////////////////////////////////////////////

    @Override
    public void execute() {
        List<? extends Counter> counters = null;
        counters = _autoScaleService.listCounters(this);
        ListResponse<CounterResponse> response = new ListResponse<CounterResponse>();
        List<CounterResponse> ctrResponses = new ArrayList<CounterResponse>();
        for (Counter ctr : counters) {
            CounterResponse ctrResponse = _responseGenerator.createCounterResponse(ctr);
            ctrResponses.add(ctrResponse);
        }

        response.setResponses(ctrResponses);
        response.setResponseName(getCommandName());
        this.setResponseObject(response);
    }

    // /////////////////////////////////////////////////
    // ///////////////// Accessors ///////////////////////
    // ///////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSource() {
        return source;
    }

    public String getProvider() {
        return provider;
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }
}
