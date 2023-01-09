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

import com.cloud.user.Account;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.response.QuotaEnabledResponse;
import org.apache.cloudstack.quota.QuotaService;
import org.apache.log4j.Logger;


import javax.inject.Inject;

@APICommand(name = "quotaIsEnabled", responseObject = QuotaEnabledResponse.class, description = "Return true if the plugin is enabled", since = "4.7.0", requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class QuotaEnabledCmd extends BaseCmd {

    public static final Logger s_logger = Logger.getLogger(QuotaEnabledCmd.class);


    @Inject
    QuotaService _quotaService;

    public QuotaEnabledCmd() {
        super();
    }

    @Override
    public void execute() {
        Boolean isEnabled = _quotaService.isQuotaServiceEnabled();
        QuotaEnabledResponse response = new QuotaEnabledResponse(isEnabled);
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

}
