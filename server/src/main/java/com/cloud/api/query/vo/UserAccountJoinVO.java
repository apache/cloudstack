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

import com.cloud.user.Account;
import com.cloud.user.User;
import com.cloud.user.UserAccount;
import com.cloud.utils.db.Encrypt;
import com.cloud.utils.db.GenericDao;
import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.api.InternalIdentity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Date;

@Entity
@Table(name = "user_view")
public class UserAccountJoinVO extends BaseViewVO implements InternalIdentity, Identity, ControlledViewEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private long id;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "username")
    private String username = null;

    @Column(name = "password")
    private String password = null;

    @Column(name = "firstname")
    private String firstname = null;

    @Column(name = "lastname")
    private String lastname = null;

    @Column(name = "email")
    private String email = null;

    @Column(name = "state")
    private String state;

    @Column(name = "api_key")
    private String apiKey = null;

    @Encrypt
    @Column(name = "secret_key")
    private String secretKey = null;

    @Column(name = GenericDao.CREATED_COLUMN)
    private Date created;

    @Column(name = GenericDao.REMOVED_COLUMN)
    private Date removed;

    @Column(name = "timezone")
    private String timezone;

    @Column(name = "registration_token")
    private String registrationToken = null;

    @Column(name = "is_registered")
    boolean registered;

    @Column(name = "incorrect_login_attempts")
    int loginAttempts;

    @Column(name = "account_id")
    private long accountId;

    @Column(name = "account_uuid")
    private String accountUuid;

    @Column(name = "account_name")
    private String accountName = null;

    @Column(name = "account_type")
    @Enumerated(value = EnumType.ORDINAL)
    private Account.Type accountType;

    @Column(name = "account_role_id")
    private Long accountRoleId;

    @Column(name = "domain_id")
    private long domainId;

    @Column(name = "domain_uuid")
    private String domainUuid;

    @Column(name = "domain_name")
    private String domainName = null;

    @Column(name = "domain_path")
    private String domainPath = null;

    @Column(name = "job_id")
    private Long jobId;

    @Column(name = "job_uuid")
    private String jobUuid;

    @Column(name = "job_status")
    private int jobStatus;

    @Column(name = "default")
    boolean isDefault;

    @Column(name = "source")
    @Enumerated(value = EnumType.STRING)
    private User.Source source;

    public UserAccountJoinVO() {
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public String getUuid() {
        return uuid;
    }

    public long getAccountId() {
        return accountId;
    }

    public String getAccountUuid() {
        return accountUuid;
    }

    public String getAccountName() {
        return accountName;
    }

    public Account.Type getAccountType() {
        return accountType;
    }

    public Long getAccountRoleId() {
        return accountRoleId;
    }

    public long getDomainId() {
        return domainId;
    }

    public String getDomainUuid() {
        return domainUuid;
    }

    public String getDomainName() {
        return domainName;
    }

    public String getDomainPath() {
        return domainPath;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getFirstname() {
        return firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public String getEmail() {
        return email;
    }

    public String getState() {
        return state;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public Date getCreated() {
        return created;
    }

    public Date getRemoved() {
        return removed;
    }

    public String getTimezone() {
        return timezone;
    }

    public String getRegistrationToken() {
        return registrationToken;
    }

    public boolean isRegistered() {
        return registered;
    }

    public int getLoginAttempts() {
        return loginAttempts;
    }

    public Long getJobId() {
        return jobId;
    }

    public String getJobUuid() {
        return jobUuid;
    }

    public int getJobStatus() {
        return jobStatus;
    }

    public boolean isDefault() {
        return isDefault;
    }

    @Override
    public Class<?> getEntityType() {
        return UserAccount.class;
    }

    @Override
    public String getName() {
        return accountName;
    }

    @Override
    public String getProjectUuid() {
        return null;
    }

    @Override
    public String getProjectName() {
        return null;
    }

    public User.Source getSource() {
        return source;
    }
}
