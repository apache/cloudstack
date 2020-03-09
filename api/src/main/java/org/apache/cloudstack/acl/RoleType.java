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

import com.cloud.user.Account;
import com.google.common.base.Enums;
import com.google.common.base.Strings;

// Enum for default roles in CloudStack
public enum RoleType {
    Admin(1L, Account.ACCOUNT_TYPE_ADMIN, 1),
    ResourceAdmin(2L, Account.ACCOUNT_TYPE_RESOURCE_DOMAIN_ADMIN, 2),
    DomainAdmin(3L, Account.ACCOUNT_TYPE_DOMAIN_ADMIN, 4),
    User(4L, Account.ACCOUNT_TYPE_NORMAL, 8),
    Unknown(-1L, (short) -1, 0);

    private long id;
    private short accountType;
    private int mask;

    RoleType(final long id, final short accountType, final int mask) {
        this.id = id;
        this.accountType = accountType;
        this.mask = mask;
    }

    public long getId() {
        return id;
    }

    public short getAccountType() {
        return accountType;
    }

    public int getMask() {
        return mask;
    }

    public static RoleType fromString(final String name) {
        if (!Strings.isNullOrEmpty(name)
                && Enums.getIfPresent(RoleType.class, name).isPresent()) {
            return RoleType.valueOf(name);
        }
        throw new IllegalStateException("Illegal RoleType name provided");
    }

    public static RoleType fromMask(int mask) {
        for (RoleType roleType : RoleType.values()) {
            if (roleType.getMask() == mask) {
                return roleType;
            }
        }
        return Unknown;
    }

    public static RoleType getByAccountType(final short accountType) {
        RoleType roleType = RoleType.Unknown;
        switch (accountType) {
            case Account.ACCOUNT_TYPE_ADMIN:
                roleType = RoleType.Admin;
                break;
            case Account.ACCOUNT_TYPE_DOMAIN_ADMIN:
                roleType = RoleType.DomainAdmin;
                break;
            case Account.ACCOUNT_TYPE_RESOURCE_DOMAIN_ADMIN:
                roleType = RoleType.ResourceAdmin;
                break;
            case Account.ACCOUNT_TYPE_NORMAL:
                roleType = RoleType.User;
                break;
        }
        return roleType;
    }

    public static Long getRoleByAccountType(final Long roleId, final Short accountType) {
        if (roleId == null && accountType != null) {
            RoleType defaultRoleType = RoleType.getByAccountType(accountType);
            if (defaultRoleType != null && defaultRoleType != RoleType.Unknown) {
                return defaultRoleType.getId();
            }
        }
        return roleId;
    }

    public static Short getAccountTypeByRole(final Role role, final Short accountType) {
        if (role != null && role.getId() > 0L) {
            return role.getRoleType().getAccountType();
        }
        return accountType;
    }
}
