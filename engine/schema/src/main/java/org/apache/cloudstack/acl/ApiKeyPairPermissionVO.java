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

import org.apache.cloudstack.acl.apikeypair.ApiKeyPairPermission;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "keypair_permissions")
public class ApiKeyPairPermissionVO extends RolePermissionBaseVO implements ApiKeyPairPermission {
    @Column(name = "api_keypair_id")
    private long apiKeyPairId;

    @Column(name = "sort_order")
    private long sortOrder = 0;

    public ApiKeyPairPermissionVO(long apiKeyPairId, String rule, Permission permission, String description) {
        super(rule, permission, description);
        this.apiKeyPairId = apiKeyPairId;
    }

    public long getApiKeyPairId() {
        return this.apiKeyPairId;
    }

    public ApiKeyPairPermissionVO() {
    }

    public void setSortOrder(long sortOrder) {
        this.sortOrder = sortOrder;
    }

    public long getSortOrder() {
        return sortOrder;
    }
}

