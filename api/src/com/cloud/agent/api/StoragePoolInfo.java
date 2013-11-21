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
package com.cloud.agent.api;

import java.util.Map;

import com.cloud.storage.Storage.StoragePoolType;

public class StoragePoolInfo {
    String uuid;
    String host;
    String localPath;
    String hostPath;
    StoragePoolType poolType;
    long capacityBytes;
    long availableBytes;
    Map<String, String> details;

    protected StoragePoolInfo() {
        super();
    }

    public StoragePoolInfo(String uuid, String host, String hostPath, String localPath, StoragePoolType poolType, long capacityBytes, long availableBytes) {
        super();
        this.uuid = uuid;
        this.host = host;
        this.localPath = localPath;
        this.hostPath = hostPath;
        this.poolType = poolType;
        this.capacityBytes = capacityBytes;
        this.availableBytes = availableBytes;
    }

    public StoragePoolInfo(String uuid, String host, String hostPath, String localPath, StoragePoolType poolType, long capacityBytes, long availableBytes,
            Map<String, String> details) {
        this(uuid, host, hostPath, localPath, poolType, capacityBytes, availableBytes);
        this.details = details;
    }

    public long getCapacityBytes() {
        return capacityBytes;
    }

    public long getAvailableBytes() {
        return availableBytes;
    }

    public String getUuid() {
        return uuid;
    }

    public String getHost() {
        return host;
    }

    public String getLocalPath() {
        return localPath;
    }

    public String getHostPath() {
        return hostPath;
    }

    public StoragePoolType getPoolType() {
        return poolType;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public Map<String, String> getDetails() {
        return details;
    }
}
