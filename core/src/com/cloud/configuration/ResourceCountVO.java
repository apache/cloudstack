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

import com.cloud.configuration.ResourceCount.ResourceType;

@Entity
@Table(name="resource_count")
public class ResourceCountVO implements ResourceCount {
	
	@Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="id")
    private Long id = null;
	
	@Column(name="type")
	@Enumerated(EnumType.STRING)
	private ResourceCount.ResourceType type;
	
	@Column(name="account_id")
    private long accountId;
	
	@Column(name="count")
	private long count;
	
	public ResourceCountVO() {}
	
	public ResourceCountVO(long accountId, ResourceCount.ResourceType type, long count) {
		this.accountId = accountId;
		this.type = type;
		this.count = count;
	}
	
	public Long getId() {
		return id;
	}
	
	public void setId(Long id) {
		this.id = id;
	}
	
	public ResourceCount.ResourceType getType() {
		return type;
	}
	
	public void setType(ResourceCount.ResourceType type) {
		this.type = type;
	}
	
	public long getAccountId() {
		return accountId;
	}
	
	public void setAccountId(long accountId) {
		this.accountId = accountId;
	}
	
	public long getCount() {
		return count;
	}
	
	public void setCount(long count) {
		this.count = count;
	}

}
