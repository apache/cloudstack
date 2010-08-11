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

	public Long getId() {
		return id;
	}
	
    public Date getCreated() {
        return created;
    }
    public Date getRemoved() {
        return removed;
    }
    
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public String getFirstname() {
		return firstname;
	}
	public void setFirstname(String firstname) {
		this.firstname = firstname;
	}
	public String getLastname() {
		return lastname;
	}
	public void setLastname(String lastname) {
		this.lastname = lastname;
	}
	public long getAccountId() {
	    return accountId;
	}
	public void setAccountId(long accountId) {
	    this.accountId = accountId;
	}
    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public String getState() {
        return state;
    }
    public void setState(String state) {
        this.state = state;
    }
	public String getApiKey() {
	    return apiKey;
	}
	public void setApiKey(String apiKey) {
	    this.apiKey = apiKey;
	}
    public String getSecretKey() {
        return secretKey;
    }
    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }
    public String getTimezone()
    {
    	return timezone;
    }
    public void setTimezone(String timezone)
    {
    	this.timezone = timezone;
    }
}
