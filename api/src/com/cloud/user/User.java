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

import org.apache.cloudstack.api.InternalIdentity;

public interface User extends OwnedBy, InternalIdentity {
    public static final long UID_SYSTEM = 1;

    public long getId();

    public String getUuid();

    public Date getCreated();

    public Date getRemoved();

    public String getUsername();

    public void setUsername(String username);

    public String getPassword();

    public void setPassword(String password);

    public String getFirstname();

    public void setFirstname(String firstname);

    public String getLastname();

    public void setLastname(String lastname);

    public void setAccountId(long accountId);

    public String getEmail();

    public void setEmail(String email);

    public Account.State getState();

    public void setState(Account.State state);

    public String getApiKey();

    public void setApiKey(String apiKey);

    public String getSecretKey();

    public void setSecretKey(String secretKey);

    public String getTimezone();

    public void setTimezone(String timezone);

    String getRegistrationToken();

    boolean isRegistered();

}
