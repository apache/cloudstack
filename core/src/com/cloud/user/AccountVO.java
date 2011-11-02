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
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.cloud.api.Identity;
import com.cloud.utils.db.GenericDao;

@Entity
@Table(name="account")
public class AccountVO implements Account, Identity {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="id")
    private long id;

    @Column(name="account_name")
    private String accountName = null;

    @Column(name="type")
    private short type = ACCOUNT_TYPE_NORMAL;

    @Column(name="domain_id")
    private long domainId;

    @Column(name="state")
    @Enumerated(value=EnumType.STRING)
    private State state;

    @Column(name=GenericDao.REMOVED_COLUMN)
    private Date removed;
    
    @Column(name="cleanup_needed")
    private boolean needsCleanup = false;
    
    @Column(name="network_domain")
    private String networkDomain;

    @Column(name="uuid")
    private String uuid;

    public AccountVO() {
    	this.uuid = UUID.randomUUID().toString();
    }
    
    public AccountVO(long id) {
        this.id = id;
    	this.uuid = UUID.randomUUID().toString();
    }
    
    public AccountVO(String accountName, long domainId, String networkDomain, short type) {
        this.accountName = accountName;
        this.domainId = domainId;
        this.networkDomain = networkDomain;
        this.type = type;
        this.state = State.enabled;
    	this.uuid = UUID.randomUUID().toString();
    }
    
    public void setNeedsCleanup(boolean value) {
    	needsCleanup = value;
    }
    
    public boolean getNeedsCleanup() {
    	return needsCleanup;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public String getAccountName() {
        return accountName;
    }
    
    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }
    
    @Override
    public short getType() {
        return type;
    }
    
    public void setType(short type) {
        this.type = type;
    }

    @Override
    public long getDomainId() {
        return domainId;
    }
    
    public void setDomainId(long domainId) {
        this.domainId = domainId;
    }

    @Override
    public State getState() {
        return state;
    }
    
    public void setState(State state) {
        this.state = state;
    }

    @Override
    public Date getRemoved() {
        return removed;
    }
    
    @Override
    public long getAccountId() {
        return id;
    }
    
    @Override
    public String toString() {
        return new StringBuilder("Acct[").append(id).append("-").append(accountName).append("]").toString();
    }
    
    @Override
    public String getNetworkDomain() {
        return networkDomain;
    }
    
    public void setNetworkDomain(String networkDomain) {
        this.networkDomain = networkDomain;
    }
    
    @Override
    public String getUuid() {
    	return this.uuid;
    }
    
    public void setUuid(String uuid) {
    	this.uuid = uuid;
    }
}
