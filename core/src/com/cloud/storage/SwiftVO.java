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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.utils.db.GenericDao;
import com.google.gson.annotations.Expose;

@Entity
@Table(name="swift")
public class SwiftVO implements Swift {
	
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="id")
    private long id = -1;
    
    @Column(name="hostname")
    String hostName;

    @Column(name="account")
    String account;
    
    @Column(name="username")
    String userName;

    @Column(name="token")
    String token;
        
    public SwiftVO() { }

    public SwiftVO(String hostName, String account, String userName, String token) {
        this.hostName = hostName;
        this.account = account;
        this.userName = userName;
        this.token = token;
    }
    @Override
    public String getHostName() {
        return hostName;
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
    public String getToken() {
        return token;
    }

}
