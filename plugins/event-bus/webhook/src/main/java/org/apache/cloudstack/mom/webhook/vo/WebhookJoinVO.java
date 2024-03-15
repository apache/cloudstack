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

package org.apache.cloudstack.mom.webhook.vo;


import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;

import org.apache.cloudstack.mom.webhook.Webhook;
import org.apache.cloudstack.utils.reflectiontostringbuilderutils.ReflectionToStringBuilderUtils;

import com.cloud.api.query.vo.ControlledViewEntity;
import com.cloud.user.Account;
import com.cloud.utils.db.Encrypt;
import com.cloud.utils.db.GenericDao;

@Entity
@Table(name = "webhook_view")
public class WebhookJoinVO implements ControlledViewEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private long id;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "name")
    private String name;

    @Column(name = "description", length = 4096)
    private String description;

    @Column(name = "state")
    @Enumerated(value = EnumType.STRING)
    private Webhook.State state;

    @Column(name = "payload_url")
    private String payloadUrl;

    @Column(name = "secret_key")
    @Encrypt
    private String secretKey;

    @Column(name = "ssl_verification")
    private boolean sslVerification;

    @Column(name = "scope")
    @Enumerated(value = EnumType.STRING)
    private Webhook.Scope scope;

    @Column(name = GenericDao.CREATED_COLUMN)
    private Date created;

    @Column(name = GenericDao.REMOVED_COLUMN)
    private Date removed;

    @Column(name = "account_id")
    private long accountId;

    @Column(name = "account_uuid")
    private String accountUuid;

    @Column(name = "account_name")
    private String accountName;

    @Column(name = "account_type")
    @Enumerated(value = EnumType.STRING)
    private Account.Type accountType;

    @Column(name = "domain_id")
    private long domainId;

    @Column(name = "domain_uuid")
    private String domainUuid;

    @Column(name = "domain_name")
    private String domainName;

    @Column(name = "domain_path")
    private String domainPath;

    @Column(name = "project_id")
    private long projectId;

    @Column(name = "project_uuid")
    private String projectUuid;

    @Column(name = "project_name")
    private String projectName;

    @Override
    public long getId() {
        return id;
    }

    @Override
    public String getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Webhook.State getState() {
        return state;
    }

    public String getPayloadUrl() {
        return payloadUrl;
    }

    public void setPayloadUrl(String payloadUrl) {
        this.payloadUrl = payloadUrl;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public Webhook.Scope getScope() {
        return scope;
    }

    public boolean isSslVerification() {
        return sslVerification;
    }

    public Date getCreated() {
        return created;
    }

    public Date getRemoved() {
        return removed;
    }

    @Override
    public long getDomainId() {
        return domainId;
    }

    @Override
    public String getDomainPath() {
        return domainPath;
    }

    @Override
    public String getDomainUuid() {
        return domainUuid;
    }

    @Override
    public String getDomainName() {
        return domainName;
    }

    @Override
    public Account.Type getAccountType() {
        return accountType;
    }

    @Override
    public long getAccountId() {
        return accountId;
    }

    @Override
    public String getAccountUuid() {
        return accountUuid;
    }

    @Override
    public String getAccountName() {
        return accountName;
    }

    @Override
    public String getProjectUuid() {
        return projectUuid;
    }

    @Override
    public String getProjectName() {
        return projectName;
    }

    @Override
    public Class<?> getEntityType() {
        return Webhook.class;
    }

    @Override
    public String toString() {
        return String.format("Webhook [%s]", ReflectionToStringBuilderUtils.reflectOnlySelectedFields(
                this, "id", "uuid", "name"));
    }

    public WebhookJoinVO() {
    }
}
