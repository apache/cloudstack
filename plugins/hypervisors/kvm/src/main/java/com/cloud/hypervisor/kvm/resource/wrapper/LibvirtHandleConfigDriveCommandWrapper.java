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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.cloudstack.storage.configdrive.ConfigDriveBuilder;
import org.apache.cloudstack.storage.to.PrimaryDataStoreTO;
import org.apache.log4j.Logger;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.HandleConfigDriveIsoAnswer;
import com.cloud.agent.api.HandleConfigDriveIsoCommand;
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.storage.KVMStoragePool;
import com.cloud.hypervisor.kvm.storage.KVMStoragePoolManager;
import com.cloud.network.element.NetworkElement;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.storage.Storage;
import com.cloud.utils.exception.CloudRuntimeException;

@ResourceWrapper(handles =  HandleConfigDriveIsoCommand.class)
public final class LibvirtHandleConfigDriveCommandWrapper extends CommandWrapper<HandleConfigDriveIsoCommand, Answer, LibvirtComputingResource> {
    private static final Logger LOG = Logger.getLogger(LibvirtHandleConfigDriveCommandWrapper.class);

    @Override
    public Answer execute(final HandleConfigDriveIsoCommand command, final LibvirtComputingResource libvirtComputingResource) {
        String mountPoint = null;

        try {
            if (command.isCreate()) {
                LOG.debug("Creating config drive: " + command.getIsoFile());

                NetworkElement.Location location = NetworkElement.Location.PRIMARY;
                if (command.isHostCachePreferred()) {
                    LOG.debug("Using the KVM host for config drive");
                    mountPoint = libvirtComputingResource.getConfigPath();
                    location = NetworkElement.Location.HOST;
                } else {
                    final KVMStoragePoolManager storagePoolMgr = libvirtComputingResource.getStoragePoolMgr();
                    KVMStoragePool pool = null;
                    String poolUuid = null;
                    Storage.StoragePoolType poolType = null;
                    DataStoreTO dataStoreTO = command.getDestStore();
                    if (dataStoreTO != null) {
                        if (dataStoreTO instanceof PrimaryDataStoreTO) {
                            PrimaryDataStoreTO primaryDataStoreTO = (PrimaryDataStoreTO) dataStoreTO;
                            poolType = primaryDataStoreTO.getPoolType();
                        } else {
                            poolType = Storage.StoragePoolType.NetworkFilesystem;
                        }
                        poolUuid = command.getDestStore().getUuid();
                        pool = storagePoolMgr.getStoragePool(poolType, poolUuid);
                    }

                    if (pool == null || poolType == null) {
                        return new HandleConfigDriveIsoAnswer(command, "Unable to create config drive, Pool " + (poolUuid != null ? poolUuid : "") + " not found");
                    }

                    if (pool.supportsConfigDriveIso()) {
                        LOG.debug("Using the pool: " + poolUuid + " for config drive");
                        mountPoint = pool.getLocalPath();
                    } else if (command.getUseHostCacheOnUnsupportedPool()) {
                        LOG.debug("Config drive for KVM is not supported for pool type: " + poolType.toString() + ", using the KVM host");
                        mountPoint = libvirtComputingResource.getConfigPath();
                        location = NetworkElement.Location.HOST;
                    } else {
                        LOG.debug("Config drive for KVM is not supported for pool type: " + poolType.toString());
                        return new HandleConfigDriveIsoAnswer(command, "Config drive for KVM is not supported for pool type: " + poolType.toString());
                    }
                }

                Path isoPath = Paths.get(mountPoint, command.getIsoFile());
                File isoFile = new File(mountPoint, command.getIsoFile());

                if (command.getIsoData() == null) {
                    return new HandleConfigDriveIsoAnswer(command, "Invalid config drive ISO data received");
                }
                if (isoFile.exists()) {
                    LOG.debug("An old config drive iso already exists");
                }

                Files.createDirectories(isoPath.getParent());
                ConfigDriveBuilder.base64StringToFile(command.getIsoData(), mountPoint, command.getIsoFile());

                return new HandleConfigDriveIsoAnswer(command, location);
            } else {
                LOG.debug("Deleting config drive: " + command.getIsoFile());
                Path configDrivePath = null;

                if (command.isHostCachePreferred()) {
                    // Check and delete config drive in host storage if exists
                    mountPoint = libvirtComputingResource.getConfigPath();
                    configDrivePath = Paths.get(mountPoint, command.getIsoFile());
                    Files.deleteIfExists(configDrivePath);
                } else {
                    final KVMStoragePoolManager storagePoolMgr = libvirtComputingResource.getStoragePoolMgr();
                    KVMStoragePool pool = null;
                    DataStoreTO dataStoreTO = command.getDestStore();
                    if (dataStoreTO != null) {
                        if (dataStoreTO instanceof PrimaryDataStoreTO) {
                            PrimaryDataStoreTO primaryDataStoreTO = (PrimaryDataStoreTO) dataStoreTO;
                            Storage.StoragePoolType poolType = primaryDataStoreTO.getPoolType();
                            pool = storagePoolMgr.getStoragePool(poolType, command.getDestStore().getUuid());
                        } else {
                            pool = storagePoolMgr.getStoragePool(Storage.StoragePoolType.NetworkFilesystem, command.getDestStore().getUuid());
                        }
                    }

                    if (pool != null && pool.supportsConfigDriveIso()) {
                        mountPoint = pool.getLocalPath();
                        configDrivePath = Paths.get(mountPoint, command.getIsoFile());
                        Files.deleteIfExists(configDrivePath);
                    }
                }

                return new HandleConfigDriveIsoAnswer(command);
            }
        } catch (final IOException e) {
            LOG.debug("Failed to handle config drive due to " + e.getMessage(), e);
            return new HandleConfigDriveIsoAnswer(command, "Failed due to exception: " + e.getMessage());
        } catch (final CloudRuntimeException e) {
            LOG.debug("Failed to handle config drive due to " + e.getMessage(), e);
            return new HandleConfigDriveIsoAnswer(command, "Failed due to exception: " + e.toString());
        }
    }
}
