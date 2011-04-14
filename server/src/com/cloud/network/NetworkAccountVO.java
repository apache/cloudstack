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

/**
 * 
 */
package com.cloud.network;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.cloud.user.OwnedBy;

@Entity
@Table(name="account_network_ref")
public class NetworkAccountVO implements OwnedBy {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    long id;
    
    @Column(name="account_id")
    long accountId;
    
    @Column(name="network_id")
    long networkId;
    
    @Column(name="is_owner")
    boolean owner;

    protected NetworkAccountVO() {
    }
    
    public NetworkAccountVO(long networkId, long accountId, boolean owner) {
        this.networkId = networkId;
        this.accountId = accountId;
        this.owner = owner;
    }
    
    @Override
    public long getAccountId() {
        return accountId;
    }
    
    public long getNetworkId() {
        return networkId;
    }
    
    public boolean isOwner() {
        return owner;
    }

}
