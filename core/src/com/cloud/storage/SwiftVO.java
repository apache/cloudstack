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

package com.cloud.storage;

import java.util.Date;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.cloud.agent.api.to.SwiftTO;
import com.cloud.api.Identity;
import com.cloud.utils.db.GenericDao;

@Entity
@Table(name="swift")
public class SwiftVO implements Swift, Identity {
	
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="id")
    private long id;
    
    @Column(name="url")
    String url;

    @Column(name="account")
    String account;
    
    @Column(name="username")
    String userName;

    @Column(name="key")
    String key;

    @Column(name="uuid")
    String uuid = UUID.randomUUID().toString();
    
    @Column(name = GenericDao.CREATED_COLUMN)
    private Date created;
        
    public SwiftVO() { }

    public SwiftVO(String url, String account, String userName, String key) {
        this.url = url;
        this.account = account;
        this.userName = userName;
        this.key = key;
    }

    @Override
    public long getId() {
        return id;
    }
    @Override
    public String getUrl() {
        return url;
    }
    @Override
    public String getAccount() {
        return account;
    }
    @Override
    public String getUserName() {
        return userName;
    }
    @Override
    public String getKey() {
        return key;
    }

    public Date getCreated() {
        return created;
    }

    @Override
    public SwiftTO toSwiftTO() {
        return new SwiftTO(getId(), getUrl(), getAccount(), getUserName(), getKey());
    }
    
    @Override
    public String getUuid() {
    	return this.uuid;
    }
    
    public void setUuid(String uuid) {
    	this.uuid = uuid;
    }
}
