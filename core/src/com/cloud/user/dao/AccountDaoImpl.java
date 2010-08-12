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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Date;
import java.util.List;

import javax.ejb.Local;

import org.apache.log4j.Logger;

import com.cloud.user.Account;
import com.cloud.user.AccountVO;
import com.cloud.user.User;
import com.cloud.user.UserVO;
import com.cloud.utils.Pair;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.SearchCriteria.Op;

@Local(value={AccountDao.class})
public class AccountDaoImpl extends GenericDaoBase<AccountVO, Long> implements AccountDao {
    private static final Logger s_logger = Logger.getLogger(AccountDaoImpl.class);
    private final String FIND_USER_ACCOUNT_BY_API_KEY = "SELECT u.id, u.username, u.account_id, u.secret_key, u.state, " +
    		                                      "a.id, a.account_name, a.type, a.domain_id, a.state " +
    		                                      "FROM `cloud`.`user` u, `cloud`.`account` a " +
    		                                      "WHERE u.account_id = a.id AND u.api_key = ? and u.removed IS NULL";
    
    protected final SearchBuilder<AccountVO> AccountNameSearch;
    protected final SearchBuilder<AccountVO> AccountTypeSearch;

    protected final SearchBuilder<AccountVO> CleanupSearch;
    
    protected AccountDaoImpl() {
        AccountNameSearch = createSearchBuilder();
        AccountNameSearch.and("accountName", AccountNameSearch.entity().getAccountName(), SearchCriteria.Op.EQ);
        AccountNameSearch.done();
        
        AccountTypeSearch = createSearchBuilder();
        AccountTypeSearch.and("domainId", AccountTypeSearch.entity().getDomainId(), SearchCriteria.Op.EQ);
        AccountTypeSearch.and("type", AccountTypeSearch.entity().getType(), SearchCriteria.Op.EQ);
        AccountTypeSearch.done();
        
        CleanupSearch = createSearchBuilder();
        CleanupSearch.and("cleanup", CleanupSearch.entity().getNeedsCleanup(), SearchCriteria.Op.EQ);
        CleanupSearch.and("removed", CleanupSearch.entity().getRemoved(), SearchCriteria.Op.NNULL);
        CleanupSearch.done();
        
    }
    
    @Override
    public List<AccountVO> findCleanups() {
    	SearchCriteria sc = CleanupSearch.create();
    	sc.setParameters("cleanup", true);
    	
    	return searchAll(sc, null, null, false);
    }
    
    public Pair<User, Account> findUserAccountByApiKey(String apiKey) {
        Transaction txn = Transaction.currentTxn();
        PreparedStatement pstmt = null;
        Pair<User, Account> userAcctPair = null;
        try {
            String sql = FIND_USER_ACCOUNT_BY_API_KEY;
            pstmt = txn.prepareAutoCloseStatement(sql);
            pstmt.setString(1, apiKey);
            ResultSet rs = pstmt.executeQuery();
            // TODO:  make sure we don't have more than 1 result?  ApiKey had better be unique
            if (rs.next()) {
                User u = new UserVO(rs.getLong(1));
                u.setUsername(rs.getString(2));
                u.setAccountId(rs.getLong(3));
                u.setSecretKey(rs.getString(4));
                u.setState(rs.getString(5));

                Account a = new AccountVO(rs.getLong(6));
                a.setAccountName(rs.getString(7));
                a.setType(rs.getShort(8));
                a.setDomainId(rs.getLong(9));
                a.setState(rs.getString(10));

                userAcctPair = new Pair<User, Account>(u, a);
            }
        } catch (Exception e) {
            s_logger.warn("Exception finding user/acct by api key: " + apiKey, e);
        }
        return userAcctPair;
    }

    @Override
    public List<AccountVO> findAccountsLike(String accountName) {
        SearchCriteria sc = createSearchCriteria();
        sc.addAnd("accountName", SearchCriteria.Op.LIKE, "%"+accountName+"%");
        return listActiveBy(sc);
    }

    @Override
    public Account findActiveAccount(String accountName, Long domainId) {
        SearchCriteria sc = AccountNameSearch.create("accountName", accountName);
        sc.addAnd("domainId", SearchCriteria.Op.EQ, domainId);
        return findOneActiveBy(sc);
    }

    @Override
    public Account findAccount(String accountName, Long domainId) {
        SearchCriteria sc = AccountNameSearch.create("accountName", accountName);
        sc.addAnd("domainId", SearchCriteria.Op.EQ, domainId);
        return findOneBy(sc);
    }
    
    public Account findActiveAccountByName(String accountName) {
    	SearchCriteria sc = AccountNameSearch.create("accountName", accountName);
        return findOneActiveBy(sc);
    }

    public List<AccountVO> findActiveAccounts(Long maxAccountId, Filter filter) {
        if (maxAccountId == null) return null;

        SearchCriteria sc = createSearchCriteria();
        sc.addAnd("id", SearchCriteria.Op.LTEQ, maxAccountId);

        return listActiveBy(sc, filter);
    }

    public List<AccountVO> findRecentlyDeletedAccounts(Long maxAccountId, Date earliestRemovedDate, Filter filter) {
        if (earliestRemovedDate == null) return null;
        SearchCriteria sc = createSearchCriteria();
        if (maxAccountId != null) {
            sc.addAnd("id", SearchCriteria.Op.LTEQ, maxAccountId);
        }
        sc.addAnd("removed", SearchCriteria.Op.NNULL);
        sc.addAnd("removed", SearchCriteria.Op.GTEQ, earliestRemovedDate);

        return listBy(sc, filter);
    }

    public List<AccountVO> findNewAccounts(Long minAccountId, Filter filter) {
        if (minAccountId == null) return null;

        SearchCriteria sc = createSearchCriteria();
        sc.addAnd("id", SearchCriteria.Op.GT, minAccountId);

        return listBy(sc, filter);
    }

	@Override
	public List<AccountVO> findAdminAccountsForDomain(Long domain) {
        SearchCriteria sc = AccountTypeSearch.create();
        sc.addAnd("domainId", Op.EQ,  domain);
        sc.addAnd("type", Op.IN, Account.ACCOUNT_TYPE_ADMIN, Account.ACCOUNT_TYPE_DOMAIN_ADMIN, Account.ACCOUNT_TYPE_READ_ONLY_ADMIN);
		return null;
	}
	
	@Override
	public void markForCleanup(long accountId) {
		AccountVO account = findById(accountId);
		if (!account.getNeedsCleanup()) {
			account.setNeedsCleanup(true);
        	update(accountId, account);
		}
	}
}
