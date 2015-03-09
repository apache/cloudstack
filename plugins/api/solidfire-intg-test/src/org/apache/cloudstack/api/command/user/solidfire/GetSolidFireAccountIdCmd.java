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
package org.apache.cloudstack.api.command.user.solidfire;

import com.cloud.user.Account;
import com.cloud.user.dao.AccountDao;

import javax.inject.Inject;

import org.apache.log4j.Logger;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.ApiSolidFireAccountIdResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.solidfire.ApiSolidFireService;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;

@APICommand(name = "getSolidFireAccountId", responseObject = ApiSolidFireAccountIdResponse.class, description = "Get SolidFire Account ID",
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class GetSolidFireAccountIdCmd extends BaseCmd {
    private static final Logger s_logger = Logger.getLogger(GetSolidFireAccountIdCmd.class.getName());
    private static final String s_name = "getsolidfireaccountidresponse";

    @Parameter(name = ApiConstants.ACCOUNT_ID, type = CommandType.STRING, description = "CloudStack Account UUID", required = true)
    private String accountUuid;
    @Parameter(name = ApiConstants.STORAGE_ID, type = CommandType.STRING, description = "Storage Pool UUID", required = true)
    private String storagePoolUuid;

    @Inject private ApiSolidFireService _apiSolidFireService;
    @Inject private AccountDao _accountDao;
    @Inject private PrimaryDataStoreDao _storagePoolDao;

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public long getEntityOwnerId() {
        Account account = CallContext.current().getCallingAccount();

        if (account != null) {
            return account.getId();
        }

        return Account.ACCOUNT_ID_SYSTEM; // no account info given, parent this command to SYSTEM so ERROR events are tracked
    }

    @Override
    public void execute() {
        Account account = _accountDao.findByUuid(accountUuid);
        StoragePoolVO storagePool = _storagePoolDao.findByUuid(storagePoolUuid);

        ApiSolidFireAccountIdResponse response = _apiSolidFireService.getSolidFireAccountId(account.getId(), storagePool.getId());

        response.setResponseName(getCommandName());
        response.setObjectName("apisolidfireaccountid");

        this.setResponseObject(response);
    }
}
