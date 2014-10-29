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

public class DeleteStoragePoolCommand extends Command {
    public static final String DATASTORE_NAME = "datastoreName";
    public static final String IQN = "iqn";
    public static final String STORAGE_HOST = "storageHost";
    public static final String STORAGE_PORT = "storagePort";

    public static final String LOCAL_PATH_PREFIX = "/mnt/";

    private StorageFilerTO _pool;
    private String _localPath;
    private boolean _removeDatastore;
    private Map<String, String> _details;

    public DeleteStoragePoolCommand() {

    }

    public DeleteStoragePoolCommand(StoragePool pool, String localPath) {
        _pool = new StorageFilerTO(pool);
        _localPath = localPath;
    }

    public DeleteStoragePoolCommand(StoragePool pool) {
        this(pool, LOCAL_PATH_PREFIX + File.separator + UUID.nameUUIDFromBytes((pool.getHostAddress() + pool.getPath()).getBytes()));
    }

    public void setPool(StoragePool pool) {
        _pool = new StorageFilerTO(pool);
    }

    public StorageFilerTO getPool() {
        return _pool;
    }

    public String getLocalPath() {
        return _localPath;
    }

    public void setRemoveDatastore(boolean removeDatastore) {
        _removeDatastore = removeDatastore;
    }

    public boolean getRemoveDatastore() {
        return _removeDatastore;
    }

    public void setDetails(Map<String, String> details) {
        _details = details;
    }

    public Map<String, String> getDetails() {
        return _details;
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }
}
