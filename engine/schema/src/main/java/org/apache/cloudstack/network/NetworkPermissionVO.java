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
package org.apache.cloudstack.network;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import com.cloud.network.NetworkPermission;

@Entity
@Table(name = "network_permissions")
public class NetworkPermissionVO implements NetworkPermission {
    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "network_id")
    private long networkId;

    @Column(name = "account_id")
    private long accountId;

    public NetworkPermissionVO() {
    }

    public NetworkPermissionVO(long networkId, long accountId) {
        this.networkId = networkId;
        this.accountId = accountId;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public long getNetworkId() {
        return networkId;
    }

    @Override
    public long getAccountId() {
        return accountId;
    }
}
