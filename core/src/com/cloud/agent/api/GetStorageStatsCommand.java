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

import com.cloud.agent.api.LogLevel.Log4jLevel;
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.storage.Storage.StoragePoolType;

@LogLevel(Log4jLevel.Trace)
public class GetStorageStatsCommand extends Command {
    private String id;
    private String localPath;
    private StoragePoolType pooltype;
    private String secUrl;
    private DataStoreTO store;

    public String getSecUrl() {
        return secUrl;
    }

    public void setSecUrl(String secUrl) {
        this.secUrl = secUrl;
    }

    public GetStorageStatsCommand() {
    }

    public StoragePoolType getPooltype() {
        return pooltype;
    }

    public void setPooltype(StoragePoolType pooltype) {
        this.pooltype = pooltype;
    }

    public GetStorageStatsCommand(DataStoreTO store) {
        this.store = store;
    }

    public GetStorageStatsCommand(String secUrl) {
        this.secUrl = secUrl;
    }

    public GetStorageStatsCommand(String id, StoragePoolType pooltype) {
        this.id = id;
        this.pooltype = pooltype;
    }

    public GetStorageStatsCommand(String id, StoragePoolType pooltype, String localPath) {
        this.id = id;
        this.pooltype = pooltype;
        this.localPath = localPath;
    }

    public String getStorageId() {
        return this.id;
    }

    public String getLocalPath() {
        return this.localPath;
    }

    public DataStoreTO getStore() {
        return this.store;
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }
}
