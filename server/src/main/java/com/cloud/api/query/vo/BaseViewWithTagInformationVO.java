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
package com.cloud.api.query.vo;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

import com.cloud.server.ResourceTag.ResourceObjectType;

@MappedSuperclass
public abstract class BaseViewWithTagInformationVO extends BaseViewVO implements Serializable {

    @Id
    @Column(name = "id")
    private long id;

    @Column(name = "tag_id")
    private long tagId;

    @Column(name = "tag_uuid")
    private String tagUuid;

    @Column(name = "tag_key")
    private String tagKey;

    @Column(name = "tag_value")
    private String tagValue;

    @Column(name = "tag_domain_id")
    private long tagDomainId;

    @Column(name = "tag_account_id")
    private long tagAccountId;

    @Column(name = "tag_resource_id")
    private long tagResourceId;

    @Column(name = "tag_resource_uuid")
    private String tagResourceUuid;

    @Column(name = "tag_resource_type")
    @Enumerated(value = EnumType.STRING)
    private ResourceObjectType tagResourceType;

    @Column(name = "tag_customer")
    private String tagCustomer;

    @Column(name = "tag_account_name")
    private String tagAccountName;

    @Column(name = "tag_domain_uuid")
    private String tagDomainUuid;

    @Column(name = "tag_domain_name")
    private String tagDomainName;

    public long getTagId() {
        return tagId;
    }

    public void setTagId(long tagId) {
        this.tagId = tagId;
    }

    public String getTagUuid() {
        return tagUuid;
    }

    public void setTagUuid(String tagUuid) {
        this.tagUuid = tagUuid;
    }

    public String getTagKey() {
        return tagKey;
    }

    public void setTagKey(String tagKey) {
        this.tagKey = tagKey;
    }

    public String getTagValue() {
        return tagValue;
    }

    public void setTagValue(String tagValue) {
        this.tagValue = tagValue;
    }

    public long getTagDomainId() {
        return tagDomainId;
    }

    public void setTagDomainId(long tagDomainId) {
        this.tagDomainId = tagDomainId;
    }

    public long getTagAccountId() {
        return tagAccountId;
    }

    public void setTagAccountId(long tagAccountId) {
        this.tagAccountId = tagAccountId;
    }

    public long getTagResourceId() {
        return tagResourceId;
    }

    public void setTagResourceId(long tagResourceId) {
        this.tagResourceId = tagResourceId;
    }

    public String getTagResourceUuid() {
        return tagResourceUuid;
    }

    public void setTagResourceUuid(String tagResourceUuid) {
        this.tagResourceUuid = tagResourceUuid;
    }

    public ResourceObjectType getTagResourceType() {
        return tagResourceType;
    }

    public void setTagResourceType(ResourceObjectType tagResourceType) {
        this.tagResourceType = tagResourceType;
    }

    public String getTagCustomer() {
        return tagCustomer;
    }

    public void setTagCustomer(String tagCustomer) {
        this.tagCustomer = tagCustomer;
    }

    public String getTagAccountName() {
        return tagAccountName;
    }

    public void setTagAccountName(String tagAccountName) {
        this.tagAccountName = tagAccountName;
    }

    public String getTagDomainUuid() {
        return tagDomainUuid;
    }

    public void setTagDomainUuid(String tagDomainUuid) {
        this.tagDomainUuid = tagDomainUuid;
    }

    public String getTagDomainName() {
        return tagDomainName;
    }

    public void setTagDomainName(String tagDomainName) {
        this.tagDomainName = tagDomainName;
    }

    public long getId() {
        return id;
    }

}
