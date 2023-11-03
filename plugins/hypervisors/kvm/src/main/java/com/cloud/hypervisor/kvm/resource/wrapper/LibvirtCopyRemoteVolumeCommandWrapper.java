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
import com.cloud.agent.api.CopyRemoteVolumeAnswer;
import com.cloud.agent.api.CopyRemoteVolumeCommand;
import com.cloud.agent.api.to.StorageFilerTO;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.storage.KVMStoragePool;
import com.cloud.hypervisor.kvm.storage.KVMStoragePoolManager;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.storage.Storage;
import org.apache.log4j.Logger;

@ResourceWrapper(handles = CopyRemoteVolumeCommand.class)
public final class LibvirtCopyRemoteVolumeCommandWrapper extends CommandWrapper<CopyRemoteVolumeCommand, Answer, LibvirtComputingResource> {

    private static final Logger s_logger = Logger.getLogger(LibvirtCopyRemoteVolumeCommandWrapper.class);

    @Override
    public Answer execute(final CopyRemoteVolumeCommand command, final LibvirtComputingResource libvirtComputingResource) {
        String result = null;
        String srcIp = command.getRemoteIp();
        String username = command.getUsername();
        String password = command.getPassword();
        String srcFile = command.getSrcFile();
        StorageFilerTO storageFilerTO = command.getStorageFilerTO();
        String tmpPath = command.getTmpPath();
        KVMStoragePoolManager poolMgr = libvirtComputingResource.getStoragePoolMgr();
        KVMStoragePool pool = poolMgr.getStoragePool(storageFilerTO.getType(), storageFilerTO.getUuid());
        String dstPath = pool.getLocalPath();

        try {
            String filename = libvirtComputingResource.copyVolume(srcIp, username, password, dstPath, srcFile, tmpPath);
            s_logger.debug("Volume Copy Successful");
            if (storageFilerTO.getType() == Storage.StoragePoolType.Filesystem ||
                    storageFilerTO.getType() == Storage.StoragePoolType.NetworkFilesystem) {
                return  new CopyRemoteVolumeAnswer(command, "", filename);
            } else {
                return new Answer(command, false, "Unsupported Storage Pool");
            }

        } catch (final Exception e) {
            s_logger.error("Error while copying file from remote host: "+ e.getMessage());
            return new Answer(command, false, result);
        }
    }

}
