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
package com.cloud.api.query.dao;

import java.util.EnumSet;

import org.apache.cloudstack.api.ApiConstants.DomainDetails;
import org.apache.cloudstack.api.ResponseObject.ResponseView;
import org.apache.cloudstack.api.response.AccountResponse;
import org.apache.cloudstack.api.response.ResourceLimitAndCountResponse;

import com.cloud.api.query.vo.AccountJoinVO;
import com.cloud.user.Account;
import com.cloud.utils.db.GenericDao;

public interface AccountJoinDao extends GenericDao<AccountJoinVO, Long> {

    AccountResponse newAccountResponse(ResponseView view, EnumSet<DomainDetails> details, AccountJoinVO vol);

    AccountJoinVO newAccountView(Account vol);

    void setResourceLimits(AccountJoinVO account, boolean accountIsAdmin, ResourceLimitAndCountResponse response);

}
