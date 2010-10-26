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
import javax.persistence.Transient;

import com.cloud.utils.db.GenericDao;

/**
 * A bean representing a user
 * 
 * @author Will Chan
 *
 */
@Entity
@Table(name="user")
public class UserVO implements User {
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	@Column(name="id")
	private Long id = null;
	
	@Column(name="username")
	private String username = null;
	
	@Column(name="password")
	private String password = null;
	
	@Column(name="firstname")
	private String firstname = null;
	
	@Column(name="lastname")
	private String lastname = null;

	@Column(name="account_id")
	private long accountId;

    @Column(name="email")
    private String email = null;

    @Column(name="state")
    private String state;

	@Column(name="api_key")
	private String apiKey = null;

	@Column(name="secret_key")
	private String secretKey = null;
	
	@Column(name=GenericDao.CREATED_COLUMN)
	private Date created;
	
    @Column(name=GenericDao.REMOVED_COLUMN)
	private Date removed;

    @Column(name="timezone")
    private String timezone;
    
    public UserVO() {}
    public UserVO(Long id) {
        this.id = id;
    }

	@Override
    public Long getId() {
		return id;
	}
	
    @Override
    public Date getCreated() {
        return created;
    }
    @Override
    public Date getRemoved() {
        return removed;
    }
    
	@Override
    public String getUsername() {
		return username;
	}
	@Override
    public void setUsername(String username) {
		this.username = username;
	}
	@Override
    public String getPassword() {
		return password;
	}
	@Override
    public void setPassword(String password) {
		this.password = password;
	}
	@Override
    public String getFirstname() {
		return firstname;
	}
	@Override
    public void setFirstname(String firstname) {
		this.firstname = firstname;
	}
	@Override
    public String getLastname() {
		return lastname;
	}
	@Override
    public void setLastname(String lastname) {
		this.lastname = lastname;
	}
	@Override
    public long getAccountId() {
	    return accountId;
	}
	@Override
    public void setAccountId(long accountId) {
	    this.accountId = accountId;
	}
    @Override
    public String getEmail() {
        return email;
    }
    @Override
    public void setEmail(String email) {
        this.email = email;
    }
    @Override
    public String getState() {
        return state;
    }
    @Override
    public void setState(String state) {
        this.state = state;
    }
	@Override
    public String getApiKey() {
	    return apiKey;
	}
	@Override
    public void setApiKey(String apiKey) {
	    this.apiKey = apiKey;
	}
    @Override
    public String getSecretKey() {
        return secretKey;
    }
    @Override
    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }
    @Override
    public String getTimezone()
    {
    	return timezone;
    }
    @Override
    public void setTimezone(String timezone)
    {
    	this.timezone = timezone;
    }

    @Transient
    String toString = null;
    
    @Override
    public String toString() {
        if (toString == null) {
            toString = new StringBuilder("User:").append(id).append(":").append(username).toString(); 
        }
        return toString;
    }
}
