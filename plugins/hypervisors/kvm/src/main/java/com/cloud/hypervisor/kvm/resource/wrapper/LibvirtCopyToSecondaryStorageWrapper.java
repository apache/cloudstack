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

import static org.apache.cloudstack.diagnostics.DiagnosticsHelper.setDirFilePermissions;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.cloudstack.diagnostics.CopyToSecondaryStorageAnswer;
import org.apache.cloudstack.diagnostics.CopyToSecondaryStorageCommand;
import org.apache.cloudstack.diagnostics.DiagnosticsService;
import org.apache.log4j.Logger;

import com.cloud.agent.api.Answer;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.storage.KVMStoragePool;
import com.cloud.hypervisor.kvm.storage.KVMStoragePoolManager;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.utils.ssh.SshHelper;

@ResourceWrapper(handles = CopyToSecondaryStorageCommand.class)
public class LibvirtCopyToSecondaryStorageWrapper extends CommandWrapper<CopyToSecondaryStorageCommand, Answer, LibvirtComputingResource> {
    public static final Logger LOGGER = Logger.getLogger(LibvirtCopyToSecondaryStorageWrapper.class);

    @Override
    public Answer execute(CopyToSecondaryStorageCommand command, LibvirtComputingResource libvirtResource) {

        String diagnosticsZipFile = command.getFileName();
        String vmSshIp = command.getSystemVmIp();
        String secondaryStorageUrl = command.getSecondaryStorageUrl();

        KVMStoragePoolManager storagePoolMgr = libvirtResource.getStoragePoolMgr();
        KVMStoragePool secondaryPool;

        boolean success;

        secondaryPool = storagePoolMgr.getStoragePoolByURI(secondaryStorageUrl);
        String mountPoint = secondaryPool.getLocalPath();

        // /mnt/SecStorage/uuid/diagnostics_data
        String dataDirectoryInSecondaryStore = String.format("%s/%s", mountPoint, DiagnosticsService.DIAGNOSTICS_DIRECTORY);
        try {
            File dataDirectory = new File(dataDirectoryInSecondaryStore);
            boolean existsInSecondaryStore = dataDirectory.exists() || dataDirectory.mkdir();

            // Modify directory file permissions
            Path path = Paths.get(dataDirectory.getAbsolutePath());
            setDirFilePermissions(path);
            if (existsInSecondaryStore) {
                LOGGER.info(String.format("Copying %s from %s to secondary store %s", diagnosticsZipFile, vmSshIp, secondaryStorageUrl));
                int port = Integer.valueOf(LibvirtComputingResource.DEFAULTDOMRSSHPORT);
                File permKey = new File(LibvirtComputingResource.SSHPRVKEYPATH);
                SshHelper.scpFrom(vmSshIp, port, "root", permKey, dataDirectoryInSecondaryStore, diagnosticsZipFile);
            }
            // Verify File copy to Secondary Storage
            File fileInSecondaryStore = new File(dataDirectoryInSecondaryStore + diagnosticsZipFile.replace("/root", ""));
            if (fileInSecondaryStore.exists()) {
                return new CopyToSecondaryStorageAnswer(command, true, "File copied to secondary storage successfully");
            } else {
                return new CopyToSecondaryStorageAnswer(command, false, "Zip file " + diagnosticsZipFile.replace("/root/", "") + "not found in secondary storage");
            }

        } catch (Exception e) {
            return new CopyToSecondaryStorageAnswer(command, false, e.getMessage());
        } finally {
            // unmount secondary storage from hypervisor host
            secondaryPool.delete();
        }
    }
}
