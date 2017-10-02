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
package com.cloud.tags;

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.cloud.server.ResourceTag;


@Entity
@Table(name = "resource_tags")
public class ResourceTagVO implements ResourceTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "key")
    private String key;

    @Column(name = "value")
    String value;

    @Column(name = "domain_id")
    long domainId;

    @Column(name = "account_id")
    long accountId;

    @Column(name = "resource_id")
    long resourceId;

    @Column(name = "resource_uuid")
    private String resourceUuid;

    @Column(name = "resource_type")
    @Enumerated(value = EnumType.STRING)
    private ResourceObjectType resourceType;

    @Column(name = "customer")
    String customer;

    protected ResourceTagVO() {
        uuid = UUID.randomUUID().toString();
    }

    /**
     * @param key
     * @param value
     * @param accountId
     * @param domainId
     * @param resourceId
     * @param resourceType
     * @param customer TODO
     * @param resourceUuid TODO
     */
    public ResourceTagVO(String key, String value, long accountId, long domainId, long resourceId, ResourceObjectType resourceType, String customer, String resourceUuid) {
        super();
        this.key = key;
        this.value = value;
        this.domainId = domainId;
        this.accountId = accountId;
        this.resourceId = resourceId;
        this.resourceType = resourceType;
        uuid = UUID.randomUUID().toString();
        this.customer = customer;
        this.resourceUuid = resourceUuid;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder("Tag[");
        buf.append(id)
            .append("|key=")
            .append(key)
            .append("|value=")
            .append(domainId)
            .append("|value=")
            .append("|resourceType=")
            .append(resourceType)
            .append("|resourceId=")
            .append(resourceId)
            .append("|accountId=")
            .append(accountId)
            .append("]");
        return buf.toString();
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public long getDomainId() {
        return domainId;
    }

    @Override
    public long getAccountId() {
        return accountId;
    }

    @Override
    public long getResourceId() {
        return resourceId;
    }

    @Override public void setResourceId(long resourceId) {
        this.resourceId = resourceId;
    }

    @Override
    public ResourceObjectType getResourceType() {
        return resourceType;
    }

    @Override
    public String getUuid() {
        return uuid;
    }

    @Override
    public String getCustomer() {
        return customer;
    }

    @Override
    public String getResourceUuid() {
        return resourceUuid;
    }

    @Override
    public Class<?> getEntityType() {
        return ResourceTag.class;
    }
}
