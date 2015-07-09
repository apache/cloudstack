//Licensed to the Apache Software Foundation (ASF) under one
//or more contributor license agreements.  See the NOTICE file
//distributed with this work for additional information
//regarding copyright ownership.  The ASF licenses this file
//to you under the Apache License, Version 2.0 (the
//"License"); you may not use this file except in compliance
//with the License.  You may obtain a copy of the License at
//
//http://www.apache.org/licenses/LICENSE-2.0
//
//Unless required by applicable law or agreed to in writing,
//software distributed under the License is distributed on an
//"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
//KIND, either express or implied.  See the License for the
//specific language governing permissions and limitations
//under the License.
package org.apache.cloudstack.api.command;

import org.apache.log4j.Logger;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.QuotaRefreshResponse;

import com.cloud.user.Account;

@APICommand(name = "quotaRefresh", responseObject = QuotaRefreshResponse.class, description = "Refresh the quota for all accounts if enabled", since = "4.2.0", requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class QuotaRefreshCmd extends BaseCmd {

    public static final Logger s_logger = Logger.getLogger(QuotaStatementCmd.class.getName());

    private static final String s_name = "quotarefreshresponse";

    public QuotaRefreshCmd() {
        super();
    }

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public void execute() throws ServerApiException {
        final QuotaRefreshResponse response = new QuotaRefreshResponse("Success");
        setResponseObject(response);
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

}
