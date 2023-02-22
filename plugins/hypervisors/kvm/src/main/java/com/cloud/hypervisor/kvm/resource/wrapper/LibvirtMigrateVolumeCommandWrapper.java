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
import com.cloud.agent.api.storage.MigrateVolumeAnswer;
import com.cloud.agent.api.storage.MigrateVolumeCommand;
import com.cloud.agent.api.to.DiskTO;
import com.cloud.exception.InternalErrorException;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.resource.LibvirtDomainXMLParser;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef;
import com.cloud.hypervisor.kvm.storage.KVMPhysicalDisk;
import com.cloud.hypervisor.kvm.storage.KVMStoragePool;
import com.cloud.hypervisor.kvm.storage.KVMStoragePoolManager;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.cloud.storage.Storage;
import org.apache.cloudstack.storage.datastore.client.ScaleIOGatewayClient;
import org.apache.cloudstack.storage.datastore.util.ScaleIOUtil;
import org.apache.cloudstack.storage.to.PrimaryDataStoreTO;
import org.apache.cloudstack.storage.to.VolumeObjectTO;
import org.apache.log4j.Logger;
import org.libvirt.Connect;
import org.libvirt.Domain;
import org.libvirt.DomainInfo;
import org.libvirt.TypedParameter;
import org.libvirt.LibvirtException;
import org.libvirt.event.BlockJobListener;
import org.libvirt.event.BlockJobStatus;
import org.libvirt.event.BlockJobType;

@ResourceWrapper(handles =  MigrateVolumeCommand.class)
public final class LibvirtMigrateVolumeCommandWrapper extends CommandWrapper<MigrateVolumeCommand, Answer, LibvirtComputingResource> {
    private static final Logger LOGGER = Logger.getLogger(LibvirtMigrateVolumeCommandWrapper.class);

    @Override
    public Answer execute(final MigrateVolumeCommand command, final LibvirtComputingResource libvirtComputingResource) {
        LOGGER.info("I'm here HARIIIII");

        VolumeObjectTO srcVolumeObjectTO = (VolumeObjectTO)command.getSrcData();
        PrimaryDataStoreTO srcPrimaryDataStore = (PrimaryDataStoreTO)srcVolumeObjectTO.getDataStore();

        MigrateVolumeAnswer answer;
        if (srcPrimaryDataStore.getPoolType().equals(Storage.StoragePoolType.PowerFlex)) {
            answer = migrateVolumeInternal(command, libvirtComputingResource);
        } else {
            answer = migrateRegularVolume(command, libvirtComputingResource);
        }

        return answer;
    }

    private MigrateVolumeAnswer migrateVolumeInternal (final MigrateVolumeCommand command, final LibvirtComputingResource libvirtComputingResource) {
        VolumeObjectTO srcVolumeObjectTO = (VolumeObjectTO)command.getSrcData();
        PrimaryDataStoreTO srcPrimaryDataStore = (PrimaryDataStoreTO)srcVolumeObjectTO.getDataStore();
        final String vmName = srcVolumeObjectTO.getVmName();
        LOGGER.info("HARI VM name: "+ vmName);

        VolumeObjectTO destVolumeObjectTO = (VolumeObjectTO)command.getDestData();
        PrimaryDataStoreTO destPrimaryDataStore = (PrimaryDataStoreTO)destVolumeObjectTO.getDataStore();

        String srcPath = srcVolumeObjectTO.getPath();
        String destPath = destVolumeObjectTO.getPath();

        Map<String, String> destDetails = command.getDestDetails();

        final String srcVolumeId = ScaleIOUtil.getVolumePath(srcVolumeObjectTO.getPath());
        LOGGER.info("HARI Source volume ID: "+ srcVolumeId);

        final String destVolumeId = ScaleIOUtil.getVolumePath(destVolumeObjectTO.getPath());
        LOGGER.info("HARI destination volume ID: "+ destVolumeId);

        final String destSystemId = destDetails.get(ScaleIOGatewayClient.STORAGE_POOL_SYSTEM_ID);
        LOGGER.info("HARI destination system ID: "+ destSystemId);


        final String destDiskFileName = ScaleIOUtil.DISK_NAME_PREFIX + destSystemId + "-" + destVolumeId;
        final String diskFilePath = ScaleIOUtil.DISK_PATH + File.separator + destDiskFileName;

        Domain dm = null;
        try {
            final LibvirtUtilitiesHelper libvirtUtilitiesHelper = libvirtComputingResource.getLibvirtUtilitiesHelper();
            Connect conn = libvirtUtilitiesHelper.getConnection();
            dm = libvirtComputingResource.getDomain(conn, vmName);

            if (dm == null) {
                return new MigrateVolumeAnswer(command, false,
                        "Migrate volume failed due to can not find vm: " + vmName, null);
            }

            DomainInfo.DomainState domainState = dm.getInfo().state ;
            if (domainState != DomainInfo.DomainState.VIR_DOMAIN_RUNNING) {
                return new MigrateVolumeAnswer(command, false,
                        "Migrate volume failed due to VM is not running: " + vmName + " with domainState = " + domainState, null);
            }

            final LibvirtDomainXMLParser parser = new LibvirtDomainXMLParser();
            final String domXml = dm.getXMLDesc(0);
            parser.parseDomainXML(domXml);
            LOGGER.info(String.format("VM [%s] with XML configuration [%s] will be migrated to host.", vmName, domXml));

            List<LibvirtVMDef.DiskDef> disks = parser.getDisks();
            LibvirtVMDef.DiskDef diskdef = null;
            for (final LibvirtVMDef.DiskDef disk : disks) {
                final String file = disk.getDiskPath();
                LOGGER.info("HARIIII : " + file);
                if (file != null && file.contains(srcVolumeId)) {
                    diskdef = disk;
                    break;
                }
            }
            if (diskdef == null) {
                throw new InternalErrorException("disk: " + srcPath + " is not attached before");
            }
            diskdef.setDiskPath(diskFilePath);
            LOGGER.info("HARIIII Destination xml : " + diskdef.toString());
            dm.blockCopy(srcPath, diskdef.toString(), new TypedParameter[]{}, 0);
            BlockJobListener listener = new BlockJobListener() {
                @Override
                public void onEvent(Domain domain, String diskPath, BlockJobType type, BlockJobStatus status) {

                }
            };

            return new MigrateVolumeAnswer(command, true, null, destPath);
        } catch (LibvirtException e) {
            String msg = "Migrate volume failed due to " + e.toString();
            LOGGER.warn(msg, e);
            return new MigrateVolumeAnswer(command, false, msg, null);
        } catch (InternalErrorException e) {
            throw new RuntimeException(e);
        } finally {
            if (dm != null) {
                try {
                    dm.free();
                } catch (LibvirtException l) {
                    LOGGER.trace("Ignoring libvirt error.", l);
                };
            }
        }
    }
    private MigrateVolumeAnswer migrateRegularVolume(final MigrateVolumeCommand command, final LibvirtComputingResource libvirtComputingResource) {
        KVMStoragePoolManager storagePoolManager = libvirtComputingResource.getStoragePoolMgr();

        VolumeObjectTO srcVolumeObjectTO = (VolumeObjectTO)command.getSrcData();
        PrimaryDataStoreTO srcPrimaryDataStore = (PrimaryDataStoreTO)srcVolumeObjectTO.getDataStore();

        Map<String, String> srcDetails = command.getSrcDetails();
        String srcPath = srcDetails != null ? srcDetails.get(DiskTO.IQN) : srcVolumeObjectTO.getPath();

        VolumeObjectTO destVolumeObjectTO = (VolumeObjectTO)command.getDestData();
        PrimaryDataStoreTO destPrimaryDataStore = (PrimaryDataStoreTO)destVolumeObjectTO.getDataStore();

        Map<String, String> destDetails = command.getDestDetails();

        String destPath = destDetails != null && destDetails.get(DiskTO.IQN) != null ? destDetails.get(DiskTO.IQN) :
                (destVolumeObjectTO.getPath() != null ? destVolumeObjectTO.getPath() : UUID.randomUUID().toString());

        try {
            storagePoolManager.connectPhysicalDisk(srcPrimaryDataStore.getPoolType(), srcPrimaryDataStore.getUuid(), srcPath, srcDetails);

            KVMPhysicalDisk srcPhysicalDisk = storagePoolManager.getPhysicalDisk(srcPrimaryDataStore.getPoolType(), srcPrimaryDataStore.getUuid(), srcPath);

            KVMStoragePool destPrimaryStorage = storagePoolManager.getStoragePool(destPrimaryDataStore.getPoolType(), destPrimaryDataStore.getUuid());

            storagePoolManager.connectPhysicalDisk(destPrimaryDataStore.getPoolType(), destPrimaryDataStore.getUuid(), destPath, destDetails);

            storagePoolManager.copyPhysicalDisk(srcPhysicalDisk, destPath, destPrimaryStorage, command.getWaitInMillSeconds());
        }
        catch (Exception ex) {
            return new MigrateVolumeAnswer(command, false, ex.getMessage(), null);
        }
        finally {
            try {
                storagePoolManager.disconnectPhysicalDisk(destPrimaryDataStore.getPoolType(), destPrimaryDataStore.getUuid(), destPath);
            }
            catch (Exception e) {
                LOGGER.warn("Unable to disconnect from the destination device.", e);
            }

            try {
                storagePoolManager.disconnectPhysicalDisk(srcPrimaryDataStore.getPoolType(), srcPrimaryDataStore.getUuid(), srcPath);
            }
            catch (Exception e) {
                LOGGER.warn("Unable to disconnect from the source device.", e);
            }
        }

        return new MigrateVolumeAnswer(command, true, null, destPath);
    }
}
