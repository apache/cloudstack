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

package com.cloud.server;

import java.util.HashMap;

public class Criteria {
	
	private Long offset;
	private Long limit;
	private String orderBy;
	private Boolean ascending;
	private final HashMap<String, Object> criteria;

	public static final String ID = "id";
	public static final String USERID = "userId";
	public static final String NAME = "name";
	public static final String NOTSTATE = "notState";
	public static final String STATE = "state";
	public static final String DATACENTERID = "dataCenterId";
	public static final String DESCRIPTION = "description";
	public static final String PODID = "podId";
	public static final String HOSTID = "hostId";
	public static final String PODNAME  = "podName";
	public static final String ZONENAME = "zoneName";
	public static final String HOSTNAME = "hostName";
	public static final String HOST = "host";
	public static final String USERNAME = "username";
	public static final String TYPE = "type";
	public static final String STATUS = "status";
	public static final String READY = "ready";
	public static final String ISPUBLIC = "isPublic";
	public static final String ADDRESS = "address";
	public static final String REMOVED = "removed";
	public static final String ISRECURSIVE = "isRecursive";
	public static final String ISDISABLED = "isDisabled";
	public static final String ISCLEANUPREQUIRED = "isCleanupRequired";
	public static final String LEVEL = "level";
	public static final String STARTDATE = "startDate";
	public static final String ENDDATE = "endDate";
	public static final String VTYPE = "vType";
	public static final String INSTANCEID = "instanceId";
	public static final String VOLUMEID = "volumeId";
	public static final String DOMAINID = "domainId";
	public static final String DOMAIN = "domain";
    public static final String ACCOUNTID = "accountId";
    public static final String ACCOUNTNAME = "accountName";
    public static final String CATEGORY = "category";
    public static final String CREATED_BY = "createdBy";
    public static final String GROUPID = "groupId";
    public static final String PATH = "path";
    public static final String KEYWORD = "keyword";
    public static final String ISADMIN = "isadmin";
    public static final String VLAN = "vlan";
    public static final String ISALLOCATED = "isallocated";
    public static final String IPADDRESS = "ipaddress";
    public static final String FOR_VIRTUAL_NETWORK = "forvirtualnetwork";
    public static final String TARGET_IQN = "targetiqn";
    public static final String SCOPE = "scope";
    public static final String NETWORKGROUP = "networkGroup";


	public Criteria(String orderBy, Boolean ascending, Long offset, Long limit) {
		this.offset = offset;
		this.limit = limit;
		this.orderBy = orderBy;
		this.ascending = ascending;
		criteria = new HashMap<String, Object>();
	}
	
	public Criteria() {
		criteria = new HashMap<String, Object>();
		this.ascending = false;
	}

	public Long getOffset() {
		return offset;
	}
	
	public void addCriteria(String name, Object val) {
		criteria.put(name, val);
	}
	
	public Object getCriteria(String name) {
		return criteria.get(name);
	}

	public void setOffset(Long offset) {
		this.offset = offset;
	}

	public Long getLimit() {
		return limit;
	}

	public void setLimit(Long limit) {
		this.limit = limit;
	}

	public String getOrderBy() {
		return orderBy;
	}

	public void setOrderBy(String orderBy) {
		this.orderBy = orderBy;
	}

	public Boolean getAscending() {
		return ascending;
	}

	public void setAscending(Boolean ascending) {
		this.ascending = ascending;
	}
	
}
