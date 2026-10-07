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

        MigrateVolumeAnswer answer;
        if (Objects.equals(Storage.StoragePoolType.PowerFlex, srcPrimaryDataStore.getPoolType())) {
            answer = migratePowerFlexVolume(command, libvirtComputingResource);
        } else if (Objects.equals(Storage.StoragePoolType.RBD, srcPrimaryDataStore.getPoolType())) {
            answer = migrateRBDVolume(command, libvirtComputingResource);
        } else {
            answer = migrateRegularVolume(command, libvirtComputingResource);
        }

        return answer;
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
        final String sourceFilePath = String.format("%s/%s", sourcePool.getPath(), srcVolumeObjectTO.getPath());

        VolumeObjectTO destVolumeObjectTO = (VolumeObjectTO)command.getDestData();
        PrimaryDataStoreTO destPool = (PrimaryDataStoreTO)destVolumeObjectTO.getDataStore();
        final String destFilePath = String.format("%s/%s", destPool.getPath(), destVolumeObjectTO.getPath());

        String destDiskLabel = null;

        VolumeMigrationCancelHook cancelHook = null;

        final KVMStoragePoolManager storagePoolMgr = libvirtComputingResource.getStoragePoolMgr();

        Domain dm = null;
        MigrateVolumeAnswer answer = null;
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
            pool.connectPhysicalDisk(destVolumeObjectTO.getPath(), null);

            String srcSecretUUID = null;
            String destSecretUUID = null;
            if (ArrayUtils.isNotEmpty(destVolumeObjectTO.getPassphrase())) {
                srcSecretUUID = libvirtComputingResource.createLibvirtVolumeSecret(conn, srcVolumeObjectTO.getPath(), srcVolumeObjectTO.getPassphrase());
                destSecretUUID = libvirtComputingResource.createLibvirtVolumeSecret(conn, destVolumeObjectTO.getPath(), destVolumeObjectTO.getPassphrase());
            }

            String diskdef = generateDestinationDiskXMLForRBD(dm, sourceFilePath, destFilePath, destSecretUUID, pool);
            destDiskLabel = generateDestinationDiskLabel(diskdef);

            TypedUlongParameter parameter = new TypedUlongParameter("bandwidth", 0);
            TypedParameter[] parameters = new TypedParameter[1];
            parameters[0] = parameter;

            cancelHook = new VolumeMigrationCancelHook(dm, destDiskLabel);
            libvirtComputingResource.addDisconnectHook(cancelHook);

            libvirtComputingResource.createOrUpdateLogFileForCommand(command, Command.State.PROCESSING_IN_BACKEND);

            dm.blockCopy(destDiskLabel, diskdef, parameters, Domain.BlockCopyFlags.SHALLOW);
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
            if (answer != null && answer.getResult()) {
                // Remove old volume after successful migration
                KVMStoragePool libvirtSourcePool = storagePoolMgr.getStoragePool(sourcePool.getPoolType(), sourcePool.getUuid());
                libvirtSourcePool.deletePhysicalDisk(srcVolumeObjectTO.getPath(), Storage.ImageFormat.RAW);
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

    protected String generateDestinationDiskXMLForRBD(Domain dm, String srcVolumePath, String diskFilePath, String destSecretUUID, KVMStoragePool pool) throws LibvirtException, ParserConfigurationException, IOException, TransformerException, SAXException {
        final String domXml = dm.getXMLDesc(0);

        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(new ByteArrayInputStream(domXml.getBytes("UTF-8")));
        doc.getDocumentElement().normalize();

        NodeList disks = doc.getElementsByTagName("disk");

        for (int i = 0; i < disks.getLength(); i++) {
            Node diskNode = disks.item(i);
            Node diskTypeAttribute = diskNode.getAttributes().getNamedItem("type");
            if (diskTypeAttribute == null || !diskTypeAttribute.getNodeValue().equalsIgnoreCase(LibvirtVMDef.DiskDef.DiskType.NETWORK.toString())) {
                continue;
            }

            NodeList diskChildNodes = diskNode.getChildNodes();
            boolean found = false;
            for (int j = 0; j < diskChildNodes.getLength(); j++) {
                Node diskChildNode = diskChildNodes.item(j);

                if ("source".equals(diskChildNode.getNodeName())) {
                    NamedNodeMap diskNodeAttributes = diskChildNode.getAttributes();
                    Node diskNodeAttribute = diskNodeAttributes.getNamedItem("protocol");
                    if (diskNodeAttribute == null || !diskNodeAttribute.getNodeValue().equalsIgnoreCase(LibvirtVMDef.DiskDef.DiskProtocol.RBD.toString())) {
                        logger.debug("Skipped disk node with protocol: " + diskNodeAttribute);
                        continue;
                    }
                    diskNodeAttribute = diskNodeAttributes.getNamedItem("name");
                    if (diskNodeAttribute == null || !diskNodeAttribute.getNodeValue().contains(srcVolumePath)) {
                        logger.debug("Skipped disk node with name: " + diskNodeAttribute);
                        continue;
                    }

                    logger.debug(String.format("Found disk node with source RBD volume path %s: %s", srcVolumePath, diskNodeAttribute));
                    diskNode.removeChild(diskChildNode);
                    Element newChildSourceNode = doc.createElement("source");
                    newChildSourceNode.setAttribute("protocol", LibvirtVMDef.DiskDef.DiskProtocol.RBD.toString().toLowerCase());
                    newChildSourceNode.setAttribute("name", diskFilePath);
                    for (String sourceHost : pool.getSourceHost().split(",")) {
                        Element newChildDiskNodeForHost = doc.createElement("host");
                        newChildDiskNodeForHost.setAttribute("name", sourceHost);
                        if (pool.getSourcePort() != 0) {
                            newChildDiskNodeForHost.setAttribute("port", String.valueOf(pool.getSourcePort()));
                        }
                        newChildSourceNode.appendChild(newChildDiskNodeForHost);
                    }
                    diskNode.appendChild(newChildSourceNode);

                    found = true;
                    break;
                }
            }
            if (found) {
                for (int j = 0; j < diskChildNodes.getLength(); j++) {
                    Node diskChildNode = diskChildNodes.item(j);
                    if ("auth".equals(diskChildNode.getNodeName())) {
                        NamedNodeMap diskNodeAttributes = diskChildNode.getAttributes();
                        Node diskNodeAttribute = diskNodeAttributes.getNamedItem("username");
                        if (diskNodeAttribute != null) {
                            diskNodeAttribute.setNodeValue(pool.getAuthUserName());
                        }
                        for (int m = 0;  m < diskChildNode.getChildNodes().getLength(); m++) {
                            Node authChild = diskChildNode.getChildNodes().item(m);
                            if ("secret".equals(authChild.getNodeName())) {
                                NamedNodeMap secretAttributes = authChild.getAttributes();
                                Node uuidAttribute = secretAttributes.getNamedItem("uuid");
                                uuidAttribute.setTextContent(pool.getAuthSecretUUID());
                            }
                        }
                    } else if ("encryption".equals(diskChildNode.getNodeName())) {
                        for (int n = 0; n < diskChildNode.getChildNodes().getLength(); n++) {
                            Node encryptionChild = diskChildNode.getChildNodes().item(n);
                            if ("secret".equals(encryptionChild.getNodeName())) {
                                NamedNodeMap secretAttributes = encryptionChild.getAttributes();
                                Node uuidAttribute = secretAttributes.getNamedItem("uuid");
                                uuidAttribute.setTextContent(destSecretUUID);
                            }
                        }
                    }
                }

                StringWriter diskSection = new StringWriter();
                Transformer xformer = TransformerFactory.newInstance().newTransformer();
                xformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
                xformer.transform(new DOMSource(diskNode), new StreamResult(diskSection));

                return diskSection.toString();
            }
        }
        return null;
    }
}
