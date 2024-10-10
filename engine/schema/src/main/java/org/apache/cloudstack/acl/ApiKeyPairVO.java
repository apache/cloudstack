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
package org.apache.cloudstack.acl;

import com.cloud.exception.PermissionDeniedException;
import com.cloud.user.Account;
import com.cloud.utils.db.Encrypt;
import org.apache.cloudstack.acl.apikeypair.ApiKeyPair;
import org.joda.time.DateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Entity
@Table(name = "api_keypair")
public class ApiKeyPairVO implements ApiKeyPair {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "uuid", nullable = false)
    private String uuid = UUID.randomUUID().toString();

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "domain_id", nullable = false)
    private Long domainId;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "start_date")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date startDate;

    @Column(name = "end_date")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date endDate;

    @Column(name = "created", nullable = false)
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date created = Date.from(Instant.now());

    @Column(name = "description")
    private String description = "";

    @Column(name = "api_key", nullable = false)
    private String apiKey;

    @Encrypt
    @Column(name = "secret_key", nullable = false)
    private String secretKey;

    @Column(name = "removed")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date removed;

    public ApiKeyPairVO() {
    }

    public ApiKeyPairVO(Long id) {
        this.id = id;
    }

    public ApiKeyPairVO(Long userId, String description, Date startDate, Date endDate,
                        String apiKey, String secretKey) {
        this.userId = userId;
        this.description = description;
        this.startDate = startDate;
        this.endDate = endDate;
        this.apiKey = apiKey;
        this.secretKey = secretKey;
    }

    public ApiKeyPairVO(String name, Long userId, String description, Date startDate, Date endDate, Account account) {
        if (name == null) {
            this.name = userId + " - API Keypair";
        } else {
            this.name = name;
        }
        this.userId = userId;
        this.description = description;
        this.startDate = startDate;
        this.endDate = endDate;
        this.domainId = account.getDomainId();
        this.accountId = account.getAccountId();
    }

    public ApiKeyPairVO(Long id, Long userId) {
        this.id = id;
        this.userId = userId;
    }

    public boolean validateDate(boolean throwException) throws PermissionDeniedException {
        Date now = DateTime.now().toDate();
        Date keypairStart = this.getStartDate();
        Date keypairExpiration = this.getEndDate();
        if (keypairStart != null && now.compareTo(keypairStart) <= 0) {
            if (throwException) {
                throw new PermissionDeniedException(String.format("Keypair is not valid yet, start date: %s", keypairStart));
            }
            return false;
        }
        if (keypairExpiration != null && now.compareTo(keypairExpiration) >= 0) {
            if (throwException) {
                throw new PermissionDeniedException(String.format("Keypair is expired, expiration date: %s", keypairExpiration));
            }
            return false;
        }
        return true;
    }

    public long getId() {
        return id;
    }

    public String getUuid() {
        return uuid;
    }

    public Long getUserId() {
        return userId;
    }

    public Date getStartDate() {
        return startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public Date getCreated() {
        return created;
    }

    public String getDescription() {
        return description;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public Class<?> getEntityType() {
        return ApiKeyPair.class;
    }

    public String getName() {
        return name;
    }

    public Date getRemoved() {
        return removed;
    }

    @Override
    public long getDomainId() {
        return this.domainId;
    }

    @Override
    public long getAccountId() {
        return this.accountId;
    }

    public void setId(Long id) { this.id = id; }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDomainId(Long domainId) {
        this.domainId = domainId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public void setRemoved(Date removed) {
        this.removed = removed;
    }
}
