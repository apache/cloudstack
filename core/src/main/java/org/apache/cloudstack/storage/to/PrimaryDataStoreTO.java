//
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
//

package org.apache.cloudstack.storage.to;

import java.util.Map;

import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStore;

import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.Storage.StoragePoolType;

public class PrimaryDataStoreTO implements DataStoreTO {
    public static final String MANAGED = PrimaryDataStore.MANAGED;
    public static final String STORAGE_HOST = PrimaryDataStore.STORAGE_HOST;
    public static final String STORAGE_PORT = PrimaryDataStore.STORAGE_PORT;
    public static final String MANAGED_STORE_TARGET = PrimaryDataStore.MANAGED_STORE_TARGET;
    public static final String MANAGED_STORE_TARGET_ROOT_VOLUME = PrimaryDataStore.MANAGED_STORE_TARGET_ROOT_VOLUME;
    public static final String CHAP_INITIATOR_USERNAME = PrimaryDataStore.CHAP_INITIATOR_USERNAME;
    public static final String CHAP_INITIATOR_SECRET = PrimaryDataStore.CHAP_INITIATOR_SECRET;
    public static final String CHAP_TARGET_USERNAME = PrimaryDataStore.CHAP_TARGET_USERNAME;
    public static final String CHAP_TARGET_SECRET = PrimaryDataStore.CHAP_TARGET_SECRET;
    public static final String REMOVE_AFTER_COPY = PrimaryDataStore.REMOVE_AFTER_COPY;
    public static final String VOLUME_SIZE = PrimaryDataStore.VOLUME_SIZE;

    private String uuid;
    private final String name;
    private String type;
    private final long id;
    private StoragePoolType poolType;
    private String host;
    private String path;
    private int port;
    private final String url;
    private Map<String, String> details;
    private static final String pathSeparator = "/";
    private Boolean fullCloneFlag;
    private final boolean isManaged;

    public PrimaryDataStoreTO(PrimaryDataStore dataStore) {
        this.uuid = dataStore.getUuid();
        this.name = dataStore.getName();
        this.id = dataStore.getId();
        this.setPoolType(dataStore.getPoolType());
        this.setHost(dataStore.getHostAddress());
        this.setPath(dataStore.getPath());
        this.setPort(dataStore.getPort());
        this.url = dataStore.getUri();
        this.details = dataStore.getDetails();
        this.isManaged = dataStore.isManaged();
    }

    public long getId() {
        return this.id;
    }

    @Override
    public String getUuid() {
        return this.uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getUrl() {
        return this.url;
    }

    public Map<String, String> getDetails() {
        return this.details;
    }

    public String getName() {
        return this.name;
    }

    public String getType() {
        return this.type;
    }

    @Override
    public DataStoreRole getRole() {
        return DataStoreRole.Primary;
    }

    public StoragePoolType getPoolType() {
        return poolType;
    }

    public void setPoolType(StoragePoolType poolType) {
        this.poolType = poolType;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public String getPathSeparator() {
        return pathSeparator;
    }

    @Override
    public String toString() {
        return new StringBuilder("PrimaryDataStoreTO[uuid=").append(uuid)
            .append("|name=")
            .append(name)
            .append("|id=")
            .append(id)
            .append("|pooltype=")
            .append(poolType)
            .append("]")
            .toString();
    }

    public Boolean isFullCloneFlag() {
        return fullCloneFlag;
    }

    public void setFullCloneFlag(Boolean fullCloneFlag) {
        this.fullCloneFlag = fullCloneFlag;
    }

    public boolean isManaged() {
        return isManaged;
    }
}
