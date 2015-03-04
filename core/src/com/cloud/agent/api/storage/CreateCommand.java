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
import com.cloud.storage.StoragePool;
import com.cloud.vm.DiskProfile;

public class CreateCommand extends Command {
    private long volId;
    private StorageFilerTO pool;
    private DiskProfile diskCharacteristics;
    private String templateUrl;
    boolean executeInSequence = false;

    protected CreateCommand() {
        super();
    }

    /**
     * Construction for template based volumes.
     * @param diskCharacteristics
     * @param templateUrl
     * @param pool
     * @param executeInSequence TODO
     * @param vol
     * @param vm
     */
    public CreateCommand(DiskProfile diskCharacteristics, String templateUrl, StorageFilerTO pool, boolean executeInSequence) {
        this(diskCharacteristics, pool, executeInSequence);
        this.templateUrl = templateUrl;
        this.executeInSequence = executeInSequence;
    }

    /**
     * Construction for regular volumes.
     * @param diskCharacteristics
     * @param pool
     * @param executeInSequence TODO
     * @param vol
     * @param vm
     */
    public CreateCommand(DiskProfile diskCharacteristics, StorageFilerTO pool, boolean executeInSequence) {
        this.volId = diskCharacteristics.getVolumeId();
        this.diskCharacteristics = diskCharacteristics;
        this.pool = pool;
        this.templateUrl = null;
        this.executeInSequence = executeInSequence;
    }

    public CreateCommand(DiskProfile diskCharacteristics, String templateUrl, StoragePool pool, boolean executeInSequence) {
        this(diskCharacteristics, templateUrl, new StorageFilerTO(pool), executeInSequence);
    }

    public CreateCommand(DiskProfile diskCharacteristics, StoragePool pool, boolean executeInSequence) {
        this(diskCharacteristics, new StorageFilerTO(pool), executeInSequence);
        this.executeInSequence = executeInSequence;
    }

    @Override
    public boolean executeInSequence() {
        return executeInSequence;
    }

    public String getTemplateUrl() {
        return templateUrl;
    }

    public StorageFilerTO getPool() {
        return pool;
    }

    public DiskProfile getDiskCharacteristics() {
        return diskCharacteristics;
    }

    public long getVolumeId() {
        return volId;
    }

    @Deprecated
    public String getInstanceName() {
        return null;
    }
}
