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
package com.cloud.agent.api.to;

import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.StoragePool;

public class StorageFilerTO {
    long id;
    String uuid;
    String host;
    String path;
    String userInfo;
    int port;
    StoragePoolType type;

    public StorageFilerTO(StoragePool pool) {
        this.id = pool.getId();
        this.host = pool.getHostAddress();
        this.port = pool.getPort();
        this.path = pool.getPath();
        this.type = pool.getPoolType();
        this.uuid = pool.getUuid();
        this.userInfo = pool.getUserInfo();
    }

    public long getId() {
        return id;
    }

    public String getUuid() {
        return uuid;
    }

    public String getHost() {
        return host;
    }

    public String getPath() {
        return path;
    }

    public String getUserInfo() {
        return userInfo;
    }

    public int getPort() {
        return port;
    }

    public StoragePoolType getType() {
        return type;
    }

    protected StorageFilerTO() {
    }

    @Override
    public String toString() {
        return new StringBuilder("Pool[").append(id).append("|").append(host).append(":").append(port).append("|").append(path).append("]").toString();
    }
}
