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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CheckVMActivityOnStoragePoolCommand;
import com.cloud.agent.api.to.StorageFilerTO;
import com.cloud.hypervisor.kvm.resource.KVMHABase.NfsStoragePool;
import com.cloud.hypervisor.kvm.resource.KVMHABase.RbdStoragePool;
import com.cloud.hypervisor.kvm.resource.KVMHAMonitor;
import com.cloud.hypervisor.kvm.resource.KVMHAVMActivityChecker;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.storage.Storage;

@ResourceWrapper(handles = CheckVMActivityOnStoragePoolCommand.class)
public final class LibvirtCheckVMActivityOnStoragePoolCommandWrapper extends CommandWrapper<CheckVMActivityOnStoragePoolCommand, Answer, LibvirtComputingResource> {

    @Override
    public Answer execute(final CheckVMActivityOnStoragePoolCommand command, final LibvirtComputingResource libvirtComputingResource) {
        final ExecutorService executors = Executors.newSingleThreadExecutor();
        final KVMHAMonitor monitor = libvirtComputingResource.getMonitor();
        final StorageFilerTO pool = command.getPool();
        if (Storage.StoragePoolType.NetworkFilesystem == pool.getType() || Storage.StoragePoolType.RBD == pool.getType()){
            final NfsStoragePool nfspool = monitor.getStoragePool(pool.getUuid());
            final RbdStoragePool rbdpool = monitor.getRbdStoragePool(pool.getUuid());
            String vmActivityCheckPath = "";
            if (Storage.StoragePoolType.NetworkFilesystem == pool.getType()) {
                vmActivityCheckPath = libvirtComputingResource.getVmActivityCheckPath();
            } else if (Storage.StoragePoolType.RBD == pool.getType()) {
                vmActivityCheckPath = libvirtComputingResource.getVmActivityCheckPathRbd();
            }
            final KVMHAVMActivityChecker ha = new KVMHAVMActivityChecker(nfspool, rbdpool, command.getHost().getPrivateNetwork().getIp(), command.getVolumeList(), vmActivityCheckPath, command.getSuspectTimeInSeconds(), pool.getType());
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
}
