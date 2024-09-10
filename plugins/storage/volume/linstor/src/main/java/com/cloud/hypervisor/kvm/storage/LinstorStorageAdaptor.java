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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nonnull;

import com.cloud.storage.Storage;
import com.cloud.utils.exception.CloudRuntimeException;

import org.apache.cloudstack.storage.datastore.util.LinstorUtil;
import org.apache.cloudstack.utils.qemu.QemuImg;
import org.apache.cloudstack.utils.qemu.QemuImgException;
import org.apache.cloudstack.utils.qemu.QemuImgFile;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.libvirt.LibvirtException;

import com.linbit.linstor.api.ApiClient;
import com.linbit.linstor.api.ApiConsts;
import com.linbit.linstor.api.ApiException;
import com.linbit.linstor.api.Configuration;
import com.linbit.linstor.api.DevelopersApi;
import com.linbit.linstor.api.model.ApiCallRc;
import com.linbit.linstor.api.model.ApiCallRcList;
import com.linbit.linstor.api.model.Properties;
import com.linbit.linstor.api.model.ProviderKind;
import com.linbit.linstor.api.model.Resource;
import com.linbit.linstor.api.model.ResourceConnectionModify;
import com.linbit.linstor.api.model.ResourceDefinition;
import com.linbit.linstor.api.model.ResourceGroupSpawn;
import com.linbit.linstor.api.model.ResourceMakeAvailable;
import com.linbit.linstor.api.model.ResourceWithVolumes;
import com.linbit.linstor.api.model.StoragePool;
import com.linbit.linstor.api.model.Volume;
import com.linbit.linstor.api.model.VolumeDefinition;

public class LinstorStorageAdaptor implements StorageAdaptor {
    protected Logger logger = LogManager.getLogger(getClass());
    private static final Map<String, KVMStoragePool> MapStorageUuidToStoragePool = new HashMap<>();
    private final String localNodeName;

    private DevelopersApi getLinstorAPI(KVMStoragePool pool) {
        ApiClient client = Configuration.getDefaultApiClient();
        client.setBasePath(pool.getSourceHost());
        return new DevelopersApi(client);
    }

    @Override
    public Storage.StoragePoolType getStoragePoolType() {
        return Storage.StoragePoolType.Linstor;
    }

    private static String getLinstorRscName(String name) {
        return LinstorUtil.RSC_PREFIX + name;
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

    private void logLinstorAnswers(@Nonnull ApiCallRcList answers) {
        answers.forEach(this::logLinstorAnswer);
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
        localNodeName = LinstorStoragePool.getHostname();
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

            final String devicePath = LinstorUtil.getDevicePath(api, rscName);
            final KVMPhysicalDisk kvmDisk = new KVMPhysicalDisk(devicePath, name, pool);
            kvmDisk.setFormat(QemuImg.PhysicalDiskFormat.RAW);
            kvmDisk.setSize(size);
            kvmDisk.setVirtualSize(size);
            return kvmDisk;
        } catch (ApiException apiEx) {
            logger.error(apiEx);
            throw new CloudRuntimeException(apiEx.getBestMessage(), apiEx);
        }
    }

    @Override
    public KVMStoragePool createStoragePool(String name, String host, int port, String path, String userInfo,
                                            Storage.StoragePoolType type, Map<String, String> details)
    {
        logger.debug("Linstor createStoragePool: name: '{}', host: '{}', path: {}, userinfo: {}", name, host, path, userInfo);
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

    private void makeResourceAvailable(DevelopersApi api, String rscName, boolean diskfull) throws ApiException
    {
        ResourceMakeAvailable rma = new ResourceMakeAvailable();
        rma.diskful(diskfull);
        ApiCallRcList answers = api.resourceMakeAvailableOnNode(rscName, localNodeName, rma);
        handleLinstorApiAnswers(answers,
            String.format("Linstor: Unable to make resource %s available on node: %s", rscName, localNodeName));
    }

    /**
     * createPhysicalDisk will check if the resource wasn't yet created and do so, also it will make sure
     * it is accessible from this node (MakeAvailable).
     */
    @Override
    public KVMPhysicalDisk createPhysicalDisk(String name, KVMStoragePool pool, QemuImg.PhysicalDiskFormat format,
                                              Storage.ProvisioningType provisioningType, long size, byte[] passphrase)
    {
        logger.debug("Linstor.createPhysicalDisk: {};{}", name, format);
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

                logger.info("Linstor: Spawn resource " + rscName);
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

            makeResourceAvailable(api, rscName, false);

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
            logger.error("Linstor.createPhysicalDisk: ApiException: {}", apiEx.getBestMessage());
            throw new CloudRuntimeException(apiEx.getBestMessage(), apiEx);
        }
    }

    /**
     * Checks if the given resource is in use by drbd on any host and
     * if so set the drbd option allow-two-primaries
     * @param api linstor api object
     * @param rscName resource name to set allow-two-primaries if in use
     * @throws ApiException if any problem connecting to the Linstor controller
     */
    private void allow2PrimariesIfInUse(DevelopersApi api, String rscName) throws ApiException {
        String inUseNode = LinstorUtil.isResourceInUse(api, rscName);
        if (inUseNode != null && !inUseNode.equalsIgnoreCase(localNodeName)) {
            // allow 2 primaries for live migration, should be removed by disconnect on the other end
            ResourceConnectionModify rcm = new ResourceConnectionModify();
            Properties props = new Properties();
            props.put("DrbdOptions/Net/allow-two-primaries", "yes");
            props.put("DrbdOptions/Net/protocol", "C");
            rcm.setOverrideProps(props);
            ApiCallRcList answers = api.resourceConnectionModify(rscName, inUseNode, localNodeName, rcm);
            if (answers.hasError()) {
                logger.error("Unable to set protocol C and 'allow-two-primaries' on {}/{}/{}",
                        inUseNode, localNodeName, rscName);
                // do not fail here as adding allow-two-primaries property is only a problem while live migrating
            }
        }
    }

    @Override
    public boolean connectPhysicalDisk(String volumePath, KVMStoragePool pool, Map<String, String> details)
    {
        logger.debug("Linstor: connectPhysicalDisk {}:{} -> {}", pool.getUuid(), volumePath, details);
        if (volumePath == null) {
            logger.warn("volumePath is null, ignoring");
            return false;
        }

        final DevelopersApi api = getLinstorAPI(pool);
        String rscName;
        try
        {
            rscName = getLinstorRscName(volumePath);

            ResourceMakeAvailable rma = new ResourceMakeAvailable();
            ApiCallRcList answers = api.resourceMakeAvailableOnNode(rscName, localNodeName, rma);
            checkLinstorAnswersThrow(answers);

        } catch (ApiException apiEx) {
            logger.error(apiEx);
            throw new CloudRuntimeException(apiEx.getBestMessage(), apiEx);
        }

        try
        {
            allow2PrimariesIfInUse(api, rscName);
        } catch (ApiException apiEx) {
            logger.error(apiEx);
            // do not fail here as adding allow-two-primaries property is only a problem while live migrating
        }
        return true;
    }

    private void removeTwoPrimariesRcProps(DevelopersApi api, String inUseNode, String rscName) throws ApiException {
        ResourceConnectionModify rcm = new ResourceConnectionModify();
        List<String> deleteProps = new ArrayList<>();
        deleteProps.add("DrbdOptions/Net/allow-two-primaries");
        deleteProps.add("DrbdOptions/Net/protocol");
        rcm.deleteProps(deleteProps);
        ApiCallRcList answers = api.resourceConnectionModify(rscName, localNodeName, inUseNode, rcm);
        if (answers.hasError()) {
            logger.error("Failed to remove 'protocol' and 'allow-two-primaries' on {}/{}/{}: {}",
                            localNodeName,
                            inUseNode,
                            rscName, LinstorUtil.getBestErrorMessage(answers));
            // do not fail here as removing allow-two-primaries property isn't fatal
        }
    }

    private boolean tryDisconnectLinstor(String volumePath, KVMStoragePool pool)
    {
        if (volumePath == null) {
            return false;
        }

        logger.debug("Linstor: Using storage pool: " + pool.getUuid());
        final DevelopersApi api = getLinstorAPI(pool);

        Optional<ResourceWithVolumes> optRsc;
        try
        {
            List<ResourceWithVolumes> resources = api.viewResources(
                    Collections.singletonList(localNodeName),
                    null,
                    null,
                    null,
                    null,
                    null);

            optRsc = getResourceByPathOrName(resources, volumePath);
        } catch (ApiException apiEx) {
            // couldn't query linstor controller
            logger.error(apiEx.getBestMessage());
            return false;
        }


        if (optRsc.isPresent()) {
            Resource rsc = optRsc.get();
            try {
                String inUseNode = LinstorUtil.isResourceInUse(api, rsc.getName());
                if (inUseNode != null && !inUseNode.equalsIgnoreCase(localNodeName)) {
                    removeTwoPrimariesRcProps(api, inUseNode, rsc.getName());
                }
            } catch (ApiException apiEx) {
                logger.error(apiEx.getBestMessage());
                // do not fail here as removing allow-two-primaries property or deleting diskless isn't fatal
            }

            try {
                // if diskless resource remove it, in the worst case it will be transformed to a tiebreaker
                if (rsc.getFlags() != null &&
                        rsc.getFlags().contains(ApiConsts.FLAG_DRBD_DISKLESS) &&
                        !rsc.getFlags().contains(ApiConsts.FLAG_TIE_BREAKER)) {
                    ApiCallRcList delAnswers = api.resourceDelete(rsc.getName(), localNodeName);
                    logLinstorAnswers(delAnswers);
                }
            } catch (ApiException apiEx) {
                logger.error(apiEx.getBestMessage());
                // do not fail here as removing allow-two-primaries property or deleting diskless isn't fatal
            }

            return true;
        }

        logger.warn("Linstor: Couldn't find resource for this path: " + volumePath);
        return false;
    }

    @Override
    public boolean disconnectPhysicalDisk(String volumePath, KVMStoragePool pool)
    {
        logger.debug("Linstor: disconnectPhysicalDisk {}:{}", pool.getUuid(), volumePath);
        if (MapStorageUuidToStoragePool.containsValue(pool)) {
            return tryDisconnectLinstor(volumePath, pool);
        }
        return false;
    }

    @Override
    public boolean disconnectPhysicalDisk(Map<String, String> volumeToDisconnect)
    {
        // as of now this is only relevant for iscsi targets
        logger.info("Linstor: disconnectPhysicalDisk(Map<String, String> volumeToDisconnect) called?");
        return false;
    }

    private Optional<ResourceWithVolumes> getResourceByPathOrName(
            final List<ResourceWithVolumes> resources, String path) {
        return resources.stream()
            .filter(rsc -> getLinstorRscName(path).equalsIgnoreCase(rsc.getName()) || rsc.getVolumes().stream()
                .anyMatch(v -> path.equals(v.getDevicePath())))
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

            return tryDisconnectLinstor(localPath, pool);
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

    /**
     * Checks if all diskful resource are on a zeroed block device.
     * @param destPool Linstor pool to use
     * @param resName Linstor resource name
     * @return true if all resources are on a provider with zeroed blocks.
     */
    private boolean resourceSupportZeroBlocks(KVMStoragePool destPool, String resName) {
        final DevelopersApi api = getLinstorAPI(destPool);

        try {
            List<ResourceWithVolumes> resWithVols = api.viewResources(
                    Collections.emptyList(),
                    Collections.singletonList(resName),
                    Collections.emptyList(),
                    Collections.emptyList(),
                    null,
                    null);

            if (resWithVols != null) {
                return resWithVols.stream()
                        .allMatch(res -> {
                            Volume vol0 = res.getVolumes().get(0);
                            return vol0 != null && (vol0.getProviderKind() == ProviderKind.LVM_THIN ||
                                    vol0.getProviderKind() == ProviderKind.ZFS ||
                                    vol0.getProviderKind() == ProviderKind.ZFS_THIN ||
                                    vol0.getProviderKind() == ProviderKind.DISKLESS);
                        } );
            }
        } catch (ApiException apiExc) {
            logger.error(apiExc.getMessage());
        }
        return false;
    }

    @Override
    public KVMPhysicalDisk copyPhysicalDisk(KVMPhysicalDisk disk, String name, KVMStoragePool destPools, int timeout, byte[] srcPassphrase, byte[] destPassphrase, Storage.ProvisioningType provisioningType)
    {
        logger.debug("Linstor.copyPhysicalDisk: {} -> {}", disk.getPath(), name);
        final QemuImg.PhysicalDiskFormat sourceFormat = disk.getFormat();
        final String sourcePath = disk.getPath();

        final QemuImgFile srcFile = new QemuImgFile(sourcePath, sourceFormat);

        final KVMPhysicalDisk dstDisk = destPools.createPhysicalDisk(
            name, QemuImg.PhysicalDiskFormat.RAW, provisioningType, disk.getVirtualSize(), null);

        final DevelopersApi api = getLinstorAPI(destPools);
        final String rscName = LinstorUtil.RSC_PREFIX + name;
        try {
            LinstorUtil.applyAuxProps(api, rscName, disk.getDispName(), disk.getVmName());
        } catch (ApiException apiExc) {
            logger.error("Error setting aux properties for {}", rscName);
            logLinstorAnswers(apiExc.getApiCallRcList());
        }

        logger.debug("Linstor.copyPhysicalDisk: dstPath: {}", dstDisk.getPath());
        final QemuImgFile destFile = new QemuImgFile(dstDisk.getPath());
        destFile.setFormat(dstDisk.getFormat());
        destFile.setSize(disk.getVirtualSize());

        boolean zeroedDevice = resourceSupportZeroBlocks(destPools, LinstorUtil.RSC_PREFIX + name);

        try {
            final QemuImg qemu = new QemuImg(timeout, zeroedDevice, true);
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
        final String rscGroupName = pool.getResourceGroup();
        return LinstorUtil.getCapacityBytes(pool.getSourceHost(), rscGroupName);
    }

    public long getAvailable(LinstorStoragePool pool) {
        DevelopersApi linstorApi = getLinstorAPI(pool);
        final String rscGroupName = pool.getResourceGroup();
        try {
            List<StoragePool> storagePools = LinstorUtil.getRscGroupStoragePools(linstorApi, rscGroupName);

            final long free = storagePools.stream()
                .filter(sp -> sp.getProviderKind() != ProviderKind.DISKLESS)
                .mapToLong(sp -> sp.getFreeCapacity() != null ? sp.getFreeCapacity() : 0L).sum() * 1024;  // linstor uses KiB

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
            List<StoragePool> storagePools = LinstorUtil.getRscGroupStoragePools(linstorApi, rscGroupName);

            final long used = storagePools.stream()
                .filter(sp -> sp.getProviderKind() != ProviderKind.DISKLESS)
                .mapToLong(sp -> sp.getTotalCapacity() != null && sp.getFreeCapacity() != null ?
                        sp.getTotalCapacity() - sp.getFreeCapacity() : 0L)
                    .sum() * 1024; // linstor uses Kib
            logger.debug("Linstor: getUsed() -> " + used);
            return used;
        } catch (ApiException apiEx) {
            logger.error(apiEx.getMessage());
            throw new CloudRuntimeException(apiEx.getBestMessage(), apiEx);
        }
    }
}
