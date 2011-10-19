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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="swift")
public class SwiftVO implements Swift {
	
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="id")
    private long id = -1;
    
    @Column(name="url")
    String url;

    @Column(name="account")
    String account;
    
    @Column(name="username")
    String userName;

    @Column(name="key")
    String key;
        
    public SwiftVO() { }

    public SwiftVO(String url, String account, String userName, String key) {
        this.url = url;
        this.account = account;
        this.userName = userName;
        this.key = key;
    }

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

}
