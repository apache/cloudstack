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
package com.cloud.hypervisor.kvm.storage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;

import javax.annotation.Nonnull;

import org.apache.cloudstack.utils.qemu.QemuImg;
import org.apache.cloudstack.utils.qemu.QemuImgException;
import org.apache.cloudstack.utils.qemu.QemuImgFile;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.libvirt.LibvirtException;

import com.cloud.storage.Storage;
import com.cloud.utils.exception.CloudRuntimeException;
import com.linbit.linstor.api.ApiClient;
import com.linbit.linstor.api.ApiException;
import com.linbit.linstor.api.Configuration;
import com.linbit.linstor.api.DevelopersApi;
import com.linbit.linstor.api.model.ApiCallRc;
import com.linbit.linstor.api.model.ApiCallRcList;
import com.linbit.linstor.api.model.Properties;
import com.linbit.linstor.api.model.ProviderKind;
import com.linbit.linstor.api.model.ResourceDefinition;
import com.linbit.linstor.api.model.ResourceDefinitionModify;
import com.linbit.linstor.api.model.ResourceGroup;
import com.linbit.linstor.api.model.ResourceGroupSpawn;
import com.linbit.linstor.api.model.ResourceMakeAvailable;
import com.linbit.linstor.api.model.ResourceWithVolumes;
import com.linbit.linstor.api.model.StoragePool;
import com.linbit.linstor.api.model.VolumeDefinition;

@StorageAdaptorInfo(storagePoolType=Storage.StoragePoolType.Linstor)
public class LinstorStorageAdaptor implements StorageAdaptor {
    protected Logger logger = LogManager.getLogger(getClass());
    private static final Map<String, KVMStoragePool> MapStorageUuidToStoragePool = new HashMap<>();
    private final String localNodeName;

    private DevelopersApi getLinstorAPI(KVMStoragePool pool) {
        ApiClient client = Configuration.getDefaultApiClient();
        client.setBasePath(pool.getSourceHost());
        return new DevelopersApi(client);
    }

    private String getLinstorRscName(String name) {
        return "cs-" + name;
    }

    private String getHostname() {
        // either there is already some function for that in the agent or a better way.
        ProcessBuilder pb = new ProcessBuilder("/usr/bin/hostname");
        try
        {
            String result;
            Process p = pb.start();
            final BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

            StringJoiner sj = new StringJoiner(System.getProperty("line.separator"));
            reader.lines().iterator().forEachRemaining(sj::add);
            result = sj.toString();

            p.waitFor();
            p.destroy();
            return result.trim();
        } catch (IOException | InterruptedException exc) {
            Thread.currentThread().interrupt();
            throw new CloudRuntimeException("Unable to run '/usr/bin/hostname' command.");
        }
    }

    private void logLinstorAnswer(@Nonnull ApiCallRc answer) {
        if (answer.isError()) {
            logger.error(answer.getMessage());
        } else if (answer.isWarning()) {
            logger.warn(answer.getMessage());
        } else if (answer.isInfo()) {
            logger.info(answer.getMessage());
        }
    }

    private void checkLinstorAnswersThrow(@Nonnull ApiCallRcList answers) {
        answers.forEach(this::logLinstorAnswer);
        if (answers.hasError())
        {
            String errMsg = answers.stream()
                .filter(ApiCallRc::isError)
                .findFirst()
                .map(ApiCallRc::getMessage).orElse("Unknown linstor error");
            throw new CloudRuntimeException(errMsg);
        }
    }

    private void handleLinstorApiAnswers(ApiCallRcList answers, String excMessage) {
        answers.forEach(this::logLinstorAnswer);
        if (answers.hasError()) {
            throw new CloudRuntimeException(excMessage);
        }
    }

    public LinstorStorageAdaptor() {
        localNodeName = getHostname();
    }

    @Override
    public KVMStoragePool getStoragePool(String uuid) {
        return MapStorageUuidToStoragePool.get(uuid);
    }

    @Override
    public KVMStoragePool getStoragePool(String uuid, boolean refreshInfo) {
        logger.debug("Linstor getStoragePool: " + uuid + " -> " + refreshInfo);
        return MapStorageUuidToStoragePool.get(uuid);
    }

    @Override
    public KVMPhysicalDisk getPhysicalDisk(String name, KVMStoragePool pool)
    {
        logger.debug("Linstor: getPhysicalDisk for " + name);
        if (name == null) {
            return null;
        }

        final DevelopersApi api = getLinstorAPI(pool);
        try {
            final String rscName = getLinstorRscName(name);

            List<VolumeDefinition> volumeDefs = api.volumeDefinitionList(rscName, null, null);
            final long size = volumeDefs.isEmpty() ? 0 : volumeDefs.get(0).getSizeKib() * 1024;

            List<ResourceWithVolumes> resources = api.viewResources(
                Collections.emptyList(),
                Collections.singletonList(rscName),
                Collections.emptyList(),
                null,
                null,
                null);
            if (!resources.isEmpty() && !resources.get(0).getVolumes().isEmpty()) {
                final String devPath = resources.get(0).getVolumes().get(0).getDevicePath();
                final KVMPhysicalDisk kvmDisk = new KVMPhysicalDisk(devPath, name, pool);
                kvmDisk.setFormat(QemuImg.PhysicalDiskFormat.RAW);
                kvmDisk.setSize(size);
                kvmDisk.setVirtualSize(size);
                return kvmDisk;
            } else {
                logger.error("Linstor: viewResources didn't return resources or volumes for " + rscName);
                throw new CloudRuntimeException("Linstor: viewResources didn't return resources or volumes.");
            }
        } catch (ApiException apiEx) {
            logger.error(apiEx);
            throw new CloudRuntimeException(apiEx.getBestMessage(), apiEx);
        }
    }

    @Override
    public KVMStoragePool createStoragePool(String name, String host, int port, String path, String userInfo,
                                            Storage.StoragePoolType type, Map<String, String> details)
    {
        logger.debug(String.format(
            "Linstor createStoragePool: name: '%s', host: '%s', path: %s, userinfo: %s", name, host, path, userInfo));
        LinstorStoragePool storagePool = new LinstorStoragePool(name, host, port, userInfo, type, this);

        MapStorageUuidToStoragePool.put(name, storagePool);

        return storagePool;
    }

    @Override
    public boolean deleteStoragePool(String uuid) {
        return MapStorageUuidToStoragePool.remove(uuid) != null;
    }

    @Override
    public boolean deleteStoragePool(KVMStoragePool pool) {
        return deleteStoragePool(pool.getUuid());
    }

    @Override
    public KVMPhysicalDisk createPhysicalDisk(String name, KVMStoragePool pool, QemuImg.PhysicalDiskFormat format,
                                              Storage.ProvisioningType provisioningType, long size, byte[] passphrase)
    {
        final String rscName = getLinstorRscName(name);
        LinstorStoragePool lpool = (LinstorStoragePool) pool;
        final DevelopersApi api = getLinstorAPI(pool);

        try {
            List<ResourceDefinition> definitionList = api.resourceDefinitionList(
                Collections.singletonList(rscName), null, null, null);

            if (definitionList.isEmpty()) {
                ResourceGroupSpawn rgSpawn = new ResourceGroupSpawn();
                rgSpawn.setResourceDefinitionName(rscName);
                rgSpawn.addVolumeSizesItem(size / 1024); // linstor uses KiB

                logger.debug("Linstor: Spawn resource " + rscName);
                ApiCallRcList answers = api.resourceGroupSpawn(lpool.getResourceGroup(), rgSpawn);
                handleLinstorApiAnswers(answers, "Linstor: Unable to spawn resource.");
            }

            // query linstor for the device path
            List<ResourceWithVolumes> resources = api.viewResources(
                Collections.emptyList(),
                Collections.singletonList(rscName),
                Collections.emptyList(),
                null,
                null,
                null);

            // TODO make available on node

            if (!resources.isEmpty() && !resources.get(0).getVolumes().isEmpty()) {
                final String devPath = resources.get(0).getVolumes().get(0).getDevicePath();
                logger.info("Linstor: Created drbd device: " + devPath);
                final KVMPhysicalDisk kvmDisk = new KVMPhysicalDisk(devPath, name, pool);
                kvmDisk.setFormat(QemuImg.PhysicalDiskFormat.RAW);
                return kvmDisk;
            } else {
                logger.error("Linstor: viewResources didn't return resources or volumes.");
                throw new CloudRuntimeException("Linstor: viewResources didn't return resources or volumes.");
            }
        } catch (ApiException apiEx) {
            throw new CloudRuntimeException(apiEx.getBestMessage(), apiEx);
        }
    }

    @Override
    public boolean connectPhysicalDisk(String volumePath, KVMStoragePool pool, Map<String, String> details)
    {
        logger.debug(String.format("Linstor: connectPhysicalDisk %s:%s -> %s", pool.getUuid(), volumePath, details));
        if (volumePath == null) {
            logger.warn("volumePath is null, ignoring");
            return false;
        }

        final DevelopersApi api = getLinstorAPI(pool);
        try
        {
            final String rscName = getLinstorRscName(volumePath);

            ResourceMakeAvailable rma = new ResourceMakeAvailable();
            ApiCallRcList answers = api.resourceMakeAvailableOnNode(rscName, localNodeName, rma);
            checkLinstorAnswersThrow(answers);

            // allow 2 primaries for live migration, should be removed by disconnect on the other end
            ResourceDefinitionModify rdm = new ResourceDefinitionModify();
            Properties props = new Properties();
            props.put("DrbdOptions/Net/allow-two-primaries", "yes");
            rdm.setOverrideProps(props);
            answers = api.resourceDefinitionModify(rscName, rdm);
            if (answers.hasError()) {
                logger.error("Unable to set 'allow-two-primaries' on " + rscName);
                throw new CloudRuntimeException(answers.get(0).getMessage());
            }
        } catch (ApiException apiEx) {
            logger.error(apiEx);
            throw new CloudRuntimeException(apiEx.getBestMessage(), apiEx);
        }
        return true;
    }

    @Override
    public boolean disconnectPhysicalDisk(String volumePath, KVMStoragePool pool)
    {
        logger.debug("Linstor: disconnectPhysicalDisk " + pool.getUuid() + ":" + volumePath);
        return true;
    }

    @Override
    public boolean disconnectPhysicalDisk(Map<String, String> volumeToDisconnect)
    {
        return false;
    }

    private Optional<ResourceWithVolumes> getResourceByPath(final List<ResourceWithVolumes> resources, String path) {
        return resources.stream()
            .filter(rsc -> rsc.getVolumes().stream()
                .anyMatch(v -> v.getDevicePath().equals(path)))
            .findFirst();
    }

    /**
     * disconnectPhysicalDiskByPath is called after e.g. a live migration.
     * The problem is we have no idea just from the path to which linstor-controller
     * this resource would belong to. But as it should be highly unlikely that someone
     * uses more than one linstor-controller to manage resource on the same kvm host.
     * We will just take the first stored storagepool.
     */
    @Override
    public boolean disconnectPhysicalDiskByPath(String localPath)
    {
        // get first storage pool from the map, as we don't know any better:
        Optional<KVMStoragePool> optFirstPool = MapStorageUuidToStoragePool.values().stream().findFirst();
        if (optFirstPool.isPresent())
        {
            logger.debug("Linstor: disconnectPhysicalDiskByPath " + localPath);
            final KVMStoragePool pool = optFirstPool.get();

            logger.debug("Linstor: Using storpool: " + pool.getUuid());
            final DevelopersApi api = getLinstorAPI(pool);

            try
            {
                List<ResourceWithVolumes> resources = api.viewResources(
                    Collections.singletonList(localNodeName),
                    null,
                    null,
                    null,
                    null,
                    null);

                Optional<ResourceWithVolumes> rsc = getResourceByPath(resources, localPath);

                if (rsc.isPresent())
                {
                    ResourceDefinitionModify rdm = new ResourceDefinitionModify();
                    rdm.deleteProps(Collections.singletonList("DrbdOptions/Net/allow-two-primaries"));
                    ApiCallRcList answers = api.resourceDefinitionModify(rsc.get().getName(), rdm);
                    if (answers.hasError())
                    {
                        logger.error("Failed to remove 'allow-two-primaries' on " + rsc.get().getName());
                        throw new CloudRuntimeException(answers.get(0).getMessage());
                    }

                    return true;
                }
                logger.warn("Linstor: Couldn't find resource for this path: " + localPath);
            } catch (ApiException apiEx) {
                logger.error(apiEx);
                throw new CloudRuntimeException(apiEx.getBestMessage(), apiEx);
            }
        }
        return false;
    }

    @Override
    public boolean deletePhysicalDisk(String name, KVMStoragePool pool, Storage.ImageFormat format)
    {
        logger.debug("Linstor: deletePhysicalDisk " + name);
        final DevelopersApi api = getLinstorAPI(pool);

        try {
            final String rscName = getLinstorRscName(name);
            logger.debug("Linstor: delete resource definition " + rscName);
            ApiCallRcList answers = api.resourceDefinitionDelete(rscName);
            handleLinstorApiAnswers(answers, "Linstor: Unable to delete resource definition " + rscName);
        } catch (ApiException apiEx) {
            throw new CloudRuntimeException(apiEx.getBestMessage(), apiEx);
        }
        return true;
    }

    @Override
    public KVMPhysicalDisk createDiskFromTemplate(
        KVMPhysicalDisk template,
        String name,
        QemuImg.PhysicalDiskFormat format,
        Storage.ProvisioningType provisioningType,
        long size,
        KVMStoragePool destPool,
        int timeout,
        byte[] passphrase)
    {
        logger.info("Linstor: createDiskFromTemplate");
        return copyPhysicalDisk(template, name, destPool, timeout);
    }

    @Override
    public List<KVMPhysicalDisk> listPhysicalDisks(String storagePoolUuid, KVMStoragePool pool)
    {
        throw new UnsupportedOperationException("Listing disks is not supported for this configuration.");
    }

    @Override
    public KVMPhysicalDisk createTemplateFromDisk(
        KVMPhysicalDisk disk,
        String name,
        QemuImg.PhysicalDiskFormat format,
        long size,
        KVMStoragePool destPool)
    {
        throw new UnsupportedOperationException("Copying a template from disk is not supported in this configuration.");
    }

    @Override
    public KVMPhysicalDisk copyPhysicalDisk(KVMPhysicalDisk disk, String name, KVMStoragePool destPool, int timeout) {
        return copyPhysicalDisk(disk, name, destPool, timeout, null, null, null);
    }

    @Override
    public KVMPhysicalDisk copyPhysicalDisk(KVMPhysicalDisk disk, String name, KVMStoragePool destPools, int timeout, byte[] srcPassphrase, byte[] destPassphrase, Storage.ProvisioningType provisioningType)
    {
        logger.debug("Linstor: copyPhysicalDisk");
        final QemuImg.PhysicalDiskFormat sourceFormat = disk.getFormat();
        final String sourcePath = disk.getPath();

        final QemuImgFile srcFile = new QemuImgFile(sourcePath, sourceFormat);

        final KVMPhysicalDisk dstDisk = destPools.createPhysicalDisk(
            name, QemuImg.PhysicalDiskFormat.RAW, Storage.ProvisioningType.FAT, disk.getVirtualSize(), null);

        final QemuImgFile destFile = new QemuImgFile(dstDisk.getPath());
        destFile.setFormat(dstDisk.getFormat());
        destFile.setSize(disk.getVirtualSize());

        try {
            final QemuImg qemu = new QemuImg(timeout);
            qemu.convert(srcFile, destFile);
        } catch (QemuImgException | LibvirtException e) {
            logger.error(e);
            destPools.deletePhysicalDisk(name, Storage.ImageFormat.RAW);
            throw new CloudRuntimeException("Failed to copy " + disk.getPath() + " to " + name);
        }

        return dstDisk;
    }

    @Override
    public boolean refresh(KVMStoragePool pool)
    {
        logger.debug("Linstor: refresh");
        return true;
    }

    @Override
    public boolean createFolder(String uuid, String path) {
        return createFolder(uuid, path, null);
    }

    @Override
    public boolean createFolder(String uuid, String path, String localPath) {
        throw new UnsupportedOperationException("A folder cannot be created in this configuration.");
    }

    @Override
    public KVMPhysicalDisk createDiskFromTemplateBacking(
        KVMPhysicalDisk template,
        String name,
        QemuImg.PhysicalDiskFormat format,
        long size,
        KVMStoragePool destPool,
        int timeout, byte[] passphrase)
    {
        logger.debug("Linstor: createDiskFromTemplateBacking");
        return null;
    }

    @Override
    public KVMPhysicalDisk createTemplateFromDirectDownloadFile(String templateFilePath, String destTemplatePath,
                                                                KVMStoragePool destPool, Storage.ImageFormat format,
                                                                int timeout)
    {
        logger.debug("Linstor: createTemplateFromDirectDownloadFile");
        return null;
    }

    public long getCapacity(LinstorStoragePool pool) {
        DevelopersApi linstorApi = getLinstorAPI(pool);
        final String rscGroupName = pool.getResourceGroup();
        try {
            List<ResourceGroup> rscGrps = linstorApi.resourceGroupList(
                Collections.singletonList(rscGroupName),
                null,
                null,
                null);

            if (rscGrps.isEmpty()) {
                final String errMsg = String.format("Linstor: Resource group '%s' not found", rscGroupName);
                logger.error(errMsg);
                throw new CloudRuntimeException(errMsg);
            }

            List<StoragePool> storagePools = linstorApi.viewStoragePools(
                Collections.emptyList(),
                rscGrps.get(0).getSelectFilter().getStoragePoolList(),
                null,
                null,
                null
            );

            final long capacity = storagePools.stream()
                .filter(sp -> sp.getProviderKind() != ProviderKind.DISKLESS)
                .mapToLong(sp -> sp.getTotalCapacity() != null ? sp.getTotalCapacity() : 0)
                .sum() * 1024;  // linstor uses kiB
            logger.debug("Linstor: GetCapacity() -> " + capacity);
            return capacity;
        } catch (ApiException apiEx) {
            logger.error(apiEx.getMessage());
            throw new CloudRuntimeException(apiEx.getBestMessage(), apiEx);
        }
    }

    public long getAvailable(LinstorStoragePool pool) {
        DevelopersApi linstorApi = getLinstorAPI(pool);
        final String rscGroupName = pool.getResourceGroup();
        try {
            List<ResourceGroup> rscGrps = linstorApi.resourceGroupList(
                Collections.singletonList(rscGroupName),
                null,
                null,
                null);

            if (rscGrps.isEmpty()) {
                final String errMsg = String.format("Linstor: Resource group '%s' not found", rscGroupName);
                logger.error(errMsg);
                throw new CloudRuntimeException(errMsg);
            }

            List<StoragePool> storagePools = linstorApi.viewStoragePools(
                Collections.emptyList(),
                rscGrps.get(0).getSelectFilter().getStoragePoolList(),
                null,
                null,
                null
            );

            final long free = storagePools.stream()
                .filter(sp -> sp.getProviderKind() != ProviderKind.DISKLESS)
                .mapToLong(StoragePool::getFreeCapacity).sum() * 1024;  // linstor uses KiB

            logger.debug("Linstor: getAvailable() -> " + free);
            return free;
        } catch (ApiException apiEx) {
            logger.error(apiEx.getMessage());
            throw new CloudRuntimeException(apiEx.getBestMessage(), apiEx);
        }
    }

    public long getUsed(LinstorStoragePool pool) {
        DevelopersApi linstorApi = getLinstorAPI(pool);
        final String rscGroupName = pool.getResourceGroup();
        try {
            List<ResourceGroup> rscGrps = linstorApi.resourceGroupList(
                Collections.singletonList(rscGroupName),
                null,
                null,
                null);

            if (rscGrps.isEmpty()) {
                final String errMsg = String.format("Linstor: Resource group '%s' not found", rscGroupName);
                logger.error(errMsg);
                throw new CloudRuntimeException(errMsg);
            }

            List<StoragePool> storagePools = linstorApi.viewStoragePools(
                Collections.emptyList(),
                rscGrps.get(0).getSelectFilter().getStoragePoolList(),
                null,
                null,
                null
            );

            final long used = storagePools.stream()
                .filter(sp -> sp.getProviderKind() != ProviderKind.DISKLESS)
                .mapToLong(sp -> sp.getTotalCapacity() - sp.getFreeCapacity()).sum() * 1024; // linstor uses Kib
            logger.debug("Linstor: getUsed() -> " + used);
            return used;
        } catch (ApiException apiEx) {
            logger.error(apiEx.getMessage());
            throw new CloudRuntimeException(apiEx.getBestMessage(), apiEx);
        }
    }
}
