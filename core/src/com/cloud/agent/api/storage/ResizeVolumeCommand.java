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

package com.cloud.agent.api.storage;

import com.cloud.agent.api.Command;
import com.cloud.agent.api.to.StorageFilerTO;

public class ResizeVolumeCommand extends Command {
    private String path;
    private StorageFilerTO pool;
    private String vmInstance;
    private Long newSize;
    private Long currentSize;
    private boolean shrinkOk;

    protected ResizeVolumeCommand() {

    }

    public ResizeVolumeCommand(String path, StorageFilerTO pool, Long currentSize, Long newSize, boolean shrinkOk, String vmInstance) {
        this.path = path;
        this.pool = pool;
        this.vmInstance = vmInstance;
        this.currentSize = currentSize;
        this.newSize = newSize;
        this.shrinkOk = shrinkOk;
    }

    public String getPath() {
        return path;
    }

    public String getPoolUuid() {
        return pool.getUuid();
    }

    public StorageFilerTO getPool() {
        return pool;
    }

    public long getNewSize() {
        return newSize;
    }

    public long getCurrentSize() {
        return currentSize;
    }

    public boolean getShrinkOk() {
        return shrinkOk;
    }

    public String getInstanceName() {
        return vmInstance;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean executeInSequence() {
        return false;
    }

}
