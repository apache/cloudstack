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

package com.cloud.network;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.SecondaryTable;
import javax.persistence.Table;

@Entity
@Table(name=("vpn_users"))
@SecondaryTable(name="account",
        pkJoinColumns={@PrimaryKeyJoinColumn(name="account_id", referencedColumnName="id")})
public class VpnUserVO implements VpnUser {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="id")
    private long id;

    @Column(name="account_id")
    private long accountId;
    
    @Column(name="account_name", table="account", insertable=false, updatable=false)
    private String accountName = null;
    
    @Column(name="domain_id", table="account", insertable=false, updatable=false)
    private long domainId;

    @Column(name="username")
    private String username;
    
    @Column(name="password")
    private String password;

    public VpnUserVO() { }

    public VpnUserVO(long accountId, String userName, String password) {
        this.accountId = accountId;
        this.username = userName;
        this.password = password;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public long getAccountId() {
        return accountId;
    }
    
    @Override
    public String getAccountName() {
        return accountName;
    }

	@Override
    public String getUsername() {
		return username;
	}

	public void setUsername(String userName) {
		this.username = userName;
	}

	@Override
    public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public void setId(Long id) {
		this.id = id;
	}


	@Override
    public long getDomainId() {
		return domainId;
	}
    
    
}
