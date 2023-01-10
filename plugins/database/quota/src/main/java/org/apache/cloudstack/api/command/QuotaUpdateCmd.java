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
import org.apache.cloudstack.api.response.QuotaUpdateResponse;
import org.apache.cloudstack.quota.QuotaAlertManager;
import org.apache.cloudstack.quota.QuotaManager;
import org.apache.cloudstack.quota.QuotaStatement;

import java.util.Calendar;

import javax.inject.Inject;

@APICommand(name = "quotaUpdate", responseObject = QuotaUpdateResponse.class, description = "Update quota calculations, alerts and statements", since = "4.7.0", requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class QuotaUpdateCmd extends BaseCmd {



    @Inject
    QuotaManager _manager;
    @Inject
    QuotaStatement _statement;
    @Inject
    QuotaAlertManager _alert;

    public QuotaUpdateCmd() {
        super();
    }

    @Override
    public void execute() {
        _manager.calculateQuotaUsage();
        _statement.sendStatement();
        _alert.checkAndSendQuotaAlertEmails();
        QuotaUpdateResponse response = new QuotaUpdateResponse(Calendar.getInstance());
        response.setResponseName(getCommandName());
        response.setObjectName("quotacredits");
        setResponseObject(response);
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

}
