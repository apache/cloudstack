// Licensed to the Apache Software Foundation (ASF) under one or more
// contributor license agreements. See the NOTICE file distributed with this
// work for additional information regarding copyright ownership. The ASF
// licenses this file to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance with the
// License. You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
// WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
// License for the specific language governing permissions and limitations
// under the License.
package org.apache.cloudstack.ai.engine;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Who is calling, taken from the CloudStack session (SPEC.md section 10).
 *
 * The engine authenticates nobody: it is unreachable from tenant networks
 * and requires a client certificate, and this assertion is what it filters
 * every response by. Building it anywhere other than from a real
 * {@code CallContext} would hand a caller someone else's data.
 */
public final class CallerContext {

    public static final String USER_HEADER = "X-Aisvc-User-Id";
    public static final String ACCOUNT_HEADER = "X-Aisvc-Account";
    public static final String DOMAIN_HEADER = "X-Aisvc-Domain";
    public static final String ROLE_HEADER = "X-Aisvc-Role";

    /** The role the engine treats as seeing everything. */
    public static final String ROLE_ADMIN = "Admin";

    private final String userId;
    private final String account;
    private final String domainPath;
    private final String role;

    public CallerContext(String userId, String account, String domainPath, String role) {
        this.userId = require(userId, "userId");
        this.account = require(account, "account");
        this.domainPath = require(domainPath, "domainPath");
        this.role = require(role, "role");
    }

    /**
     * The plugin acting on its own behalf, not for any user.
     *
     * <p>Used only where there is no CloudStack session to build a context
     * from and the work is the platform's own — host maintenance draining a
     * GPU node, for instance. It is deliberately a separate factory rather
     * than a default: a context for a tenant request must always come from
     * {@code CallContext}, never from a fallback, because the engine filters
     * every response by whatever the plugin asserts here.
     */
    public static CallerContext system() {
        return new CallerContext("system", "system", "/", ROLE_ADMIN);
    }

    /** Whether this caller sees the whole platform rather than one account. */
    public boolean isAdmin() {
        return ROLE_ADMIN.equalsIgnoreCase(role);
    }

    private static String require(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " is required in the caller context");
        }
        return value;
    }

    public Map<String, String> asHeaders() {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put(USER_HEADER, userId);
        headers.put(ACCOUNT_HEADER, account);
        headers.put(DOMAIN_HEADER, domainPath);
        headers.put(ROLE_HEADER, role);
        return headers;
    }

    public String getUserId() {
        return userId;
    }

    public String getAccount() {
        return account;
    }

    public String getDomainPath() {
        return domainPath;
    }

    public String getRole() {
        return role;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof CallerContext)) {
            return false;
        }
        CallerContext that = (CallerContext) other;
        return userId.equals(that.userId)
                && account.equals(that.account)
                && domainPath.equals(that.domainPath)
                && role.equals(that.role);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, account, domainPath, role);
    }

    /** Deliberately omits nothing sensitive; none of these are secrets. */
    @Override
    public String toString() {
        return "CallerContext{user=" + userId + ", account=" + account
                + ", domain=" + domainPath + ", role=" + role + "}";
    }
}
