// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
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
    public static final String DATACENTERTYPE = "dataCenterType";
    public static final String DESCRIPTION = "description";
    public static final String PODID = "podId";
    public static final String CLUSTERID = "clusterId";
    public static final String HOSTID = "hostId";
    public static final String OSCATEGORYID = "osCategoryId";
    public static final String PODNAME = "podName";
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
    public static final String GROUP = "group";
    public static final String EMPTY_GROUP = "emptyGroup";
    public static final String NETWORKID = "networkId";
    public static final String HYPERVISOR = "hypervisor";
    public static final String STORAGE_ID = "storageid";
    public static final String TEMPLATE_ID = "templateid";
    public static final String ISO_ID = "isoid";
    public static final String VPC_ID = "vpcId";
    public static final String AFFINITY_GROUP_ID = "affinitygroupid";
    public static final String SERVICE_OFFERING_ID = "serviceofferingid";
    public static final String DISPLAY = "display";
    public static final String SSH_KEYPAIR = "keypair";

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
