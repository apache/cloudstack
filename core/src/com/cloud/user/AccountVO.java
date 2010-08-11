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

package com.cloud.user;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.cloud.utils.db.GenericDao;

@Entity
@Table(name="account")
public class AccountVO implements Account {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="id")
    private Long id = null;

    @Column(name="account_name")
    private String accountName = null;

    @Column(name="type")
    private short type = ACCOUNT_TYPE_NORMAL;

    @Column(name="domain_id")
    private Long domainId = null;

    @Column(name="state")
    private String state;

    @Column(name=GenericDao.REMOVED_COLUMN)
    private Date removed;
    
    @Column(name="cleanup_needed")
    private boolean needsCleanup = false;

    public AccountVO() {}
    public AccountVO(Long id) {
        this.id = id;
    }
    
    public void setNeedsCleanup(boolean value) {
    	needsCleanup = value;
    }
    
    public boolean getNeedsCleanup() {
    	return needsCleanup;
    }

    public Long getId() {
        return id;
    }

    public String getAccountName() {
        return accountName;
    }
    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }
    public short getType() {
        return type;
    }
    public void setType(short type) {
        this.type = type;
    }

    public Long getDomainId() {
        return domainId;
    }
    public void setDomainId(Long domainId) {
        this.domainId = domainId;
    }

    public String getState() {
        return state;
    }
    public void setState(String state) {
        this.state = state;
    }

    public Date getRemoved() {
        return removed;
    }
}
