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

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CheckVMActivityOnStoragePoolCommand;
import com.cloud.agent.api.to.StorageFilerTO;
import com.cloud.agent.properties.AgentProperties;
import com.cloud.agent.properties.AgentPropertiesFileHandler;
import com.cloud.hypervisor.kvm.resource.KVMHABase.HAStoragePool;
import com.cloud.hypervisor.kvm.storage.KVMStoragePool;
import com.cloud.hypervisor.kvm.storage.KVMStoragePoolManager;
import com.cloud.hypervisor.kvm.resource.KVMHAMonitor;
import com.cloud.hypervisor.kvm.resource.KVMHAVMActivityChecker;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.utils.script.Script;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@ResourceWrapper(handles = CheckVMActivityOnStoragePoolCommand.class)
public final class LibvirtCheckVMActivityOnStoragePoolCommandWrapper extends CommandWrapper<CheckVMActivityOnStoragePoolCommand, Answer, LibvirtComputingResource> {

    @Override
    public Answer execute(final CheckVMActivityOnStoragePoolCommand command, final LibvirtComputingResource libvirtComputingResource) {
        final ExecutorService executors = Executors.newSingleThreadExecutor();
        final KVMHAMonitor monitor = libvirtComputingResource.getMonitor();
        final StorageFilerTO pool = command.getPool();
        final KVMStoragePoolManager storagePoolMgr = libvirtComputingResource.getStoragePoolMgr();

        KVMStoragePool primaryPool = storagePoolMgr.getStoragePool(pool.getType(), pool.getUuid());

        if (primaryPool.isPoolSupportHA()) {
            final HAStoragePool haPool = monitor.getStoragePool(pool.getUuid());
            final String vmActivityCheckPath = getVmActivityCheckPath(pool.getType(), libvirtComputingResource);
            if (vmActivityCheckPath == null) {
                return new Answer(command, false, "CheckVMActivityOnStoragePoolCommand: unable to find the script which checks the VM activity on a storage pool of type " + pool.getType());
            }
            final KVMHAVMActivityChecker ha = new KVMHAVMActivityChecker(haPool, command.getHost(), command.getVolumeList(), vmActivityCheckPath, command.getSuspectTimeInSeconds());
            final Future<Boolean> future = executors.submit(ha);
            try {
                final Boolean result = future.get();
                if (result) {
                    return new Answer(command, false, "VMHA disk activity detected ...");
                } else {
                    return new Answer(command);
                }
            } catch (InterruptedException e) {
                return new Answer(command, false, "CheckVMActivityOnStoragePoolCommand: can't get status of host: InterruptedException");
            } catch (ExecutionException e) {
                return new Answer(command, false, "CheckVMActivityOnStoragePoolCommand: can't get status of host: ExecutionException");
            }
        }
        return new Answer(command, false, "Unsupported Storage");
    }

    /**
     * The script which checks the VM activity depends on the type of the storage pool: a RBD pool has no shared mount point to look at,
     * so its script checks the watchers of the volumes, instead of the files of the volumes as the script of the other pools.
     */
    private String getVmActivityCheckPath(final StoragePoolType poolType, final LibvirtComputingResource libvirtComputingResource) {
        if (StoragePoolType.RBD.equals(poolType)) {
            final String kvmScriptsDir = AgentPropertiesFileHandler.getPropertyValue(AgentProperties.KVM_SCRIPTS_DIR);
            return Script.findScript(kvmScriptsDir, "kvmvmactivity_rbd.py");
        }
        return libvirtComputingResource.getVmActivityCheckPath();
    }
}
