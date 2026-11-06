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

package com.cloud.hypervisor.kvm.resource.wrapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.apache.cloudstack.backup.BackupAnswer;
import org.apache.cloudstack.backup.RemoveBackupAnchorsCommand;
import org.apache.cloudstack.storage.to.PrimaryDataStoreTO;
import org.apache.commons.collections.CollectionUtils;

import com.cloud.agent.api.Answer;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.storage.KVMPhysicalDisk;
import com.cloud.hypervisor.kvm.storage.KVMStoragePool;
import com.cloud.hypervisor.kvm.storage.KVMStoragePoolManager;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.storage.Storage;
import com.cloud.utils.Pair;
import com.cloud.utils.script.Script;

@ResourceWrapper(handles = RemoveBackupAnchorsCommand.class)
public class LibvirtRemoveBackupAnchorsCommandWrapper extends CommandWrapper<RemoveBackupAnchorsCommand, Answer, LibvirtComputingResource> {

    @Override
    public Answer execute(RemoveBackupAnchorsCommand command, LibvirtComputingResource libvirtComputingResource) {
        final List<PrimaryDataStoreTO> volumePools = command.getVolumePools();
        final List<String> volumePaths = command.getVolumePaths();
        final KVMStoragePoolManager storagePoolMgr = libvirtComputingResource.getStoragePoolMgr();

        // Only an RBD volume carries an anchor, so the volumes of every other pool type are left out
        // rather than passed to a script which would have nothing to ask about them.
        List<String> diskPaths = new ArrayList<>();
        if (Objects.nonNull(volumePaths)) {
            for (int idx = 0; idx < volumePaths.size(); idx++) {
                PrimaryDataStoreTO volumePool = volumePools.get(idx);
                if (volumePool == null || volumePool.getPoolType() != Storage.StoragePoolType.RBD) {
                    continue;
                }
                KVMStoragePool volumeStoragePool = storagePoolMgr.getStoragePool(volumePool.getPoolType(), volumePool.getUuid());
                diskPaths.add(KVMPhysicalDisk.RBDStringBuilder(volumeStoragePool, volumePaths.get(idx)));
            }
        }

        if (CollectionUtils.isEmpty(diskPaths)) {
            return new BackupAnswer(command, true, null);
        }

        List<String[]> commands = new ArrayList<>();
        commands.add(new String[]{
                libvirtComputingResource.getNasBackupPath(),
                "-o", "removeanchors",
                "-d", String.join(",", diskPaths)
        });

        Pair<Integer, String> result = Script.executePipedCommands(commands, libvirtComputingResource.getCmdsTimeout());

        logger.debug("Removing the backup anchors of {} returned: {}, exit code: {}",
                () -> Arrays.toString(diskPaths.toArray()), result::second, result::first);

        if (result.first() != 0) {
            logger.warn("Failed to remove the backup anchors of {}: {}", Arrays.toString(diskPaths.toArray()), result.second());
            return new BackupAnswer(command, false, result.second());
        }
        return new BackupAnswer(command, true, null);
    }
}
