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
package com.cloud.user.dao;

import java.util.Date;
import java.util.List;

import com.cloud.user.Account;
import com.cloud.user.AccountVO;
import com.cloud.user.User;
import com.cloud.utils.Pair;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDao;

public interface AccountDao extends GenericDao<AccountVO, Long> {
    Pair<User, Account> findUserAccountByApiKey(String apiKey);

    List<AccountVO> findAccountsLike(String accountName);

    List<AccountVO> findActiveAccounts(Long maxAccountId, Filter filter);

    List<AccountVO> findRecentlyDeletedAccounts(Long maxAccountId, Date earliestRemovedDate, Filter filter);

    List<AccountVO> findNewAccounts(Long minAccountId, Filter filter);

    List<AccountVO> findCleanupsForRemovedAccounts(Long domainId);

    List<AccountVO> findActiveAccountsForDomain(Long domain);

    void markForCleanup(long accountId);

    List<AccountVO> listAccounts(String accountName, Long domainId, Filter filter);

    List<AccountVO> findCleanupsForDisabledAccounts();

    //return account only in enabled state
    Account findEnabledAccount(String accountName, Long domainId);

    Account findEnabledNonProjectAccount(String accountName, Long domainId);

    //returns account even when it's removed
    Account findAccountIncludingRemoved(String accountName, Long domainId);

    Account findNonProjectAccountIncludingRemoved(String accountName, Long domainId);

    //returns only non-removed account
    Account findActiveAccount(String accountName, Long domainId);

    Account findActiveNonProjectAccount(String accountName, Long domainId);

    List<Long> getAccountIdsForDomains(List<Long> ids);

    /*
    @Desc:   Retrieves the DomainId for a given Account Id
    @Input:  id : Id of the Account
    @Output: DomainId matching for the given Account Id. Returns -1
             in case of no match;
     */
    long getDomainIdForGivenAccountId(long id);

}
