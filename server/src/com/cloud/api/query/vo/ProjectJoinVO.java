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

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;

import com.cloud.server.ResourceTag.TaggedResourceType;
import com.cloud.utils.db.GenericDao;
import com.cloud.vm.VirtualMachine.State;

import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.api.InternalIdentity;

@Entity
@Table(name="project_view")
public class ProjectJoinVO extends BaseViewVO implements InternalIdentity, Identity {

    @Id
    @Column(name="id", updatable=false, nullable = false)
    private long id;

    @Column(name="uuid")
    private String uuid;

    @Column(name="name")
    private String name;

    @Column(name="display_text")
    String displayText;


    @Column(name="owner")
    String owner;

    @Column(name="state")
    @Enumerated(value=EnumType.STRING)
    private State state;

    @Column(name=GenericDao.CREATED_COLUMN)
    private Date created;

    @Column(name=GenericDao.REMOVED_COLUMN)
    private Date removed;

    @Column(name="account_id")
    private long accountId;

    @Column(name="domain_id")
    private long domainId;

    @Column(name="domain_uuid")
    private String domainUuid;

    @Column(name="domain_name")
    private String domainName;

    @Column(name="domain_path")
    private String domainPath;

    @Column(name="tag_id")
    private long tagId;

    @Column(name="tag_uuid")
    private String tagUuid;

    @Column(name="tag_key")
    private String tagKey;

    @Column(name="tag_value")
    private String tagValue;

    @Column(name="tag_domain_id")
    private long tagDomainId;

    @Column(name="tag_account_id")
    private long tagAccountId;

    @Column(name="tag_resource_id")
    private long tagResourceId;

    @Column(name="tag_resource_uuid")
    private String tagResourceUuid;

    @Column(name="tag_resource_type")
    @Enumerated(value=EnumType.STRING)
    private TaggedResourceType tagResourceType;

    @Column(name="tag_customer")
    private String tagCustomer;

    public ProjectJoinVO() {
    }

    @Override
    public long getId() {
        return id;
    }


    @Override
    public void setId(long id) {
        this.id = id;

    }

    @Override
    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getDomainId() {
        return domainId;
    }

    public void setDomainId(long domainId) {
        this.domainId = domainId;
    }

    public String getDomainUuid() {
        return domainUuid;
    }

    public void setDomainUuid(String domainUuid) {
        this.domainUuid = domainUuid;
    }

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public String getDomainPath() {
        return domainPath;
    }

    public void setDomainPath(String domainPath) {
        this.domainPath = domainPath;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public Date getRemoved() {
        return removed;
    }

    public void setRemoved(Date removed) {
        this.removed = removed;
    }

    public String getDisplayText() {
        return displayText;
    }

    public void setDisplayText(String displayText) {
        this.displayText = displayText;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

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

    public TaggedResourceType getTagResourceType() {
        return tagResourceType;
    }

    public void setTagResourceType(TaggedResourceType tagResourceType) {
        this.tagResourceType = tagResourceType;
    }

    public String getTagCustomer() {
        return tagCustomer;
    }

    public void setTagCustomer(String tagCustomer) {
        this.tagCustomer = tagCustomer;
    }

    public long getAccountId() {
        return accountId;
    }

    public void setAccountId(long accountId) {
        this.accountId = accountId;
    }

}
