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

import com.cloud.agent.api.to.VolumeTO;
import com.cloud.storage.StoragePool;
import com.cloud.storage.VMTemplateStorageResourceAssoc;
import com.cloud.storage.Volume;

public class DestroyCommand extends StorageCommand {
    // in VMware, things are designed around VM instead of volume, we need it the volume VM context if the volume is attached
    String vmName;
    VolumeTO volume;

    protected DestroyCommand() {
    }

    public DestroyCommand(StoragePool pool, Volume volume, String vmName) {
        this.volume = new VolumeTO(volume, pool);
        this.vmName = vmName;
    }

    public DestroyCommand(StoragePool pool, VMTemplateStorageResourceAssoc templatePoolRef) {
        volume =
            new VolumeTO(templatePoolRef.getId(), null, pool.getPoolType(), pool.getUuid(), null, pool.getPath(), templatePoolRef.getInstallPath(),
                templatePoolRef.getTemplateSize(), null);
    }

    public VolumeTO getVolume() {
        return volume;
    }

    public String getVmName() {
        return vmName;
    }

    @Override
    public boolean executeInSequence() {
        return true;
    }
}
