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

import org.apache.cloudstack.acl.ControlledEntity;
import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.api.InternalIdentity;

public interface Account extends ControlledEntity, InternalIdentity, Identity {


    public enum State {
        disabled, enabled, locked
    }

    public static final short ACCOUNT_TYPE_NORMAL = 0;
    public static final short ACCOUNT_TYPE_ADMIN = 1;
    public static final short ACCOUNT_TYPE_DOMAIN_ADMIN = 2;
    public static final short ACCOUNT_TYPE_RESOURCE_DOMAIN_ADMIN = 3;
    public static final short ACCOUNT_TYPE_READ_ONLY_ADMIN = 4;
    public static final short ACCOUNT_TYPE_PROJECT = 5;

    public static final String ACCOUNT_STATE_DISABLED = "disabled";
    public static final String ACCOUNT_STATE_ENABLED = "enabled";
    public static final String ACCOUNT_STATE_LOCKED = "locked";

    public static final long ACCOUNT_ID_SYSTEM = 1;

    public String getAccountName();

    public short getType();

    public State getState();

    public Date getRemoved();

    public String getNetworkDomain();

    public Long getDefaultZoneId();

    @Override
    public String getUuid();

    boolean isDefault();

}
