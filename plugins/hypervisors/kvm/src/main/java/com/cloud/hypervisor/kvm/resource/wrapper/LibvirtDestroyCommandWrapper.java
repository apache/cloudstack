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
import com.cloud.agent.api.storage.DestroyCommand;
import com.cloud.agent.api.to.VolumeTO;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.storage.KVMStoragePool;
import com.cloud.hypervisor.kvm.storage.KVMStoragePoolManager;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.utils.exception.CloudRuntimeException;

@ResourceWrapper(handles =  DestroyCommand.class)
public final class LibvirtDestroyCommandWrapper extends CommandWrapper<DestroyCommand, Answer, LibvirtComputingResource> {


    @Override
    public Answer execute(final DestroyCommand command, final LibvirtComputingResource libvirtComputingResource) {
        final VolumeTO vol = command.getVolume();
        try {
            final KVMStoragePoolManager storagePoolMgr = libvirtComputingResource.getStoragePoolMgr();
            final KVMStoragePool pool = storagePoolMgr.getStoragePool(vol.getPoolType(), vol.getPoolUuid());
            pool.deletePhysicalDisk(vol.getPath(), null);
            return new Answer(command, true, "Success");
        } catch (final CloudRuntimeException e) {
            logger.debug("Failed to delete volume: " + e.toString());
            return new Answer(command, false, e.toString());
        }
    }
}
