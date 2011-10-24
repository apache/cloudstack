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
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name=("vpn_users"))
public class VpnUserVO implements VpnUser {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="id")
    private long id;

    @Column(name="owner_id")
    private long accountId;
    
    @Column(name="domain_id")
    private long domainId;

    @Column(name="username")
    private String username;
    
    @Column(name="password", encryptable=true)
    private String password;
    
    @Column(name="state")
    @Enumerated(value=EnumType.STRING)
    private State state;

    public VpnUserVO() { }

    public VpnUserVO(long accountId, long domainId, String userName, String password) {
        this.accountId = accountId;
        this.domainId = domainId;
        this.username = userName;
        this.password = password;
        this.state = State.Add;
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
	
	@Override
    public State getState() {
	    return state;
	}
	
	public void setState(State state) {
	    this.state = state;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	@Override
    public long getDomainId() {
		return domainId;
	}
    

	@Override
    public String toString() {
	    return new StringBuilder("VpnUser[").append(id).append("-").append(username).append("-").append(accountId).append("]").toString();
	}
}
