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
import com.cloud.agent.api.CleanupConvertedInstanceDisksCommand;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.resource.LibvirtDomainXMLParser;
import com.cloud.hypervisor.kvm.storage.KVMPhysicalDisk;
import com.cloud.hypervisor.kvm.storage.KVMStoragePool;
import com.cloud.hypervisor.kvm.storage.KVMStoragePoolManager;
import com.cloud.resource.ResourceWrapper;
import com.cloud.resource.ServerResource;

import java.io.File;
import java.util.List;

@ResourceWrapper(handles = CleanupConvertedInstanceDisksCommand.class)
public class LibvirtCleanupConvertedInstanceDisksCommandWrapper extends LibvirtBaseConvertCommandWrapper<CleanupConvertedInstanceDisksCommand, Answer, LibvirtComputingResource> {

    @Override
    public Answer execute(Command command, ServerResource resource) {
        CleanupConvertedInstanceDisksCommand cmd = (CleanupConvertedInstanceDisksCommand) command;
        LibvirtComputingResource serverResource = (LibvirtComputingResource) resource;
        DataStoreTO vmVolumesStore = cmd.getVmVolumesStore();
        String vmVolumesPrefix = cmd.getVmVolumesPrefix();

        final KVMStoragePoolManager storagePoolMgr = serverResource.getStoragePoolMgr();
        KVMStoragePool conversionPool = getTemporaryStoragePool(vmVolumesStore, storagePoolMgr);
        final String conversionPoolPath = conversionPool.getLocalPath();

        try {
            String volumesBasePath = String.format("%s/%s", conversionPoolPath, vmVolumesPrefix);
            String xmlPath = String.format("%s.xml", volumesBasePath);
            boolean xmlExists = new File(xmlPath).exists();

            LibvirtDomainXMLParser xmlParser = xmlExists ? parseMigratedVMXmlDomain(volumesBasePath) : null;
            List<KVMPhysicalDisk> temporaryDisks = xmlExists ?
                    getTemporaryDisksFromParsedXml(conversionPool, xmlParser, volumesBasePath) :
                    getTemporaryDisksWithPrefixFromTemporaryPool(conversionPool, conversionPoolPath, vmVolumesPrefix);

            cleanupDisksAndDomainFromTemporaryLocation(temporaryDisks, conversionPool, vmVolumesPrefix, xmlExists);

        } catch (Exception e) {
            String error = String.format("Error cleaning up converted disks with prefix %s from %s, due to: %s",
                    vmVolumesPrefix, conversionPoolPath, e.getMessage());
            logger.error(error, e);
            return new Answer(command, false, error);
        }

        return new Answer(command);
    }
}
