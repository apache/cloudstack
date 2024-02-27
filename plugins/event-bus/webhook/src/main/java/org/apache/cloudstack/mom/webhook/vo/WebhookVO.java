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
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.apache.cloudstack.mom.webhook.Webhook;
import org.apache.cloudstack.utils.reflectiontostringbuilderutils.ReflectionToStringBuilderUtils;

import com.cloud.utils.db.Encrypt;
import com.cloud.utils.db.GenericDao;

@Entity
@Table(name = "webhook")
public class WebhookVO implements Webhook {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "name")
    private String name;

    @Column(name = "description", length = 4096)
    private String description;

    @Column(name = "state")
    @Enumerated(value = EnumType.STRING)
    private State state;

    @Column(name = "domain_id")
    private long domainId;

    @Column(name = "account_id")
    private long accountId;

    @Column(name = "payload_url")
    private String payloadUrl;

    @Column(name = "secret_key")
    @Encrypt
    private String secretKey;

    @Column(name = "ssl_verification")
    private boolean sslVerification;

    @Column(name = "scope")
    @Enumerated(value = EnumType.STRING)
    private Scope scope;

    @Column(name = GenericDao.CREATED_COLUMN)
    private Date created;

    @Column(name = GenericDao.REMOVED_COLUMN)
    private Date removed;

    @Override
    public long getId() {
        return id;
    }

    @Override
    public String getUuid() {
        return uuid;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    @Override
    public long getDomainId() {
        return domainId;
    }

    public void setDomainId(long domainId) {
        this.domainId = domainId;
    }

    @Override
    public long getAccountId() {
        return accountId;
    }

    public void setAccountId(long accountId) {
        this.accountId = accountId;
    }

    @Override
    public String getPayloadUrl() {
        return payloadUrl;
    }

    public void setPayloadUrl(String payloadUrl) {
        this.payloadUrl = payloadUrl;
    }

    @Override
    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    @Override
    public Scope getScope() {
        return scope;
    }

    public void setScope(Scope scope) {
        this.scope = scope;
    }

    @Override
    public boolean isSslVerification() {
        return sslVerification;
    }

    public void setSslVerification(boolean sslVerification) {
        this.sslVerification = sslVerification;
    }

    @Override
    public Date getCreated() {
        return created;
    }

    public Date getRemoved() {
        return removed;
    }

    @Override
    public Class<?> getEntityType() {
        return Webhook.class;
    }

    @Override
    public String toString() {
        return String.format("Webhook [%s]",ReflectionToStringBuilderUtils.reflectOnlySelectedFields(
                this, "id", "uuid", "name", "payloadUrl"));
    }

    public WebhookVO() {
        this.uuid = UUID.randomUUID().toString();
    }

    public WebhookVO(String name, String description, State state, long domainId, long accountId,
                     String payloadUrl, String secretKey, boolean sslVerification, Scope scope) {
        this.uuid = UUID.randomUUID().toString();
        this.name = name;
        this.description = description;
        this.state = state;
        this.domainId = domainId;
        this.accountId = accountId;
        this.payloadUrl = payloadUrl;
        this.secretKey = secretKey;
        this.sslVerification = sslVerification;
        this.scope = scope;
    }

    /*
     * For creating a dummy rule for testing delivery
     */
    public WebhookVO(long domainId, long accountId, String payloadUrl, String secretKey, boolean sslVerification) {
        this.uuid = UUID.randomUUID().toString();
        this.id = ID_DUMMY;
        this.name = NAME_DUMMY;
        this.description = NAME_DUMMY;
        this.state = State.Enabled;
        this.domainId = domainId;
        this.accountId = accountId;
        this.payloadUrl = payloadUrl;
        this.secretKey = secretKey;
        this.sslVerification = sslVerification;
        this.scope = Scope.Local;
    }
}
