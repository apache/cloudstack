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
package com.cloud.agent.api.storage;

import com.cloud.agent.api.Command;
import com.cloud.agent.api.to.StorageFilerTO;
import com.cloud.storage.StoragePool;

public class CreateVolumeOVACommand extends Command {
    String secUrl;
    String volPath;
    String volName;
    StorageFilerTO pool;

    public CreateVolumeOVACommand() {
    }

    public CreateVolumeOVACommand(String secUrl, String volPath, String volName, StoragePool pool, int wait) {
        this.secUrl = secUrl;
        this.volPath = volPath;
        this.volName = volName;
        this.pool = new StorageFilerTO(pool);
        setWait(wait);
    }

    @Override
    public boolean executeInSequence() {
        return true;
    }

    public String getVolPath() {
        return this.volPath;
    }

    public String getVolName() {
        return this.volName;
    }

    public String getSecondaryStorageUrl() {
        return this.secUrl;
    }

    public StorageFilerTO getPool() {
        return pool;
    }
}
