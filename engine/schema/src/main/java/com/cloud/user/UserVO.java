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
package com.cloud.user;

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

import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.api.InternalIdentity;
import org.apache.cloudstack.utils.reflectiontostringbuilderutils.ReflectionToStringBuilderUtils;

import com.cloud.user.Account.State;
import com.cloud.utils.db.Encrypt;
import com.cloud.utils.db.GenericDao;
import org.apache.commons.lang3.StringUtils;

/**
 * A bean representing a user
 *
 */
@Entity
@Table(name = "user")
public class UserVO implements User, Identity, InternalIdentity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "username")
    private String username = null;

    @Column(name = "password")
    private String password = null;

    @Column(name = "firstname")
    private String firstname = null;

    @Column(name = "lastname")
    private String lastname = null;

    @Column(name = "account_id")
    private long accountId;

    @Column(name = "email")
    private String email = null;

    @Column(name = "state")
    @Enumerated(value = EnumType.STRING)
    private State state;

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

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "default")
    boolean isDefault;

    @Column(name = "source")
    @Enumerated(value = EnumType.STRING)
    private Source source;

    @Column(name = "external_entity", length = 65535)
    private String externalEntity;

    @Column(name = "is_user_2fa_enabled")
    private boolean user2faEnabled;

    @Column(name = "user_2fa_provider")
    private String user2faProvider;

    @Column(name = "key_for_2fa")
    private String keyFor2fa;

    public UserVO() {
        this.uuid = UUID.randomUUID().toString();
    }

    public UserVO(long id) {
        this.id = id;
        this.uuid = UUID.randomUUID().toString();
    }

    public UserVO(long accountId, String username, String password, String firstName, String lastName, String email, String timezone, String uuid, Source source) {
        this.accountId = accountId;
        this.username = username;
        this.password = password;
        this.firstname = firstName;
        this.lastname = lastName;
        this.email = email;
        this.timezone = timezone;
        this.state = State.ENABLED;
        this.uuid = uuid;
        this.source = source;
    }

    public UserVO(UserVO user) {
        this.setAccountId(user.getAccountId());
        this.setUsername(user.getUsername());
        this.setPassword(user.getPassword());
        this.setFirstname(user.getFirstname());
        this.setLastname(user.getLastname());
        this.setEmail(user.getEmail());
        this.setTimezone(user.getTimezone());
        this.setUuid(user.getUuid());
        this.setSource(user.getSource());
        this.setApiKey(user.getApiKey());
        this.setSecretKey(user.getSecretKey());
        this.setExternalEntity(user.getExternalEntity());
        this.setRegistered(user.isRegistered());
        this.setRegistrationToken(user.getRegistrationToken());
        this.setState(user.getState());
    }

        @Override
    public long getId() {
        return id;
    }

    @Override
    public Date getCreated() {
        return created;
    }

    @Override
    public Date getRemoved() {
        return removed;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String getFirstname() {
        return firstname;
    }

    @Override
    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    @Override
    public String getLastname() {
        return lastname;
    }

    @Override
    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    @Override
    public long getAccountId() {
        return accountId;
    }

    @Override
    public void setAccountId(long accountId) {
        this.accountId = accountId;
    }

    @Override
    public String getEmail() {
        return email;
    }

    @Override
    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public State getState() {
        return state;
    }

    @Override
    public void setState(State state) {
        this.state = state;
    }

    @Override
    public String getApiKey() {
        return apiKey;
    }

    @Override
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public String getSecretKey() {
        return secretKey;
    }

    @Override
    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    @Override
    public String getTimezone() {
        if (StringUtils.isEmpty(timezone)) {
            return "UTC";
        }
        return timezone;
    }

    @Override
    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    @Override
    public String getRegistrationToken() {
        return registrationToken;
    }

    public void setRegistrationToken(String registrationToken) {
        this.registrationToken = registrationToken;
    }

    @Override
    public boolean isRegistered() {
        return registered;
    }

    public void setRegistered(boolean registered) {
        this.registered = registered;
    }

    @Override
    public String toString() {
        return String.format("User %s.", ReflectionToStringBuilderUtils.reflectOnlySelectedFields(this, "username", "uuid"));
    }

    @Override
    public String getUuid() {
        return this.uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public boolean isDefault() {
        return isDefault;
    }

    public Source getSource() {
        return source;
    }

    public void setSource(Source source) {
        this.source = source;
    }

    public String getExternalEntity() {
        return externalEntity;
    }

    public void setExternalEntity(String externalEntity) {
        this.externalEntity = externalEntity;
    }

    public boolean isUser2faEnabled() {
        return user2faEnabled;
    }

    public void setUser2faEnabled(boolean user2faEnabled) {
        this.user2faEnabled = user2faEnabled;
    }

    public String getKeyFor2fa() {
        return keyFor2fa;
    }

    public void setKeyFor2fa(String keyFor2fa) {
        this.keyFor2fa = keyFor2fa;
    }

    public String getUser2faProvider() {
        return user2faProvider;
    }

    public void setUser2faProvider(String user2faProvider) {
        this.user2faProvider = user2faProvider;
    }

}
