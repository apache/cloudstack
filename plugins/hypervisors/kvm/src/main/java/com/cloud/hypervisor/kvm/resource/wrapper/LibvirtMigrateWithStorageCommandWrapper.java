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
import com.cloud.agent.api.MigrateWithStorageAnswer;
import com.cloud.agent.api.MigrateWithStorageCommand;
import com.cloud.agent.api.to.DataTO;
import com.cloud.agent.api.to.DiskTO;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import org.apache.cloudstack.storage.to.VolumeObjectTO;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@ResourceWrapper(handles = MigrateWithStorageCommand.class)
public final class  LibvirtMigrateWithStorageCommandWrapper extends CommandWrapper<MigrateWithStorageCommand, Answer, LibvirtComputingResource> {

    @Override
    public Answer execute(final MigrateWithStorageCommand command, final LibvirtComputingResource libvirtComputingResource) {
        List<VolumeObjectTO> volumes = new ArrayList<>();
        VirtualMachineTO vm = command.getVirtualMachine();
        List<DiskTO> disks = Arrays.asList(vm.getDisks());
        for (DiskTO disk : disks) {
            DataTO data = disk.getData();
            if (data instanceof VolumeObjectTO) {
                volumes.add((VolumeObjectTO) data);
            }
        }

        final String result = LibvirtMigrationHelper.executeMigrationWithFlags(libvirtComputingResource, vm.getName(), command.getTargetHost(), libvirtComputingResource.getMigrateWithStorageFlags(), null, true);
        return new MigrateWithStorageAnswer(command, volumes, result != null, result);

    }
}
