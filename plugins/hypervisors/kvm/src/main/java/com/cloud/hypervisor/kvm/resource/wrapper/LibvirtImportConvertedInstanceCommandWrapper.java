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

import java.util.List;

import com.cloud.agent.api.Command;
import com.cloud.resource.ServerResource;
import org.apache.cloudstack.vm.UnmanagedInstanceTO;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.ImportConvertedInstanceAnswer;
import com.cloud.agent.api.ImportConvertedInstanceCommand;
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.agent.api.to.NfsTO;
import com.cloud.agent.api.to.RemoteInstanceTO;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.resource.LibvirtDomainXMLParser;
import com.cloud.hypervisor.kvm.storage.KVMPhysicalDisk;
import com.cloud.hypervisor.kvm.storage.KVMStoragePool;
import com.cloud.hypervisor.kvm.storage.KVMStoragePoolManager;
import com.cloud.resource.ResourceWrapper;

@ResourceWrapper(handles =  ImportConvertedInstanceCommand.class)
public class LibvirtImportConvertedInstanceCommandWrapper extends LibvirtBaseConvertCommandWrapper<ImportConvertedInstanceCommand, Answer, LibvirtComputingResource> {

    @Override
    public Answer execute(Command command, ServerResource resource) {
        ImportConvertedInstanceCommand cmd = (ImportConvertedInstanceCommand) command;
        LibvirtComputingResource serverResource = (LibvirtComputingResource) resource;
        RemoteInstanceTO sourceInstance = cmd.getSourceInstance();
        Hypervisor.HypervisorType sourceHypervisorType = sourceInstance.getHypervisorType();
        String sourceInstanceName = sourceInstance.getInstanceName();
        List<String> destinationStoragePools = cmd.getDestinationStoragePools();
        DataStoreTO conversionTemporaryLocation = cmd.getConversionTemporaryLocation();
        final String temporaryConvertUuid = cmd.getTemporaryConvertUuid();
        final boolean forceConvertToPool = cmd.isForceConvertToPool();

        final KVMStoragePoolManager storagePoolMgr = serverResource.getStoragePoolMgr();
        KVMStoragePool temporaryStoragePool = getTemporaryStoragePool(conversionTemporaryLocation, storagePoolMgr);
        final String temporaryConvertPath = temporaryStoragePool.getLocalPath();

        try {
            String convertedBasePath = String.format("%s/%s", temporaryConvertPath, temporaryConvertUuid);
            LibvirtDomainXMLParser xmlParser = parseMigratedVMXmlDomain(convertedBasePath);

            List<KVMPhysicalDisk> temporaryDisks = xmlParser == null ?
                    getTemporaryDisksWithPrefixFromTemporaryPool(temporaryStoragePool, temporaryConvertPath, temporaryConvertUuid) :
                    getTemporaryDisksFromParsedXml(temporaryStoragePool, xmlParser, convertedBasePath);

            List<KVMPhysicalDisk> disks;
            if (forceConvertToPool) {
                // Force flag to use the conversion path, no need to move disks
                disks = temporaryDisks;
            } else {
                disks = moveTemporaryDisksToDestination(temporaryDisks,
                        destinationStoragePools, storagePoolMgr);
                cleanupDisksAndDomainFromTemporaryLocation(temporaryDisks, temporaryStoragePool, temporaryConvertUuid, true);
            }

            UnmanagedInstanceTO convertedInstanceTO = getConvertedUnmanagedInstance(temporaryConvertUuid,
                    disks, xmlParser);
            return new ImportConvertedInstanceAnswer(cmd, convertedInstanceTO);
        } catch (Exception e) {
            String error = String.format("Error converting instance %s from %s, due to: %s",
                    sourceInstanceName, sourceHypervisorType, e.getMessage());
            logger.error(error, e);
            return new ImportConvertedInstanceAnswer(cmd, false, error);
        } finally {
            if (conversionTemporaryLocation instanceof NfsTO) {
                logger.debug("Cleaning up secondary storage temporary location");
                storagePoolMgr.deleteStoragePool(temporaryStoragePool.getType(), temporaryStoragePool.getUuid());
            }
        }
    }
}
