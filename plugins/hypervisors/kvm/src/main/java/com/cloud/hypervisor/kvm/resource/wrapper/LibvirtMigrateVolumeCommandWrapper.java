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
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
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
import java.util.UUID;

import org.apache.cloudstack.storage.datastore.client.ScaleIOGatewayClient;
import org.apache.cloudstack.storage.datastore.util.ScaleIOUtil;
import org.apache.cloudstack.storage.to.PrimaryDataStoreTO;
import org.apache.cloudstack.storage.to.VolumeObjectTO;
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
        if (srcPrimaryDataStore.getPoolType().equals(Storage.StoragePoolType.PowerFlex)) {
            answer = migratePowerFlexVolume(command, libvirtComputingResource);
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

        Domain dm = null;
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

            String diskdef = generateDestinationDiskXML(dm, srcVolumeId, diskFilePath, destSecretUUID);
            destDiskLabel = generateDestinationDiskLabel(diskdef);

            TypedUlongParameter parameter = new TypedUlongParameter("bandwidth", 0);
            TypedParameter[] parameters = new TypedParameter[1];
            parameters[0] = parameter;

            dm.blockCopy(destDiskLabel, diskdef, parameters, Domain.BlockCopyFlags.REUSE_EXT);
            logger.info(String.format("Block copy has started for the volume %s : %s ", destDiskLabel, srcPath));

            return checkBlockJobStatus(command, dm, destDiskLabel, srcPath, destPath, libvirtComputingResource, conn, srcSecretUUID);
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
            return new MigrateVolumeAnswer(command, false, msg, null);
        } finally {
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

        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(new ByteArrayInputStream(diskXml.getBytes("UTF-8")));
        doc.getDocumentElement().normalize();

        Element disk = doc.getDocumentElement();
        String diskLabel = getAttrValue("target", "dev", disk);

        return diskLabel;
    }

    protected String generateDestinationDiskXML(Domain dm, String srcVolumeId, String diskFilePath, String destSecretUUID) throws LibvirtException, ParserConfigurationException, IOException, TransformerException, SAXException {
        final String domXml = dm.getXMLDesc(0);

        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
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

            KVMPhysicalDisk newDiskCopy = storagePoolManager.copyPhysicalDisk(srcPhysicalDisk, destPath, destPrimaryStorage, command.getWaitInMillSeconds());
            if (newDiskCopy == null) {
                return new MigrateVolumeAnswer(command, false, "Copy command failed to return handle to copied physical disk", destPath);
            }
        }
        catch (Exception ex) {
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
}
