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
package com.cloud.vm;

import com.cloud.domain.Domain;
import com.cloud.domain.dao.DomainDao;
import com.cloud.user.Account;
import com.cloud.user.User;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.UserDao;
import com.cloud.utils.Journal;

public class ReservationContextImpl implements ReservationContext {
    User _caller;
    Account _account;
    Domain _domain;
    Journal _journal;
    String _reservationId;
    
    public ReservationContextImpl(String reservationId, Journal journal, User caller) {
        this(reservationId, journal, caller, null, null);
    }
    
    public ReservationContextImpl(String reservationId, Journal journal, User caller, Account account) {
        this(reservationId, journal, caller, account, null);
        
    }
    
    public ReservationContextImpl(String reservationId, Journal journal, User caller, Account account, Domain domain) {
        _caller = caller;
        _account = account;
        _domain = domain;
        _journal = journal;
        _reservationId = reservationId;
    }
    
    @Override
    public long getDomainId() {
        return 0;
    }

    @Override
    public long getAccountId() {
        return _caller.getAccountId();
    }

    @Override
    public User getCaller() {
        return _caller;
    }

    @Override
    public Account getAccount() {
        if (_account == null) {
            _account = s_accountDao.findByIdIncludingRemoved(_caller.getId());
        }
        return _account; 
    }

    @Override
    public Domain getDomain() {
        if (_domain == null) {
            getAccount();
            _domain = s_domainDao.findByIdIncludingRemoved(_account.getDomainId());
        }
        return _domain;
    }

    @Override
    public Journal getJournal() {
        return _journal;
    }

    @Override
    public String getReservationId() {
        return _reservationId;
    }
    
    static UserDao s_userDao;
    static DomainDao s_domainDao;
    static AccountDao s_accountDao;
    
    static public void setComponents(UserDao userDao, DomainDao domainDao, AccountDao accountDao) {
        s_userDao = userDao;
        s_domainDao = domainDao;
        s_accountDao = accountDao;
    }
}
