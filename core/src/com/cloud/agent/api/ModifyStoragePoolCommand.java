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
import java.util.UUID;

import com.cloud.agent.api.to.StorageFilerTO;
import com.cloud.storage.StoragePool;

public class ModifyStoragePoolCommand extends Command {

    boolean add;
    StorageFilerTO pool;
    String localPath;
    String[] options;
    public static final String LOCAL_PATH_PREFIX = "/mnt/";

    public ModifyStoragePoolCommand() {

    }

    public ModifyStoragePoolCommand(boolean add, StoragePool pool, String localPath) {
        this.add = add;
        this.pool = new StorageFilerTO(pool);
        this.localPath = localPath;

    }

    public ModifyStoragePoolCommand(boolean add, StoragePool pool) {
        this(add, pool, LOCAL_PATH_PREFIX + File.separator + UUID.nameUUIDFromBytes((pool.getHostAddress() + pool.getPath()).getBytes()));
    }

    public StorageFilerTO getPool() {
        return pool;
    }

    public void setPool(StoragePool pool) {
        this.pool = new StorageFilerTO(pool);
    }

    public boolean getAdd() {
        return add;
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }

    public String getLocalPath() {
        return localPath;
    }

    public void setOptions(String[] options) {
        this.options = options;
    }

}
