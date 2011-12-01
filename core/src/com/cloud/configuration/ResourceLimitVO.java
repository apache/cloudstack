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

package com.cloud.configuration;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="resource_limit")
public class ResourceLimitVO implements ResourceLimit {
	
	@Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="id")
    private Long id = null;
	
	@Column(name="type")
	@Enumerated(EnumType.STRING)
	private ResourceCount.ResourceType type;
	
	@Column(name="domain_id")
    private Long domainId;
	
	@Column(name="account_id")
    private Long accountId;
	
	@Column(name="max")
	private Long max;
    
    public ResourceLimitVO() {}
	
	public ResourceLimitVO(ResourceCount.ResourceType type, Long max, long ownerId, ResourceOwnerType ownerType) {
		this.type = type;
		this.max = max;
		
		if (ownerType == ResourceOwnerType.Account) {
            this.accountId = ownerId;
        } else if (ownerType == ResourceOwnerType.Domain) {
            this.domainId = ownerId;
        }
	}
	
	@Override
	public Long getId() {
		return id;
	}
	
	public void setId(Long id) {
		this.id = id;
	}
	
	@Override
	public ResourceType getType() {
		return type;
	}
	
	public void setType(ResourceCount.ResourceType type) {
		this.type = type;
	}
	
	public Long getDomainId() {
		return domainId;
	}
	
	public Long getAccountId() {
		return accountId;
	}
	
	@Override
	public Long getMax() {
		return max;
	}
	
	@Override
	public void setMax(Long max) {
		this.max = max;
	}
	
   @Override
    public long getOwnerId() {
        if (accountId != null) {
            return accountId;
        } 
        
        return domainId;
    }
   
   @Override
   public ResourceOwnerType getResourceOwnerType() {
       if (accountId != null) {
           return ResourceOwnerType.Account;
       } else {
           return ResourceOwnerType.Domain;
       }
   }

   public void setDomainId(Long domainId) {
       this.domainId = domainId;
    }

   public void setAccountId(Long accountId) {
       this.accountId = accountId;
   }

}
