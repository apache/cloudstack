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
import com.cloud.agent.api.Command;
import com.cloud.agent.api.storage.MigrateVolumeAnswer;
import com.cloud.agent.api.storage.MigrateVolumeCommand;
import com.cloud.agent.api.to.DiskTO;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.resource.disconnecthook.VolumeMigrationCancelHook;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef;
import com.cloud.hypervisor.kvm.storage.KVMPhysicalDisk;
import com.cloud.hypervisor.kvm.storage.KVMStoragePool;
import com.cloud.hypervisor.kvm.storage.KVMStoragePoolManager;
import com.cloud.hypervisor.kvm.storage.LibvirtStorageAdaptor;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.storage.Storage;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.apache.cloudstack.storage.datastore.client.ScaleIOGatewayClient;
import org.apache.cloudstack.storage.datastore.util.ScaleIOUtil;
import org.apache.cloudstack.storage.to.PrimaryDataStoreTO;
import org.apache.cloudstack.storage.to.VolumeObjectTO;
import org.apache.cloudstack.utils.qemu.QemuImg.PhysicalDiskFormat;
import org.apache.cloudstack.utils.security.ParserUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.libvirt.Connect;
import org.libvirt.Domain;
import org.libvirt.DomainBlockJobInfo;
import org.libvirt.DomainInfo;
import org.libvirt.TypedParameter;
import org.libvirt.TypedUlongParameter;
import org.libvirt.LibvirtException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

@ResourceWrapper(handles =  MigrateVolumeCommand.class)
public class LibvirtMigrateVolumeCommandWrapper extends CommandWrapper<MigrateVolumeCommand, Answer, LibvirtComputingResource> {

    @Override
    public Answer execute(final MigrateVolumeCommand command, final LibvirtComputingResource libvirtComputingResource) {
        VolumeObjectTO srcVolumeObjectTO = (VolumeObjectTO)command.getSrcData();
        PrimaryDataStoreTO srcPrimaryDataStore = (PrimaryDataStoreTO)srcVolumeObjectTO.getDataStore();

        VolumeObjectTO destVolumeObjectTO = (VolumeObjectTO)command.getDestData();
        PrimaryDataStoreTO destPrimaryDataStore = (PrimaryDataStoreTO)destVolumeObjectTO.getDataStore();

        MigrateVolumeAnswer answer;
        if (Objects.equals(Storage.StoragePoolType.PowerFlex, srcPrimaryDataStore.getPoolType())) {
            answer = migratePowerFlexVolume(command, libvirtComputingResource);
        } else if (Objects.equals(Storage.StoragePoolType.RBD, srcPrimaryDataStore.getPoolType())
                || Objects.equals(Storage.StoragePoolType.RBD, destPrimaryDataStore.getPoolType())
                || (isFileBasedPool(srcPrimaryDataStore) && isFileBasedPool(destPrimaryDataStore))) {
            // RBD, NFS, SharedMountPoint and local volumes are live migrated in place with a libvirt block copy, as both pools are
            // reachable from the host the Instance is running on (the command is sent to that host).
            answer = migrateRBDVolume(command, libvirtComputingResource);
        } else {
            answer = migrateRegularVolume(command, libvirtComputingResource);
        }

        return answer;
    }

    private static boolean isFileBasedPool(PrimaryDataStoreTO dataStore) {
        return Objects.equals(Storage.StoragePoolType.NetworkFilesystem, dataStore.getPoolType())
                || Objects.equals(Storage.StoragePoolType.SharedMountPoint, dataStore.getPoolType())
                || Objects.equals(Storage.StoragePoolType.Filesystem, dataStore.getPoolType());
    }

    protected MigrateVolumeAnswer migratePowerFlexVolume(final MigrateVolumeCommand command, final LibvirtComputingResource libvirtComputingResource) {

        // Source Details
        VolumeObjectTO srcVolumeObjectTO = (VolumeObjectTO)command.getSrcData();
        String srcPath = srcVolumeObjectTO.getPath();
        final String srcVolumeId = ScaleIOUtil.getVolumePath(srcVolumeObjectTO.getPath());
        final String vmName = srcVolumeObjectTO.getVmName();

        // Destination Details
        VolumeObjectTO destVolumeObjectTO = (VolumeObjectTO)command.getDestData();
        String destPath = destVolumeObjectTO.getPath();
        final String destVolumeId = ScaleIOUtil.getVolumePath(destVolumeObjectTO.getPath());
        Map<String, String> destDetails = command.getDestDetails();
        final String destSystemId = destDetails.get(ScaleIOGatewayClient.STORAGE_POOL_SYSTEM_ID);
        String destDiskLabel = null;

        final String destDiskFileName = ScaleIOUtil.DISK_NAME_PREFIX + destSystemId + "-" + destVolumeId;
        final String diskFilePath = ScaleIOUtil.DISK_PATH + File.separator + destDiskFileName;

        VolumeMigrationCancelHook cancelHook = null;

        Domain dm = null;
        try {
            final LibvirtUtilitiesHelper libvirtUtilitiesHelper = libvirtComputingResource.getLibvirtUtilitiesHelper();
            Connect conn = libvirtUtilitiesHelper.getConnection();
            dm = libvirtComputingResource.getDomain(conn, vmName);
            if (dm == null) {
                return new MigrateVolumeAnswer(command, false, "Migrate volume failed due to can not find Instance: " + vmName, null);
            }

            DomainInfo.DomainState domainState = dm.getInfo().state ;
            if (domainState != DomainInfo.DomainState.VIR_DOMAIN_RUNNING) {
                return new MigrateVolumeAnswer(command, false, "Migrate volume failed due to Instance is not running: " + vmName + " with domainState = " + domainState, null);
            }

            final KVMStoragePoolManager storagePoolMgr = libvirtComputingResource.getStoragePoolMgr();
            PrimaryDataStoreTO spool = (PrimaryDataStoreTO)destVolumeObjectTO.getDataStore();
            KVMStoragePool pool = storagePoolMgr.getStoragePool(spool.getPoolType(), spool.getUuid());
            pool.connectPhysicalDisk(destVolumeObjectTO.getPath(), null);

            String srcSecretUUID = null;
            String destSecretUUID = null;
            if (ArrayUtils.isNotEmpty(destVolumeObjectTO.getPassphrase())) {
                srcSecretUUID = libvirtComputingResource.createLibvirtVolumeSecret(conn, srcVolumeObjectTO.getPath(), srcVolumeObjectTO.getPassphrase());
                destSecretUUID = libvirtComputingResource.createLibvirtVolumeSecret(conn, destVolumeObjectTO.getPath(), destVolumeObjectTO.getPassphrase());
            }

            String diskdef = generateDestinationDiskXMLForPowerFlex(dm, srcVolumeId, diskFilePath, destSecretUUID);
            destDiskLabel = generateDestinationDiskLabel(diskdef);

            TypedUlongParameter parameter = new TypedUlongParameter("bandwidth", 0);
            TypedParameter[] parameters = new TypedParameter[1];
            parameters[0] = parameter;

            cancelHook = new VolumeMigrationCancelHook(dm, destDiskLabel);
            libvirtComputingResource.addDisconnectHook(cancelHook);

            libvirtComputingResource.createOrUpdateLogFileForCommand(command, Command.State.PROCESSING_IN_BACKEND);

            dm.blockCopy(destDiskLabel, diskdef, parameters, Domain.BlockCopyFlags.REUSE_EXT);
            logger.info(String.format("Block copy has started for the volume %s : %s ", destDiskLabel, srcPath));

            MigrateVolumeAnswer answer = checkBlockJobStatus(command, dm, destDiskLabel, srcPath, destPath, libvirtComputingResource, conn, srcSecretUUID);
            if (answer != null) {
                if (answer.getResult()) {
                    libvirtComputingResource.createOrUpdateLogFileForCommand(command, Command.State.COMPLETED);
                } else {
                    libvirtComputingResource.createOrUpdateLogFileForCommand(command, Command.State.FAILED);
                }
            }
            return answer;
        } catch (Exception e) {
            String msg = "Migrate volume failed due to " + e.toString();
            logger.warn(msg, e);
            if (destDiskLabel != null) {
                try {
                    dm.blockJobAbort(destDiskLabel, Domain.BlockJobAbortFlags.ASYNC);
                } catch (LibvirtException ex) {
                    logger.error("Migrate volume failed while aborting the block job due to " + ex.getMessage());
                }
            }
            libvirtComputingResource.createOrUpdateLogFileForCommand(command, Command.State.FAILED);
            return new MigrateVolumeAnswer(command, false, msg, null);
        } finally {
            if (cancelHook != null) {
                libvirtComputingResource.removeDisconnectHook(cancelHook);
            }
            if (dm != null) {
                try {
                    dm.free();
                } catch (LibvirtException l) {
                    logger.trace("Ignoring libvirt error.", l);
                };
            }
        }
    }

    protected MigrateVolumeAnswer checkBlockJobStatus(MigrateVolumeCommand command, Domain dm, String diskLabel, String srcPath, String destPath, LibvirtComputingResource libvirtComputingResource, Connect conn, String srcSecretUUID) throws LibvirtException {
        int timeBetweenTries = 1000; // Try more frequently (every sec) and return early if disk is found
        int waitTimeInSec = command.getWait();
        double blockCopyProgress = 0;
        while (waitTimeInSec > 0) {
            DomainBlockJobInfo blockJobInfo = dm.getBlockJobInfo(diskLabel, 0);
            if (blockJobInfo != null) {
                blockCopyProgress = (blockJobInfo.end == 0)? blockCopyProgress : 100 * (blockJobInfo.cur / (double) blockJobInfo.end);
                logger.debug(String.format("Volume %s : %s, block copy progress: %s%%, current value: %s end value: %s, job info - type: %s, bandwidth: %s",
                        diskLabel, srcPath, blockCopyProgress, blockJobInfo.cur, blockJobInfo.end, blockJobInfo.type, blockJobInfo.bandwidth));
                if (blockJobInfo.cur == blockJobInfo.end) {
                    if (blockJobInfo.end > 0) {
                        logger.info(String.format("Block copy completed for the volume %s : %s", diskLabel, srcPath));
                        dm.blockJobAbort(diskLabel, Domain.BlockJobAbortFlags.PIVOT);
                        if (StringUtils.isNotEmpty(srcSecretUUID)) {
                            libvirtComputingResource.removeLibvirtVolumeSecret(conn, srcSecretUUID);
                        }
                        break;
                    } else {
                        // cur = 0, end = 0 - at this point, disk does not have an active block job (so, no need to abort job)
                        String msg = String.format("No active block copy job for the volume %s : %s - job stopped at %s progress", diskLabel, srcPath, blockCopyProgress);
                        logger.warn(msg);
                        return new MigrateVolumeAnswer(command, false, msg, null);
                    }
                }
            } else {
                logger.info("Failed to get the block copy status, trying to abort the job");
                dm.blockJobAbort(diskLabel, Domain.BlockJobAbortFlags.ASYNC);
            }
            waitTimeInSec--;

            try {
                Thread.sleep(timeBetweenTries);
            } catch (Exception ex) {
                // don't do anything
            }
        }

        if (waitTimeInSec <= 0) {
            String msg = "Block copy is taking long time, failing the job";
            logger.error(msg);
            try {
                dm.blockJobAbort(diskLabel, Domain.BlockJobAbortFlags.ASYNC);
            } catch (LibvirtException ex) {
                logger.error("Migrate volume failed while aborting the block job due to " + ex.getMessage());
            }
            return new MigrateVolumeAnswer(command, false, msg, null);
        }

        return new MigrateVolumeAnswer(command, true, null, destPath);
    }

    private String generateDestinationDiskLabel(String diskXml) throws ParserConfigurationException, IOException, SAXException {

        DocumentBuilderFactory dbFactory = ParserUtils.getSaferDocumentBuilderFactory();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(new ByteArrayInputStream(diskXml.getBytes("UTF-8")));
        doc.getDocumentElement().normalize();

        Element disk = doc.getDocumentElement();
        String diskLabel = getAttrValue("target", "dev", disk);

        return diskLabel;
    }

    protected String generateDestinationDiskXMLForPowerFlex(Domain dm, String srcVolumeId, String diskFilePath, String destSecretUUID) throws LibvirtException, ParserConfigurationException, IOException, TransformerException, SAXException {
        final String domXml = dm.getXMLDesc(0);

        DocumentBuilderFactory dbFactory = ParserUtils.getSaferDocumentBuilderFactory();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(new ByteArrayInputStream(domXml.getBytes("UTF-8")));
        doc.getDocumentElement().normalize();

        NodeList disks = doc.getElementsByTagName("disk");

        for (int i = 0; i < disks.getLength(); i++) {
            Element disk = (Element)disks.item(i);
            String type = disk.getAttribute("type");
            if (!type.equalsIgnoreCase("network")) {
                String diskDev = getAttrValue("source", "dev", disk);
                if (StringUtils.isNotEmpty(diskDev) && diskDev.contains(srcVolumeId)) {
                    setAttrValue("source", "dev", diskFilePath, disk);
                    if (StringUtils.isNotEmpty(destSecretUUID)) {
                        setAttrValue("secret", "uuid", destSecretUUID, disk);
                    }
                    StringWriter diskSection = new StringWriter();
                    Transformer xformer = TransformerFactory.newInstance().newTransformer();
                    xformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
                    xformer.transform(new DOMSource(disk), new StreamResult(diskSection));

                    return diskSection.toString();
                }
            }
        }

        return null;
    }

    private static String getAttrValue(String tag, String attr, Element eElement) {
        NodeList tagNode = eElement.getElementsByTagName(tag);
        if (tagNode.getLength() == 0) {
            return null;
        }
        Element node = (Element)tagNode.item(0);
        return node.getAttribute(attr);
    }

    private static void setAttrValue(String tag, String attr, String newValue, Element eElement) {
        NodeList tagNode = eElement.getElementsByTagName(tag);
        if (tagNode.getLength() == 0) {
            return;
        }
        Element node = (Element)tagNode.item(0);
        node.setAttribute(attr, newValue);
    }

    protected MigrateVolumeAnswer migrateRegularVolume(final MigrateVolumeCommand command, final LibvirtComputingResource libvirtComputingResource) {
        KVMStoragePoolManager storagePoolManager = libvirtComputingResource.getStoragePoolMgr();

        VolumeObjectTO srcVolumeObjectTO = (VolumeObjectTO)command.getSrcData();
        PrimaryDataStoreTO srcPrimaryDataStore = (PrimaryDataStoreTO)srcVolumeObjectTO.getDataStore();

        Map<String, String> srcDetails = command.getSrcDetails();
        String srcPath = srcDetails != null ? srcDetails.get(DiskTO.IQN) : srcVolumeObjectTO.getPath();
        // its possible a volume has details but is not using IQN addressing...
        if (srcPath == null) {
            srcPath = srcVolumeObjectTO.getPath();
        }

        VolumeObjectTO destVolumeObjectTO = (VolumeObjectTO)command.getDestData();
        PrimaryDataStoreTO destPrimaryDataStore = (PrimaryDataStoreTO)destVolumeObjectTO.getDataStore();

        Map<String, String> destDetails = command.getDestDetails();

        String destPath = destDetails != null && destDetails.get(DiskTO.IQN) != null ? destDetails.get(DiskTO.IQN) :
                (destVolumeObjectTO.getPath() != null ? destVolumeObjectTO.getPath() : UUID.randomUUID().toString());

        // Update path in the command for reconciliation
        if (destVolumeObjectTO.getPath() == null) {
            destVolumeObjectTO.setPath(destPath);
        }

        try {
            KVMStoragePool sourceStoragePool = storagePoolManager.getStoragePool(srcPrimaryDataStore.getPoolType(), srcPrimaryDataStore.getUuid());

            if (!sourceStoragePool.connectPhysicalDisk(srcPath, srcDetails)) {
                return new MigrateVolumeAnswer(command, false, "Unable to connect source volume on hypervisor", srcPath);
            }

            KVMPhysicalDisk srcPhysicalDisk = storagePoolManager.getPhysicalDisk(srcPrimaryDataStore.getPoolType(), srcPrimaryDataStore.getUuid(), srcPath);
            if (srcPhysicalDisk == null) {
                return new MigrateVolumeAnswer(command, false, "Unable to get handle to source volume on hypervisor", srcPath);
            }

            KVMStoragePool destPrimaryStorage = storagePoolManager.getStoragePool(destPrimaryDataStore.getPoolType(), destPrimaryDataStore.getUuid());

            if (!destPrimaryStorage.connectPhysicalDisk(destPath, destDetails)) {
                return new MigrateVolumeAnswer(command, false, "Unable to connect destination volume on hypervisor", srcPath);
            }

            libvirtComputingResource.createOrUpdateLogFileForCommand(command, Command.State.PROCESSING_IN_BACKEND);
            KVMPhysicalDisk newDiskCopy = storagePoolManager.copyPhysicalDisk(srcPhysicalDisk, destPath, destPrimaryStorage, command.getWaitInMillSeconds());
            if (newDiskCopy == null) {
                libvirtComputingResource.createOrUpdateLogFileForCommand(command, Command.State.FAILED);
                return new MigrateVolumeAnswer(command, false, "Copy command failed to return handle to copied physical disk", destPath);
            }
            libvirtComputingResource.createOrUpdateLogFileForCommand(command, Command.State.COMPLETED);
        }
        catch (Exception ex) {
            libvirtComputingResource.createOrUpdateLogFileForCommand(command, Command.State.FAILED);
            return new MigrateVolumeAnswer(command, false, ex.getMessage(), null);
        }
        finally {
            try {
                storagePoolManager.disconnectPhysicalDisk(destPrimaryDataStore.getPoolType(), destPrimaryDataStore.getUuid(), destPath);
            }
            catch (Exception e) {
                logger.warn("Unable to disconnect from the destination device.", e);
            }

            try {
                storagePoolManager.disconnectPhysicalDisk(srcPrimaryDataStore.getPoolType(), srcPrimaryDataStore.getUuid(), srcPath);
            }
            catch (Exception e) {
                logger.warn("Unable to disconnect from the source device.", e);
            }
        }

        return new MigrateVolumeAnswer(command, true, null, destPath);
    }


    protected MigrateVolumeAnswer migrateRBDVolume(final MigrateVolumeCommand command, final LibvirtComputingResource libvirtComputingResource) {
        final String vmName = command.getAttachedVmName();

        VolumeObjectTO srcVolumeObjectTO = (VolumeObjectTO)command.getSrcData();
        PrimaryDataStoreTO sourcePool = (PrimaryDataStoreTO)srcVolumeObjectTO.getDataStore();

        VolumeObjectTO destVolumeObjectTO = (VolumeObjectTO)command.getDestData();
        PrimaryDataStoreTO destPool = (PrimaryDataStoreTO)destVolumeObjectTO.getDataStore();

        String destDiskLabel = null;

        VolumeMigrationCancelHook cancelHook = null;

        final KVMStoragePoolManager storagePoolMgr = libvirtComputingResource.getStoragePoolMgr();

        Domain dm = null;
        MigrateVolumeAnswer answer = null;
        boolean destVolumeCreated = false;
        KVMStoragePool destKvmPool = null;
        try {
            final LibvirtUtilitiesHelper libvirtUtilitiesHelper = libvirtComputingResource.getLibvirtUtilitiesHelper();
            Connect conn = libvirtUtilitiesHelper.getConnection();
            dm = libvirtComputingResource.getDomain(conn, vmName);
            if (dm == null) {
                return new MigrateVolumeAnswer(command, false, "Migrate volume failed due to can not find vm: " + vmName, null);
            }

            DomainInfo.DomainState domainState = dm.getInfo().state ;
            if (domainState != DomainInfo.DomainState.VIR_DOMAIN_RUNNING) {
                return new MigrateVolumeAnswer(command, false, "Migrate volume failed due to VM is not running: " + vmName + " with domainState = " + domainState, null);
            }

            KVMStoragePool pool = storagePoolMgr.getStoragePool(destPool.getPoolType(), destPool.getUuid());
            destKvmPool = pool;
            pool.connectPhysicalDisk(destVolumeObjectTO.getPath(), null);

            KVMStoragePool srcKvmPool = storagePoolMgr.getStoragePool(sourcePool.getPoolType(), sourcePool.getUuid());
            final String sourceFilePath = generateVolumePathOnHost(srcKvmPool, sourcePool, srcVolumeObjectTO);
            final String destFilePath = generateVolumePathOnHost(pool, destPool, destVolumeObjectTO);

            String srcSecretUUID = null;
            String destSecretUUID = null;
            if (ArrayUtils.isNotEmpty(destVolumeObjectTO.getPassphrase())) {
                srcSecretUUID = libvirtComputingResource.createLibvirtVolumeSecret(conn, srcVolumeObjectTO.getPath(), srcVolumeObjectTO.getPassphrase());
                destSecretUUID = libvirtComputingResource.createLibvirtVolumeSecret(conn, destVolumeObjectTO.getPath(), destVolumeObjectTO.getPassphrase());
                if (Objects.equals(srcSecretUUID, destSecretUUID)) {
                    // the secret is named after the volume path and is shared by both disks: keep it for the Instance
                    srcSecretUUID = null;
                }
            }

            // A block copy between two RBD pools only has to copy the active layer, as the destination image is
            // already provisioned. When NFS is involved the backing chain of the source differs from what the
            // destination can reference, so the whole chain is flattened into the destination instead.
            boolean srcAndDestOnRbd = Objects.equals(Storage.StoragePoolType.RBD, sourcePool.getPoolType())
                    && Objects.equals(Storage.StoragePoolType.RBD, destPool.getPoolType());

            String diskdef = generateDestinationDiskXMLForRBD(dm, sourceFilePath, destFilePath, destSecretUUID, pool, destPool.getPoolType(),
                    srcAndDestOnRbd);
            if (diskdef == null) {
                return new MigrateVolumeAnswer(command, false,
                        String.format("Migrate volume failed as the disk with path %s was not found in the XML domain of vm %s", sourceFilePath, vmName), null);
            }
            destDiskLabel = generateDestinationDiskLabel(diskdef);

            TypedUlongParameter parameter = new TypedUlongParameter("bandwidth", 0);
            TypedParameter[] parameters = new TypedParameter[1];
            parameters[0] = parameter;

            int blockCopyFlags = srcAndDestOnRbd ? Domain.BlockCopyFlags.SHALLOW : 0;
            if (ArrayUtils.isNotEmpty(destVolumeObjectTO.getPassphrase())) {
                // libvirt can not create (and format) an encrypted mirror itself, so the destination volume is
                // created with its encryption header up front and the block copy writes into it.
                KVMPhysicalDisk srcDisk = srcKvmPool.getPhysicalDisk(srcVolumeObjectTO.getPath());
                long guestSize = srcDisk.getVirtualSize();
                if (Objects.equals(Storage.StoragePoolType.RBD, sourcePool.getPoolType())) {
                    guestSize -= LibvirtStorageAdaptor.LUKS2_HEADER_RESERVE_BYTES;
                }
                createEncryptedDestinationVolume(pool, destPool, destVolumeObjectTO, guestSize);
                destVolumeCreated = true;
                blockCopyFlags |= Domain.BlockCopyFlags.REUSE_EXT;
            }

            cancelHook = new VolumeMigrationCancelHook(dm, destDiskLabel);
            libvirtComputingResource.addDisconnectHook(cancelHook);

            libvirtComputingResource.createOrUpdateLogFileForCommand(command, Command.State.PROCESSING_IN_BACKEND);

            dm.blockCopy(destDiskLabel, diskdef, parameters, blockCopyFlags);
            logger.info(String.format("Block copy has started for the volume %s : %s to %s", destDiskLabel, sourceFilePath, destFilePath));

            answer = checkBlockJobStatus(command, dm, destDiskLabel, sourceFilePath, destFilePath, libvirtComputingResource, conn, srcSecretUUID);
            if (answer != null) {
                if (answer.getResult()) {
                    libvirtComputingResource.createOrUpdateLogFileForCommand(command, Command.State.COMPLETED);
                } else {
                    libvirtComputingResource.createOrUpdateLogFileForCommand(command, Command.State.FAILED);
                }
            }
            if (answer != null && answer.getResult()) {
                answer = new MigrateVolumeAnswer(command, true, null, destVolumeObjectTO.getPath());

                // Libvirt only detects the volume's real format (e.g. qcow2) if the destination pool
                // already knew about it as such. Since the block copy wrote the file directly (not
                // through libvirt's own volume-creation calls), any pool other than RBD needs an
                // explicit refresh here; otherwise libvirt keeps whatever it inferred when the disk
                // was first attached to this domain (or falls back to a filename-suffix guess), which
                // for a volume carried over from RBD would wrongly be treated as a raw block device the
                // next time the Instance is (re)started.
                if (!Objects.equals(Storage.StoragePoolType.RBD, destPool.getPoolType())) {
                    pool.refresh();
                }
            }
            return answer;
        } catch (Exception e) {
            String msg = "Migrate volume failed due to " + e.toString();
            logger.warn(msg, e);
            if (destDiskLabel != null) {
                try {
                    dm.blockJobAbort(destDiskLabel, Domain.BlockJobAbortFlags.ASYNC);
                } catch (LibvirtException ex) {
                    logger.error("Migrate volume failed while aborting the block job due to " + ex.getMessage());
                }
            }
            libvirtComputingResource.createOrUpdateLogFileForCommand(command, Command.State.FAILED);
            answer = new MigrateVolumeAnswer(command, false, msg, null);
            return answer;
        } finally {
            if (cancelHook != null) {
                libvirtComputingResource.removeDisconnectHook(cancelHook);
            }
            if (destVolumeCreated && (answer == null || !answer.getResult())) {
                deleteDestinationVolume(destKvmPool, destPool, destVolumeObjectTO.getPath());
            }
            if (answer != null && answer.getResult()) {
                // Remove old volume after successful migration
                KVMStoragePool libvirtSourcePool = storagePoolMgr.getStoragePool(sourcePool.getPoolType(), sourcePool.getUuid());
                Storage.ImageFormat srcFormat = Objects.equals(Storage.StoragePoolType.RBD, sourcePool.getPoolType())
                        ? Storage.ImageFormat.RAW : Storage.ImageFormat.QCOW2;
                libvirtSourcePool.deletePhysicalDisk(srcVolumeObjectTO.getPath(), srcFormat);
            }
            if (dm != null) {
                try {
                    dm.free();
                } catch (LibvirtException l) {
                    logger.trace("Ignoring libvirt error.", l);
                }
            }
        }
    }


    /**
     * Creates the (empty) destination volume of a live migration of an encrypted volume, with its encryption
     * header, so that the block copy can write into it. An RBD volume gets a native librbd LUKS2 header, a volume
     * on a file based pool is a qemu LUKS encrypted QCOW2 file.
     *
     * @param guestSize size of the disk as seen by the Instance
     */
    protected void createEncryptedDestinationVolume(KVMStoragePool pool, PrimaryDataStoreTO destPool, VolumeObjectTO destVolumeObjectTO, long guestSize) {
        String path = destVolumeObjectTO.getPath();
        // a previous failed attempt may have left the volume behind
        deleteDestinationVolume(pool, destPool, path);
        if (Objects.equals(Storage.StoragePoolType.RBD, destPool.getPoolType())) {
            // the LUKS2 header is carved out of the image, so it has to be added to what the Instance can use
            pool.createPhysicalDisk(path, PhysicalDiskFormat.RAW, Storage.ProvisioningType.THIN,
                    guestSize + LibvirtStorageAdaptor.LUKS2_HEADER_RESERVE_BYTES, destVolumeObjectTO.getPassphrase());
        } else {
            pool.createPhysicalDisk(path, PhysicalDiskFormat.QCOW2, Storage.ProvisioningType.THIN, guestSize, destVolumeObjectTO.getPassphrase());
        }
    }

    protected void deleteDestinationVolume(KVMStoragePool pool, PrimaryDataStoreTO destPool, String path) {
        if (pool == null) {
            return;
        }
        try {
            pool.deletePhysicalDisk(path, Objects.equals(Storage.StoragePoolType.RBD, destPool.getPoolType())
                    ? Storage.ImageFormat.RAW : Storage.ImageFormat.QCOW2);
        } catch (Exception e) {
            logger.debug("Could not remove the destination volume {} (it may not exist): {}", path, e.getMessage());
        }
    }

    /**
     * Returns the path of the volume as it is referenced in the XML domain of the Instance on this host. RBD volumes
     * are addressed as "<pool>/<image>", while file based volumes (e.g. NFS) are addressed by their path on the
     * local mount point of the pool.
     */
    protected String generateVolumePathOnHost(KVMStoragePool kvmStoragePool, PrimaryDataStoreTO dataStore, VolumeObjectTO volumeObjectTO) {
        if (Objects.equals(Storage.StoragePoolType.RBD, dataStore.getPoolType())) {
            return String.format("%s/%s", dataStore.getPath(), volumeObjectTO.getPath());
        }
        return String.format("%s/%s", kvmStoragePool.getLocalPath(), volumeObjectTO.getPath());
    }

    /**
     * Generates the XML of the destination disk used by the libvirt block copy. The source disk is located in the XML
     * domain of the Instance by its current path, and is then rewritten to point at the destination pool. Both an RBD
     * (network) and an NFS (file) destination are supported, independently of the type of the source disk.
     */
    /**
     * Build the mirror disk XML handed to {@code blockCopy} from the disk the volume currently uses.
     *
     * @param shallowBlockCopy whether the copy will run with {@code VIR_DOMAIN_BLOCK_COPY_SHALLOW}, which decides
     *                         whether the source's backing chain may be carried over to the mirror.
     */
    protected String generateDestinationDiskXMLForRBD(Domain dm, String srcVolumePath, String diskFilePath, String destSecretUUID, KVMStoragePool pool,
            Storage.StoragePoolType destPoolType, boolean shallowBlockCopy)
            throws LibvirtException, ParserConfigurationException, IOException, TransformerException, SAXException {
        final String domXml = dm.getXMLDesc(0);

        DocumentBuilderFactory dbFactory = ParserUtils.getSaferDocumentBuilderFactory();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(new ByteArrayInputStream(domXml.getBytes("UTF-8")));
        doc.getDocumentElement().normalize();

        NodeList disks = doc.getElementsByTagName("disk");

        for (int i = 0; i < disks.getLength(); i++) {
            Node diskNode = disks.item(i);
            if (!isDiskNodeOfVolume(diskNode, srcVolumePath)) {
                continue;
            }

            logger.debug(String.format("Found disk node with source volume path %s, generating destination disk XML for %s on %s", srcVolumePath, diskFilePath,
                    destPoolType));
            updateDiskNodeForDestinationPool(doc, diskNode, diskFilePath, destSecretUUID, pool, destPoolType, shallowBlockCopy);

            StringWriter diskSection = new StringWriter();
            Transformer xformer = TransformerFactory.newInstance().newTransformer();
            xformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            xformer.transform(new DOMSource(diskNode), new StreamResult(diskSection));

            return diskSection.toString();
        }
        return null;
    }

    /**
     * True if the disk node is the one backing the volume with the given path, be it an RBD (network) or a file disk.
     */
    protected boolean isDiskNodeOfVolume(Node diskNode, String srcVolumePath) {
        Node diskTypeAttribute = diskNode.getAttributes().getNamedItem("type");
        if (diskTypeAttribute == null) {
            return false;
        }
        String diskType = diskTypeAttribute.getNodeValue();
        boolean networkDisk = LibvirtVMDef.DiskDef.DiskType.NETWORK.toString().equalsIgnoreCase(diskType);
        boolean fileDisk = LibvirtVMDef.DiskDef.DiskType.FILE.toString().equalsIgnoreCase(diskType);
        if (!networkDisk && !fileDisk) {
            return false;
        }

        Node sourceNode = getChildNode(diskNode, "source");
        if (sourceNode == null) {
            return false;
        }
        NamedNodeMap sourceAttributes = sourceNode.getAttributes();

        if (networkDisk) {
            Node protocolAttribute = sourceAttributes.getNamedItem("protocol");
            if (protocolAttribute == null || !protocolAttribute.getNodeValue().equalsIgnoreCase(LibvirtVMDef.DiskDef.DiskProtocol.RBD.toString())) {
                logger.debug("Skipped disk node with protocol: " + protocolAttribute);
                return false;
            }
            Node nameAttribute = sourceAttributes.getNamedItem("name");
            return nameAttribute != null && nameAttribute.getNodeValue().contains(srcVolumePath);
        }

        Node fileAttribute = sourceAttributes.getNamedItem("file");
        return fileAttribute != null && fileAttribute.getNodeValue().contains(srcVolumePath);
    }

    /**
     * Rewrites the disk node so that it refers to the volume on the destination pool. The disk type, the source, the
     * driver type and the Ceph authentication are all adjusted to what the destination pool requires.
     */
    protected void updateDiskNodeForDestinationPool(Document doc, Node diskNode, String diskFilePath, String destSecretUUID, KVMStoragePool pool,
            Storage.StoragePoolType destPoolType, boolean shallowBlockCopy) {
        boolean destinationOnRbd = Objects.equals(Storage.StoragePoolType.RBD, destPoolType);

        Element diskElement = (Element)diskNode;
        diskElement.setAttribute("type", destinationOnRbd ? LibvirtVMDef.DiskDef.DiskType.NETWORK.toString().toLowerCase()
                : LibvirtVMDef.DiskDef.DiskType.FILE.toString().toLowerCase());

        Node sourceNode = getChildNode(diskNode, "source");
        if (sourceNode != null) {
            diskNode.removeChild(sourceNode);
        }
        diskNode.appendChild(destinationOnRbd ? createRbdSourceNode(doc, diskFilePath, pool) : createFileSourceNode(doc, diskFilePath));

        // A disk cloned from a template keeps that template as its backing file, and libvirt reports the whole
        // chain in the live domain XML. Only a shallow copy may carry that chain into the mirror: a full copy
        // flattens every layer into the destination, so a <backingStore> there describes a chain the destination
        // does not have. libvirt refuses that combination outright with "backingStore of mirror without
        // VIR_DOMAIN_BLOCK_COPY_SHALLOW doesn't make sense", which is why the element is dropped here.
        if (!shallowBlockCopy) {
            Node backingStoreNode = getChildNode(diskNode, "backingStore");
            if (backingStoreNode != null) {
                diskNode.removeChild(backingStoreNode);
            }
        }

        Node driverNode = getChildNode(diskNode, "driver");
        if (driverNode instanceof Element) {
            // RBD volumes are raw, whereas volumes on a file based pool are qcow2
            ((Element)driverNode).setAttribute("type", destinationOnRbd ? "raw" : "qcow2");
        }

        // The Ceph authentication of the source pool does not apply to the destination pool, so it is always replaced
        Node authNode = getChildNode(diskNode, "auth");
        if (authNode != null) {
            diskNode.removeChild(authNode);
        }
        if (destinationOnRbd && StringUtils.isNotBlank(pool.getAuthUserName())) {
            diskNode.appendChild(createAuthNode(doc, pool));
        }

        if (StringUtils.isNotBlank(destSecretUUID)) {
            Node encryptionNode = getChildNode(diskNode, "encryption");
            if (encryptionNode instanceof Element) {
                // librbd encrypts an RBD image natively (LUKS2), a file is encrypted by qemu (LUKS)
                Element encryptionElement = (Element)encryptionNode;
                if (destinationOnRbd) {
                    encryptionElement.setAttribute("format", "luks2");
                    encryptionElement.setAttribute("engine", "librbd");
                } else {
                    encryptionElement.setAttribute("format", "luks");
                    encryptionElement.removeAttribute("engine");
                }
            }
            if (encryptionNode != null) {
                Node secretNode = getChildNode(encryptionNode, "secret");
                if (secretNode != null) {
                    Node uuidAttribute = secretNode.getAttributes().getNamedItem("uuid");
                    if (uuidAttribute != null) {
                        uuidAttribute.setTextContent(destSecretUUID);
                    }
                }
            }
        }
    }

    protected Element createRbdSourceNode(Document doc, String diskFilePath, KVMStoragePool pool) {
        Element sourceNode = doc.createElement("source");
        sourceNode.setAttribute("protocol", LibvirtVMDef.DiskDef.DiskProtocol.RBD.toString().toLowerCase());
        sourceNode.setAttribute("name", diskFilePath);
        for (String sourceHost : pool.getSourceHost().split(",")) {
            Element hostNode = doc.createElement("host");
            hostNode.setAttribute("name", sourceHost);
            if (pool.getSourcePort() != 0) {
                hostNode.setAttribute("port", String.valueOf(pool.getSourcePort()));
            }
            sourceNode.appendChild(hostNode);
        }
        return sourceNode;
    }

    protected Element createFileSourceNode(Document doc, String diskFilePath) {
        Element sourceNode = doc.createElement("source");
        sourceNode.setAttribute("file", diskFilePath);
        return sourceNode;
    }

    protected Element createAuthNode(Document doc, KVMStoragePool pool) {
        Element authNode = doc.createElement("auth");
        authNode.setAttribute("username", pool.getAuthUserName());
        Element secretNode = doc.createElement("secret");
        secretNode.setAttribute("type", "ceph");
        secretNode.setAttribute("uuid", pool.getAuthSecretUUID());
        authNode.appendChild(secretNode);
        return authNode;
    }

    protected Node getChildNode(Node parent, String nodeName) {
        NodeList childNodes = parent.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node childNode = childNodes.item(i);
            if (nodeName.equals(childNode.getNodeName())) {
                return childNode;
            }
        }
        return null;
    }
}
