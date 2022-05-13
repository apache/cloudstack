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

package com.cloud.agent.api;

import java.io.File;
import java.util.Map;
import java.util.UUID;

import com.cloud.agent.api.to.StorageFilerTO;
import com.cloud.storage.StoragePool;

public class ModifyStoragePoolCommand extends Command {
    public static final String LOCAL_PATH_PREFIX = "/mnt/";

    private boolean add;
    private StorageFilerTO pool;
    private String localPath;
    private String storagePath;
    private Map<String, String> details;

    public ModifyStoragePoolCommand(boolean add, StoragePool pool, String localPath) {
        this.add = add;
        this.pool = new StorageFilerTO(pool);
        this.localPath = localPath;
    }

    public ModifyStoragePoolCommand(boolean add, StoragePool pool, String localPath, Map<String, String> details) {
        this(add, pool, localPath);
        this.details = details;
    }

    public ModifyStoragePoolCommand(boolean add, StoragePool pool) {
        this(add, pool, LOCAL_PATH_PREFIX + File.separator + UUID.nameUUIDFromBytes((pool.getHostAddress() + pool.getPath()).getBytes()));
    }

    public boolean getAdd() {
        return add;
    }

    public void setPool(StoragePool pool) {
        this.pool = new StorageFilerTO(pool);
    }

    public StorageFilerTO getPool() {
        return pool;
    }

    public String getLocalPath() {
        return localPath;
    }

    public void setStoragePath(String storagePath) {
        this.storagePath = storagePath;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public void setDetails(Map<String, String> details) {
        this.details = details;
    }

    public Map<String, String> getDetails() {
        return details;
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }
}
