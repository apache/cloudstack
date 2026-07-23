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
package com.cloud.hypervisor.kvm.resource.wrapper;

import com.cloud.agent.api.Answer;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.storage.KVMStoragePoolManager;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.storage.Storage;
import org.apache.cloudstack.backup.ConsolidateVolumesAnswer;
import org.apache.cloudstack.backup.ConsolidateVolumesCommand;
import org.apache.cloudstack.storage.to.VolumeObjectTO;
import org.libvirt.LibvirtException;

import java.util.ArrayList;
import java.util.List;

@ResourceWrapper(handles = ConsolidateVolumesCommand.class)
public class LibvirtConsolidateVolumesCommandWrapper extends CommandWrapper<ConsolidateVolumesCommand, Answer, LibvirtComputingResource> {

    @Override
    public Answer execute(ConsolidateVolumesCommand command, LibvirtComputingResource serverResource) {
        List<VolumeObjectTO> volumeObjectTOs = command.getVolumesToConsolidate();
        String vmName = command.getVmName();

        List<VolumeObjectTO> successfulConsolidations = new ArrayList<>();
        try {
            for (VolumeObjectTO volumeObjectTO : volumeObjectTOs) {
                if (!serverResource.pullVolumeBackingFile(volumeObjectTO, vmName)) {
                    return new ConsolidateVolumesAnswer(command, false, "Failed to consolidate all volumes.", successfulConsolidations);
                }
                successfulConsolidations.add(volumeObjectTO);
            }
        } catch (LibvirtException ex) {
            return new ConsolidateVolumesAnswer(command, false, ex.getMessage(), successfulConsolidations);
        }

        KVMStoragePoolManager kvmStoragePoolManager = serverResource.getStoragePoolMgr();
        for (String secStorageUuid : command.getSecondaryStorageUuids()) {
            kvmStoragePoolManager.deleteStoragePool(Storage.StoragePoolType.NetworkFilesystem, secStorageUuid);
        }
        return new ConsolidateVolumesAnswer(command, true, "Success", successfulConsolidations);
    }
}
