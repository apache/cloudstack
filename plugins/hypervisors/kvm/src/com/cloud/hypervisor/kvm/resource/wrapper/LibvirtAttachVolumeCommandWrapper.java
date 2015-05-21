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

import org.libvirt.Connect;
import org.libvirt.LibvirtException;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.AttachVolumeAnswer;
import com.cloud.agent.api.AttachVolumeCommand;
import com.cloud.exception.InternalErrorException;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.storage.KVMPhysicalDisk;
import com.cloud.hypervisor.kvm.storage.KVMStoragePool;
import com.cloud.resource.CommandWrapper;

public final class LibvirtAttachVolumeCommandWrapper extends CommandWrapper<AttachVolumeCommand, Answer, LibvirtComputingResource> {

    @Override
    public Answer execute(final AttachVolumeCommand command, final LibvirtComputingResource libvirtComputingResource) {
        try {
            final LibvirtUtilitiesHelper libvirtUtilitiesHelper = libvirtComputingResource.getLibvirtUtilitiesHelper();

            final Connect conn = libvirtUtilitiesHelper.getConnectionByVmName(command.getVmName());

            final KVMStoragePool primary = libvirtComputingResource.getStoragePoolMgr().getStoragePool(command.getPooltype(), command.getPoolUuid());
            final KVMPhysicalDisk disk = primary.getPhysicalDisk(command.getVolumePath());

            libvirtComputingResource.attachOrDetachDisk(conn, command.getAttach(), command.getVmName(), disk,
                    command.getDeviceId().intValue(), command.getBytesReadRate(), command.getBytesWriteRate(), command.getIopsReadRate(), command.getIopsWriteRate(),
                    command.getCacheMode());

        } catch (final LibvirtException e) {
            return new AttachVolumeAnswer(command, e.toString());
        } catch (final InternalErrorException e) {
            return new AttachVolumeAnswer(command, e.toString());
        }

        return new AttachVolumeAnswer(command, command.getDeviceId(), command.getVolumePath());
    }
}