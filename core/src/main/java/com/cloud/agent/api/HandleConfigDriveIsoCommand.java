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

import com.cloud.agent.api.to.DataStoreTO;

public class HandleConfigDriveIsoCommand extends Command {

    @LogLevel(LogLevel.Log4jLevel.Off)
    private String isoData;
    private String isoFile;
    private boolean create = false;
    private DataStoreTO destStore;
    private boolean useHostCacheOnUnsupportedPool = false;
    private boolean preferHostCache = false;

    public HandleConfigDriveIsoCommand(String isoFile, String isoData, DataStoreTO destStore, boolean useHostCacheOnUnsupportedPool, boolean preferHostCache, boolean create) {
        this.isoFile = isoFile;
        this.isoData = isoData;
        this.destStore = destStore;
        this.create = create;
        this.useHostCacheOnUnsupportedPool = useHostCacheOnUnsupportedPool;
        this.preferHostCache = preferHostCache;
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }

    public String getIsoData() {
        return isoData;
    }

    public boolean isCreate() {
        return create;
    }

    public DataStoreTO getDestStore() {
        return destStore;
    }

    public String getIsoFile() {
        return isoFile;
    }

    public boolean isHostCachePreferred() {
        return preferHostCache;
    }

    public boolean getUseHostCacheOnUnsupportedPool() {
        return useHostCacheOnUnsupportedPool;
    }
}
