/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

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
}
