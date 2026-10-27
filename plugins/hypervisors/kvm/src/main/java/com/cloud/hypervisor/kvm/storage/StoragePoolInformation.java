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
package com.cloud.hypervisor.kvm.storage;

import com.cloud.storage.Storage;

import java.util.Map;

class StoragePoolInformation {
    private String name;
    private String host;
    private int port;
    private String path;
    private String userInfo;
    private boolean type;
    private Storage.StoragePoolType poolType;
    private Map<String, String> details;

    public StoragePoolInformation(String name, String host, int port, String path, String userInfo, Storage.StoragePoolType poolType, Map<String, String> details, boolean type) {
        this.name = name;
        this.host = host;
        this.port = port;
        this.path = path;
        this.userInfo = userInfo;
        this.type = type;
        this.poolType = poolType;
        this.details = details;
    }

    public String getName() {
        return name;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getPath() {
        return path;
    }

    public String getUserInfo() {
        return userInfo;
    }

    public boolean isType() {
        return type;
    }

    public Storage.StoragePoolType getPoolType() {
        return poolType;
    }

    public Map<String, String> getDetails() {
        return details;
    }
}
