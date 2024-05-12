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

import org.apache.commons.lang3.StringUtils;

import com.cloud.user.Account;
import com.google.common.base.Enums;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.util.HashMap;
import java.util.Map;

// Enum for default roles in CloudStack
public enum RoleType {
    Admin(1L, Account.Type.ADMIN, 1),
    ResourceAdmin(2L, Account.Type.RESOURCE_DOMAIN_ADMIN, 2),
    DomainAdmin(3L, Account.Type.DOMAIN_ADMIN, 4),
    User(4L, Account.Type.NORMAL, 8),
    Unknown(-1L,  Account.Type.UNKNOWN, 0);

    private long id;
    private Account.Type accountType;
    private int mask;

    private static Logger LOGGER = LogManager.getLogger(RoleType.class.getName());
    private static Map<Account.Type, RoleType> ACCOUNT_TYPE_MAP = new HashMap<>();

    static {
        for (RoleType t: RoleType.values()) {
            ACCOUNT_TYPE_MAP.put(t.getAccountType(),t);
        }
    }

    RoleType(final long id, final Account.Type accountType, final int mask) {
        this.id = id;
        this.accountType = accountType;
        this.mask = mask;
    }

    public long getId() {
        return id;
    }

    public Account.Type getAccountType() {
        return accountType;
    }

    public int getMask() {
        return mask;
    }

    public static RoleType fromString(final String name) {
        if (StringUtils.isNotEmpty(name)
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

    public static RoleType getByAccountType(final Account.Type accountType) {
        RoleType t = ACCOUNT_TYPE_MAP.get(accountType);
        if (t == null) {
            return RoleType.Unknown;
        }
        return t;
    }

    public static Long getRoleByAccountType(final Long roleId, final Account.Type accountType) {
        if (roleId == null && accountType != null) {
            RoleType defaultRoleType = RoleType.getByAccountType(accountType);
            if (defaultRoleType != null && defaultRoleType != RoleType.Unknown) {
                return defaultRoleType.getId();
            }
        }
        return roleId;
    }

    /**
     * This method returns the role account type if the role isn't null, else it returns the default account type.
     * */
    public static Account.Type getAccountTypeByRole(final Role role, final Account.Type defautAccountType) {
        if (role != null) {
            LOGGER.debug(String.format("Role [%s] is not null; therefore, we use its account type [%s].", role, defautAccountType));
            return role.getRoleType().getAccountType();
        }
        LOGGER.debug(String.format("Role is null; therefore, we use the default account type [%s] value.", defautAccountType));
        return defautAccountType;
    }
}
